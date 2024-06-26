/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.util.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchException;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoadingException;
import org.hibernate.search.engine.service.spi.ServiceManager;

/**
 * Utility class to load instances of other classes by using a fully qualified name,
 * or from a class type.
 * Uses reflection and throws SearchException(s) with proper descriptions of the error,
 * like the target class is missing a proper constructor, is an interface, is not found...
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 * @author Ales Justin
 */
public class ClassLoaderHelper {

	private ClassLoaderHelper() {
	}

	/**
	 * Creates an instance of a target class specified by the fully qualified class name using a {@link ClassLoader}
	 * as fallback when the class cannot be found in the context one.
	 *
	 * @param <T> matches the type of targetSuperType: defines the return type
	 * @param targetSuperType the return type of the function, the classNameToLoad will be checked
	 * to be assignable to this type.
	 * @param classNameToLoad a fully qualified class name, whose type is assignable to targetSuperType
	 * @param componentDescription a meaningful description of the role the instance will have,
	 * used to enrich error messages to describe the context of the error
	 * @param serviceManager Service manager allowing access to the class loading service
	 *
	 * @return a new instance of the type given by {@code classNameToLoad}
	 *
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * classNotFound, missing proper constructor, wrong type, security errors.
	 */
	public static <T> T instanceFromName(Class<T> targetSuperType,
			String classNameToLoad,
			String componentDescription,
			ServiceManager serviceManager) {
		final Class<?> clazzDef = classForName( classNameToLoad, componentDescription, serviceManager );
		return instanceFromClass( targetSuperType, clazzDef, componentDescription );
	}

	/**
	 * Creates an instance of target class
	 *
	 * @param <T> the type of targetSuperType: defines the return type
	 * @param targetSuperType the created instance will be checked to be assignable to this type
	 * @param classToLoad the class to be instantiated
	 * @param componentDescription a role name/description to contextualize error messages
	 *
	 * @return a new instance of classToLoad
	 *
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * missing proper constructor, wrong type, security errors.
	 */
	public static <T> T instanceFromClass(Class<T> targetSuperType, Class<?> classToLoad, String componentDescription) {
		checkClassType( classToLoad, componentDescription );
		checkHasNoArgConstructor( classToLoad, componentDescription );
		Object instance;
		try {
			instance = classToLoad.newInstance();
		}
		catch (IllegalAccessException e) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
							". Class or constructor is not accessible.", e
			);
		}
		catch (InstantiationException e) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
							". Verify it has a no-args public constructor and is not abstract.", e
			);
		}
		return verifySuperTypeCompatibility( targetSuperType, instance, classToLoad, componentDescription );
	}

	/**
	 * Verifies that an object instance is implementing a specific interface, or extending a type.
	 *
	 * @param targetSuperType the type to extend, or the interface it should implement
	 * @param instance the object instance to be verified
	 * @param classToLoad the Class of the instance
	 * @param componentDescription a user friendly description of the component represented by the verified instance
	 *
	 * @return the same instance
	 */
	@SuppressWarnings("unchecked")
	private static <T> T verifySuperTypeCompatibility(Class<T> targetSuperType, Object instance, Class<?> classToLoad, String componentDescription) {
		if ( !targetSuperType.isInstance( instance ) ) {
			// have a proper error message according to interface implementation or subclassing
			if ( targetSuperType.isInterface() ) {
				throw new SearchException(
						"Wrong configuration of " + componentDescription + ": class " + classToLoad.getName()
								+ " does not implement interface " + targetSuperType.getName()
				);
			}
			else {
				throw new SearchException(
						"Wrong configuration of " + componentDescription + ": class " + classToLoad.getName()
								+ " is not a subtype of " + targetSuperType.getName()
				);
			}
		}
		else {
			return (T) instance;
		}
	}

	/**
	 * Creates an instance of target class having a Map of strings as constructor parameter.
	 * Most of the Analyzer SPIs provided by Lucene have such a constructor.
	 *
	 * @param <T> the type of targetSuperType: defines the return type
	 * @param targetSuperType the created instance will be checked to be assignable to this type
	 * @param classToLoad the class to be instantiated
	 * @param componentDescription a role name/description to contextualize error messages
	 * @param constructorParameter a Map to be passed to the constructor. The loaded type must have such a constructor.
	 *
	 * @return a new instance of classToLoad
	 *
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * missing proper constructor, wrong type, security errors.
	 */
	public static <T> T instanceFromClass(Class<T> targetSuperType, Class<?> classToLoad, String componentDescription,
			Map<String, String> constructorParameter) {
		checkClassType( classToLoad, componentDescription );
		Constructor<?> singleMapConstructor = getSingleMapConstructor( classToLoad, componentDescription );
		if ( constructorParameter == null ) {
			constructorParameter = new HashMap<String, String>( 0 );//can't use the emptyMap singleton as it needs to be mutable
		}
		final Object instance;
		try {
			instance = singleMapConstructor.newInstance( constructorParameter );
		}
		catch (Exception e) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
							". The implementation class did not recognize the applied parameters.", e
			);
		}
		return verifySuperTypeCompatibility( targetSuperType, instance, classToLoad, componentDescription );
	}

	public static Analyzer analyzerInstanceFromClass(Class<?> classToInstantiate, Version luceneMatchVersion) {
		checkClassType( classToInstantiate, "analyzer" );
		Analyzer analyzerInstance;

		// try to get a constructor with a version parameter
		Constructor constructor;
		boolean useVersionParameter = true;
		try {
			constructor = classToInstantiate.getConstructor( Version.class );
		}
		catch (NoSuchMethodException e) {
			try {
				constructor = classToInstantiate.getConstructor();
				useVersionParameter = false;
			}
			catch (NoSuchMethodException nsme) {
				StringBuilder msg = new StringBuilder( "Unable to instantiate analyzer class: " );
				msg.append( classToInstantiate.getName() );
				msg.append( ". Class neither has a default constructor nor a constructor with a Version parameter" );
				throw new SearchException( msg.toString(), e );
			}
		}

		try {
			if ( useVersionParameter ) {
				analyzerInstance = (Analyzer) constructor.newInstance( luceneMatchVersion );
			}
			else {
				analyzerInstance = (Analyzer) constructor.newInstance();
			}
		}
		catch (IllegalAccessException e) {
			throw new SearchException(
					"Unable to instantiate analyzer class: " + classToInstantiate.getName() +
							". Class or constructor is not accessible.", e
			);
		}
		catch (InstantiationException e) {
			throw new SearchException(
					"Unable to instantiate analyzer class: " + classToInstantiate.getName() +
							". Verify it has a no-args public constructor and is not abstract.", e
			);
		}
		catch (InvocationTargetException e) {
			throw new SearchException(
					"Unable to instantiate analyzer class: " + classToInstantiate.getName() +
							". Verify it has a no-args public constructor and is not abstract."
							+ " Also Analyzer implementation classes or their tokenStream() and reusableTokenStream() implementations must be final.",
					e
			);
		}
		return analyzerInstance;
	}

	private static void checkClassType(Class<?> classToLoad, String componentDescription) {
		if ( classToLoad.isInterface() ) {
			throw new SearchException(
					classToLoad.getName() + " defined for component " + componentDescription
							+ " is an interface: implementation required."
			);
		}
	}

	/**
	 * Verifies if target class has a no-args constructor, and that it is
	 * accessible in current security manager.
	 *
	 * @param classToLoad the class type to check
	 * @param componentDescription adds a meaningful description to the type to describe in the
	 * exception message
	 */
	private static void checkHasNoArgConstructor(Class<?> classToLoad, String componentDescription) {
		try {
			classToLoad.getConstructor();
		}
		catch (SecurityException e) {
			throw new SearchException(
					classToLoad.getName() + " defined for component " + componentDescription
							+ " could not be instantiated because of a security manager error", e
			);
		}
		catch (NoSuchMethodException e) {
			throw new SearchException(
					classToLoad.getName() + " defined for component " + componentDescription
							+ " is missing a no-arguments constructor"
			);
		}
	}

	private static Constructor<?> getSingleMapConstructor(Class<?> classToLoad, String componentDescription) {
		try {
			return classToLoad.getConstructor( Map.class );
		}
		catch (SecurityException e) {
			throw new SearchException(
					classToLoad.getName() + " defined for component " + componentDescription
							+ " could not be instantiated because of a security manager error", e
			);
		}
		catch (NoSuchMethodException e) {
			throw new SearchException(
					classToLoad.getName() + " defined for component " + componentDescription
							+ " is missing an appropriate constructor: expected a public constructor with a single parameter of type Map"
			);
		}
	}

	public static Class<?> classForName(String classNameToLoad, String componentDescription, ServiceManager serviceManager) {
		Class<?> clazz;
		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		try {
			clazz = classLoaderService.classForName( classNameToLoad );
		}
		catch (ClassLoadingException e) {
			throw new SearchException(
					"Unable to find " + componentDescription +
							" implementation class: " + classNameToLoad, e
			);
		}
		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}
		return clazz;
	}

	public static <T> Class<? extends T> classForName(Class<T> targetSuperType,
			String classNameToLoad,
			String componentDescription,
			ServiceManager serviceManager) {
		final Class<?> clazzDef = classForName( classNameToLoad, componentDescription, serviceManager );
		try {
			return clazzDef.asSubclass( targetSuperType );
		}
		catch (ClassCastException cce) {
			throw new SearchException(
					"Unable to load class for " + componentDescription + ". Configured implementation " + classNameToLoad +
							" is not assignable to type " + targetSuperType
			);
		}
	}

	/**
	 * Perform resolution of a class name.
	 * <p/>
	 * Here we first check the context classloader, if one, before delegating to
	 * {@link Class#forName(String, boolean, ClassLoader)} using the caller's classloader
	 *
	 * @param classNameToLoad The class name
	 * @param serviceManager The service manager from which to retrieve the class loader service
	 *
	 * @return The class reference.
	 *
	 * @throws ClassLoadingException From {@link Class#forName(String, boolean, ClassLoader)}.
	 */
	public static Class classForName(String classNameToLoad, ServiceManager serviceManager) {
		ClassLoaderService classLoaderService = serviceManager.requestService( ClassLoaderService.class );
		try {
			return classLoaderService.classForName( classNameToLoad );
		}

		finally {
			serviceManager.releaseService( ClassLoaderService.class );
		}
	}

	/**
	 * Perform resolution of a class name.
	 * <p/>
	 * Here we first check the context classloader, if one, before delegating to
	 * {@link Class#forName(String, boolean, ClassLoader)} using the caller's classloader
	 *
	 * @param name The class name
	 * @param classLoader The classloader from which this call originated.
	 *
	 * @return The class reference.
	 *
	 * @throws ClassNotFoundException From {@link Class#forName(String, boolean, ClassLoader)}.
	 */
	public static Class classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
		try {
			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass( name );
			}
		}
		catch (Throwable ignore) {
		}
		return Class.forName( name, true, classLoader );
	}

}
