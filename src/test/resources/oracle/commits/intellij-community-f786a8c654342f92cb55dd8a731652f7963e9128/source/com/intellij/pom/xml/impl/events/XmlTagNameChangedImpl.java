package com.intellij.pom.xml.impl.events;

import com.intellij.pom.PomModel;
import com.intellij.pom.event.PomModelEvent;
import com.intellij.pom.xml.XmlAspect;
import com.intellij.pom.xml.impl.XmlAspectChangeSetImpl;
import com.intellij.pom.xml.XmlChangeVisitor;
import com.intellij.pom.xml.events.XmlTagNameChanged;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

public class XmlTagNameChangedImpl implements XmlTagNameChanged {
  private final String myOldName;
  private final XmlTag myTag;

  public XmlTagNameChangedImpl(XmlTag tag, String oldName) {
    myOldName = oldName;
    myTag = tag;
  }

  public String getOldName() {
    return myOldName;
  }

  public XmlTag getTag() {
    return myTag;
  }

  public static PomModelEvent createXmlTagNameChanged(PomModel model, XmlTag tag, String oldName) {
    final PomModelEvent event = new PomModelEvent(model);
    final XmlAspectChangeSetImpl xmlAspectChangeSet = new XmlAspectChangeSetImpl(model, PsiTreeUtil.getParentOfType(tag, XmlFile.class));
    xmlAspectChangeSet.add(new XmlTagNameChangedImpl(tag, oldName));
    event.registerChangeSet(model.getModelAspect(XmlAspect.class), xmlAspectChangeSet);
    return event;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "tag name changed to " + getTag().getName() + " was: " + getOldName();
  }

  public void accept(XmlChangeVisitor visitor) {
    visitor.visitXmlTagNameChanged(this);
  }
}
