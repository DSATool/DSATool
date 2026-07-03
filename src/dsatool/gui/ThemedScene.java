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

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import dsatool.resources.ResourceManager;
import dsatool.resources.Settings;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import jsonant.event.JSONListener;
import jsonant.value.JSONObject;

public class ThemedScene extends Scene {

	private static List<WeakReference<ThemedScene>> allScenes = new LinkedList<>();
	private static String theme = null;
	private static JSONListener listener = null;

	synchronized protected static String getTheme() {
		if (theme == null) {
			theme = getThemeSheet();
		}
		return theme;
	}

	private static String getThemeSheet() {
		final String themeFile = switch (Settings.getSettingString("Allgemein", "Thema")) {
			case "Stein" -> "stone-theme.css";
			case "Marmor" -> "marble-theme.css";
			case "Pergament" -> "parchment-theme.css";
			case "Holz" -> "wood-theme.css";
			case "Anthrazit" -> "ash-theme.css";
			case "Mine" -> "mine-theme.css";
			case "Gift" -> "poison-theme.css";
			case "Wald" -> "forest-theme.css";
			case "Gras" -> "grass-theme.css";
			case "Meer" -> "sea-theme.css";
			case "Himmel" -> "sky-theme.css";
			case null, default -> "stone-theme.css";
		};
		return ThemedScene.class.getResource(themeFile).toExternalForm();
	}

	public StringProperty titleProperty;

	public ThemedScene(final Parent root, final double width, final double height) {
		final BorderPane main = new BorderPane();

		final HeaderBar headerBar = new HeaderBar();
		headerBar.getStyleClass().add("header-bar");

		final Label titleLabel = new Label();
		titleLabel.getStyleClass().add("title-label");
		titleProperty = titleLabel.textProperty();
		HeaderBar.setDragType(titleLabel, HeaderDragType.DRAGGABLE_SUBTREE);

		headerBar.setCenter(titleLabel);
		main.setTop(headerBar);
		main.setCenter(root);

		super(main, width, height);

		final ObservableList<String> localStyleSheets = getStylesheets();
		localStyleSheets.add(getClass().getResource("application.css").toExternalForm());

		localStyleSheets.add(getTheme());
		setFill();

		synchronized (ThemedScene.class) {
			allScenes.add(new WeakReference<>(this));

			if (listener == null) {
				listener = settings -> {
					final String newThemeSheet = getThemeSheet();
					if (!newThemeSheet.equals(getTheme())) {
						theme = newThemeSheet;
						allScenes.removeIf(ref -> ref.get() == null);
						for (final WeakReference<ThemedScene> ref : allScenes) {
							final ThemedScene scene = ref.get();
							if (scene != null) {
								final ObservableList<String> styleSheets = scene.getStylesheets();
								styleSheets.removeIf(url -> url.endsWith("-theme.css"));
								styleSheets.add(newThemeSheet);
								setFill();
							}
						}
					}
				};
				final JSONObject settings = ResourceManager.getResource("settings/Einstellungen").getObj("Allgemein");
				settings.addListener(listener);
			}
		}

	}

	private void setFill() {
		final Parent root = getRoot();
		root.applyCss();
		for (final CssMetaData<? extends Styleable, ?> metaData : root.getCssMetaData()) {
			if ("-fx-region-background".equals(metaData.getProperty())) {
				@SuppressWarnings("unchecked")
				final StyleableProperty<Background> prop = ((CssMetaData<Parent, Background>) metaData).getStyleableProperty(root);
				setFill(prop.getValue().getFills().get(0).getFill());
			}
		}
	}
}
