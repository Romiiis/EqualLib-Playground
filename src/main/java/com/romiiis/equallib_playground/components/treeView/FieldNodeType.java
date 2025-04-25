package com.romiiis.equallib_playground.components.treeView;

/**
 * FieldNodeType.java
 * <p>
 * Enum to represent the type of a field node in the object tree.
 * <p>
 * This enum is used to represent the type of a field node in the object tree. It is used to determine how to handle the field node.
 *
 * @author Romiiis
 * @version 1.0
 */
public enum FieldNodeType {
    /**
     * Indicates that the field node is editable.
     */
    EDITABLE,

    /**
     * Indicates that the field node is not editable.
     */
    NON_EDITABLE,

    /**
     * Indicates that the field node is an array and is editable.
     */
    ARRAY_EDITABLE,

    /**
     * Indicates that the field node is an array and is not editable.
     */
    INFO
}
