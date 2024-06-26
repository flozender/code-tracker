package org.apache.lucene.analysis.sinks;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.analysis.TeeSinkTokenFilter.SinkFilter;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * Attempts to parse the {@link org.apache.lucene.analysis.Token#termBuffer()} as a Date using a {@link java.text.DateFormat}.
 * If the value is a Date, it will add it to the sink.
 * <p/> 
 *
 **/
public class DateRecognizerSinkFilter extends SinkFilter {
  public static final String DATE_TYPE = "date";

  protected DateFormat dateFormat;
  protected TermAttribute termAtt;
  
  /**
   * Uses {@link java.text.SimpleDateFormat#getDateInstance()} as the {@link java.text.DateFormat} object.
   */
  public DateRecognizerSinkFilter() {
    this(SimpleDateFormat.getDateInstance());
  }
  
  public DateRecognizerSinkFilter(DateFormat dateFormat) {
    this.dateFormat = dateFormat; 
  }

  public boolean accept(AttributeSource source) {
    if (termAtt == null) {
      termAtt = source.addAttribute(TermAttribute.class);
    }
    try {
      Date date = dateFormat.parse(termAtt.term());//We don't care about the date, just that we can parse it as a date
      if (date != null) {
        return true;
      }
    } catch (ParseException e) {
  
    }
    
    return false;
  }

}
