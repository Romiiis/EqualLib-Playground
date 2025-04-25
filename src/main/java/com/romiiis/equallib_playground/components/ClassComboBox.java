package com.romiiis.equallib_playground.components;

import com.romiiis.equallib_playground.components.listView.LoadObjectListView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.Setter;

/**
 * ClassComboBox.java
 * <p>
 * Represents a ComboBox for selecting classes. Used to filter the objects in the list view by class.
 *
 * @author Romiiis
 * @version 1.0
 */
public class ClassComboBox extends ComboBox<Class<?>> {

    /**
     * The string to display for the "ALL" option
     */
    private final String ALL = "All";

    @Setter
    private LoadObjectListView objectListView;

    /**
     * Create a new ClassComboBox instance
     */
    public ClassComboBox() {
        super();

        // Add a custom "ALL" entry to the ComboBox
        getItems().add(0, null);

        // null represents "ALL" (or a special case)

        // Set the cell factory to display class names (getSimpleName) or "ALL"
        setCellFactory(new Callback<>() {
            @Override
            public ListCell<Class<?>> call(ListView<Class<?>> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Class<?> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(ALL);  // Show "ALL" for the special case
                        } else {
                            setText(item.getSimpleName());  // Show the simple name of the class
                        }
                    }
                };
            }
        });

        // Set the button cell to display the class name or "ALL" as well
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Class<?> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(ALL);  // Show "ALL" for the special case
                } else {
                    setText(item.getSimpleName());  // Show the simple name of the class
                }
            }
        });

        initializeClickHandler();
    }


    /**
     * Initialize the click handler for the ComboBox
     */
    private void initializeClickHandler() {
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Do something with the selected class
            objectListView.filterByClass(newValue);
        });
    }
}
