/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type.descriptor.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Adapter for incorporating JPA {@link AttributeConverter} handling into the SqlTypeDescriptor contract.
 * <p/>
 * Essentially this is responsible for mapping to/from the intermediate database type representation.  Continuing the
 * {@code AttributeConverter<Integer,String>} example from
 * {@link org.hibernate.mapping.SimpleValue#buildAttributeConverterTypeAdapter()}, the "intermediate database type
 * representation" would be the String representation.  So on binding, we convert the incoming Integer to String;
 * on extraction we extract the value as String and convert to Integer.
 *
 * @author Steve Ebersole
 */
public class AttributeConverterSqlTypeDescriptorAdapter implements SqlTypeDescriptor {
	private static final Logger log = Logger.getLogger( AttributeConverterSqlTypeDescriptorAdapter.class );

	private final AttributeConverter converter;
	private final SqlTypeDescriptor delegate;
	private final JavaTypeDescriptor intermediateJavaTypeDescriptor;

	public AttributeConverterSqlTypeDescriptorAdapter(
			AttributeConverter converter,
			SqlTypeDescriptor delegate,
			JavaTypeDescriptor intermediateJavaTypeDescriptor) {
		this.converter = converter;
		this.delegate = delegate;
		this.intermediateJavaTypeDescriptor = intermediateJavaTypeDescriptor;
	}

	@Override
	public int getSqlType() {
		return delegate.getSqlType();
	}

	@Override
	public boolean canBeRemapped() {
		// todo : consider the ramifications of this.
		// certainly we need to account for the remapping of the delegate sql-type, but is it really valid to
		// allow remapping of the converter sql-type?
		return delegate.canBeRemapped();
	}


	// Binding ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings("unchecked")
	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		// Get the binder for the intermediate type representation
		final ValueBinder realBinder = delegate.getBinder( intermediateJavaTypeDescriptor );

		return new ValueBinder<X>() {
			@Override
			public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				final Object convertedValue;
				try {
					convertedValue = converter.convertToDatabaseColumn( value );
				}
				catch (PersistenceException pe) {
					throw pe;
				}
				catch (RuntimeException re) {
					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
				}

				log.debugf( "Converted value on binding : %s -> %s", value, convertedValue );
				realBinder.bind( st, convertedValue, index, options );
			}
		};
	}


	// Extraction ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		// Get the extractor for the intermediate type representation
		final ValueExtractor realExtractor = delegate.getExtractor( intermediateJavaTypeDescriptor );

		return new ValueExtractor<X>() {
			@Override
			public X extract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return doConversion( realExtractor.extract( rs, name, options ) );
			}

			@Override
			public X extract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return doConversion( realExtractor.extract( statement, index, options ) );
			}

			@Override
			public X extract(CallableStatement statement, String[] paramNames, WrapperOptions options) throws SQLException {
				if ( paramNames.length > 1 ) {
					throw new IllegalArgumentException( "Basic value extraction cannot handle multiple output parameters" );
				}
				return doConversion( realExtractor.extract( statement, paramNames, options ) );
			}

			@SuppressWarnings("unchecked")
			private X doConversion(Object extractedValue) {
				try {
					X convertedValue = (X) converter.convertToEntityAttribute( extractedValue );
					log.debugf( "Converted value on extraction: %s -> %s", extractedValue, convertedValue );
					return convertedValue;
				}
				catch (PersistenceException pe) {
					throw pe;
				}
				catch (RuntimeException re) {
					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
				}
			}
		};
	}
}
