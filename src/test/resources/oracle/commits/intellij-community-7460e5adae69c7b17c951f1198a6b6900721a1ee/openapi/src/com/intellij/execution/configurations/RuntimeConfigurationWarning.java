/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.execution.configurations;



public class RuntimeConfigurationWarning extends RuntimeConfigurationException{
  public RuntimeConfigurationWarning(final String message) {
    super(message, "Warning");
  }
}