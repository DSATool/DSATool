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

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

public class ReactiveComboBox<T> extends ComboBox<T> {
	public ReactiveComboBox() {
		super();

		getEditor().textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			final String text = getEditor().getText();

			final StringConverter<T> converter = getConverter();
			if (converter != null) {
				final T value = converter.fromString(text);
				setValue(value);
			}
		});
	}

	/**
	 * Creates a default ComboBox instance with the provided items list and
	 * a default {@link #selectionModelProperty() selection model}.
	 */
	public ReactiveComboBox(ObservableList<T> items) {
		super(items);

		getEditor().textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			final String text = getEditor().getText();

			final StringConverter<T> converter = getConverter();
			if (converter != null) {
				final T value = converter.fromString(text);
				setValue(value);
			}
		});
	}
}
