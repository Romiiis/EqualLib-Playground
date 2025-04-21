package com.romiis.equallib_playground.components.treeView;

import javafx.scene.control.TreeItem;

import java.util.List;

public class LazyTreeItem extends TreeItem<FieldNode> {
    private boolean childrenLoaded = false;

    public LazyTreeItem(FieldNode fieldNode) {
        super(fieldNode);
        // Instead of an event handler that requires casting, add a listener to the expandedProperty.
        this.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && !childrenLoaded) {
                loadChildren();
            }
        });
    }

    /**
     * Loads children from the underlying FieldNode.
     */
    private void loadChildren() {
        List<FieldNode> childNodes = getValue().getChildren();
        for (FieldNode child : childNodes) {
            getChildren().add(new LazyTreeItem(child));
        }
        childrenLoaded = true;
    }

    @Override
    public boolean isLeaf() {
        // If not yet loaded, determine if this node can have children
        if (!childrenLoaded) {
            return getValue().getChildren().isEmpty();
        }
        return getChildren().isEmpty();
    }


}
