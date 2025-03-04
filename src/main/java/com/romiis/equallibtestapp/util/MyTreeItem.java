package com.romiis.equallibtestapp.util;

import javafx.scene.control.TreeItem;

public class MyTreeItem extends TreeItem<String> {
    private final Object reference;

    public MyTreeItem(String value, Object reference) {
        super(value);
        this.reference = reference;
    }

    public Object getReference() {
        return reference;
    }

}
