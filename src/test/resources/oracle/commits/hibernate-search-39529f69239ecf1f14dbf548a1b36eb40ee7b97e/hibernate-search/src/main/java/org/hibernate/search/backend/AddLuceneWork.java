/* $Id$
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
package org.hibernate.search.backend;

import java.io.Serializable;
import java.util.Map;

import org.apache.lucene.document.Document;

/**
 * @author Emmanuel Bernard
 */
public class AddLuceneWork extends LuceneWork implements Serializable {

	private static final long serialVersionUID = -2450349312813297371L;

	private final Map<String, String> fieldToAnalyzerMap;

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document) {
		this( id, idInString, entity, document, false );
	}

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document, boolean batch) {
		this( id, idInString, entity, document, null, batch );
	}

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document, Map<String, String> fieldToAnalyzerMap) {
		this( id, idInString, entity, document, fieldToAnalyzerMap, false );
	}

	public AddLuceneWork(Serializable id, String idInString, Class entity, Document document, Map<String, String> fieldToAnalyzerMap, boolean batch) {
		super( id, idInString, entity, document, batch );
		this.fieldToAnalyzerMap = fieldToAnalyzerMap;
	}

	public Map<String, String> getFieldToAnalyzerMap() {
		return fieldToAnalyzerMap;
	}

	@Override
	public <T> T getWorkDelegate(final WorkVisitor<T> visitor) {
		return visitor.getDelegate( this );
	}

}
