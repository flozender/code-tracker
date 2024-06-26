/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.filters;

import com.intellij.openapi.util.Key;

/**
 * @author egor
 */
public interface LineNumbersMapping {
  /**
   * A mapping between lines contained in a byte code and actual source lines
   * (placed into a user data of a VirtualFile for a .class file).
   */
  Key<LineNumbersMapping> LINE_NUMBERS_MAPPING_KEY = Key.create("line.numbers.mapping.key");

  int map(int line);
  int unmap(int line);
}
