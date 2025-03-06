package com.romiis.equallibtestapp.util;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SelectedClass {

    @Setter
    private boolean modified = false;

    private final Class<?> selectedClass;

    public SelectedClass(Class<?> selected) {
        this.selectedClass = selected;
    }
}
