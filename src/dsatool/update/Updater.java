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
package dsatool.update;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Updater {
	private final static String updateListPath = getAppDir() + "/update/updateList.txt";
	private static File appDir;

	private static void applyUpdate(final String zipPath) {
		try (ZipFile zipFile = new ZipFile(zipPath)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final String fileName = entry.getName();
				if (entry.isDirectory()) {
					new File(getAppDir() + "/" + fileName).mkdirs();
				} else if ("deleted.txt".equals(fileName)) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))) {
						String deletedFileName;
						while ((deletedFileName = in.readLine()) != null) {
							final File toDelete = new File(getAppDir() + "/" + deletedFileName);
							File parent = toDelete.getCanonicalFile().getParentFile();
							for (int i = 0; parent != null && i < 20; ++i) {
								if (parent.equals(appDir)) {
									toDelete.delete();
									break;
								}
								parent = parent.getParentFile();
							}
						}
					}
				} else if (!"update/Updater.jar".equals(fileName) && !"release-info.json".equals(fileName)) {
					final File destination = new File(getAppDir() + "/" + fileName);
					if (destination.exists()) {
						destination.delete();
					}
					try (ReadableByteChannel in = Channels.newChannel(zipFile.getInputStream(entry));
							FileOutputStream out = new FileOutputStream(destination)) {
						out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);
					}
				}
			}
		} catch (final IOException e) {
			logError(e);
		}
		new File(zipPath).delete();
	}

	private static File getAppDir() {
		try {
			final File directory = new File(Updater.class.getResource("../../").toURI());
			return directory.getParentFile();
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static void logError(final Exception e) {
		final File log = new File(getAppDir() + "/error.log");
		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(log)))) {
			if (e.getMessage() != null) {
				writer.write(e.getMessage());
				writer.println();
			}
			e.printStackTrace(writer);
		} catch (final IOException e1) {
			e1.printStackTrace();
		} finally {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		try {
			appDir = getAppDir().getCanonicalFile();
			Files.lines(Paths.get(updateListPath)).forEach(zipPath -> {
				applyUpdate(getAppDir() + "/update/" + zipPath);
			});
			new File(updateListPath).delete();
		} catch (final IOException e) {
			logError(e);
		}
		try {
			Runtime.getRuntime().exec(new String[] { "java", "-jar", getAppDir() + "/DSATool.jar" });
		} catch (final IOException e) {
			logError(e);
		}
		System.exit(0);
	}
}
