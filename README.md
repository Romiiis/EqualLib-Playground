<h1 align="center">
  <img src="src/main/resources/EqualLib-PlaygroundIcon-noBgr.png" alt="EqualLib Playground Icon" width="75"><br>
  EqualLib Playground
</h1>
<p align="center">A visual JavaFX-based playground for testing and exploring deep object comparison with EqualLib</p>

---

## 🧠 Overview

EqualLib Playground is a desktop JavaFX application designed to test and visualize the behavior of the EqualLib library. Users can load or edit complex object structures through a graphical interface and verify their equality with fine-tuned settings.

---

## 🚀 Features

- Interactive editing of Java object trees
- Supports class loading from source files
- Detailed configuration of comparison depth, ignored fields, and collection behavior
- Cross-platform support (Windows & Linux)

---

## 📦 Installation & Setup

### Requirements
- Java 17 or higher (JDK not needed thanks to bundled runtime)
- Maven for building from source

### 1. Download or Clone

Download a pre-built ZIP or clone the repository:
```bash
git clone https://github.com/Romiiis/EqualLib-Playground
cd EqualLib-Playground
```

### 2. Build with Maven

Make sure [EqualLib](https://github.com/Romiiis/EqualLib) is installed locally and that your `pom.xml` references the correct version of the library. Here is an example dependency block:

```xml
<dependencies>
  <dependency>
    <groupId>com.Romiiis</groupId>
    <artifactId>EqualLib</artifactId>
    <version>1.0.0</version> <!-- Replace with the correct version -->
  </dependency>
</dependencies>
```

## 3. Build the Application

Then package the app:
```bash
mvn clean package
```

This produces a `.zip` distribution in the `target/` directory.

---

## ▶️ Running the Application

### Windows

Unzip the package and double-click `EqualLib-Playground.bat` inside the `bin/` folder.

### Linux

Give the script execution rights and run:
```bash
chmod +x EqualLib-Playground.sh
./EqualLib-Playground.sh
```

---

## 🧪 Application Guide

### Main Features
- **Class Selector Panel**: Choose or add your own Java classes
- **Object Hierarchy View**: Inspect and edit object structures interactively
- **Comparison Settings Panel**: Configure EqualLib (max depth, ignore fields, etc.)
- **Object Filler Panel**: Define how objects are auto-filled for testing
- **Compare & Test Buttons**: Compare two objects or run predefined test suite

---

## 📂 Adding Custom Classes

Place your `.java` files in the `bin/classes/` directory. They will be compiled at runtime and added to the selection panel.

### Rules:
- Classes must have a public no-argument constructor
- Collections/fields must be initialized or declared properly
- All referenced Java packages must be imported

---

## ⚠️ Notes & Limitations

- The app assumes advanced users — invalid configs may break runtime
- Complex object trees may cause performance or memory issues
- Only valid `.java` files that compile are loaded

---

## 📖 License & Contribution

Open-source under the MIT License. Feedback and contributions are welcome!

---

## 📚 Resources

- [Playground Repository](https://github.com/Romiiis/EqualLib-Playground)
- [EqualLib Library](https://github.com/Romiiis/EqualLib)

