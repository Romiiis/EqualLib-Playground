package com.romiis.equallibtestapp.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class JsonUtilObjectsTest {

    // A simple nested class for testing.
    public static class Address {
        public String street;
        public String city;

        public Address() {
        }

        public Address(String street, String city) {
            this.street = street;
            this.city = city;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Address that = (Address) obj;
            return (street == null ? that.street == null : street.equals(that.street))
                    && (city == null ? that.city == null : city.equals(that.city));
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(street, city);
        }
    }

    // A simple POJO with a nested Address.
    public static class Person {
        public String name;
        public int age;
        public Address address;

        public Person() {
        }

        public Person(String name, int age, Address address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Person that = (Person) obj;
            return age == that.age
                    && (name == null ? that.name == null : name.equals(that.name))
                    && (address == null ? that.address == null : address.equals(that.address));
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age, address);
        }
    }

    @Test
    public void testSerializeDeserializePerson() throws Exception {
        // Create an instance of Person with nested Address.
        Person original = new Person("Alice", 30, new Address("123 Main St", "Metropolis"));

        // Serialize the object into JSON.
        String json = JsonUtil.serialize(original);
        assertNotNull(json, "Serialized JSON should not be null");
        System.out.println("Serialized JSON: " + json);

        // Deserialize the JSON back into an object.
        Object deserializedObj = JsonUtil.deserialize(json);
        assertNotNull(deserializedObj, "Deserialized object should not be null");
        assertTrue(deserializedObj instanceof Person, "Deserialized object should be instance of Person");

        Person deserialized = (Person) deserializedObj;
        // Verify that the deserialized object is equal to the original.
        assertEquals(original, deserialized, "Deserialized Person should equal the original Person");
    }
}
