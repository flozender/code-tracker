/*
 * Copyright 2005 Sascha Weinreuter
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

package org.intellij.lang.xpath.xslt.impl.references;

import org.intellij.lang.xpath.completion.NamespaceLookup;
import org.intellij.lang.xpath.psi.impl.ResolveUtil;
import org.intellij.lang.xpath.xslt.XsltSupport;
import org.intellij.lang.xpath.xslt.impl.XsltChecker;
import org.intellij.lang.xpath.xslt.context.XsltNamespaceContext;
import org.intellij.lang.xpath.xslt.psi.impl.ImplicitModeElement;
import org.intellij.lang.xpath.xslt.util.MatchTemplateMatcher;
import org.intellij.lang.xpath.xslt.util.QNameUtil;
import org.intellij.lang.xpath.xslt.util.XsltCodeInsightUtil;

import com.intellij.codeInsight.daemon.EmptyResolveMessageProvider;
import com.intellij.codeInsight.daemon.QuickFixProvider;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.xml.*;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

class ModeReference extends SimpleAttributeReference implements PsiPolyVariantReference, EmptyResolveMessageProvider {
    private final boolean myIsDeclaration;
    private final ImplicitModeElement myImplicitModeElement;

    public ModeReference(XmlAttribute attribute, boolean isDeclaration) {
        super(attribute);
        if (isDeclaration) {
            myIsDeclaration = true;
        } else {
            final PsiFile file = attribute.getContainingFile();
            if (file != null && XsltSupport.getXsltSupportLevel(file) == XsltChecker.SupportLevel.PARTIAL) {
                myIsDeclaration = "#current".equals(attribute.getValue());
            } else {
                myIsDeclaration = false;
            }
        }
        myImplicitModeElement = new ImplicitModeElement(attribute);
    }

    @Override
    @NotNull
    protected TextRange getTextRange() {
        return myImplicitModeElement.getModeRange();
    }

    public Object[] getVariants() {
        final PsiFile containingFile = myAttribute.getContainingFile();
        if (containingFile instanceof XmlFile && XsltSupport.isXsltFile(containingFile)) {
            final List<Object> l = new ArrayList<Object>();
            if (!myImplicitModeElement.hasPrefix()) {
                final Object[] prefixes = PrefixReference.getPrefixCompletions(myAttribute);
                l.addAll(Arrays.asList(prefixes));
            }

            if (myImplicitModeElement.getQName() != null) {
                final PsiElement[] modes = ResolveUtil.collect(getMatcher().variantMatcher());
                l.addAll(Arrays.asList(modes));
            }
          return ArrayUtil.toObjectArray(l);
        }
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    public boolean isSoft() {
        return myIsDeclaration;
    }

    @Override
    @Nullable
    public PsiElement resolveImpl() {
        if (myIsDeclaration) {
            return myImplicitModeElement;
        } else {
            final ResolveResult[] results = multiResolve(false);
            return results.length == 1 ? results[0].getElement() : null;
        }
    }

    @NotNull
    public ResolveResult[] multiResolve(final boolean incompleteCode) {
        final PsiFile containingFile = myAttribute.getContainingFile();
        if (containingFile instanceof XmlFile && XsltSupport.isXsltFile(containingFile) && myImplicitModeElement.getQName() != null) {
            final PsiElement[] psiElements = ResolveUtil.collect(getMatcher());
            final ResolveResult[] results = new ResolveResult[psiElements.length];
            for (int i = 0; i < psiElements.length; i++) {
                results[i] = new PsiElementResolveResult(psiElements[i]);
            }
            return results;
        }
        return ResolveResult.EMPTY_ARRAY;
    }

    private MyModeMatcher getMatcher() {
        return new MyModeMatcher(myAttribute, myImplicitModeElement.getQName());
    }

    public static PsiReference[] create(XmlAttribute attribute, boolean isDeclaration) {
        final String value = attribute.getValue();
        if (value == null) {
            return PsiReference.EMPTY_ARRAY;
        }
        if (value.trim().indexOf(' ') != -1) {
            // TODO: fix for xslt 2.0
            return PsiReference.EMPTY_ARRAY;
        }
        return createImpl(attribute, isDeclaration, value);
    }

    private static PsiReference[] createImpl(XmlAttribute attribute, boolean isDeclaration, String value) {
        final int p = value.indexOf(':');
        if (p == -1) {
            return new PsiReference[]{ new ModeReference(attribute, isDeclaration) };
        } else if (p == value.length() - 1) {
            return new PsiReference[]{ new PrefixReference(attribute) };
        } else {
            return new PsiReference[]{
                    new PrefixReference(attribute),
                    new ModeReference(attribute, isDeclaration)
            };
        }
    }

    static class PrefixReference extends SimpleAttributeReference implements EmptyResolveMessageProvider, QuickFixProvider<PrefixReference> {
        public PrefixReference(XmlAttribute attribute) {
            super(attribute);
        }

        public Object[] getVariants() {
            return getPrefixCompletions(myAttribute);
        }

        static Object[] getPrefixCompletions(XmlAttribute attribute) {
            final MyModeMatcher matcher = new MyModeMatcher(attribute, QNameUtil.ANY);
            final PsiElement[] modes = ResolveUtil.collect(matcher);
            final Collection<String> prefixes = XsltNamespaceContext.getPrefixes(attribute);
            final Set<NamespaceLookup> lookups = new HashSet<NamespaceLookup>(prefixes.size());

            for (PsiElement mode : modes) {
                final QName qName = ((ImplicitModeElement)mode).getQName();
                if (qName == null) continue;
                final String prefix = qName.getPrefix();
                if (!prefixes.contains(prefix)) continue;

                lookups.add(new NamespaceLookup(prefix));
            }

          return ArrayUtil.toObjectArray(lookups);
        }

        public void registerQuickfix(HighlightInfo highlightInfo, PrefixReference psiReference) {
            // TODO: This should actually scan all (reachable) xslt files for mode-declarations with the same local name
        }

        public boolean isSoft() {
            return false;
        }

        @Override
        @NotNull
        protected TextRange getTextRange() {
            return ImplicitModeElement.getPrefixRange(myAttribute);
        }

        @Override
        @Nullable
        public PsiElement resolveImpl() {
            return XsltNamespaceContext.resolvePrefix(getCanonicalText(), myAttribute);
        }

        public String getUnresolvedMessagePattern() {
            return "Undeclared namespace prefix ''{0}''";
        }
    }

    private static class MyModeMatcher extends MatchTemplateMatcher {
        public MyModeMatcher(XmlDocument document, QName mode) {
            super(document, mode);
        }
        
        public MyModeMatcher(XmlElement element, QName mode) {
            super(XsltCodeInsightUtil.getDocument(element), mode);
        }

        protected PsiElement transform(XmlTag element) {
            return new ImplicitModeElement(element.getAttribute("mode", null));
        }

        public boolean matches(XmlTag element) {
            final String s = element.getAttributeValue("mode");
            return myMode != null &&
                    s != null && s.indexOf("IntellijIdeaRulezzz") == -1 &&
                    super.matches(element);
        }

        @Override
        protected ResolveUtil.Matcher changeDocument(XmlDocument document) {
            return new MyModeMatcher(document, myMode);
        }

        public ResolveUtil.Matcher variantMatcher() {
            return new MyModeMatcher(myDocument, myMode != null ? QNameUtil.createAnyLocalName(myMode.getNamespaceURI()) : null);
        }
    }

    public String getUnresolvedMessagePattern() {
        final QName qName = myImplicitModeElement.getQName();
        if (qName != null && qName != QNameUtil.UNRESOLVED) {
            return "Undefined mode ''" + qName.toString() + "''";
        } else {
            return "Undefined mode ''{0}''";
        }
    }
}