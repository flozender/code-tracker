package com.intellij.cvsSupport2.javacvsImpl.io;

import com.intellij.cvsSupport2.javacvsImpl.ProjectContentInfoProvider;
import com.intellij.cvsSupport2.util.CvsVfsUtil;
import com.intellij.cvsSupport2.CvsUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import org.netbeans.lib.cvsclient.IConnectionStreams;
import org.netbeans.lib.cvsclient.file.*;

import java.io.IOException;
import java.util.Collection;

/**
 * author: lesya
 */
public class LocalFileReaderBasedOnVFS implements ILocalFileReader {

  private static final Logger LOG = Logger.getInstance("#com.intellij.cvsSupport2.javacvsImpl.io.LocalFileReaderBasedOnVFS");

  private final ILocalFileReader myLocalFileReader;
  private final ProjectContentInfoProvider myProjectContentInfoProvider;

  public LocalFileReaderBasedOnVFS(ISendTextFilePreprocessor sendTextFilePreprocessor,
                                    ProjectContentInfoProvider projectContentInfoProvider) {
    myLocalFileReader = new LocalFileReader(sendTextFilePreprocessor);
    myProjectContentInfoProvider = projectContentInfoProvider;
  }

  public void transmitTextFile(FileObject fileObject,
                               IConnectionStreams connectionStreams,
                               ICvsFileSystem cvsFileSystem) throws IOException {
    myLocalFileReader.transmitTextFile(fileObject,
                                       connectionStreams,
                                       cvsFileSystem);
  }

  public void transmitBinaryFile(FileObject fileObject,
                                 IConnectionStreams connectionStreams,
                                 ICvsFileSystem cvsFileSystem) throws IOException {
    myLocalFileReader.transmitBinaryFile(fileObject,
                                         connectionStreams,
                                         cvsFileSystem);
  }

  public boolean exists(AbstractFileObject fileObject, ICvsFileSystem cvsFileSystem) {
    return getVirtualFile(fileObject, cvsFileSystem) != null;
  }

  private VirtualFile getVirtualFile(AbstractFileObject fileObject, ICvsFileSystem cvsFileSystem) {
    return CvsVfsUtil.findFileByIoFile(cvsFileSystem.getLocalFileSystem().getFile(fileObject));
  }

  public boolean isWritable(FileObject fileObject, ICvsFileSystem cvsFileSystem) {
    VirtualFile virtualFile = getVirtualFile(fileObject, cvsFileSystem);
    if (virtualFile == null) return false;
    return CvsVfsUtil.isWritable(virtualFile);
  }

  public void listFilesAndDirectories(DirectoryObject directoryObject,
                                      Collection fileNames,
                                      Collection directoryNames,
                                      ICvsFileSystem cvsFileSystem) {
    VirtualFile virtualDirectory = getVirtualFile(directoryObject, cvsFileSystem);
    if (virtualDirectory == null) return;
    VirtualFile[] children = CvsVfsUtil.getChildrenOf(virtualDirectory);
    if (children == null) return;

    for (int i = 0; i < children.length; i++) {
      final VirtualFile fileOrDirectory = children[i];
      if (CvsUtil.CVS.equals(fileOrDirectory.getName())) continue;
      if (!myProjectContentInfoProvider.fileIsUnderProject(fileOrDirectory)) continue;
      final String name = fileOrDirectory.getName();
      if (fileOrDirectory.isDirectory()) {
        if (directoryNames != null) {
          directoryNames.add(name);
        }
      }
      else {
        if (fileNames != null) {
          LOG.assertTrue(name.length() > 0);
          fileNames.add(name);
        }
      }
    }

  }
}
