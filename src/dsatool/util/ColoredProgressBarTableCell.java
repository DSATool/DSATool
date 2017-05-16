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

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ColoredProgressBarTableCell<S extends Colorable> extends GraphicTableCell<S, Double> {

	public ColoredProgressBarTableCell() {
		super(true);
	}

	private String createColor(Color color) {
		final StringBuilder string = new StringBuilder("#");

		final String red = Integer.toHexString((int) (color.getRed() * 255));
		string.append(red.length() < 2 ? "0" + red : red);
		final String green = Integer.toHexString((int) (color.getGreen() * 255));
		string.append(green.length() < 2 ? "0" + green : green);
		final String blue = Integer.toHexString((int) (color.getBlue() * 255));
		string.append(blue.length() < 2 ? "0" + blue : blue);

		return string.toString();
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
