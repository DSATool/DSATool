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
package dsatool.settings;

import dsatool.resources.Settings;
import javafx.beans.property.BooleanProperty;

public class BooleanSetting extends Setting {

	private final boolean defaultVal;

	public BooleanSetting(final String name, final boolean defaultVal, final String... path) {
		super(name, path);
		this.defaultVal = defaultVal;
	}

	@Override
	public void create(final SettingsPage page) {
		page.addBooleanChoice(name);
		final BooleanProperty property = page.getBool(name);
		property.set(Settings.getSettingBoolOrDefault(defaultVal, path));
		property.addListener((o, oldV, newV) -> {
			Settings.setSetting(newV, path);
		});
	}
}
