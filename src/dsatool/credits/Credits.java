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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import dsatool.gui.Main;
import dsatool.util.ErrorLogger;
import dsatool.util.Util;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class Credits {

	public static List<Credits> credits = new ArrayList<>();

	static {
		credits.add(new Credits("DSATool\nCopyright (c) 2017, DSATool Team", "Apache License 2.0", Util.getAppDir() + "/licenses/ALv2.txt",
				"http://github.com/DSATool/DSATool", null));
		credits.add(new Credits("ControlsFX\nCopyright (c) 2013, ControlsFX", "Lizenz", Util.getAppDir() + "/licenses/controlsfx-license.txt",
				"http://fxexperience.com/controlsfx/", null));
		credits.add(new Credits("JavaFX\nCopyright (c) 2008, Oracle", "GPLv2 with Class Path Exception", Util.getAppDir() + "/licenses/GPLv2_CPE.txt",
				"https://openjfx.io/", null));
		credits.add(new Credits("Material Icons", "Apache License Version 2.0", Util.getAppDir() + "/licenses/ALv2.txt", "https://material.io/icons/", null));
		credits.add(new Credits("Jsonant", "Apache License Version 2.0", Util.getAppDir() + "/licenses/ALv2.txt", "https://github.com/errt/Jsonant", null));
		credits.add(new Credits("BoxTable", "Apache License Version 2.0", Util.getAppDir() + "/licenses/ALv2.txt", "https://github.com/errt/BoxTable", null));
	}

	private final String text;
	private final String license;
	private final String licenseUrl;
	private final String url;
	private final String imagePath;

	@FXML
	private Label textLabel;
	@FXML
	private Label licenseLabel;
	@FXML
	private Hyperlink licenseLink;
	@FXML
	private Hyperlink urlLabel;
	@FXML
	private ImageView image;

	public Credits(final String text, final String license, final String licenseUrl, final String url, final String imagePath) {
		this.text = text;
		this.license = license;
		this.licenseUrl = licenseUrl;
		this.url = url;
		this.imagePath = imagePath;
	}

	public Region getControl() {
		final FXMLLoader fxmlLoader = new FXMLLoader();

		fxmlLoader.setController(this);

		Region control = null;
		try {
			control = fxmlLoader.load(getClass().getResource("Credits.fxml").openStream());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}

		textLabel.setText(text);

		if (license != null && licenseUrl != null) {
			licenseLabel.setVisible(false);
			licenseLabel.setManaged(false);
			licenseLink.setText(license);
			licenseLink.setTooltip(new Tooltip(licenseUrl));
			licenseLink.setOnAction(e -> Main.app.getHostServices().showDocument(licenseUrl));
		} else {
			if (license != null) {
				licenseLabel.setText(license);
			} else {
				licenseLabel.setVisible(false);
				licenseLabel.setManaged(false);
			}
			licenseLink.setVisible(false);
			licenseLink.setManaged(false);
		}

		if (url != null) {
			urlLabel.setText(url);
			urlLabel.setOnAction(e -> Main.app.getHostServices().showDocument(url));
		} else {
			urlLabel.setVisible(false);
			urlLabel.setManaged(false);
		}

		if (imagePath != null) {
			try {
				image.setImage(new Image(new FileInputStream(new File(imagePath)), 150, 75, true, true));
			} catch (final FileNotFoundException e) {
				ErrorLogger.logError(e);
			}
		} else {
			image.setVisible(false);
			image.setManaged(false);
		}

		return control;
	}
}
