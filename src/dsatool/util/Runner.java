package dsatool.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import dsatool.gui.Main;
import javafx.application.Application;

public class Runner {

	public static void main(final String[] args) {
		Logger.getLogger("javafx").setLevel(Level.SEVERE);
		System.setProperty("javafx.enablePreview", "true");
		Application.launch(Main.class, args);
	}
}
