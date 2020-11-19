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
package dsatool.ui;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Represents a node that is always detached from the main window
 *
 * @author Dominik Helm
 */
public class DetachedNode {
	/**
	 * The node managed by this DetachedNode
	 */
	private Node node;

	/**
	 * A constructor for building this node
	 */
	private final Supplier<Node> constructor;

	/**
	 * The window title for detached windows of this node
	 */
	private final String name;

	/**
	 * A function that can customize the stage
	 */
	private final Consumer<Stage> windowHandler;

	/**
	 * The window this node is currently displayed on if detached
	 */
	private Stage window;

	private final int height;
	private final int width;

	/**
	 * Constructs a DetachableNode
	 *
	 * @param constructor
	 *            A function that creates the node as the child of a given
	 *            node
	 * @param name
	 *            The name of the detached window
	 */
	public DetachedNode(final Supplier<Node> constructor, final String name, final int width, final int height, final Consumer<Stage> windowHandler) {
		this.constructor = constructor;
		this.name = name;
		this.width = width;
		this.height = height;
		this.windowHandler = windowHandler;
	}

	/**
	 * Detaches this node into a new window
	 */
	public void open() {
		if (window == null) {
			window = new Stage();
			window.setTitle(name);
			window.setScene(new Scene(new BorderPane(), width, height));
			window.setWidth(width);
			window.setHeight(height);

			window.setOnCloseRequest(event -> {
				window = null;
			});

			if (node == null) {
				if (Platform.isFxApplicationThread()) {
					node = constructor.get();
				} else {
					Platform.runLater(() -> {
						node = constructor.get();
						synchronized (this) {
							notify();
						}
					});
					synchronized (this) {
						while (node == null) {
							try {
								wait();
							} catch (final InterruptedException e) {}
						}
					}
				}
			}

			((BorderPane) window.getScene().getRoot()).setCenter(node);
			node.setVisible(true);

			if (windowHandler != null) {
				windowHandler.accept(window);
			}

			window.show();
		} else {
			window.requestFocus();
		}

	}
}
