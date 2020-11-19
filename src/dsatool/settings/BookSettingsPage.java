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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

public class BookSettingsPage {
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

	private final Window window;

	private final Map<String, List<String>> categories = new HashMap<>();
	private final Map<String, Tuple<String, CheckBox>> bookCategory = new HashMap<>();

	private JSONArray used;
	private boolean modified = false;

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

		final JSONObject books = ResourceManager.getResource("data/Buecher");
		for (final String categoryName : books.keySet()) {
			final List<String> categoryBooks = new LinkedList<>();
			categories.put(categoryName, categoryBooks);

			final Label label = new Label(categoryName + ":");
			label.setMaxWidth(Double.POSITIVE_INFINITY);
			HBox.setHgrow(label, Priority.ALWAYS);
			final CheckBox check = new CheckBox();
			box.getChildren().add(box.getChildren().size() - 1, new HBox(2, label, check));
			check.selectedProperty().addListener((o, oldV, newV) -> {
				for (final String bookName : categoryBooks) {
					moveBook(bookName, newV ? unusedBooks : usedBooks, newV ? usedBooks : unusedBooks);
				}
			});

			final JSONObject category = books.getObj(categoryName);
			for (final String bookName : category.keySet()) {
				categoryBooks.add(bookName);
				bookCategory.put(bookName, new Tuple<>(categoryName, check));
				unusedBooks.getItems().add(bookName);
			}
		}

		used = Settings.getSettingArrayOrDefault(null, "Allgemein", "Bücher");
		if (used == null) {
			used = new JSONArray(ResourceManager.getResource("settings/Einstellungen").getObj("Allgemein"));
			for (final String bookName : books.getObj(books.keySet().iterator().next()).keySet()) {
				used.add(bookName);
			}
		}

		for (int i = 0; i < used.size(); ++i) {
			final String name = used.getString(i);
			if (unusedBooks.getItems().contains(name)) {
				unusedBooks.getItems().remove(name);
				usedBooks.getItems().add(name);
				checkBook(name);
			}
		}
	}

	private void checkBook(final String name) {
		final Tuple<String, CheckBox> categoryCheck = bookCategory.get(name);
		boolean allUsed = true;
		boolean allUnused = true;
		for (final String bookName : categories.get(categoryCheck._1)) {
			if (usedBooks.getItems().contains(bookName)) {
				allUnused = false;
			} else if (unusedBooks.getItems().contains(bookName)) {
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

			cell.setOnDragDetected(e -> {
				if (cell.isEmpty()) return;
				final Dragboard dragBoard = list.startDragAndDrop(TransferMode.MOVE);
				final ClipboardContent content = new ClipboardContent();
				final StringBuilder indices = new StringBuilder(list == usedBooks ? "used," : "unused,");
				for (final int index : list.getSelectionModel().getSelectedIndices()) {
					indices.append(index);
					indices.append(',');
				}
				content.putString(indices.substring(0, indices.length() - 1));
				dragBoard.setContent(content);
				e.consume();
			});

			cell.setOnDragDropped(e -> {
				final String[] indices = e.getDragboard().getString().split(",");
				final List<String> toMove = new LinkedList<>();
				ListView<String> source = null;
				for (final String index : indices) {
					if (source == null) {
						source = "used".equals(index) ? usedBooks : unusedBooks;
					} else {
						toMove.add(source.getItems().get(Integer.parseInt(index)));
					}
				}
				final String item = cell.getItem();
				for (final String name : toMove) {
					source.getItems().remove(name);
					if (source == usedBooks) {
						used.remove(name);
					}
				}
				final int index = item == null ? list.getItems().size() : list.getItems().indexOf(item);
				for (int i = 0; i < toMove.size(); ++i) {
					list.getItems().add(index + i, toMove.get(i));
					if (list == usedBooks) {
						used.add(index + i, toMove.get(i));
					}
					checkBook(toMove.get(i));
				}
				modify();
				e.setDropCompleted(true);
			});

			cell.setOnDragOver(e -> e.acceptTransferModes(TransferMode.MOVE));

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
		for (final String name : usedBooks.getSelectionModel().getSelectedItems()) {
			toMove.add(name);
		}
		for (final String name : toMove) {
			moveBook(name, usedBooks, unusedBooks);
		}
	}

	@FXML
	public void useBooks() {
		final List<String> toMove = new LinkedList<>();
		for (final String name : unusedBooks.getSelectionModel().getSelectedItems()) {
			toMove.add(name);
		}
		for (final String name : toMove) {
			moveBook(name, unusedBooks, usedBooks);
		}
	}
}
