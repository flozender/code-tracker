/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.intercept.javassist;
import java.util.Set;
import org.hibernate.bytecode.javassist.FieldHandled;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.intercept.FieldInterceptor;

/**
 * @author Steve Ebersole
 */
public class JavassistHelper {
	private JavassistHelper() {
	}

	public static FieldInterceptor extractFieldInterceptor(Object entity) {
		return ( FieldInterceptor ) ( ( FieldHandled ) entity ).getFieldHandler();
	}

	public static FieldInterceptor injectFieldInterceptor(
			Object entity,
	        String entityName,
	        Set uninitializedFieldNames,
	        SessionImplementor session) {
		FieldInterceptorImpl fieldInterceptor = new FieldInterceptorImpl( session, uninitializedFieldNames, entityName );
		( ( FieldHandled ) entity ).setFieldHandler( fieldInterceptor );
		return fieldInterceptor;
	}
}
