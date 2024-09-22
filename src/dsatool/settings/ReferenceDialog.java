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

import java.io.File;

import dsatool.resources.ResourceManager;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONObject;

public class ReferenceDialog {
	@FXML
	private Parent pane;
	@FXML
	private TextField path;
	@FXML
	private RadioButton defaultApp;
	@FXML
	private RadioButton specified;
	@FXML
	private RadioButton withOffset;
	@FXML
	private TextField command;
	@FXML
	private ReactiveSpinner<Integer> offset;

	private final Window window;
	private final Stage stage;

	private final JSONObject books;
	private final JSONObject book;

	public ReferenceDialog(final Window window, final String title) {
		this.window = window;

		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("ReferenceDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		stage = new Stage();
		stage.setTitle("Pfad für " + title);
		stage.setScene(new Scene(pane, 650, 125));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setResizable(false);
		stage.initOwner(window);

		books = ResourceManager.getResource("settings/Buecher");
		book = books.getObj(title);

		offset.disableProperty().bind(defaultApp.selectedProperty());

		path.setText(book.getStringOrDefault("Pfad", ""));
		if (book.getStringOrDefault("Befehl", null) != null) {
			specified.setSelected(true);
			command.setDisable(false);
			command.setText(book.getString("Befehl"));
			offset.getValueFactory().setValue(book.getIntOrDefault("Seitenoffset", 0));
		} else if (book.getIntOrDefault("Seitenoffset", null) != null) {
			withOffset.setSelected(true);
			offset.getValueFactory().setValue(book.getIntOrDefault("Seitenoffset", 0));
		}

		path.textProperty().addListener((o, oldV, newV) -> {
			if ("".equals(newV.trim())) {
				book.removeKey("Pfad");
			} else {
				book.put("Pfad", newV.trim());
			}
		});

		specified.selectedProperty().addListener((o, oldV, newV) -> {
			command.setDisable(!newV);
			if (newV) {
				book.put("Befehl", command.getText().trim());
				book.put("Seitenoffset", offset.getValue());
			} else {
				book.putNull("Befehl");
				if (!withOffset.isSelected()) {
					book.putNull("Seitenoffset");
				}
			}
		});

		withOffset.selectedProperty().addListener((o, oldV, newV) -> {
			if (newV) {
				book.put("Seitenoffset", offset.getValue());
			} else if (!specified.isSelected()) {
				book.putNull("Seitenoffset");
			}
		});

		command.textProperty().addListener((o, oldV, newV) -> {
			if ("".equals(newV.trim())) {
				book.putNull("Befehl");
			} else {
				book.put("Befehl", newV.trim());
			}
		});

		offset.valueProperty().addListener((o, oldV, newV) -> {
			book.put("Seitenoffset", newV);
		});

		stage.show();
	}

	@FXML
	private void browsePath() {
		final FileChooser dialog = new FileChooser();

		dialog.setTitle("Datei wählen");
		final File file = dialog.showOpenDialog(window);

		if (file != null) {
			path.setText(file.getAbsolutePath());
		}
	}

	@FXML
	private void close() {
		stage.close();
	}
}
