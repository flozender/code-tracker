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
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This specifies that a property is part of the natural id of the entity.
 *
 * @author Nicol�s Lichtmaier
 */
@Target( { METHOD, FIELD } )
@Retention( RUNTIME )
public @interface NaturalId {
	/**
	 * Is this natural id mutable (or immutable)?
	 *
	 * @return {@code true} indicates the natural id is mutable; {@code false} (the default) that it is immutable.
	 */
	boolean mutable() default false;
	
	/**
	 * Should the mapping of this natural id to the primary id be cached 
	 * 
	 * @return {@code true} (the default) indicates the natural id mapping should be cached; {@code false} that the mapping should not be cached.
	 */
	boolean cache() default true;
}
