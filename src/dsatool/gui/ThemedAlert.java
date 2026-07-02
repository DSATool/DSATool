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
package dsatool.gui;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

public class ThemedAlert extends Alert {

	public ThemedAlert(final AlertType type) {
		super(type);
		final Scene scene = getDialogPane().getScene();
		final ObservableList<String> styleSheets = scene.getStylesheets();
		styleSheets.add(getClass().getResource("application.css").toExternalForm());
		styleSheets.add(ThemedScene.getTheme());
	}

}
