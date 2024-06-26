/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.persister.internal;

import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistry;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class PersisterClassResolverInitiator implements ServiceInitiator<PersisterClassResolver> {
	public static final PersisterClassResolverInitiator INSTANCE = new PersisterClassResolverInitiator();
	public static final String IMPL_NAME = "hibernate.persister.resolver";

	@Override
	public Class<PersisterClassResolver> getServiceInitiated() {
		return PersisterClassResolver.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public PersisterClassResolver initiateService(Map configurationValues, ServiceRegistry registry) {
		final Object customImpl = configurationValues.get( IMPL_NAME );
		if ( customImpl == null ) {
			return new StandardPersisterClassResolver();
		}

		if ( PersisterClassResolver.class.isInstance( customImpl ) ) {
			return (PersisterClassResolver) customImpl;
		}

		final Class<? extends PersisterClassResolver> customImplClass = Class.class.isInstance( customImpl )
				? (Class<? extends PersisterClassResolver>) customImpl
				: registry.getService( ClassLoaderService.class ).classForName( customImpl.toString() );

		try {
			return customImplClass.newInstance();
		}
		catch (Exception e) {
			throw new ServiceException( "Could not initialize custom PersisterClassResolver impl [" + customImplClass.getName() + "]", e );
		}
	}
}
