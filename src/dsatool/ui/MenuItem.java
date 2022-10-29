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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;

/**
 * Represents a single menu entry
 *
 * @author Dominik Helm
 */
public class MenuItem {

	/**
	 * The widget managed by this item
	 */
	private final Object item;

	/**
	 * Constructs a MenuItem for the given widget
	 *
	 * @param widget
	 *            The widget managed by this MenuItem
	 */
	public MenuItem(final Object item) {
		this.item = item;
	}

	/**
	 * Adds a context menu entry to this MenuItem if possible
	 *
	 * @return A MenuItem that manages the new context menu entry, or null, if this MenuItem doesn't support context
	 *         menus
	 */
	public MenuItem addContextMenuEntry() {
		if (item instanceof final Control w) {
			ContextMenu menu = w.getContextMenu();
			if (menu == null) {
				menu = new ContextMenu();
				w.setContextMenu(menu);
			}
			final javafx.scene.control.MenuItem context = new javafx.scene.control.MenuItem();
			menu.getItems().add(context);
			return new MenuItem(context);
		}
		return null;
	}

	/**
	 * Sets an action to be executed when this MenuItem is selected
	 *
	 * @param action
	 *            The action to be executed (will be passed null as parameter!)
	 */
	public void setAction(final EventHandler<ActionEvent> action) {
		if (item instanceof final javafx.scene.control.MenuItem mi) {
			mi.setOnAction(action);
		} else if (item instanceof final Button b) {
			b.setOnAction(action);
		}
	}

	/**
	 * Changes the displayed text of this MenuItem if possible
	 *
	 * @param name
	 *            The new text
	 */
	public void setText(final String name) {
		if (item instanceof final javafx.scene.control.MenuItem mi) {
			mi.setText(name);
		} else if (item instanceof final Button b) {
			b.setText(name);
		}
	}
}
