/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

/*
 * User: anna
 * Date: 19-Dec-2006
 */
package com.intellij.compiler.ant.taskdefs;

import com.intellij.compiler.ant.Tag;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NonNls;

public class Zip extends Tag {
  public Zip(@NonNls final String destFile) {
    //noinspection HardCodedStringLiteral
    super("zip", new Pair[] {Pair.create("destfile", destFile)});
  }
}