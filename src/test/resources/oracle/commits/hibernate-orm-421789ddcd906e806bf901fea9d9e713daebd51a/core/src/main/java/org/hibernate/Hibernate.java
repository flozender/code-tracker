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
package org.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.HibernateIterator;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.StreamUtils;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.AnyType;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.BigIntegerType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.BlobType;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ByteType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.CharacterType;
import org.hibernate.type.ClassType;
import org.hibernate.type.ClobType;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CurrencyType;
import org.hibernate.type.CustomType;
import org.hibernate.type.DateType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.FloatType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LocaleType;
import org.hibernate.type.LongType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.ObjectType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.ShortType;
import org.hibernate.type.StringType;
import org.hibernate.type.TextType;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimeZoneType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.TrueFalseType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.YesNoType;
import org.hibernate.type.CharArrayType;
import org.hibernate.type.WrapperBinaryType;
import org.hibernate.type.CharacterArrayType;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.ImageType;
import org.hibernate.type.MaterializedClobType;
import org.hibernate.usertype.CompositeUserType;

/**
 * <ul>
 * <li>Provides access to the full range of Hibernate built-in types. <tt>Type</tt>
 * instances may be used to bind values to query parameters.
 * <li>A factory for new <tt>Blob</tt>s and <tt>Clob</tt>s.
 * <li>Defines static methods for manipulation of proxies.
 * </ul>
 *
 * @author Gavin King
 * @see java.sql.Clob
 * @see java.sql.Blob
 * @see org.hibernate.type.Type
 */

public final class Hibernate {
	/**
	 * Cannot be instantiated.
	 */
	private Hibernate() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Hibernate <tt>long</tt> type.
	 * @deprecated Use {@link LongType#INSTANCE} instead.
	 */
	public static final LongType LONG = LongType.INSTANCE;
	/**
	 * Hibernate <tt>short</tt> type.
	 * @deprecated Use {@link ShortType#INSTANCE} instead.
	 */
	public static final ShortType SHORT = ShortType.INSTANCE;
	/**
	 * Hibernate <tt>integer</tt> type.
	 * @deprecated Use {@link IntegerType#INSTANCE} instead.
	 */
	public static final IntegerType INTEGER = IntegerType.INSTANCE;
	/**
	 * Hibernate <tt>byte</tt> type.
	 * @deprecated Use {@link ByteType#INSTANCE} instead.
	 */
	public static final ByteType BYTE = ByteType.INSTANCE;
	/**
	 * Hibernate <tt>float</tt> type.
	 * @deprecated Use {@link FloatType#INSTANCE} instead.
	 */
	public static final FloatType FLOAT = FloatType.INSTANCE;
	/**
	 * Hibernate <tt>double</tt> type.
	 * @deprecated Use {@link DoubleType#INSTANCE} instead.
	 */
	public static final DoubleType DOUBLE = DoubleType.INSTANCE;
	/**
	 * Hibernate <tt>character</tt> type.
	 * @deprecated Use {@link CharacterType#INSTANCE} instead.
	 */
	public static final CharacterType CHARACTER = CharacterType.INSTANCE;
	/**
	 * Hibernate <tt>string</tt> type.
	 * @deprecated Use {@link StringType#INSTANCE} instead.
	 */
	public static final StringType STRING = StringType.INSTANCE;
	/**
	 * Hibernate <tt>time</tt> type.
	 * @deprecated Use {@link TimeType#INSTANCE} instead.
	 */
	public static final TimeType TIME = TimeType.INSTANCE;
	/**
	 * Hibernate <tt>date</tt> type.
	 * @deprecated Use {@link DateType#INSTANCE} instead.
	 */
	public static final DateType DATE = DateType.INSTANCE;
	/**
	 * Hibernate <tt>timestamp</tt> type.
	 * @deprecated Use {@link TimestampType#INSTANCE} instead.
	 */
	public static final TimestampType TIMESTAMP = TimestampType.INSTANCE;
	/**
	 * Hibernate <tt>boolean</tt> type.
	 * @deprecated Use {@link BooleanType#INSTANCE} instead.
	 */
	public static final BooleanType BOOLEAN = BooleanType.INSTANCE;
	/**
	 * Hibernate <tt>true_false</tt> type.
	 * @deprecated Use {@link TrueFalseType#INSTANCE} instead.
	 */
	public static final TrueFalseType TRUE_FALSE = TrueFalseType.INSTANCE;
	/**
	 * Hibernate <tt>yes_no</tt> type.
	 * @deprecated Use {@link YesNoType#INSTANCE} instead.
	 */
	public static final YesNoType YES_NO = YesNoType.INSTANCE;
	/**
	 * Hibernate <tt>big_decimal</tt> type.
	 * @deprecated Use {@link BigDecimalType#INSTANCE} instead.
	 */
	public static final BigDecimalType BIG_DECIMAL = BigDecimalType.INSTANCE;
	/**
	 * Hibernate <tt>big_integer</tt> type.
	 * @deprecated Use {@link BigIntegerType#INSTANCE} instead.
	 */
	public static final BigIntegerType BIG_INTEGER = BigIntegerType.INSTANCE;
	/**
	 * Hibernate <tt>binary</tt> type.
	 * @deprecated Use {@link BinaryType#INSTANCE} instead.
	 */
	public static final BinaryType BINARY = BinaryType.INSTANCE;
	/**
	 * Hibernate <tt>wrapper-binary</tt> type.
	 * @deprecated Use {@link WrapperBinaryType#INSTANCE} instead.
	 */
	public static final WrapperBinaryType WRAPPER_BINARY = WrapperBinaryType.INSTANCE;
	/**
	 * Hibernate char[] type.
	 * @deprecated Use {@link CharArrayType#INSTANCE} instead.
	 */
	public static final CharArrayType CHAR_ARRAY = CharArrayType.INSTANCE;
	/**
	 * Hibernate Character[] type.
	 * @deprecated Use {@link CharacterArrayType#INSTANCE} instead.
	 */
	public static final CharacterArrayType CHARACTER_ARRAY = CharacterArrayType.INSTANCE;
	/**
	 * Hibernate <tt>image</tt> type.
	 * @deprecated Use {@link ImageType#INSTANCE} instead.
	 */
	public static final ImageType IMAGE = ImageType.INSTANCE;
	/**
	 * Hibernate <tt>text</tt> type.
	 * @deprecated Use {@link TextType#INSTANCE} instead.
	 */
	public static final TextType TEXT = TextType.INSTANCE;
	/**
	 * Hibernate <tt>materialized_blob</tt> type.
	 * @deprecated Use {@link MaterializedBlobType#INSTANCE} instead.
	 */
	public static final MaterializedBlobType MATERIALIZED_BLOB = MaterializedBlobType.INSTANCE;
	/**
	 * Hibernate <tt>materialized_clob</tt> type.
	 * @deprecated Use {@link MaterializedClobType#INSTANCE} instead.
	 */
	public static final MaterializedClobType MATERIALIZED_CLOB = MaterializedClobType.INSTANCE;
	/**
	 * Hibernate <tt>blob</tt> type.
	 * @deprecated Use {@link BlobType#INSTANCE} instead.
	 */
	public static final BlobType BLOB = BlobType.INSTANCE;
	/**
	 * Hibernate <tt>clob</tt> type.
	 * @deprecated Use {@link ClobType#INSTANCE} instead.
	 */
	public static final ClobType CLOB = ClobType.INSTANCE;
	/**
	 * Hibernate <tt>calendar</tt> type.
	 * @deprecated Use {@link CalendarType#INSTANCE} instead.
	 */
	public static final CalendarType CALENDAR = CalendarType.INSTANCE;
	/**
	 * Hibernate <tt>calendar_date</tt> type.
	 * @deprecated Use {@link CalendarDateType#INSTANCE} instead.
	 */
	public static final CalendarDateType CALENDAR_DATE = CalendarDateType.INSTANCE;
	/**
	 * Hibernate <tt>locale</tt> type.
	 * @deprecated Use {@link LocaleType#INSTANCE} instead.
	 */
	public static final LocaleType LOCALE = LocaleType.INSTANCE;
	/**
	 * Hibernate <tt>currency</tt> type.
	 * @deprecated Use {@link CurrencyType#INSTANCE} instead.
	 */
	public static final CurrencyType CURRENCY = CurrencyType.INSTANCE;
	/**
	 * Hibernate <tt>timezone</tt> type.
	 * @deprecated Use {@link TimeZoneType#INSTANCE} instead.
	 */
	public static final TimeZoneType TIMEZONE = TimeZoneType.INSTANCE;
	/**
	 * Hibernate <tt>class</tt> type.
	 * @deprecated Use {@link ClassType#INSTANCE} instead.
	 */
	public static final ClassType CLASS = ClassType.INSTANCE;
	/**
	 * Hibernate <tt>serializable</tt> type.
	 * @deprecated Use {@link SerializableType#INSTANCE} instead.
	 */
	public static final SerializableType SERIALIZABLE = SerializableType.INSTANCE;
	/**
	 * Hibernate <tt>object</tt> type.
	 * @deprecated Use {@link ObjectType#INSTANCE} instead.
	 */
	public static final ObjectType OBJECT = ObjectType.INSTANCE;

	/**
	 * A Hibernate <tt>serializable</tt> type.
	 * @deprecated Use {@link SerializableType#SerializableType} instead.
	 */
	public static Type serializable(Class serializableClass) {
		return new SerializableType( serializableClass );
	}

	/**
	 * A Hibernate <tt>any</tt> type.
	 *
	 * @param metaType       a type mapping <tt>java.lang.Class</tt> to a single column
	 * @param identifierType the entity identifier type
	 * @return the Type
	 */
	public static Type any(Type metaType, Type identifierType) {
		return new AnyType( metaType, identifierType );
	}

	/**
	 * A Hibernate persistent object (entity) type.
	 *
	 * @param persistentClass a mapped entity class
	 */
	public static Type entity(Class persistentClass) {
		// not really a many-to-one association *necessarily*
		return new ManyToOneType( persistentClass.getName() );
	}

	/**
	 * A Hibernate persistent object (entity) type.
	 *
	 * @param entityName a mapped entity class
	 */
	public static Type entity(String entityName) {
		// not really a many-to-one association *necessarily*
		return new ManyToOneType( entityName );
	}

	/**
	 * A Hibernate custom type.
	 *
	 * @param userTypeClass a class that implements <tt>UserType</tt>
	 */
	public static Type custom(Class userTypeClass) throws HibernateException {
		return custom( userTypeClass, null );
	}

	/**
	 * A Hibernate parameterizable custom type.
	 *
	 * @param userTypeClass   a class that implements <tt>UserType and ParameterizableType</tt>
	 * @param parameterNames  the names of the parameters passed to the type
	 * @param parameterValues the values of the parameters passed to the type. They must match
	 *                        up with the order and length of the parameterNames array.
	 */
	public static Type custom(Class userTypeClass, String[] parameterNames, String[] parameterValues)
			throws HibernateException {
		Properties parameters = new Properties();
		for ( int i = 0; i < parameterNames.length; i++ ) {
			parameters.setProperty( parameterNames[i], parameterValues[i] );
		}
		return custom( userTypeClass, parameters );
	}

	/**
	 * A Hibernate parameterizable custom type.
	 *
	 * @param userTypeClass a class that implements <tt>UserType and ParameterizableType</tt>
	 * @param parameters    the parameters as a collection of name/value pairs
	 */
	public static Type custom(Class userTypeClass, Properties parameters)
			throws HibernateException {
		if ( CompositeUserType.class.isAssignableFrom( userTypeClass ) ) {
			return TypeFactory.customComponent( userTypeClass, parameters );
		}
		else {
			return TypeFactory.custom( userTypeClass, parameters );
		}
	}

	/**
	 * Force initialization of a proxy or persistent collection.
	 * <p/>
	 * Note: This only ensures intialization of a proxy object or collection;
	 * it is not guaranteed that the elements INSIDE the collection will be initialized/materialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or <tt>null</tt>
	 * @throws HibernateException if we can't initialize the proxy at this time, eg. the <tt>Session</tt> was closed
	 */
	public static void initialize(Object proxy) throws HibernateException {
		if ( proxy == null ) {
			return;
		}
		else if ( proxy instanceof HibernateProxy ) {
			( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().initialize();
		}
		else if ( proxy instanceof PersistentCollection ) {
			( ( PersistentCollection ) proxy ).forceInitialization();
		}
	}

	/**
	 * Check if the proxy or persistent collection is initialized.
	 *
	 * @param proxy a persistable object, proxy, persistent collection or <tt>null</tt>
	 * @return true if the argument is already initialized, or is not a proxy or collection
	 */
	public static boolean isInitialized(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			return !( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().isUninitialized();
		}
		else if ( proxy instanceof PersistentCollection ) {
			return ( ( PersistentCollection ) proxy ).wasInitialized();
		}
		else {
			return true;
		}
	}

	/**
	 * Get the true, underlying class of a proxied persistent class. This operation
	 * will initialize a proxy by side-effect.
	 *
	 * @param proxy a persistable object or proxy
	 * @return the true class of the instance
	 * @throws HibernateException
	 */
	public static Class getClass(Object proxy) {
		if ( proxy instanceof HibernateProxy ) {
			return ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer()
					.getImplementation()
					.getClass();
		}
		else {
			return proxy.getClass();
		}
	}

	/**
	 * Create a new {@link Blob}. The returned object will be initially immutable.
	 *
	 * @param bytes a byte array
	 * @return the Blob
	 * @deprecated Use {@link #createBlob(byte[], Session)} instead
	 */
	public static Blob createBlob(byte[] bytes) {
		return NonContextualLobCreator.INSTANCE.wrap(
				NonContextualLobCreator.INSTANCE.createBlob( bytes )
		);
	}

	/**
	 * Create a new {@link Blob}.
	 *
	 * @param bytes a byte array
	 * @param session The session in which the {@link Blob} will be used.
	 * @return the Blob
	 */
	public static Blob createBlob(byte[] bytes, Session session) {
		// todo : wrap?
		return getLobCreator( session ).createBlob( bytes );
	}

	public static LobCreator getLobCreator(Session session) {
		return getLobCreator( ( SessionImplementor ) session );
	}

	public static LobCreator getLobCreator(SessionImplementor session) {
		return session.getFactory()
				.getSettings()
				.getJdbcSupport()
				.getLobCreator( ( LobCreationContext ) session );
	}

	/**
	 * Create a new {@link Blob}. The returned object will be initially immutable.
	 *
	 * @param stream a binary stream
	 * @param length the number of bytes in the stream
	 * @return the Blob
	 * @deprecated Use {@link #createBlob(InputStream, long, Session)} instead
	 */
	public static Blob createBlob(InputStream stream, int length) {
		return NonContextualLobCreator.INSTANCE.wrap(
				NonContextualLobCreator.INSTANCE.createBlob( stream, length )
		);
	}

	/**
	 * Create a new {@link Blob}. The returned object will be initially immutable.
	 *
	 * @param stream a binary stream
	 * @param length the number of bytes in the stream
	 * @return the Blob
	 * @deprecated Use {@link #createBlob(InputStream, long, Session)} instead
	 */
	public static Blob createBlob(InputStream stream, long length) {
		return NonContextualLobCreator.INSTANCE.wrap(
				NonContextualLobCreator.INSTANCE.createBlob( stream, length )
		);
	}

	/**
	 * Create a new {@link Blob}.
	 *
	 * @param stream a binary stream
	 * @param length the number of bytes in the stream
	 * @param session The session in which the {@link Blob} will be used.
	 * @return the Blob
	 */
	public static Blob createBlob(InputStream stream, long length, Session session) {
		// todo : wrap?
		return getLobCreator( session ).createBlob( stream, length );
	}

	/**
	 * Create a new {@link Blob}. The returned object will be initially immutable.
	 * <p/>
	 * NOTE: this method will read the entire contents of the incoming stream in order to properly
	 * handle the {@link Blob#length()} method.  If you do not want the stream read, use the
	 * {@link #createBlob(InputStream,long)} version instead.
	 *
	 * @param stream a binary stream
	 * @return the Blob
	 * @throws IOException Indicates an I/O problem accessing the stream
	 * @deprecated Use {@link #createBlob(InputStream, long, Session)} instead
	 */
	public static Blob createBlob(InputStream stream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream( stream.available() );
		StreamUtils.copy( stream, buffer );
		return createBlob( buffer.toByteArray() );
	}

	/**
	 * Create a new {@link Clob}. The returned object will be initially immutable.
	 *
	 * @param string The string data
	 * @return The created {@link Clob}
	 * @deprecated Use {@link #createClob(String, Session)} instead
	 */
	public static Clob createClob(String string) {
		return NonContextualLobCreator.INSTANCE.wrap(
				NonContextualLobCreator.INSTANCE.createClob( string )
		);
	}

	/**
	 * Create a new {@link Clob}.
	 *
	 * @param string The string data
	 * @param session The session in which the {@link Clob} will be used.
	 * @return The created {@link Clob}
	 */
	public static Clob createClob(String string, Session session) {
		// todo : wrap?
		return getLobCreator( session ).createClob( string );
	}

	/**
	 * Create a new {@link Clob}. The returned object will be initially immutable.
	 *
	 * @param reader a character stream
	 * @param length the number of characters in the stream
	 * @return The created {@link Clob}
	 * @deprecated Use {@link #createClob(Reader,long,Session)} instead
	 */
	public static Clob createClob(Reader reader, int length) {
		return NonContextualLobCreator.INSTANCE.wrap(
				NonContextualLobCreator.INSTANCE.createClob( reader, length )
		);
	}

	/**
	 * Create a new {@link Clob}. The returned object will be initially immutable.
	 *
	 * @param reader a character stream
	 * @param length the number of characters in the stream
	 * @return The created {@link Clob}
	 * @deprecated Use {@link #createClob(Reader,long,Session)} instead
	 */
	public static Clob createClob(Reader reader, long length) {
		return NonContextualLobCreator.INSTANCE.wrap(
				NonContextualLobCreator.INSTANCE.createClob( reader, length ) 
		);
	}

	/**
	 * Create a new {@link Clob}.
	 *
	 * @param reader a character stream
	 * @param length the number of characters in the stream
	 * @param session The session in which the {@link Clob} will be used.
	 * @return The created {@link Clob}
	 */
	public static Clob createClob(Reader reader, long length, Session session) {
		return getLobCreator( session ).createClob( reader, length );
	}

	/**
	 * Close an <tt>Iterator</tt> created by <tt>iterate()</tt> immediately,
	 * instead of waiting until the session is closed or disconnected.
	 *
	 * @param iterator an <tt>Iterator</tt> created by <tt>iterate()</tt>
	 * @throws HibernateException
	 * @see org.hibernate.Query#iterate
	 * @see Query#iterate()
	 */
	public static void close(Iterator iterator) throws HibernateException {
		if ( iterator instanceof HibernateIterator ) {
			( ( HibernateIterator ) iterator ).close();
		}
		else {
			throw new IllegalArgumentException( "not a Hibernate iterator" );
		}
	}

	/**
	 * Check if the property is initialized. If the named property does not exist
	 * or is not persistent, this method always returns <tt>true</tt>.
	 *
	 * @param proxy The potential proxy
	 * @param propertyName the name of a persistent attribute of the object
	 * @return true if the named property of the object is not listed as uninitialized; false otherwise
	 */
	public static boolean isPropertyInitialized(Object proxy, String propertyName) {
		
		Object entity;
		if ( proxy instanceof HibernateProxy ) {
			LazyInitializer li = ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				return false;
			}
			else {
				entity = li.getImplementation();
			}
		}
		else {
			entity = proxy;
		}

		if ( FieldInterceptionHelper.isInstrumented( entity ) ) {
			FieldInterceptor interceptor = FieldInterceptionHelper.extractFieldInterceptor( entity );
			return interceptor == null || interceptor.isInitialized( propertyName );
		}
		else {
			return true;
		}
		
	}

}
