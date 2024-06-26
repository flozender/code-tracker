/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.commons.lang.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.apache.commons.lang.ArrayUtils;
/**
 * <p><code>ConstructorUtils</code> contains utility methods for working for
 * constructors by reflection.</p>
 *
 * <p>The ability is provided to break the scoping restrictions coded by the
 * programmer. This can allow classes to be created that shouldn't be, for
 * example new instances of an enumerated type. Thus, this facility should
 * be used with care.</p>
 *
 * @author <a href="mailto:scolebourne@apache.org">Stephen Colebourne</a>
 * @version $Id: ConstructorUtils.java,v 1.3 2003/08/18 02:22:25 bayard Exp $
 */
public class ConstructorUtils {

    /**
     * An empty constructor array.
     */
    public static final Constructor[] EMPTY_CONSTRUCTOR_ARRAY = new Constructor[0];
    
    /**
     * <p>ConstructorUtils instances should NOT be constructed in standard programming.</p>
     *
     * <p>Instead, the class should be used as <code>ConstructorUtils.newInstance(...)</code>.
     * This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public ConstructorUtils() {
    }

    // -------------------------------------------------------------------------
    
    /**
     * <p>Gets a public <code>Constructor</code> object by matching the
     * parameter types as per the Java Language Specification.</p>
     *
     * @param cls  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter
     *  types, may be <code>null</code>
     * @return Constructor object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Constructor getConstructor(Class cls, Class[] types) {
        return getConstructor(cls, types, false);
    }
    
    /**
     * <p>Gets a public <code>Constructor</code> object by matching the
     * parameter types as per the Java Language Specification.</p>
     *
     * @param cls  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter
     *  types, may be <code>null</code>
     * @param breakScope  whether to break scope restrictions using
     *  the <code>setAccessible</code> method. <code>False</code> will
     *  only match public methods.
     * @return Constructor object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Constructor getConstructor(Class cls, Class[] types, boolean breakScope) {
        if (cls == null) {
            throw new IllegalArgumentException("The class must not be null");
        }
        // try exact call first for speed
        try {
            getConstructorExact(cls, types, breakScope);
            
        } catch (ReflectionException ex) {
            if (types == null || types.length == 0) {
                throw ex;
            }
            if (ex.getCause() instanceof NoSuchMethodException == false) {
                throw ex;
            }
        }
        // try to find best match
        try {
            Constructor[] cons = cls.getDeclaredConstructors();
            for (int i = 0; i < cons.length; i++) {
                if (cons[i].getParameterTypes().length == types.length) {
                    // TODO
                }
            }
            return null;

        } catch (ReflectionException ex) {
            throw ex;
        } catch (LinkageError ex) {
            throw new ReflectionException(ReflectionUtils.getThrowableText(
                ex, "getting constructor", cls.getName(), types, null), ex);
        } catch (Exception ex) {
            throw new ReflectionException(ReflectionUtils.getThrowableText(
                ex, "getting constructor", cls.getName(), types, null), ex);
        }
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * <p>Gets a public <code>Constructor</code> object by exactly matching the
     * parameter types.</p>
     *
     * @param cls  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter
     *  types, may be <code>null</code>
     * @return Constructor object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Constructor getConstructorExact(Class cls, Class[] types) {
        return getConstructorExact(cls, types, false);
    }
    
    /**
     * <p>Gets a <code>Constructor</code> object by exactly matching the
     * parameter types.</p>
     *
     * @param cls  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter types, may
     *  be <code>null</code>
     * @param breakScope  whether to break scope restrictions using the
     *  <code>setAccessible</code> method. False will only match public methods.
     * @return Constructor object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Constructor getConstructorExact(Class cls, Class[] types, boolean breakScope) {
        if (cls == null) {
            throw new IllegalArgumentException("The class must not be null");
        }
        try {
            if (breakScope) {
                Constructor con = cls.getDeclaredConstructor(types);
                if (Modifier.isPublic(con.getModifiers()) == false) {
                    con.setAccessible(true);
                }
                return con;
                
            } else {
                return cls.getConstructor(types);
            }

        } catch (ReflectionException ex) {
            throw ex;
        } catch (LinkageError ex) {
            throw new ReflectionException(ReflectionUtils.getThrowableText(
                ex, "getting constructor", cls.getName(), types, null), ex);
        } catch (Exception ex) {
            throw new ReflectionException(ReflectionUtils.getThrowableText(
                ex, "getting constructor", cls.getName(), types, null), ex);
        }
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * <p>Creates a new instance using a <code>Constructor</code> and parameters.</p>
     * 
     * @param con  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param param  the single parameter to pass to the constructor, may
     *  be <code>null</code>
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the constructor is <code>null</code>
     */
    public static Object newInstance(Constructor con, Object param) {
        return newInstance(con, new Object[] {param}, false);
    }
    
    /**
     * <p>Creates a new instance using a <code>Constructor</code> and parameters.</p>
     * 
     * @param con  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param params  array of objects to pass as parameters, may
     *  be <code>null</code>
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the constructor is <code>null</code>
     */
    public static Object newInstance(Constructor con, Object[] params) {
        return newInstance(con, params, false);
    }
    
    /**
     * <p>Creates a new instance using a <code>Constructor</code> and parameters.</p>
     * 
     * @param con  Class object to find constructor for, must not
     *  be <code>null</code>
     * @param params  array of objects to pass as parameters, may
     *  be <code>null</code>
     * @param breakScope  whether to break scope restrictions using the
     *  <code>setAccessible</code> method. <code>False</code> will only
     *  match public methods.
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the constructor is <code>null</code>
     */
    public static Object newInstance(Constructor con, Object[] params, boolean breakScope) {
        if (con == null) {
            throw new IllegalArgumentException("The constructor must not be null");
        }
        try {
            if (breakScope && Modifier.isPublic(con.getModifiers()) == false) {
                con.setAccessible(true);
            }
            return con.newInstance(params);
    
        } catch (ReflectionException ex) {
            throw ex;
        } catch (LinkageError ex) {
            throw new ReflectionException(ReflectionUtils.getThrowableText(
                ex, "invoking constructor", con.getDeclaringClass().getName(), con.getParameterTypes(), null), ex);
        } catch (Exception ex) {
            throw new ReflectionException(ReflectionUtils.getThrowableText(
                ex, "invoking constructor", con.getDeclaringClass().getName(), con.getParameterTypes(), null), ex);
        }
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code> by name.</p>
     * 
     * @param className  String class name to instantiate, must not be empty
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class name is empty
     */
    public static Object newInstance(String className) {
        return newInstance(className, false);
    }
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code> by name.</p>
     *
     * <p>If the constructor is not public, <code>setAccessible(true)</code>
     * is used to make it accessible.</p>
     * 
     * @param className  String class name to instantiate, must not be empty
     * @param breakScope  whether to break scope restrictions using the
     *  <code>setAccessible</code> method. False will only match public methods.
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class name is empty
     */
    public static Object newInstance(String className, boolean breakScope) {
        Class cls = ReflectionUtils.getClass(className);
        return newInstance(cls, breakScope);
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code>.</p>
     * 
     * @param cls  Class object to instantiate, must not be <code>null</code>
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Object newInstance(Class cls) {
        return newInstance(cls, false);
    }
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code>.</p>
     *
     * <p>If the constructor is not public, <code>setAccessible(true)</code>
     * is used to make it accessible.</p>
     * 
     * @param cls  Class object to instantiate, must not be <code>null</code>
     * @param breakScope  whether to break scope restrictions using the
     *  <code>setAccessible</code> method. <code>False</code> will only
     *  match public methods.
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Object newInstance(Class cls, boolean breakScope) {
        if (breakScope) {
            return newInstanceExact(cls, null, null, true);
            
        } else {
            if (cls == null) {
                throw new IllegalArgumentException("The constructor must not be null");
            }
            try {
                return cls.newInstance();
        
            } catch (ReflectionException ex) {
                throw ex;
            } catch (LinkageError ex) {
                throw new ReflectionException(ReflectionUtils.getThrowableText(
                    ex, "instantiating class", cls.getName(), null, null), ex);
            } catch (Exception ex) {
                throw new ReflectionException(ReflectionUtils.getThrowableText(
                    ex, "instantiating class", cls.getName(), null, null), ex);
            }
        }
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code>.</p>
     *
     * <p>The constructor is found by matching the
     * parameter types as per the Java Language Specification.</p>
     * 
     * @param cls  Class object to instantiate, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter types,
     *  may be <code>null</code>
     * @param params  array of objects to pass as parameters, may
     *  be <code>null</code>
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Object newInstance(Class cls, Class[] types, Object[] params) {
        return newInstance(cls, types, params, false);
    }
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code>.</p>
     *
     * <p>The constructor is found by matching the
     * parameter types as per the Java Language Specification.</p>
     * 
     * @param cls  Class object to instantiate, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter types,
     *  may be <code>null</code>
     * @param params  array of objects to pass as parameters, may
     *  be <code>null</code>
     * @param breakScope  whether to break scope restrictions using the
     *  <code>setAccessible</code> method. <code>False</code> will only
     *  match public methods.
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the types and params lengths differ
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Object newInstance(Class cls, Class[] types, Object[] params, boolean breakScope) {
        if (ArrayUtils.isSameLength(types, params) == false) {
            throw new IllegalArgumentException("The types and params lengths must be the same");
        }
        Constructor con = getConstructor(cls, types, breakScope);
        return newInstance(con, params, breakScope);
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code>.</p>
     *
     * <p>The constructor is found by matching the parameter types exactly.</p>
     * 
     * @param cls  Class object to instantiate, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter types,
     *  may be <code>null</code>
     * @param params  array of objects to pass as parameters, may
     *  be <code>null</code>
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Object newInstanceExact(Class cls, Class[] types, Object[] params) {
        return newInstanceExact(cls, types, params, false);
    }
    
    /**
     * <p>Creates a new instance of the specified <code>Class</code>.</p>
     *
     * <p>The constructor is found by matching the parameter types exactly.</p>
     * 
     * @param cls  Class object to instantiate, must not
     *  be <code>null</code>
     * @param types  array of Class objects representing parameter types,
     *  may be <code>null</code>
     * @param params  array of objects to pass as parameters, may
     *  be <code>null</code>
     * @param breakScope  whether to break scope restrictions using the
     *  <code>setAccessible</code> method. <code>False</code> will only match
     *  public methods.
     * @return the newly created object
     * @throws ReflectionException if an error occurs during reflection
     * @throws IllegalArgumentException if the types and params lengths differ
     * @throws IllegalArgumentException if the class is <code>null</code>
     */
    public static Object newInstanceExact(Class cls, Class[] types, Object[] params, boolean breakScope) {
        if (ArrayUtils.isSameLength(types, params) == false) {
            throw new IllegalArgumentException("The types and params lengths must be the same");
        }
        Constructor con = getConstructorExact(cls, types, breakScope);
        return newInstance(con, params, breakScope);
    }
    
    // -------------------------------------------------------------------------
    
}
