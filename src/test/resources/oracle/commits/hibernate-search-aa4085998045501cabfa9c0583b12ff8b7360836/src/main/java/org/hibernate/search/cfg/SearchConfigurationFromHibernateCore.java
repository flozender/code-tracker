/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.search.cfg;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.mapping.PersistentClass;

/**
 * Search configuration implementation wrapping an Hibernate Core configuration
 *
 * @author Emmanuel Bernard
 */
public class SearchConfigurationFromHibernateCore implements SearchConfiguration {
	private final org.hibernate.cfg.Configuration cfg;
	private ReflectionManager reflectionManager;

	public SearchConfigurationFromHibernateCore(org.hibernate.cfg.Configuration cfg) {
		if ( cfg == null ) throw new NullPointerException( "Configuration is null" );
		this.cfg = cfg;
	}

	public Iterator<Class<?>> getClassMappings() {
		return new ClassIterator( cfg.getClassMappings() );
	}

	public Class<?> getClassMapping(String name) {
		return cfg.getClassMapping( name ).getMappedClass();
	}

	public String getProperty(String propertyName) {
		return cfg.getProperty( propertyName );
	}

	public Properties getProperties() {
		return cfg.getProperties();
	}

	public ReflectionManager getReflectionManager() {
		if ( reflectionManager == null ) {
			try {
				//TODO introduce a ReflectionManagerHolder interface to avoid reflection
				//I want to avoid hard link between HAN and Validator for such a simple need
				//reuse the existing reflectionManager one when possible
				reflectionManager =
						(ReflectionManager) cfg.getClass().getMethod( "getReflectionManager" ).invoke( cfg );

			}
			catch (Exception e) {
				reflectionManager = new JavaReflectionManager();
			}
		}
		return reflectionManager;
	}

	public SearchMapping getProgrammaticMapping() {
		return ( SearchMapping ) getProperties().get( "hibernate.search.mapping_model" );
	}

	private static class ClassIterator implements Iterator<Class<?>> {
		private Iterator hibernatePersistentClassIterator;
		private Class<?> future;

		private ClassIterator(Iterator hibernatePersistentClassIterator) {
			this.hibernatePersistentClassIterator = hibernatePersistentClassIterator;
		}

		public boolean hasNext() {
			//we need to read the next non null one. getMappedClass() can return null and should be ignored
			if ( future != null) return true;
			do {
				if ( ! hibernatePersistentClassIterator.hasNext() ) {
					future = null;
					return false;
				}
				final PersistentClass pc = (PersistentClass) hibernatePersistentClassIterator.next();
				future = pc.getMappedClass();
			}
			while ( future == null );
			return true;
		}

		public Class<?> next() {
			//run hasNext to init the next element
			if ( ! hasNext() ) throw new NoSuchElementException();
			Class<?> result = future;
			future = null;
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException( "Cannot modify Hibenrate Core metadata" );
		}
	}
}
