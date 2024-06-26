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

//$Id$
package org.hibernate.test.annotations.various;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.TimestampType;

/**
 * Test for the @Timestamp annotation.
 *
 * @author Hardy Ferentschik
 */
public class TimestampTest extends TestCase {

	public void testTimestampSourceIsVM() throws Exception {
		assertTimestampSource( VMTimestamped.class, TimestampType.class );
	}

	public void testTimestampSourceIsDB() throws Exception {
		assertTimestampSource( DBTimestamped.class, DbTimestampType.class );
	}

	private void assertTimestampSource(Class<?> clazz, Class<?> expectedTypeClass) throws Exception {
		buildConfiguration();
		ClassMetadata meta = sessions.getClassMetadata( clazz );
		assertTrue( "Entity is annotated with @Timestamp and should hence be versioned", meta.isVersioned() );

		PersistentClass persistentClass = cfg.getClassMapping( clazz.getName() );
		assertNotNull( persistentClass );
		Property versionProperty = persistentClass.getVersion();
		assertNotNull( versionProperty );
		assertEquals( "Wrong timestamp type", expectedTypeClass, versionProperty.getType().getClass() );
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				VMTimestamped.class, DBTimestamped.class
		};
	}
}
