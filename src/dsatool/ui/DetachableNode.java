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

import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * Represents a node that can be attached and detached from the main window
 *
 * @author Dominik Helm
 */
public class DetachableNode {
	/**
	 * The node managed by this DetachableNode
	 */
	private Node node;

	/**
	 * A constructor for building this node
	 */
	private final Supplier<Node> constructor;

	/**
	 * The context menu entry for attaching or detaching this node
	 */
	private final MenuItem context;

	/**
	 * The window title for detached windows of this node
	 */
	private final String name;

	/**
	 * The node this node will be displayed on if attached
	 */
	private final Pane toolArea;

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
	 * @param item
	 *            The MenuItem the detaching functionality will be added as
	 *            context menu entry
	 * @param name
	 *            The name of the window if detached
	 * @param toolArea
	 *            The node the node will be displayed on if attached
	 */
	public DetachableNode(final Supplier<Node> constructor, final MenuItem item, final String name, final Pane toolArea, final int width, final int height) {
		this.constructor = constructor;
		this.name = name;
		this.toolArea = toolArea;
		this.width = width;
		this.height = height;
		context = item.addContextMenuEntry();
		context.setText("Lösen");
		context.setAction(event -> {
			detach();
			event.consume();
		});
	}

	/**
	 * Attaches the node to the given node
	 *
	 * @param bringToTop
	 *            Make the node visible?
	 */
	public void attach(final boolean bringToTop) {
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
		if (bringToTop) {
			toolArea.getChildren().get(toolArea.getChildren().size() - 1).setVisible(false);
			toolArea.getChildren().add(node);
			node.setVisible(true);
		} else {
			node.setVisible(false);
			toolArea.getChildren().add(0, node);
		}
		if (window != null) {
			window.close();
			window = null;
		}
		Platform.runLater(toolArea::layout);
		context.setText("Lösen");
		context.setAction(event -> {
			detach();
			event.consume();
		});
	}

	/**
	 * Makes this node visible, whether attached or detached
	 */
	public void bringToTop() {
		if (node == null) {
			attach(true);
		} else if (window == null) {
			toolArea.getChildren().get(toolArea.getChildren().size() - 1).setVisible(false);
			node.setVisible(true);
			node.toFront();
		} else {
			window.requestFocus();
		}
	}

	/**
	 * Detaches this node into a new window
	 */
	public void detach() {
		if (window == null) {
			window = new Stage();
			window.setTitle(name);
			window.setScene(new Scene(new BorderPane(), width, height));

			window.setOnCloseRequest(event -> {
				attach(false);
				event.consume();
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
			if (!toolArea.getChildren().isEmpty()) {
				toolArea.getChildren().get(toolArea.getChildren().size() - 1).setVisible(true);
			}

			window.show();

			context.setText("Anhängen");
			context.setAction(event -> {
				attach(true);
				event.consume();
			});
		} else {
			window.requestFocus();
		}

	}
}
