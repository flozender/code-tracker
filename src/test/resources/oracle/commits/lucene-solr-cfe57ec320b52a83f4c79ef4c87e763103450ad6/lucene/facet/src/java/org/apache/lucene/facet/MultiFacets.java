package org.apache.lucene.facet;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Maps specified dims to provided Facets impls; else, uses
 *  the default Facets impl. */
public class MultiFacets extends Facets {
  private final Map<String,Facets> dimToFacets;
  private final Facets defaultFacets;

  public MultiFacets(Map<String,Facets> dimToFacets) {
    this(dimToFacets, null);
  }

  public MultiFacets(Map<String,Facets> dimToFacets, Facets defaultFacets) {
    this.dimToFacets = dimToFacets;
    this.defaultFacets = defaultFacets;
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... path) throws IOException {
    Facets facets = dimToFacets.get(dim);
    if (facets == null) {
      if (defaultFacets == null) {
        throw new IllegalArgumentException("invalid dim \"" + dim + "\"");
      }
      facets = defaultFacets;
    }
    return facets.getTopChildren(topN, dim, path);
  }

  @Override
  public Number getSpecificValue(String dim, String... path) throws IOException {
    Facets facets = dimToFacets.get(dim);
    if (facets == null) {
      if (defaultFacets == null) {
        throw new IllegalArgumentException("invalid dim \"" + dim + "\"");
      }
      facets = defaultFacets;
    }
    return facets.getSpecificValue(dim, path);
  }

  @Override
  public List<FacetResult> getAllDims(int topN) throws IOException {
    // TODO
    throw new UnsupportedOperationException();
  }
}
