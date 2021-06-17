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

import java.util.Collection;

import dsatool.resources.Settings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StringChoiceSetting extends Setting {

	private final String defaultVal;
	private final Collection<String> choices;

	public StringChoiceSetting(final String name, final String defaultVal, final Collection<String> choices, final String... path) {
		super(name, path);
		this.defaultVal = defaultVal;
		this.choices = choices;
	}

	@Override
	public void create(final SettingsPage page) {
		final StringProperty property = new SimpleStringProperty(Settings.getSettingStringOrDefault(defaultVal, path));
		property.addListener((o, oldV, newV) -> {
			Settings.setSetting(newV, path);
		});
		page.addStringChoice(name, property, choices);
	}
}
