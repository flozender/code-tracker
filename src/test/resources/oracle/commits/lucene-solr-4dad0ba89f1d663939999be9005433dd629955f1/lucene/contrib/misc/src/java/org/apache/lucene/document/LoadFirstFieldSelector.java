package org.apache.lucene.document;
/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Load the First field and break.
 * <p/>
 * See {@link FieldSelectorResult#LOAD_AND_BREAK}
 */
public class LoadFirstFieldSelector implements FieldSelector {

  public FieldSelectorResult accept(String fieldName) {
    return FieldSelectorResult.LOAD_AND_BREAK;
  }
}
