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
package org.hibernate.type.descriptor.java;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

import org.hibernate.util.SerializationHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class SerializableTypeDescriptor<T extends Serializable> extends AbstractTypeDescriptor<T> {
	// unfortunately the param types cannot be the same so use something other than 'T' here to make that obvious
	public static class SerializableMutabilityPlan<S extends Serializable> extends MutableMutabilityPlan<S> {
		public static final SerializableMutabilityPlan INSTANCE = new SerializableMutabilityPlan();
		@SuppressWarnings({ "unchecked" })
		public S deepCopyNotNull(S value) {
			return (S) SerializationHelper.clone( value );
		}
	}

	@SuppressWarnings({ "unchecked" })
	public SerializableTypeDescriptor(Class<T> type) {
		super( type, SerializableMutabilityPlan.INSTANCE );
	}

	public String toString(T value) {
		return PrimitiveByteArrayTypeDescriptor.INSTANCE.toString( toBytes( value ) );
	}

	public T fromString(String string) {
		return fromBytes( PrimitiveByteArrayTypeDescriptor.INSTANCE.fromString( string ) );
	}

	@Override
	public boolean areEqual(T one, T another) {
		if ( one == another ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}
		return one.equals( another )
				|| PrimitiveByteArrayTypeDescriptor.INSTANCE.areEqual( toBytes( one ), toBytes( another ) );
	}

	@Override
	public int extractHashCode(T value) {
		return PrimitiveByteArrayTypeDescriptor.INSTANCE.extractHashCode( toBytes( value ) );
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) toBytes( value );
		}
		if ( InputStream.class.isAssignableFrom( type ) ) {
			return (X) new ByteArrayInputStream( toBytes( value ) );
		}
		throw unknownUnwrap( type );
	}

	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( byte[].class.isInstance( value ) ) {
			return fromBytes( (byte[]) value );
		}
		if ( InputStream.class.isInstance( value ) ) {
			return fromBytes( DataHelper.extractBytes( (InputStream) value ) );
		}
		throw unknownWrap( value.getClass() );
	}

	protected byte[] toBytes(T value) {
		return SerializationHelper.serialize( value );
	}

	@SuppressWarnings({ "unchecked" })
	protected T fromBytes(byte[] bytes) {
		return (T) SerializationHelper.deserialize( bytes, getJavaTypeClass().getClassLoader() );
	}
}
