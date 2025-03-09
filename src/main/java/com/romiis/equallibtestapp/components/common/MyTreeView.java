package com.romiis.equallibtestapp.components.common;

import com.romiis.equallibtestapp.MainClass;
import com.romiis.equallibtestapp.components.mainScene.ClassListView;
import com.romiis.equallibtestapp.controllers.LoadObjectController;
import com.romiis.equallibtestapp.io.FileManager;
import com.romiis.equallibtestapp.util.JsonUtil;
import com.romiis.equallibtestapp.util.ObjectTreeBuilder;
import com.romiis.equallibtestapp.util.ReflectionUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

/**
 * MyTreeView.java
 * <p>
 * Custom TreeView component for displaying and editing object fields in MainScene.
 * It handles selection changes, editing, saving, and loading of objects.
 * </p>
 */
@Slf4j
public class MyTreeView extends TreeView<ObjectReference> {

    /**
     * The object instance that is currently selected in the ListView.
     */
    private final ObjectProperty<Object> selectedObject = new SimpleObjectProperty<>();

    /**
     * Reference to the class list view.
     */
    @Setter
    private ClassListView classListView;

    /**
     * Flag indicating whether to treat fields as objects or display as simple values.
     */
    private boolean treatAsObjects = true;

    /**
     * Flag to indicate if the object has been modified (dirty flag).
     */
    @Getter
    @Setter
    private boolean modified = false;

    /**
     * Constructs a new MyTreeView instance.
     */
    public MyTreeView() {
        super();
        initializeClickHandler();
        setEditable(true);

        // Listener for changes to the selected object.
        selectedObject.addListener((obs, oldVal, newVal) -> {
            System.out.println("Selected object changed: " + newVal);
            // Additional UI updates can be added here.
        });
    }

    /**
     * Initializes the click handlers for the TreeView.
     * Sets up selection change listeners and double-click behavior for editing fields.
     */
    private void initializeClickHandler() {
        // Listen for selection changes.
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ObjectReference objectReference = newValue.getValue();
                if (objectReference.getField() != null && objectReference.isModifiable()) {
                    log.info("Field: {}", objectReference);
                }
            }
        });

        // Handle mouse double-click events.
        this.setOnMouseClicked(event -> {
            if (!isEditable()) return;
            if (event.getClickCount() == 2) {
                TreeItem<ObjectReference> selectedItem = this.getSelectionModel().getSelectedItem();
                if (selectedItem == null) {
                    return;
                }
                // If the selected item has children, do not start editing.
                if (!selectedItem.getChildren().isEmpty()) {
                    return;
                }
                // If the field is modifiable, start editing.
                if (selectedItem.getValue().isModifiable()) {
                    startEdit(selectedItem);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Warning");
                    alert.setHeaderText("Field is not modifiable");
                    alert.setContentText("The selected field is not modifiable");
                    alert.showAndWait();
                }
            }
        });
    }

    /**
     * Starts editing the selected field.
     *
     * @param item The TreeItem that should be edited.
     */
    private void startEdit(TreeItem<ObjectReference> item) {
        ObjectReference objectReference = item.getValue();
        Field field = objectReference.getField();

        // Only proceed if the field is non-null and modifiable.
        if (field == null || !objectReference.isModifiable()) {
            return;
        }

        EditorsUtil editors = new EditorsUtil();
        editors.decideEditor(this, item, objectReference, field);
    }

    /**
     * Sets the selected object instance based on a Class.
     *
     * @param selectedObjectName The class of the selected object.
     */
    public void setSelectedObject(Class<?> selectedObjectName) {
        Object instance = ReflectionUtil.createInstance(selectedObjectName);
        setSelectedObject(instance);
    }

    /**
     * Sets the selected object instance.
     *
     * @param selectedObject The object instance to set.
     */
    public void setSelectedObject(Object selectedObject) {
        this.selectedObject.set(selectedObject);
        modified = false;
        handleSelectionChange(treatAsObjects);
    }

    /**
     * Clears the TreeView and selected object.
     */
    public void clear() {
        this.setRoot(null);
        this.selectedObject.set(null);
    }

    /**
     * Handles changes in selection from the ListView.
     * Delegates tree creation to ObjectTreeBuilder and expands the resulting tree.
     *
     * @param treatAsObjects Whether to treat fields as objects or print simple values.
     */
    public void handleSelectionChange(boolean treatAsObjects) {
        if (selectedObject.get() == null) {
            return;
        }
        this.treatAsObjects = treatAsObjects;

        // Create the root TreeItem for the selected object.
        TreeItem<ObjectReference> rootItem = new TreeItem<>(new ObjectReference(selectedObject.get(), null));

        // Create a set to track visited objects to prevent cyclic recursion.
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        // Build the tree structure.
        rootItem = ObjectTreeBuilder.createTree(rootItem, visited, false);

        // Set the root item of the TreeView.
        this.setRoot(rootItem);

        // Expand all nodes in the tree.
        ObjectTreeBuilder.expandAll(rootItem);
    }

    /**
     * Saves the current selected object.
     *
     * @return True if the save was successful, false otherwise.
     */
    public boolean save() {
        TextInputDialog dialog = new TextInputDialog("savedObject");
        dialog.setTitle("Save File");
        dialog.setHeaderText("Enter file name:");
        dialog.setContentText("File Name:");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                String content = JsonUtil.serialize(this.getSelectedObject());
                log.info("Serialized object: {}", content);
                FileManager.saveFile(result.get(), content);
                this.setModified(false);
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the currently selected object.
     *
     * @return The selected object.
     */
    public Object getSelectedObject() {
        return selectedObject.get();
    }

    /**
     * Loads an object from a file.
     * Opens a file selection dialog and loads the object into the TreeView.
     */
    public void load() {


        if (modified) {
            SaveResult result = handleUnsavedChanges();
            if (result == SaveResult.SAVE) {
                boolean saved = save();
                if (!saved) {
                    log.error("Error saving object");
                    return;
                }
            } else if (result == SaveResult.CANCEL) {
                return;
            }
        }


        // Get list of saved files.
        String[] files = FileManager.getSavedFiles();
        if (files.length == 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning");
            alert.setHeaderText("No files found");
            alert.setContentText("No files found in the save folder");
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.LOAD_OBJECTS_SCENE_FXML));
            Parent root = loader.load();

            // Pass this TreeView to the controller.
            LoadObjectController controller = loader.getController();
            controller.setAssignedTreeView(this);

            Stage stage = new Stage();
            stage.setTitle("Load Object");
            stage.setScene(new Scene(root, 800, 600));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            log.error("Error loading object", e);
        }
    }

    public void refresh() {
        handleSelectionChange(treatAsObjects);
    }

    /**
     * Changes the selected object from the list view.
     * If unsaved changes exist, prompts the user to save or discard changes.
     *
     * @param clazz The class to create an instance from.
     * @return True if the change was successful, false otherwise.
     */
    public SaveResult changeFromListView(Class<?> clazz) {
        SaveResult result;
        if (modified) {

            result = handleUnsavedChanges();
            if (result == SaveResult.SAVE) {
                boolean saved = save();
                if (!saved) {
                    log.error("Error saving object");
                    return SaveResult.CANCEL;
                }
            } else if (result == SaveResult.CANCEL) {
                return SaveResult.CANCEL;
            }
        }
        setSelectedObject(clazz);
        return SaveResult.SAVE;
    }

    /**
     * Handles unsaved changes by prompting the user.
     *
     * @return True if the change should proceed, false otherwise.
     */
    private SaveResult handleUnsavedChanges() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Unsaved changes");
        alert.setContentText("There are unsaved changes in the current object. Do you want to save them?");

        ButtonType saveButton = new ButtonType("Save");
        ButtonType discardButton = new ButtonType("Discard");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();


        if (result.isPresent()) {
            ButtonType buttonType = result.get();
            if (buttonType == saveButton) {
                return SaveResult.SAVE;
            } else if (buttonType == discardButton) {
                return SaveResult.DISCARD;
            } else {
                return SaveResult.CANCEL;
            }
        }
        return SaveResult.CANCEL;
    }


    public void updateObject(ObjectReference objectReference) {


    }
}
