/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.tomcat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Service;
import org.apache.catalina.SessionIdGenerator;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.util.CharsetMapper;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactoryTests;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TomcatServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class TomcatServletWebServerFactoryTests
		extends AbstractServletWebServerFactoryTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Override
	protected TomcatServletWebServerFactory getFactory() {
		return new TomcatServletWebServerFactory(0);
	}

	@After
	public void restoreTccl() {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
	}

	// JMX MBean names clash if you get more than one Engine with the same name...
	@Test
	public void tomcatEngineNames() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		factory.setPort(0);
		TomcatWebServer tomcatWebServer = (TomcatWebServer) factory.getWebServer();
		// Make sure that the names are different
		String firstName = ((TomcatWebServer) this.webServer).getTomcat().getEngine()
				.getName();
		String secondName = tomcatWebServer.getTomcat().getEngine().getName();
		assertThat(firstName).as("Tomcat engines must have different names")
				.isNotEqualTo(secondName);
		tomcatWebServer.stop();
	}

	@Test
	public void defaultTomcatListeners() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		assertThat(factory.getContextLifecycleListeners())
				.hasSize(1)
				.first().isInstanceOf(AprLifecycleListener.class);
	}

	@Test
	public void tomcatListeners() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		LifecycleListener[] listeners = new LifecycleListener[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(LifecycleListener.class);
		}
		factory.setContextLifecycleListeners(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextLifecycleListeners(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (LifecycleListener listener : listeners) {
			ordered.verify(listener).lifecycleEvent(any(LifecycleEvent.class));
		}
	}

	@Test
	public void tomcatCustomizers() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatContextCustomizer[] listeners = new TomcatContextCustomizer[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(TomcatContextCustomizer.class);
		}
		factory.setTomcatContextCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextCustomizers(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatContextCustomizer listener : listeners) {
			ordered.verify(listener).customize(any(Context.class));
		}
	}

	@Test
	public void tomcatConnectorCustomizers() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		TomcatConnectorCustomizer[] listeners = new TomcatConnectorCustomizer[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(TomcatConnectorCustomizer.class);
		}
		factory.setTomcatConnectorCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addConnectorCustomizers(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer();
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatConnectorCustomizer listener : listeners) {
			ordered.verify(listener).customize(any(Connector.class));
		}
	}

	@Test
	public void tomcatAdditionalConnectors() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		Connector[] listeners = new Connector[4];
		for (int i = 0; i < listeners.length; i++) {
			Connector connector = mock(Connector.class);
			given(connector.getState()).willReturn(LifecycleState.STOPPED);
			listeners[i] = connector;
		}
		factory.addAdditionalTomcatConnectors(listeners);
		this.webServer = factory.getWebServer();
		Map<Service, Connector[]> connectors = ((TomcatWebServer) this.webServer)
				.getServiceConnectors();
		assertThat(connectors.values().iterator().next().length)
				.isEqualTo(listeners.length + 1);
	}

	@Test
	public void addNullAdditionalConnectorThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Connectors must not be null");
		factory.addAdditionalTomcatConnectors((Connector[]) null);
	}

	@Test
	public void sessionTimeout() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.setSessionTimeout(10);
		assertTimeout(factory, 1);
	}

	@Test
	public void sessionTimeoutInMins() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.setSessionTimeout(1, TimeUnit.MINUTES);
		assertTimeout(factory, 1);
	}

	@Test
	public void noSessionTimeout() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.setSessionTimeout(0);
		assertTimeout(factory, -1);
	}

	@Test
	public void valve() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		Valve valve = mock(Valve.class);
		factory.addContextValves(valve);
		this.webServer = factory.getWebServer();
		verify(valve).setNext(any(Valve.class));
	}

	@Test
	public void setNullTomcatContextCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatContextCustomizers must not be null");
		factory.setTomcatContextCustomizers(null);
	}

	@Test
	public void addNullContextCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatContextCustomizers must not be null");
		factory.addContextCustomizers((TomcatContextCustomizer[]) null);
	}

	@Test
	public void setNullTomcatConnectorCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatConnectorCustomizers must not be null");
		factory.setTomcatConnectorCustomizers(null);
	}

	@Test
	public void addNullConnectorCustomizersThrows() {
		TomcatServletWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TomcatConnectorCustomizers must not be null");
		factory.addConnectorCustomizers((TomcatConnectorCustomizer[]) null);
	}

	@Test
	public void uriEncoding() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.setUriEncoding(Charset.forName("US-ASCII"));
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors()
				.get(tomcat.getService())[0];
		assertThat(connector.getURIEncoding()).isEqualTo("US-ASCII");
	}

	@Test
	public void defaultUriEncoding() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors()
				.get(tomcat.getService())[0];
		assertThat(connector.getURIEncoding()).isEqualTo("UTF-8");
	}

	@Test
	public void sslCiphersConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		TomcatServletWebServerFactory factory = getFactory();
		factory.setSsl(ssl);

		Tomcat tomcat = getTomcat(factory);
		Connector connector = ((TomcatWebServer) this.webServer).getServiceConnectors()
				.get(tomcat.getService())[0];
		SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler()
				.findSslHostConfigs();
		assertThat(sslHostConfigs[0].getCiphers()).isEqualTo("ALPHA:BRAVO:CHARLIE");
	}

	@Test
	public void sslEnabledMultipleProtocolsConfiguration() throws Exception {
		Ssl ssl = getSsl(null, "password", "src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });

		TomcatServletWebServerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Connector connector = tomcat.getConnector();

		SSLHostConfig sslHostConfig = connector.getProtocolHandler()
				.findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols())
				.containsExactlyInAnyOrder("TLSv1.1", "TLSv1.2");
	}

	@Test
	public void sslEnabledProtocolsConfiguration() throws Exception {
		Ssl ssl = getSsl(null, "password", "src/test/resources/test.jks");
		ssl.setEnabledProtocols(new String[] { "TLSv1.2" });
		ssl.setCiphers(new String[] { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "BRAVO" });

		TomcatServletWebServerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.webServer = factory.getWebServer(sessionServletRegistration());
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		this.webServer.start();
		Connector connector = tomcat.getConnector();
		SSLHostConfig sslHostConfig = connector.getProtocolHandler()
				.findSslHostConfigs()[0];
		assertThat(sslHostConfig.getSslProtocol()).isEqualTo("TLS");
		assertThat(sslHostConfig.getEnabledProtocols()).containsExactly("TLSv1.2");
	}

	@Test
	public void primaryConnectorPortClashThrowsIllegalStateException()
			throws InterruptedException, IOException {
		doWithBlockedPort((port) -> {
			TomcatServletWebServerFactory factory = getFactory();
			factory.setPort(port);
			try {
				this.webServer = factory.getWebServer();
				this.webServer.start();
				fail();
			}
			catch (WebServerException ex) {
				// Ignore
			}
		});
	}

	@Test
	public void startupFailureDoesNotResultInUnstoppedThreadsBeingReported()
			throws IOException {
		super.portClashOfPrimaryConnectorResultsInPortInUseException();
		String string = this.outputCapture.toString();
		assertThat(string)
				.doesNotContain("appears to have started a thread named [main]");
	}

	@Test
	public void stopCalledWithoutStart() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.stop();
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		assertThat(tomcat.getServer().getState()).isSameAs(LifecycleState.DESTROYED);
	}

	@Override
	protected void addConnector(int port, AbstractServletWebServerFactory factory) {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(port);
		((TomcatServletWebServerFactory) factory)
				.addAdditionalTomcatConnectors(connector);
	}

	@Test
	public void useForwardHeaders() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		factory.addContextValves(new RemoteIpValve());
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	public void disableDoesNotSaveSessionFiles() throws Exception {
		File baseDir = this.temporaryFolder.newFolder();
		TomcatServletWebServerFactory factory = getFactory();
		// If baseDir is not set SESSIONS.ser is written to a different temp directory
		// each time. By setting it we can really ensure that data isn't saved
		factory.setBaseDirectory(baseDir);
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s1 = getResponse(getLocalUrl("/session"));
		String s2 = getResponse(getLocalUrl("/session"));
		this.webServer.stop();
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s3 = getResponse(getLocalUrl("/session"));
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(s2.split(":")[0]).as(message).isEqualTo(s1.split(":")[1]);
		assertThat(s3.split(":")[0]).as(message).isNotEqualTo(s2.split(":")[1]);
	}

	@Test
	public void jndiLookupsCanBePerformedDuringApplicationContextRefresh()
			throws NamingException {
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0) {

			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				tomcat.enableNaming();
				return super.getTomcatWebServer(tomcat);
			}

		};
		// Server is created in onRefresh
		this.webServer = factory.getWebServer();
		// Lookups should now be possible
		new InitialContext().lookup("java:comp/env");
		// Called in finishRefresh, giving us an opportunity to remove the context binding
		// and avoid a leak
		this.webServer.start();
		// Lookups should no longer be possible
		this.thrown.expect(NamingException.class);
		new InitialContext().lookup("java:comp/env");
	}

	@Test
	public void defaultLocaleCharsetMappingsAreOverriden() throws Exception {
		TomcatServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		// override defaults, see org.apache.catalina.util.CharsetMapperDefault.properties
		assertThat(getCharset(Locale.ENGLISH).toString()).isEqualTo("UTF-8");
		assertThat(getCharset(Locale.FRENCH).toString()).isEqualTo("UTF-8");
	}

	@Test
	public void sessionIdGeneratorIsConfiguredWithAttributesFromTheManager() {
		System.setProperty("jvmRoute", "test");
		try {
			TomcatServletWebServerFactory factory = getFactory();
			this.webServer = factory.getWebServer();
			this.webServer.start();
		}
		finally {
			System.clearProperty("jvmRoute");
		}
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Context context = (Context) tomcat.getHost().findChildren()[0];
		SessionIdGenerator sessionIdGenerator = context.getManager()
				.getSessionIdGenerator();
		assertThat(sessionIdGenerator).isInstanceOf(LazySessionIdGenerator.class);
		assertThat(sessionIdGenerator.getJvmRoute()).isEqualTo("test");
	}

	@Override
	protected JspServlet getJspServlet() throws ServletException {
		Tomcat tomcat = ((TomcatWebServer) this.webServer).getTomcat();
		Container container = tomcat.getHost().findChildren()[0];
		StandardWrapper standardWrapper = (StandardWrapper) container.findChild("jsp");
		if (standardWrapper == null) {
			return null;
		}
		standardWrapper.load();
		return (JspServlet) standardWrapper.getServlet();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, String> getActualMimeMappings() {
		Context context = (Context) ((TomcatWebServer) this.webServer).getTomcat()
				.getHost().findChildren()[0];
		return (Map<String, String>) ReflectionTestUtils.getField(context,
				"mimeMappings");
	}

	@Override
	protected Charset getCharset(Locale locale) {
		Context context = (Context) ((TomcatWebServer) this.webServer).getTomcat()
				.getHost().findChildren()[0];
		CharsetMapper mapper = ((TomcatEmbeddedContext) context).getCharsetMapper();
		String charsetName = mapper.getCharset(locale);
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	private void assertTimeout(TomcatServletWebServerFactory factory, int expected) {
		Tomcat tomcat = getTomcat(factory);
		Context context = (Context) tomcat.getHost().findChildren()[0];
		assertThat(context.getSessionTimeout()).isEqualTo(expected);
	}

	private Tomcat getTomcat(TomcatServletWebServerFactory factory) {
		this.webServer = factory.getWebServer();
		return ((TomcatWebServer) this.webServer).getTomcat();
	}

	@Override
	protected void handleExceptionCausedByBlockedPort(RuntimeException ex,
			int blockedPort) {
		assertThat(ex).isInstanceOf(ConnectorStartFailedException.class);
		assertThat(((ConnectorStartFailedException) ex).getPort()).isEqualTo(blockedPort);
	}

}
