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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a group of related menu items
 *
 * @author Dominik Helm
 */
public class MenuGroup {
	/**
	 * Maps the menu items into some kind of menu group widget
	 */
	private final Function<String, MenuItem> addItem;

	/**
	 * Function to add a separator into this MenuGroup if possible;
	 */
	private final Consumer<Object> addSeparator;

	/**
	 * The items in this MenuGroup
	 */
	private final Map<String, MenuItem> items = new LinkedHashMap<>();

	/**
	 * Constructs a new MenuGroup given a function to add new items
	 *
	 * @param addItem
	 *            A function that adds new items to this MenuGroup
	 */
	public MenuGroup(Function<String, MenuItem> addItem) {
		this.addItem = addItem;
		addSeparator = o -> {};
	}

	/**
	 * Constructs a new MenuGroup given a function to add new items and a function to add separators
	 *
	 * @param addItem
	 *            A function that adds new items to this MenuGroup
	 * @param addSeparator
	 *            A function that adds separators (will be called with null!)
	 */
	public MenuGroup(Function<String, MenuItem> addItem, Consumer<Object> addSeparator) {
		this.addItem = addItem;
		this.addSeparator = addSeparator;
	}

	/**
	 * Tries to add a menu item with the given name
	 *
	 * @param name
	 *            The name of the item that will be displayed
	 * @return The MenuItem that corresponds to the given name
	 */
	public MenuItem addItem(String name) {
		if (items.containsKey(name)) return items.get(name);
		final MenuItem item = addItem.apply(name);
		items.put(name, item);
		return item;
	}

	/**
	 * Adds a separator into this MenuGroup if possible
	 */
	public void addSeparator() {
		addSeparator.accept(null);
	}
}
