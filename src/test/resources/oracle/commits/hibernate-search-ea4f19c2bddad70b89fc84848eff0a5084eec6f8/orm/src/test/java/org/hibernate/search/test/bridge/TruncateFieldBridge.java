/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.bridge;

import org.apache.lucene.document.Document;
//Fieldable was removed in Lucene 4 with no alternative replacement
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

/**
 * @author Emmanuel Bernard
 */
public class TruncateFieldBridge implements FieldBridge {

	public Object get(String name, Document document) {
		Fieldable field = document.getFieldable( name );
		return field.stringValue();
	}

	@Override
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String stringValue = (String) value;
		if ( stringValue != null ) {
			String indexedString = stringValue.substring( 0, stringValue.length() / 2 );
			luceneOptions.addFieldToDocument( name, indexedString, document );
		}
	}

}
