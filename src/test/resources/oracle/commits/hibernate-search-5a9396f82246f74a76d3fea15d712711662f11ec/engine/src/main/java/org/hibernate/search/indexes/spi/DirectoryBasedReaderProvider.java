/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.spi;

import java.util.Properties;

import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public interface DirectoryBasedReaderProvider extends ReaderProvider {

	void initialize(DirectoryBasedIndexManager indexManager, Properties props);

	void stop();

}
