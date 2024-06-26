package com.intellij.cvsSupport2.config;

import com.intellij.cvsSupport2.CvsResultEx;
import com.intellij.cvsSupport2.connections.*;
import com.intellij.cvsSupport2.connections.ssh.ui.SshSettings;
import com.intellij.cvsSupport2.cvsExecution.ModalityContext;
import com.intellij.cvsSupport2.cvsoperations.common.CvsExecutionEnvironment;
import com.intellij.cvsSupport2.cvsoperations.common.PostCvsActivity;
import com.intellij.cvsSupport2.cvsoperations.cvsContent.GetModulesListOperation;
import com.intellij.cvsSupport2.cvsoperations.cvsErrors.ErrorMessagesProcessor;
import com.intellij.cvsSupport2.cvsoperations.cvsMessages.CvsListenerWithProgress;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDate;
import com.intellij.cvsSupport2.cvsoperations.dateOrRevision.RevisionOrDateImpl;
import com.intellij.cvsSupport2.errorHandling.CvsException;
import com.intellij.cvsSupport2.javacvsImpl.io.ReadWriteStatistics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.cvsIntegration.CvsRepository;
import com.intellij.openapi.cvsIntegration.CvsResult;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.VcsException;
import org.netbeans.lib.cvsclient.CvsRoot;
import org.netbeans.lib.cvsclient.ValidRequestsExpectedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.IConnection;

import java.io.IOException;
import java.util.List;

public class CvsRootConfiguration
  extends AbstractConfiguration
  implements CvsEnvironment, Cloneable {

  public String CVS_ROOT = "";
  public String PATH_TO_WORKING_FOLDER = "";

  public ProxySettings PROXY_SETTINGS = new ProxySettings();
  public ExtConfiguration EXT_CONFIGURATION = new ExtConfiguration();
  public SshSettings SSH_CONFIGURATION = new SshSettings();
  public SshSettings SSH_FOR_EXT_CONFIGURATION = new SshSettings();
  public LocalSettings LOCAL_CONFIGURATION = new LocalSettings();

  public DateOrRevisionSettings DATE_OR_REVISION_SETTINGS = new DateOrRevisionSettings();

  private static final String SEPARATOR = ":";
  private static final String AT = "@";



  public CvsRootConfiguration(CvsApplicationLevelConfiguration mainConfiguration) {
    super("CvsRootConfiguration");
    EXT_CONFIGURATION = mainConfiguration.EXT_CONFIGURATION.clone();
    SSH_CONFIGURATION = mainConfiguration.SSH_CONFIGURATION.clone();
    SSH_FOR_EXT_CONFIGURATION = mainConfiguration.SSH_FOR_EXT_CONFIGURATION.clone();
    LOCAL_CONFIGURATION = mainConfiguration.LOCAL_CONFIGURATION.clone();
    PROXY_SETTINGS = mainConfiguration.PROXY_SETTINGS.clone();
  }

  private CvsConnectionSettings getSettings() {
    return createSettings();
  }

  public CvsConnectionSettings createSettings() {
    return RootFormatter.createConfigurationOn(this);
  }


  public IConnection createConnection(ReadWriteStatistics statistics, ModalityContext executor) {
    return getSettings().createConnection(statistics, executor);
  }

  public String getCvsRootAsString() {
    return CVS_ROOT;
  }

  private static String createFieldByFieldCvsRoot(CvsRepository cvsRepository) {

    return createStringRepresentationOn(CvsMethod.getValue(cvsRepository.getMethod()),
                                        cvsRepository.getUser(),
                                        cvsRepository.getHost(),
                                        String.valueOf(cvsRepository.getPort()),
                                        cvsRepository.getRepository());

  }

  public static String createStringRepresentationOn(CvsMethod method,
                                                    String user,
                                                    String host,
                                                    String port,
                                                    String repository) {
    StringBuffer result = new StringBuffer();

    if (method == CvsMethod.LOCAL_METHOD) return repository;

    result.append(SEPARATOR);
    result.append(method.getName());
    result.append(SEPARATOR);
    result.append(user);
    result.append(AT);
    result.append(host);
    if (port.length() > 0) {
      result.append(SEPARATOR);
      result.append(port);
    }
    result.append(SEPARATOR);
    result.append(repository);
    return result.toString();
  }

  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append(getCvsRootAsString());
    if (useBranch()) {
      result.append(" (on branch " + DATE_OR_REVISION_SETTINGS.BRANCH + ")");
    }

    if (useDate()) {
      result.append(" (for date " + DATE_OR_REVISION_SETTINGS.getDate() + ")");
    }

    return result.toString();
  }

  private boolean useDate() {
    return DATE_OR_REVISION_SETTINGS.USE_DATE && (DATE_OR_REVISION_SETTINGS.getDate().length() > 0);
  }

  private boolean useBranch() {
    return DATE_OR_REVISION_SETTINGS.USE_BRANCH && (DATE_OR_REVISION_SETTINGS.BRANCH.length() > 0);
  }

  public CvsRootConfiguration getMyCopy() {
    try {
      return (CvsRootConfiguration)clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  public void testConnection() throws Exception {
    testConnection(getSettings().createConnection(new ReadWriteStatistics(), new ModalityContext(true)), getSettings());
  }

  public static void testConnection(final IConnection connection, CvsConnectionSettings settings)
    throws AuthenticationException, IOException {
    final GetModulesListOperation operation =
      new GetModulesListOperation(settings);
    ErrorMessagesProcessor errorProcessor = new ErrorMessagesProcessor();
    final CvsExecutionEnvironment cvsExecutionEnvironment = new CvsExecutionEnvironment(errorProcessor,
                                                                                        CvsExecutionEnvironment.DUMMY_STOPPER,
                                                                                        errorProcessor,
                                                                                        new ModalityContext(true),
                                                                                        PostCvsActivity.DEAF);

    final CvsResult result = new CvsResultEx();
    try {
      final CvsRootProvider cvsRootProvider = operation.getCvsRootProvider();
      ApplicationManager.getApplication().runProcessWithProgressSynchronously(new Runnable() {
        public void run() {
          try {
            if (connection instanceof SelfTestingConnection) {
              ((SelfTestingConnection)connection).test(CvsListenerWithProgress.createOnProgress());
            }
            operation.execute(cvsRootProvider, cvsExecutionEnvironment, connection);
          }
          catch (ValidRequestsExpectedException ex) {
            result.addError(new CvsException(ex, cvsRootProvider.getCvsRootAsString()));
          }
          catch (CommandException ex) {
            result.addError(new CvsException(ex.getUnderlyingException(), cvsRootProvider.getCvsRootAsString()));
          }
          catch (ProcessCanceledException ex) {
            result.setIsCanceled();
          }
          catch (Exception e) {
            result.addError(new CvsException(e, cvsRootProvider.getCvsRootAsString()));
          }
        }
      }, "Test Connection", true, null);
      if (result.isCanceled()) throw new ProcessCanceledException();

      if (!result.hasNoErrors()) {
        VcsException vcsException = result.composeError();
        throw new AuthenticationException(vcsException.getLocalizedMessage(), vcsException.getCause());
      }
      List<VcsException> errors = errorProcessor.getErrors();
      if (!errors.isEmpty()) {
        VcsException firstError = errors.get(0);
        throw new AuthenticationException(firstError.getLocalizedMessage(), firstError);
      }
    }
    finally {
      connection.close();
    }
  }

  public int hashCode() {
    return CVS_ROOT.hashCode() ^ DATE_OR_REVISION_SETTINGS.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof CvsRootConfiguration)) {
      return false;
    }
    CvsRootConfiguration another = ((CvsRootConfiguration)obj);

    return CVS_ROOT.equals(another.CVS_ROOT)
           && DATE_OR_REVISION_SETTINGS.equals(another.DATE_OR_REVISION_SETTINGS)
           && Comparing.equal(EXT_CONFIGURATION, another.EXT_CONFIGURATION);
  }

  public boolean login(ModalityContext executor) {
    return getSettings().login(executor);
  }

  public RevisionOrDate getRevisionOrDate() {
    return RevisionOrDateImpl.createOn(DATE_OR_REVISION_SETTINGS);
  }

  public String getRepository() {
    return getSettings().getRepository();
  }

  public CvsRoot getCvsRoot() {
    return getSettings().getCvsRoot();
  }

  public boolean isValid() {
    return getSettings().isValid();
  }

  public CvsRepository createCvsRepository() {
    CvsConnectionSettings settings = createSettings();

    return new CvsRepository(settings.getCvsRootAsString(),
                             settings.METHOD.getName(),
                             settings.USER,
                             settings.HOST,
                             settings.REPOSITORY,
                             settings.PORT,
                             DATE_OR_REVISION_SETTINGS);

  }

  public static CvsRootConfiguration createOn(CvsRepository repository) {
    CvsRootConfiguration result = new CvsRootConfiguration(CvsApplicationLevelConfiguration.getInstance());
    result.DATE_OR_REVISION_SETTINGS.updateFrom(repository.getDateOrRevision());
    result.CVS_ROOT = createFieldByFieldCvsRoot(repository);
    return result;
  }

  protected Object clone() throws CloneNotSupportedException {
    CvsRootConfiguration result = (CvsRootConfiguration)super.clone();
    result.DATE_OR_REVISION_SETTINGS = (DateOrRevisionSettings)DATE_OR_REVISION_SETTINGS.clone();
    return result;
  }

  public CommandException processException(CommandException t) {
    return getSettings().processException(t);
  }

}
