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
package com.intellij.openapi.application;

import java.util.EventListener;

/**
 * Listener for application events.
 */
public interface ApplicationListener extends EventListener {
  /**
   * This method is called to check whether the Application is ready to exit.
   * @return true or false
   */
  boolean canExitApplication();

  /**
   * Is called when application is exiting.
   */
  void applicationExiting();

  /**
   * Is called before action start.
   */
  void beforeWriteActionStart(Object action);

  /**
   * Is called on action start.
   */
  void writeActionStarted(Object action);

  /**
   *  Is called on action finish.
   */
  void writeActionFinished(Object action);
}