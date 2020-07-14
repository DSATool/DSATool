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
package dsatool.control;

import java.util.Optional;

import org.controlsfx.control.StatusBar;

import dsatool.credits.CreditsDialog;
import dsatool.gui.Main;
import dsatool.resources.GroupFileManager;
import dsatool.resources.ResourceManager;
import dsatool.settings.SettingsDialog;
import dsatool.ui.MenuGroup;
import dsatool.update.Update;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindowController {
	@FXML
	private BorderPane mainPane;

	@FXML
	private MenuBar menu;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	private StatusBar statusBar;

	@FXML
	private Pane toolArea;

	@FXML
	private Pane toolSelector;

	private Stage window;

	/**
	 * Asks the user whether to save unsaved changes
	 *
	 * @return True, if the current operation is to be continued, false if it is
	 *         to be cancelled
	 */
	private boolean askSaveChanges() {
		resizeToolSelector();
		final Alert saveConfirmation = new Alert(AlertType.CONFIRMATION);
		saveConfirmation.setTitle("Änderungen speichern?");
		saveConfirmation.setHeaderText("Änderungen speichern?");
		saveConfirmation.setContentText("Sollen ungespeicherte Änderungen jetzt gespeichert werden?");
		saveConfirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

		final Optional<ButtonType> result = saveConfirmation.showAndWait();
		if (result.isPresent() && result.get().equals(ButtonType.NO)) {
			ResourceManager.discardChanges();
		}
		return result.isPresent() && !result.get().equals(ButtonType.CANCEL);
	}

	public StatusBar getStatusBar() {
		return statusBar;
	}

	public Pane getToolArea() {
		return toolArea;
	}

	public void initializeMenus() {
		final MenuBar menuBar = menu;
		Main.mainMenu = new dsatool.ui.Menu(groupName -> {
			final Menu group = new Menu(groupName);
			menuBar.getMenus().add(group);
			return new MenuGroup(itemName -> {
				final MenuItem item = new MenuItem(itemName);
				group.getItems().add(item);
				return new dsatool.ui.MenuItem(item);
			}, o -> {
				group.getItems().add(new SeparatorMenuItem());
			});
		});
		Main.toolsMenu = new dsatool.ui.Menu(groupName -> {
			final TitledPane menu = new TitledPane();
			menu.setAnimated(false);
			menu.setText(groupName);
			toolSelector.getChildren().add(menu);
			final VBox group = new VBox();
			menu.setContent(group);
			return new MenuGroup(itemName -> {
				final Button item = new Button(itemName);
				group.getChildren().add(item);
				item.setMaxWidth(Double.MAX_VALUE);
				VBox.setVgrow(item, Priority.ALWAYS);
				return new dsatool.ui.MenuItem(item);
			});
		});
		final MenuGroup file = Main.mainMenu.addGroup("Datei");
		file.addItem("Neu").setAction(o -> {
			if (askSaveChanges()) {
				GroupFileManager.createNewZipFile();
			}
		});
		file.addItem("Laden").setAction(o -> {
			if (askSaveChanges()) {
				GroupFileManager.openNewGroup();
			}
		});
		file.addItem("Speichern").setAction(o -> {
			ResourceManager.saveResources();
		});
		file.addSeparator();
		file.addItem("Neu laden").setAction(o -> {
			ResourceManager.discardChanges();
		});
		file.addSeparator();
		file.addItem("Schließen").setAction(o -> {
			if (askSaveChanges()) {
				Platform.exit();
			}
		});

		final MenuGroup edit = Main.mainMenu.addGroup("Bearbeiten");
		edit.addItem("Einstellungen").setAction(o -> {
			new SettingsDialog(window);
		});

		final MenuGroup help = Main.mainMenu.addGroup("Hilfe");
		help.addItem("Nach Updates suchen").setAction(o -> {
			new Thread(() -> new Update().searchUpdates(true)).start();
		});
		help.addItem("Über DSATool").setAction(o -> {
			new CreditsDialog(window);
		});
	}

	public void resizeToolSelector() {
		scrollPane.setPrefWidth(toolSelector.prefWidth(0) + 52);
	}

	public void setStage(final Stage stage) {
		window = stage;

		window.setOnCloseRequest(o -> {
			ResourceManager.saveResources();
			Platform.exit();
		});
	}
}
