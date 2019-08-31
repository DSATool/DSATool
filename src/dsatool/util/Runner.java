package dsatool.util;

import java.lang.ProcessBuilder.Redirect;

public class Runner {

	public static void main(final String[] args) {
		final ProcessBuilder pb = new ProcessBuilder(Util.javaExecutable,
				"--enable-preview", // TODO remove once switch is not preview anymore
				"--add-opens", "javafx.graphics/javafx.scene=ALL-UNNAMED",
				"-cp", "DSATool.jar",
				"dsatool.gui.Main");
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		try {
			pb.start();
		} catch (final Exception e) {
			ErrorLogger.logError(e);
		}
	}
}
