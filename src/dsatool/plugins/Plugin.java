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
package dsatool.plugins;

/**
 * Interface to be implemented by plugins
 *
 * @author Dominik Helm
 */
public abstract class Plugin {
	/**
	 * Does the plugin want to get notified that resources should be reloaded?
	 */
	protected boolean getNotifications = false;

	/**
	 * Returns a string to uniquely identify a plugin
	 *
	 * @return A string to uniquely identify a plugin
	 */
	public abstract String getPluginName();

	/**
	 * Initializes the plugin. This should register all needed callbacks and, if needed, GUI components to access the
	 * plugins functionality
	 */
	public abstract void initialize();

	/**
	 * Notifies the plugin that all resources have been discarded and should be reloaded if necessary
	 */
	protected abstract void load();

	/**
	 * Notifies the plugin that all resources have been discarded and should be reloaded if necessary.
	 * Will only result in a notification if isLoaded was set to true by the plugin.
	 */
	public void loadData() {
		if (getNotifications) {
			load();
		}
	}
}
