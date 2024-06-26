//$Id$
package org.hibernate.search.test.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;

/**
 * Apply a security filter to the results
 *
 * @author Emmanuel Bernard
 */
public class SecurityFilterFactory {
	private String login;

	/**
	 * injected parameter
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	@Key
	public FilterKey getKey() {
		StandardFilterKey key = new StandardFilterKey();
		key.addParameter( login );
		return key;
	}

	@Factory
	public Filter getFilter() {
		Query query = new TermQuery( new Term("teacher", login) );
		return new QueryWrapperFilter(query);
	}
}
