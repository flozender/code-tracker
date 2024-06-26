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
package org.apache.solr.spelling;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.lucene.index.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.store.RAMDirectory;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.util.HighFrequencyDictionary;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * <p>
 * A spell checker implementation that loads words from a text file (one word per line).
 * </p>
 *
 * @since solr 1.3
 **/
public class FileBasedSpellChecker extends AbstractLuceneSpellChecker {

  private static final Logger log = LoggerFactory.getLogger(FileBasedSpellChecker.class);

  public static final String SOURCE_FILE_CHAR_ENCODING = "characterEncoding";

  private String characterEncoding;
  public static final String WORD_FIELD_NAME = "word";

  public String init(NamedList config, SolrCore core) {
    super.init(config, core);
    characterEncoding = (String) config.get(SOURCE_FILE_CHAR_ENCODING);
    return name;
  }

  public void build(SolrCore core, SolrIndexSearcher searcher) {
    try {
      loadExternalFileDictionary(core);
      spellChecker.clearIndex();
      spellChecker.indexDictionary(dictionary);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Override to return null, since there is no reader associated with a file based index
   */
  @Override
  protected IndexReader determineReader(IndexReader reader) {
    return null;
  }

  private void loadExternalFileDictionary(SolrCore core) {
    try {

      // Get the field's analyzer
      if (fieldTypeName != null && core.getSchema().getFieldTypeNoEx(fieldTypeName) != null) {
        FieldType fieldType = core.getSchema().getFieldTypes().get(fieldTypeName);
        // Do index-time analysis using the given fieldType's analyzer
        RAMDirectory ramDir = new RAMDirectory();

        LogMergePolicy mp = new LogByteSizeMergePolicy();
        mp.setMergeFactor(300);

        IndexWriter writer = new IndexWriter(
            ramDir,
            new IndexWriterConfig(core.getSolrConfig().luceneMatchVersion, fieldType.getAnalyzer()).
                setMaxBufferedDocs(150).
                setMergePolicy(mp).
                setOpenMode(IndexWriterConfig.OpenMode.CREATE)
        );

        List<String> lines = core.getResourceLoader().getLines(sourceLocation, characterEncoding);

        for (String s : lines) {
          Document d = new Document();
          d.add(new Field(WORD_FIELD_NAME, s, Field.Store.NO, Field.Index.ANALYZED));
          writer.addDocument(d);
        }
        writer.optimize();
        writer.close();

        dictionary = new HighFrequencyDictionary(IndexReader.open(ramDir),
                WORD_FIELD_NAME, 0.0f);
      } else {
        // check if character encoding is defined
        if (characterEncoding == null) {
          dictionary = new PlainTextDictionary(core.getResourceLoader().openResource(sourceLocation));
        } else {
          dictionary = new PlainTextDictionary(new InputStreamReader(core.getResourceLoader().openResource(sourceLocation), characterEncoding));
        }
      }


    } catch (IOException e) {
      log.error( "Unable to load spellings", e);
    }
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }
}
