package org.apache.lucene.spatial;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.spatial.bbox.BBoxStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.TermQueryPrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.QuadPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.serialized.SerializedDVStrategy;
import org.apache.lucene.spatial.vector.PointVectorStrategy;
import org.junit.Test;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

public class DistanceStrategyTest extends StrategyTestCase {

  @ParametersFactory
  public static Iterable<Object[]> parameters() {
    List<Object[]> ctorArgs = new ArrayList<>();

    SpatialContext ctx = SpatialContext.GEO;
    SpatialPrefixTree grid;
    SpatialStrategy strategy;

    grid = new QuadPrefixTree(ctx,25);
    strategy = new RecursivePrefixTreeStrategy(grid, "recursive_quad");
    ctorArgs.add(new Object[]{new Param(strategy)});

    grid = new GeohashPrefixTree(ctx,12);
    strategy = new TermQueryPrefixTreeStrategy(grid, "termquery_geohash");
    ctorArgs.add(new Object[]{new Param(strategy)});

    strategy = new PointVectorStrategy(ctx, "pointvector");
    ctorArgs.add(new Object[]{new Param(strategy)});

    strategy = new BBoxStrategy(ctx, "bbox");
    ctorArgs.add(new Object[]{new Param(strategy)});

    strategy = new SerializedDVStrategy(ctx, "serialized");
    ctorArgs.add(new Object[]{new Param(strategy)});

    return ctorArgs;
  }

  // this is a hack for clover!
  static class Param {
    SpatialStrategy strategy;

    Param(SpatialStrategy strategy) {
      this.strategy = strategy;
    }

    @Override
    public String toString() {
      return strategy.getFieldName();
    }
  }

//  private String fieldName;

  public DistanceStrategyTest(@Name("strategy") Param param) {
    SpatialStrategy strategy = param.strategy;
    this.ctx = strategy.getSpatialContext();
    this.strategy = strategy;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    if (strategy instanceof BBoxStrategy && random().nextBoolean()) {//disable indexing sometimes
      BBoxStrategy bboxStrategy = (BBoxStrategy)strategy;
      final FieldType fieldType = new FieldType(bboxStrategy.getFieldType());
      fieldType.setIndexOptions(IndexOptions.NO);
      bboxStrategy.setFieldType(fieldType);
    }
  }

  @Override
  protected boolean needsDocValues() {
    return true;
  }

  @Test
  public void testDistanceOrder() throws IOException {
    adoc("100", ctx.makePoint(2, 1));
    adoc("101", ctx.makePoint(-1, 4));
    adoc("103", (Shape)null);//test score for nothing
    adoc("999", ctx.makePoint(2, 1));//test deleted
    commit();
    deleteDoc("999");
    commit();
    //FYI distances are in docid order
    checkDistValueSource(ctx.makePoint(4, 3), 2.8274937f, 5.0898066f, 180f);
    checkDistValueSource(ctx.makePoint(0, 4), 3.6043684f, 0.9975641f, 180f);
  }

  @Test
  public void testRecipScore() throws IOException {
    Point p100 = ctx.makePoint(2, 1);
    adoc("100", p100);
    Point p101 = ctx.makePoint(-1, 4);
    adoc("101", p101);
    adoc("103", (Shape)null);//test score for nothing
    adoc("999", ctx.makePoint(2, 1));//test deleted
    commit();
    deleteDoc("999");
    commit();

    double dist = ctx.getDistCalc().distance(p100, p101);
    Shape queryShape = ctx.makeCircle(2.01, 0.99, dist);
    checkValueSource(strategy.makeRecipDistanceValueSource(queryShape),
        new float[]{1.00f, 0.10f, 0f}, 0.09f);
  }

  void checkDistValueSource(Point pt, float... distances) throws IOException {
    float multiplier = random().nextFloat() * 100f;
    float[] dists2 = Arrays.copyOf(distances, distances.length);
    for (int i = 0; i < dists2.length; i++) {
      dists2[i] *= multiplier;
    }
    checkValueSource(strategy.makeDistanceValueSource(pt, multiplier), dists2, 1.0e-3f);
  }
}
