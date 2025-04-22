package com.romiis.equallib_playground.controllers;

import com.romiis.core.EqualLib;
import com.romiis.core.EqualLibConfig;
import com.romiis.equallib_playground.CacheUtil;
import com.romiis.equallib_playground.components.listView.ClassListView;
import com.romiis.equallib_playground.components.treeView.MyTreeView;
import com.romiis.equallib_playground.util.ObjectFillerUtil;
import com.romiis.equallib_playground.util.ReflectionUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.log4j.Log4j2;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * MainSceneController.java
 * <p>
 * This class is the controller for the main scene of the application.
 * It handles the UI components and their interactions.
 */
@Log4j2
public class MainSceneController {

    @FXML
    private MyTreeView treeView1;
    @FXML
    private MyTreeView treeView2;
    @FXML
    private StackPane loadingOverlay;
    @FXML
    private ClassListView objectListView1;
    @FXML
    private ClassListView objectListView2;
    @FXML
    private Label comparisonResult;
    @FXML
    private Label comparisonTime;
    @FXML
    private ListView<String> ignoredFieldsList;
    @FXML
    private TextField newIgnoredField;
    @FXML
    private Spinner<Integer> maxDepthSpinner;
    @FXML
    private TitledPane fillSettingsPane;
    @FXML
    private Slider arraySizeSlider;
    @FXML
    private Spinner<Integer> arraySizeSpinner;
    @FXML
    private Slider collectionSizeSlider;
    @FXML
    private Spinner<Integer> collectionSizeSpinner;
    @FXML
    private RadioButton similarRadio;
    @FXML
    private RadioButton differentRadio;
    @FXML
    private ToggleGroup fillToggleGroup;
    @FXML
    private Slider maxDepthSliderFill;
    @FXML
    private Spinner<Integer> maxDepthSpinnerFill;
    @FXML
    private CheckBox useEqualsAfterMaxDepth;
    @FXML
    private CheckBox equivalenceByInheritance;
    @FXML
    private CheckBox compareByElementsAndKeys;


    /**
     * The initialize method is called when the FXML file is loaded.
     * It initializes the UI components and sets up event handlers.
     */
    @FXML
    private void initialize() {
        CacheUtil.getInstance().updateCache();

        initializeMaxDepthSpinner();
        initializeObjects();
        initializeFillSettings();

        objectListView1.setAssignedTreeView(treeView1);
        objectListView2.setAssignedTreeView(treeView2);
        treeView1.setClassListView(objectListView1);
        treeView2.setClassListView(objectListView2);
    }

    /**
     * Initializes the max depth spinner with a value factory.
     * The spinner allows values from -1 (unlimited) to 100.
     */
    private void initializeMaxDepthSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-1, 100, -1);
        maxDepthSpinner.setValueFactory(valueFactory);
    }

    /**
     * Initializes the objects in the list views.
     * This method populates the list views with classes from the cache.
     */
    private void initializeObjects() {
        objectListView1.getItems().addAll(CacheUtil.getInstance().getClasses(true));
        objectListView2.getItems().addAll(CacheUtil.getInstance().getClasses(true));
    }

    /**
     * Initializes the fill settings for the fill objects panel.
     * This method sets up the sliders and spinners for array size and collection size.
     */
    private void initializeFillSettings() {
        // When the slider changes, update the spinner.
        arraySizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int newInt = newVal.intValue();
            if (!newIntEquals(arraySizeSpinner.getValue(), newInt)) {
                arraySizeSpinner.getValueFactory().setValue(newInt);
            }
        });
        // When the spinner changes, update the slider.
        arraySizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            double newDouble = newVal.doubleValue();
            if (arraySizeSlider.getValue() != newDouble) {
                arraySizeSlider.setValue(newDouble);
            }
        });

        // Same for collection size.
        collectionSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int newInt = newVal.intValue();
            if (!newIntEquals(collectionSizeSpinner.getValue(), newInt)) {
                collectionSizeSpinner.getValueFactory().setValue(newInt);
            }
        });
        collectionSizeSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            double newDouble = newVal.doubleValue();
            if (collectionSizeSlider.getValue() != newDouble) {
                collectionSizeSlider.setValue(newDouble);
            }
        });

        maxDepthSpinnerFill.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxDepthSliderFill.setValue(newVal.doubleValue());
        });
        maxDepthSliderFill.valueProperty().addListener((obs, oldVal, newVal) -> {
            maxDepthSpinnerFill.getValueFactory().setValue(newVal.intValue());
        });


        // Set default selection for the radio buttons.
        similarRadio.setSelected(true);
    }

    /**
     * Helper method to check if two Integer values are equal.
     * This is used to avoid NullPointerExceptions when comparing Integer objects.
     *
     * @param a The Integer object
     * @param b The int value
     * @return true if they are equal, false otherwise
     */
    private boolean newIntEquals(Integer a, int b) {
        return a != null && a.intValue() == b;
    }


    /**
     * Helper method to check if two Integer values are equal.
     * This is used to avoid NullPointerExceptions when comparing Integer objects.
     *
     * @param event The ActionEvent
     */
    @FXML
    private void onAddIgnoredField(ActionEvent event) {
        String ignoredField = newIgnoredField.getText().trim();
        if (!ignoredField.isEmpty()) {
            addIgnoredFieldToList(ignoredField);
        }
    }

    /**
     * Adds the ignored field to the list view and clears the text field.
     *
     * @param ignoredField The ignored field to add
     */
    private void addIgnoredFieldToList(String ignoredField) {
        ignoredFieldsList.getItems().add(ignoredField);
        newIgnoredField.clear();
    }

    /**
     * Removes the selected ignored field from the list view.
     *
     * @param event The ActionEvent
     */
    @FXML
    private void onRemoveIgnoredField(ActionEvent event) {
        String selectedField = ignoredFieldsList.getSelectionModel().getSelectedItem();
        if (selectedField != null) {
            removeIgnoredFieldFromList(selectedField);
        }
    }

    /**
     * Removes the ignored field from the list view.
     *
     * @param selectedField The ignored field to remove
     */
    private void removeIgnoredFieldFromList(String selectedField) {
        ignoredFieldsList.getItems().remove(selectedField);
    }


    /**
     * Button handler for the "Make Tests" button.
     */
    @FXML
    private void onMakeTestsButtonClick() {
        int testCount = 12;

        // Create your config and get the two objects to compare
        EqualLibConfig config = createConfig();
        Object object1 = treeView1.getSelectedObject();
        Object object2 = treeView2.getSelectedObject();

        // Prepare a background Task that returns (areEqual, elapsedNanos)
        EqualLib.clearFieldCache();

        float[] elapsedTimes = new float[testCount];

        boolean t = true;
        for (int i = 0; i < testCount; i++) {
            long start = System.nanoTime();
            boolean result = EqualLib.areEqual(object1, object2, config);
            long end = System.nanoTime();
            if (!result) {
                t = false;
            }
            elapsedTimes[i] = (end - start) / 1_000_000.0f;
        }

        log.info("Current locale: " + Locale.getDefault());


        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');  // <– here’s the magic
        DecimalFormat df = new DecimalFormat("0.###", symbols);

        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < testCount; i++) {
            csv.append(df.format(elapsedTimes[i])).append(";");

        }
        // Remove the last semicolon
        csv.deleteCharAt(csv.length() - 1);

        log.info(csv + "|" + t + "|");

        // Show the result in a dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Test Results");
        alert.setHeaderText("Test Results in CSV format");
        alert.setContentText(String.valueOf(csv));
        alert.showAndWait();
    }


    /**
     * Button handler for the "Compare" button.
     * This method compares two objects selected in the tree views.
     */
    @FXML
    private void onCompareButtonClick() {
        // Create your config and get the two objects to compare
        EqualLibConfig config = createConfig();
        Object object1 = treeView1.getSelectedObject();
        Object object2 = treeView2.getSelectedObject();

        // Prepare a background Task that returns (areEqual, elapsedNanos)
        Task<javafx.util.Pair<Boolean, Long>> compareTask = new Task<>() {
            @Override
            protected javafx.util.Pair<Boolean, Long> call() {
                long start = System.nanoTime();
                boolean result = EqualLib.areEqual(object1, object2, config);
                long end = System.nanoTime();
                return new javafx.util.Pair<>(result, end - start);
            }
        };

        // --- ANIMATION: A little "dot" animation while comparing. ---
        // We'll cycle the label text: "Comparing", "Comparing.", "Comparing..", "Comparing..."
        comparisonResult.setText("Comparing");

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0.5), e -> comparisonResult.setText("Comparing.")),
                new KeyFrame(Duration.seconds(1.0), e -> comparisonResult.setText("Comparing..")),
                new KeyFrame(Duration.seconds(1.5), e -> comparisonResult.setText("Comparing...")),
                new KeyFrame(Duration.seconds(2.0), e -> comparisonResult.setText("Comparing"))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // When the task finishes successfully:
        compareTask.setOnSucceeded(evt -> {
            // Stop the animation
            timeline.stop();

            // Retrieve the (areEqual, elapsedNanos) from the Task
            javafx.util.Pair<Boolean, Long> resultPair = compareTask.getValue();
            boolean areEqual = resultPair.getKey();
            long nanos = resultPair.getValue();

            // Update the label with the result and time
            comparisonResult.setText(
                    "Comparison result: " + areEqual + "\n" + calculateComparisonTime(nanos)
            );
        });

        // If something goes wrong (e.g., an exception)...
        compareTask.setOnFailed(evt -> {
            timeline.stop();
            Throwable ex = compareTask.getException();
            comparisonResult.setText("Comparison failed: " + ex.getMessage());
        });

        // Run the task in a background thread
        Thread bgThread = new Thread(compareTask, "ComparisonThread");
        bgThread.setDaemon(true);
        bgThread.start();

    }

    /**
     * Helper method to format the comparison time in a human-readable format.
     *
     * @param nanos The time in nanoseconds
     * @return A formatted string representing the comparison time
     */
    private String calculateComparisonTime(long nanos) {
        if (nanos < 1_000) {
            return "Comparison time: " + nanos + " ns";
        } else if (nanos < 1_000_000) {
            double micros = nanos / 1_000.0;
            return String.format("Comparison time: %.2f µs", micros);
        } else if (nanos < 1_000_000_000) {
            double millis = nanos / 1_000_000.0;
            return String.format("Comparison time: %.2f ms", millis);
        } else {
            double seconds = nanos / 1_000_000_000.0;
            return String.format("Comparison time: %.2f s", seconds);
        }
    }


    /**
     * Creates a configuration object for the comparison.
     *
     * @return An EqualLibConfig object with the current settings
     */
    private EqualLibConfig createConfig() {
        EqualLibConfig config = new EqualLibConfig();
        config.setMaxComparisonDepth(maxDepthSpinner.getValue(), useEqualsAfterMaxDepth.isSelected())
                .setCompareCollectionsByElements(compareByElementsAndKeys.isSelected())
                .setCompareInheritedFields(equivalenceByInheritance.isSelected())
                .setIgnoredFieldPaths(ignoredFieldsList.getItems().toArray(new String[0]));
        return config;
    }


    /**
     * Button handler for the "Clear" button.
     * This method clears the selected objects in both tree views.
     */
    @FXML
    public void onSaveAsButton1Click() throws Exception {
        if (maxDepthSpinnerFill.getValue() >= 1000) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Max depth cannot exceed 999.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        treeView1.save();
    }


    /**
     * Button handler for the "Clear" button.
     * This method clears the selected objects in both tree views.
     */
    @FXML
    public void onSaveAsButton2Click() throws Exception {
        if (maxDepthSpinnerFill.getValue() >= 1000) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Max depth cannot exceed 999.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        treeView2.save();
    }

    /**
     * Button handler for the "Load" button.
     * This method loads the selected objects in both tree views.
     */
    @FXML
    public void onLoadButtonClick() {
        treeView1.load();
    }

    /**
     * Button handler for the "Load" button.
     * This method loads the selected objects in both tree views.
     */
    @FXML
    public void onLoadButton2Click() {
        treeView2.load();
    }

    /**
     * Button handler for the "Fill" button.
     * This method fills the selected objects in both tree views with random data.
     */
    @FXML
    public void onFillButtonClick(ActionEvent event) {
        int arraySize = arraySizeSpinner.getValue();
        int collectionSize = collectionSizeSpinner.getValue();
        boolean fillSimilar = similarRadio.isSelected();

        // Retrieve the currently selected objects.
        Object obj1 = treeView1.getSelectedObject();
        Object obj2 = treeView2.getSelectedObject();

        if (obj1 == null || obj2 == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Please select objects in both tree views to fill.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // check if both objects have at least super class
        if (!ReflectionUtil.hasCommonSuperclass(obj1.getClass(), obj2.getClass())) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Objects must have a common superclass to fill", ButtonType.OK);
            alert.showAndWait();
            return;
        }


        // Create a Task to perform the fill operation.
        Task<Void> fillTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                long startTime = System.currentTimeMillis();
                // Run the fill operation (this may take time)
                ObjectFillerUtil.fillObjects(obj1, obj2, fillSimilar, arraySize, collectionSize, maxDepthSpinnerFill.getValue());
                long elapsed = System.currentTimeMillis() - startTime;
                updateMessage("Completed in " + (elapsed / 1000.0) + " seconds.");
                updateProgress(1, 1);
                return null;
            }
        };

        // Create an indeterminate ProgressBar.
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setProgress(-1); // indeterminate

        // Create a label to display elapsed time.
        Label progressLabel = new Label("Starting...");
        // Do not bind the label's text property here—update it manually.

        final long startTime = System.currentTimeMillis();
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    progressLabel.setText("Elapsed time: " + (elapsed / 1000) + " seconds...");
                })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Create a simple window (stage) to show progress.
        VBox vbox = new VBox(10, progressBar, progressLabel);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        Scene scene = new Scene(vbox);
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("Filling Objects...");
        progressStage.setScene(scene);
        progressStage.setResizable(false);
        progressStage.show();

        fillTask.setOnSucceeded(e -> {
            timeline.stop();
            progressStage.close();
            log.info("Fill operation completed.");
            treeView1.refresh();
            treeView2.refresh();
        });

        fillTask.setOnFailed(e -> {
            timeline.stop();
            fillTask.getException().printStackTrace();
            progressStage.close();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Fill operation failed: " +
                    fillTask.getException().getMessage(), ButtonType.OK);
            errorAlert.showAndWait();
        });

        new Thread(fillTask).start();
    }


}
