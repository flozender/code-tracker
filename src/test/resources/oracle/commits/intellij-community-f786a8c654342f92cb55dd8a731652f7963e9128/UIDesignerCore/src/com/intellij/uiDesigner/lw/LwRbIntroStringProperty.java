/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.uiDesigner.lw;

import org.jdom.Element;
import com.intellij.uiDesigner.UIFormXmlConstants;

/**
 * @author Vladimir Kondratyev
 */
public final class LwRbIntroStringProperty extends LwIntrospectedProperty {
  public LwRbIntroStringProperty(final String name){
    super(name, String.class.getName());
  }

  /**
   * @return instance of {@link com.intellij.uiDesigner.lw.StringDescriptor}
   */
  public Object read(final Element element) throws Exception{
    final StringDescriptor descriptor = LwXmlReader.getStringDescriptor(element,
                                                                        UIFormXmlConstants.ATTRIBUTE_VALUE,
                                                                        UIFormXmlConstants.ATTRIBUTE_RESOURCE_BUNDLE,
                                                                        UIFormXmlConstants.ATTRIBUTE_KEY);
    if (descriptor == null) {
      throw new IllegalArgumentException("String descriptor value required");
    }
    return descriptor;
  }
}

