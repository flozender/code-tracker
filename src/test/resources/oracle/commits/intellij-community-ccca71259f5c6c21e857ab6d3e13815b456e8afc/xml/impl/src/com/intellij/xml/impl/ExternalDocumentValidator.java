/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.xml.impl;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.Validator;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.ide.highlighter.XHtmlFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.reference.SoftReference;
import com.intellij.xml.actions.ValidateXmlActionHandler;
import com.intellij.xml.util.CheckXmlFileWithXercesValidatorInspection;
import com.intellij.xml.util.XmlResourceResolver;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.xml.sax.SAXParseException;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: maxim
 * Date: 21.01.2005
 * Time: 0:07:51
 * To change this template use File | Settings | File Templates.
 */
public class ExternalDocumentValidator {
  private static final Logger LOG = Logger.getInstance("#com.intellij.xml.impl.ExternalDocumentValidator");
  private static final Key<SoftReference<ExternalDocumentValidator>> validatorInstanceKey = Key.create("validatorInstance");
  private ValidateXmlActionHandler myHandler;
  private Validator.ValidationHost myHost;

  private long myModificationStamp;
  private PsiFile myFile;
  @NonNls
  private static final String CANNOT_FIND_DECLARATION_ERROR_PREFIX = "Cannot find the declaration of element";
  @NonNls
  private static final String ELEMENT_ERROR_PREFIX = "Element";
  @NonNls
  private static final String ROOT_ELEMENT_ERROR_PREFIX = "Document root element";
  @NonNls
  private static final String CONTENT_OF_ELEMENT_TYPE_ERROR_PREFIX = "The content of element type";
  @NonNls
  private static final String VALUE_ERROR_PREFIX = "Value ";
  @NonNls
  private static final String ATTRIBUTE_ERROR_PREFIX = "Attribute ";
  @NonNls
  private static final String STRING_ERROR_PREFIX = "The string";
  @NonNls
  private static final String ATTRIBUTE_MESSAGE_PREFIX = "cvc-attribute.";

  private static class ValidationInfo {
    PsiElement element;
    String message;
    int type;
  }

  private WeakReference<List<ValidationInfo>> myInfos; // last jaxp validation result

  private void runJaxpValidation(final XmlElement element, Validator.ValidationHost host) {
    final PsiFile file = element.getContainingFile();

    if (myFile == file &&
        file != null &&
        myModificationStamp == file.getModificationStamp() &&
        !ValidateXmlActionHandler.isValidationDependentFilesOutOfDate((XmlFile)file) &&
        myInfos!=null &&
        myInfos.get()!=null // we have validated before
        ) {
      addAllInfos(host,myInfos.get());
      return;
    }

    if (myHandler==null)  myHandler = new ValidateXmlActionHandler(false);
    final Project project = element.getProject();

    final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document==null) return;
    final List<ValidationInfo> results = new LinkedList<ValidationInfo>();

    myHost = new Validator.ValidationHost() {
      public void addMessage(PsiElement context, String message, int type) {
        final ValidationInfo o = new ValidationInfo();

        results.add(o);
        o.element = context;
        o.message = message;
        o.type = type;
      }

      public void addMessage(final PsiElement context, final String message, final ErrorType type, final IntentionAction... fixes) {
        addMessage(context, message, type.ordinal());
      }
    };

    myHandler.setErrorReporter(myHandler.new ErrorReporter() {
      public boolean isStopOnUndeclaredResource() {
        return true;
      }

      public void processError(final SAXParseException e, final boolean warning) {
        try {
          ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
              if (e.getPublicId() != null) {
                return;
              }

              final VirtualFile errorFile = myHandler.getFile(e.getPublicId(), e.getSystemId());
              if (errorFile != file.getVirtualFile() && errorFile != null) {
                return; // error in attached schema
              }

              if (document.getLineCount() < e.getLineNumber() || e.getLineNumber() <= 0) {
                return;
              }

              int offset = Math.max(0, document.getLineStartOffset(e.getLineNumber() - 1) + e.getColumnNumber() - 2);
              if (offset >= document.getTextLength()) return;
              PsiElement currentElement = PsiDocumentManager.getInstance(project).getPsiFile(document).findElementAt(offset);
              PsiElement originalElement = currentElement;
              final String elementText = currentElement.getText();

              if (elementText.equals("</")) {
                currentElement = currentElement.getNextSibling();
              }
              else if (elementText.equals(">") || elementText.equals("=")) {
                currentElement = currentElement.getPrevSibling();
              }

              // Cannot find the declaration of element
              String localizedMessage = e.getLocalizedMessage();

              // Ideally would be to switch one messageIds
              int endIndex = localizedMessage.indexOf(':');
              if (endIndex < localizedMessage.length() - 1 && localizedMessage.charAt(endIndex + 1) == '/') {
                endIndex = -1;  // ignore : in http://
              }
              String messageId = endIndex != -1 ? localizedMessage.substring(0, endIndex ):"";
              localizedMessage = localizedMessage.substring(endIndex + 1).trim();

              if (localizedMessage.startsWith(CANNOT_FIND_DECLARATION_ERROR_PREFIX) ||
                  localizedMessage.startsWith(ELEMENT_ERROR_PREFIX) ||
                  localizedMessage.startsWith(ROOT_ELEMENT_ERROR_PREFIX) ||
                  localizedMessage.startsWith(CONTENT_OF_ELEMENT_TYPE_ERROR_PREFIX)
                  ) {
                addProblemToTagName(currentElement, originalElement, localizedMessage, warning);
                //return;
              } else if (localizedMessage.startsWith(VALUE_ERROR_PREFIX)) {
                addProblemToTagName(currentElement, originalElement, localizedMessage, warning);
              } else if (messageId.startsWith(ATTRIBUTE_MESSAGE_PREFIX)) {
                @NonNls String prefix = "of attribute ";
                final int i = localizedMessage.indexOf(prefix);
                
                if (i != -1) {
                  int messagePrefixLength = prefix.length() + i;
                  final int nextQuoteIndex = localizedMessage.indexOf(localizedMessage.charAt(messagePrefixLength), messagePrefixLength + 1);
                  String attrName = nextQuoteIndex == -1 ? null : localizedMessage.substring(messagePrefixLength + 1, nextQuoteIndex);

                  XmlTag parent = PsiTreeUtil.getParentOfType(originalElement,XmlTag.class);
                  currentElement = parent.getAttribute(attrName,null);

                  if (currentElement != null) {
                    currentElement = ((XmlAttribute)currentElement).getValueElement();
                  }
                }

                if (currentElement!=null) {
                  assertValidElement(currentElement, originalElement,localizedMessage);
                  myHost.addMessage(currentElement,localizedMessage,warning ? Validator.ValidationHost.WARNING:Validator.ValidationHost.ERROR);
                } else {
                  addProblemToTagName(originalElement, originalElement, localizedMessage, warning);
                }
              }
              else if (localizedMessage.startsWith(ATTRIBUTE_ERROR_PREFIX)) {
                final int messagePrefixLength = ATTRIBUTE_ERROR_PREFIX.length();

                if ( localizedMessage.charAt(messagePrefixLength) == '"' ||
                     localizedMessage.charAt(messagePrefixLength) == '\''
                   ) {
                  // extract the attribute name from message and get it from tag!
                  final int nextQuoteIndex = localizedMessage.indexOf(localizedMessage.charAt(messagePrefixLength), messagePrefixLength + 1);
                  String attrName = nextQuoteIndex == -1 ? null : localizedMessage.substring(messagePrefixLength + 1, nextQuoteIndex);

                  XmlTag parent = PsiTreeUtil.getParentOfType(originalElement,XmlTag.class);
                  currentElement = parent.getAttribute(attrName,null);

                  if (currentElement!=null) {
                    currentElement = SourceTreeToPsiMap.treeElementToPsi(
                      XmlChildRole.ATTRIBUTE_NAME_FINDER.findChild(
                        SourceTreeToPsiMap.psiElementToTree(currentElement)
                      )
                    );
                  }
                } else {
                  currentElement = PsiTreeUtil.getParentOfType(currentElement, XmlTag.class, false);
                }

                if (currentElement!=null) {
                  assertValidElement(currentElement, originalElement,localizedMessage);
                  myHost.addMessage(currentElement,localizedMessage,warning ? Validator.ValidationHost.WARNING:Validator.ValidationHost.ERROR);
                } else {
                  addProblemToTagName(originalElement, originalElement, localizedMessage, warning);
                }
              } else if (localizedMessage.startsWith(STRING_ERROR_PREFIX)) {
                if (currentElement != null) {
                  myHost.addMessage(currentElement,localizedMessage,Validator.ValidationHost.WARNING);
                }
              }
              else {
                currentElement = getNodeForMessage(currentElement != null ? currentElement:originalElement);
                assertValidElement(currentElement, originalElement,localizedMessage);
                if (currentElement!=null) {
                  myHost.addMessage(currentElement,localizedMessage,warning ? Validator.ValidationHost.WARNING:Validator.ValidationHost.ERROR);
                }
              }
            }
          });
        }
        catch (Exception ex) {
          if (ex instanceof ProcessCanceledException) throw (ProcessCanceledException)ex;
          if (ex instanceof XmlResourceResolver.IgnoredResourceException) throw (XmlResourceResolver.IgnoredResourceException)ex;
          LOG.error(ex);
        }
      }

    });

    myHandler.doValidate(project, element.getContainingFile());

    myFile = file;
    myModificationStamp = myFile.getModificationStamp();
    myInfos = new WeakReference<List<ValidationInfo>>(results);

    addAllInfos(host,results);
  }

  private static PsiElement getNodeForMessage(final PsiElement currentElement) {
    PsiElement parentOfType = PsiTreeUtil.getNonStrictParentOfType(
      currentElement,
      XmlTag.class,
      XmlProcessingInstruction.class,
      XmlElementDecl.class,
      XmlMarkupDecl.class,
      XmlEntityRef.class,
      XmlDoctype.class
    );

    if (parentOfType == null) {
      if (currentElement instanceof XmlToken) {
        parentOfType = currentElement.getParent();
      }
      else {
        parentOfType = currentElement;
      }
    }
    return parentOfType;
  }

  private static void addAllInfos(Validator.ValidationHost host,List<ValidationInfo> highlightInfos) {
    for (ValidationInfo info : highlightInfos) {
      host.addMessage(info.element, info.message, info.type);
    }
  }

  private PsiElement addProblemToTagName(PsiElement currentElement,
                                     final PsiElement originalElement,
                                     final String localizedMessage,
                                     final boolean warning) {
    currentElement = PsiTreeUtil.getParentOfType(currentElement,XmlTag.class,false);
    if (currentElement==null) {
      currentElement = PsiTreeUtil.getParentOfType(originalElement,XmlElementDecl.class,false);
    }
    if (currentElement == null) {
      currentElement = originalElement;
    }
    assertValidElement(currentElement, originalElement,localizedMessage);

    if (currentElement!=null) {
      myHost.addMessage(currentElement,localizedMessage,warning ? Validator.ValidationHost.WARNING:Validator.ValidationHost.ERROR);
    }

    return currentElement;
  }

  private static void assertValidElement(PsiElement currentElement, PsiElement originalElement, String message) {
    if (currentElement==null) {
      XmlTag tag = PsiTreeUtil.getParentOfType(originalElement, XmlTag.class);
      LOG.assertTrue(
        false,
        "The validator message:"+ message+ " is bound to null node,\n" +
        "initial element:"+originalElement.getText()+",\n"+
        "parent:" + originalElement.getParent()+",\n" +
        "tag:" + (tag != null? tag.getText():"null") + ",\n" +
        "offset in tag: " + (originalElement.getTextOffset() - (tag == null ? 0 : tag.getTextOffset()))
      );
    }
  }

  public static synchronized void doValidation(final XmlDocument document, final Validator.ValidationHost host) {
    final PsiFile containingFile = document.getContainingFile();
    if (containingFile == null) {
      return;
    }

    if (containingFile.getViewProvider() instanceof TemplateLanguageFileViewProvider) {
      return;
    }
    
    final FileType fileType = containingFile.getViewProvider().getVirtualFile().getFileType();
    if (fileType != XmlFileType.INSTANCE && fileType != XHtmlFileType.INSTANCE) {
      return;
    }

    for(Language lang: containingFile.getViewProvider().getLanguages()) {
      if ("ANT".equals(lang.getID())) return;
    }

    final XmlTag rootTag = document != null ? document.getRootTag() : null;
    if (rootTag == null) return;

    if (XmlUtil.ANT_URI.equals(rootTag.getNamespace())) return;

    final Project project = document.getProject();

    final InspectionProfile profile = InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    final LocalInspectionToolWrapper toolWrapper =
      (LocalInspectionToolWrapper)profile.getInspectionTool(CheckXmlFileWithXercesValidatorInspection.SHORT_NAME, containingFile);

    if (toolWrapper == null) return;
    if (!profile.isToolEnabled(HighlightDisplayKey.find(CheckXmlFileWithXercesValidatorInspection.SHORT_NAME), containingFile)) return;

    SoftReference<ExternalDocumentValidator> validatorReference = project.getUserData(validatorInstanceKey);
    ExternalDocumentValidator validator = validatorReference != null? validatorReference.get() : null;

    if(validator == null) {
      validator = new ExternalDocumentValidator();
      project.putUserData(validatorInstanceKey,new SoftReference<ExternalDocumentValidator>(validator));
    }

    validator.runJaxpValidation(document,host);
  }
}
