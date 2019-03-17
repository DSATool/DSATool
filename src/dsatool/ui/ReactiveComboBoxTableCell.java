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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ReactiveComboBoxTableCell<S> extends GraphicTableCell<S, String> {

	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn(final boolean alwaysVisible, final String... items) {
		return forTableColumn(FXCollections.observableArrayList(items), alwaysVisible);
	}

	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn(final ObservableList<String> items, final boolean alwaysVisible) {
		return list -> new ReactiveComboBoxTableCell<>(items, alwaysVisible);
	}

	protected final ObjectProperty<ObservableList<String>> items;

	public ReactiveComboBoxTableCell(final boolean alwaysVisible) {
		super(alwaysVisible);
		this.items = new SimpleObjectProperty<>();
	}

	public ReactiveComboBoxTableCell(final ObservableList<String> items, final boolean alwaysVisible) {
		super(alwaysVisible);
		this.items = new SimpleObjectProperty<>(items);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see dsatool.util.GraphicTableCell#createGraphic()
	 */
	@Override
	protected void createGraphic() {
		final ReactiveComboBox<String> comboBox = new ReactiveComboBox<>();
		comboBox.itemsProperty().bind(items);
		createGraphic(comboBox, () -> comboBox.getValue(), t -> comboBox.setValue(t));
	}

	public void setItems(final ObservableList<String> items) {
		this.items.set(items);
	}

}
