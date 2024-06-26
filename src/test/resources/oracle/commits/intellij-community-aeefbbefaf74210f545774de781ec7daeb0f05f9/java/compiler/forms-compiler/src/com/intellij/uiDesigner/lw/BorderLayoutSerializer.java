/*
 * Copyright 2000-2006 JetBrains s.r.o.
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

import java.awt.*;

/**
 * @author yole
 */
public class BorderLayoutSerializer extends LayoutSerializer {
  public static final BorderLayoutSerializer INSTANCE = new BorderLayoutSerializer();

  private BorderLayoutSerializer() {
  }

  void readLayout(Element element, LwContainer container) {
    final int hGap = LwXmlReader.getOptionalInt(element, UIFormXmlConstants.ATTRIBUTE_HGAP, 0);
    final int vGap = LwXmlReader.getOptionalInt(element, UIFormXmlConstants.ATTRIBUTE_VGAP, 0);
    container.setLayout(new BorderLayout(hGap, vGap));
  }

  void readChildConstraints(final Element constraintsElement, final LwComponent component) {
    component.setCustomLayoutConstraints(LwXmlReader.getRequiredString(constraintsElement,
                                                                       UIFormXmlConstants.ATTRIBUTE_BORDER_CONSTRAINT));
  }
}
