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
import java.util.Collection;
import java.util.List;

import dsatool.util.ErrorLogger;
import dsatool.util.ReactiveSpinner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class SettingsPage {
	@FXML
	protected VBox box;
	@FXML
	private ScrollPane pane;

	public SettingsPage() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("SettingsPage.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}
	}

	public CheckBox addBooleanChoice(final String text, final BooleanProperty property) {
		final Label label = new Label(text + ":");
		label.setMaxWidth(Double.POSITIVE_INFINITY);
		HBox.setHgrow(label, Priority.ALWAYS);
		final CheckBox result = new CheckBox();
		box.getChildren().add(new HBox(2, label, result));
		result.selectedProperty().bindBidirectional(property);
		return result;
	}

	public Button addFileChoice(final String text, final ObjectProperty<File> property, final String extensionDesc, final List<String> fileExtensions) {
		final Label label = new Label(text + ":");
		label.setMaxWidth(Double.POSITIVE_INFINITY);
		HBox.setHgrow(label, Priority.ALWAYS);
		final Button result = new Button();
		box.getChildren().add(new HBox(2, label, result));

		final File currentFile = property.get();
		result.setText(currentFile != null ? currentFile.getName() : "Kein");
		property.addListener((ov, oldVal, newVal) -> {
			result.setText(newVal != null ? newVal.getName() : "Kein");
		});
		result.setOnAction(e -> {
			final FileChooser dialog = new FileChooser();

			dialog.setTitle("Datei öffnen");
			dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(extensionDesc, fileExtensions));

			property.setValue(dialog.showOpenDialog(null));
		});
		return result;
	}

	public ReactiveSpinner<Integer> addIntegerChoice(final String text, final IntegerProperty property, final int min, final int max) {
		final Label label = new Label(text + ":");
		label.setMaxWidth(Double.POSITIVE_INFINITY);
		HBox.setHgrow(label, Priority.ALWAYS);
		final ReactiveSpinner<Integer> result = new ReactiveSpinner<>(min, max);
		result.setEditable(true);
		box.getChildren().add(new HBox(2, label, result));
		result.getValueFactory().setValue(property.get());
		result.getValueFactory().valueProperty().addListener((o, oldV, newV) -> {
			property.set(newV);
		});
		property.addListener((o, oldV, newV) -> {
			result.getValueFactory().setValue(newV.intValue());
		});
		return result;
	}

	public void addLabel(final String text) {
		final Label label = new Label(text + ":");
		label.setMaxWidth(Double.POSITIVE_INFINITY);
		box.getChildren().add(label);
	}

	public void addNode(final Node node) {
		box.getChildren().add(node);
	}

	public void addSeparator() {
		box.getChildren().add(new Separator());
	}

	public ComboBox<String> addStringChoice(final String text, final StringProperty property, final Collection<String> choices) {
		final Label label = new Label(text + ":");
		label.setMaxWidth(Double.POSITIVE_INFINITY);
		HBox.setHgrow(label, Priority.ALWAYS);
		final ComboBox<String> result = new ComboBox<>(FXCollections.observableArrayList(choices));
		box.getChildren().add(new HBox(2, label, result));
		result.getSelectionModel().select(property.get());
		result.getSelectionModel().selectedItemProperty().addListener((ov, oldVal, newVal) -> {
			property.set(newVal);
		});
		property.addListener((ov, oldVal, newVal) -> {
			result.getSelectionModel().select(newVal);
		});
		return result;
	}

	public void clear() {
		box.getChildren().clear();
	}

	public Node getControl() {
		return pane;
	}
}
