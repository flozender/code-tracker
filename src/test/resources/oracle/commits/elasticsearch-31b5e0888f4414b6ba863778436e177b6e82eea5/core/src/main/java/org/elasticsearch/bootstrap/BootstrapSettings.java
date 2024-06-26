/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.bootstrap;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.SettingsProperty;

public final class BootstrapSettings {

    private BootstrapSettings() {
    }

    // TODO: remove this hack when insecure defaults are removed from java
    public static final Setting<Boolean> SECURITY_FILTER_BAD_DEFAULTS_SETTING =
            Setting.boolSetting("security.manager.filter_bad_defaults", true, false, SettingsProperty.ClusterScope);

    public static final Setting<Boolean> MLOCKALL_SETTING =
        Setting.boolSetting("bootstrap.mlockall", false, false, SettingsProperty.ClusterScope);
    public static final Setting<Boolean> SECCOMP_SETTING =
        Setting.boolSetting("bootstrap.seccomp", true, false, SettingsProperty.ClusterScope);
    public static final Setting<Boolean> CTRLHANDLER_SETTING =
        Setting.boolSetting("bootstrap.ctrlhandler", true, false, SettingsProperty.ClusterScope);

}
