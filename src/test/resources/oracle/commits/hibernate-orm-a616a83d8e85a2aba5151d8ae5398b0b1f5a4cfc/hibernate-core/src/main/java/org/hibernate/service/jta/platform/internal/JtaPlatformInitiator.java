/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.jta.platform.internal;

import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.HibernateLogger;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.jndi.JndiHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.jta.platform.spi.JtaPlatformException;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * Standard initiator for the standard {@link org.hibernate.service.jta.platform.spi.JtaPlatform}
 *
 * @author Steve Ebersole
 */
public class JtaPlatformInitiator implements BasicServiceInitiator<JtaPlatform> {
	public static final JtaPlatformInitiator INSTANCE = new JtaPlatformInitiator();
	public static final String JTA_PLATFORM = "hibernate.jta.platform";

    private static final HibernateLogger LOG = Logger.getMessageLogger(HibernateLogger.class, JtaPlatformInitiator.class.getName());

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public JtaPlatform initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object platform = getConfiguredPlatform( configurationValues, registry );
		if ( platform == null ) {
			return new NoJtaPlatform();
		}

		if ( JtaPlatform.class.isInstance( platform ) ) {
			return (JtaPlatform) platform;
		}

		final Class<JtaPlatform> jtaPlatformImplClass;

		if ( Class.class.isInstance( platform ) ) {
			jtaPlatformImplClass = (Class<JtaPlatform>) platform;
		}
		else {
			final String platformImplName = platform.toString();
			final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
			try {
				jtaPlatformImplClass = classLoaderService.classForName( platformImplName );
			}
			catch ( Exception e ) {
				throw new HibernateException( "Unable to locate specified JtaPlatform class [" + platformImplName + "]", e );
			}
		}

		try {
			return jtaPlatformImplClass.newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to create specified JtaPlatform class [" + jtaPlatformImplClass.getName() + "]", e );
		}
	}

	private Object getConfiguredPlatform(Map configVales, ServiceRegistryImplementor registry) {
		Object platform = configVales.get( JTA_PLATFORM );
		if ( platform == null ) {
			final String transactionManagerLookupImplName = (String) configVales.get( Environment.TRANSACTION_MANAGER_STRATEGY );
			if ( transactionManagerLookupImplName != null ) {
                LOG.deprecatedTransactionManagerStrategy(TransactionManagerLookup.class.getName(),
                                                         Environment.TRANSACTION_MANAGER_STRATEGY,
                                                         JtaPlatform.class.getName(),
                                                         JTA_PLATFORM);
				platform = mapLegacyClasses( transactionManagerLookupImplName, configVales, registry );
                LOG.debugf("Mapped %s -> %s", transactionManagerLookupImplName, platform);
			}
		}
		return platform;
	}

	private JtaPlatform mapLegacyClasses(String tmlImplName, Map configVales, ServiceRegistryImplementor registry) {
		if ( tmlImplName == null ) {
			return null;
		}

        LOG.legacyTransactionManagerStrategy(JtaPlatform.class.getName(), JTA_PLATFORM);

		if ( "org.hibernate.transaction.BESTransactionManagerLookup".equals( tmlImplName ) ) {
			return new BorlandEnterpriseServerJtaPlatform();
		}

		if ( "org.hibernate.transaction.BTMTransactionManagerLookup".equals( tmlImplName ) ) {
			return new BitronixJtaPlatform();
		}

		if ( "org.hibernate.transaction.JBossTransactionManagerLookup".equals( tmlImplName ) ) {
			return new JBossAppServerPlatform();
		}

		if ( "org.hibernate.transaction.JBossTSStandaloneTransactionManagerLookup".equals( tmlImplName ) ) {
			return new JBossStandAloneJtaPlatform();
		}

		if ( "org.hibernate.transaction.JOnASTransactionManagerLookup".equals( tmlImplName ) ) {
			return new JOnASJtaPlatform();
		}

		if ( "org.hibernate.transaction.JOTMTransactionManagerLookup".equals( tmlImplName ) ) {
			return new JOTMJtaPlatform();
		}

		if ( "org.hibernate.transaction.JRun4TransactionManagerLookup".equals( tmlImplName ) ) {
			return new JRun4JtaPlatform();
		}

		if ( "org.hibernate.transaction.OC4JTransactionManagerLookup".equals( tmlImplName ) ) {
			return new OC4JJtaPlatform();
		}

		if ( "org.hibernate.transaction.OrionTransactionManagerLookup".equals( tmlImplName ) ) {
			return new OrionJtaPlatform();
		}

		if ( "org.hibernate.transaction.ResinTransactionManagerLookup".equals( tmlImplName ) ) {
			return new ResinJtaPlatform();
		}

		if ( "org.hibernate.transaction.SunONETransactionManagerLookup".equals( tmlImplName ) ) {
			return new SunOneJtaPlatform();
		}

		if ( "org.hibernate.transaction.WeblogicTransactionManagerLookup".equals( tmlImplName ) ) {
			return new WeblogicJtaPlatform();
		}

		if ( "org.hibernate.transaction.WebSphereTransactionManagerLookup".equals( tmlImplName ) ) {
			return new WebSphereJtaPlatform();
		}

		if ( "org.hibernate.transaction.WebSphereExtendedJTATransactionLookup".equals( tmlImplName ) ) {
			return new WebSphereExtendedJtaPlatform();
		}

		try {
			TransactionManagerLookup lookup = (TransactionManagerLookup) registry.getService( ClassLoaderService.class )
					.classForName( tmlImplName )
					.newInstance();
			return new TransactionManagerLookupBridge( lookup, JndiHelper.extractJndiProperties( configVales ) );
		}
		catch ( Exception e ) {
			throw new JtaPlatformException(
					"Unable to build " + TransactionManagerLookupBridge.class.getName() + " from specified " +
							TransactionManagerLookup.class.getName() + " implementation: " +
							tmlImplName
			);
		}
	}
}
