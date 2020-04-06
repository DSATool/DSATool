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
package dsatool.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipOutputStream;

import org.controlsfx.dialog.CommandLinksDialog;
import org.controlsfx.dialog.CommandLinksDialog.CommandLinksButtonType;

import dsatool.plugins.Plugin;
import dsatool.plugins.PluginLoader;
import dsatool.util.ErrorLogger;
import dsatool.util.Util;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.FileChooser;

/**
 * Manages the zip file that contains the group data
 *
 * @author user
 */
public class GroupFileManager {
	/**
	 * Asks the user to create a new zip file for a new group
	 *
	 * @return True, if a new zip file was created, false otherwise
	 */
	public static boolean createNewZipFile() {
		final FileChooser dialog = new FileChooser();

		dialog.setTitle("Gruppe anlegen");
		dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.zip", "*.zip"));

		final File file = dialog.showSaveDialog(null);
		if (file != null) {
			ResourceManager.discardResources();
			try (final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
				ResourceManager.setZipFile(file);
				for (final Plugin plugin : PluginLoader.plugins.values()) {
					plugin.loadData();
				}
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
			return true;
		}
		return false;
	}

	/**
	 * Opens the current group or asks the user to create a new one
	 */
	public static void openCurrentGroup() {
		try (final BufferedReader reader = new BufferedReader(new FileReader(Util.getAppDir() + "/settings/Gruppe.txt"))) {
			final String zipPath = reader.readLine();
			if (zipPath != null) {
				final File zipFile = new File(zipPath);
				if (zipFile.exists()) {
					ResourceManager.setZipFile(new File(zipPath));
					return;
				}
			}
		} catch (final FileNotFoundException e) {
			// Nothing to do here, just progress to zipFile selection
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
		boolean zipFileExists = false;
		while (!zipFileExists) {
			final Dialog<ButtonType> groupDialog = new CommandLinksDialog(new CommandLinksButtonType("Gruppe erstellen", true),
					new CommandLinksButtonType("Gruppe laden", false), new CommandLinksButtonType("Schließen", false));
			groupDialog.setTitle("Neue Gruppe erstellen");
			groupDialog.setHeaderText(
					"Es wurde keine aktuelle Gruppe gefunden oder die aktuelle Gruppe konnte nicht geöffnet werden.\nWie soll vorgegangen werden?");
			final Optional<ButtonType> result = groupDialog.showAndWait();
			if (!result.isPresent()) {
				continue;
			}
			zipFileExists = switch (result.get().getText()) {
				case "Gruppe erstellen" -> createNewZipFile();
				case "Gruppe laden" -> openNewGroup();
				default -> {
					System.exit(0);
					yield false;
				}
			};
		}
	}

	/**
	 * Opens a group zip file
	 *
	 * @return True, if a zip file was opened, false otherwise
	 */
	public static boolean openNewGroup() {
		final FileChooser dialog = new FileChooser();

		dialog.setTitle("Gruppe öffnen");
		dialog.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("*.zip", "*.zip"));

		final File file = dialog.showOpenDialog(null);
		if (file != null) {
			ResourceManager.discardResources();
			ResourceManager.setZipFile(file);
			for (final Plugin plugin : PluginLoader.plugins.values()) {
				plugin.loadData();
			}
			return true;
		}
		return false;
	}

	private GroupFileManager() {}
}
