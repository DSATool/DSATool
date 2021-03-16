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
package dsatool.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.LabeledSkinBase;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import jsonant.value.JSONObject;

public class Util {

	public final static String javaExecutable = ProcessHandle.current().info().command().orElse("java");

	public static void addReference(final Labeled control, final JSONObject data, final double padding, final ReadOnlyDoubleProperty width) {
		final JSONObject books = ResourceManager.getResource("settings/Buecher");
		final JSONObject refs = data.containsKey("Regelwerke") ? data.getObj("Regelwerke") : ResourceManager.getDiscrimination(data);
		if (refs != null && refs.size() > 0) {
			String name = null;
			for (final String bookName : refs.keySet()) {
				if (books.containsKey(bookName) && books.getObj(bookName).containsKey("Pfad")) {
					name = bookName;
					break;
				}
			}
			if (name == null) {
				name = refs.keySet().iterator().next();
			}

			final String finalName = name;
			final Label iconLabel = new Label("\uE54B");
			iconLabel.getStyleClass().add("icon-font");
			iconLabel.setTextFill(Color.DIMGREY);
			final String page = Integer.toString(refs.getInt(name));

			final Hyperlink label = new Hyperlink(page, iconLabel);
			label.setGraphicTextGap(0);
			label.setTextFill(Color.DIMGREY);
			label.getStyleClass().add("flat-link");
			label.setOnAction(o -> openBook(finalName, books.getObj(finalName), refs.getInt(finalName)));

			boolean first = true;
			final ContextMenu openMenu = new ContextMenu();
			final StringBuilder tooltip = new StringBuilder();
			for (final String bookName : refs.keySet()) {
				final String reference = bookName + " S. " + refs.getInt(bookName);
				final MenuItem openItem = new MenuItem(reference);
				openItem.setOnAction(e -> openBook(bookName, books.getObj(bookName), refs.getInt(bookName)));
				openMenu.getItems().add(openItem);

				if (first) {
					first = false;
				} else {
					tooltip.append("\n");
				}
				tooltip.append(reference);
			}
			label.setTooltip(new Tooltip(tooltip.toString()));

			final Label graphic = new Label("", label);
			graphic.setAlignment(Pos.CENTER_RIGHT);
			graphic.setPadding(new Insets(0, (3 - page.length()) * 6, 0, 0));
			control.setContentDisplay(ContentDisplay.RIGHT);
			control.setGraphic(graphic);
			if (control.getSkin() != null) {
				final Text text = (Text) ((LabeledSkinBase<?>) control.getSkin()).getChildren().get(1);
				text.setText(control.getText());
				graphic.minWidthProperty()
						.bind(Bindings.max(width.subtract(text.getLayoutBounds().getWidth() + padding), label.widthProperty()));
			} else {
				control.skinProperty().addListener((o, oldV, newV) -> {
					if (newV != null) {
						final Text text = (Text) ((LabeledSkinBase<?>) newV).getChildren().get(1);
						text.setText(control.getText());
						graphic.minWidthProperty()
								.bind(Bindings.max(width.subtract(text.getLayoutBounds().getWidth() + padding), label.widthProperty()));
					}
				});
			}
			if (refs.size() > 1) {
				label.setContextMenu(openMenu);
			}
		} else {
			control.setGraphic(null);
		}
	}

	public static Alert alert(final String text) {
		final Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Fehler");
		alert.setHeaderText("Ein Fehler ist aufgetreten.\nWeitere Informationen befinden sich unter ./error.log");

		final TextArea textArea = new TextArea(text);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(textArea);

		return alert;
	}

	public static <T> ChangeListener<T> changeListener(final BooleanSupplier check, final Consumer<T> listener) {
		return (observable, oldV, newV) -> {
			if (newV == null || oldV == null || oldV.equals(newV) || check.getAsBoolean()) return;
			listener.accept(newV);
		};
	}

	public static Alert exceptionAlert(final Exception e) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		final String exceptionText = sw.toString();

		return alert(exceptionText);
	}

	public static String getAppDir() {
		try {
			final File directory = new File(Util.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
			return directory.getCanonicalPath();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getSignedIntegerString(final long i) {
		if (i == 0)
			return "±0";
		else if (i > 0)
			return "+" + i;
		else
			return Long.toString(i);
	}

	private static void openBook(final String bookName, final JSONObject book, final int page) {
		final File file = new File(book.getStringOrDefault("Pfad", ""));
		if (file.exists()) {
			try {
				if (book.containsKey("Befehl")) {
					openWith(file, page + book.getIntOrDefault("Seitenoffset", 0), book.getString("Befehl"));
				} else {
					final String commandString = Settings.getSettingString("Allgemein", "Bücher:Befehl");
					if (commandString != null) {
						final int offset = book.getIntOrDefault("Seitenoffset", Settings.getSettingIntOrDefault(0, "Allgemein", "Bücher:Seitenoffset"));
						openWith(file, page + offset, commandString);
					} else {
						openFile(file);
					}
				}
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
		}
	}

	public static void openFile(final File file) throws IOException {
		final String osName = System.getProperty("os.name").toLowerCase();
		boolean openedSuccessfully = false;
		if (osName.contains("win")) {
			openedSuccessfully = run("explorer " + file.getAbsolutePath());
		} else if (osName.contains("linux")) {
			openedSuccessfully = run("xdg-open " + file.getAbsolutePath());
		} else if (osName.contains("mac")) {
			openedSuccessfully = run("open " + file.getAbsolutePath());
		}
		if (!openedSuccessfully && Desktop.isDesktopSupported()) {
			Desktop.getDesktop().open(file);
		}
	}

	private static void openWith(final File file, final int page, final String command) throws IOException {
		Runtime.getRuntime().exec(command.replace("%f", "\"" + file.getAbsolutePath() + "\"").replace("%p", Integer.toString(page)));
	}

	private static boolean run(final String command) throws IOException {
		final Process process = Runtime.getRuntime().exec(command);
		try {
			process.exitValue();
		} catch (final IllegalThreadStateException e) {
			return true;
		}
		return false;
	}

	private Util() {}
}
