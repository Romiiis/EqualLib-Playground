package com.romiis.equallibtestapp.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeepCopyUtilTest {


    class Person {
        String name;
        int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void testDeepCopy() {
        DeepCopyUtil deepCopyUtil = new DeepCopyUtil();
        Person person = new Person("John", 30);
        Person copiedPerson = deepCopyUtil.deepCopy(person);
        assertNotSame(person, copiedPerson);
        assertEquals(person.name, copiedPerson.name);
        assertEquals(person.age, copiedPerson.age);
    }


    @Test
    void testDeepCopyWithNull() {
        DeepCopyUtil deepCopyUtil = new DeepCopyUtil();
        Person copiedPerson = deepCopyUtil.deepCopy(null);
        assertNull(copiedPerson);
    }

    @Test
    void testDeepCopyWithArray() {
        DeepCopyUtil deepCopyUtil = new DeepCopyUtil();
        Person[] people = new Person[] {new Person("John", 30), new Person("Alice", 25)};
        Person[] copiedPeople = deepCopyUtil.deepCopy(people);
        assertNotSame(people, copiedPeople);
        assertNotSame(people[0], copiedPeople[0]);
        assertNotSame(people[1], copiedPeople[1]);
        assertEquals(people[0].name, copiedPeople[0].name);
        assertEquals(people[0].age, copiedPeople[0].age);
        assertEquals(people[1].name, copiedPeople[1].name);
        assertEquals(people[1].age, copiedPeople[1].age);
    }

    @Test
    void testDeepCopyCollection() {
        DeepCopyUtil deepCopyUtil = new DeepCopyUtil();
        Person person1 = new Person("John", 30);
        Person person2 = new Person("Alice", 25);
        List<Person> people = List.of(person1, person2);
        List<Person> copiedPeople = deepCopyUtil.deepCopy(people);
        assertNotSame(people, copiedPeople);
        assertNotSame(people.get(0), copiedPeople.get(0));
        assertNotSame(people.get(1), copiedPeople.get(1));
        assertEquals(people.get(0).name, copiedPeople.get(0).name);
        assertEquals(people.get(0).age, copiedPeople.get(0).age);
        assertEquals(people.get(1).name, copiedPeople.get(1).name);
        assertEquals(people.get(1).age, copiedPeople.get(1).age);
    }






}