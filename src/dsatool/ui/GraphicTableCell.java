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

import com.sun.javafx.scene.control.FakeFocusTextField;

import dsatool.util.ErrorLogger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public abstract class GraphicTableCell<S, T> extends TableCell<S, T> {

	private final boolean alwaysVisible;

	protected Node graphic;

	public GraphicTableCell(final boolean alwaysVisible) {
		this.alwaysVisible = alwaysVisible;
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		if (!alwaysVisible) {
			setText(getItem() != null ? getItem().toString() : "");
			setPadding(new Insets(2, 2, 2, 2));
			setGraphic(graphic);
		}
	}

	protected abstract void createGraphic();

	protected void createGraphic(final Control graphic, final Supplier<T> getText, final Consumer<T> setText) {
		if (getGraphic() != null) return;
		setPadding(Insets.EMPTY);
		graphic.setMaxWidth(Double.MAX_VALUE);
		graphic.setContextMenu(getContextMenu());
		final ObjectProperty<T> string = itemProperty();
		setText.accept(string.get());
		setText(null);
		setGraphic(graphic);
		final TableView<S> table = getTableView();
		final TableColumn<S, T> column = getTableColumn();
		graphic.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
				if (oldValue && !newValue) {
					if (graphic.getScene().getFocusOwner() != graphic) {
						final int index = getTableRow().getIndex();
						graphic.focusedProperty().removeListener(this);
						if (index != -1) {
							final CellEditEvent<S, T> editEvent = new CellEditEvent<>(table, new TablePosition<>(table, index, column),
									TableColumn.editCommitEvent(), getText.get());
							Event.fireEvent(getTableColumn(), editEvent);
							cancelEdit();
						}
					} else if (graphic instanceof final ComboBox<?> cb) {
						cb.valueProperty().addListener((o, oldV, newV) -> {
							final int index = getTableRow().getIndex();
							graphic.focusedProperty().removeListener(this);
							if (index != -1) {
								final CellEditEvent<S, T> editEvent = new CellEditEvent<>(table, new TablePosition<>(table, index, column),
										TableColumn.editCommitEvent(), getText.get());
								Event.fireEvent(getTableColumn(), editEvent);
								cancelEdit();
							}
						});
					}
				}
			}
		});
		if (graphic instanceof final TextField tf) {
			tf.setOnAction(e -> {
				requestFocus();
			});
		} else if (graphic instanceof final ButtonBase b) {
			b.setOnAction(e -> {
				requestFocus();
			});
		} else if (graphic instanceof final ComboBox<?> cb) {
			cb.setOnAction(e -> {
				requestFocus();
			});
			graphic.focusedProperty().addListener((o, oldV, newV) -> {
				if (newV) {
					try {
						((FakeFocusTextField) cb.getEditor()).setFakeFocus(true);
					} catch (final Exception e) {
						ErrorLogger.logError(e);
					}
				}
			});
		} else if (graphic instanceof final Spinner<?> s) {
			s.focusedProperty().addListener((o, oldV, newV) -> {
				if (newV) {
					try {
						((FakeFocusTextField) s.getEditor()).setFakeFocus(true);
					} catch (final Exception e) {
						ErrorLogger.logError(e);
					}
				}
			});
		}
	}

	@Override
	public void startEdit() {
		graphic = getGraphic();
		if (!isEmpty() && !alwaysVisible) {
			createGraphic();
		}
		getGraphic().requestFocus();
	}

	@Override
	public void updateItem(final T item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			if (alwaysVisible || isEditing()) {
				if (getGraphic() == null) {
					createGraphic();
					setText(null);
					setGraphic(getGraphic());
				}
			} else {
				if (item == null) {
					setText("");
				} else {
					setText(item.toString());
				}
				setPadding(new Insets(2, 2, 2, 2));
				setGraphic(null);
			}
		}
	}
}
