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
import java.util.function.Function;

/**
 * Represents a complete menu
 *
 * @author Dominik Helm
 */
public class Menu {
	/**
	 * A function adding a group to this menu
	 */
	private final Function<String, MenuGroup> addGroup;

	/**
	 * The groups in this MenuGroup
	 */
	private final Map<String, MenuGroup> groups = new LinkedHashMap<>();

	/**
	 * Constructs a menu from a MenuGroupMapper and a prototype MenuItemMapper
	 *
	 * @param addGroup
	 *            The function used to add groups to this menu
	 */
	public Menu(Function<String, MenuGroup> addGroup) {
		this.addGroup = addGroup;
	}

	/**
	 * Tries to add a menu group with the given name
	 *
	 * @param name
	 *            The name of the group that will be displayed
	 * @param group
	 *            The group to be associated with this name
	 * @return The group associated with the given name
	 */
	public MenuGroup addGroup(String name) {
		if (groups.containsKey(name)) return groups.get(name);
		final MenuGroup group = addGroup.apply(name);
		groups.put(name, group);
		return group;
	}
}
