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

import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public abstract class GraphicListCell<T> extends ListCell<T> {

	private final boolean alwaysVisible;
	private Node graphic;

	public GraphicListCell(final boolean alwaysVisible) {
		this.alwaysVisible = alwaysVisible;
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		if (!alwaysVisible) {
			setText(getItem() != null ? getItem().toString() : "");
			setPadding(new Insets(5, 0, 0, 5));
			setGraphic(graphic);
		}
	}

	protected abstract void createGraphic();

	protected void createGraphic(final Control graphic, final Supplier<T> getText, final Consumer<T> setText) {
		setPadding(Insets.EMPTY);
		graphic.setMaxWidth(Double.MAX_VALUE);
		graphic.setContextMenu(getContextMenu());
		final ObjectProperty<T> string = itemProperty();
		setText.accept(string.get());
		setText(null);
		setGraphic(graphic);
		final ListView<T> list = getListView();
		final int row = getIndex();
		graphic.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
				if (oldValue && !newValue) {
					graphic.focusedProperty().removeListener(this);
					Event.fireEvent(list, new ListView.EditEvent<>(list, ListView.<T> editCommitEvent(), getText.get(), row));
					cancelEdit();
				}
			}
		});
		if (graphic instanceof final TextField tf) {
			tf.setOnAction(e -> {
				requestFocus();
			});
		} else if (graphic instanceof final ButtonBase b) {
			b.setOnAction(e -> {
				requestFocus();
			});
		} else if (graphic instanceof final ComboBoxBase<?> cb) {
			cb.setOnAction(e -> {
				requestFocus();
			});
		}
	}

	@Override
	public void startEdit() {
		graphic = getGraphic();
		if (!isEmpty() && !alwaysVisible) {
			createGraphic();
		}
		getGraphic().requestFocus();
	}

	@Override
	public void updateItem(final T item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			if (alwaysVisible || isEditing()) {
				if (getGraphic() == null) {
					createGraphic();
				}
			} else {
				if (item == null) {
					setText("");
				} else {
					setText(item.toString());
				}
				setPadding(new Insets(5, 0, 0, 5));
				setGraphic(null);
			}
		}
	}
}
