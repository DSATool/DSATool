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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dsatool.gui.GUIUtil;
import dsatool.ui.ReactiveComboBox;
import dsatool.ui.ReactiveSpinner;
import dsatool.util.ErrorLogger;
import dsatool.util.Util;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class SettingsPage {
	@FXML
	protected VBox box;
	@FXML
	private ScrollPane pane;

	private int inset = 5;
	private int sectionInset = 0;

	private final Map<String, Property<?>> properties = new HashMap<>();

	private LinkedHashMap<TitledPane, Map<String, Property<?>>> sections;
	private int sectionStart;
	private TitledPane currentSection;

	public SettingsPage() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("SettingsPage.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}
	}

	public CheckBox addBooleanChoice(final String text) {
		final CheckBox result = new CheckBox();
		result.setPadding(new Insets(2, 0, 0, 0));
		final BooleanProperty property = result.selectedProperty();
		createLine(text, result, property);
		return result;
	}

	public Button addFileChoice(final String text, final String extensionDesc, final List<String> fileExtensions) {
		final Button result = new Button("Kein");
		final ObjectProperty<File> property = new SimpleObjectProperty<>();
		createLine(text, result, property);
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

	public ReactiveSpinner<Integer> addIntegerChoice(final String text, final int min, final int max) {
		final ReactiveSpinner<Integer> result = new ReactiveSpinner<>(min, max);
		result.setEditable(true);

		result.setPrefWidth(45 + (min < 0 ? 10 : 0) + Math.floor(Math.log10(max)) * 10);

		final IntegerProperty property = new SimpleIntegerProperty();
		createLine(text, result, property);
		result.getValueFactory().valueProperty().addListener((o, oldV, newV) -> {
			property.set(newV);
		});
		property.addListener((o, oldV, newV) -> {
			result.getValueFactory().setValue(newV.intValue());
		});
		return result;
	}

	public void addLabel(final String text) {
		addNode(createLabel(text));
	}

	public void addNode(final Node node) {
		final VBox parent = currentSection != null ? (VBox) currentSection.getContent() : box;
		parent.getChildren().add(node);
		if (currentSection != null) {
			currentSection.setCollapsible(true);
			currentSection.setExpanded(true);
		}
	}

	public TitledPane addSection(final String title, final boolean needsCheckbox) {
		if (sections == null) {
			sections = new LinkedHashMap<>();
			sectionStart = box.getChildren().size();
		}
		final VBox content = new VBox(2);
		content.setPadding(new Insets(1, -1, 1, -1));
		final TitledPane section = new TitledPane();
		section.setContent(content);
		box.getChildren().add(section);
		section.minWidthProperty().bind(box.widthProperty());
		section.maxWidthProperty().bind(box.widthProperty());
		VBox.setMargin(section, new Insets(0, -2, 0, 0));
		currentSection = section;

		section.setCollapsible(false);
		section.setExpanded(false);

		final Map<String, Property<?>> properties = new HashMap<>();
		sections.put(section, properties);

		GUIUtil.dragDropReorder(section, o -> {
			final int index = box.getChildren().indexOf(o) - sectionStart;
			moveSection((TitledPane) o, index);
		}, box);

		final ContextMenu contextMenu = new ContextMenu();
		final MenuItem upItem = new MenuItem("Nach oben");
		upItem.setOnAction(event -> {
			final int index = box.getChildren().indexOf(section) - sectionStart;
			moveSection(section, index - 1);
		});
		contextMenu.getItems().add(upItem);
		final MenuItem downItem = new MenuItem("Nach unten");
		downItem.setOnAction(event -> {
			final int index = box.getChildren().indexOf(section) - sectionStart;
			moveSection(section, index + 1);
		});
		contextMenu.getItems().add(downItem);
		contextMenu.setOnShowing(e -> {
			final int index = box.getChildren().indexOf(section) - sectionStart;
			upItem.setVisible(index > 0);
			downItem.setVisible(index < sections.size() - 1);
		});
		section.setContextMenu(contextMenu);

		if (needsCheckbox) {
			final HBox titleBox = new HBox();
			section.setGraphic(titleBox);
			titleBox.prefWidthProperty().bind(pane.widthProperty().add(-52 + sectionInset));
			titleBox.paddingProperty().bind(Bindings.createObjectBinding(() -> section.isCollapsible() ? new Insets(0, 0, 0, 0) : new Insets(0, -15, 0, 15),
					section.collapsibleProperty()));

			final Label titleLabel = new Label(title);
			titleLabel.setMaxWidth(Double.POSITIVE_INFINITY);
			HBox.setHgrow(titleLabel, Priority.ALWAYS);
			properties.put(null, titleLabel.textProperty());

			final CheckBox check = new CheckBox();
			titleBox.getChildren().addAll(titleLabel, check);
			HBox.setMargin(check, new Insets(0, -6, 0, 0));
			check.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
			properties.put("", check.selectedProperty());
		} else {
			final Label titleLabel = new Label(title);
			section.setGraphic(titleLabel);
			titleLabel.paddingProperty().bind(Bindings.createObjectBinding(() -> section.isCollapsible() ? new Insets(0, 0, 0, 0) : new Insets(0, 0, 0, 15),
					section.collapsibleProperty()));
			properties.put(null, section.textProperty());
		}

		return section;
	}

	public void addSeparator() {
		addNode(new Separator());
	}

	public ComboBox<String> addStringChoice(final String text, final Collection<String> choices) {
		final ComboBox<String> result = new ReactiveComboBox<>(FXCollections.observableArrayList(choices));
		final StringProperty property = new SimpleStringProperty();
		createLine(text, result, property);
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
		properties.clear();
		if (sections != null) {
			sections.clear();
		}
		currentSection = null;
	}

	private Label createLabel(final String text) {
		final Label label = new Label(text + ":");
		label.setPadding(new Insets(5, 0, 0, inset));
		label.setMaxWidth(Double.POSITIVE_INFINITY);
		return label;
	}

	public void createLine(final String text, final Control control, final Property<?> property) {
		final Label label = createLabel(text);
		HBox.setHgrow(label, Priority.ALWAYS);
		HBox.setMargin(control, new Insets(0, inset, 0, 0));
		final HBox line = new HBox(2, label, control);
		addNode(line);
		if (currentSection == null) {
			if (properties.containsKey(text)) {
				ErrorLogger.log("Name bereits vergeben: " + text);
			}
			properties.put(text, property);
		} else {
			sections.get(currentSection).put(text, property);
		}
	}

	public void endSection() {
		currentSection = null;
	}

	public BooleanProperty getBool(final String key) {
		return (BooleanProperty) properties.get(key);
	}

	public BooleanProperty getBool(final TitledPane section, final String key) {
		return (BooleanProperty) sections.get(section).get(key);
	}

	public ScrollPane getControl() {
		return pane;
	}

	@SuppressWarnings("unchecked")
	public ObjectProperty<File> getFile(final String key) {
		return (ObjectProperty<File>) properties.get(key);
	}

	@SuppressWarnings("unchecked")
	public ObjectProperty<File> getFile(final TitledPane section, final String key) {
		return (ObjectProperty<File>) sections.get(section).get(key);
	}

	public IntegerProperty getInt(final String key) {
		return (IntegerProperty) properties.get(key);
	}

	public IntegerProperty getInt(final TitledPane section, final String key) {
		return (IntegerProperty) sections.get(section).get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> Property<T> getProperty(final String key) {
		return (Property<T>) properties.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> Property<T> getProperty(final TitledPane section, final String key) {
		return (Property<T>) sections.get(section).get(key);
	}

	public Set<TitledPane> getSections() {
		if (sections == null)
			return Collections.emptySet();
		return sections.keySet();
	}

	public StringProperty getString(final String key) {
		return (StringProperty) properties.get(key);
	}

	public StringProperty getString(final TitledPane section, final String key) {
		return (StringProperty) sections.get(section).get(key);
	}

	public void moveSection(final TitledPane section, final int index) {
		final Map<String, Property<?>> movedProperties = sections.remove(section);
		Util.putAt(sections, index, section, movedProperties);
		box.getChildren().remove(section);
		box.getChildren().add(index + sectionStart, section);
	}

	public void removeNode(final Node node) {
		box.getChildren().remove(node);
	}

	public void removeSection(final TitledPane section) {
		sections.remove(section);
		box.getChildren().remove(section);
		if (currentSection == section) {
			for (final TitledPane otherSection : sections.keySet()) {
				currentSection = otherSection;
			}
		}
	}

	public void setInsets(final int inset, final int sectionInset) {
		this.inset = inset;
		this.sectionInset = sectionInset;
	}
}
