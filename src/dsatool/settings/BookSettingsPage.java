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

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dsatool.gui.GUIUtil;
import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class BookSettingsPage implements Serializable {

	private static final long serialVersionUID = 5125665276749218151L;

	@FXML
	protected VBox box;
	@FXML
	private ScrollPane pane;
	@FXML
	private ListView<String> usedBooks;
	@FXML
	private ListView<String> unusedBooks;
	@FXML
	private Button edit;
	@FXML
	private RadioButton defaultApp;
	@FXML
	private RadioButton specified;
	@FXML
	private TextField command;
	@FXML
	private ReactiveSpinner<Integer> offset;

	private final Window window;

	private final Map<String, List<String>> categories = new HashMap<>();
	private final Map<String, Tuple<String, CheckBox>> bookCategory = new HashMap<>();

	private JSONArray used;
	private boolean modified = true;

	/*
	 * Typical commands are:
	 * Acrobat Reader: AcroRd32.exe /A page=%p %f
	 * Evince: evince.exe --page-label=%p %f
	 */

	public BookSettingsPage(final Window window) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("BookSettingsPage.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		this.window = window;

		usedBooks.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		unusedBooks.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		initializeList(usedBooks);
		initializeList(unusedBooks);

		usedBooks.getSelectionModel().selectedIndexProperty().addListener((o, oldV, newV) -> {
			edit.setDisable(usedBooks.getSelectionModel().getSelectedIndices().size() != 1);
		});

		final String commandString = Settings.getSettingString("Allgemein", "Bücher:Befehl");
		if (commandString != null) {
			specified.setSelected(true);
			command.setDisable(false);
			command.setText(commandString);
			offset.setDisable(false);
			offset.getValueFactory().setValue(Settings.getSettingIntOrDefault(0, "Allgemein", "Bücher:Seitenoffset"));
		}

		specified.selectedProperty().addListener((o, oldV, newV) -> {
			command.setDisable(!newV);
			offset.setDisable(!newV);
			if (newV) {
				Settings.setSetting(command.getText().trim(), "Allgemein", "Bücher:Befehl");
				if (offset.getValue() != 0) {
					Settings.setSetting(offset.getValue(), "Allgemein", "Bücher:Seitenoffset");
				}
			} else {
				Settings.removeSetting("Allgemein", "Bücher:Befehl");
				Settings.removeSetting("Allgemein", "Bücher:Seitenoffset");
			}
		});

		command.textProperty().addListener((o, oldV, newV) -> {
			if ("".equals(newV.trim())) {
				Settings.removeSetting("Allgemein", "Bücher:Befehl");
			} else {
				Settings.setSetting(newV.trim(), "Allgemein", "Bücher:Befehl");
			}
		});

		offset.valueProperty().addListener((o, oldV, newV) -> {
			if (newV == 0) {
				Settings.removeSetting("Allgemein", "Bücher:Seitenoffset");
			} else {
				Settings.setSetting(newV, "Allgemein", "Bücher:Seitenoffset");
			}
		});

		final JSONObject books = ResourceManager.getResource("data/Buecher");
		for (final String categoryName : books.keySet()) {
			final List<String> categoryBooks = new LinkedList<>();
			categories.put(categoryName, categoryBooks);

			final Label label = new Label(categoryName + ":");
			label.setMaxWidth(Double.POSITIVE_INFINITY);
			HBox.setHgrow(label, Priority.ALWAYS);
			final CheckBox check = new CheckBox();
			box.getChildren().add(box.getChildren().size() - 2, new HBox(2, label, check));
			check.setOnAction(e -> {
				for (final String bookName : categoryBooks) {
					moveBook(bookName, check.isSelected() ? unusedBooks : usedBooks, check.isSelected() ? usedBooks : unusedBooks);
				}
			});

			final JSONObject category = books.getObj(categoryName);
			for (final String bookName : category.keySet()) {
				categoryBooks.add(bookName);
				bookCategory.put(bookName, new Tuple<>(categoryName, check));
				unusedBooks.getItems().add(bookName);
			}
		}

		used = Settings.getSettingArray("Allgemein", "Bücher");
		if (used == null) {
			ErrorLogger.log(
					"Einstellung für zu verwendende Bücher fehlt.\nAuf Standardeinstellung zurückgesetzt.\nBitte unter Einstellungen -> Regelwerke überprüfen.");
			final JSONObject general = ResourceManager.getResource("settings/Einstellungen").getObj("Allgemein");
			used = new JSONArray(general);
			for (final String bookName : books.getObj(books.keySet().iterator().next()).keySet()) {
				used.add(bookName);
			}
			general.put("Bücher", used);
		}

		for (int i = 0; i < used.size(); ++i) {
			final String name = used.getString(i);
			if (unusedBooks.getItems().contains(name)) {
				unusedBooks.getItems().remove(name);
				usedBooks.getItems().add(name);
				checkBook(name);
			}
		}

		modified = false;
	}

	private void checkBook(final String name) {
		final Tuple<String, CheckBox> categoryCheck = bookCategory.get(name);
		boolean allUsed = true;
		boolean allUnused = true;
		for (final String bookName : categories.get(categoryCheck._1)) {
			if (used.contains(bookName)) {
				allUnused = false;
			} else {
				allUsed = false;
			}
		}
		if (allUsed) {
			categoryCheck._2.setIndeterminate(false);
			categoryCheck._2.setSelected(true);
		} else if (allUnused) {
			categoryCheck._2.setIndeterminate(false);
			categoryCheck._2.setSelected(false);
		} else {
			categoryCheck._2.setIndeterminate(true);
		}
	}

	@FXML
	private void editBook() {
		final String item = usedBooks.getSelectionModel().getSelectedItem();
		if (item != null) {
			new ReferenceDialog(window, item);
		}
	}

	public Node getControl() {
		return pane;
	}

	private void initializeList(final ListView<String> bookList) {
		bookList.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2) {
				final String item = bookList.getSelectionModel().getSelectedItem();
				if (item != null) {
					new ReferenceDialog(window, item);
				}
			}
		});

		bookList.setCellFactory(list -> {
			final ListCell<String> cell = new TextFieldListCell<>();

			final ContextMenu menu = new ContextMenu();
			final MenuItem editItem = new MenuItem("Bearbeiten");
			editItem.setOnAction(e -> {
				final String item = cell.getItem();
				new ReferenceDialog(window, item);
			});
			menu.getItems().add(editItem);

			cell.contextMenuProperty().bind(Bindings.when(cell.itemProperty().isNotNull()).then(menu).otherwise((ContextMenu) null));

			GUIUtil.dragDropReorder(cell, moved -> {
				if (moved.length > 0) {
					used.clear();
					for (final String usedBook : usedBooks.getItems()) {
						used.add(usedBook);
					}
					for (final Object movedBook : moved) {
						checkBook((String) movedBook);
					}
					modify();
				}
			}, list, list == usedBooks ? unusedBooks : usedBooks);

			return cell;
		});
	}

	private void modify() {
		if (!modified) {
			Platform.runLater(() -> {
				final Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Hinweis");
				alert.setHeaderText("Ein Neustart des Tools ist erforderlich, um diese Änderung wirksam zu machen.");
				alert.show();
			});
			modified = true;
		}
	}

	private void moveBook(final String name, final ListView<String> source, final ListView<String> target) {
		if (source.getItems().contains(name)) {
			source.getItems().remove(name);
			target.getItems().add(name);
			if (source == usedBooks) {
				used.remove(name);
			} else {
				used.add(name);
			}
			checkBook(name);
			modify();
		}
	}

	@FXML
	public void removeBooks() {
		final List<String> toMove = new LinkedList<>();
		toMove.addAll(usedBooks.getSelectionModel().getSelectedItems());
		for (final String name : toMove) {
			moveBook(name, usedBooks, unusedBooks);
		}
	}

	@FXML
	public void useBooks() {
		final List<String> toMove = new LinkedList<>();
		toMove.addAll(unusedBooks.getSelectionModel().getSelectedItems());
		for (final String name : toMove) {
			moveBook(name, unusedBooks, usedBooks);
		}
	}
}
