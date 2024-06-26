/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.spi;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;

import org.hibernate.PropertyAccessException;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * @author Steve Ebersole
 */
public class EnhancedGetterMethodImpl implements Getter {
	private static final CoreMessageLogger LOG = messageLogger( EnhancedGetterMethodImpl.class );

	private final Class containerClass;
	private final String propertyName;
	private final Method getterMethod;

	public EnhancedGetterMethodImpl(Class containerClass, String propertyName, Method getterMethod) {
		this.containerClass = containerClass;
		this.propertyName = propertyName;
		this.getterMethod = getterMethod;
	}

	private boolean isAttributeLoaded(Object owner) {
		if ( owner instanceof PersistentAttributeInterceptable ) {
			PersistentAttributeInterceptor interceptor = ( (PersistentAttributeInterceptable) owner ).$$_hibernate_getInterceptor();
			if ( interceptor != null && interceptor instanceof LazyAttributeLoadingInterceptor ) {
				return ( (LazyAttributeLoadingInterceptor) interceptor ).isAttributeLoaded( propertyName );
			}
		}
		return true;
	}

	@Override
	public Object get(Object owner) {
		try {

			// We don't want to trigger lazy loading of byte code enhanced attributes
			if ( isAttributeLoaded( owner ) ) {
				return getterMethod.invoke( owner );
			}
			return null;

		}
		catch (InvocationTargetException ite) {
			throw new PropertyAccessException(
					ite,
					"Exception occurred inside",
					false,
					containerClass,
					propertyName
			);
		}
		catch (IllegalAccessException iae) {
			throw new PropertyAccessException(
					iae,
					"IllegalAccessException occurred while calling",
					false,
					containerClass,
					propertyName
			);
			//cannot occur
		}
		catch (IllegalArgumentException iae) {
			LOG.illegalPropertyGetterArgument( containerClass.getName(), propertyName );
			throw new PropertyAccessException(
					iae,
					"IllegalArgumentException occurred calling",
					false,
					containerClass,
					propertyName
			);
		}
	}

	@Override
	public Object getForInsert(Object owner, Map mergeMap, SessionImplementor session) {
		return get( owner );
	}

	@Override
	public Class getReturnType() {
		return getterMethod.getReturnType();
	}

	@Override
	public Member getMember() {
		return getterMethod;
	}

	@Override
	public String getMethodName() {
		return getterMethod.getName();
	}

	@Override
	public Method getMethod() {
		return getterMethod;
	}

	private Object writeReplace() throws ObjectStreamException {
		return new SerialForm( containerClass, propertyName, getterMethod );
	}

	private static class SerialForm implements Serializable {
		private final Class containerClass;
		private final String propertyName;

		private final Class declaringClass;
		private final String methodName;

		private SerialForm(Class containerClass, String propertyName, Method method) {
			this.containerClass = containerClass;
			this.propertyName = propertyName;
			this.declaringClass = method.getDeclaringClass();
			this.methodName = method.getName();
		}

		private Object readResolve() {
			return new EnhancedGetterMethodImpl( containerClass, propertyName, resolveMethod() );
		}

		@SuppressWarnings("unchecked")
		private Method resolveMethod() {
			try {
				return declaringClass.getDeclaredMethod( methodName );
			}
			catch (NoSuchMethodException e) {
				throw new PropertyAccessSerializationException(
						"Unable to resolve getter method on deserialization : " + declaringClass.getName() + "#" + methodName
				);
			}
		}
	}
}
