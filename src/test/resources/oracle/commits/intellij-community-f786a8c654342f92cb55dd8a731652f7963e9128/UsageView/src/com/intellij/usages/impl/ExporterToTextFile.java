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
package com.intellij.usages.impl;

import com.intellij.usages.TextChunk;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageViewSettings;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.SystemProperties;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;
import java.util.TooManyListenersException;

/**
 * Created by IntelliJ IDEA.
 * User: max
 * Date: Dec 22, 2004
 * Time: 4:51:52 PM
 * To change this template use File | Settings | File Templates.
 */
class ExporterToTextFile implements com.intellij.ide.ExporterToTextFile {
  private UsageViewImpl myUsageView;

  public ExporterToTextFile(UsageViewImpl usageView) {
    myUsageView = usageView;
  }

  public JComponent getSettingsEditor() {
    return null;
  }

  public void addSettingsChangedListener(ChangeListener listener) throws TooManyListenersException {
  }

  public void removeSettingsChangedListener(ChangeListener listener) {
  }

  public String getReportText() {
    StringBuffer buf = new StringBuffer();
    //noinspection HardCodedStringLiteral
    appendNode(buf, myUsageView.getModelRoot(), SystemProperties.getLineSeparator(), "");
    return buf.toString();
  }

  private void appendNode(StringBuffer buf, DefaultMutableTreeNode node, String lineSeparator, String indent) {
    buf.append(indent);
    final String childIndent;
    if (node.getParent() != null) {
      childIndent = indent + "    ";
      appendNodeText(buf, node, lineSeparator);
    }
    else {
      childIndent = indent;
    }

    Enumeration enumeration = node.children();
    while (enumeration.hasMoreElements()) {
      DefaultMutableTreeNode child = (DefaultMutableTreeNode)enumeration.nextElement();
      appendNode(buf, child, lineSeparator, childIndent);
    }
  }

  private void appendNodeText(StringBuffer buf, DefaultMutableTreeNode node, String lineSeparator) {
    if (node instanceof UsageNode) {
      TextChunk[] chunks = ((UsageNode)node).getUsage().getPresentation().getText();
      for (TextChunk chunk : chunks) {
        buf.append(chunk.getText());
      }
    }
    else if (node instanceof GroupNode) {
      UsageGroup group = ((GroupNode)node).getGroup();
      buf.append(group != null ? group.getText(myUsageView) : UsageViewBundle.message("usages.title"));
      buf.append(" ");
      int count = ((GroupNode)node).getRecursiveUsageCount();
      buf.append(" (").append(UsageViewBundle.message("usages.n", count)).append(")");
    }
    else if (node instanceof UsageTargetNode) {
      buf.append(((UsageTargetNode)node).getTarget().getPresentation().getPresentableText());
    }
    else {
      buf.append(node.toString());
    }
    buf.append(lineSeparator);
  }

  public String getDefaultFilePath() {
    return UsageViewSettings.getInstance().EXPORT_FILE_NAME;
  }

  public void exportedTo(String filePath) {
    UsageViewSettings.getInstance().EXPORT_FILE_NAME = filePath;
  }

  public boolean canExport() {
    return !myUsageView.isSearchInProgress() && myUsageView.areTargetsValid();
  }
}
