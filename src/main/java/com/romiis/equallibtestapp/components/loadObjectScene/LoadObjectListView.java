package com.romiis.equallibtestapp.components.loadObjectScene;

import com.romiis.equallibtestapp.CacheUtil;
import com.romiis.equallibtestapp.components.common.MyTreeView;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import lombok.Setter;

@Setter
public class LoadObjectListView extends ListView<String> {


    private MyTreeView assignedTreeView;

    /**
     * Create a new MyListView instance
     */
    public LoadObjectListView() {
        super();
        initializeClickHandler();

        setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item + " (" + CacheUtil.getInstance().getObjectClass(item).getSimpleName() + ")");
                }
            }
        });
    }


    private void initializeClickHandler() {
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            assignedTreeView.setSelectedObject(CacheUtil.getInstance().getObjectByName(newValue, false));
        });
    }


    public void filterByClass(Class<?> clazz) {
        getItems().clear();
        if (clazz == null) {
            getItems().addAll(CacheUtil.getInstance().getAllObjects(null));
        } else {
            getItems().addAll(CacheUtil.getInstance().getAllObjects(clazz));
        }

        assignedTreeView.clear();
    }
}
