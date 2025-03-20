package com.romiis.equallibtestapp.util;

import static org.junit.jupiter.api.Assertions.*;

import com.romiis.equallibtestapp.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.*;

public class JsonUtilComplexStructTest {

    // A simple nested class representing a person.
    public static class Person {
        public String name;
        public int age;

        public Person() {} // Default constructor for deserialization

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age &&
                    Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    // A complex structure that holds an array, a collection, a map, and nested objects.
    public static class ComplexStruct {
        public String[] names;
        public List<Integer> numbers;
        public Map<String, Double> ratios;
        public Person person;
        public List<Person> people;

        public ComplexStruct() {} // Default constructor for deserialization

        public ComplexStruct(String[] names, List<Integer> numbers, Map<String, Double> ratios,
                             Person person, List<Person> people) {
            this.names = names;
            this.numbers = numbers;
            this.ratios = ratios;
            this.person = person;
            this.people = people;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComplexStruct that = (ComplexStruct) o;
            return Arrays.equals(names, that.names) &&
                    Objects.equals(numbers, that.numbers) &&
                    Objects.equals(ratios, that.ratios) &&
                    Objects.equals(person, that.person) &&
                    Objects.equals(people, that.people);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(numbers, ratios, person, people);
            result = 31 * result + Arrays.hashCode(names);
            return result;
        }
    }

    public static class Cyclic {
        public Cyclic next;

        public Cyclic() {} // Default constructor for deserialization

        public Cyclic(Cyclic next) {
            this.next = next;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Cyclic cyclic = (Cyclic) o;
            return Objects.equals(next, cyclic.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(next);
        }
    }

    @Test
    public void testSerializeDeserializeComplexStruct() throws Exception {
        // Create the components of our complex structure.
        String[] names = {"Alice", "Bob", "Charlie"};
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        Map<String, Double> ratios = new HashMap<>();
        ratios.put("a", 0.1);
        ratios.put("b", 0.2);
        Person person = new Person("David", 40);
        List<Person> people = new ArrayList<>();
        people.add(new Person("Eve", 25));
        people.add(new Person("Frank", 30));

        // Create a complex object that includes arrays, collections, maps, and nested objects.
        ComplexStruct original = new ComplexStruct(names, numbers, ratios, person, people);

        // Serialize the complex structure.
        String json = JsonUtil.serialize(original);
        assertNotNull(json, "Serialized JSON should not be null");
        System.out.println("Serialized JSON: " + json);

        // Deserialize the JSON back into an object.
        Object deserialized = JsonUtil.deserialize(json);
        assertNotNull(deserialized, "Deserialized object should not be null");
        assertTrue(deserialized instanceof ComplexStruct, "Deserialized object should be an instance of ComplexStruct");

        ComplexStruct result = (ComplexStruct) deserialized;
        // Verify that the deserialized object equals the original.
        assertEquals(original, result, "Deserialized complex struct should equal the original");
    }

    @Test
    public void testSerializeDeserializeCyclic() throws Exception {
        // Create a cyclic structure.
        Cyclic node1 = new Cyclic();
        Cyclic node2 = new Cyclic(node1);
        node1.next = node2;

        // Serialize the cyclic structure.
        String json = JsonUtil.serialize(node1);
        assertNotNull(json, "Serialized JSON should not be null");
        System.out.println("Serialized JSON: " + json);

        // Deserialize the JSON back into an object.
        Object deserialized = JsonUtil.deserialize(json);
        assertNotNull(deserialized, "Deserialized object should not be null");
        assertTrue(deserialized instanceof Cyclic, "Deserialized object should be an instance of Cyclic");

        Cyclic result = (Cyclic) deserialized;
        // Check that the cycle is preserved: result.next.next should be the same as result.
        assertNotNull(result.next, "Deserialized result.next should not be null");
        assertNotNull(result.next.next, "Deserialized result.next.next should not be null");
        assertSame(result, result.next.next, "Cycle is not preserved after deserialization");
    }
}

