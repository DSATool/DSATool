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
package dsatool.credits;

import dsatool.gui.Main;
import dsatool.util.ErrorLogger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class CreditsDialog {
	@FXML
	private Parent pane;
	@FXML
	private ScrollPane scroll;
	@FXML
	private VBox box;

	private final Stage stage;

	public CreditsDialog(final Window window) {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		try {
			fxmlLoader.load(getClass().getResource("CreditsDialog.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		stage = new Stage();
		stage.setTitle("Über DSATool");
		stage.setScene(new Scene(pane, 600, 350));
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(window);

		box.prefWidthProperty().bind(stage.widthProperty().subtract(35));

		stage.show();

		boolean first = true;
		for (final Credits credits : Credits.credits) {
			if (first) {
				first = false;
			} else {
				box.getChildren().add(new Separator());
			}
			final Region control = credits.getControl();
			control.prefWidthProperty().bind(box.widthProperty());
			box.getChildren().add(control);
		}
	}

	@FXML
	public void close() {
		stage.close();
	}

	@FXML
	public void openToolWebsite() {
		Main.app.getHostServices().showDocument("https://dsatool.github.io/");
	}
}
