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
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class DoubleSpinnerTableCell<S> extends GraphicTableCell<S, Double> {

	public static <S> Callback<TableColumn<S, Double>, TableCell<S, Double>> forTableColumn(final double min, final double max, final double step,
			final boolean alwaysVisible, final BiFunction<DoubleSpinnerTableCell<S>, Boolean, Tuple<Double, Double>> update) {
		return list -> new DoubleSpinnerTableCell<>(min, max, step, alwaysVisible) {
			@Override
			public void updateItem(final Double item, final boolean empty) {
				final Tuple<Double, Double> bounds = update.apply(this, empty);
				min = bounds._1;
				max = bounds._2;
				super.updateItem(item, empty);
			}
		};
	}

	protected double min;
	protected double max;
	protected double step;

	public DoubleSpinnerTableCell(final double min, final double max, final boolean alwaysVisible) {
		super(alwaysVisible);
		this.min = min;
		this.max = max;
	}

	public DoubleSpinnerTableCell(final double min, final double max, final double step, final boolean alwaysVisible) {
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
		final ReactiveSpinner<Double> spinner = new ReactiveSpinner<>(min, max);
		spinner.setEditable(true);
		((DoubleSpinnerValueFactory) spinner.getValueFactory()).setAmountToStepBy(step);
		createGraphic(spinner, () -> spinner.getValue(), t -> spinner.getValueFactory().setValue(t));
	}

	@Override
	public void startEdit() {
		if (getItem() == null || min == max) return;
		super.startEdit();
	}

	@Override
	public void updateItem(final Double item, final boolean empty) {
		if (empty) {
			setText("");
			setGraphic(null);
		} else {
			super.updateItem(item, empty);
		}
	}
}
