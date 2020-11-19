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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SignatureTool {
	private static final String providerName = "SunEC";
	private static final String keyAlgorithmName = "EC";
	private static final String signatureAlgorithmName = "SHA512withECDSA";
	private static final int keyLength = 256;

	private static void createKeys() {
		try {
			// Get key generator and random number generator
			final KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance(keyAlgorithmName, providerName);
			final SecureRandom secRandom = SecureRandom.getInstanceStrong();

			// Create key pair
			keyGenerator.initialize(keyLength, secRandom);
			final KeyPair keyPair = keyGenerator.generateKeyPair();

			// Output private key to file
			final File privateKeyFile = new File("private.key");
			final PrivateKey privateKey = keyPair.getPrivate();
			try (FileOutputStream privateKeyStream = new FileOutputStream(privateKeyFile)) {
				privateKeyStream.write(privateKey.getEncoded());
			} catch (final Exception e) {
				e.printStackTrace();
				return;
			}
			System.out.println("Saved private key to file: " + privateKeyFile.getAbsolutePath());

			System.out.println("Copy this file to your plugin's release directory and");
			System.out.println("add the lines below to your plugin's 'release-info.template'!");

			// Output algorithm information to System.out
			System.out.println("\"signatureProviderName\" : \"" + providerName + "\",");
			System.out.println("\"signatureKeyAlgorithm\" : \"" + keyAlgorithmName + "\",");
			System.out.println("\"signatureAlgorithm\" : \"" + signatureAlgorithmName + "\",");

			// Output public key to System.out
			final PublicKey publicKey = keyPair.getPublic();
			System.out.println("\"signatureKey\" : \"" + Base64.getEncoder().encodeToString(publicKey.getEncoded()) + "\"");
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		if (args.length == 0) {
			createKeys();
			System.exit(0);
		} else if ("sign".equals(args[0])) {
			if (args.length != 7) {
				System.out.println("Wrong number of arguments");
				System.out.println(
						"Usage: sign pathToDirectoryToBeSigned pathToZipToBeCreated pathToPrivateKeyFile signatureProviderName keyAlgorithm signatureAlgorithm");
				System.exit(1);
			}
			zipAndSign(args[1], args[2], args[3], args[4], args[5], args[6]);
			System.exit(0);
		} else {
			System.out.println("Unknown command");
			System.out.println("Use with no arguments to create a key pair or use 'sign' to zip and sign a directory");
			System.exit(1);
		}
	}

	private static void zipAndSign(final String sourcePath, final String zipPath, final String privateKeyPath, final String signatureProviderName,
			final String keyAlgorithm, final String signatureAlgorithm) {
		// Read the private key bytes
		byte[] keyBytes = new byte[0];
		try (FileInputStream keyStream = new FileInputStream(privateKeyPath)) {
			keyBytes = new byte[keyStream.available()];
			keyStream.read(keyBytes);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		final Provider signatureProvider = Security.getProvider(signatureProviderName);
		try {
			// Prepare the private key
			final KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm, signatureProvider);
			final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
			final PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

			// Initialize signature
			final Signature sig = Signature.getInstance(signatureAlgorithm, signatureProvider);
			sig.initSign(privateKey);

			try (ZipOutputStream zipStream = new ZipOutputStream(new FileOutputStream(zipPath))) {
				// Zip and sign contents of sourcePath directory
				final File source = new File(sourcePath);
				final int sourcePathLength = sourcePath.length() + (sourcePath.endsWith("/") ? 0 : 1);
				zipAndSignDirectory(sourcePathLength, source, zipStream, sig);

				// Add signature.sig signature file to zip
				zipStream.putNextEntry(new ZipEntry("signature.sig"));
				zipStream.write(sig.sign());
			} catch (final Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void zipAndSignDirectory(final int sourcePathLength, final File directory, final ZipOutputStream zipStream, final Signature sig) {
		final byte[] buffer = new byte[1024];

		try {
			for (final File file : directory.listFiles()) {
				String name = file.getPath().substring(sourcePathLength).replaceAll("\\\\", "/");
				if (file.isDirectory()) {
					if (!name.endsWith("/")) {
						name += "/"; // Directory names in zip files have to end in "/"
					}
					zipStream.putNextEntry(new ZipEntry(name));
					sig.update(name.getBytes());
					zipAndSignDirectory(sourcePathLength, file, zipStream, sig);
				} else {
					zipStream.putNextEntry(new ZipEntry(name));
					sig.update(name.getBytes());
					try (FileInputStream input = new FileInputStream(file)) {
						int len;
						while ((len = input.read(buffer)) > 0) {
							zipStream.write(buffer, 0, len);
							sig.update(buffer, 0, len);
						}
					} catch (final Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private SignatureTool() {}
}
