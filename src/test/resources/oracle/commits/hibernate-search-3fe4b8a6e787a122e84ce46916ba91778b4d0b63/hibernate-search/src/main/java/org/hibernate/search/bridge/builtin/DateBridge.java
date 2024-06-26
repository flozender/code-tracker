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
package org.hibernate.search.bridge.builtin;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.document.DateTools;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.search.SearchException;
import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Bridge a java.util.Date to a String, truncated to the resolution
 * Date are stored GMT based
 * <p/>
 * ie
 * Resolution.YEAR: yyyy
 * Resolution.MONTH: yyyyMM
 * Resolution.DAY: yyyyMMdd
 * Resolution.HOUR: yyyyMMddHH
 * Resolution.MINUTE: yyyyMMddHHmm
 * Resolution.SECOND: yyyyMMddHHmmss
 * Resolution.MILLISECOND: yyyyMMddHHmmssSSS
 *
 * @author Emmanuel Bernard
 */
//TODO split into StringBridge and TwoWayStringBridge?
public class DateBridge implements TwoWayStringBridge, ParameterizedBridge {

	public static final TwoWayStringBridge DATE_YEAR = new DateBridge( Resolution.YEAR );
	public static final TwoWayStringBridge DATE_MONTH = new DateBridge( Resolution.MONTH );
	public static final TwoWayStringBridge DATE_DAY = new DateBridge( Resolution.DAY );
	public static final TwoWayStringBridge DATE_HOUR = new DateBridge( Resolution.HOUR );
	public static final TwoWayStringBridge DATE_MINUTE = new DateBridge( Resolution.MINUTE );
	public static final TwoWayStringBridge DATE_SECOND = new DateBridge( Resolution.SECOND );
	public static final TwoWayStringBridge DATE_MILLISECOND = new DateBridge( Resolution.MILLISECOND );

	private DateTools.Resolution resolution;

	public DateBridge() {
	}

	public DateBridge(Resolution resolution) {
		this.resolution = DateResolutionUtil.getLuceneResolution(resolution);
	}

	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		try {
			return DateTools.stringToDate( stringValue );
		}
		catch (ParseException e) {
			throw new SearchException( "Unable to parse into date: " + stringValue, e );
		}
	}

	public String objectToString(Object object) {
		return object != null ?
				DateTools.dateToString( (Date) object, resolution ) :
				null;
	}

	public void setParameterValues(Map parameters) {
		Object resolution = parameters.get( "resolution" );
		Resolution hibResolution;
		if ( resolution instanceof String ) {
			hibResolution = Resolution.valueOf( ( (String) resolution ).toUpperCase( Locale.ENGLISH ) );
		}
		else {
			hibResolution = (Resolution) resolution;
		}
		this.resolution = DateResolutionUtil.getLuceneResolution( hibResolution );
	}
	
}
