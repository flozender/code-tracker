/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.yarn.server.resourcemanager.security;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.InetSocketAddress;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import javax.security.sasl.SaslException;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.KerberosInfo;
import org.apache.hadoop.security.SecurityInfo;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenIdentifier;
import org.apache.hadoop.security.token.TokenInfo;
import org.apache.hadoop.security.token.TokenSelector;
import org.apache.hadoop.service.AbstractService;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetApplicationReportRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetApplicationReportResponse;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.DrainDispatcher;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.security.client.ClientToAMTokenIdentifier;
import org.apache.hadoop.yarn.security.client.ClientToAMTokenSecretManager;
import org.apache.hadoop.yarn.security.client.ClientToAMTokenSelector;
import org.apache.hadoop.yarn.server.resourcemanager.ClientRMService;
import org.apache.hadoop.yarn.server.resourcemanager.MockAM;
import org.apache.hadoop.yarn.server.resourcemanager.MockNM;
import org.apache.hadoop.yarn.server.resourcemanager.MockRM;
import org.apache.hadoop.yarn.server.resourcemanager.MockRMWithCustomAMLauncher;
import org.apache.hadoop.yarn.server.resourcemanager.rmapp.RMApp;
import org.apache.hadoop.yarn.server.utils.BuilderUtils;
import org.apache.hadoop.yarn.util.ProtoUtils;
import org.apache.hadoop.yarn.util.Records;
import org.junit.Test;

public class TestClientToAMTokens {

  private interface CustomProtocol {
    @SuppressWarnings("unused")
    public static final long versionID = 1L;

    public void ping() throws YarnException, IOException;
  }

  private static class CustomSecurityInfo extends SecurityInfo {

    @Override
    public TokenInfo getTokenInfo(Class<?> protocol, Configuration conf) {
      return new TokenInfo() {

        @Override
        public Class<? extends Annotation> annotationType() {
          return null;
        }

        @Override
        public Class<? extends TokenSelector<? extends TokenIdentifier>>
            value() {
          return ClientToAMTokenSelector.class;
        }
      };
    }

    @Override
    public KerberosInfo getKerberosInfo(Class<?> protocol, Configuration conf) {
      return null;
    }
  };

  private static class CustomAM extends AbstractService implements
      CustomProtocol {

    private final ApplicationAttemptId appAttemptId;
    private final byte[] secretKey;
    private InetSocketAddress address;
    private boolean pinged = false;
    private ClientToAMTokenSecretManager secretManager;
    
    public CustomAM(ApplicationAttemptId appId, byte[] secretKey) {
      super("CustomAM");
      this.appAttemptId = appId;
      this.secretKey = secretKey;
    }

    @Override
    public void ping() throws YarnException, IOException {
      this.pinged = true;
    }

    @Override
    protected void serviceStart() throws Exception {
      Configuration conf = getConfig();

      secretManager = new ClientToAMTokenSecretManager(this.appAttemptId, secretKey);
      Server server;
      try {
        server =
            new RPC.Builder(conf).setProtocol(CustomProtocol.class)
              .setNumHandlers(1).setSecretManager(secretManager)
              .setInstance(this).build();
      } catch (Exception e) {
        throw new YarnRuntimeException(e);
      }
      server.start();
      this.address = NetUtils.getConnectAddress(server);
      super.serviceStart();
    }
    
    public ClientToAMTokenSecretManager getClientToAMTokenSecretManager() {
      return this.secretManager;
    }
  }

  @Test
  public void testClientToAMs() throws Exception {

    final Configuration conf = new Configuration();
    conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION,
      "kerberos");
    UserGroupInformation.setConfiguration(conf);

    ContainerManagementProtocol containerManager =
        mock(ContainerManagementProtocol.class);
    final DrainDispatcher dispatcher = new DrainDispatcher();

    MockRM rm = new MockRMWithCustomAMLauncher(conf, containerManager) {
      protected ClientRMService createClientRMService() {
        return new ClientRMService(this.rmContext, scheduler,
          this.rmAppManager, this.applicationACLsManager,
          this.rmDTSecretManager);
      };

      @Override
      protected Dispatcher createDispatcher() {
        return dispatcher;
      }

      @Override
      protected void doSecureLogin() throws IOException {
      }
    };
    rm.start();

    // Submit an app
    RMApp app = rm.submitApp(1024);

    // Set up a node.
    MockNM nm1 = rm.registerNode("localhost:1234", 3072);
    nm1.nodeHeartbeat(true);
    dispatcher.await();
    

    nm1.nodeHeartbeat(true);
    dispatcher.await();

    // Get the app-report.
    GetApplicationReportRequest request =
        Records.newRecord(GetApplicationReportRequest.class);
    request.setApplicationId(app.getApplicationId());
    GetApplicationReportResponse reportResponse =
        rm.getClientRMService().getApplicationReport(request);
    ApplicationReport appReport = reportResponse.getApplicationReport();
    org.apache.hadoop.yarn.api.records.Token clientToAMToken =
        appReport.getClientToAMToken();

    ApplicationAttemptId appAttempt = app.getCurrentAppAttempt().getAppAttemptId();
    final MockAM mockAM =
        new MockAM(rm.getRMContext(), rm.getApplicationMasterService(),
            app.getCurrentAppAttempt().getAppAttemptId());
    UserGroupInformation appUgi =
        UserGroupInformation.createRemoteUser(appAttempt.toString());
    RegisterApplicationMasterResponse response =
        appUgi.doAs(new PrivilegedAction<RegisterApplicationMasterResponse>() {

          @Override
          public RegisterApplicationMasterResponse run() {
            RegisterApplicationMasterResponse response = null;
            try {
              response = mockAM.registerAppAttempt();
            } catch (Exception e) {
              Assert.fail("Exception was not expected");
            }
            return response;
          }
        });
     
    // ClientToAMToken master key should have been received on register
    // application master response.
    Assert.assertNotNull(response.getClientToAMTokenMasterKey());
    Assert
        .assertTrue(response.getClientToAMTokenMasterKey().array().length > 0);
    
    // Start the AM with the correct shared-secret.
    ApplicationAttemptId appAttemptId =
        app.getAppAttempts().keySet().iterator().next();
    Assert.assertNotNull(appAttemptId);
    final CustomAM am =
        new CustomAM(appAttemptId, response.getClientToAMTokenMasterKey()
            .array());
    am.init(conf);
    am.start();

    // Now the real test!
    // Set up clients to be able to pick up correct tokens.
    SecurityUtil.setSecurityInfoProviders(new CustomSecurityInfo());

    // Verify denial for unauthenticated user
    try {
      CustomProtocol client =
          (CustomProtocol) RPC.getProxy(CustomProtocol.class, 1L, am.address,
            conf);
      client.ping();
      fail("Access by unauthenticated user should fail!!");
    } catch (Exception e) {
      Assert.assertFalse(am.pinged);
    }

    // Verify denial for a malicious user
    UserGroupInformation ugi = UserGroupInformation.createRemoteUser("me");
    Token<ClientToAMTokenIdentifier> token =
        ProtoUtils.convertFromProtoFormat(clientToAMToken, am.address);

    // Malicious user, messes with appId
    ClientToAMTokenIdentifier maliciousID =
        new ClientToAMTokenIdentifier(BuilderUtils.newApplicationAttemptId(
          BuilderUtils.newApplicationId(app.getApplicationId()
            .getClusterTimestamp(), 42), 43));

    Token<ClientToAMTokenIdentifier> maliciousToken =
        new Token<ClientToAMTokenIdentifier>(maliciousID.getBytes(),
          token.getPassword(), token.getKind(),
          token.getService());
    ugi.addToken(maliciousToken);

    try {
      ugi.doAs(new PrivilegedExceptionAction<Void>()  {
        @Override
        public Void run() throws Exception {
          try {
            CustomProtocol client =
                (CustomProtocol) RPC.getProxy(CustomProtocol.class, 1L,
                  am.address, conf);
            client.ping();
            fail("Connection initiation with illegally modified "
                + "tokens is expected to fail.");
            return null;
          } catch (YarnException ex) {
            fail("Cannot get a YARN remote exception as "
                + "it will indicate RPC success");
            throw ex;
          }
        }
      });
    } catch (Exception e) {
      Assert.assertEquals(RemoteException.class.getName(), e.getClass()
          .getName());
      e = ((RemoteException)e).unwrapRemoteException();
      Assert
        .assertEquals(SaslException.class
          .getCanonicalName(), e.getClass().getCanonicalName());
      Assert.assertTrue(e
        .getMessage()
        .contains(
          "DIGEST-MD5: digest response format violation. "
              + "Mismatched response."));
      Assert.assertFalse(am.pinged);
    }

    // Now for an authenticated user
    ugi = UserGroupInformation.createRemoteUser("me");
    ugi.addToken(token);

    ugi.doAs(new PrivilegedExceptionAction<Void>() {
      @Override
      public Void run() throws Exception {
        CustomProtocol client =
            (CustomProtocol) RPC.getProxy(CustomProtocol.class, 1L, am.address,
              conf);
        client.ping();
        Assert.assertTrue(am.pinged);
        return null;
      }
    });
  }

}
