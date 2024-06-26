/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.sql.Connection;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Represents a consolidation of all session creation options into a builder style delegate.
 * 
 * @author Steve Ebersole
 */
public interface SessionBuilder<T extends SessionBuilder> {
	/**
	 * Opens a session with the specified options.
	 *
	 * @return The session
	 */
	Session openSession();

	/**
	 * Adds a specific interceptor to the session options.
	 *
	 * @param interceptor The interceptor to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T interceptor(Interceptor interceptor);

	/**
	 * Signifies that no {@link Interceptor} should be used.
	 * <p/>
	 * By default the {@link Interceptor} associated with the {@link SessionFactory} is passed to the
	 * {@link Session} whenever we open one without the user having specified a specific interceptor to
	 * use.
	 * <p/>
	 * Calling {@link #interceptor(Interceptor)} with null has the same net effect.
	 *
	 * @return {@code this}, for method chaining
	 */
	T noInterceptor();

	/**
	 * Applies a specific StatementInspector to the session options.
	 *
	 * @param statementInspector The StatementInspector to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T statementInspector(StatementInspector statementInspector);

	/**
	 * Adds a specific connection to the session options.
	 *
	 * @param connection The connection to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connection(Connection connection);

	/**
	 * Use a specific connection release mode for these session options.
	 *
	 * @param connectionReleaseMode The connection release mode to use.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated (since 6.0) use {@link #connectionHandlingMode} instead
	 */
	@Deprecated
	T connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode);

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @param mode The connection handling mode to use.
	 *
	 * @return {@code this}, for method chaining
	 */
	T connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	/**
	 * Should the session built automatically join in any ongoing JTA transactions.
	 *
	 * @param autoJoinTransactions Should JTA transactions be automatically joined
	 *
	 * @return {@code this}, for method chaining
	 */
	T autoJoinTransactions(boolean autoJoinTransactions);

	/**
	 * Should the session be automatically closed afterQuery transaction completion.
	 *
	 * @param autoClose Should the session be automatically closed
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated Only integrations can specify autoClosing behavior of individual sessions.  See
	 * {@link org.hibernate.engine.spi.SessionOwner}
	 */
	@Deprecated
	T autoClose(boolean autoClose);

	/**
	 * Should the session be automatically flushed during the "beforeQuery completion" phase of transaction handling.
	 *
	 * @param flushBeforeCompletion Should the session be automatically flushed
	 *
	 * @return {@code this}, for method chaining
	 */
	T flushBeforeCompletion(boolean flushBeforeCompletion);

	/**
	 * Define the tenant identifier to be associated with the opened session.
	 *
	 * @param tenantIdentifier The tenant identifier.
	 *
	 * @return {@code this}, for method chaining
	 */
	T tenantIdentifier(String tenantIdentifier);

	/**
	 * Apply one or more SessionEventListener instances to the listeners for the Session to be built.
	 *
	 * @param listeners The listeners to incorporate into the built Session
	 *
	 * @return {@code this}, for method chaining
	 */
	T eventListeners(SessionEventListener... listeners);

	/**
	 * Remove all listeners intended for the built Session currently held here, including any auto-apply ones; in other
	 * words, start with a clean slate.
	 *
	 * {@code this}, for method chaining
	 */
	T clearEventListeners();

	T persistenceUnitTransactionType(PersistenceUnitTransactionType persistenceUnitTransactionType);

}
