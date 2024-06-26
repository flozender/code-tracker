/*
 *  Copyright 2000-2007 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.maddyhome.idea.copyright.psi;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.maddyhome.idea.copyright.CopyrightManager;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.options.LanguageOptions;
import com.maddyhome.idea.copyright.pattern.EntityUtil;
import com.maddyhome.idea.copyright.pattern.VelocityHelper;
import com.maddyhome.idea.copyright.util.FileTypeUtil;

public abstract class AbstractUpdateCopyright implements UpdateCopyright {
  private String commentText = null;
  private Project project;
  private Module module;
  private VirtualFile root;
  private CopyrightProfile myCopyrightProfile;
  private PsiManager manager;

  protected AbstractUpdateCopyright(Project project, Module module, VirtualFile root, CopyrightProfile copyrightProfile) {
    this.project = project;
    this.module = module;
    this.root = root;
    myCopyrightProfile = copyrightProfile;
    manager = PsiManager.getInstance(project);
  }

  protected String getCommentText(String prefix, String suffix) {
    if (commentText == null) {
      FileType ftype = FileTypeUtil.getInstance().getFileTypeByFile(root);
      LanguageOptions opts = CopyrightManager.getInstance(project).getOptions().getMergedOptions(ftype.getName());
      String base = EntityUtil.decode(myCopyrightProfile.getNotice());
      if (base.length() > 0) {
        String expanded = VelocityHelper.evaluate(manager.findFile(root), project, module, base);
        String cmt = FileTypeUtil.buildComment(root.getFileType(), expanded, opts);
        commentText = prefix + cmt + suffix;
      }
      else {
        commentText = "";
      }
    }

    return commentText;
  }


  public VirtualFile getRoot() {
    return root;
  }

  public PsiManager getManager() {
    return manager;
  }

  protected void resetCommentText() {
    commentText = null;
  }

  protected static int countNewline(String text) {
    int cnt = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        cnt++;
      }
    }

    return cnt;
  }
}