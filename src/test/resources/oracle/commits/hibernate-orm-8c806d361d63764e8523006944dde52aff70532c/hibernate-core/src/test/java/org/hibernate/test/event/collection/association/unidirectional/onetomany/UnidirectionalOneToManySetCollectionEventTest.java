//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution statements
 * applied by the authors.
 *
 * All third-party contributions are distributed under license by Red Hat
 * Middleware LLC.  This copyrighted material is made available to anyone
 * wishing to use, modify, copy, or redistribute it subject to the terms
 * and conditions of the GNU Lesser General Public License, as published by
 * the Free Software Foundation.  This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.  You should
 * have received a copy of the GNU Lesser General Public License along with
 * this distribution; if not, write to: Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor Boston, MA  02110-1301  USA
 */
package org.hibernate.test.event.collection.association.unidirectional.onetomany;
import java.util.Collection;
import java.util.HashSet;
import junit.framework.Test;
import org.hibernate.test.event.collection.ParentWithCollection;
import org.hibernate.test.event.collection.association.AbstractAssociationCollectionEventTest;
import org.hibernate.test.event.collection.association.unidirectional.ParentWithCollectionOfEntities;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 *
 * @author Gail Badner
 */
public class UnidirectionalOneToManySetCollectionEventTest extends AbstractAssociationCollectionEventTest {

	public UnidirectionalOneToManySetCollectionEventTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( UnidirectionalOneToManySetCollectionEventTest.class );
	}

	public String[] getMappings() {
		return new String[] { "event/collection/association/unidirectional/onetomany/UnidirectionalOneToManySetMapping.hbm.xml" };
	}

	public ParentWithCollection createParent(String name) {
		return new ParentWithCollectionOfEntities( name );
	}

	public Collection createCollection() {
		return new HashSet();
	}	
}

