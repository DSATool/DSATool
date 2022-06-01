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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
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
	private volatile static ZipFile zip;

	private volatile static ReentrantReadWriteLock zipLock = new ReentrantReadWriteLock();

	/**
	 * The path to the zip file
	 */
	private static File zipPath;

	private static String discriminatingAttribute;

	/**
	 * A list of listeners for changes to specific paths
	 */
	private static Map<String, List<Consumer<Boolean>>> pathListeners = new HashMap<>();

	private static Collection<String> priorities = new ArrayList<>();

	private static final List<Function<JSONObject, JSONObject>> resourceSanitizers = new ArrayList<>();

	private static final Map<JSONObject, JSONObject> discriminatingAttributes = new IdentityHashMap<>();

	private static final ParseListener discriminator = new ParseListener() {
		@Override
		public void handle(final ArrayParseEvent event) {
			if (event.getValue() instanceof JSONObject) {
				final JSONObject attribute = ((JSONObject) event.getValue()).getObjOrDefault(discriminatingAttribute, null);
				if (attribute != null) {
					((JSONObject) event.getValue()).removeKey(discriminatingAttribute);
					discriminatingAttributes.put((JSONObject) event.getValue(), attribute);
				}
			}
		}

		@Override
		public void handle(final ObjectParseEvent event) {
			if (event.getValue() instanceof JSONObject) {
				final JSONObject attribute = ((JSONObject) event.getValue()).getObjOrDefault(discriminatingAttribute, null);
				if (attribute != null) {
					((JSONObject) event.getValue()).removeKey(discriminatingAttribute);
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
		final JSONParser parser = new JSONParser(discriminate ? discriminator : null, ErrorLogger::logError);
		final String jsonpath = path + ".json";
		Source source = null;
		JSONObject result = new JSONObject(null);
		File file = new File(Util.getAppDir() + File.separator + jsonpath);
		if (file.exists()) {
			source = Source.GENERAL;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
				result = parser.parse(reader);
			} catch (final IOException e) {
				ErrorLogger.logError(e);
				return false;
			}
		}
		file = new File(Util.getAppDir() + "/mod/" + jsonpath);
		if (file.exists()) {
			source = Source.MOD;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
				final JSONObject mod = parser.parse(reader);
				modifyResource(result, mod);
			} catch (final IOException e) {
				ErrorLogger.logError(e);
				return false;
			}
		}
		if (zip != null) {
			zipLock.readLock().lock();
			try {
				final ZipEntry entry = zip.getEntry(jsonpath);
				if (entry != null) {
					source = Source.ZIP;
					try (final BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), "UTF-8"))) {
						final JSONObject mod = parser.parse(reader);
						modifyResource(result, mod);
					} catch (final IOException e) {
						ErrorLogger.logError(e);
						return false;
					}
				}
			} finally {
				zipLock.readLock().unlock();
			}
		}

		for (final Function<JSONObject, JSONObject> sanitizer : resourceSanitizers) {
			result = sanitizer.apply(result);
		}

		resources.put(path, new Tuple<>(result, source == null ? Source.ZIP : source));
		paths.put(result, path);
		if (notifyPathListeners) {
			notifyPathListeners(path);
		}
		return source != null;
	}

	/**
	 * Registers a listener that will be called if a file in the specified path is added, moved or removed
	 *
	 * @param path
	 *            The path that will be monitored
	 * @param listener
	 *            The listener that is executed on changes
	 */
	public static void addPathListener(final String path, final Consumer<Boolean> listener) {
		if (!pathListeners.containsKey(path)) {
			pathListeners.put(path, new ArrayList<>());
		}
		pathListeners.get(path).add(listener);
	}

	public static void addResourceSanitizer(final Function<JSONObject, JSONObject> sanitizer) {
		resourceSanitizers.add(sanitizer);
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
				zipLock.writeLock().lock();
				try {
					final ZipEntry entry = zip.getEntry(path);
					if (entry != null) {
						try {
							zip.close();
						} catch (final IOException e) {
							ErrorLogger.logError(e);
							return false;
						}
						try (final FileSystem zipFile = FileSystems.newFileSystem(zipPath.toPath())) {
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
				} finally {
					zipLock.writeLock().unlock();
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
		for (final List<Consumer<Boolean>> listeners : pathListeners.values()) {
			for (final Consumer<Boolean> listener : listeners) {
				listener.accept(true);
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
		final Map<String, JSONObject> result = new TreeMap<>();
		for (final String resourcePath : resources.keySet()) {
			if (resourcePath.startsWith(path)) {
				result.put(resourcePath.substring(resourcePath.lastIndexOf("/") + 1) + ".json", resources.get(resourcePath)._1);
			}
		}
		if (zip != null) {
			zipLock.readLock().lock();
			try {
				final ZipEntry entry = zip.getEntry(path);
				if (entry != null && entry.isDirectory()) {
					final Enumeration<? extends ZipEntry> entries = zip.entries();
					while (entries.hasMoreElements()) {
						final ZipEntry current = entries.nextElement();
						if (!current.isDirectory()) {
							final Path currentPath = Paths.get(current.getName());
							if (currentPath.startsWith(path)) {
								result.put(currentPath.getFileName().toString(),
										getResource(current.getName().substring(0, current.getName().lastIndexOf('.'))));
							}
						}
					}
				}
			} finally {
				zipLock.readLock().unlock();
			}
		}
		return new ArrayList<>(result.values());
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
				final JSONObject result = new JSONParser(discriminator, ErrorLogger::logError).parse(reader);
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
	 * Modifies a resource according to a JSONObject containing overrides and deletions
	 *
	 * @param resource
	 *            The resource modified
	 * @param modification
	 *            The modifications to apply
	 */
	private static void modifyResource(final JSONObject resource, final JSONObject modification) {
		for (final String key : modification.keySet()) {
			final Object value = modification.getUnsafe(key);
			if (value instanceof JSONObject) {
				if (resource.containsKey(key)) {
					modifyResource(resource.getObj(key), (JSONObject) value);
				} else {
					resource.put(key, ((JSONObject) value).clone(resource));
				}
			} else if (value instanceof JSONArray) {
				resource.put(key, ((JSONArray) value).clone(resource));
			} else if (value instanceof Double) {
				resource.put(key, (Double) value);
			} else if (value instanceof Long) {
				resource.put(key, (Long) value);
			} else if (value instanceof Boolean) {
				resource.put(key, (Boolean) value);
			} else if (value instanceof String) {
				resource.put(key, (String) value);
			} else {
				resource.removeKey(key);
			}
		}
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
		zipLock.writeLock().lock();
		try {
			if (zip != null) {
				try {
					zip.close();
				} catch (final IOException e) {
					ErrorLogger.logError(e);
					return;
				}
			}
			try (final FileSystem zipFile = FileSystems.newFileSystem(zipPath.toPath())) {
				path += ".json";
				final Path p = zipFile.getPath("/" + path);
				final Path parent = p.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				try (final BufferedWriter zipwriter = Files.newBufferedWriter(p, StandardOpenOption.CREATE)) {
					JSONPrinter.print(zipwriter, resource);
				} catch (final IOException e) {
					ErrorLogger.logError(e);
				}
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
			try {
				zip = new ZipFile(zipPath);
			} catch (final ZipException e) {
				// This can happen if the zip is empty, just ignore it
				zip = null;
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
		} finally {
			zipLock.writeLock().unlock();
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
				for (final Consumer<Boolean> listener : pathListeners.get(listenerPath)) {
					listener.accept(false);
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
	public static void removePathListener(final String path, final Consumer<Boolean> listener) {
		final List<Consumer<Boolean>> listeners = pathListeners.get(path);
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
	public static void saveResource(JSONObject resource, final String path) {
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
			for (final Function<JSONObject, JSONObject> sanitizer : resourceSanitizers) {
				resource = sanitizer.apply(resource);
			}
			JSONPrinter.print(writer, resource);
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
	}

	/**
	 * Saves all changed resources
	 */
	public static void saveResources() {
		zipLock.writeLock().lock();
		try {
			if (zip != null) {
				try {
					zip.close();
				} catch (final IOException e) {
					ErrorLogger.logError(e);
					return;
				}
			}
			try (final FileSystem zipFile = FileSystems.newFileSystem(zipPath.toPath())) {
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
							final Path parent = p.getParent();
							if (parent != null) {
								Files.createDirectories(parent);
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
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
			try {
				zip = new ZipFile(zipPath);
			} catch (final ZipException e) {
				// This can happen if the zip is empty, just ignore it
				zip = null;
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
		} finally {
			zipLock.writeLock().unlock();
		}
	}

	public static void setDiscriminatingAttribute(final String key) {
		discriminatingAttribute = key;
	}

	public static void setPriorities(final Collection<String> priorities) {
		ResourceManager.priorities = priorities;
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
		zipLock.writeLock().lock();
		try {
			zipPath = path;
			try {
				zip = new ZipFile(path);
				discardChanges();
			} catch (final ZipException e) {
				// This can happen if the zip is empty, just ignore it
				zip = null;
			} catch (final IOException e) {
				ErrorLogger.logError(e);
			}
			for (final List<Consumer<Boolean>> listeners : pathListeners.values()) {
				for (final Consumer<Boolean> listener : listeners) {
					listener.accept(false);
				}
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
		} finally {
			zipLock.writeLock().unlock();
		}
	}

	private ResourceManager() {}
}
