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
package com.intellij.psi.xml;

import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

/**
 * @author Mike
 */
public interface XmlTag extends XmlElement, PsiNamedElement, PsiMetaOwner, XmlTagChild {
  XmlTag[] EMPTY = new XmlTag[0];

  @NonNls String getName();
  @NonNls String getNamespace();
  @NonNls String getLocalName();

  XmlElementDescriptor getDescriptor();

  XmlAttribute[] getAttributes();
  XmlAttribute getAttribute(@NonNls String name, @NonNls String namespace);

  String getAttributeValue(@NonNls String name);
  String getAttributeValue(@NonNls String name, @NonNls String namespace);

  XmlAttribute setAttribute(@NonNls String name, @NonNls String namespace, @NonNls String value) throws IncorrectOperationException;
  XmlAttribute setAttribute(@NonNls String name, @NonNls String value) throws IncorrectOperationException;

  XmlTag createChildTag(@NonNls String localName, @NonNls String namespace, @NonNls String bodyText, boolean enforceNamespacesDeep);

  XmlTag[] getSubTags();
  XmlTag[] findSubTags(@NonNls String qname);
  XmlTag[] findSubTags(@NonNls String localName, @NonNls String namespace);
  XmlTag findFirstSubTag(@NonNls String qname);

  @NonNls String getNamespacePrefix();
  String getNamespaceByPrefix(@NonNls String prefix);
  String getPrefixByNamespace(@NonNls String namespace);
  String[] knownNamespaces();

  boolean hasNamespaceDeclarations();
  Map<String, String> getLocalNamespaceDeclarations();

  XmlTagValue getValue();

  XmlNSDescriptor getNSDescriptor(@NonNls String namespace, boolean strict);

  boolean isEmpty();
}
