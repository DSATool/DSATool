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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import dsatool.util.ErrorLogger;
import dsatool.util.Util;

/**
 * This class loads and initializes all plugins
 *
 * @author Dominik Helm
 */
public class PluginLoader {

	/**
	 * A map of plugin names to the corresponding plugin class, so plugins can find each other
	 */
	public static final Map<String, Plugin> plugins = new HashMap<>();

	/**
	 * The directory where plugins reside
	 */
	private static final String pluginDir = "plugins";

	/**
	 * Loads all classes from all plugin jars, identifies the plugins and initializes them
	 */
	public static void loadPlugins() {
		final File pluginDirectory = new File(Util.getAppDir() + File.separator + pluginDir);

		final File[] pluginJars = pluginDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

		if (pluginJars == null) {
			ErrorLogger.log("No plugins found in dir " + pluginDirectory);
			return;
		}

		final URL[] jarURLs = new URL[pluginJars.length];

		for (int i = 0; i < pluginJars.length; ++i) {
			try {
				jarURLs[i] = pluginJars[i].toURI().toURL();
			} catch (final MalformedURLException e) {
				ErrorLogger.logError(e);
			}
		}

		final ServiceLoader<Plugin> loader = ServiceLoader.load(Plugin.class, new URLClassLoader(jarURLs));
		for (final Plugin plugin : loader) {
			plugins.put(plugin.getPluginName(), plugin);
		}
		loader.reload(); // Clear the ServiceLoader's cache, as we use our own

		for (final Plugin plugin : plugins.values()) {
			plugin.initialize();
		}
	}

	private PluginLoader() {}
}
