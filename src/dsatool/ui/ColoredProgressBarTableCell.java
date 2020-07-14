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

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ColoredProgressBarTableCell<S extends Colorable> extends GraphicTableCell<S, Double> {

	public ColoredProgressBarTableCell() {
		super(true);
	}

	private String createColor(final Color color) {
		return "#" + color.toString().substring(2, 8);
	}

	@Override
	protected void createGraphic() {
		final Label label = new Label();
		label.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000");
		final ProgressBar progressBar = new ProgressBar();
		progressBar.setMaxWidth(Double.MAX_VALUE);
		final StackPane pane = new StackPane(progressBar, label);
		setGraphic(pane);

		progressBar.progressProperty().bind(getTableColumn().getCellObservableValue(getIndex()));

		label.textProperty().bind(getTableView().getItems().get(getIndex()).textProperty());

		progressBar.setStyle("-fx-accent: " + createColor(getTableView().getItems().get(getIndex()).getColor()) + "; -fx-control-inner-background: "
				+ createColor(getTableView().getItems().get(getIndex()).getColor().brighter().desaturate().desaturate()) + ";");

		setText(null);
		setGraphic(pane);
	}
}
