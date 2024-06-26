package com.intellij.pom.xml.impl.events;

import com.intellij.pom.PomModel;
import com.intellij.pom.event.PomModelEvent;
import com.intellij.pom.xml.XmlAspect;
import com.intellij.pom.xml.XmlChangeVisitor;
import com.intellij.pom.xml.events.XmlChange;
import com.intellij.pom.xml.events.XmlTagChildRemoved;
import com.intellij.pom.xml.impl.XmlAspectChangeSetImpl;
import com.intellij.pom.xml.XmlChangeVisitor;
import com.intellij.pom.xml.impl.XmlAspectChangeSetImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagChild;

public class XmlTagChildRemovedImpl implements XmlTagChildRemoved {
  private final XmlTag myTag;
  private final XmlTagChild myChild;
  public XmlTagChildRemovedImpl(XmlTag context, XmlTagChild treeElement) {
    myTag = context;
    myChild = treeElement;
  }

  public XmlTag getTag() {
    return myTag;
  }

  public XmlTagChild getChild() {
    return myChild;
  }

  public static PomModelEvent createXmlTagChildRemoved(PomModel source, XmlTag context, XmlTagChild treeElement) {
    final PomModelEvent event = new PomModelEvent(source);
    final XmlAspectChangeSetImpl xmlAspectChangeSet = new XmlAspectChangeSetImpl(source, PsiTreeUtil.getParentOfType(context, XmlFile.class));
    xmlAspectChangeSet.add(new XmlTagChildRemovedImpl(context, treeElement));
    event.registerChangeSet(source.getModelAspect(XmlAspect.class), xmlAspectChangeSet);
    return event;
  }
  @SuppressWarnings({"HardCodedStringLiteral"})
  public String toString() {
    return "child removed from " + getTag().getName() + " child: " + myChild.toString();
  }

  public void accept(XmlChangeVisitor visitor) {
    visitor.visitXmlTagChildRemoved(this);
  }
}
