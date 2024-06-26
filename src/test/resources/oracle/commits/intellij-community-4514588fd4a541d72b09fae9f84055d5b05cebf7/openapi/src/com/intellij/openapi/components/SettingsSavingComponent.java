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
package com.intellij.openapi.components;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;

/**
 * Component which implements this interfaces will be asked to save ({@link #save}) custom settings (in their own custom way)
 *  when {@link Application#saveSettings()} (for Application level components) or {@link Project#save()}
 * (for Project level compoents) is invoked.
 * @see BaseComponent
 */
public interface SettingsSavingComponent {
  void save();
}
