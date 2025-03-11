package com.romiis.equallibtestapp.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FinalFieldUpdater {
    private static final Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access Unsafe", e);
        }
    }

    public static void setFinalField(Object target, Field field, Object newValue) {
        try {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) {
                // For static fields, use Unsafe to get the base and offset, then update
                Object staticFieldBase = unsafe.staticFieldBase(field);
                long offset = unsafe.staticFieldOffset(field);
                unsafe.putObject(staticFieldBase, offset, newValue);
            } else {
                // For non-static fields, remove the final modifier and update normally.
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
                field.set(target, newValue);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
