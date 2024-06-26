package com.intellij.openapi.roots.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.UserDefinedExcludeFolder;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 *  @author dsl
 */
public class ExcludeFolderImpl extends ContentFolderBaseImpl implements ClonableContentFolder,
                                                                        UserDefinedExcludeFolder {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.roots.impl.SimpleExcludeFolderImpl");
  @NonNls static final String ELEMENT_NAME = "excludeFolder";

  ExcludeFolderImpl(@NotNull VirtualFile file, @NotNull ContentEntryImpl contentEntry) {
    super(file, contentEntry);
  }

  ExcludeFolderImpl(@NotNull String url, @NotNull ContentEntryImpl contentEntry) {
    super(url, contentEntry);
  }

  ExcludeFolderImpl(@NotNull Element element, @NotNull ContentEntryImpl contentEntry) throws InvalidDataException {
    super(element, contentEntry);
    LOG.assertTrue(ELEMENT_NAME.equals(element.getName()));
  }

  public ExcludeFolderImpl(@NotNull ExcludeFolderImpl that, @NotNull ContentEntryImpl contentEntry) {
    super(that, contentEntry);
  }

  public void writeExternal(Element element) {
    writeFolder(element, ELEMENT_NAME);
  }

  public ContentFolder cloneFolder(ContentEntry contentEntry) {
    return new ExcludeFolderImpl(this, (ContentEntryImpl)contentEntry);
  }

  @Override
  public int compareTo(ContentFolderBaseImpl folder) {
    if (!(folder instanceof ExcludeFolderImpl)) return -1;
    return super.compareTo(folder);
  }
}
