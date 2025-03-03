package com.romiis.equallibtestapp.controllers;

import com.romiis.equallibtestapp.MainClass;
import com.romiis.equallibtestapp.util.DynamicCompiler;
import com.romiis.equallibtestapp.util.ReflectionUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;

import static com.romiis.equallibtestapp.util.ReflectionUtil.getFieldValue;

public class ObjectSelectorController {

    // ListView of all objects available
    @FXML
    private ListView<String> objectListView;

    // TreeView to show structure of object
    @FXML
    private TreeView<String> treeView;

    // Stage of the dialog
    private Stage dialogStage;

    // Main TreeView from MainSceneController
    private TreeView<String> mainTreeView;


    /**
     * Initialize the dialog
     * @param dialogStage Stage of the dialog
     * @param treeView TreeView to show structure of object
     */
    public void init(Stage dialogStage, TreeView<String> treeView) throws IOException, ClassNotFoundException {




        this.dialogStage = dialogStage;
        this.mainTreeView = treeView;

        objectListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Akce na základě nově vybraného objektu
            handleSelectionChange(newValue);
        });





    }


    // Handler pro změnu výběru objektu
    private void handleSelectionChange(String selectedObject) {
        try {
            // Načteme třídu podle názvu objektu
            Class<?> clazz = DynamicCompiler.loadClass(selectedObject);
            // Načteme všechna pole dané třídy
            Field[] fields = clazz.getDeclaredFields();

            // Vytvoříme kořenový TreeItem pro TreeView
            TreeItem<String> rootItem = new TreeItem<>(selectedObject);
            treeView.setRoot(rootItem);

            // Přidáme pole do TreeView
            for (Field field : fields) {
                // Zakážeme přístup do privátních polí (pro jednoduchost)
                field.setAccessible(true);

                // Pokud je pole primitivní typ nebo String
                if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                    rootItem.getChildren().add(new TreeItem<>(field.getName() + " : " + field.getType().getSimpleName()));
                } else if (field.getType().isEnum()) {
                    // Pokud je pole typu enum, přidáme hodnoty enumu
                    TreeItem<String> fieldItem = new TreeItem<>(field.getName() + " : Enum");
                    rootItem.getChildren().add(fieldItem);

                    // Získání hodnot enumu a přidání do TreeView
                    Object[] enumConstants = field.getType().getEnumConstants();
                    for (Object constant : enumConstants) {
                        fieldItem.getChildren().add(new TreeItem<>(constant.toString()));
                    }
                } else {
                    // Pokud je pole typu objekt, rekurzivně jej zpracujeme
                    TreeItem<String> fieldItem = new TreeItem<>(field.getName() + " : " + field.getType().getSimpleName());
                    rootItem.getChildren().add(fieldItem);
                    // Rekurzivní volání pro pole, které je objektem
                    handleNestedObject(field.getType(), fieldItem);
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Rekurzivní metoda pro zpracování objektu a jeho polí
    private void handleNestedObject(Class<?> clazz, TreeItem<String> parentItem) {
        // Získání všech polí pro tento objekt
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            // Pokud je pole primitivní typ nebo String, přidáme ho přímo
            if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                parentItem.getChildren().add(new TreeItem<>(field.getName() + " : " + field.getType().getSimpleName()));
            } else if (field.getType().isEnum()) {
                // Pokud je to enum, přidáme hodnoty enumu
                TreeItem<String> enumItem = new TreeItem<>(field.getName() + " : Enum");
                parentItem.getChildren().add(enumItem);

                // Získání hodnot enumu a přidání do TreeView
                Object[] enumConstants = field.getType().getEnumConstants();
                for (Object constant : enumConstants) {
                    enumItem.getChildren().add(new TreeItem<>(constant.toString()));
                }
            } else {
                // Pokud je to další objekt, vytvoříme nový item pro TreeView a zpracujeme ho rekurzivně
                TreeItem<String> nestedItem = new TreeItem<>(field.getName() + " : " + field.getType().getSimpleName());
                parentItem.getChildren().add(nestedItem);
                handleNestedObject(field.getType(), nestedItem); // Rekurzivní volání pro další úroveň
            }
        }
    }


    @FXML
    private void onSelectButtonClick() {
        // Selected object
        String selectedObject = objectListView.getSelectionModel().getSelectedItem();
        if (selectedObject != null) {
            MainSceneController mainController = new MainSceneController();
            mainController.addObjectToTreeView(selectedObject, mainTreeView);
            dialogStage.close();
        }
    }


    /**
     * Close the dialog without selecting an object
     */
    @FXML
    private void onCancelButtonClick() {
        dialogStage.close(); // Zavření okna bez výběru
    }
}