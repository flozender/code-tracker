package com.intellij.rt.execution.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author ven
 * @noinspection HardCodedStringLiteral
 */
public class AppMain {
  private static final String PROPERTY_PORT_NUMBER = "idea.launcher.port";
  private static final String PROPERTY_BINPATH = "idea.launcher.bin.path";

  private static native void triggerControlBreak();

  static {
    String binPath = System.getProperty(PROPERTY_BINPATH) + File.separator;
    final String osName = System.getProperty("os.name").toLowerCase();
    String libPath = null;
    if (osName.startsWith("windows")) {
      libPath = binPath + "breakgen.dll";
    } else if (osName.startsWith("linux")) {
      if (System.getProperty("os.name").toLowerCase().equals("amd64")) {
        libPath = binPath + "libbreakgen64.so";
      } else {
        libPath = binPath + "libbreakgen.so";
      }
    }
    try {
      if (libPath != null) {
        System.load(libPath);
      }
    }
    catch (UnsatisfiedLinkError e) {
      //Do nothing, unknown os or some other error => no ctrl-break is available
    }
  }

  public static void main(String[] args) throws Throwable, NoSuchMethodException, IllegalAccessException {

    final int portNumber = Integer.getInteger(PROPERTY_PORT_NUMBER).intValue();
    Thread t = new Thread(
      new Runnable() {
        public void run() {
          try {
            ServerSocket socket = new ServerSocket(portNumber);
            Socket client = socket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            while (true) {
              String msg = reader.readLine();

              if ("TERM".equals(msg)){
                return;
              }
              else if ("BREAK".equals(msg)) {
                triggerControlBreak();
              }
              else if ("STOP".equals(msg)) {
                System.exit(1);
              }
            }
          } catch (IOException e) {
            return;
          } catch (IllegalArgumentException iae) {
            return;
          } catch (SecurityException se) {
            return;
          }
        }
      }, "Monitor Ctrl-Break");
    try {
      t.setDaemon(true);
      t.start();
    } catch (Exception e) {}

    String mainClass = args[0];
    String[] parms = new String[args.length - 1];
    for (int j = 1; j < args.length; j++) {
      parms[j - 1] = args[j];
    }
    Method m = Class.forName(mainClass).getMethod("main", new Class[]{parms.getClass()});
    try {
      ensureAccess(m);
      m.invoke(null, new Object[]{parms});
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    }
  }

  private static void ensureAccess(Object reflectionObject) {
    // need to call setAccessible here in order to be able to launch package-local classes
    // calling setAccessible() via reflection because the method is missing from java version 1.1.x
    final Class aClass = reflectionObject.getClass();
    try {
      final Method setAccessibleMethod = aClass.getMethod("setAccessible", new Class[] {boolean.class});
      setAccessibleMethod.invoke(reflectionObject, new Object[] {Boolean.TRUE});
    }
    catch (Exception e) {
      // the method not found
    }
  }
}
