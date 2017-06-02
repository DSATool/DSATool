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
package dsatool.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javafx.application.Platform;

/**
 * A class for displaying and logging errors
 *
 * @author Dominik Helm
 */
public class ErrorLogger {
	public static void log(final String text) {
		Platform.runLater(() -> Util.alert(text).show());
		final File log = new File(Util.getAppDir() + "/error.log");
		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(log)))) {
			writer.write(text);
			writer.println();
		} catch (final IOException e1) {
			e1.printStackTrace();
		} finally {
			System.err.println(text);
		}
	}

	/**
	 * Logs any kind of exception, displaying a dialog if possible
	 *
	 * @param e
	 *            The exception that happened
	 */
	public static void logError(final Exception e) {
		Platform.runLater(() -> Util.exceptionAlert(e).show());
		final File log = new File(Util.getAppDir() + "/error.log");
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
}
