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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Updater {
	private final static String updateListPath = getAppDir() + "/update/updateList.txt";
	private static File appDir;

	private static void applyUpdate(final String zipName, final String signatureProviderName, final String keyAlgorithm, final String signatureAlgorithm,
			final String signatureKeyString) {
		final String zipPath = getAppDir() + "/update/" + zipName;

		try (ZipFile zipFile = new ZipFile(zipPath)) {
			if (signatureProviderName != null && keyAlgorithm != null && signatureAlgorithm != null && signatureKeyString != null) {
				final ZipEntry signatureFile = zipFile.getEntry("signature.sig");
				if (signatureFile == null) {
					log("Update " + zipName + " ist nicht signiert, Update nicht durchgeführt");
					throw new RuntimeException();
				}

				try {
					final Provider signatureProvider = Security.getProvider(signatureProviderName);

					// Prepare the public key
					final byte[] signatureKey = Base64.getDecoder().decode(signatureKeyString);
					final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(signatureKey);
					final KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm, signatureProvider);
					final PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

					// Prepare the signature object
					final Signature sig = Signature.getInstance(signatureAlgorithm, signatureProvider);
					sig.initVerify(publicKey);

					// Input the data
					final byte[] buffer = new byte[1024];
					final Enumeration<? extends ZipEntry> entries = zipFile.entries();
					while (entries.hasMoreElements()) {
						final ZipEntry entry = entries.nextElement();
						final String fileName = entry.getName();
						if (!"signature.sig".equals(fileName)) {
							sig.update(fileName.getBytes());
							if (!entry.isDirectory()) {
								try (InputStream inputStream = zipFile.getInputStream(entry)) {
									int len;
									while ((len = inputStream.read(buffer)) > 0) {
										sig.update(buffer, 0, len);
									}
								}
							}
						}
					}

					final byte[] signature = new byte[(int) signatureFile.getSize()];
					try (DataInputStream signatureStream = new DataInputStream(zipFile.getInputStream(signatureFile))) {
						// Read the signature
						signatureStream.readFully(signature);

						// Verify the signature
						if (!sig.verify(signature)) {
							log("Update " + zipName + " nicht korrekt signiert, Update nicht durchgeführt");
							throw new RuntimeException();
						}
					}
				} catch (final Exception e) {
					log("Fehler bei der Verifikation von " + zipName + ", Update nicht durchgeführt");
					throw e;
				}
			}
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
									try {
										Files.deleteIfExists(toDelete.toPath());
									} catch (final IOException e) {
										logError(e);
									}
									break;
								}
								parent = parent.getParentFile();
							}
						}
					}
				} else if (!"update/Updater.jar".equals(fileName) && !"release-info.json".equals(fileName) && !"signature.sig".equals(fileName)) {
					final File destination = new File(getAppDir() + "/" + fileName);
					Files.deleteIfExists(destination.toPath());
					try (ReadableByteChannel in = Channels.newChannel(zipFile.getInputStream(entry));
							FileOutputStream out = new FileOutputStream(destination)) {
						out.getChannel().transferFrom(in, 0, Long.MAX_VALUE);
					}
				}
			}
		} catch (final Exception e) {
			logError(e);
		} finally {
			try {
				new File(zipPath).delete();
			} catch (final Exception e) {
				logError(e);
			}
		}
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

	private static void log(final String message) {
		final File log = new File(getAppDir() + "/error.log");
		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(log)))) {
			writer.write(message);
			writer.println();
		} catch (final IOException ioe) {
			ioe.printStackTrace();
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
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		} finally {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		final Optional<ProcessHandle> parent = ProcessHandle.current().parent();
		if (parent.isPresent()) {
			try {
				parent.get().onExit().get();
			} catch (final InterruptedException | ExecutionException e) {
				logError(e);
				System.exit(0);
			}
		}
		try {
			appDir = getAppDir().getCanonicalFile();
			Files.lines(Paths.get(updateListPath)).forEach(line -> {
				final String[] entry = line.split(";", 5);
				if (entry.length == 5) {
					applyUpdate(entry[0], entry[1], entry[2], entry[3], entry[4]);
				} else {
					applyUpdate(entry[0], null, null, null, null);
				}
			});
			new File(updateListPath).delete();
		} catch (final IOException e) {
			logError(e);
		}
		try {
			Runtime.getRuntime().exec(new String[] { ProcessHandle.current().info().command().orElse("java"), "-jar", getAppDir() + "/DSATool.jar" });
		} catch (final IOException e) {
			logError(e);
		}
		System.exit(0);
	}

	private Updater() {}
}
