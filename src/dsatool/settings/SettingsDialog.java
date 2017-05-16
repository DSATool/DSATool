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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dsatool.resources.Settings;
import dsatool.util.ErrorLogger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SettingsDialog {

	@FXML
	private ListView<String> list;
	@FXML
	private BorderPane pane;
	@FXML
	private StackPane tabArea;

	private final Stage stage;

	private final Map<String, Node> pages = new HashMap<>();

	public SettingsDialog(final Window window) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("SettingsDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		stage = new Stage();
		stage.setTitle("Einstellungen");
		stage.setScene(new Scene(pane, 600, 350));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(window);

		list.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
			if (newV != null) {
				pages.get(newV).toFront();
			}
		});

		final BookSettingsPage books = new BookSettingsPage(stage);
		list.getItems().add("Regelwerke");
		pages.put("Regelwerke", books.getControl());
		tabArea.getChildren().add(books.getControl());

		final Map<String, Map<String, Set<Setting>>> settings = Settings.getSettings();
		for (final String pageName : settings.keySet()) {
			final Map<String, Set<Setting>> page = settings.get(pageName);
			final SettingsPage control = new SettingsPage();
			list.getItems().add(pageName);
			pages.put(pageName, control.getControl());
			tabArea.getChildren().add(control.getControl());
			boolean first = true;
			for (final String categoryName : page.keySet()) {
				if (first) {
					first = false;
				} else {
					control.addSeparator();
				}
				control.addLabel(categoryName);
				final Set<Setting> category = page.get(categoryName);
				for (final Setting setting : category) {
					setting.create(control);
				}
			}
		}

		list.getSelectionModel().clearAndSelect(0);

		stage.show();
	}

	@FXML
	public void close() {
		stage.close();
	}
}
