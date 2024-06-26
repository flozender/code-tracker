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
package org.hibernate.cfg.annotations;

import java.io.Serializable;
import java.lang.reflect.TypeVariable;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Converts;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyTemporal;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.annotations.common.util.ReflectHelper;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3Column;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.Mappings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.PkDrivenByDefaultMapsIdSecondPass;
import org.hibernate.cfg.SetSimpleValueTypeSecondPass;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.CharacterArrayClobType;
import org.hibernate.type.CharacterArrayNClobType;
import org.hibernate.type.CharacterNCharType;
import org.hibernate.type.EnumType;
import org.hibernate.type.PrimitiveCharacterArrayClobType;
import org.hibernate.type.PrimitiveCharacterArrayNClobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.StringNVarcharType;
import org.hibernate.type.WrappedMaterializedBlobType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class SimpleValueBinder {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SimpleValueBinder.class.getName());

	private String propertyName;
	private String returnedClassName;
	private Ejb3Column[] columns;
	private String persistentClassName;
	private String explicitType = "";
	private String defaultType = "";
	private Properties typeParameters = new Properties();
	private Mappings mappings;
	private Table table;
	private SimpleValue simpleValue;
	private boolean isVersion;
	private String timeStampVersionType;
	//is a Map key
	private boolean key;
	private String referencedEntityName;
	private XProperty xproperty;
	private AccessType accessType;

	private AttributeConverterDefinition attributeConverterDefinition;

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public boolean isVersion() {
		return isVersion;
	}

	public void setVersion(boolean isVersion) {
		this.isVersion = isVersion;
	}

	public void setTimestampVersionType(String versionType) {
		this.timeStampVersionType = versionType;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;

		if ( defaultType.length() == 0 ) {
			defaultType = returnedClassName;
		}
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setColumns(Ejb3Column[] columns) {
		this.columns = columns;
	}


	public void setPersistentClassName(String persistentClassName) {
		this.persistentClassName = persistentClassName;
	}

	//TODO execute it lazily to be order safe

	public void setType(XProperty property, XClass returnedClass, String declaringClassName) {
		if ( returnedClass == null ) {
			// we cannot guess anything
			return;
		}
		XClass returnedClassOrElement = returnedClass;
                boolean isArray = false;
		if ( property.isArray() ) {
			returnedClassOrElement = property.getElementClass();
			isArray = true;
		}
		this.xproperty = property;
		Properties typeParameters = this.typeParameters;
		typeParameters.clear();
		String type = BinderHelper.ANNOTATION_STRING_DEFAULT;

		final boolean isNationalized = property.isAnnotationPresent( Nationalized.class )
				|| mappings.useNationalizedCharacterData();

		Type annType = property.getAnnotation( Type.class );
		if ( annType != null ) {
			setExplicitType( annType );
			type = explicitType;
		}
		else if ( ( !key && property.isAnnotationPresent( Temporal.class ) )
				|| ( key && property.isAnnotationPresent( MapKeyTemporal.class ) ) ) {

			boolean isDate;
			if ( mappings.getReflectionManager().equals( returnedClassOrElement, Date.class ) ) {
				isDate = true;
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Calendar.class ) ) {
				isDate = false;
			}
			else {
				throw new AnnotationException(
						"@Temporal should only be set on a java.util.Date or java.util.Calendar property: "
								+ StringHelper.qualify( persistentClassName, propertyName )
				);
			}
			final TemporalType temporalType = getTemporalType( property );
			switch ( temporalType ) {
				case DATE:
					type = isDate ? "date" : "calendar_date";
					break;
				case TIME:
					type = "time";
					if ( !isDate ) {
						throw new NotYetImplementedException(
								"Calendar cannot persist TIME only"
										+ StringHelper.qualify( persistentClassName, propertyName )
						);
					}
					break;
				case TIMESTAMP:
					type = isDate ? "timestamp" : "calendar";
					break;
				default:
					throw new AssertionFailure( "Unknown temporal type: " + temporalType );
			}
			explicitType = type;
		}
		else if ( property.isAnnotationPresent( Lob.class ) ) {
			if ( mappings.getReflectionManager().equals( returnedClassOrElement, java.sql.Clob.class ) ) {
				type = isNationalized
						? StandardBasicTypes.NCLOB.getName()
						: StandardBasicTypes.CLOB.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, java.sql.NClob.class ) ) {
				type = StandardBasicTypes.NCLOB.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, java.sql.Blob.class ) ) {
				type = "blob";
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, String.class ) ) {
				type = isNationalized
						? StandardBasicTypes.MATERIALIZED_NCLOB.getName()
						: StandardBasicTypes.MATERIALIZED_CLOB.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Character.class ) && isArray ) {
				type = isNationalized
						? CharacterArrayNClobType.class.getName()
						: CharacterArrayClobType.class.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, char.class ) && isArray ) {
				type = isNationalized
						? PrimitiveCharacterArrayNClobType.class.getName()
						: PrimitiveCharacterArrayClobType.class.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Byte.class ) && isArray ) {
				type = WrappedMaterializedBlobType.class.getName();
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, byte.class ) && isArray ) {
				type = StandardBasicTypes.MATERIALIZED_BLOB.getName();
			}
			else if ( mappings.getReflectionManager()
					.toXClass( Serializable.class )
					.isAssignableFrom( returnedClassOrElement ) ) {
				type = SerializableToBlobType.class.getName();
				typeParameters.setProperty(
						SerializableToBlobType.CLASS_NAME,
						returnedClassOrElement.getName()
				);
			}
			else {
				type = "blob";
			}
			explicitType = type;
		}
		else if ( ( !key && property.isAnnotationPresent( Enumerated.class ) )
				|| ( key && property.isAnnotationPresent( MapKeyEnumerated.class ) ) ) {
			final Class attributeJavaType = mappings.getReflectionManager().toClass( returnedClassOrElement );
			if ( !Enum.class.isAssignableFrom( attributeJavaType ) ) {
				throw new AnnotationException(
						String.format(
								"Attribute [%s.%s] was annotated as enumerated, but its java type is not an enum [%s]",
								declaringClassName,
								xproperty.getName(),
								attributeJavaType.getName()
						)
				);
			}
			type = EnumType.class.getName();
			explicitType = type;
		}
		else if ( isNationalized ) {
			if ( mappings.getReflectionManager().equals( returnedClassOrElement, String.class ) ) {
				// nvarchar
				type = StringNVarcharType.INSTANCE.getName();
				explicitType = type;
			}
			else if ( mappings.getReflectionManager().equals( returnedClassOrElement, Character.class ) ) {
				if ( isArray ) {
					// nvarchar
					type = StringNVarcharType.INSTANCE.getName();
				}
				else {
					// nchar
					type = CharacterNCharType.INSTANCE.getName();
				}
				explicitType = type;
			}
		}

		// implicit type will check basic types and Serializable classes
		if ( columns == null ) {
			throw new AssertionFailure( "SimpleValueBinder.setColumns should be set before SimpleValueBinder.setType" );
		}

		if ( BinderHelper.ANNOTATION_STRING_DEFAULT.equals( type ) ) {
			if ( returnedClassOrElement.isEnum() ) {
				type = EnumType.class.getName();
			}
		}

		defaultType = BinderHelper.isEmptyAnnotationValue( type ) ? returnedClassName : type;
		this.typeParameters = typeParameters;

		applyAttributeConverter( property );
	}

	private void applyAttributeConverter(XProperty property) {
		if ( attributeConverterDefinition != null ) {
			return;
		}

		LOG.debugf( "Starting applyAttributeConverter [%s:%s]", persistentClassName, property.getName() );

		if ( property.isAnnotationPresent( Id.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Id attribute [%s]", property.getName() );
			return;
		}

		if ( isVersion ) {
			LOG.debugf( "Skipping AttributeConverter checks for version attribute [%s]", property.getName() );
			return;
		}

		if ( property.isAnnotationPresent( Temporal.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Temporal attribute [%s]", property.getName() );
			return;
		}

		if ( property.isAnnotationPresent( Enumerated.class ) ) {
			LOG.debugf( "Skipping AttributeConverter checks for Enumerated attribute [%s]", property.getName() );
			return;
		}

		if ( isAssociation() ) {
			LOG.debugf( "Skipping AttributeConverter checks for association attribute [%s]", property.getName() );
			return;
		}

		// @Convert annotations take precedence if present
		final Convert convertAnnotation = locateConvertAnnotation( property );
		if ( convertAnnotation != null ) {
			LOG.debugf(
					"Applying located @Convert AttributeConverter [%s] to attribute [%]",
					convertAnnotation.converter().getName(),
					property.getName()
			);
			attributeConverterDefinition = mappings.locateAttributeConverter( convertAnnotation.converter() );
		}
		else {
			attributeConverterDefinition = locateAutoApplyAttributeConverter( property );
		}
	}

	private AttributeConverterDefinition locateAutoApplyAttributeConverter(XProperty property) {
		final Class propertyType = mappings.getReflectionManager().toClass( property.getType() );
		for ( AttributeConverterDefinition attributeConverterDefinition : mappings.getAttributeConverters() ) {
			if ( ! attributeConverterDefinition.isAutoApply() ) {
				continue;
			}
			if ( areTypeMatch( attributeConverterDefinition.getEntityAttributeType(), propertyType ) ) {
				return attributeConverterDefinition;
			}
		}
		return null;
	}

	private boolean isAssociation() {
		// todo : this information is only known to caller(s), need to pass that information in somehow.
		// or, is this enough?
		return referencedEntityName != null;
	}

	@SuppressWarnings("unchecked")
	private Convert locateConvertAnnotation(XProperty property) {
		LOG.debugf(
				"Attempting to locate Convert annotation for property [%s:%s]",
				persistentClassName,
				property.getName()
		);

		// first look locally on the property for @Convert/@Converts
		{
			Convert localConvertAnnotation = property.getAnnotation( Convert.class );
			if ( localConvertAnnotation != null ) {
				LOG.debugf(
						"Found matching local @Convert annotation [disableConversion=%s]",
						localConvertAnnotation.disableConversion()
				);
				return localConvertAnnotation.disableConversion()
						? null
						: localConvertAnnotation;
			}
		}

		{
			Converts localConvertsAnnotation = property.getAnnotation( Converts.class );
			if ( localConvertsAnnotation != null ) {
				for ( Convert localConvertAnnotation : localConvertsAnnotation.value() ) {
					if ( isLocalMatch( localConvertAnnotation, property ) ) {
						LOG.debugf(
								"Found matching @Convert annotation as part local @Converts [disableConversion=%s]",
								localConvertAnnotation.disableConversion()
						);
						return localConvertAnnotation.disableConversion()
								? null
								: localConvertAnnotation;
					}
				}
			}
		}

		if ( persistentClassName == null ) {
			LOG.debug( "Persistent Class name not known during attempt to locate @Convert annotations" );
			return null;
		}

		final XClass owner;
		try {
			final Class ownerClass = ReflectHelper.classForName( persistentClassName );
			owner = mappings.getReflectionManager().classForName( persistentClassName, ownerClass  );
		}
		catch (ClassNotFoundException e) {
			throw new AnnotationException( "Unable to resolve Class reference during attempt to locate @Convert annotations" );
		}

		return lookForEntityDefinedConvertAnnotation( property, owner );
	}

	private Convert lookForEntityDefinedConvertAnnotation(XProperty property, XClass owner) {
		if ( owner == null ) {
			// we have hit the root of the entity hierarchy
			return null;
		}

		LOG.debugf(
				"Attempting to locate any AttributeConverter defined via @Convert/@Converts on type-hierarchy [%s] to apply to attribute [%s]",
				owner.getName(),
				property.getName()
		);

		{
			Convert convertAnnotation = owner.getAnnotation( Convert.class );
			if ( convertAnnotation != null && isMatch( convertAnnotation, property ) ) {
				LOG.debugf(
						"Found matching @Convert annotation [disableConversion=%s]",
						convertAnnotation.disableConversion()
				);
				return convertAnnotation.disableConversion() ? null : convertAnnotation;
			}
		}

		{
			Converts convertsAnnotation = owner.getAnnotation( Converts.class );
			if ( convertsAnnotation != null ) {
				for ( Convert convertAnnotation : convertsAnnotation.value() ) {
					if ( isMatch( convertAnnotation, property ) ) {
						LOG.debugf(
								"Found matching @Convert annotation as part @Converts [disableConversion=%s]",
								convertAnnotation.disableConversion()
						);
						return convertAnnotation.disableConversion() ? null : convertAnnotation;
					}
				}
			}
		}

		// finally, look on superclass
		return lookForEntityDefinedConvertAnnotation( property, owner.getSuperclass() );
	}

	@SuppressWarnings("unchecked")
	private boolean isLocalMatch(Convert convertAnnotation, XProperty property) {
		if ( StringHelper.isEmpty( convertAnnotation.attributeName() ) ) {
			return isTypeMatch( convertAnnotation.converter(), property );
		}

		return property.getName().equals( convertAnnotation.attributeName() )
				&& isTypeMatch( convertAnnotation.converter(), property );
	}

	@SuppressWarnings("unchecked")
	private boolean isMatch(Convert convertAnnotation, XProperty property) {
		return property.getName().equals( convertAnnotation.attributeName() );
//		return property.getName().equals( convertAnnotation.attributeName() )
//				&& isTypeMatch( convertAnnotation.converter(), property );
	}

	private boolean isTypeMatch(Class<? extends AttributeConverter> attributeConverterClass, XProperty property) {
		return areTypeMatch(
				extractEntityAttributeType( attributeConverterClass ),
				mappings.getReflectionManager().toClass( property.getType() )
		);
	}

	private Class extractEntityAttributeType(Class<? extends AttributeConverter> attributeConverterClass) {
		// this is duplicated in SimpleValue...
		final TypeVariable[] attributeConverterTypeInformation = attributeConverterClass.getTypeParameters();
		if ( attributeConverterTypeInformation == null || attributeConverterTypeInformation.length < 2 ) {
			throw new AnnotationException(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] did not retain parameterized type information"
			);
		}

		if ( attributeConverterTypeInformation.length > 2 ) {
			LOG.debug(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] specified more than 2 parameterized types"
			);
		}
		final Class entityAttributeJavaType = extractType( attributeConverterTypeInformation[0] );
		if ( entityAttributeJavaType == null ) {
			throw new AnnotationException(
					"Could not determine 'entity attribute' type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}
		return entityAttributeJavaType;
	}

	private Class extractType(TypeVariable typeVariable) {
		java.lang.reflect.Type[] boundTypes = typeVariable.getBounds();
		if ( boundTypes == null || boundTypes.length != 1 ) {
			return null;
		}

		return (Class) boundTypes[0];
	}

	private boolean areTypeMatch(Class converterDefinedType, Class propertyType) {
		if ( converterDefinedType == null ) {
			throw new AnnotationException( "AttributeConverter defined java type cannot be null" );
		}
		if ( propertyType == null ) {
			throw new AnnotationException( "Property defined java type cannot be null" );
		}

		return converterDefinedType.equals( propertyType )
				|| arePrimitiveWrapperEquivalents( converterDefinedType, propertyType );
	}

	private boolean arePrimitiveWrapperEquivalents(Class converterDefinedType, Class propertyType) {
		if ( converterDefinedType.isPrimitive() ) {
			return getWrapperEquivalent( converterDefinedType ).equals( propertyType );
		}
		else if ( propertyType.isPrimitive() ) {
			return getWrapperEquivalent( propertyType ).equals( converterDefinedType );
		}
		return false;
	}

	private static Class getWrapperEquivalent(Class primitive) {
		if ( ! primitive.isPrimitive() ) {
			throw new AssertionFailure( "Passed type for which to locate wrapper equivalent was not a primitive" );
		}

		if ( boolean.class.equals( primitive ) ) {
			return Boolean.class;
		}
		else if ( char.class.equals( primitive ) ) {
			return Character.class;
		}
		else if ( byte.class.equals( primitive ) ) {
			return Byte.class;
		}
		else if ( short.class.equals( primitive ) ) {
			return Short.class;
		}
		else if ( int.class.equals( primitive ) ) {
			return Integer.class;
		}
		else if ( long.class.equals( primitive ) ) {
			return Long.class;
		}
		else if ( float.class.equals( primitive ) ) {
			return Float.class;
		}
		else if ( double.class.equals( primitive ) ) {
			return Double.class;
		}

		throw new AssertionFailure( "Unexpected primitive type (VOID most likely) passed to getWrapperEquivalent" );
	}
	
	private TemporalType getTemporalType(XProperty property) {
		if ( key ) {
			MapKeyTemporal ann = property.getAnnotation( MapKeyTemporal.class );
			return ann.value();
		}
		else {
			Temporal ann = property.getAnnotation( Temporal.class );
			return ann.value();
		}
	}

	public void setExplicitType(String explicitType) {
		this.explicitType = explicitType;
	}

	//FIXME raise an assertion failure  if setResolvedTypeMapping(String) and setResolvedTypeMapping(Type) are use at the same time

	public void setExplicitType(Type typeAnn) {
		if ( typeAnn != null ) {
			explicitType = typeAnn.type();
			typeParameters.clear();
			for ( Parameter param : typeAnn.parameters() ) {
				typeParameters.setProperty( param.name(), param.value() );
			}
		}
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	private void validate() {
		//TODO check necessary params
		Ejb3Column.checkPropertyConsistency( columns, propertyName );
	}

	public SimpleValue make() {

		validate();
		LOG.debugf( "building SimpleValue for %s", propertyName );
		if ( table == null ) {
			table = columns[0].getTable();
		}
		simpleValue = new SimpleValue( mappings, table );

		linkWithValue();

		boolean isInSecondPass = mappings.isInSecondPass();
		SetSimpleValueTypeSecondPass secondPass = new SetSimpleValueTypeSecondPass( this );
		if ( !isInSecondPass ) {
			//Defer this to the second pass
			mappings.addSecondPass( secondPass );
		}
		else {
			//We are already in second pass
			fillSimpleValue();
		}
		return simpleValue;
	}

	public void linkWithValue() {
		if ( columns[0].isNameDeferred() && !mappings.isInSecondPass() && referencedEntityName != null ) {
			mappings.addSecondPass(
					new PkDrivenByDefaultMapsIdSecondPass(
							referencedEntityName, ( Ejb3JoinColumn[] ) columns, simpleValue
					)
			);
		}
		else {
			for ( Ejb3Column column : columns ) {
				column.linkWithValue( simpleValue );
			}
		}
	}

	public void fillSimpleValue() {
		LOG.debugf( "Starting fillSimpleValue for %s", propertyName );
                
		if ( attributeConverterDefinition != null ) {
			if ( ! BinderHelper.isEmptyAnnotationValue( explicitType ) ) {
				throw new AnnotationException(
						String.format(
								"AttributeConverter and explicit Type cannot be applied to same attribute [%s.%s];" +
										"remove @Type or specify @Convert(disableConversion = true)",
								persistentClassName,
								propertyName
						)
				);
			}
			LOG.debugf(
					"Applying JPA AttributeConverter [%s] to [%s:%s]",
					attributeConverterDefinition,
					persistentClassName,
					propertyName
			);
			simpleValue.setJpaAttributeConverterDefinition( attributeConverterDefinition );
		}
		else {
			String type;
			org.hibernate.mapping.TypeDef typeDef;

			if ( !BinderHelper.isEmptyAnnotationValue( explicitType ) ) {
				type = explicitType;
				typeDef = mappings.getTypeDef( type );
			}
			else {
				// try implicit type
				org.hibernate.mapping.TypeDef implicitTypeDef = mappings.getTypeDef( returnedClassName );
				if ( implicitTypeDef != null ) {
					typeDef = implicitTypeDef;
					type = returnedClassName;
				}
				else {
					typeDef = mappings.getTypeDef( defaultType );
					type = defaultType;
				}
			}

			if ( typeDef != null ) {
				type = typeDef.getTypeClass();
				simpleValue.setTypeParameters( typeDef.getParameters() );
			}
			if ( typeParameters != null && typeParameters.size() != 0 ) {
				//explicit type params takes precedence over type def params
				simpleValue.setTypeParameters( typeParameters );
			}
			simpleValue.setTypeName( type );
		}

		if ( persistentClassName != null || attributeConverterDefinition != null ) {
			simpleValue.setTypeUsingReflection( persistentClassName, propertyName );
		}

		if ( !simpleValue.isTypeSpecified() && isVersion() ) {
			simpleValue.setTypeName( "integer" );
		}

		// HHH-5205
		if ( timeStampVersionType != null ) {
			simpleValue.setTypeName( timeStampVersionType );
		}
		
		if ( simpleValue.getTypeName() != null && simpleValue.getTypeName().length() > 0
				&& simpleValue.getMappings().getTypeResolver().basic( simpleValue.getTypeName() ) == null ) {
			try {
				Class typeClass = ReflectHelper.classForName( simpleValue.getTypeName() );

				if ( typeClass != null && DynamicParameterizedType.class.isAssignableFrom( typeClass ) ) {
					Properties parameters = simpleValue.getTypeParameters();
					if ( parameters == null ) {
						parameters = new Properties();
					}
					parameters.put( DynamicParameterizedType.IS_DYNAMIC, Boolean.toString( true ) );
					parameters.put( DynamicParameterizedType.RETURNED_CLASS, returnedClassName );
					parameters.put( DynamicParameterizedType.IS_PRIMARY_KEY, Boolean.toString( key ) );

					parameters.put( DynamicParameterizedType.ENTITY, persistentClassName );
					parameters.put( DynamicParameterizedType.XPROPERTY, xproperty );
					parameters.put( DynamicParameterizedType.PROPERTY, xproperty.getName() );
					parameters.put( DynamicParameterizedType.ACCESS_TYPE, accessType.getType() );
					simpleValue.setTypeParameters( parameters );
				}
			}
			catch ( ClassNotFoundException cnfe ) {
				throw new MappingException( "Could not determine type for: " + simpleValue.getTypeName(), cnfe );
			}
		}

	}

	public void setKey(boolean key) {
		this.key = key;
	}

	public AccessType getAccessType() {
		return accessType;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}
}
