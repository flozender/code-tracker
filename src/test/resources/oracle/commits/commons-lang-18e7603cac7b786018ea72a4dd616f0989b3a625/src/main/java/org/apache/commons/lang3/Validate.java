/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang3;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>This class assists in validating arguments. The validation methods are 
 * based along the following principles: 
 * <ul>
 *   <li>An invalid <code>null</code> argument causes a {@link NullPointerException}.</li>
 *   <li>A non-<code>null</code> argument causes an {@link IllegalArgumentException}.</li>
 *   <li>An invalid index into an array/collection/map/string causes an {@link IndexOutOfBoundsException}.</li> 
 * </ul>
 *  
 * <p>All exceptions messages are <a href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Formatter.html#syntax">format strings</a>
 * as defined by the Java platform. For example:</p>
 * 
 * <pre>
 * Validate.isTrue(i > 0, "The value must be greater than zero: %d", i);
 * Validate.notNull(surname, "The surname must not be %s", null);
 * </pre>
 * 
 * <p>#ThreadSafe#
 * @author Apache Software Foundation
 * @author <a href="mailto:ola.berg@arkitema.se">Ola Berg</a>
 * @author Gary Gregory
 * @author Norm Deane
 * @author Paul Benedict
 * @version $Id$
 * @see java.lang.String#format(String, Object...)
 * @since 2.0
 */
public class Validate {

    private static final String DEFAULT_EXCLUSIVE_BETWEEN_EX_MESSAGE = "The value %s is not in the specified exclusive range of %s to %s";
    private static final String DEFAULT_INCLUSIVE_BETWEEN_EX_MESSAGE = "The value %s is not in the specified inclusive range of %s to %s";
    private static final String DEFAULT_MATCHES_PATTERN_EX = "The string %s does not match the pattern %s";
    private static final String DEFAULT_IS_NULL_EX_MESSAGE = "The validated object is null";
    private static final String DEFAULT_IS_TRUE_EX_MESSAGE = "The validated expression is false";
    private static final String DEFAULT_NO_NULL_ELEMENTS_ARRAY_EX_MESSAGE = "The validated array contains null element at index: %d";
    private static final String DEFAULT_NO_NULL_ELEMENTS_COLLECTION_EX_MESSAGE = "The validated collection contains null element at index: %d";
    private static final String DEFAULT_NOT_BLANK_EX_MESSAGE = "The validated character sequence is blank";
    private static final String DEFAULT_NOT_EMPTY_ARRAY_EX_MESSAGE = "The validated array is empty";
    private static final String DEFAULT_NOT_EMPTY_CHAR_SEQUENCE_EX_MESSAGE = "The validated character sequence is empty";
    private static final String DEFAULT_NOT_EMPTY_COLLECTION_EX_MESSAGE = "The validated collection is empty";
    private static final String DEFAULT_NOT_EMPTY_MAP_EX_MESSAGE = "The validated map is empty";
    private static final String DEFAULT_VALID_INDEX_ARRAY_EX_MESSAGE = "The validated array index is invalid: %d";
    private static final String DEFAULT_VALID_INDEX_CHAR_SEQUENCE_EX_MESSAGE = "The validated character sequence index is invalid: %d";
    private static final String DEFAULT_VALID_INDEX_COLLECTION_EX_MESSAGE = "The validated collection index is invalid: %d";
    private static final String DEFAULT_VALID_STATE_EX_MESSAGE = "The validated state is false";
    private static final String DEFAULT_IS_ASSIGNABLE_EX_MESSAGE = "The validated class can not be converted to the %s class";
    private static final String DEFAULT_IS_INSTANCE_OF_EX_MESSAGE = "The validated object is not an instance of %s";

    /**
     * Constructor. This class should not normally be instantiated.
     */
    public Validate() {
      super();
    }

    // isTrue
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the argument condition is <code>true</code>; otherwise 
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a 
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>Validate.isTrue(i > 0.0, "The value must be greater than zero: %d", i);</pre>
     *
     * <p>For performance reasons, the long value is passed as a separate parameter and
     * appended to the exception message only in the case of an error.</p>
     * 
     * @param expression the boolean expression to check 
     * @param message the exception message if invalid
     * @param value the value to append to the message when invalid
     * @throws IllegalArgumentException if expression is <code>false</code>
     * @see #isTrue(boolean)
     * @see #isTrue(boolean, String, double)
     * @see #isTrue(boolean, String, Object...)
     */
    public static void isTrue(boolean expression, String message, long value) {
        if (expression == false) {
            throw new IllegalArgumentException(String.format(message, Long.valueOf(value)));
        }
    }

    /**
     * <p>Validate that the argument condition is <code>true</code>; otherwise 
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a 
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>Validate.isTrue(d > 0.0, "The value must be greater than zero: %s", d);</pre>
     *
     * <p>For performance reasons, the double value is passed as a separate parameter and
     * appended to the exception message only in the case of an error.</p>
     * 
     * @param expression the boolean expression to check 
     * @param message the exception message if invalid
     * @param value the value to append to the message when invalid
     * @throws IllegalArgumentException if expression is <code>false</code>
     * @see #isTrue(boolean)
     * @see #isTrue(boolean, String, long)
     * @see #isTrue(boolean, String, Object...)
     */
    public static void isTrue(boolean expression, String message, double value) {
        if (expression == false) {
            throw new IllegalArgumentException(String.format(message, new Double(value)));
        }
    }

    /**
     * <p>Validate that the argument condition is <code>true</code>; otherwise 
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a 
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>
     * Validate.isTrue(i >= min && i <= max, "The value must be between %d and %d", min, max);
     * Validate.isTrue(myObject.isOk(), "The object is not okay");</pre>
     *
     * @param expression the boolean expression to check 
     * @param message the exception message if invalid
     * @param values the optional values for the formatted exception message
     * @throws IllegalArgumentException if expression is <code>false</code>
     * @see #isTrue(boolean)
     * @see #isTrue(boolean, String, long)
     * @see #isTrue(boolean, String, double)
     */
    public static void isTrue(boolean expression, String message, Object... values) {
        if (expression == false) {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }

    /**
     * <p>Validate that the argument condition is <code>true</code>; otherwise 
     * throwing an exception. This method is useful when validating according 
     * to an arbitrary boolean expression, such as validating a 
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>
     * Validate.isTrue(i > 0);
     * Validate.isTrue(myObject.isOk());</pre>
     *
     * <p>The message of the exception is &quot;The validated expression is 
     * false&quot;.</p>
     * 
     * @param expression the boolean expression to check 
     * @throws IllegalArgumentException if expression is <code>false</code>
     * @see #isTrue(boolean, String, long)
     * @see #isTrue(boolean, String, double)
     * @see #isTrue(boolean, String, Object...)
     */
    public static void isTrue(boolean expression) {
        if (expression == false) {
            throw new IllegalArgumentException(DEFAULT_IS_TRUE_EX_MESSAGE);
        }
    }

    // notNull
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument is not <code>null</code>; 
     * otherwise throwing an exception.
     *
     * <pre>Validate.notNull(myObject, "The object must not be null");</pre>
     *
     * <p>The message of the exception is &quot;The validated object is 
     * null&quot;.</p>
     * 
     * @param <T> the object type
     * @param object the object to check
     * @return the validated object (never <code>null</code> for method chaining)
     * @throws NullPointerException if the object is <code>null</code>
     * @see #notNull(Object, String, Object...)
     */
    public static <T> T notNull(T object) {
        return notNull(object, DEFAULT_IS_NULL_EX_MESSAGE);
    }

    /**
     * <p>Validate that the specified argument is not <code>null</code>; 
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.notNull(myObject, "The object must not be null");</pre>
     * 
     * @param <T> the object type
     * @param object the object to check
     * @param message the exception message if invalid
     * @param values the optional values for the formatted exception message
     * @return the validated object (never <code>null</code> for method chaining)
     * @throws NullPointerException if the object is <code>null</code>
     * @see #notNull(Object)
     */
    public static <T> T notNull(T object, String message, Object... values) {
        if (object == null) {
            throw new NullPointerException(String.format(message, values));
        }
        return object;
    }

    // notEmpty array
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument array is neither <code>null</code> 
     * nor a length of zero (no elements); otherwise throwing an exception 
     * with the specified message.
     *
     * <pre>Validate.notEmpty(myArray, "The array must not be empty");</pre>
     * 
     * @param <T> the array type
     * @param array the array to check
     * @param message the exception message if invalid
     * @return the validated array (never <code>null</code> method for chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IllegalArgumentException if the array is empty
     * @see #notEmpty(Object[])
     */
    public static <T> T[] notEmpty(T[] array, String message, Object... values) {
        if (array == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (array.length == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return array;
    }

    /**
     * <p>Validate that the specified argument array is neither <code>null</code> 
     * nor a length of zero (no elements); otherwise throwing an exception. 
     *
     * <pre>Validate.notEmpty(myArray);</pre>
     * 
     * <p>The message in the exception is &quot;The validated array is 
     * empty&quot;.
     * 
     * @param <T> the array type
     * @param array the array to check
     * @return the validated array (never <code>null</code> method for chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IllegalArgumentException if the array is empty
     * @see #notEmpty(Object[], String, Object...)
     */
    public static <T> T[] notEmpty(T[] array) {
        return notEmpty(array, DEFAULT_NOT_EMPTY_ARRAY_EX_MESSAGE);
    }

    // notEmpty collection
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument collection is neither <code>null</code> 
     * nor a size of zero (no elements); otherwise throwing an exception 
     * with the specified message.
     *
     * <pre>Validate.notEmpty(myCollection, "The collection must not be empty");</pre>
     * 
     * @param <T> the collection type
     * @param collection the collection to check
     * @param message the exception message if invalid
     * @return the validated collection (never <code>null</code> method for chaining)
     * @throws NullPointerException if the collection is <code>null</code>
     * @throws IllegalArgumentException if the collection is empty
     * @see #notEmpty(Object[])
     */
    public static <T extends Collection<?>> T notEmpty(T collection, String message, Object... values) {
        if (collection == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (collection.size() == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return collection;
    }

    /**
     * <p>Validate that the specified argument collection is neither <code>null</code> 
     * nor a size of zero (no elements); otherwise throwing an exception. 
     *
     * <pre>Validate.notEmpty(myCollection);</pre>
     * 
     * <p>The message in the exception is &quot;The validated collection is 
     * empty&quot;.</p>
     * 
     * @param <T> the collection type
     * @param collection the collection to check
     * @return the validated collection (never <code>null</code> method for chaining)
     * @throws NullPointerException if the collection is <code>null</code>
     * @throws IllegalArgumentException if the collection is empty
     * @see #notEmpty(Collection, String, Object...)
     */
    public static <T extends Collection<?>> T notEmpty(T collection) {
        return notEmpty(collection, DEFAULT_NOT_EMPTY_COLLECTION_EX_MESSAGE);
    }

    // notEmpty map
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument map is neither <code>null</code> 
     * nor a size of zero (no elements); otherwise throwing an exception 
     * with the specified message.
     *
     * <pre>Validate.notEmpty(myMap, "The map must not be empty");</pre>
     * 
     * @param <T> the map type
     * @param map the map to check
     * @param message the exception message if invalid
     * @return the validated map (never <code>null</code> method for chaining)
     * @throws NullPointerException if the map is <code>null</code>
     * @throws IllegalArgumentException if the map is empty
     * @see #notEmpty(Object[])
     */
    public static <T extends Map<?, ?>> T notEmpty(T map, String message, Object... values) {
        if (map == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (map.size() == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return map;
    }

    /**
     * <p>Validate that the specified argument map is neither <code>null</code> 
     * nor a size of zero (no elements); otherwise throwing an exception. 
     *
     * <pre>Validate.notEmpty(myMap);</pre>
     * 
     * <p>The message in the exception is &quot;The validated map is 
     * empty&quot;.</p>
     * 
     * @param <T> the map type
     * @param map the map to check
     * @return the validated map (never <code>null</code> method for chaining)
     * @throws NullPointerException if the map is <code>null</code>
     * @throws IllegalArgumentException if the map is empty
     * @see #notEmpty(Map, String, Object...)
     */
    public static <T extends Map<?, ?>> T notEmpty(T map) {
        return notEmpty(map, DEFAULT_NOT_EMPTY_MAP_EX_MESSAGE);
    }

    // notEmpty string
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument character sequence is 
     * neither <code>null</code> nor a length of zero (no characters); 
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.notEmpty(myString, "The string must not be empty");</pre>
     * 
     * @param <T> the character sequence type
     * @param chars the character sequence to check
     * @param message the exception message if invalid
     * @return the validated character sequence (never <code>null</code> method for chaining)
     * @throws NullPointerException if the character sequence is <code>null</code>
     * @throws IllegalArgumentException if the character sequence is empty
     * @see #notEmpty(CharSequence)
     */
    public static <T extends CharSequence> T notEmpty(T chars, String message, Object... values) {
        if (chars == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (chars.length() == 0) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return chars;
    }

    /**
     * <p>Validate that the specified argument character sequence is 
     * neither <code>null</code> nor a length of zero (no characters); 
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.notEmpty(myString);</pre>
     * 
     * <p>The message in the exception is &quot;The validated 
     * character sequence is empty&quot;.</p>
     * 
     * @param <T> the character sequence type
     * @param chars the character sequence to check
     * @return the validated character sequence (never <code>null</code> method for chaining)
     * @throws NullPointerException if the character sequence is <code>null</code>
     * @throws IllegalArgumentException if the character sequence is empty
     * @see #notEmpty(CharSequence, String, Object...)
     */
    public static <T extends CharSequence> T notEmpty(T chars) {
        return notEmpty(chars, DEFAULT_NOT_EMPTY_CHAR_SEQUENCE_EX_MESSAGE);
    }

    // notBlank string
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument character sequence is 
     * neither <code>null</code>, a length of zero (no characters), empty
     * nor whitespace; otherwise throwing an exception with the specified 
     * message.
     *
     * <pre>Validate.notBlank(myString, "The string must not be blank");</pre>
     * 
     * @param <T> the character sequence type
     * @param chars the character sequence to check
     * @param message the exception message if invalid
     * @return the validated character sequence (never <code>null</code> method for chaining)
     * @throws NullPointerException if the character sequence is <code>null</code>
     * @throws IllegalArgumentException if the character sequence is blank
     * @see #notBlank(CharSequence)
     */
    public static <T extends CharSequence> T notBlank(T chars, String message, Object... values) {
        if (chars == null) {
            throw new NullPointerException(String.format(message, values));
        }
        if (StringUtils.isBlank(chars)) {
            throw new IllegalArgumentException(String.format(message, values));
        }
        return chars;
    }

    /**
     * <p>Validate that the specified argument character sequence is 
     * neither <code>null</code>, a length of zero (no characters), empty
     * nor whitespace; otherwise throwing an exception.
     *
     * <pre>Validate.notBlank(myString);</pre>
     * 
     * <p>The message in the exception is &quot;The validated character 
     * sequence is blank&quot;.</p>
     * 
     * @param <T> the character sequence type
     * @param chars the character sequence to check
     * @return the validated character sequence (never <code>null</code> method for chaining)
     * @throws NullPointerException if the character sequence is <code>null</code>
     * @throws IllegalArgumentException if the character sequence is blank
     * @see #notBlank(CharSequence, String, Object...)
     */
    public static <T extends CharSequence> T notBlank(T chars) {
        return notBlank(chars, DEFAULT_NOT_BLANK_EX_MESSAGE);
    }

    // noNullElements array
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument array is neither 
     * <code>null</code> nor contains any elements that are <code>null</code>;
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.noNullElements(myArray, "The array contain null at position %d");</pre>
     * 
     * <p>If the array is <code>null</code>, then the message in the exception 
     * is &quot;The validated object is null&quot;.</p>
     * 
     * <p>If the array has a <code>null</code> element, then the iteration 
     * index of the invalid element is appended to the <code>values</code> 
     * argument.</p>
     * 
     * @param <T> the array type
     * @param array the array to check
     * @return the validated array (never <code>null</code> method for chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IllegalArgumentException if an element is <code>null</code>
     * @see #noNullElements(Object[])
     */
    public static <T> T[] noNullElements(T[] array, String message, Object... values) {
        Validate.notNull(array);
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                Object[] values2 = ArrayUtils.add(values, Integer.valueOf(i));
                throw new IllegalArgumentException(String.format(message, values2));
            }
        }
        return array;
    }

    /**
     * <p>Validate that the specified argument array is neither 
     * <code>null</code> nor contains any elements that are <code>null</code>;
     * otherwise throwing an exception.
     *
     * <pre>Validate.noNullElements(myArray);</pre>
     * 
     * <p>If the array is <code>null</code>, then the message in the exception 
     * is &quot;The validated object is null&quot;.</p>
     * 
     * <p>If the array has a <code>null</code> element, then the message in the
     * exception is &quot;The validated array contains null element at index: 
     * &quot followed by the index.</p>
     *
     * @param <T> the array type
     * @param array the array to check
     * @return the validated array (never <code>null</code> method for chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IllegalArgumentException if an element is <code>null</code>
     * @see #noNullElements(Object[], String, Object...)
     */
    public static <T> T[] noNullElements(T[] array) {
        return noNullElements(array, DEFAULT_NO_NULL_ELEMENTS_ARRAY_EX_MESSAGE);
    }

    // noNullElements iterable
    //---------------------------------------------------------------------------------

    /**
     * <p>Validate that the specified argument iterable is neither 
     * <code>null</code> nor contains any elements that are <code>null</code>;
     * otherwise throwing an exception with the specified message.
     *
     * <pre>Validate.noNullElements(myCollection, "The collection contains null at position %d");</pre>
     * 
     * <p>If the iterable is <code>null</code>, then the message in the exception 
     * is &quot;The validated object is null&quot;.</p>
     * 
     * <p>If the iterable has a <code>null</code> element, then the iteration 
     * index of the invalid element is appended to the <code>values</code> 
     * argument.</p>
     *
     * @param <T> the iterable type
     * @param iterable the iterable to check
     * @return the validated iterable (never <code>null</code> method for chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IllegalArgumentException if an element is <code>null</code>
     * @see #noNullElements(Iterable)
     */
    public static <T extends Iterable<?>> T noNullElements(T iterable, String message, Object... values) {
        Validate.notNull(iterable);
        int i = 0;
        for (Iterator<?> it = iterable.iterator(); it.hasNext(); i++) {
            if (it.next() == null) {
                Object[] values2 = ArrayUtils.addAll(values, Integer.valueOf(i));
                throw new IllegalArgumentException(String.format(message, values2));
            }
        }
        return iterable;
    }

    /**
     * <p>Validate that the specified argument iterable is neither 
     * <code>null</code> nor contains any elements that are <code>null</code>;
     * otherwise throwing an exception.
     *
     * <pre>Validate.noNullElements(myCollection);</pre>
     * 
     * <p>If the iterable is <code>null</code>, then the message in the exception 
     * is &quot;The validated object is null&quot;.</p>
     * 
     * <p>If the array has a <code>null</code> element, then the message in the
     * exception is &quot;The validated iterable contains null element at index: 
     * &quot followed by the index.</p>
     *
     * @param <T> the iterable type
     * @param iterable the iterable to check
     * @return the validated iterable (never <code>null</code> method for chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IllegalArgumentException if an element is <code>null</code>
     * @see #noNullElements(Iterable, String, Object...)
     */
    public static <T extends Iterable<?>> T noNullElements(T iterable) {
        return noNullElements(iterable, DEFAULT_NO_NULL_ELEMENTS_COLLECTION_EX_MESSAGE);
    }

    // validIndex array
    //---------------------------------------------------------------------------------

    /**
     * <p>Validates that the index is within the bounds of the argument 
     * array; otherwise throwing an exception with the specified message.</p>
     *
     * <pre>Validate.validIndex(myArray, 2, "The array index is invalid: ");</pre>
     * 
     * <p>If the array is <code>null</code>, then the message of the exception 
     * is &quot;The validated object is null&quot;.</p>
     * 
     * @param <T> the array type
     * @param array the array to check
     * @param index the index
     * @param message the exception message if invalid
     * @return the validated array (never <code>null</code> for method chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is invalid
     * @see #validIndex(Object[], int)
     */
    public static <T> T[] validIndex(T[] array, int index, String message, Object... values) {
        Validate.notNull(array);
        if (index < 0 || index >= array.length) {
            throw new IndexOutOfBoundsException(String.format(message, values));
        }
        return array;
    }

    /**
     * <p>Validates that the index is within the bounds of the argument 
     * array; otherwise throwing an exception.</p>
     *
     * <pre>Validate.validIndex(myArray, 2);</pre>
     *
     * <p>If the array is <code>null</code>, then the message of the exception
     * is &quot;The validated object is null&quot;.</p>
     * 
     * <p>If the index is invalid, then the message of the exception is 
     * &quot;The validated array index is invalid: &quot; followed by the 
     * index.</p>
     * 
     * @param <T> the array type
     * @param array the array to check
     * @param index the index
     * @return the validated array (never <code>null</code> for method chaining)
     * @throws NullPointerException if the array is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is invalid
     * @see #validIndex(Object[], int, String, Object...)
     */
    public static <T> T[] validIndex(T[] array, int index) {
        return validIndex(array, index, DEFAULT_VALID_INDEX_ARRAY_EX_MESSAGE, Integer.valueOf(index));
    }

    // validIndex collection
    //---------------------------------------------------------------------------------

    /**
     * <p>Validates that the index is within the bounds of the argument 
     * collection; otherwise throwing an exception with the specified message.</p>
     *
     * <pre>Validate.validIndex(myCollection, 2, "The collection index is invalid: ");</pre>
     * 
     * <p>If the collection is <code>null</code>, then the message of the 
     * exception is &quot;The validated object is null&quot;.</p>
     *
     * @param <T> the collection type
     * @param collection the collection to check
     * @param index the index
     * @param message the exception message if invalid
     * @return the validated collection (never <code>null</code> for chaining)
     * @throws NullPointerException if the collection is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is invalid
     * @see #validIndex(Collection, int)
     */
    public static <T extends Collection<?>> T validIndex(T collection, int index, String message, Object... values) {
        Validate.notNull(collection);
        if (index < 0 || index >= collection.size()) {
            throw new IndexOutOfBoundsException(String.format(message, values));
        }
        return collection;
    }

    /**
     * <p>Validates that the index is within the bounds of the argument 
     * collection; otherwise throwing an exception.</p>
     *
     * <pre>Validate.validIndex(myCollection, 2);</pre>
     *
     * <p>If the index is invalid, then the message of the exception 
     * is &quot;The validated collection index is invalid: &quot; 
     * followed by the index.</p>
     * 
     * @param <T> the collection type
     * @param collection the collection to check
     * @param index the index
     * @return the validated collection (never <code>null</code> for method chaining)
     * @throws NullPointerException if the collection is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is invalid
     * @see #validIndex(Collection, int, String, Object...)
     */
    public static <T extends Collection<?>> T validIndex(T collection, int index) {
        return validIndex(collection, index, DEFAULT_VALID_INDEX_COLLECTION_EX_MESSAGE, Integer.valueOf(index));
    }

    // validIndex string
    //---------------------------------------------------------------------------------

    /**
     * <p>Validates that the index is within the bounds of the argument 
     * character sequence; otherwise throwing an exception with the 
     * specified message.</p>
     *
     * <pre>Validate.validIndex(myStr, 2, "The string index is invalid: ");</pre>
     * 
     * <p>If the character sequence is <code>null</code>, then the message 
     * of the exception is &quot;The validated object is null&quot;.</p>
     *
     * @param <T> the character sequence type
     * @param chars the character sequence to check
     * @param index the index
     * @param message the exception message if invalid
     * @return the validated character sequence (never <code>null</code> for method chaining)
     * @throws NullPointerException if the character sequence is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is invalid
     * @see #validIndex(CharSequence, int)
     */
    public static <T extends CharSequence> T validIndex(T chars, int index, String message, Object... values) {
        Validate.notNull(chars);
        if (index < 0 || index >= chars.length()) {
            throw new IndexOutOfBoundsException(String.format(message, values));
        }
        return chars;
    }

    /**
     * <p>Validates that the index is within the bounds of the argument 
     * character sequence; otherwise throwing an exception.</p>
     * 
     * <pre>Validate.validIndex(myStr, 2);</pre>
     *
     * <p>If the character sequence is <code>null</code>, then the message 
     * of the exception is &quot;The validated object is 
     * null&quot;.</p>
     * 
     * <p>If the index is invalid, then the message of the exception 
     * is &quot;The validated character sequence index is invalid: &quot; 
     * followed by the index.</p>
     * 
     * @param <T> the character sequence type
     * @param chars the character sequence to check
     * @param index the index
     * @return the validated character sequence (never <code>null</code> for method chaining)
     * @throws NullPointerException if the character sequence is <code>null</code>
     * @throws IndexOutOfBoundsException if the index is invalid
     * @see #validIndex(CharSequence, int, String, Object...)
     */
    public static <T extends CharSequence> T validIndex(T chars, int index) {
        return validIndex(chars, index, DEFAULT_VALID_INDEX_CHAR_SEQUENCE_EX_MESSAGE, Integer.valueOf(index));
    }

    /**
     * <p>Validate that the stateful condition is <code>true</code>; otherwise 
     * throwing an exception. This method is useful when validating according 
     * to an arbitrary boolean expression, such as validating a 
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>
     * Validate.validState(field > 0);
     * Validate.validState(this.isOk());</pre>
     *
     * <p>The message of the exception is &quot;The validated state is 
     * false&quot;.</p>
     * 
     * @param expression the boolean expression to check 
     * @throws IllegalStateException if expression is <code>false</code>
     * @see #validState(boolean, String, Object...)
     */
    public static void validState(boolean expression) {
        if (expression == false) {
            throw new IllegalArgumentException(DEFAULT_VALID_STATE_EX_MESSAGE);
        }
    }

    /**
     * <p>Validate that the stateful condition is <code>true</code>; otherwise 
     * throwing an exception with the specified message. This method is useful when
     * validating according to an arbitrary boolean expression, such as validating a 
     * primitive number or using your own custom validation expression.</p>
     *
     * <pre>Validate.validState(this.isOk(), "The state is not OK: %s", myObject);</pre>
     *
     * @param expression the boolean expression to check 
     * @param message the exception message if invalid
     * @param values the optional values for the formatted exception message
     * @throws IllegalStateException if expression is <code>false</code>
     * @see #validState(boolean)
     */
    public static void validState(boolean expression, String message, Object... values) {
        if (expression == false) {
            throw new IllegalStateException(String.format(message, values));
        }
    }
    
    /**
     * <p>Validate that the specified argument character sequence matches the specified regular
     * expression pattern; otherwise throwing an exception.</p>
     *
     * <pre>Validate.matchesPattern("hi", "[a-z]*");</pre>
     * 
     * <p>The syntax of the pattern is the one used in the {@link Pattern} class.</p>
     * 
     * @param input the character sequence to validate
     * @param pattern regular expression pattern
     * @throws IllegalArgumentException if the character sequence does not match the pattern
     * @see #matchesPattern(CharSequence, String, String, Object...)
     */
    public static void matchesPattern(CharSequence input, String pattern)
    {
        if (Pattern.matches(pattern, input) == false)
        {
            throw new IllegalArgumentException(String.format(DEFAULT_MATCHES_PATTERN_EX, input, pattern));
        }
    }
    
    /**
     * <p>Validate that the specified argument character sequence matches the specified regular
     * expression pattern; otherwise throwing an exception with the specified message.</p>
     *
     * <pre>Validate.matchesPattern("hi", "[a-z]*", "%s does not match %s", "hi" "[a-z]*");</pre>
     * 
     * <p>The syntax of the pattern is the one used in the {@link Pattern} class.</p>
     * 
     * @param input the character sequence to validate
     * @param pattern regular expression pattern
     * @param message the exception message
     * @param values (optional) values to replace in the exception message
     * @throws IllegalArgumentException if the character sequence does not match the pattern
     * @see #matchesPattern(CharSequence, String)
     */
    public static void matchesPattern(CharSequence input, String pattern, String message, Object... values)
    {
        if (Pattern.matches(pattern, input) == false)
        {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
    
    /**
     * <p>Validate that the specified argument object fall between the two
     * inclusive values specified; otherwise, throws an exception.</p>
     *
     * <pre>Validate.inclusiveBetween(0, 2, 1);</pre>
     * 
     * @param value the object to validate
     * @param start the inclusive start value
     * @param end the inclusive end value
     * @throws IllegalArgumentException if the value falls out of the boundaries
     * @see #inclusiveBetween(Object, Object, Comparable, String, Object...)
     */
    public static <T> void inclusiveBetween(T start, T end, Comparable<T> value)
    {
        if (value.compareTo(start) < 0 || value.compareTo(end) > 0)
        {
            throw new IllegalArgumentException(String.format(DEFAULT_INCLUSIVE_BETWEEN_EX_MESSAGE, value, start, end));
        }
    }
    
    /**
     * <p>Validate that the specified argument object fall between the two
     * inclusive values specified; otherwise, throws an exception with the
     * specified message.</p>
     *
     * <pre>Validate.inclusiveBetween(0, 2, 1, "Not in boundaries");</pre>
     * 
     * @param value the object to validate
     * @param start the inclusive start value
     * @param end the inclusive end value
     * @param message the exception message
     * @param values to replace in the exception message (optional)
     * @throws IllegalArgumentException if the value falls out of the boundaries
     * @see #inclusiveBetween(Object, Object, Comparable)
     */
    public static <T> void inclusiveBetween(T start, T end, Comparable<T> value, String message, Object... values)
    {
        if (value.compareTo(start) < 0 || value.compareTo(end) > 0)
        {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
    
    /**
     * <p>Validate that the specified argument object fall between the two
     * exclusive values specified; otherwise, throws an exception.</p>
     *
     * <pre>Validate.inclusiveBetween(0, 2, 1);</pre>
     * 
     * @param value the object to validate
     * @param start the exclusive start value
     * @param end the exclusive end value
     * @throws IllegalArgumentException if the value falls out of the boundaries
     * @see #exclusiveBetween(Object, Object, Comparable, String, Object...)
     */
    public static <T> void exclusiveBetween(T start, T end, Comparable<T> value)
    {
        if (value.compareTo(start) <= 0 || value.compareTo(end) >= 0)
        {
            throw new IllegalArgumentException(String.format(DEFAULT_EXCLUSIVE_BETWEEN_EX_MESSAGE, value, start, end));
        }
    }
    
    /**
     * <p>Validate that the specified argument object fall between the two
     * exclusive values specified; otherwise, throws an exception with the
     * specified message.</p>
     *
     * <pre>Validate.inclusiveBetween(0, 2, 1, "Not in boundaries");</pre>
     * 
     * @param value the object to validate
     * @param start the exclusive start value
     * @param end the exclusive end value
     * @param message the exception message
     * @param values to replace in the exception message (optional)
     * @throws IllegalArgumentException if the value falls out of the boundaries
     * @see #exclusiveBetween(Object, Object, Comparable)
     */
    public static <T> void exclusiveBetween(T start, T end, Comparable<T> value, String message, Object... values)
    {
        if (value.compareTo(start) <= 0 || value.compareTo(end) >= 0)
        {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }

    /**
     * <p>Validate that the argument is an instance of the specified class; otherwise
     * throwing an exception. This method is useful when validating according to an arbitrary
     * class</p>
     * 
     * <pre>Validate.isInstanceOf(OkClass.class, object);</pre>
     * 
     * <p>The message of the exception is &quot;The validated object is not an instance of&quot;
     * followed by the name of the class</p>
     * 
     * @param type the class the object must be validated against
     * @param o the object to check
     * @throws IllegalArgumentException if argument is not of specified class
     * @see #isInstanceOf(Class, Object, String, Object...)
     */
    public static void isInstanceOf(Class<?> type, Object o)
    {
        if (type.isInstance(o) == false)
        {
            throw new IllegalArgumentException(String.format(DEFAULT_IS_INSTANCE_OF_EX_MESSAGE, type.getName()));
        }
    }
    
    /**
     * <p>Validate that the argument is an instance of the specified class; otherwise
     * throwing an exception with the specified message. This method is useful when 
     * validating according to an arbitrary class</p>
     * 
     * <pre>Validate.isInstanceOf(OkClass.classs, object, "Wrong class, object is of class %s", object.getClass().getName());</pre>
     * 
     * @param type the class the object must be validated against
     * @param o the object to check
     * @param message exception message
     * @param values optional value for the exception message
     * @throws IllegalArgumentException if argument is not of specified class
     * @see #isInstanceOf(Class, Object)
     */
    public static void isInstanceOf(Class<?> type, Object o, String message, Object... values)
    {
        if (type.isInstance(o) == false)
        {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
    
    /**
     * <p>Validate that the argument can be converted to the specified class; otherwise
     * throwing an exception with the specified message. This method is useful when
     * validating if there will be no casting errors.</p>
     * 
     * <pre>Validate.isAssignableFrom(SuperClass.class, object.getClass());</pre>
     * 
     * <p>The message of the exception is &quot;The validated object can not be converted to the&quot;
     * followed by the name of the class and &quot;class&quot;</p>
     * 
     * @param superType the class the class must be validated against
     * @param type the class to check
     * @throws IllegalArgumentException if argument can not be converted to the specified class
     * @see #isAssignableFrom(Class, Class, String, Object...)
     */
    public static void isAssignableFrom(Class<?> superType, Class<?> type)
    {
        if (superType.isAssignableFrom(type) == false)
        {
            throw new IllegalArgumentException(String.format(DEFAULT_IS_ASSIGNABLE_EX_MESSAGE, superType.getName()));
        }
    }
    
    /**
     * <p>Validate that the argument can be converted to the specified class; otherwise
     * throwing an exception. This method is useful when validating if there will be no
     * casting errors.</p>
     * 
     * <pre>Validate.isAssignableFrom(SuperClass.class, object.getClass());</pre>
     * 
     * <p>The message of the exception is &quot;The validated object can not be converted to the&quot;
     * followed by the name of the class and &quot;class&quot;</p>
     * 
     * @param superType the class the class must be validated against
     * @param type the class to check
     * @param message the exception message if invalid
     * @param values the optional values for the formatted exception message
     * @throws IllegalArgumentException if argument can not be converted to the specified class
     * @see #isAssignableFrom(Class, Class)
     */
    public static void isAssignableFrom(Class<?> superType, Class<?> type, String message, Object... values)
    {
        if (superType.isAssignableFrom(type) == false)
        {
            throw new IllegalArgumentException(String.format(message, values));
        }
    }
}
