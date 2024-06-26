package com.intellij.cvsSupport2.impl;

import com.intellij.cvsSupport2.config.CvsApplicationLevelConfiguration;
import com.intellij.cvsSupport2.config.CvsRootConfiguration;
import com.intellij.cvsSupport2.config.DateOrRevisionSettings;
import com.intellij.cvsSupport2.connections.CvsConnectionSettings;
import com.intellij.cvsSupport2.connections.RootFormatter;
import com.intellij.cvsSupport2.connections.pserver.PServerLoginProvider;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutor;
import com.intellij.cvsSupport2.cvsExecution.CvsOperationExecutorCallback;
import com.intellij.cvsSupport2.cvshandlers.CommandCvsHandler;
import com.intellij.cvsSupport2.cvsoperations.cvsCheckOut.CheckoutProjectOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetFileContentOperation;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDate;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDateImpl;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.SimpleRevision;
import com.intellij.cvsSupport2.history.ComparableVcsRevisionOnOperation;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.cvsIntegration.CvsModule;
import com.intellij.openapi.cvsIntegration.CvsRepository;
import com.intellij.openapi.cvsIntegration.CvsResult;
import com.intellij.openapi.cvsIntegration.CvsServices;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.vfs.VcsFileSystem;
import com.intellij.openapi.vcs.vfs.VcsVirtualFile;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.CvsBundle;
import org.netbeans.lib.cvsclient.command.KeywordSubstitution;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * author: lesya
 */
public class CvsServicesImpl extends CvsServices implements ApplicationComponent {

  private static final Logger LOG = Logger.getInstance("#com.intellij.cvsSupport2.impl.CvsServicesImpl");

  public CvsModule[] chooseModules(Project project,
                                   boolean allowRootSelection,
                                   boolean allowMultipleSelection,
                                   boolean allowFilesSelection, String title, String selectModulePageTitle) {
    ModuleChooser moduleChooser = new ModuleChooser(project,
                                                    allowFilesSelection,
                                                    allowMultipleSelection,
                                                    allowRootSelection,
                                                    title,
                                                    selectModulePageTitle);
    moduleChooser.show();

    if (!moduleChooser.isOK()) return null;

    return moduleChooser.getSelectedModules();
  }

  public CvsRepository[] getConfiguredRepositories() {
    List configurations = CvsApplicationLevelConfiguration.getInstance().CONFIGURATIONS;
    CvsRepository[] result = new CvsRepository[configurations.size()];
    for (int i = 0; i < configurations.size(); i++) {
      result[i] = ((CvsRootConfiguration)configurations.get(i)).createCvsRepository();
    }
    return result;
  }

  private ComparableVcsRevisionOnOperation createCvsVersionOn(CvsModule module, Project project) {
    CvsConnectionSettings env = RootFormatter.createConfigurationOn(CvsApplicationLevelConfiguration.getInstance()
                                                                    .getConfigurationForCvsRoot(module.getRepository()
                                                                                                .getStringRepresentation()));

    GetFileContentOperation operation = new GetFileContentOperation(new File(module.getPathInCvs()),
                                                                    env, new SimpleRevision(module.getRevision())
    );

    return new ComparableVcsRevisionOnOperation(operation, project);

  }

  public void showDifferencesForFiles(CvsModule first, CvsModule second, Project project) throws Exception {
    AbstractVcsHelper.getInstance(project).showDifferences(
      createCvsVersionOn(first, project),
      createCvsVersionOn(second, project),
      new File(first.getPathInCvs()));
  }

  public String getScrambledPasswordForPServerCvsRoot(String cvsRoot) {
    return PServerLoginProvider.getInstance()
      .getScrambledPasswordForCvsRoot(cvsRoot);
  }

  public String getComponentName() {
    return "CvsServices";
  }

  public void initComponent() { }

  public void disposeComponent() {
  }

  public boolean saveRepository(CvsRepository repository) {
    CvsApplicationLevelConfiguration configuration = CvsApplicationLevelConfiguration.getInstance();
    CvsRootConfiguration config = CvsRootConfiguration.createOn(repository);
    if (configuration.CONFIGURATIONS.contains(config)) return false;
    configuration.CONFIGURATIONS.add(config);
    return configuration.CONFIGURATIONS.contains(config);
  }

  public void openInEditor(Project project, CvsModule cvsFile) {
    CvsRepository repository = cvsFile.getRepository();
    RevisionOrDate revisionOrDate = RevisionOrDateImpl.createOn(new DateOrRevisionSettings().updateFrom(repository.getDateOrRevision()));

    GetFileContentOperation operation = new GetFileContentOperation(new File(cvsFile.getPathInCvs()),
                                                                    CvsRootConfiguration.createOn(repository),
                                                                    revisionOrDate
    );

    ComparableVcsRevisionOnOperation revision = new ComparableVcsRevisionOnOperation(operation,
                                                                                     project);

    VcsVirtualFile vcsVirtualFile = new VcsVirtualFile(cvsFile.getPathInCvs(),
                                                       revision,
                                                       VcsFileSystem.getInstance());
    OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, vcsVirtualFile);
    FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, false);
  }

  public byte[] getFileContent(Project project, CvsModule cvsFile) throws IOException {
    final GetFileContentOperation operation = new GetFileContentOperation(new File(cvsFile.getPathInCvs()),
                                                                          CvsRootConfiguration.createOn(cvsFile.getRepository()),
                                                                          new SimpleRevision(cvsFile.getRevision())
    );

    final CvsOperationExecutor executor = new CvsOperationExecutor(project);
    executor.performActionSync(new CommandCvsHandler(CvsBundle.message("operation.name.load.file.content"), operation, false),
                               CvsOperationExecutorCallback.EMPTY);

    if (!executor.hasNoErrors()) throw new RuntimeException(executor.getFirstError());
    if (operation.isDeleted()) throw new IOException(CvsBundle.message("exception.text.revision.has.been.deleted"));
    return operation.getFileBytes();
  }

  public CvsResult checkout(String[] modules,
                            File checkoutTo,
                            String directory,
                            boolean makeNewFilesReadOnly,
                            boolean pruneEmptyDirectories,
                            Object keywordSubstitution,
                            Project project,
                            CvsRepository repository) {
    LOG.assertTrue(modules.length > 0);
    CheckoutProjectOperation operation = new CheckoutProjectOperation(modules,
                                                                      CvsRootConfiguration.createOn(repository),
                                                                      makeNewFilesReadOnly,
                                                                      checkoutTo,
                                                                      directory,
                                                                      pruneEmptyDirectories,
                                                                      (KeywordSubstitution)keywordSubstitution);
    final CvsOperationExecutor executor = new CvsOperationExecutor(project);
    executor.performActionSync(new CommandCvsHandler(CvsBundle.message("operation.name.checkout"), operation, true),
                               CvsOperationExecutorCallback.EMPTY);
    return executor.getResult();
  }

}
