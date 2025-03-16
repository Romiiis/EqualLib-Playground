package com.romiis.equallibtestapp.controllers;

import com.romiis.core.EqualLib;
import com.romiis.core.EqualLibConfig;
import com.romiis.equallibtestapp.CacheUtil;
import com.romiis.equallibtestapp.components.mainScene.ClassListView;
import com.romiis.equallibtestapp.components.common.MyTreeView;
import com.romiis.equallibtestapp.util.ObjectFillerUtil;
import com.romiis.equallibtestapp.util.ReflectionUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class MainSceneController {

    // --- Existing UI Components ---
    @FXML
    private MyTreeView treeView1;
    @FXML
    private MyTreeView treeView2;

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

    // --- New UI Components for Fill Objects Panel ---
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




    // --- Initialization ---
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

    // Initialize the spinner with appropriate value factory for max depth.
    private void initializeMaxDepthSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-1, 100, -1);
        maxDepthSpinner.setValueFactory(valueFactory);
    }

    private void initializeObjects() {
        objectListView1.getItems().addAll(CacheUtil.getInstance().getClasses(true));
        objectListView2.getItems().addAll(CacheUtil.getInstance().getClasses(true));
    }

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

    // Helper method to compare two Integer values safely.
    private boolean newIntEquals(Integer a, int b) {
        return a != null && a.intValue() == b;
    }


    @FXML
    private void onAddIgnoredField(ActionEvent event) {
        String ignoredField = newIgnoredField.getText().trim();
        if (!ignoredField.isEmpty()) {
            addIgnoredFieldToList(ignoredField);
        }
    }

    private void addIgnoredFieldToList(String ignoredField) {
        ignoredFieldsList.getItems().add(ignoredField);
        newIgnoredField.clear();
    }

    @FXML
    private void onRemoveIgnoredField(ActionEvent event) {
        String selectedField = ignoredFieldsList.getSelectionModel().getSelectedItem();
        if (selectedField != null) {
            removeIgnoredFieldFromList(selectedField);
        }
    }

    private void removeIgnoredFieldFromList(String selectedField) {
        ignoredFieldsList.getItems().remove(selectedField);
    }

    // --- Comparison ---


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
     * Formats elapsed time in nanoseconds as ns, µs, ms, or s,
     * depending on magnitude.
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






    @FXML
    private CheckBox useEqualsAfterMaxDepth;

    @FXML
    private CheckBox equivalenceByInheritance;

    @FXML
    private CheckBox compareByElementsAndKeys;
    private EqualLibConfig createConfig() {
        EqualLibConfig config = new EqualLibConfig();
        config.setMaxDepth(maxDepthSpinner.getValue(), useEqualsAfterMaxDepth.isSelected())
                .setCompareByElementsAndKeys(compareByElementsAndKeys.isSelected())
                .equivalenceByInheritance(equivalenceByInheritance.isSelected())
                .setIgnoredFields(ignoredFieldsList.getItems().toArray(new String[0]));
        return config;
    }



    @FXML
    public void onSaveAsButton1Click() throws Exception {
        treeView1.save();
    }

    @FXML
    public void onSaveAsButton2Click() throws Exception {
        treeView2.save();
    }

    @FXML
    public void onLoadButton1Click() {
        treeView1.load();
    }

    @FXML
    public void onLoadButton2Click() {
        treeView2.load();
    }

    @FXML
    public void onLoadButtonClick() {
        treeView1.load();
    }

    // --- Fill Button Handler ---
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

        // Create a Task to perform the fill operation.
        Task<Void> fillTask = new Task<Void>() {
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
