/* $Id: DynamicBoostedDescriptionLibrary.java 17630 2009-10-06 13:38:43Z sannegrinovero $
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.configuration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Test entity using a custom <code>CustomBoostStrategy</code> to set
 * the document boost as the dynScore field.
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
@Entity
public class DynamicBoostedDescLibrary {

	@Id
	@GeneratedValue
	private int libraryId;
	private float dynScore;
	private String name;

	public DynamicBoostedDescLibrary() {
		dynScore = 1.0f;
	}

	
	public int getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(int id) {
		this.libraryId = id;
	}

	public float getDynScore() {
		return dynScore;
	}

	public void setDynScore(float dynScore) {
		this.dynScore = dynScore;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
