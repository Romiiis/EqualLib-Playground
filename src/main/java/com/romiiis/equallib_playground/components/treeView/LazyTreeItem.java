package com.romiiis.equallib_playground.components.treeView;

import javafx.scene.control.TreeItem;

import java.util.List;

/**
 * LazyTreeItem.java
 * <p>
 * Represents a TreeItem that loads its children lazily.
 * This class is used to represent a TreeItem that loads its children lazily. It is used to improve performance when dealing with large trees.
 *
 * @author Romiiis
 * @version 1.0
 */
public class LazyTreeItem extends TreeItem<FieldNode> {
    private boolean childrenLoaded = false;

    /**
     * Constructor for LazyTreeItem.
     *
     * @param fieldNode The FieldNode associated with this TreeItem.
     */
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


    /**
     * Returns the children of this TreeItem.
     *
     * @return The list of children.
     */
    @Override
    public boolean isLeaf() {
        // If not yet loaded, determine if this node can have children
        if (!childrenLoaded) {
            return getValue().getChildren().isEmpty();
        }
        return getChildren().isEmpty();
    }


}
