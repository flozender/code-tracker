/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.bootstrap;

import org.apache.lucene.util.StringHelper;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.Version;
import org.elasticsearch.common.PidFile;
import org.elasticsearch.common.SuppressForbidden;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.CreationException;
import org.elasticsearch.common.inject.spi.Message;
import org.elasticsearch.common.jna.Kernel32Library;
import org.elasticsearch.common.jna.Natives;
import org.elasticsearch.common.lease.Releasables;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.monitor.jvm.JvmInfo;
import org.elasticsearch.monitor.process.JmxProcessProbe;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.hyperic.sigar.Sigar;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.google.common.collect.Sets.newHashSet;
import static org.elasticsearch.common.jna.Kernel32Library.ConsoleCtrlHandler;
import static org.elasticsearch.common.settings.ImmutableSettings.Builder.EMPTY_SETTINGS;

/**
 * A main entry point when starting from the command line.
 */
public class Bootstrap {

    private static volatile Bootstrap INSTANCE;

    private Node node;
    private final CountDownLatch keepAliveLatch = new CountDownLatch(1);
    private final Thread keepAliveThread;

    /** creates a new instance */
    Bootstrap() {
        keepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    keepAliveLatch.await();
                } catch (InterruptedException e) {
                    // bail out
                }
            }
        }, "elasticsearch[keepAlive/" + Version.CURRENT + "]");
        keepAliveThread.setDaemon(false);
        // keep this thread alive (non daemon thread) until we shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                keepAliveLatch.countDown();
            }
        });
    }
    
    /** initialize native resources */
    public static void initializeNatives(boolean mlockAll, boolean ctrlHandler) {
        // mlockall if requested
        if (mlockAll) {
            Natives.tryMlockall();
        }
        
        // check if the user is running as root, and bail
        if (Natives.definitelyRunningAsRoot()) {
            if (Boolean.parseBoolean(System.getProperty("es.insecure.allow.root"))) {
                Loggers.getLogger(Bootstrap.class).warn("running as ROOT user. this is a bad idea!");
            } else {
                throw new RuntimeException("don't run elasticsearch as root.");
            }
        }

        // listener for windows close event
        if (ctrlHandler) {
            Natives.addConsoleCtrlHandler(new ConsoleCtrlHandler() {
                @Override
                public boolean handle(int code) {
                    if (CTRL_CLOSE_EVENT == code) {
                        ESLogger logger = Loggers.getLogger(Bootstrap.class);
                        logger.info("running graceful exit on windows");

                        Bootstrap.INSTANCE.stop();
                        return true;
                    }
                    return false;
                }
            });
        }

        // force remainder of JNA to be loaded (if available).
        try {
            Kernel32Library.getInstance();
        } catch (Throwable ignored) {
            // we've already logged this.
        }
 
        // initialize sigar explicitly
        try {
            Sigar.load();
            Loggers.getLogger(Bootstrap.class).trace("sigar libraries loaded successfully");
        } catch (Throwable t) {
            Loggers.getLogger(Bootstrap.class).trace("failed to load sigar libraries", t);
        }

        // init lucene random seed. it will use /dev/urandom where available:
        StringHelper.randomId();
    }

    private void setup(boolean addShutdownHook, Settings settings, Environment environment) throws Exception {
        initializeNatives(settings.getAsBoolean("bootstrap.mlockall", false), 
                          settings.getAsBoolean("bootstrap.ctrlhandler", true));

        NodeBuilder nodeBuilder = NodeBuilder.nodeBuilder().settings(settings).loadConfigSettings(false);
        node = nodeBuilder.build();
        if (addShutdownHook) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    node.close();
                }
            });
        }
        
        // install SM after natives, shutdown hooks, etc.
        setupSecurity(settings, environment);
    }
    
    /** 
     * option for elasticsearch.yml etc to turn off our security manager completely,
     * for example if you want to have your own configuration or just disable.
     */
    static final String SECURITY_SETTING = "security.manager.enabled";

    private void setupSecurity(Settings settings, Environment environment) throws Exception {
        if (settings.getAsBoolean(SECURITY_SETTING, true)) {
            Security.configure(environment);
        }
    }

    @SuppressForbidden(reason = "Exception#printStackTrace()")
    private static void setupLogging(Settings settings, Environment environment) {
        try {
            settings.getClassLoader().loadClass("org.apache.log4j.Logger");
            LogConfigurator.configure(settings);
        } catch (ClassNotFoundException e) {
            // no log4j
        } catch (NoClassDefFoundError e) {
            // no log4j
        } catch (Exception e) {
            sysError("Failed to configure logging...", false);
            e.printStackTrace();
        }
    }

    private static Tuple<Settings, Environment> initialSettings() {
        return InternalSettingsPreparer.prepareSettings(EMPTY_SETTINGS, true);
    }

    private void start() {
        node.start();
        keepAliveThread.start();
    }

    private void stop() {
        try {
            Releasables.close(node);
        } finally {
            keepAliveLatch.countDown();
        }
    }

    public static void main(String[] args) {
        System.setProperty("es.logger.prefix", "");
        INSTANCE = new Bootstrap();

        boolean foreground = System.getProperty("es.foreground", System.getProperty("es-foreground")) != null;
        // handle the wrapper system property, if its a service, don't run as a service
        if (System.getProperty("wrapper.service", "XXX").equalsIgnoreCase("true")) {
            foreground = false;
        }

        String stage = "Settings";

        Settings settings = null;
        Environment environment = null;
        try {
            Tuple<Settings, Environment> tuple = initialSettings();
            settings = tuple.v1();
            environment = tuple.v2();

            if (environment.pidFile() != null) {
                stage = "Pid";
                PidFile.create(environment.pidFile(), true);
            }

            stage = "Logging";
            setupLogging(settings, environment);
        } catch (Exception e) {
            String errorMessage = buildErrorMessage(stage, e);
            sysError(errorMessage, true);
            System.exit(3);
        }

        if (System.getProperty("es.max-open-files", "false").equals("true")) {
            ESLogger logger = Loggers.getLogger(Bootstrap.class);
            logger.info("max_open_files [{}]", JmxProcessProbe.getMaxFileDescriptorCount());
        }

        // warn if running using the client VM
        if (JvmInfo.jvmInfo().getVmName().toLowerCase(Locale.ROOT).contains("client")) {
            ESLogger logger = Loggers.getLogger(Bootstrap.class);
            logger.warn("jvm uses the client vm, make sure to run `java` with the server vm for best performance by adding `-server` to the command line");
        }

        stage = "Initialization";
        try {
            if (!foreground) {
                Loggers.disableConsoleLogging();
                closeSystOut();
            }

            // fail if using broken version
            JVMCheck.check();

            INSTANCE.setup(true, settings, environment);

            stage = "Startup";
            INSTANCE.start();

            if (!foreground) {
                closeSysError();
            }
        } catch (Throwable e) {
            ESLogger logger = Loggers.getLogger(Bootstrap.class);
            if (INSTANCE.node != null) {
                logger = Loggers.getLogger(Bootstrap.class, INSTANCE.node.settings().get("name"));
            }
            String errorMessage = buildErrorMessage(stage, e);
            if (foreground) {
                sysError(errorMessage, true);
                Loggers.disableConsoleLogging();
            }
            logger.error("Exception", e);
            
            System.exit(3);
        }
    }

    @SuppressForbidden(reason = "System#out")
    private static void closeSystOut() {
        System.out.close();
    }

    @SuppressForbidden(reason = "System#err")
    private static void closeSysError() {
        System.err.close();
    }

    @SuppressForbidden(reason = "System#err")
    private static void sysError(String line, boolean flush) {
        System.err.println(line);
        if (flush) {
            System.err.flush();
        }
    }

    private static String buildErrorMessage(String stage, Throwable e) {
        StringBuilder errorMessage = new StringBuilder("{").append(Version.CURRENT).append("}: ");
        errorMessage.append(stage).append(" Failed ...\n");
        if (e instanceof CreationException) {
            CreationException createException = (CreationException) e;
            Set<String> seenMessages = newHashSet();
            int counter = 1;
            for (Message message : createException.getErrorMessages()) {
                String detailedMessage;
                if (message.getCause() == null) {
                    detailedMessage = message.getMessage();
                } else {
                    detailedMessage = ExceptionsHelper.detailedMessage(message.getCause(), true, 0);
                }
                if (detailedMessage == null) {
                    detailedMessage = message.getMessage();
                }
                if (seenMessages.contains(detailedMessage)) {
                    continue;
                }
                seenMessages.add(detailedMessage);
                errorMessage.append("").append(counter++).append(") ").append(detailedMessage);
            }
        } else {
            errorMessage.append("- ").append(ExceptionsHelper.detailedMessage(e, true, 0));
        }
        if (Loggers.getLogger(Bootstrap.class).isDebugEnabled()) {
            errorMessage.append("\n").append(ExceptionsHelper.stackTrace(e));
        }
        return errorMessage.toString();
    }
}
