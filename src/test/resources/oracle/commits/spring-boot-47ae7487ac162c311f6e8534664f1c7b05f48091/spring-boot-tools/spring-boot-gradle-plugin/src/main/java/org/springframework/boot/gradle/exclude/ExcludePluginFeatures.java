/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.exclude;

import org.gradle.api.Project;
import org.springframework.boot.gradle.PluginFeatures;
import org.springframework.boot.gradle.SpringBootPluginExtension;

/**
 * {@link PluginFeatures} to apply exclusion rules.
 *
 * @author Phillip Webb
 */
public class ExcludePluginFeatures implements PluginFeatures {

	@Override
	public void apply(Project project) {
		SpringBootPluginExtension extension = project.getExtensions()
				.getByType(SpringBootPluginExtension.class);
		if (extension.isApplyExcludeRules()) {
			project.getConfigurations().all(new ApplyExcludeRules(project));
		}
	}

}
