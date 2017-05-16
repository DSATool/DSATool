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
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
	 * The directory where dependencies reside
	 */
	private static final String dependencyDir = "dependencies";

	private static URLClassLoader loader;

	/**
	 * The directory where plugins reside
	 */
	private static final String pluginDir = "plugins";

	private static void addJarsToClassPath(File file) {
		try {
			final Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			addUrlMethod.setAccessible(true);
			addUrlMethod.invoke(Thread.currentThread().getContextClassLoader(), file.toURI().toURL());
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}
	}

	private static void loadDependencies() {
		final File dependencyDirectory = new File(Util.getAppDir() + File.separator + dependencyDir);
		final File[] dependencies = dependencyDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
		for (final File file : dependencies) {
			addJarsToClassPath(file);
		}
	}

	/**
	 * Loads all classes from all plugin jars, identifies the plugins and initializes them
	 */
	public static void loadPlugins() {
		loadDependencies();
		final File pluginDirectory = new File(Util.getAppDir() + File.separator + pluginDir);

		final File[] pluginJars = pluginDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

		final URL[] jarURLs = new URL[pluginJars.length];

		for (int i = 0; i < pluginJars.length; ++i) {
			try {
				jarURLs[i] = pluginJars[i].toURI().toURL();
			} catch (final MalformedURLException e) {
				ErrorLogger.logError(e);
			}
		}

		loader = new URLClassLoader(jarURLs);

		for (final File pluginJar : pluginJars) {
			try (JarFile jarFile = new JarFile(pluginJar)) {
				final Enumeration<JarEntry> entries = jarFile.entries();
				String pluginName = pluginJar.getName();
				pluginName = pluginName.substring(0, pluginName.lastIndexOf('.'));
				try {
					while (entries.hasMoreElements()) {
						final JarEntry je = entries.nextElement();
						if (je.isDirectory() || !je.getName().endsWith(".class")) {
							continue;
						}
						// -6 because of .class
						String className = je.getName().substring(0, je.getName().length() - 6);
						className = className.replace('/', '.');
						final Class<?> c = loader.loadClass(className);
						if (className.equals(pluginName)) {
							final Plugin plugin = (Plugin) c.newInstance();
							plugins.put(plugin.getPluginName(), plugin);
						}
					}
				} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
					ErrorLogger.logError(e);
				}
				jarFile.close();
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
		}

		for (final Plugin plugin : plugins.values()) {
			plugin.initialize();
		}
	}
}
