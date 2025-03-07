package com.romiis.equallibtestapp.components.mainScene;

import com.romiis.equallibtestapp.components.common.MyTreeView;
import javafx.scene.control.*;
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

    private MyTreeView assignedTreeView;


    private final String SAVE_BUTTON_TEXT = "Save";
    private final String DISCARD_BUTTON_TEXT = "Discard";
    private final String CANCEL_BUTTON_TEXT = "Cancel";


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
    private boolean ignoreSelectionChange = false;


    private void initializeClickHandler() {
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (ignoreSelectionChange) {
                return;
            }

            boolean changed = assignedTreeView.changeFromListView(newValue);
            if (!changed) {
                ignoreSelectionChange = true;
                this.getSelectionModel().select(oldValue);
                ignoreSelectionChange = false;
            }
        });
    }








}
