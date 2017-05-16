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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class IntegerSetting extends Setting {

	private final int defaultVal;
	private final int min;
	private final int max;

	public IntegerSetting(String name, int defaultVal, int min, int max, String... path) {
		super(name, path);
		this.defaultVal = defaultVal;
		this.min = min;
		this.max = max;
	}

	@Override
	public void create(SettingsPage page) {
		final IntegerProperty property = new SimpleIntegerProperty(Settings.getSettingIntOrDefault(defaultVal, path));
		property.addListener((o, oldV, newV) -> {
			Settings.setSetting(newV.intValue(), path);
		});
		page.addIntegerChoice(name, property, min, max);
	}
}
