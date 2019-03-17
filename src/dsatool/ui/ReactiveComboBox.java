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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

public class ReactiveComboBox<T> extends ComboBox<T> {
	private static EventHandler<ActionEvent> filter = e -> e.consume();
	private final StringBuilder searchString = new StringBuilder();

	private long lastChange = 0;

	private final ChangeListener<String> textListener = (observable, oldValue, newValue) -> {
		final TextField editor = getEditor();
		final int previousCaret = editor.getCaretPosition() + 1;
		final String text = newValue;

		T item = null;
		if (previousCaret > 0 && previousCaret <= text.length()) {
			final String searchText = editor.getText().substring(0, previousCaret);
			final T selected = getSelectionModel().getSelectedItem();
			if (selected == null || !selected.toString().toLowerCase().startsWith(searchText.toLowerCase())) {
				item = findItem(searchText);
			}
		}

		if (item != null) {
			editor.setText(item.toString());
			Platform.runLater(() -> {
				editor.positionCaret(editor.getText().length());
				editor.selectPositionCaret(previousCaret);
			});
		} else {
			final StringConverter<T> converter = getConverter();
			if (converter != null) {
				addEventFilter(ActionEvent.ACTION, filter);
				setValue(converter.fromString(text));
				removeEventFilter(ActionEvent.ACTION, filter);
			}
		}
	};

	private final EventHandler<KeyEvent> searchHandler = event -> {
		if (!isEditable()) {
			final long current = System.currentTimeMillis();

			if (current > lastChange + 750) {
				searchString.setLength(0);
			}
			lastChange = current;
			final KeyCode code = event.getCode();
			if (code == KeyCode.BACK_SPACE && searchString.length() > 0) {
				searchString.setLength(searchString.length() - 1);
			} else if (code == KeyCode.ESCAPE) {
				searchString.setLength(0);
			} else if (!code.isArrowKey() && !code.isFunctionKey() && !code.isMediaKey() && !code.isModifierKey()) {
				searchString.append(event.getText());
			} else
				return;

			final T item = findItem(searchString.toString());
			if (item != null) {
				addEventFilter(ActionEvent.ACTION, filter);
				setValue(item);
				removeEventFilter(ActionEvent.ACTION, filter);
			}
		}
	};

	public ReactiveComboBox() {
		super();

		getEditor().textProperty().addListener(textListener);
		setOnKeyPressed(searchHandler);
	}

	public ReactiveComboBox(final ObservableList<T> items) {
		super(items);

		getEditor().textProperty().addListener(textListener);
		setOnKeyPressed(searchHandler);
	}

	private final T findItem(final String prefix) {
		final ObservableList<T> items = getItems();
		for (int i = 0; i < items.size(); i++) {
			if (items.get(i).toString().toLowerCase().startsWith(prefix.toLowerCase())) return items.get(i);
		}
		return null;
	}
}
