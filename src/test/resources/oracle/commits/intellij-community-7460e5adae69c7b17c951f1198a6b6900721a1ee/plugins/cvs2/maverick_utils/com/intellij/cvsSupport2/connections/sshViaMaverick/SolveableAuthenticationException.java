package com.intellij.cvsSupport2.connections.sshViaMaverick;

import org.netbeans.lib.cvsclient.connection.AuthenticationException;

/**
 * author: lesya
 */
public class SolveableAuthenticationException extends AuthenticationException{
  public SolveableAuthenticationException(String message) {
    super(message);
  }

  public SolveableAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
