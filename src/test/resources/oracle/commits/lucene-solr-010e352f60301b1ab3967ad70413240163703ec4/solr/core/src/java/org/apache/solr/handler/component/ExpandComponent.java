/*
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

package org.apache.solr.handler.component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Arrays;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.LongOpenHashSet;
import com.carrotsearch.hppc.LongObjectOpenHashMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.carrotsearch.hppc.cursors.LongCursor;
import com.carrotsearch.hppc.cursors.LongObjectCursor;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.LongObjectMap;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiDocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.uninverting.UninvertingReader;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.LongValues;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ExpandParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.TrieFloatField;
import org.apache.solr.schema.TrieIntField;
import org.apache.solr.schema.TrieLongField;
import org.apache.solr.schema.TrieDoubleField;
import org.apache.solr.schema.StrField;
import org.apache.solr.search.CollapsingQParserPlugin;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrConstantScoreQuery;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;

/**
 * The ExpandComponent is designed to work with the CollapsingPostFilter.
 * The CollapsingPostFilter collapses a result set on a field.
 * <p/>
 * The ExpandComponent expands the collapsed groups for a single page.
 * <p/>
 * http parameters:
 * <p/>
 * expand=true <br/>
 * expand.rows=5 <br/>
 * expand.sort=field asc|desc<br/>
 * expand.q=*:* (optional, overrides the main query)<br/>
 * expand.fq=type:child (optional, overrides the main filter queries)<br/>
 * expand.field=field (mandatory if the not used with the CollapsingQParserPlugin)<br/>
 */
public class ExpandComponent extends SearchComponent implements PluginInfoInitialized, SolrCoreAware {
  public static final String COMPONENT_NAME = "expand";
  private static final int finishingStage = ResponseBuilder.STAGE_GET_FIELDS;
  private PluginInfo info = PluginInfo.EMPTY_INFO;

  @Override
  public void init(PluginInfo info) {
    this.info = info;
  }

  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    if (rb.req.getParams().getBool(ExpandParams.EXPAND, false)) {
      rb.doExpand = true;
    }
  }

  @Override
  public void inform(SolrCore core) {

  }

  @SuppressWarnings("unchecked")
  @Override
  public void process(ResponseBuilder rb) throws IOException {

    if (!rb.doExpand) {
      return;
    }

    SolrQueryRequest req = rb.req;
    SolrParams params = req.getParams();

    String field = params.get(ExpandParams.EXPAND_FIELD);
    String hint = null;
    if (field == null) {
      List<Query> filters = rb.getFilters();
      if (filters != null) {
        for (Query q : filters) {
          if (q instanceof CollapsingQParserPlugin.CollapsingPostFilter) {
            CollapsingQParserPlugin.CollapsingPostFilter cp = (CollapsingQParserPlugin.CollapsingPostFilter) q;
            field = cp.getField();
            hint = cp.hint;
          }
        }
      }
    }

    if (field == null) {
      throw new IOException("Expand field is null.");
    }

    String sortParam = params.get(ExpandParams.EXPAND_SORT);
    String[] fqs = params.getParams(ExpandParams.EXPAND_FQ);
    String qs = params.get(ExpandParams.EXPAND_Q);
    int limit = params.getInt(ExpandParams.EXPAND_ROWS, 5);

    Sort sort = null;

    if (sortParam != null) {
      sort = QueryParsing.parseSortSpec(sortParam, rb.req).getSort();
    }

    Query query;
    if (qs == null) {
      query = rb.getQuery();
    } else {
      try {
        QParser parser = QParser.getParser(qs, null, req);
        query = parser.getQuery();
      } catch (Exception e) {
        throw new IOException(e);
      }
    }

    List<Query> newFilters = new ArrayList<>();

    if (fqs == null) {
      List<Query> filters = rb.getFilters();
      if (filters != null) {
        for (Query q : filters) {
          if (!(q instanceof CollapsingQParserPlugin.CollapsingPostFilter)) {
            newFilters.add(q);
          }
        }
      }
    } else {
      try {
        for (String fq : fqs) {
          if (fq != null && fq.trim().length() != 0 && !fq.equals("*:*")) {
            QParser fqp = QParser.getParser(fq, null, req);
            newFilters.add(fqp.getQuery());
          }
        }
      } catch (Exception e) {
        throw new IOException(e);
      }
    }

    SolrIndexSearcher searcher = req.getSearcher();
    LeafReader reader = searcher.getLeafReader();

    FieldType fieldType = searcher.getSchema().getField(field).getType();

    SortedDocValues values = null;
    long nullValue = 0;

    if(fieldType instanceof StrField) {
      //Get The Top Level SortedDocValues
      if(CollapsingQParserPlugin.HINT_TOP_FC.equals(hint)) {
        Map<String, UninvertingReader.Type> mapping = new HashMap();
        mapping.put(field, UninvertingReader.Type.SORTED);
        UninvertingReader uninvertingReader = new UninvertingReader(new ReaderWrapper(searcher.getLeafReader(), field), mapping);
        values = uninvertingReader.getSortedDocValues(field);
      } else {
        values = DocValues.getSorted(reader, field);
      }
    } else {
      //Get the nullValue for the numeric collapse field
      String defaultValue = searcher.getSchema().getField(field).getDefaultValue();
      if(defaultValue != null) {
        if(fieldType instanceof TrieIntField || fieldType instanceof TrieLongField) {
          nullValue = Long.parseLong(defaultValue);
        } else if(fieldType instanceof TrieFloatField){
          nullValue = Float.floatToIntBits(Float.parseFloat(defaultValue));
        } else if(fieldType instanceof TrieDoubleField){
          nullValue = Double.doubleToLongBits(Double.parseDouble(defaultValue));
        }
      } else {
        if(fieldType instanceof TrieFloatField){
          nullValue = Float.floatToIntBits(0.0f);
        } else if(fieldType instanceof TrieDoubleField){
          nullValue = Double.doubleToLongBits(0.0f);
        }
      }
    }

    FixedBitSet groupBits = null;
    LongOpenHashSet groupSet = null;
    DocList docList = rb.getResults().docList;
    IntOpenHashSet collapsedSet = new IntOpenHashSet(docList.size() * 2);

    //Gather the groups for the current page of documents
    DocIterator idit = docList.iterator();
    int[] globalDocs = new int[docList.size()];
    int docsIndex = -1;
    while (idit.hasNext()) {
      globalDocs[++docsIndex] = idit.nextDoc();
    }

    Arrays.sort(globalDocs);
    Query groupQuery = null;

    /*
    * This code gathers the group information for the current page.
    */
    List<LeafReaderContext> contexts = searcher.getTopReaderContext().leaves();
    int currentContext = 0;
    int currentDocBase = contexts.get(currentContext).docBase;
    int nextDocBase = (currentContext+1)<contexts.size() ? contexts.get(currentContext+1).docBase : Integer.MAX_VALUE;
    IntObjectOpenHashMap<BytesRef> ordBytes = null;
    if(values != null) {
      groupBits = new FixedBitSet(values.getValueCount());
      MultiDocValues.OrdinalMap ordinalMap = null;
      SortedDocValues[] sortedDocValues = null;
      LongValues segmentOrdinalMap = null;
      SortedDocValues currentValues = null;
      if(values instanceof  MultiDocValues.MultiSortedDocValues) {
        ordinalMap = ((MultiDocValues.MultiSortedDocValues)values).mapping;
        sortedDocValues = ((MultiDocValues.MultiSortedDocValues)values).values;
        currentValues = sortedDocValues[currentContext];
        segmentOrdinalMap = ordinalMap.getGlobalOrds(currentContext);
      }
      int count = 0;

      ordBytes = new IntObjectOpenHashMap();

      for(int i=0; i<globalDocs.length; i++) {
        int globalDoc = globalDocs[i];
        while(globalDoc >= nextDocBase) {
          currentContext++;
          currentDocBase = contexts.get(currentContext).docBase;
          nextDocBase = (currentContext+1) < contexts.size() ? contexts.get(currentContext+1).docBase : Integer.MAX_VALUE;
          if(ordinalMap != null) {
            currentValues = sortedDocValues[currentContext];
            segmentOrdinalMap = ordinalMap.getGlobalOrds(currentContext);
          }
        }

        int contextDoc = globalDoc - currentDocBase;
        if(ordinalMap != null) {
          int ord = currentValues.getOrd(contextDoc);
          if(ord > -1) {
            ++count;
            BytesRef ref = currentValues.lookupOrd(ord);
            ord = (int)segmentOrdinalMap.get(ord);
            ordBytes.put(ord, BytesRef.deepCopyOf(ref));
            groupBits.set(ord);
            collapsedSet.add(globalDoc);
          }
        } else {
          int ord = values.getOrd(globalDoc);
          if(ord > -1) {
            ++count;
            BytesRef ref = values.lookupOrd(ord);
            ordBytes.put(ord, BytesRef.deepCopyOf(ref));
            groupBits.set(ord);
            collapsedSet.add(globalDoc);
          }
        }
      }

      if(count > 0 && count < 200) {
        try {
          groupQuery = getGroupQuery(field, count, ordBytes);
        } catch(Exception e) {
          throw new IOException(e);
        }
      }
    } else {
      groupSet = new LongOpenHashSet((int)(docList.size()*1.25));
      NumericDocValues collapseValues = contexts.get(currentContext).reader().getNumericDocValues(field);
      int count = 0;
      for(int i=0; i<globalDocs.length; i++) {
        int globalDoc = globalDocs[i];
        while(globalDoc >= nextDocBase) {
          currentContext++;
          currentDocBase = contexts.get(currentContext).docBase;
          nextDocBase = currentContext+1 < contexts.size() ? contexts.get(currentContext+1).docBase : Integer.MAX_VALUE;
          collapseValues = contexts.get(currentContext).reader().getNumericDocValues(field);
        }
        int contextDoc = globalDoc - currentDocBase;
        long value = collapseValues.get(contextDoc);
        if(value != nullValue) {
          ++count;
          groupSet.add(value);
          collapsedSet.add(globalDoc);
        }
      }

      if(count > 0 && count < 200) {
        groupQuery = getGroupQuery(field, fieldType, count, groupSet);
      }
    }

    Collector collector;
    if (sort != null)
      sort = sort.rewrite(searcher);


    Collector groupExpandCollector = null;

    if(values != null) {
      groupExpandCollector = new GroupExpandCollector(values, groupBits, collapsedSet, limit, sort);
    } else {
      groupExpandCollector = new NumericGroupExpandCollector(field, nullValue, groupSet, collapsedSet, limit, sort);
    }

    if(groupQuery !=  null) {
      //Limits the results to documents that are in the same group as the documents in the page.
      newFilters.add(groupQuery);
    }

    SolrIndexSearcher.ProcessedFilter pfilter = searcher.getProcessedFilter(null, newFilters);
    if (pfilter.postFilter != null) {
      pfilter.postFilter.setLastDelegate(groupExpandCollector);
      collector = pfilter.postFilter;
    } else {
      collector = groupExpandCollector;
    }

    searcher.search(query, pfilter.filter, collector);
    LongObjectMap groups = ((GroupCollector)groupExpandCollector).getGroups();
    Map<String, DocSlice> outMap = new HashMap<>();
    CharsRefBuilder charsRef = new CharsRefBuilder();
    for (LongObjectCursor cursor : (Iterable<LongObjectCursor>) groups) {
      long groupValue = cursor.key;
      TopDocsCollector topDocsCollector = (TopDocsCollector) cursor.value;
      TopDocs topDocs = topDocsCollector.topDocs();
      ScoreDoc[] scoreDocs = topDocs.scoreDocs;
      if (scoreDocs.length > 0) {
        int[] docs = new int[scoreDocs.length];
        float[] scores = new float[scoreDocs.length];
        for (int i = 0; i < docs.length; i++) {
          ScoreDoc scoreDoc = scoreDocs[i];
          docs[i] = scoreDoc.doc;
          scores[i] = scoreDoc.score;
        }
        DocSlice slice = new DocSlice(0, docs.length, docs, scores, topDocs.totalHits, topDocs.getMaxScore());

        if(fieldType instanceof StrField) {
          final BytesRef bytesRef = ordBytes.get((int)groupValue);
          fieldType.indexedToReadable(bytesRef, charsRef);
          String group = charsRef.toString();
          outMap.put(group, slice);
        } else {
          if(fieldType instanceof TrieIntField || fieldType instanceof TrieLongField ) {
            outMap.put(Long.toString(groupValue), slice);
          } else if(fieldType instanceof TrieFloatField) {
            outMap.put(Float.toString(Float.intBitsToFloat((int)groupValue)), slice);
          } else if(fieldType instanceof TrieDoubleField) {
            outMap.put(Double.toString(Double.longBitsToDouble(groupValue)), slice);
          }
        }
      }
    }

    rb.rsp.add("expanded", outMap);
  }

  @Override
  public int distributedProcess(ResponseBuilder rb) throws IOException {
    if (rb.doExpand && rb.stage < finishingStage) {
      return finishingStage;
    }
    return ResponseBuilder.STAGE_DONE;
  }
    
  @Override
  public void modifyRequest(ResponseBuilder rb, SearchComponent who, ShardRequest sreq) {
    SolrParams params = rb.req.getParams();
    if (!params.getBool(COMPONENT_NAME, false)) return;
    if (!rb.onePassDistributedQuery && (sreq.purpose & ShardRequest.PURPOSE_GET_FIELDS) == 0) {
      sreq.params.set(COMPONENT_NAME, "false");
    } else {
      sreq.params.set(COMPONENT_NAME, "true");
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleResponses(ResponseBuilder rb, ShardRequest sreq) {

    if (!rb.doExpand) {
      return;
    }

    if ((sreq.purpose & ShardRequest.PURPOSE_GET_FIELDS) != 0) {
      SolrQueryRequest req = rb.req;
      Map expanded = (Map) req.getContext().get("expanded");
      if (expanded == null) {
        expanded = new HashMap();
        req.getContext().put("expanded", expanded);
      }

      for (ShardResponse srsp : sreq.responses) {
        NamedList response = srsp.getSolrResponse().getResponse();
        Map ex = (Map) response.get("expanded");
        for (Map.Entry<String, SolrDocumentList> entry : (Iterable<Map.Entry<String, SolrDocumentList>>) ex.entrySet()) {
          String name = entry.getKey();
          SolrDocumentList val = entry.getValue();
          expanded.put(name, val);
        }
      }
    }
  }

  @Override
  public void finishStage(ResponseBuilder rb) {

    if (!rb.doExpand) {
      return;
    }

    if (rb.stage != finishingStage) {
      return;
    }

    Map expanded = (Map) rb.req.getContext().get("expanded");
    if (expanded == null) {
      expanded = new HashMap();
    }

    rb.rsp.add("expanded", expanded);
  }

  private class GroupExpandCollector implements Collector, GroupCollector {
    private SortedDocValues docValues;
    private MultiDocValues.OrdinalMap ordinalMap;
    private SortedDocValues segmentValues;
    private LongValues segmentOrdinalMap;
    private MultiDocValues.MultiSortedDocValues multiSortedDocValues;

    private LongObjectMap<Collector> groups;
    private FixedBitSet groupBits;
    private IntOpenHashSet collapsedSet;

    public GroupExpandCollector(SortedDocValues docValues, FixedBitSet groupBits, IntOpenHashSet collapsedSet, int limit, Sort sort) throws IOException {
      int numGroups = collapsedSet.size();
      groups = new LongObjectOpenHashMap<>(numGroups * 2);
      DocIdSetIterator iterator = new BitSetIterator(groupBits, 0); // cost is not useful here
      int group;
      while ((group = iterator.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
        Collector collector = (sort == null) ? TopScoreDocCollector.create(limit) : TopFieldCollector.create(sort, limit, false, false, false);
        groups.put(group, collector);
      }

      this.collapsedSet = collapsedSet;
      this.groupBits = groupBits;
      this.docValues = docValues;
      if(docValues instanceof MultiDocValues.MultiSortedDocValues) {
        this.multiSortedDocValues = (MultiDocValues.MultiSortedDocValues)docValues;
        this.ordinalMap = multiSortedDocValues.mapping;
      }
    }

    @Override
    public boolean needsScores() {
      return true; // TODO: is this always true?
    }

    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
      final int docBase = context.docBase;

      if(ordinalMap != null) {
        this.segmentValues = this.multiSortedDocValues.values[context.ord];
        this.segmentOrdinalMap = ordinalMap.getGlobalOrds(context.ord);
      }

      final LongObjectMap<LeafCollector> leafCollectors = new LongObjectOpenHashMap<>();
      for (LongObjectCursor<Collector> entry : groups) {
        leafCollectors.put(entry.key, entry.value.getLeafCollector(context));
      }
      return new LeafCollector() {

        @Override
        public void setScorer(Scorer scorer) throws IOException {
          for (ObjectCursor<LeafCollector> c : leafCollectors.values()) {
            c.value.setScorer(scorer);
          }
        }

        @Override
        public void collect(int docId) throws IOException {
          int globalDoc = docId + docBase;
          int ord = -1;
          if(ordinalMap != null) {
            ord = segmentValues.getOrd(docId);
            if(ord > -1) {
              ord = (int)segmentOrdinalMap.get(ord);
            }
          } else {
            ord = docValues.getOrd(globalDoc);
          }

          if (ord > -1 && groupBits.get(ord) && !collapsedSet.contains(globalDoc)) {
            LeafCollector c = leafCollectors.get(ord);
            c.collect(docId);
          }
        }
      };
    }

    public LongObjectMap<Collector> getGroups() {
      return groups;
    }
  }

  private class NumericGroupExpandCollector implements Collector, GroupCollector {
    private NumericDocValues docValues;


    private String field;
    private LongObjectOpenHashMap<Collector> groups;

    private IntOpenHashSet collapsedSet;
    private long nullValue;

    public NumericGroupExpandCollector(String field, long nullValue, LongOpenHashSet groupSet, IntOpenHashSet collapsedSet, int limit, Sort sort) throws IOException {
      int numGroups = collapsedSet.size();
      this.nullValue = nullValue;
      groups = new LongObjectOpenHashMap(numGroups * 2);
      Iterator<LongCursor> iterator = groupSet.iterator();
      while (iterator.hasNext()) {
        LongCursor cursor = iterator.next();
        Collector collector = (sort == null) ? TopScoreDocCollector.create(limit) : TopFieldCollector.create(sort, limit, false, false, false);
        groups.put(cursor.value, collector);
      }

      this.field = field;
      this.collapsedSet = collapsedSet;
    }
    
    @Override
    public boolean needsScores() {
      return true; // TODO: is this always true?
    }

    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
      final int docBase = context.docBase;
      this.docValues = context.reader().getNumericDocValues(this.field);

      final LongObjectOpenHashMap<LeafCollector> leafCollectors = new LongObjectOpenHashMap<>();

      for (LongObjectCursor<Collector> entry : groups) {
        leafCollectors.put(entry.key, entry.value.getLeafCollector(context));
      }

      return new LeafCollector() {

        @Override
        public void setScorer(Scorer scorer) throws IOException {
          for (ObjectCursor<LeafCollector> c : leafCollectors.values()) {
            c.value.setScorer(scorer);
          }
        }

        @Override
        public void collect(int docId) throws IOException {
          long value = docValues.get(docId);
          if (value != nullValue && leafCollectors.containsKey(value) && !collapsedSet.contains(docId + docBase)) {
            LeafCollector c = leafCollectors.lget();
            c.collect(docId);
          }
        }
      };
    }

    public LongObjectOpenHashMap<Collector> getGroups() {
      return groups;
    }

  }

  private interface GroupCollector {
    public LongObjectMap getGroups();

  }

  private Query getGroupQuery(String fname,
                           FieldType ft,
                           int size,
                           LongOpenHashSet groupSet) {

    BytesRef[] bytesRefs = new BytesRef[size];
    BytesRefBuilder term = new BytesRefBuilder();
    Iterator<LongCursor> it = groupSet.iterator();
    int index = -1;
    String stringVal =  null;
    while (it.hasNext()) {
      LongCursor cursor = it.next();
      if(ft instanceof TrieIntField || ft instanceof TrieLongField) {
        stringVal = Long.toString(cursor.value);
      } else {
        if(ft instanceof TrieFloatField) {
          stringVal = Float.toString(Float.intBitsToFloat((int)cursor.value));
        } else {
          stringVal = Double.toString(Double.longBitsToDouble(cursor.value));
        }
      }
      ft.readableToIndexed(stringVal, term);
      bytesRefs[++index] = term.toBytesRef();
    }

    return new SolrConstantScoreQuery(new TermsFilter(fname, bytesRefs));
  }

  private Query getGroupQuery(String fname,
                              int size,
                              IntObjectOpenHashMap<BytesRef> ordBytes) throws Exception {

    BytesRef[] bytesRefs = new BytesRef[size];
    int index = -1;
    Iterator<IntObjectCursor<BytesRef>>it = ordBytes.iterator();
    while (it.hasNext()) {
      IntObjectCursor<BytesRef> cursor = it.next();
      bytesRefs[++index] = cursor.value;
    }
    return new SolrConstantScoreQuery(new TermsFilter(fname, bytesRefs));
  }


  ////////////////////////////////////////////
  ///  SolrInfoMBean
  ////////////////////////////////////////////

  @Override
  public String getDescription() {
    return "Expand Component";
  }

  @Override
  public URL[] getDocs() {
    try {
      return new URL[]{
          new URL("http://wiki.apache.org/solr/ExpandComponent")
      };
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private class ReaderWrapper extends FilterLeafReader {

    private String field;

    public ReaderWrapper(LeafReader leafReader, String field) {
      super(leafReader);
      this.field = field;
    }

    public SortedDocValues getSortedDocValues(String field) {
      return null;
    }

    public Object getCoreCacheKey() {
      return in.getCoreCacheKey();
    }

    public FieldInfos getFieldInfos() {
      Iterator<FieldInfo> it = in.getFieldInfos().iterator();
      List<FieldInfo> newInfos = new ArrayList();
      while(it.hasNext()) {
        FieldInfo fieldInfo = it.next();

        if(fieldInfo.name.equals(field)) {
          FieldInfo f = new FieldInfo(fieldInfo.name,
              fieldInfo.number,
              fieldInfo.hasVectors(),
              fieldInfo.hasNorms(),
              fieldInfo.hasPayloads(),
              fieldInfo.getIndexOptions(),
              DocValuesType.NONE,
              fieldInfo.getDocValuesGen(),
              fieldInfo.attributes());
          newInfos.add(f);

        } else {
          newInfos.add(fieldInfo);
        }
      }
      FieldInfos infos = new FieldInfos(newInfos.toArray(new FieldInfo[newInfos.size()]));
      return infos;
    }
  }

}
