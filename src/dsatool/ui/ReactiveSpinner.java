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

import javafx.beans.NamedArg;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;

public class ReactiveSpinner<T> extends Spinner<T> {

	public ReactiveSpinner() {
		getEditor().textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			final String text = getEditor().getText();
			final SpinnerValueFactory<T> valueFactory = getValueFactory();
			if (valueFactory != null) {
				final StringConverter<T> converter = valueFactory.getConverter();
				if (converter != null) {
					final T value = converter.fromString(text);
					valueFactory.setValue(value);
				}
			}
		});
	}

	public ReactiveSpinner(@NamedArg("dmin") final double min, @NamedArg("dmax") final double max) {
		this(min, max, min, 1);
	}

	public ReactiveSpinner(@NamedArg("dmin") final double min, @NamedArg("dmax") final double max, @NamedArg("initialValue") final double initialValue) {
		this(min, max, initialValue, 1);
	}

	@SuppressWarnings("unchecked")
	public ReactiveSpinner(@NamedArg("dmin") final double min, @NamedArg("dmax") final double max, @NamedArg("initialValue") final double initialValue,
			@NamedArg("amountToStepBy") final double amountToStepBy) {
		super(min, max, initialValue, amountToStepBy);

		getEditor().textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			final String text = getEditor().getText();
			final SpinnerValueFactory<Double> valueFactory = (SpinnerValueFactory<Double>) getValueFactory();
			if (valueFactory != null) {
				final StringConverter<Double> converter = valueFactory.getConverter();
				if (converter != null) {
					Double value = 0.0;
					try {
						value = converter.fromString(text);
					} catch (final Exception e) {}
					valueFactory.setValue(value != null ? value : 0.0);
				}
			}
		});
	}

	public ReactiveSpinner(@NamedArg("imin") final int min, @NamedArg("imax") final int max) {
		this(min, max, min, 1);
	}

	public ReactiveSpinner(@NamedArg("imin") final int min, @NamedArg("imax") final int max, @NamedArg("initialValue") final int initialValue) {
		this(min, max, initialValue, 1);
	}

	@SuppressWarnings("unchecked")
	public ReactiveSpinner(@NamedArg("imin") final int min, @NamedArg("imax") final int max, @NamedArg("initialValue") final int initialValue,
			@NamedArg("amountToStepBy") final int amountToStepBy) {
		super(min, max, initialValue, amountToStepBy);

		getEditor().textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			final String text = getEditor().getText();
			final SpinnerValueFactory<Integer> valueFactory = (SpinnerValueFactory<Integer>) getValueFactory();
			if (valueFactory != null) {
				final StringConverter<Integer> converter = valueFactory.getConverter();
				if (converter != null) {
					Integer value = 0;
					try {
						value = converter.fromString(text);
					} catch (final Exception e) {}
					valueFactory.setValue(value != null ? value : 0);
				}
			}
		});
	}

	public ReactiveSpinner(@NamedArg("items") final ObservableList<T> items) {
		super(items);

		getEditor().textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> {
			final String text = getEditor().getText();
			final SpinnerValueFactory<T> valueFactory = getValueFactory();
			if (valueFactory != null) {
				final StringConverter<T> converter = valueFactory.getConverter();
				if (converter != null) {
					T value = null;
					try {
						value = converter.fromString(text);
					} catch (final Exception e) {}
					valueFactory.setValue(value);
				}
			}
		});
	}

	public final void setConverter(final StringConverter<T> converter) {
		getValueFactory().setConverter(converter);
		getEditor().setText(converter.toString(getValue()));
	}
}
