package com.intellij.cvsSupport2.cvsoperations.cvsErrors;

import com.intellij.cvsSupport2.errorHandling.CvsException;
import com.intellij.cvsSupport2.cvsoperations.cvsMessages.CvsMessagesAdapter;
import com.intellij.cvsSupport2.cvsoperations.cvsErrors.ErrorProcessor;
import com.intellij.cvsSupport2.cvsoperations.cvsErrors.ErrorProcessor;
import com.intellij.cvsSupport2.cvsoperations.cvsMessages.CvsMessagesAdapter;
import com.intellij.cvsSupport2.util.CvsVfsUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.netbeans.lib.cvsclient.file.ICvsFileSystem;

import java.util.ArrayList;
import java.util.List;

public class ErrorMessagesProcessor extends CvsMessagesAdapter implements ErrorProcessor {
  private final List<VcsException> myErrors;
  private final List<VcsException> myWarnings;


  public ErrorMessagesProcessor(List<VcsException> errors) {
    myErrors = errors;
    myWarnings = new ArrayList<VcsException>();
  }

  public ErrorMessagesProcessor() {
    this(new ArrayList<VcsException>());
  }

  public void addError(String message, String relativeFilePath, ICvsFileSystem cvsFileSystem, String cvsRoot) {

    addErrorOnCurrentMessage(relativeFilePath, message, cvsFileSystem, myErrors, cvsRoot);
  }

  public void addWarning(String message, String relativeFilePath, ICvsFileSystem cvsFileSystem, String cvsRoot) {
    addErrorOnCurrentMessage(relativeFilePath, message, cvsFileSystem, myWarnings, cvsRoot);
  }

  private void addErrorOnCurrentMessage(String relativeFileName,
                                        String message,
                                        ICvsFileSystem cvsFileSystem,
                                        List collection, String cvsRoot) {
    VirtualFile vFile = getVirtualFile(cvsFileSystem, relativeFileName);
    VcsException vcsException = new CvsException(message, cvsRoot);
    if (vFile != null) vcsException.setVirtualFile(vFile);
    collection.add(vcsException);
  }

  private VirtualFile getVirtualFile(ICvsFileSystem cvsFileSystem, String relativeFileName) {
    if (cvsFileSystem == null) return null;
    if (relativeFileName == null) return null;
    return CvsVfsUtil.findFileByIoFile(cvsFileSystem.getLocalFileSystem().getFile(relativeFileName));
  }

  public List<VcsException> getErrors() {
    return myErrors;
  }

  public List<VcsException> getWarnings() {
    return myWarnings;
  }

  public void clear() {
    myErrors.clear();
    myWarnings.clear();
  }

  public void addError(VcsException ex) {
    myErrors.add(ex);
  }

  public void addWarning(VcsException ex) {
    myWarnings.add(ex);
  }
}
