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

import java.util.function.BiFunction;

import dsatool.util.Tuple;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class IntegerSpinnerTableCell<S> extends GraphicTableCell<S, Integer> {

	public static <S> Callback<TableColumn<S, Integer>, TableCell<S, Integer>> forTableColumn(final int min, final int max, final int step,
			final boolean alwaysVisible, final BiFunction<IntegerSpinnerTableCell<S>, Boolean, Tuple<Integer, Integer>> update) {
		return list -> new IntegerSpinnerTableCell<>(min, max, step, alwaysVisible) {
			@Override
			public void updateItem(final Integer item, final boolean empty) {
				final Tuple<Integer, Integer> bounds = update.apply(this, empty);
				min = bounds._1;
				max = bounds._2;
				super.updateItem(item, empty);
			}
		};
	}

	protected int min;
	protected int max;
	protected int step;

	public IntegerSpinnerTableCell(final int min, final int max) {
		this(min, max, 1, false);
	}

	public IntegerSpinnerTableCell(final int min, final int max, final boolean alwaysVisible) {
		this(min, max, 1, alwaysVisible);
	}

	public IntegerSpinnerTableCell(final int min, final int max, final int step, final boolean alwaysVisible) {
		super(alwaysVisible);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	@Override
	protected void createGraphic() {
		if (min == max) {
			setGraphic(null);
			return;
		}
		final ReactiveSpinner<Integer> spinner = new ReactiveSpinner<>(min, max);
		spinner.setEditable(true);
		((IntegerSpinnerValueFactory) spinner.getValueFactory()).setAmountToStepBy(step);
		createGraphic(spinner, spinner::getValue, spinner.getValueFactory()::setValue);
	}

	@Override
	public void startEdit() {
		if (getItem() == null || getItem() == Integer.MIN_VALUE || min == max) return;
		super.startEdit();
	}

	@Override
	public void updateItem(final Integer item, final boolean empty) {
		if (empty || item.equals(Integer.MIN_VALUE)) {
			setText("");
			setGraphic(null);
		} else {
			super.updateItem(item, empty);
		}
	}
}
