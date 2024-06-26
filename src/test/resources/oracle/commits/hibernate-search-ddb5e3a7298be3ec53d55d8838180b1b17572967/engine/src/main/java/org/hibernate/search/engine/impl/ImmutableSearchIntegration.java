/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.impl;

import org.hibernate.search.engine.integration.impl.SearchIntegration;

/**
 * @author Yoann Rodiere
 */
public class ImmutableSearchIntegration implements SearchIntegration {

	private final AnalyzerRegistry analyzerRegistry;

	public ImmutableSearchIntegration(AnalyzerRegistry analyzerRegistry) {
		super();
		this.analyzerRegistry = new ImmutableAnalyzerRegistry( analyzerRegistry );
	}

	@Override
	public AnalyzerRegistry getAnalyzerRegistry() {
		return analyzerRegistry;
	}

	@Override
	public void close() {
		analyzerRegistry.close();
	}

}
