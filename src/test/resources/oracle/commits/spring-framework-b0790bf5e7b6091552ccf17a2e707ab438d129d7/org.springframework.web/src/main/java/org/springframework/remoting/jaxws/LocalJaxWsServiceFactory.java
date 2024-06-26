/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.remoting.jaxws;

import java.net.URL;
import java.util.concurrent.Executor;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.handler.HandlerResolver;

import org.springframework.core.task.TaskExecutor;

/**
 * Factory for locally defined JAX-WS {@link javax.xml.ws.Service} references.
 * Uses the JAX-WS {@link javax.xml.ws.Service#create} factory API underneath.
 *
 * <p>Serves as base class for {@link LocalJaxWsServiceFactoryBean} as well as
 * {@link JaxWsPortClientInterceptor} and {@link JaxWsPortProxyFactoryBean}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.xml.ws.Service
 * @see LocalJaxWsServiceFactoryBean
 * @see JaxWsPortClientInterceptor
 * @see JaxWsPortProxyFactoryBean
 */
public class LocalJaxWsServiceFactory {

	private URL wsdlDocumentUrl;

	private String namespaceUri;

	private String serviceName;

	private Executor executor;

	private HandlerResolver handlerResolver;


	/**
	 * Set the URL of the WSDL document that describes the service.
	 */
	public void setWsdlDocumentUrl(URL wsdlDocumentUrl) {
		this.wsdlDocumentUrl = wsdlDocumentUrl;
	}

	/**
	 * Return the URL of the WSDL document that describes the service.
	 */
	public URL getWsdlDocumentUrl() {
		return this.wsdlDocumentUrl;
	}

	/**
	 * Set the namespace URI of the service.
	 * Corresponds to the WSDL "targetNamespace".
	 */
	public void setNamespaceUri(String namespaceUri) {
		this.namespaceUri = (namespaceUri != null ? namespaceUri.trim() : null);
	}

	/**
	 * Return the namespace URI of the service.
	 */
	public String getNamespaceUri() {
		return this.namespaceUri;
	}

	/**
	 * Set the name of the service to look up.
	 * Corresponds to the "wsdl:service" name.
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * Return the name of the service.
	 */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * Set the JDK concurrent executor to use for asynchronous executions
	 * that require callbacks.
	 * @see javax.xml.ws.Service#setExecutor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Set the Spring TaskExecutor to use for asynchronous executions
	 * that require callbacks.
	 * @see javax.xml.ws.Service#setExecutor
	 */
	public void setTaskExecutor(TaskExecutor executor) {
		this.executor = executor;
	}

	/**
	 * Set the JAX-WS HandlerResolver to use for all proxies and dispatchers
	 * created through this factory.
	 * @see javax.xml.ws.Service#setHandlerResolver
	 */
	public void setHandlerResolver(HandlerResolver handlerResolver) {
		this.handlerResolver = handlerResolver;
	}


	/**
	 * Create a JAX-WS Service according to the parameters of this factory.
	 * @see #setServiceName
	 * @see #setWsdlDocumentUrl
	 */
	public Service createJaxWsService() {
		Service service = (this.wsdlDocumentUrl != null ?
				Service.create(this.wsdlDocumentUrl, getQName(this.serviceName)) :
				Service.create(getQName(this.serviceName)));

		if (this.executor != null) {
			service.setExecutor(this.executor);
		}
		if (this.handlerResolver != null) {
			service.setHandlerResolver(this.handlerResolver);
		}
		return service;
	}

	/**
	 * Return a QName for the given name, relative to the namespace URI
	 * of this factory, if given.
	 * @see #setNamespaceUri
	 */
	protected QName getQName(String name) {
		return (getNamespaceUri() != null ? new QName(getNamespaceUri(), name) : new QName(name));
	}

}
