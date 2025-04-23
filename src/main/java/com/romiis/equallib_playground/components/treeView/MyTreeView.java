package com.romiis.equallib_playground.components.treeView;

import com.romiis.equallib_playground.MainClass;
import com.romiis.equallib_playground.components.listView.ClassListView;
import com.romiis.equallib_playground.components.listView.SaveResult;
import com.romiis.equallib_playground.controllers.LoadObjectController;
import com.romiis.equallib_playground.io.FileManager;
import com.romiis.equallib_playground.util.EditorsUtil;
import com.romiis.equallib_playground.util.JsonUtil;
import com.romiis.equallib_playground.util.ObjectTreeBuilder;
import com.romiis.equallib_playground.util.ReflectionUtil;
import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

/**
 * MyTreeView.java
 * <p>
 * Custom TreeView component for displaying and editing object fields in MainScene.
 * It handles selection changes, editing, saving, and loading of objects.
 *
 * @author Romiis
 * @version 1.0
 */
@Log4j2
public class MyTreeView extends TreeView<FieldNode> {

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
    }

    /**
     * Initializes the click handlers for the TreeView.
     * Sets up selection change listeners and double-click behavior for editing fields.
     */
    private void initializeClickHandler() {
        // Listen for double-clicks on the TreeView
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                LazyTreeItem selectedItem = (LazyTreeItem) getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue() != null) {
                    // Call the editor for the selected node.
                    // EditorsUtil.decideEditor() will choose the proper editor based on the FieldNode's type.
                    new EditorsUtil().decideEditor(this, selectedItem, selectedItem.getValue());
                }
            }
        });
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
        handleSelectionChange();
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
     */
    public void handleSelectionChange() {
        if (selectedObject.get() == null) {
            return;
        }


        ObjectTreeBuilder objectTreeBuilder = new ObjectTreeBuilder();
        // Build the tree structure.
        FieldNode rootItem = objectTreeBuilder.buildTree(selectedObject.get(), Integer.MAX_VALUE);

        // Wrap the FieldNode in a TreeItem and set it as the root of the TreeView.
        LazyTreeItem rootTreeItem = new LazyTreeItem(rootItem);
        rootTreeItem.setExpanded(true);
        this.setRoot(rootTreeItem);


    }


    public boolean save() {
        TextInputDialog dialog = new TextInputDialog("savedObject");
        dialog.setTitle("Save File");
        dialog.setHeaderText("Enter file name:");
        dialog.setContentText("File Name:");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent() && !result.get().trim().isEmpty()) {
            // Create an overlay stage to show the loading animation.
            Stage overlayStage = new Stage(StageStyle.TRANSPARENT);
            overlayStage.initModality(Modality.APPLICATION_MODAL);
            // Create a StackPane as root with a semi-transparent background.
            StackPane overlayRoot = new StackPane();
            overlayRoot.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);");
            // Create and add a ProgressIndicator.
            ProgressIndicator indicator = new ProgressIndicator();
            overlayRoot.getChildren().add(indicator);
            Scene overlayScene = new Scene(overlayRoot, 100, 100);
            overlayScene.setFill(null);
            overlayStage.setScene(overlayScene);

            // Fade in the overlay.
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlayRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> overlayStage.show());
            fadeIn.play();

            // Create a background task to execute the save operation.
            Task<Boolean> saveTask = new Task<>() {
                @Override
                protected Boolean call() {
                    try {
                        String content = JsonUtil.serialize(getSelectedObject());
                        log.info("Serialized object: {}", content);
                        FileManager.saveFile(result.get(), content);
                        setModified(false);
                        return true;
                    } catch (Exception e) {
                        log.error("Error during save operation", e);
                        return false;
                    }
                }
            };

            // When the task finishes (successfully or not), fade out the overlay.
            saveTask.setOnSucceeded(event -> {
                fadeOutAndClose(overlayStage, overlayRoot);
                if (!saveTask.getValue()) {
                    // Optionally, show an error alert if save failed.
                    // new Alert(Alert.AlertType.ERROR, "Save operation failed.", ButtonType.OK).showAndWait();
                }
            });
            saveTask.setOnFailed(event -> {
                fadeOutAndClose(overlayStage, overlayRoot);
                // Optionally, show an error alert.
                // new Alert(Alert.AlertType.ERROR, "Save operation failed.", ButtonType.OK).showAndWait();
            });

            new Thread(saveTask).start();
            return true;
        }
        return false;
    }

    /**
     * Fades out the given overlay and closes its stage when finished.
     */
    private void fadeOutAndClose(Stage overlayStage, StackPane overlayRoot) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlayRoot);
        fadeOut.setFromValue(overlayRoot.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> overlayStage.close());
        fadeOut.play();
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
        handleSelectionChange();
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

}
