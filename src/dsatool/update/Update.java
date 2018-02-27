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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dsatool.util.ErrorLogger;
import dsatool.util.Tuple;
import dsatool.util.Util;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import jsonant.parse.JSONParser;
import jsonant.value.JSONObject;

public class Update {
	public final static String updateListPath = Util.getAppDir() + "/update/updateList.txt";

	public static void execute() {
		try {
			Files.lines(Paths.get(updateListPath)).forEach(line -> {
				final String zipPath = line.split(";", 2)[0];
				try (ZipFile zipFile = new ZipFile(Util.getAppDir() + "/update/" + zipPath)) {
					final Enumeration<? extends ZipEntry> entries = zipFile.entries();
					while (entries.hasMoreElements()) {
						final ZipEntry entry = entries.nextElement();
						if ("update/Updater.jar".equals(entry.getName())) {
							final File destination = new File(Util.getAppDir() + "/update/Updater.jar");
							try {
								Files.deleteIfExists(destination.toPath());
							} catch (final IOException e) {
								ErrorLogger.logError(e);
							}
							try (ReadableByteChannel in = Channels.newChannel(zipFile.getInputStream(entry));
									FileOutputStream out = new FileOutputStream(destination)) {
								out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);
							}
						}
					}
				} catch (final IOException e) {
					ErrorLogger.logError(e);
				}
			});
			Runtime.getRuntime().exec(new String[] { "java", "-jar", Util.getAppDir() + "/update/Updater.jar" });
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
		System.exit(0);
	}

	private final JSONParser parser = new JSONParser(null, e -> ErrorLogger.logError(e));

	private String getSignatureKeySpecification(final JSONObject info) {
		if (info.containsKey("signatureProviderName") && info.containsKey("signatureKeyAlgorithm") && info.containsKey("signatureAlgorithm")
				&& info.containsKey("signatureKey"))
			return ";" + info.getString("signatureProviderName") + ";" + info.getString("signatureKeyAlgorithm") + ";"
					+ info.getString("signatureAlgorithm") + ";" + info.getString("signatureKey");
		else
			return "";
	}

	private String performUpdate(final JSONObject oldReleaseInfo, final String link, final List<Tuple<String, String>> files) {
		final File dest = new File(Util.getAppDir() + "/update/" + link.substring(link.lastIndexOf('/')));
		try (final ReadableByteChannel in = Channels.newChannel(new URL(link).openStream()); FileOutputStream out = new FileOutputStream(dest)) {
			out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);
		} catch (final IOException e) {
			ErrorLogger.logError(e);
			return null;
		}
		final String previousTime = oldReleaseInfo.getString("releaseDate");
		try (ZipFile zipFile = new ZipFile(dest)) {
			final ZipEntry releaseInfo = zipFile.getEntry("release-info.json");
			if (releaseInfo == null) return null;
			final JSONObject info = parser.parse(new BufferedReader(new InputStreamReader(zipFile.getInputStream(releaseInfo))));
			String signatureKeySpecification;
			if (info.containsKey("previousReleaseDate") && info.getString("previousReleaseDate").compareTo(previousTime) > 0) {
				final String oldKeySpec = performUpdate(oldReleaseInfo, info.getString("previousReleaseLink"), files);
				if (oldKeySpec != null) {
					signatureKeySpecification = oldKeySpec;
				} else
					return null;
			} else {
				signatureKeySpecification = getSignatureKeySpecification(oldReleaseInfo);
			}
			if (info.containsKey("releaseDate") && info.getString("releaseDate").compareTo(previousTime) > 0) {
				files.add(new Tuple<>(info.getString("releaseDate"), dest.getName() + signatureKeySpecification));
				return getSignatureKeySpecification(info);
			}
		} catch (final IOException e) {
			ErrorLogger.logError(e);
		}
		return null;
	}

	private void performUpdates(final List<Tuple<JSONObject, String>> updates) {
		final List<Tuple<String, String>> files = new ArrayList<>();
		for (final Tuple<JSONObject, String> update : updates) {
			performUpdate(update._1, update._2, files);
		}

		files.sort((t1, t2) -> t1._1.compareTo(t2._1));
		final List<String> fileNames = new ArrayList<>(files.size());
		for (final Tuple<String, String> file : files) {
			fileNames.add(file._2);
		}

		try {
			Files.write(new File(updateListPath).toPath(), fileNames, StandardOpenOption.CREATE);
		} catch (final IOException e) {
			ErrorLogger.logError(e);
			return;
		}

		Platform.runLater(() -> {
			final Alert restart = new Alert(AlertType.INFORMATION);
			restart.setTitle("Aktualisierungen heruntergeladen");
			restart.setHeaderText("Aktualisierungen wurden heruntergeladen und werden mit dem nächsten Neustart installiert");
			restart.setContentText("Soll DSATool jetzt neu gestartet werden?");
			restart.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
			final Optional<ButtonType> result = restart.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				execute();
			}
		});
	}

	public Tuple<JSONObject, String> searchUpdate(final File releaseInfoFile) {
		JSONObject releaseInfo = null;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(releaseInfoFile), "UTF-8"))) {
			releaseInfo = parser.parse(reader);
		} catch (final IOException e) {
			ErrorLogger.logError(e);
			return null;
		}

		final String link = releaseInfo.getString("updateInfo");
		if (link == null || !releaseInfo.containsKey("releaseDate")) return null;

		JSONObject updateInfo = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(link).openStream()))) {
			updateInfo = parser.parse(reader);
		} catch (final Exception e) {
			return null;
		}

		if (updateInfo.containsKey("releaseDate") && releaseInfo.getString("releaseDate").compareTo(updateInfo.getString("releaseDate")) < 0)
			return new Tuple<>(releaseInfo, updateInfo.getString("releaseLink"));
		return null;
	}

	public void searchUpdates(final boolean notifyOnNoUpdate) {
		final List<Tuple<JSONObject, String>> updates = new LinkedList<>();
		for (final File releaseInfo : new File(Util.getAppDir() + "/update").listFiles((dir, name) -> name.toLowerCase().endsWith(".json"))) {
			final Tuple<JSONObject, String> update = searchUpdate(releaseInfo);
			if (update != null) {
				updates.add(update);
			}
		}
		if (updates.isEmpty()) {
			if (notifyOnNoUpdate) {
				Platform.runLater(() -> {
					final Alert noUpdate = new Alert(AlertType.INFORMATION);
					noUpdate.setTitle("Keine Aktualisierungen verfügbar");
					noUpdate.setHeaderText("Es sind keine Aktualisierungen verfügbar");
					noUpdate.setContentText("DSATool ist bereits auf dem neuesten Stand");
					noUpdate.getButtonTypes().setAll(ButtonType.OK);
					noUpdate.showAndWait();
				});
			}
			return;
		}

		Platform.runLater(() -> {
			final Alert performUpdate = new Alert(AlertType.INFORMATION);
			performUpdate.setTitle("Aktualisierungen verfügbar");
			performUpdate.setHeaderText("Aktualisierungen sind verfügbar");
			performUpdate.setContentText("Soll jetzt aktualisiert werden?");
			performUpdate.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
			final Optional<ButtonType> result = performUpdate.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.YES)) {
				new Thread(() -> performUpdates(updates)).start();
			}
		});
	}
}
