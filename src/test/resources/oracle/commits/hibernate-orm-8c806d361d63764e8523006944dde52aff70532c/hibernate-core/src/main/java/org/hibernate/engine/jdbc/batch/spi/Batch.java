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
package org.hibernate.engine.jdbc.batch.spi;
import java.sql.PreparedStatement;
import org.hibernate.jdbc.Expectation;

/**
 * Conceptually models a batch.
 * <p/>
 * Unlike directly in JDBC, here we add the ability to batch together multiple statements at a time.  In the underlying
 * JDBC this correlates to multiple {@link java.sql.PreparedStatement} objects (one for each DML string) maintained within the
 * batch.
 *
 * @author Steve Ebersole
 */
public interface Batch {
	/**
	 * Retrieves the object being used to key (uniquely identify) this batch.
	 *
	 * @return The batch key.
	 */
	public Object getKey();

	/**
	 * Adds an observer to this batch.
	 *
	 * @param observer The batch observer.
	 */
	public void addObserver(BatchObserver observer);

	/**
	 * Get a statement which is part of the batch.
	 *
	 * @param sql The SQL statement.
	 * @return the prepared statement representing the SQL statement, if the batch contained it;
	 *         null, otherwise.
	 */
	public PreparedStatement getBatchStatement(Object key, String sql);

	/**
	 * Add a prepared statement to the batch.
	 *
	 * @param sql The SQL statement.
	 */
	public void addBatchStatement(Object key, String sql, PreparedStatement preparedStatement);


	/**
	 * Indicates completion of the current part of the batch.
	 *
	 * @param key
	 * @param sql
	 * @param expectation The expectation for the part's result.
	 */
	public void addToBatch(Object key, String sql, Expectation expectation);

	/**
	 * Execute this batch.
	 */
	public void execute();

	/**
	 * Used to indicate that the batch instance is no longer needed and that, therefore, it can release its
	 * resources.
	 */
	public void release();
}

