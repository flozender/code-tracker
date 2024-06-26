package org.hibernate.search;

import java.util.Set;

import org.jcp.xml.dsig.internal.dom.DOMXMLSignature;

import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * @author Emmanuel Bernard
 */
public interface InitContext {
	/**
	 * Returns the SessionFactoryImplementor instance. Do not use until after the initialize method is fully executed.
	 * Implementations should not cache values provided by the SessionFactoryImplementor but rather access them
	 * each time: when the configuration is dynamically updated, new changes are available through the
	 * SearchFactoryImplementor
	 * For example, prefer
	 * <code>
	 * void method() {
	 *   int size = sfi.getDirectoryProviders().size();
	 * }
	 * </code>
	 * to
	 * <code>
	 * void method() {
	 *   int size = directoryProviders().size();
	 * }
	 * </code>
	 * where directoryProviders is a class variable. 
	 */
	SearchFactoryImplementor getUninitializedSearchFactory();

	String getIndexingStrategy();

	Set<DirectoryProvider<?>> getDirectoryProviders();
}
