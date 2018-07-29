/*
 * Copyright 2017 DSATool team
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
package dsatool.resources;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import dsatool.settings.Setting;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class Settings {
	private static Map<String, Map<String, Set<Setting>>> settings = new LinkedHashMap<>();

	public static void addSetting(final Setting setting) {
		final String[] path = setting.getPath();
		Map<String, Set<Setting>> page = settings.get(path[0]);
		if (page == null) {
			page = new HashMap<>();
			page.put("Allgemein", new LinkedHashSet<>());
			settings.put(path[0], page);
		}
		final String categoryName = path.length > 2 ? path[1] : "Allgemein";
		Set<Setting> category = page.get(categoryName);
		if (category == null) {
			category = new LinkedHashSet<>();
			page.put(categoryName, category);
		}
		category.add(setting);
	}

	private static JSONObject getSetting(final String[] path, final boolean create) {
		JSONObject setting = ResourceManager.getResource("settings/Einstellungen", false);
		for (int i = 0; i < path.length - 1 && setting != null; ++i) {
			if (create) {
				setting = setting.getObj(path[i]);
			} else {
				setting = setting.getObjOrDefault(path[i], null);
			}
		}
		return setting;
	}

	public static JSONArray getSettingArray(final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return null;
		return setting.getArrOrDefault(path[path.length - 1], null);
	}

	public static JSONArray getSettingArrayOrDefault(final JSONArray def, final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return def;
		return setting.getArrOrDefault(path[path.length - 1], def);
	}

	public static Boolean getSettingBool(final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return null;
		return setting.getBoolOrDefault(path[path.length - 1], null);
	}

	public static Boolean getSettingBoolOrDefault(final Boolean def, final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return def;
		return setting.getBoolOrDefault(path[path.length - 1], def);
	}

	public static Integer getSettingInt(final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return null;
		return setting.getIntOrDefault(path[path.length - 1], null);
	}

	public static Integer getSettingIntOrDefault(final Integer def, final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return def;
		return setting.getIntOrDefault(path[path.length - 1], def);
	}

	public static Map<String, Map<String, Set<Setting>>> getSettings() {
		return settings;
	}

	public static String getSettingString(final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return null;
		return setting.getStringOrDefault(path[path.length - 1], null);
	}

	public static String getSettingStringOrDefault(final String def, final String... path) {
		final JSONObject setting = getSetting(path, false);
		if (setting == null) return def;
		return setting.getStringOrDefault(path[path.length - 1], def);
	}

	public static void setSetting(final boolean value, final String... path) {
		final JSONObject setting = getSetting(path, true);
		setting.put(path[path.length - 1], value);
	}

	public static void setSetting(final int value, final String... path) {
		final JSONObject setting = getSetting(path, true);
		setting.put(path[path.length - 1], value);
	}

	public static void setSetting(final String value, final String... path) {
		final JSONObject setting = getSetting(path, true);
		setting.put(path[path.length - 1], value);
	}

	private Settings() {}
}
