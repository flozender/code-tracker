/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.spi.ClassHelper;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class SimpleClassHelper implements ClassHelper {

	@Override
	public Object unproxy(Object entity) {
		return null;
	}

	@Override
	public <T> Class<T> getClassFromWork(Work<T> work) {
		return null;
	}

	@Override
	public <T> Class<T> getClass(T entity) {
		return null;
	}

	@Override
	public <T> Collection<T> initializeCollection(Collection<T> value) {
		return null;
	}

	@Override
	public <K, V> Map<K, V> initializeMap(Map<K, V> value) {
		return null;
	}

	@Override
	public Object[] initializeArray(Object[] value) {
		return null;
	}

}
