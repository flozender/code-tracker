/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.collectionelement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import org.hibernate.Filter;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.test.annotations.Country;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class CollectionElementTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testSimpleElement() throws Exception {
		assertEquals(
				"BoyFavoriteNumbers",
				configuration().getCollectionMapping( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);
		Session s = openSession();
		s.getTransaction().begin();
		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		boy.getNickNames().add( "Johnny" );
		boy.getNickNames().add( "Thing" );
		boy.getScorePerNickName().put( "Johnny", 3 );
		boy.getScorePerNickName().put( "Thing", 5 );
		int[] favNbrs = new int[4];
		for (int index = 0; index < favNbrs.length - 1; index++) {
			favNbrs[index] = index * 3;
		}
		boy.setFavoriteNumbers( favNbrs );
		boy.getCharacters().add( Character.GENTLE );
		boy.getCharacters().add( Character.CRAFTY );

		HashMap<String,FavoriteFood> foods = new HashMap<String,FavoriteFood>();
		foods.put( "breakfast", FavoriteFood.PIZZA);
		foods.put( "lunch", FavoriteFood.KUNGPAOCHICKEN);
		foods.put( "dinner", FavoriteFood.SUSHI);
		boy.setFavoriteFood(foods);
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertNotNull( boy.getNickNames() );
		assertTrue( boy.getNickNames().contains( "Thing" ) );
		assertNotNull( boy.getScorePerNickName() );
		assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
		assertEquals( Integer.valueOf( 5 ), boy.getScorePerNickName().get( "Thing" ) );
		assertNotNull( boy.getFavoriteNumbers() );
		assertEquals( 3, boy.getFavoriteNumbers()[1] );
		assertTrue( boy.getCharacters().contains( Character.CRAFTY ) );
		assertTrue( boy.getFavoriteFood().get("dinner").equals(FavoriteFood.SUSHI));
		assertTrue( boy.getFavoriteFood().get("lunch").equals(FavoriteFood.KUNGPAOCHICKEN));
		assertTrue( boy.getFavoriteFood().get("breakfast").equals(FavoriteFood.PIZZA));
		List result = s.createQuery( "select boy from Boy boy join boy.nickNames names where names = :name" )
				.setParameter( "name", "Thing" ).list();
		assertEquals( 1, result.size() );
		s.delete( boy );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositeElement() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		Toy toy = new Toy();
		toy.setName( "Balloon" );
		toy.setSerial( "serial001" );
		toy.setBrand( new Brand() );
		toy.getBrand().setName( "Bandai" );
		boy.getFavoriteToys().add( toy );
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertNotNull( boy );
		assertNotNull( boy.getFavoriteToys() );
		assertTrue( boy.getFavoriteToys().contains( toy ) );
		assertEquals( "@Parent is failing", boy, boy.getFavoriteToys().iterator().next().getOwner() );
		s.delete( boy );
		tx.commit();
		s.close();
	}

	@Test
	public void testAttributedJoin() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Country country = new Country();
		country.setName( "Australia" );
		s.persist( country );

		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		CountryAttitude attitude = new CountryAttitude();
		// TODO: doesn't work
		attitude.setBoy( boy );
		attitude.setCountry( country );
		attitude.setLikes( true );
		boy.getCountryAttitudes().add( attitude );
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();

		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertTrue( boy.getCountryAttitudes().contains( attitude ) );
		s.delete( boy );
		s.delete( s.get( Country.class, country.getId() ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testLazyCollectionofElements() throws Exception {
		assertEquals(
				"BoyFavoriteNumbers",
				configuration().getCollectionMapping( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);
		Session s = openSession();
		s.getTransaction().begin();
		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		boy.getNickNames().add( "Johnny" );
		boy.getNickNames().add( "Thing" );
		boy.getScorePerNickName().put( "Johnny", 3 );
		boy.getScorePerNickName().put( "Thing", 5 );
		int[] favNbrs = new int[4];
		for (int index = 0; index < favNbrs.length - 1; index++) {
			favNbrs[index] = index * 3;
		}
		boy.setFavoriteNumbers( favNbrs );
		boy.getCharacters().add( Character.GENTLE );
		boy.getCharacters().add( Character.CRAFTY );
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertNotNull( boy.getNickNames() );
		assertTrue( boy.getNickNames().contains( "Thing" ) );
		assertNotNull( boy.getScorePerNickName() );
		assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
		assertEquals( new Integer( 5 ), boy.getScorePerNickName().get( "Thing" ) );
		assertNotNull( boy.getFavoriteNumbers() );
		assertEquals( 3, boy.getFavoriteNumbers()[1] );
		assertTrue( boy.getCharacters().contains( Character.CRAFTY ) );
		List result = s.createQuery( "select boy from Boy boy join boy.nickNames names where names = :name" )
				.setParameter( "name", "Thing" ).list();
		assertEquals( 1, result.size() );
		s.delete( boy );
		tx.commit();
		s.close();
	}

	@Test
	public void testFetchEagerAndFilter() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		TestCourse test = new TestCourse();

		LocalizedString title = new LocalizedString( "title in english" );
		title.getVariations().put( Locale.FRENCH.getLanguage(), "title en francais" );
		test.setTitle( title );
		s.save( test );

		s.flush();
		s.clear();

		Filter filter = s.enableFilter( "selectedLocale" );
		filter.setParameter( "param", "fr" );

		Query q = s.createQuery( "from TestCourse t" );
		List l = q.list();
		assertEquals( 1, l.size() );

		TestCourse t = (TestCourse) s.get( TestCourse.class, test.getTestCourseId() );
		assertEquals( 1, t.getTitle().getVariations().size() );

		tx.rollback();

		s.close();
	}

	@Test
	public void testMapKeyType() throws Exception {
		Matrix m = new Matrix();
		m.getMvalues().put( 1, 1.1f );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( m );
		s.flush();
		s.clear();
		m = (Matrix) s.get( Matrix.class, m.getId() );
		assertEquals( 1.1f, m.getMvalues().get( 1 ), 0.01f );
		tx.rollback();
		s.close();
	}

	@Test
	public void testDefaultValueColumnForBasic() throws Exception {
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "hatedNames" );
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "preferredNames" );
		isCollectionColumnPresent( Boy.class.getName(), "nickNames", "nickNames" );
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "scorePerPreferredName");
	}

	private void isDefaultValueCollectionColumnPresent(String collectionOwner, String propertyName) {
		isCollectionColumnPresent( collectionOwner, propertyName, propertyName );
	}

	private void isCollectionColumnPresent(String collectionOwner, String propertyName, String columnName) {
		final Collection collection = configuration().getCollectionMapping( collectionOwner + "." + propertyName );
		final Iterator columnIterator = collection.getCollectionTable().getColumnIterator();
		boolean hasDefault = false;
		while ( columnIterator.hasNext() ) {
			Column column = (Column) columnIterator.next();
			if ( columnName.equals( column.getName() ) ) hasDefault = true;
		}
		assertTrue( "Could not find " + columnName, hasDefault );
	}


	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameNoOverrides() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Products has @Entity (no @Table)
		checkDefaultCollectionTableName( BugSystem.class, "bugs", "BugSystem_bugs" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		checkDefaultCollectionTableName( Boy.class, "hatedNames", "Boy_hatedNames" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	@FailureExpected( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		checkDefaultCollectionTableName( Matrix.class, "mvalues", "Mtx_mvalues" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9387")
	@FailureExpected( jiraKey = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).


		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		checkDefaultCollectionTableName( Owner.class, "elements", "OWNER_elements" );
	}

	private void checkDefaultCollectionTableName(
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String expectedCollectionTableName) {
		final org.hibernate.mapping.Collection collection = configuration().getCollectionMapping(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		final org.hibernate.mapping.Table table = collection.getCollectionTable();
		assertEquals( expectedCollectionTableName, table.getName() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnNoOverrides() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Products has @Entity (no @Table)
		checkDefaultJoinColumnName( BugSystem.class, "bugs", "BugSystem_id" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Boy has @Entity @Table(name="tbl_Boys")
		checkDefaultJoinColumnName( Boy.class, "hatedNames", "Boy_id" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	@FailureExpected( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		checkDefaultJoinColumnName( Matrix.class, "mvalues", "Mtx_mId" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9389")
	@FailureExpected( jiraKey = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride() {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).


		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		checkDefaultJoinColumnName( Owner.class, "elements", "OWNER_id" );
	}

	private void checkDefaultJoinColumnName(
			Class<?> ownerEntityClass,
			String ownerCollectionPropertyName,
			String ownerForeignKeyNameExpected) {
		final org.hibernate.mapping.Collection ownerCollection = configuration().getCollectionMapping(
				ownerEntityClass.getName() + '.' + ownerCollectionPropertyName
		);
		// The default owner join column can only be computed if it has a PK with 1 column.
		assertEquals ( 1, ownerCollection.getOwner().getKey().getColumnSpan() );
		assertEquals( ownerForeignKeyNameExpected, ownerCollection.getKey().getColumnIterator().next().getText() );

		boolean hasOwnerFK = false;
		for ( Iterator it=ownerCollection.getCollectionTable().getForeignKeyIterator(); it.hasNext(); ) {
			final ForeignKey fk = (ForeignKey) it.next();
			assertSame( ownerCollection.getCollectionTable(), fk.getTable() );
			if ( fk.getColumnSpan() > 1 ) {
				continue;
			}
			if ( fk.getColumn( 0 ).getText().equals( ownerForeignKeyNameExpected ) ) {
				assertSame( ownerCollection.getOwner().getTable(), fk.getReferencedTable() );
				hasOwnerFK = true;
			}
		}
		assertTrue( hasOwnerFK );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Boy.class,
				Country.class,
				TestCourse.class,
				Matrix.class,
				Owner.class,
				BugSystem.class
		};
	}
}
