package com.romiiis.equallib_playground.components.listView;

/**
 * SaveResult.java
 * <p>
 * Enum to represent the result of a save operation.
 * <p>
 * This enum is used to represent the result of a save operation. It is used to determine whether to save, discard or cancel the operation.
 *
 * @author Romiiis
 * @version 1.0
 */
public enum SaveResult {
    /**
     * Indicates that the save operation was successful and the changes should be saved.
     */
    SAVE,

    /**
     * Indicates that the save operation was not successful and the changes should be discarded.
     */
    DISCARD,

    /**
     * Indicates that the save operation was cancelled by the user.
     */
    CANCEL
}
