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
package dsatool.ui;

import java.util.Collection;
import java.util.function.BiConsumer;

import dsatool.util.ErrorLogger;
import dsatool.util.Util;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;
import jsonant.value.JSONValue;

public class RenameDialog {

	@FXML
	private VBox root;
	@FXML
	private TextField name;
	@FXML
	private Button okButton;
	@FXML
	private Button cancelButton;

	public RenameDialog(final Window window, final String type, final String plural, final JSONValue category, final JSONObject item,
			final BiConsumer<String, String> afterRename, final Collection<String> prohibitedNames) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("RenameDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		final Stage stage = new Stage();
		stage.setTitle(type);
		stage.setScene(new Scene(root, 290, 55));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setResizable(false);
		stage.initOwner(window);

		final String oldName = item != null ? category instanceof final JSONObject obj ? obj.keyOf(item) : item.getStringOrDefault("Name", null) : null;

		name.setText(oldName == null ? nextFreeName(category, null, type, prohibitedNames) : oldName);

		okButton.setOnAction(event -> {
			final String newName = name.getText();

			if (isNameUsed(category, oldName, newName, prohibitedNames)) {
				final Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Name bereits vergeben");
				alert.setHeaderText(plural + " müssen eindeutig benannt sein.");
				alert.setContentText(type + " konnte nicht gespeichert werden.");
				alert.getButtonTypes().setAll(ButtonType.OK);
				alert.show();
			} else {
				if (item == null) {
					final JSONObject actualItem = new JSONObject(category);
					if (category instanceof final JSONArray arr) {
						actualItem.put("Name", newName);
						arr.add(actualItem);
					} else {
						((JSONObject) category).put(newName, actualItem);
					}
					afterRename.accept(null, newName);
					actualItem.notifyListeners(null);
				} else if (!newName.equals(oldName)) {
					if (category instanceof final JSONObject obj) {
						Util.rename(obj, oldName, newName);
					} else {
						item.put("Name", newName);
					}
					afterRename.accept(oldName, newName);
					item.notifyListeners(null);
				}
				stage.close();
			}
		});

		cancelButton.setOnAction(event -> stage.close());

		okButton.setDefaultButton(true);
		cancelButton.setCancelButton(true);

		stage.show();

	}

	private boolean isNameUsed(final JSONValue category, final String oldName, final String name, final Collection<String> prohibitedNames) {
		if (name.equals(oldName)) return false;
		if (prohibitedNames.contains(name)) return true;
		if (category instanceof final JSONArray arr) {
			for (int i = 0; i < arr.size(); ++i) {
				if (name.equals(arr.getObj(i).getStringOrDefault("Name", null))) return true;
			}
		} else
			return ((JSONObject) category).containsKey(name);
		return false;
	}

	private String nextFreeName(final JSONValue category, final String oldName, final String type, final Collection<String> prohibitedNames) {
		if (!isNameUsed(category, oldName, type, prohibitedNames)) return type;
		for (int i = 1; true; ++i) {
			if (!isNameUsed(category, oldName, type + ' ' + i, prohibitedNames)) return type + ' ' + i;
		}
	}

}
