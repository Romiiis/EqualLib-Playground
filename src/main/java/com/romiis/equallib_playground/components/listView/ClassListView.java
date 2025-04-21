package com.romiis.equallib_playground.components.listView;

import com.romiis.equallib_playground.components.treeView.MyTreeView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import lombok.Setter;

/**
 * A ListView that displays classes in the main scene
 * <p>
 * This class is used to display classes in the main scene. It is used to select a class to display in the tree view.
 * It also handles the case where the tree view is modified and shows an alert to save the changes.
 * <p>
 */
@Setter
public class ClassListView extends ListView<Class<?>> {

    /**
     * The assigned TreeView
     */
    private MyTreeView assignedTreeView;

    /**
     * Flag to ignore selection changes (when the tree view changes the selection)
     */
    private boolean ignoreSelectionChange = false;


    /**
     * Create a new MyListView instance
     */
    public ClassListView() {
        super();

        setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Class item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSimpleName());
                }
            }
        });

        initializeClickHandler();
    }


    /**
     * Initialize the click handler for the ListView
     */
    private void initializeClickHandler() {
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (ignoreSelectionChange) {
                return;
            }

            SaveResult changed = assignedTreeView.changeFromListView(newValue);
            if (changed == SaveResult.CANCEL) {
                ignoreSelectionChange = true;
                this.getSelectionModel().select(oldValue);
                ignoreSelectionChange = false;
            }


        });
    }


}
