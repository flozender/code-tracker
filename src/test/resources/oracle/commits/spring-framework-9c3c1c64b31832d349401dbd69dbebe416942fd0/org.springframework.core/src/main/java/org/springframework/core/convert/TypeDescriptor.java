/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Context about a type to convert from or to.
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypeDescriptor {

	/** Constant defining a TypeDescriptor for a <code>null</code> value */
	public static final TypeDescriptor NULL = new TypeDescriptor();

	private static final Map<Class<?>, TypeDescriptor> typeDescriptorCache = new HashMap<Class<?>, TypeDescriptor>();

	static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

	static {
		typeDescriptorCache.put(boolean.class, new TypeDescriptor(boolean.class));
		typeDescriptorCache.put(Boolean.class, new TypeDescriptor(Boolean.class));
		typeDescriptorCache.put(byte.class, new TypeDescriptor(byte.class));
		typeDescriptorCache.put(Byte.class, new TypeDescriptor(Byte.class));
		typeDescriptorCache.put(char.class, new TypeDescriptor(char.class));
		typeDescriptorCache.put(Character.class, new TypeDescriptor(Character.class));
		typeDescriptorCache.put(short.class, new TypeDescriptor(short.class));
		typeDescriptorCache.put(Short.class, new TypeDescriptor(Short.class));
		typeDescriptorCache.put(int.class, new TypeDescriptor(int.class));
		typeDescriptorCache.put(Integer.class, new TypeDescriptor(Integer.class));
		typeDescriptorCache.put(long.class, new TypeDescriptor(long.class));
		typeDescriptorCache.put(Long.class, new TypeDescriptor(Long.class));
		typeDescriptorCache.put(float.class, new TypeDescriptor(float.class));
		typeDescriptorCache.put(Float.class, new TypeDescriptor(Float.class));
		typeDescriptorCache.put(double.class, new TypeDescriptor(double.class));
		typeDescriptorCache.put(Double.class, new TypeDescriptor(Double.class));
		typeDescriptorCache.put(String.class, new TypeDescriptor(String.class));
	}


	private final Class<?> type;

	private final TypeDescriptor elementType;

	private final TypeDescriptor mapKeyType;

	private final TypeDescriptor mapValueType;

	private final Annotation[] annotations;

	/**
	 * Create a new type descriptor from a {@link MethodParameter}.
	 * Use this constructor when a conversion point is a constructor parameter, method parameter, or method return value. 
	 * @param methodParameter the method parameter
	 */
	public TypeDescriptor(MethodParameter methodParameter) {
		this(new ParameterDescriptor(methodParameter));
	}

	/**
	 * Create a new type descriptor for a field.
	 * Use this constructor when a conversion point is a field.
	 * @param field the field
	 */
	public TypeDescriptor(Field field) {
		this(new FieldDescriptor(field));
	}

	/**
	 * Create a new type descriptor for a bean property.
	 * Use this constructor when a target conversion point is a property on a Java class.
	 * @param beanClass the class that declares the property
	 * @param property the property descriptor
	 */
	public TypeDescriptor(Class<?> beanClass, PropertyDescriptor property) {
		this(new BeanPropertyDescriptor(beanClass, property));
	}

	/**
	 * Create a new type descriptor for the given class.
	 * Use this to instruct the conversion system to convert to an object to a specific target type, when no type location such as a method parameter or field is available to provide additional conversion context.
	 * Generally prefer use of {@link #forObject(Object)} for constructing source type descriptors for source objects.
	 * @param type the class
	 * @return the type descriptor
	 */
	public static TypeDescriptor valueOf(Class<?> type) {
		if (type == null) {
			return NULL;
		}
		TypeDescriptor desc = typeDescriptorCache.get(type);
		return (desc != null ? desc : new TypeDescriptor(type));
	}

	/**
	 * Create a new type descriptor for a java.util.Collection class.
	 * Useful for supporting conversion of source Collection objects to other types.
	 * Serves as an alternative to {@link #forObject(Object)} to be used when you cannot rely on Collection element introspection to resolve the element type.
	 * @param collectionType the collection type, which must implement {@link Collection}.
	 * @param elementType the collection's element type, used to convert collection elements
	 * @return the collection type descriptor
	 */
	public static TypeDescriptor collection(Class<?> collectionType, TypeDescriptor elementType) {
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException("collectionType must be a java.util.Collection");
		}
		return new TypeDescriptor(collectionType, elementType);
	}

	/**
	 * Create a new type descriptor for a java.util.Map class.
	 * Useful for supporting the conversion of source Map objects to other types.
	 * Serves as an alternative to {@link #forObject(Object)} to be used when you cannot rely on Map entry introspection to resolve the key and value type.
	 * @param mapType the map type, which must implement {@link Map}.
	 * @param keyType the map's key type, used to convert map keys
	 * @param valueType the map's value type, used to convert map values
	 * @return the map type descriptor
	 */
	public static TypeDescriptor map(Class<?> mapType, TypeDescriptor keyType, TypeDescriptor valueType) {
		if (!Map.class.isAssignableFrom(mapType)) {
			throw new IllegalArgumentException("mapType must be a java.util.Map");
		}
		return new TypeDescriptor(mapType, keyType, valueType);
	}
	
	/**
	 * Create a new type descriptor for an object.
	 * Use this factory method to introspect a source object's type before asking the conversion system to convert it to some another type.
	 * Populates nested type descriptors for collection and map objects through object introspection.
	 * If the provided object is null, returns {@link TypeDescriptor#NULL}.
	 * If the object is not a collection or map, simply calls {@link #valueOf(Class)}.
	 * If the object is a collection or map, this factory method will derive nested element or key/value types by introspecting the collection or map.
	 * The introspection algorithm derives nested element or key/value types by resolving the "common element type" across the collection or map.
	 * For example, if a Collection contained all java.lang.Integer elements, its element type would be java.lang.Integer.
	 * If a Collection contained several distinct number types all extending from java.lang.Number, its element type would be java.lang.Number.
	 * If a Collection contained a String and a java.util.Map element, its element type would be java.io.Serializable.
	 * @param object the source object
	 * @return the type descriptor
	 * @see ConversionService#convert(Object, Class)
	 */
	public static TypeDescriptor forObject(Object object) {
		if (object == null) {
			return NULL;
		}
		if (object instanceof Collection<?>) {
			return new TypeDescriptor(object.getClass(), CommonElement.typeDescriptor((Collection<?>) object));
		}
		else if (object instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) object;
			return new TypeDescriptor(map.getClass(), CommonElement.typeDescriptor(map.keySet()), CommonElement.typeDescriptor(map.values()));
		}
		else {
			return valueOf(object.getClass());
		}
	}

	/**
	 * Creates a type descriptor for a nested type declared within the method parameter.
	 * For example, if the methodParameter is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the methodParameter is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class.
	 * If the methodParameter is a Map<Integer, String> and the nesting level is 1, the nested type descriptor will be String, derived from the map value.
	 * If the methodParameter is a List<Map<Integer, String>> and the nesting level is 2, the nested type descriptor will be String, derived from the map value.
	 * @param methodParameter the method parameter with a nestingLevel of 1
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the method parameter.
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the method parameter is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(MethodParameter methodParameter, int nestingLevel) {
		return nested(new ParameterDescriptor(methodParameter), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the field.
	 * For example, if the field is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the field is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class. 
	 * If the field is a Map<Integer, String> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value. 
	 * If the field is a List<Map<Integer, String>> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * @param field the field
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the field.
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the field is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(Field field, int nestingLevel) {
		return nested(new FieldDescriptor(field), nestingLevel);
	}

	/**
	 * Creates a type descriptor for a nested type declared within the property.
	 * For example, if the property is a List&lt;String&gt; and the nestingLevel is 1, the nested type descriptor will be String.class.
	 * If the property is a List<List<String>> and the nestingLevel is 2, the nested type descriptor will also be a String.class. 
	 * If the field is a Map<Integer, String> and the nestingLevel is 1, the nested type descriptor will be String, derived from the map value. 
	 * If the property is a List<Map<Integer, String>> and the nestingLevel is 2, the nested type descriptor will be String, derived from the map value.
	 * @param property the property
	 * @param nestingLevel the nesting level of the collection/array element or map key/value declaration within the property.
	 * @return the nested type descriptor
	 * @throws IllegalArgumentException if the property is not of a collection, array, or map type.
	 */
	public static TypeDescriptor nested(Class<?> beanClass, PropertyDescriptor property, int nestingLevel) {
		return nested(new BeanPropertyDescriptor(beanClass, property), nestingLevel);
	}

	/**
	 * Determine the declared (non-generic) type of the wrapped parameter/field.
	 * @return the declared type, or <code>null</code> if this is {@link TypeDescriptor#NULL}
	 */
	public Class<?> getType() {
		return type;
	}

	/**
	 * Determine the declared type of the wrapped parameter/field.
	 * Returns the Object wrapper type if the underlying type is a primitive.
	 */
	public Class<?> getObjectType() {
		return ClassUtils.resolvePrimitiveIfNecessary(getType());
	}

	/**
	 * Returns the name of this type: the fully qualified class name.
	 */
	public String getName() {
		return ClassUtils.getQualifiedName(getType());
	}

	/**
	 * Is this type a primitive type?
	 */
	public boolean isPrimitive() {
		return getType().isPrimitive();
	}

	/**
	 * The annotations associated with this type descriptor, if any.
	 * @return the annotations, or an empty array if none.
	 */
	public Annotation[] getAnnotations() {
		return this.annotations;
	}

	/**
	 * Obtain the annotation associated with this type descriptor of the specified type.
	 * @return the annotation, or null if no such annotation exists on this type descriptor.
	 */
	public Annotation getAnnotation(Class<? extends Annotation> annotationType) {
		for (Annotation annotation : getAnnotations()) {
			if (annotation.annotationType().equals(annotationType)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Returns true if an object of this type can be assigned to a reference of given targetType.
	 * @param targetType the target type
	 * @return true if this type is assignable to the target
	 */
	public boolean isAssignableTo(TypeDescriptor targetType) {
		if (this == TypeDescriptor.NULL || targetType == TypeDescriptor.NULL) {
			return true;
		}
		if (isCollection() && targetType.isCollection() || isArray() && targetType.isArray()) {
			return targetType.getType().isAssignableFrom(getType()) &&
					getElementTypeDescriptor().isAssignableTo(targetType.getElementTypeDescriptor());
		}
		else if (isMap() && targetType.isMap()) {
			return targetType.getType().isAssignableFrom(getType()) &&
					getMapKeyTypeDescriptor().isAssignableTo(targetType.getMapKeyTypeDescriptor()) &&
					getMapValueTypeDescriptor().isAssignableTo(targetType.getMapValueTypeDescriptor());
		}
		else {
			return targetType.getObjectType().isAssignableFrom(getObjectType());
		}
	}

	// indexable type descriptor operations
	
	/**
	 * Is this type a {@link Collection} type?
	 */
	public boolean isCollection() {
		return Collection.class.isAssignableFrom(getType());
	}

	/**
	 * Is this type an array type?
	 */
	public boolean isArray() {
		return getType().isArray();
	}

	/**
	 * If this type is a {@link Collection} or array, returns the underlying element type.
	 * Returns <code>null</code> if this type is neither an array or collection.
	 * Returns Object.class if this type is a collection and the element type was not explicitly declared.
	 * @return the map element type, or <code>null</code> if not a collection or array.
	 */
	public Class<?> getElementType() {
		return getElementTypeDescriptor().getType();
	}

	/**
	 * The collection or array element type as a type descriptor.
	 * Returns {@link TypeDescriptor#NULL} if this type is not a collection or an array.
	 * Returns TypeDescriptor.valueOf(Object.class) if this type is a collection and the element type is not explicitly declared.
	 */
	public TypeDescriptor getElementTypeDescriptor() {
		return this.elementType;
	}

	// map type descriptor operations
	
	/**
	 * Is this type a {@link Map} type?
	 */
	public boolean isMap() {
		return Map.class.isAssignableFrom(getType());
	}

	/**
	 * If this type is a {@link Map}, returns the underlying key type.
	 * Returns <code>null</code> if this type is not map.
	 * Returns Object.class if this type is a map and its key type was not explicitly declared.
	 * @return the map key type, or <code>null</code> if not a map.
	 */
	public Class<?> getMapKeyType() {
		return getMapKeyTypeDescriptor().getType();
	}

	/**
	 * The map key type as a type descriptor.
	 * Returns {@link TypeDescriptor#NULL} if this type is not a map.
	 * Returns TypeDescriptor.valueOf(Object.class) if this type is a map and the key type is not explicitly declared.
	 */
	public TypeDescriptor getMapKeyTypeDescriptor() {
		return this.mapKeyType;
	}

	/**
	 * If this type is a {@link Map}, returns the underlying value type.
	 * Returns <code>null</code> if this type is not map.
	 * Returns Object.class if this type is a map and its value type was not explicitly declared.
	 * @return the map value type, or <code>null</code> if not a map.
	 */
	public Class<?> getMapValueType() {
		return getMapValueTypeDescriptor().getType();
	}

	/**
	 * The map value type as a type descriptor.
	 * Returns {@link TypeDescriptor#NULL} if this type is not a map.
	 * Returns TypeDescriptor.valueOf(Object.class) if this type is a map and the value type is not explicitly declared.
	 */
	public TypeDescriptor getMapValueTypeDescriptor() {
		return this.mapValueType;
	}
	
	// extending Object
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TypeDescriptor) || obj == TypeDescriptor.NULL) {
			return false;
		}
		TypeDescriptor other = (TypeDescriptor) obj;
		boolean annotatedTypeEquals = getType().equals(other.getType()) && ObjectUtils.nullSafeEquals(getAnnotations(), other.getAnnotations());
		if (isCollection()) {
			return annotatedTypeEquals && ObjectUtils.nullSafeEquals(getElementType(), other.getElementType());
		}
		else if (isMap()) {
			return annotatedTypeEquals && ObjectUtils.nullSafeEquals(getMapKeyType(), other.getMapKeyType()) &&
					ObjectUtils.nullSafeEquals(getMapValueType(), other.getMapValueType());
		}
		else {
			return annotatedTypeEquals;
		}
	}

	public int hashCode() {
		return (this == TypeDescriptor.NULL ? 0 : getType().hashCode());
	}

	public String toString() {
		if (this == TypeDescriptor.NULL) {
			return "null";
		}
		else {
			StringBuilder builder = new StringBuilder();
			Annotation[] anns = getAnnotations();
			for (Annotation ann : anns) {
				builder.append("@").append(ann.annotationType().getName()).append(' ');
			}
			builder.append(ClassUtils.getQualifiedName(getType()));
			if (isMap()) {
				builder.append("<").append(getMapKeyTypeDescriptor());
				builder.append(", ").append(getMapValueTypeDescriptor()).append(">");
			}
			else if (isCollection()) {
				builder.append("<").append(getElementTypeDescriptor()).append(">");
			}
			return builder.toString();
		}
	}

	// package private

	TypeDescriptor(AbstractDescriptor descriptor) {
		this.type = descriptor.getType();
		this.elementType = descriptor.getElementType();
		this.mapKeyType = descriptor.getMapKeyType();
		this.mapValueType = descriptor.getMapValueType();
		this.annotations = descriptor.getAnnotations();
	}

	TypeDescriptor(Class<?> collectionType, TypeDescriptor elementType) {
		this(collectionType, elementType, TypeDescriptor.NULL, TypeDescriptor.NULL);
	}

	TypeDescriptor(Class<?> mapType, TypeDescriptor keyType, TypeDescriptor valueType) {
		this(mapType, TypeDescriptor.NULL, keyType, valueType);
	}

	static Annotation[] nullSafeAnnotations(Annotation[] annotations) {
		return annotations != null ? annotations : EMPTY_ANNOTATION_ARRAY;
	}
	
	// internal constructors

	private TypeDescriptor(Class<?> type) {
		this(new ClassDescriptor(type));
	}

	private TypeDescriptor() {
		this(null, TypeDescriptor.NULL, TypeDescriptor.NULL, TypeDescriptor.NULL);
	}

	private TypeDescriptor(Class<?> type, TypeDescriptor elementType, TypeDescriptor mapKeyType, TypeDescriptor mapValueType) {
		this.type = type;
		this.elementType = elementType;
		this.mapKeyType = mapKeyType;
		this.mapValueType = mapValueType;
		this.annotations = EMPTY_ANNOTATION_ARRAY;
	}

	// internal helpers
	
	private static TypeDescriptor nested(AbstractDescriptor descriptor, int nestingLevel) {
		for (int i = 0; i < nestingLevel; i++) {
			descriptor = descriptor.nested();
		}
		return new TypeDescriptor(descriptor);		
	}
	
}