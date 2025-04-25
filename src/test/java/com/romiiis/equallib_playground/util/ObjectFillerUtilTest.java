package com.romiiis.equallib_playground.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ObjectFillerUtilTest {

    class PublicClass {
        public int a;
        public String b;
        public float[] c;
        public ArrayList<Integer> d;
    }



    @Test
    void testFillObject() {
        PublicClass obj1 = new PublicClass();
        PublicClass obj2 = new PublicClass();

        ObjectFillerUtil.fillObjects(obj1, obj2, true);

        assertEquals(obj1.a, obj2.a);
        assertEquals(obj1.b, obj2.b);
        assertArrayEquals(obj1.c, obj2.c);
        assertEquals(obj1.d, obj2.d);

    }


    @Test
    void testFillObjectWithNull() {
        PublicClass obj1 = new PublicClass();
        PublicClass obj2 = new PublicClass();

        ObjectFillerUtil.fillObjects(obj1, obj2, false);

        assertNotEquals(obj1.a, obj2.a);
        assertNotEquals(obj1.b, obj2.b);
        assertNotEquals(obj1.c, obj2.c);
        assertNotEquals(obj1.d, obj2.d);

    }

}