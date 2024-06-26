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

package org.elasticsearch.script;

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ScriptSettings {

    private static final Map<ScriptType, Setting<Boolean>> SCRIPT_TYPE_SETTING_MAP;

    static {
        Map<ScriptType, Setting<Boolean>> scriptTypeSettingMap = new EnumMap<>(ScriptType.class);
        for (ScriptType scriptType : ScriptType.values()) {
            scriptTypeSettingMap.put(scriptType, Setting.boolSetting(
                ScriptModes.sourceKey(scriptType),
                scriptType.isDefaultEnabled(),
                Property.NodeScope,
                Property.Deprecated));
        }
        SCRIPT_TYPE_SETTING_MAP = Collections.unmodifiableMap(scriptTypeSettingMap);
    }

    private final Map<ScriptContext, Setting<Boolean>> scriptContextSettingMap;
    private final List<Setting<Boolean>> scriptLanguageSettings;

    public ScriptSettings(ScriptEngineRegistry scriptEngineRegistry, ScriptContextRegistry scriptContextRegistry) {
        Map<ScriptContext, Setting<Boolean>> scriptContextSettingMap = contextSettings(scriptContextRegistry);
        this.scriptContextSettingMap = Collections.unmodifiableMap(scriptContextSettingMap);

        List<Setting<Boolean>> scriptLanguageSettings = languageSettings(SCRIPT_TYPE_SETTING_MAP, scriptContextSettingMap, scriptEngineRegistry, scriptContextRegistry);
        this.scriptLanguageSettings = Collections.unmodifiableList(scriptLanguageSettings);
    }

    private static Map<ScriptContext, Setting<Boolean>> contextSettings(ScriptContextRegistry scriptContextRegistry) {
        Map<ScriptContext, Setting<Boolean>> scriptContextSettingMap = new HashMap<>();
        for (ScriptContext scriptContext : scriptContextRegistry.scriptContexts()) {
            scriptContextSettingMap.put(scriptContext,
                    Setting.boolSetting(ScriptModes.operationKey(scriptContext), false, Property.NodeScope, Property.Deprecated));
        }
        return scriptContextSettingMap;
    }

    private static List<Setting<Boolean>> languageSettings(Map<ScriptType, Setting<Boolean>> scriptTypeSettingMap,
                                                              Map<ScriptContext, Setting<Boolean>> scriptContextSettingMap,
                                                              ScriptEngineRegistry scriptEngineRegistry,
                                                              ScriptContextRegistry scriptContextRegistry) {
        final List<Setting<Boolean>> scriptModeSettings = new ArrayList<>();

        for (final Class<? extends ScriptEngine> scriptEngineService : scriptEngineRegistry.getRegisteredScriptEngineServices()) {
            if (scriptEngineService == NativeScriptEngine.class) {
                // native scripts are always enabled, and their settings can not be changed
                continue;
            }
            final String language = scriptEngineRegistry.getLanguage(scriptEngineService);
            for (final ScriptType scriptType : ScriptType.values()) {
                // Top level, like "script.engine.groovy.inline"
                final boolean defaultNonFileScriptMode = scriptEngineRegistry.getDefaultInlineScriptEnableds().get(language);
                boolean defaultLangAndType = defaultNonFileScriptMode;
                // Files are treated differently because they are never default-deny
                final boolean defaultIfNothingSet = defaultLangAndType;

                Function<Settings, String> defaultLangAndTypeFn = settings -> {
                    final Setting<Boolean> globalTypeSetting = scriptTypeSettingMap.get(scriptType);
                    final Setting<Boolean> langAndTypeSetting = Setting.boolSetting(ScriptModes.getGlobalKey(language, scriptType),
                            defaultIfNothingSet, Property.NodeScope, Property.Deprecated);

                    if (langAndTypeSetting.exists(settings)) {
                        // fine-grained e.g. script.engine.groovy.inline
                        return langAndTypeSetting.get(settings).toString();
                    } else if (globalTypeSetting.exists(settings)) {
                        // global type - script.inline
                        return globalTypeSetting.get(settings).toString();
                    } else {
                        return Boolean.toString(defaultIfNothingSet);
                    }
                };

                // Setting for something like "script.engine.groovy.inline"
                final Setting<Boolean> langAndTypeSetting = Setting.boolSetting(ScriptModes.getGlobalKey(language, scriptType),
                        defaultLangAndTypeFn, Property.NodeScope, Property.Deprecated);
                scriptModeSettings.add(langAndTypeSetting);

                for (ScriptContext scriptContext : scriptContextRegistry.scriptContexts()) {
                    final String langAndTypeAndContextName = ScriptModes.getKey(language, scriptType, scriptContext);
                    // A function that, given a setting, will return what the default should be. Since the fine-grained script settings
                    // read from a bunch of different places this is implemented in this way.
                    Function<Settings, String> defaultSettingFn = settings -> {
                        final Setting<Boolean> globalOpSetting = scriptContextSettingMap.get(scriptContext);
                        final Setting<Boolean> globalTypeSetting = scriptTypeSettingMap.get(scriptType);
                        final Setting<Boolean> langAndTypeAndContextSetting = Setting.boolSetting(langAndTypeAndContextName,
                                defaultIfNothingSet, Property.NodeScope, Property.Deprecated);

                        // fallback logic for script mode settings
                        if (langAndTypeAndContextSetting.exists(settings)) {
                            // like: "script.engine.groovy.inline.aggs: true"
                            return langAndTypeAndContextSetting.get(settings).toString();
                        } else if (langAndTypeSetting.exists(settings)) {
                            // like: "script.engine.groovy.inline: true"
                            return langAndTypeSetting.get(settings).toString();
                        } else if (globalOpSetting.exists(settings)) {
                            // like: "script.aggs: true"
                            return globalOpSetting.get(settings).toString();
                        } else if (globalTypeSetting.exists(settings)) {
                            // like: "script.inline: true"
                            return globalTypeSetting.get(settings).toString();
                        } else {
                            // Nothing is set!
                            return Boolean.toString(defaultIfNothingSet);
                        }
                    };
                    // The actual setting for finest grained script settings
                    Setting<Boolean> setting =
                        Setting.boolSetting(langAndTypeAndContextName, defaultSettingFn, Property.NodeScope, Property.Deprecated);
                    scriptModeSettings.add(setting);
                }
            }
        }
        return scriptModeSettings;
    }

    public List<Setting<?>> getSettings() {
        List<Setting<?>> settings = new ArrayList<>();
        settings.addAll(SCRIPT_TYPE_SETTING_MAP.values());
        settings.addAll(scriptContextSettingMap.values());
        settings.addAll(scriptLanguageSettings);
        return settings;
    }

    public Iterable<Setting<Boolean>> getScriptLanguageSettings() {
        return scriptLanguageSettings;
    }
}
