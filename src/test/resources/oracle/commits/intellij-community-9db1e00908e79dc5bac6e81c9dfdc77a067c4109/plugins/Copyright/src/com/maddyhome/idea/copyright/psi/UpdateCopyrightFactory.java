/*
 * Copyright 2000-2008 JetBrains s.r.o.
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

import com.intellij.lang.Language;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.util.FileTypeUtil;

public class UpdateCopyrightFactory
{
    public static UpdateCopyright createUpdateCopyright(Project project, Module module, PsiFile file,
        CopyrightProfile options)
    {
        return createUpdateCopyright(project, module, file.getVirtualFile(), file.getFileType(), options);
    }



    private static UpdateCopyright createUpdateCopyright(Project project, Module module, VirtualFile file,
        FileType base, CopyrightProfile options)
    {
        if (base == null || file == null)
        {
            return null;
        }


        // NOTE - any changes here require changes to LanguageOptionsFactory and ConfigTabFactory
        FileType type = FileTypeUtil.getInstance().getFileTypeByType(base);
        logger.debug("file=" + file);
        logger.debug("type=" + type.getName());

        if (type.equals(StdFileTypes.JAVA))
        {
            return new UpdateJavaFileCopyright(project, module, file, options);
        }
        else if (type.equals(StdFileTypes.XML))
        {
            return new UpdateXmlFileCopyright(project, module, file, options);
        }
        else if (type.equals(StdFileTypes.HTML))
        {
            return new UpdateXmlFileCopyright(project, module, file, options);
        }
        else if (type.equals(StdFileTypes.JSP))
        {
            return new UpdateJspFileCopyright(project, module, file, options);
        }
        else if (type.equals(StdFileTypes.JSPX))
        {
            return new UpdateJspxFileCopyright(project, module, file, options);
        }
        else if (type.equals(StdFileTypes.PROPERTIES))
        {
            return new UpdatePropertiesFileCopyright(project, module, file, options);
        }
        else if ("JavaScript".equals(type.getName()))
        {
            return new UpdateJavaScriptFileCopyright(project, module, file, options);
        }
        else
        {
            if (type instanceof LanguageFileType)
            {
                Language lang = ((LanguageFileType)type).getLanguage();
                if (lang.equals(StdLanguages.CSS))
                {
                    return new UpdateCssFileCopyright(project, module, file, options);
                }
            }
        }

        logger.info("oops");
        return null;
    }

    private UpdateCopyrightFactory()
    {
    }

    private static final Logger logger = Logger.getInstance(UpdateCopyrightFactory.class.getName());
}