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
package dsatool.resources;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import dsatool.util.Util;
import jsonant.event.ArrayParseEvent;
import jsonant.event.ObjectParseEvent;
import jsonant.event.ParseListener;
import jsonant.parse.JSONParser;
import jsonant.print.JSONPrinter;
import jsonant.value.JSONArray;
import jsonant.value.JSONObject;

/**
 * A class managing textual resources for reuse. Clients can request resources to be loaded and then use them
 *
 * @author Dominik Helm
 */
public class ResourceManager {

	/**
	 * Source of the resource
	 */
	private enum Source {
		GENERAL, MOD, ZIP;
	}

	/**
	 * Maps resources to their location
	 */
	private static Map<JSONObject, String> paths = new IdentityHashMap<>();

	/**
	 * Contains all acquired resources
	 */
	private static Map<String, Tuple<JSONObject, Source>> resources = new HashMap<>();

	/**
	 * The zip file for the current group
	 */
	private static ZipFile zip;

	/**
	 * The path to the zip file
	 */
	private static File zipPath;

	/**
	 * A list of listeners for changes to specific paths
	 */
	private static Map<String, List<Runnable>> pathListeners = new HashMap<>();

	/**
	 * A Collator to sort files lexicographically according to the users locale
	 */
	private static final Collator comparator = Collator.getInstance();

	private static final List<String> priorities;

	static {
		final JSONArray prioritiesArr = Settings.getSettingArray("Allgemein", "Bücher");
		if (prioritiesArr != null) {
			priorities = new ArrayList<>();
			for (int i = 0; i < prioritiesArr.size(); ++i) {
				priorities.add(prioritiesArr.getString(i));
			}
		} else {
			priorities = null;
		}
	}

	private static final Map<JSONObject, JSONObject> discriminatingAttributes = new IdentityHashMap<>();

	private static final ParseListener discriminator = new ParseListener() {
		@Override
		public void handle(final ArrayParseEvent event) {
			if (event.getValue() instanceof JSONObject) {
				final JSONObject attribute = ((JSONObject) event.getValue()).getObjOrDefault("Bücher", null);
				if (attribute != null) {
					((JSONObject) event.getValue()).removeKey("Bücher");
					discriminatingAttributes.put((JSONObject) event.getValue(), attribute);
				}
			}
		}

		@Override
		public void handle(final ObjectParseEvent event) {
			if (priorities != null && event.getValue() instanceof JSONObject) {
				final JSONObject attribute = ((JSONObject) event.getValue()).getObjOrDefault("Bücher", null);
				if (attribute != null) {
					((JSONObject) event.getValue()).removeKey("Bücher");
					discriminatingAttributes.put((JSONObject) event.getValue(), attribute);
				}
				final int prio = attribute != null ? getPrio(attribute) : Integer.MAX_VALUE - 1;
				if (event.getObject().containsKey(event.getKey())) {
					final Object other = event.getObject().getUnsafe(event.getKey());
					if (other instanceof JSONObject) {
						final JSONObject otherAttribute = discriminatingAttributes.get(other);
						final int otherPrio = otherAttribute != null ? getPrio(otherAttribute) : Integer.MAX_VALUE - 1;
						if (otherPrio < prio) {
							event.cancel();
						}
					}
				} else if (prio == Integer.MAX_VALUE) {
					event.cancel();
				}
			}
		}
	};

	/**
	 * Requests a resource to be loaded for future use
	 *
	 * @param path
	 *            The path to the resource
	 * @param discriminate
	 *            True, if the discriminating attribute is to be evaluated, false otherwise
	 * @param notifyPathListeners
	 *            True if path listeners should be notified, false otherwise
	 * @return True, if the resource was loaded, false if it was newly created
	 */
	private static boolean acquireResource(final String path, final boolean discriminate, final boolean notifyPathListeners) {
		if (resources.containsKey(path)) return true;
		final JSONParser parser = new JSONParser(discriminate ? discriminator : null, e -> ErrorLogger.logError(e));
		final String jsonpath = path + ".json";
		Source bestMatch = Source.ZIP;
		if (zip != null) {
			final ZipEntry entry = zip.getEntry(jsonpath);
			if (entry != null) {
				try (final BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), "UTF-8"))) {
					final JSONObject result = parser.parse(reader);
					resources.put(path, new Tuple<>(result, Source.ZIP));
					paths.put(result, path);
					if (notifyPathListeners) {
						notifyPathListeners(path);
					}
					return true;
				} catch (final IOException e) {
					ErrorLogger.logError(e);
				}
			}
		}
		File file = new File(Util.getAppDir() + "/mod/" + jsonpath);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
				final JSONObject result = parser.parse(reader);
				resources.put(path, new Tuple<>(result, Source.MOD));
				paths.put(result, path);
				if (notifyPathListeners) {
					notifyPathListeners(path);
				}
				return true;
			} catch (final IOException e) {
				ErrorLogger.logError(e);
				bestMatch = Source.MOD;
			}
		}
		file = new File(Util.getAppDir() + File.separator + jsonpath);
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
				final JSONObject result = parser.parse(reader);
				resources.put(path, new Tuple<>(result, Source.GENERAL));
				paths.put(result, path);
				if (notifyPathListeners) {
					notifyPathListeners(path);
				}
				return true;
			} catch (final IOException e) {
				ErrorLogger.logError(e);
				bestMatch = Source.GENERAL;
			}
		}
		final JSONObject result = new JSONObject(null);
		resources.put(path, new Tuple<>(result, bestMatch));
		paths.put(result, path);
		if (notifyPathListeners) {
			notifyPathListeners(path);
		}
		return false;
	}

	/**
	 * Registers a listener that will be called if a file in the specified path is added, moved or removed
	 *
	 * @param path
	 *            The path that will be monitored
	 * @param listener
	 *            The listener that is executed on changes
	 */
	public static void addPathListener(final String path, final Runnable listener) {
		if (!pathListeners.containsKey(path)) {
			pathListeners.put(path, new ArrayList<>());
		}
		pathListeners.get(path).add(listener);
	}

	/**
	 * Deletes the resource the given JSONObject was created from so it will not be loaded again
	 *
	 * @param resource
	 *            The JSONObject that represents the resource to be deleted
	 * @return true, if the resource was deleted, false otherwise
	 */
	public static boolean deleteResource(final JSONObject resource) {
		return deleteResource(resource, true);
	}

	/**
	 * Deletes the resource the given JSONObject was created from so it will not be loaded again
	 *
	 * @param resource
	 *            The JSONObject that represents the resource to be deleted
	 * @param notifyPathListeners
	 *            True if path listeners should be notified, false otherwise
	 * @return true, if the resource was deleted, false otherwise
	 */
	private static boolean deleteResource(final JSONObject resource, final boolean notifyPathListeners) {
		if (paths.containsKey(resource)) {
			String path = paths.remove(resource);
			final Source source = resources.remove(path)._2;
			path += ".json";
			if (source != Source.ZIP) {
				if (notifyPathListeners) {
					notifyPathListeners(path);
				}
				return false;
			}
			if (zip != null) {
				final ZipEntry entry = zip.getEntry(path);
				if (entry != null) {
					try {
						zip.close();
					} catch (final IOException e) {
						ErrorLogger.logError(e);
						return false;
					}
					try (final FileSystem zipFile = FileSystems.newFileSystem(zipPath.toPath(), null)) {
						Files.delete(zipFile.getPath("/" + path));
						zipFile.close();
						zip = new ZipFile(zipPath);
						if (notifyPathListeners) {
							notifyPathListeners(path);
						}
						return true;
					} catch (final IOException e) {
						ErrorLogger.logError(e);
					}
				} else {
					if (notifyPathListeners) {
						notifyPathListeners(path);
					}
				}
			}
		}
		return false;
	}

	/**
	 * Discards all unsaved changes
	 */
	public static void discardChanges() {
		resources = new HashMap<>();
		paths = new HashMap<>();
		for (final List<Runnable> listeners : pathListeners.values()) {
			for (final Runnable listener : listeners) {
				listener.run();
			}
		}
	}

	/**
	 * Discards all open resources
	 */
	public static void discardResources() {
		if (zip != null) {
			saveResources();
			discardChanges();
		}
	}

	/**
	 * Gets all resources in a given directory
	 *
	 * @param path
	 *            The path to the resources
	 * @return A list of all resources in that directory
	 */
	public static List<JSONObject> getAllResources(final String path) {
		final Set<JSONObject> result = new TreeSet<>((hero1, hero2) -> {
			final String name1 = hero1.getObj("Biografie").getString("Vorname");
			final String name2 = hero2.getObj("Biografie").getString("Vorname");
			if (name1 == null) {
				if (name2 == null) return 0;
				return 1;
			}
			if (name2 == null) return -1;
			return comparator.compare(name1, name2);
		});
		for (final String resourcePath : resources.keySet()) {
			if (resourcePath.startsWith(path)) {
				result.add(resources.get(resourcePath)._1);
			}
		}
		if (zip != null) {
			final ZipEntry entry = zip.getEntry(path);
			if (entry != null && entry.isDirectory()) {
				final Enumeration<? extends ZipEntry> entries = zip.entries();
				while (entries.hasMoreElements()) {
					final ZipEntry current = entries.nextElement();
					if (!current.isDirectory()) {
						final Path currentPath = Paths.get(current.getName());
						if (currentPath.startsWith(path)) {
							result.add(getResource(current.getName().substring(0, current.getName().lastIndexOf('.'))));
						}
					}
				}
			}
		}
		return new ArrayList<>(result);
	}

	public static JSONObject getDiscrimination(final JSONObject data) {
		return discriminatingAttributes.get(data);
	}

	/**
	 * Returns a definitely new resource that has a path similar to the requested one
	 *
	 * @param path
	 *            The path the new resource should be created at if possible
	 * @return A new resource
	 */
	public static JSONObject getNewResource(final String path) {
		return getNewResource(path, true);
	}

	/**
	 * Returns a definitely new resource that has a path similar to the requested one
	 *
	 * @param path
	 *            The path the new resource should be created at if possible
	 * @param notifyPathListeners
	 *            True if path listeners should be notified, false otherwise
	 * @return A new resource
	 */
	private static JSONObject getNewResource(String path, final boolean notifyPathListeners) {
		boolean foundFreeName = false;
		while (!foundFreeName) {
			while (resources.containsKey(path)) {
				path = path + '_';
			}
			foundFreeName = !acquireResource(path, true, notifyPathListeners);
		}
		return resources.get(path)._1;
	}

	private static int getPrio(final JSONObject value) {
		int prio = 0;
		for (final String entry : priorities) {
			if (value.containsKey(entry)) return prio;
			++prio;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * Gets a resource (will be acquired if necessary)
	 *
	 * @param path
	 *            The path to the resource
	 * @return The resource
	 */
	public static JSONObject getResource(final String path) {
		return getResource(path, true);
	}

	/**
	 * Gets a resource (will be acquired if necessary)
	 *
	 * @param path
	 *            The path to the resource
	 * @param discriminate
	 *            True, if the discriminating attribute is to be evaluated, false otherwise
	 * @return The resource
	 */
	static JSONObject getResource(final String path, final boolean discriminate) {
		if (!resources.containsKey(path)) {
			acquireResource(path, discriminate, true);
		}
		return resources.get(path)._1;
	}

	/**
	 * Loads an external file and stores it's content to a resource
	 *
	 * @param file
	 *            The external file to load
	 * @param internalPath
	 *            The path the resource will be located at
	 * @return The JSONObject that contains the file's content
	 */
	public static JSONObject loadExternalResource(final File file, final String internalPath) {
		if (file.exists()) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
				final JSONObject tmp = getNewResource(makeValidFile(internalPath), false);
				final String tmpPath = paths.get(tmp);
				final JSONArray prioritiesArr = Settings.getSettingArray("Allgemein", "Bücher");
				final List<String> priorities = new ArrayList<>();
				if (prioritiesArr != null) {
					for (int i = 0; i < prioritiesArr.size(); ++i) {
						priorities.add(prioritiesArr.getString(i));
					}
				}
				final JSONObject result = new JSONParser(discriminator, e -> ErrorLogger.logError(e)).parse(reader);
				resources.put(tmpPath, new Tuple<>(result, Source.ZIP));
				paths.put(result, tmpPath);
				paths.remove(tmp);
				notifyPathListeners(tmpPath);
				return result;
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
		}
		return null;
	}

	/**
	 * Replaces bad characters in the file name part of a path
	 *
	 * @param name
	 *            The name of the file that is requested
	 * @return A cleaned up version of the name
	 */
	private static String makeValidFile(final String name) {
		final int pathEnd = name.lastIndexOf('/') + 1;
		final String path = name.substring(0, pathEnd);
		final String fileName = name.substring(pathEnd);
		return path + fileName.replaceAll("[^\\w-]", "_");
	}

	/**
	 * Moves a resource to a file with a name similar to the given one
	 *
	 * @param resource
	 *            The resource to be moved
	 * @param newName
	 *            The name of the file that is requested
	 */
	public static void moveResource(final JSONObject resource, final String newName) {
		if (newName.equals(paths.get(resource))) return;
		deleteResource(resource, false);
		final JSONObject tmp = getNewResource(makeValidFile(newName), false);
		String path = paths.get(tmp);
		resources.put(path, new Tuple<>(resource, Source.ZIP));
		paths.put(resource, path);
		paths.remove(tmp);
		notifyPathListeners(path);
		if (zip != null) {
			try {
				zip.close();
			} catch (final IOException e) {
				ErrorLogger.logError(e);
				return;
			}
		}
		try (final FileSystem zipFile = FileSystems.newFileSystem(zipPath.toPath(), null)) {
			path += ".json";
			final Path p = zipFile.getPath("/" + path);
			if (p.getParent() != null) {
				Files.createDirectories(p.getParent());
			}
			try (final BufferedWriter zipwriter = Files.newBufferedWriter(p, StandardOpenOption.CREATE)) {
				JSONPrinter.print(zipwriter, resource);
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
			zipFile.close();
			zip = new ZipFile(zipPath);
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
	}

	/**
	 * Notifies all listeners that are registered for any prefix of a path
	 *
	 * @param path
	 *            The path that was changed
	 */
	private static void notifyPathListeners(final String path) {
		for (final String listenerPath : pathListeners.keySet()) {
			if (path.startsWith(listenerPath)) {
				for (final Runnable listener : pathListeners.get(listenerPath)) {
					listener.run();
				}
			}
		}
	}

	/**
	 * Removes a listener for a specific path, so it will not be called anymore if a file in that path is added, moved or removed
	 *
	 * @param path
	 *            The path that was previously monitored
	 * @param listener
	 *            The listener that is to be removed
	 */
	public static void removePathListener(final String path, final Runnable listener) {
		final List<Runnable> listeners = pathListeners.get(path);
		if (listeners != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * Saves a JSONObject as a json file
	 *
	 * @param resource
	 *            The JSONObject to be saved
	 * @param path
	 *            The path the json file is to be created at
	 */
	public static void saveResource(final JSONObject resource, final String path) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
			JSONPrinter.print(writer, resource);
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
	}

	/**
	 * Saves all changed resources
	 */
	public static void saveResources() {
		if (zip != null) {
			try {
				zip.close();
			} catch (final IOException e) {
				ErrorLogger.logError(e);
				return;
			}
		}
		try (final FileSystem zipFile = FileSystems.newFileSystem(zipPath.toPath(), null)) {
			for (final Entry<String, Tuple<JSONObject, Source>> entry : resources.entrySet()) {
				String path = entry.getKey() + ".json";
				switch (entry.getValue()._2) {
				case MOD:
					if (!path.startsWith("settings")) {
						break;
					}
					path = "mod/" + path;
				case GENERAL:
					if (!path.startsWith("settings")) {
						break;
					}
					try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(Util.getAppDir(), path))) {
						JSONPrinter.print(writer, entry.getValue()._1);
					} catch (final IOException e) {
						ErrorLogger.logError(e);
					}
					break;
				case ZIP:
					final Path p = zipFile.getPath("/" + path);
					if (p.getParent() != null) {
						Files.createDirectories(p.getParent());
					}
					try (final BufferedWriter zipwriter = Files.newBufferedWriter(p, StandardOpenOption.CREATE)) {
						JSONPrinter.print(zipwriter, entry.getValue()._1);
					} catch (final IOException e) {
						ErrorLogger.logError(e);
					}
					break;
				default:
					break;
				}
			}
			zipFile.close();
			zip = new ZipFile(zipPath);
		} catch (final ZipException e) {
			// This can happen if the zip is empty, just ignore it
			zip = null;
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
	}

	/**
	 * Sets the zip file containing the data for the current group
	 *
	 * @param path
	 *            The path the zip file resides at
	 * @throws IOException
	 *             If there is no such zip file
	 */
	public static void setZipFile(final File path) {
		zipPath = path;
		try {
			zip = new ZipFile(path);
		} catch (final ZipException e) {
			// This can happen if the zip is empty, just ignore it
			zip = null;
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
		try (final BufferedWriter writer = new BufferedWriter(new FileWriter(Util.getAppDir() + "/settings/Gruppe.txt"))) {
			writer.write(path.getAbsolutePath());
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
		final File directory = path.getParentFile();
		for (final File current : directory.listFiles()) {
			if (current.getName().startsWith("zipfstmp")) {
				current.delete();
			}
		}
	}
}
