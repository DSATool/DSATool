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
package dsatool.gui;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;

public class GUIUtil {

	private static DataFormat dragDropDataFormat = new DataFormat("application/x-dropdata");

	private static Object[] dragDropBuffer;

	public static <T> void autosizeTable(final TableView<T> table) {
		table.setColumnResizePolicy(GUIUtil::fixedWidthPolicy);

		final DoubleBinding height = Bindings.size(table.getItems()).multiply(table.getFixedCellSize()).add(26);
		table.minHeightProperty().bind(height);
		table.maxHeightProperty().bind(height);
	}

	public static void cellValueFactories(final TableView<?> table, final String... properties) {
		for (int i = 0; i < table.getColumns().size(); ++i) {
			table.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(properties[i]));
		}
	}

	public static <T> void dragDropReorder(final IndexedCell<T> cell, final Consumer<Object[]> afterDrop, final Control... allowedSources) {
		dragDropReorder(cell, () -> {}, () -> {}, afterDrop, allowedSources);
	}

	@SuppressWarnings("unchecked")
	public static <T> void dragDropReorder(final IndexedCell<T> cell, final Runnable beforeDrag, final Runnable onDrag, final Consumer<Object[]> afterDrop,
			final Control... allowedSources) {

		final Control source = allowedSources[0];
		final List<Control> allowedSourcesList = Arrays.asList(allowedSources);
		final ObservableList<T> items = source instanceof ListView ? ((ListView<T>) source).getItems() : ((TableView<T>) source).getItems();
		final MultipleSelectionModel<T> selectionModel = source instanceof ListView ? ((ListView<T>) source).getSelectionModel()
				: ((TableView<T>) source).getSelectionModel();

		cell.setOnDragDetected(e -> {
			if (cell.isEmpty()) return;
			final Dragboard dragBoard = source.startDragAndDrop(TransferMode.MOVE);
			final ClipboardContent content = new ClipboardContent();
			dragDropBuffer = selectionModel.getSelectedItems().toArray();
			content.put(dragDropDataFormat, selectionModel.getSelectedIndices().toArray(new Integer[0]));
			dragBoard.setContent(content);
			beforeDrag.run();
			for (final Object moved : dragDropBuffer) {
				items.remove(moved);
			}
			e.consume();
		});

		source.setOnDragDone(e -> {
			if (e.getTransferMode() != TransferMode.MOVE) {
				selectionModel.clearSelection();
				final Integer[] indices = (Integer[]) e.getDragboard().getContent(dragDropDataFormat);
				for (int i = 0; i < dragDropBuffer.length; ++i) {
					items.add(indices[i], (T) dragDropBuffer[i]);
					selectionModel.select(indices[i]);
				}
				onDrag.run();
				afterDrop.accept(new Object[0]);
			}
		});

		cell.setOnDragDropped(e -> {
			afterDrop.accept(dragDropBuffer);
			e.setDropCompleted(true);
			e.consume();
		});

		cell.setOnDragOver(e -> {
			if (allowedSourcesList.contains(e.getGestureSource())) {
				e.acceptTransferModes(TransferMode.MOVE);
			}
		});

		cell.setOnDragEntered(e -> {
			if (allowedSourcesList.contains(e.getGestureSource())) {
				cell.requestFocus();
				selectionModel.clearSelection();
				int index = cell.isEmpty() ? items.size() : cell.getIndex();
				for (final Object moved : dragDropBuffer) {
					items.add(index, (T) moved);
					++index;
					selectionModel.select((T) moved);
				}
				onDrag.run();
			}
		});

		cell.setOnDragExited(e -> {
			if (!e.isDropCompleted()) {
				if (allowedSourcesList.contains(e.getGestureSource())) {
					selectionModel.clearSelection();
					for (final Object moved : dragDropBuffer) {
						items.remove(moved);
					}
				}
			}
		});
	}

	public static void dragDropReorder(final Node node, final Consumer<Node> afterDrop, final Pane... allowedSources) {
		dragDropReorder(node, () -> {}, () -> {}, afterDrop, allowedSources);
	}

	public static void dragDropReorder(final Node node, final Runnable beforeDrag, final Runnable onDrag, final Consumer<Node> afterDrop,
			final Pane... allowedSources) {

		final Pane source = allowedSources[0];
		final List<Pane> allowedSourcesList = Arrays.asList(allowedSources);
		final ObservableList<Node> items = source.getChildren();

		node.setOnDragDetected(e -> {
			final Dragboard dragBoard = source.startDragAndDrop(TransferMode.MOVE);
			final ClipboardContent content = new ClipboardContent();
			dragDropBuffer = new Object[] { node };
			content.put(dragDropDataFormat, items.indexOf(node));
			dragBoard.setContent(content);
			beforeDrag.run();
			e.consume();
		});

		source.setOnDragDropped(e -> {
			source.requestFocus();
			afterDrop.accept((Node) dragDropBuffer[0]);
			e.setDropCompleted(true);
			e.consume();
		});

		node.setOnDragOver(e -> {
			if (allowedSourcesList.contains(e.getGestureSource())) {
				e.acceptTransferModes(TransferMode.MOVE);

				if (node != dragDropBuffer[0]) {
					final boolean above = e.getSceneY() < node.localToScene(node.getBoundsInLocal()).getCenterY();
					items.remove(dragDropBuffer[0]);
					final int index = items.indexOf(node);
					if (index >= 0) {
						items.add(above ? index : index + 1, (Node) dragDropBuffer[0]);
						onDrag.run();
					}
				}
			}
			e.consume();
		});

		source.setOnDragOver(e -> {
			if (allowedSourcesList.contains(e.getGestureSource())) {
				e.acceptTransferModes(TransferMode.MOVE);
			}
			e.consume();
		});
	}

	private static <T> boolean fixedWidthPolicy(final ResizeFeatures<T> rf) {
		final TableView<T> table = rf.getTable();
		if (table.getWidth() == 0) return false;

		TableColumn<T, ?> toResize = rf.getColumn();
		double delta = rf.getDelta();

		final ObservableList<TableColumn<T, ?>> columns = table.getVisibleLeafColumns();

		double minWidth = 0;
		double maxWidth = 0;
		double resizableColumnsTotal = 0;
		for (final TableColumn<?, ?> col : columns) {
			final double min = col.getMinWidth();
			final double max = col.getMaxWidth();

			minWidth += min;
			maxWidth += max;

			if (min != max) {
				resizableColumnsTotal += col.getPrefWidth();
			}
		}

		final double slackUp = table.getWidth() - 1 - minWidth;
		final double slackDown = maxWidth - table.getWidth() + 1;

		if (resizableColumnsTotal == 0) {
			columns.get(0).setPrefWidth(columns.get(0).getWidth() + slackUp);
			return true;
		}

		double remainingTotal;

		if (delta == 0 || slackUp <= 0 || slackDown <= 0) {
			delta = table.getWidth() - 1;
			for (final TableColumn<?, ?> col : columns) {
				delta -= col.getWidth();
			}

			remainingTotal = resizableColumnsTotal;
		} else {
			// A fixed-width column can be marked resizable in order to get a resize-control on the left side of the column that actually should be resized
			if (toResize.getMinWidth() == toResize.getMaxWidth()) {
				final int index = table.getColumns().indexOf(toResize) + 1;
				if (index >= table.getColumns().size()) return true;
				toResize = table.getColumns().get(index);
				delta = -delta;
			}

			if (delta > 0) {
				delta = -Math.min(Math.min(delta, toResize.getMaxWidth() - toResize.getWidth()), slackUp - toResize.getWidth() + toResize.getMinWidth());
			} else {
				delta = -Math.max(Math.max(delta, toResize.getMinWidth() - toResize.getWidth()), toResize.getMaxWidth() - toResize.getWidth() - slackDown);
			}

			remainingTotal = resizableColumnsTotal - toResize.getPrefWidth();

			resizeTableColumn(table, toResize, -delta);
		}

		while (Math.abs(delta) > 0.00001) {
			double remainingDelta = 0;
			for (final TableColumn<T, ?> col : columns) {
				final double min = col.getMinWidth();
				final double max = col.getMaxWidth();
				if (min != max) {
					if (col != toResize && (delta > 0 && col.getWidth() < max || delta < 0 && col.getWidth() > min)) {

						final double colDelta = delta * (col.getPrefWidth() / remainingTotal);

						double newDelta;
						if (colDelta > 0) {
							newDelta = Math.min(colDelta, col.getMaxWidth() - col.getWidth());
						} else {
							newDelta = Math.max(colDelta, col.getMinWidth() - col.getWidth());
						}
						if (colDelta != newDelta) {
							remainingDelta += colDelta - newDelta;
							remainingTotal -= col.getPrefWidth();
						}

						resizeTableColumn(table, col, newDelta);
					}
				}
			}
			delta = remainingDelta;
		}

		return true;
	}

	private static <T> void resizeTableColumn(final TableView<T> table, final TableColumn<T, ?> col, final double delta) {
		final boolean resizable = col.isResizable();
		col.setResizable(true);
		TableView.UNCONSTRAINED_RESIZE_POLICY.call(new ResizeFeatures<>(table, col, delta));
		col.setResizable(resizable);
	}

	private GUIUtil() {}

}
