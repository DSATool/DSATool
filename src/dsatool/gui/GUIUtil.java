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
package dsatool.gui;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class GUIUtil {

	public static void autosizeTable(final TableView<?> table, final int resizeableCol, final double additionalSpace) {
		DoubleBinding width = table.widthProperty().subtract(additionalSpace);

		for (int i = 0; i < table.getColumns().size(); ++i) {
			if (i != resizeableCol) {
				width = width.subtract(table.getColumns().get(i).widthProperty());
			}
		}
		table.getColumns().get(resizeableCol).prefWidthProperty().bind(width);
	}

	public static void cellValueFactories(final TableView<?> table, final String... properties) {
		for (int i = 0; i < table.getColumns().size(); ++i) {
			table.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(properties[i]));
		}
	}

	private GUIUtil() {}

}
