# 📚 Leelo - Reading and Learning Application

A desktop application developed in JavaFX to improve reading skills and language learning.

## 🚀 Features

- **Text management**: Add and organize texts for reading
- **Word management**: Create word lists for study
- **Interactive practice**: Exercises to improve comprehension
- **SQLite database**: Local data storage
- **Modern interface**: Clean and easy-to-use design

## 📋 Requirements

- **Java 21** or higher
- **Apache Maven 3.6+**

## 🛠️ Installation and Usage

### For Developers

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd leelo
   ```

2. **Compile the project**
   ```bash
   mvn clean package
   ```

3. **Run in development mode**
   ```bash
   mvn javafx:run
   ```

### For End Users

1. **Download the application**
   - Download the `dist/` folder from the project
   - Or request the distribution ZIP file

2. **Run the application**
   - **Windows**: Double-click on `Leelo.bat`
   - **Manual**: `java -jar Leelo.jar`

## 📁 Project Structure

```
leelo/
├── src/
│   ├── main/
│   │   ├── java/com/leelo/
│   │   │   ├── controller/     # JavaFX controllers
│   │   │   ├── dao/           # Data access objects
│   │   │   ├── model/         # Data models
│   │   │   ├── service/       # Business logic
│   │   │   └── App.java       # Main class
│   │   └── resources/
│   │       └── com/leelo/
│   │           ├── *.fxml     # Interface files
│   │           └── icons/     # Application icons
├── dist/
│   ├── Leelo.jar             # Executable JAR
│   └── Leelo.bat             # Windows execution script
├── target/
│   └── leelo-1.0.0-executable.jar  # JAR with dependencies
└── pom.xml                   # Maven configuration
```

## 🔧 Useful Commands

### Development
```bash
# Compile and run
mvn clean javafx:run

# Compile only
mvn clean compile

# Create executable JAR
mvn clean package
```

### Distribution
```bash
# Create JAR for distribution
mvn clean package

# The executable JAR will be at:
# target/leelo-1.0.0-executable.jar
```

## 📦 Distribution

### Distribution Files
- `Leelo.jar`: Executable JAR with all dependencies
- `Leelo.bat`: Script to run on Windows

### End User Requirements
- Java 21 or higher installed
- Windows 10/11 (for the .bat script)

## 🐛 Troubleshooting

### Error: "Java is not installed"
- Install Java 21 from: https://adoptium.net/
- Make sure it's in the system PATH

### Error: "Maven is not installed"
- Install Maven from: https://maven.apache.org/
- Verify installation with: `mvn -version`

### Application won't start
- Verify you have Java 21+: `java -version`
- Run manually: `java -jar Leelo.jar`

## 🎨 Customization

### Change Application Name
Edit in `pom.xml`:
```xml
<app.name>YourName</app.name>
```

### Change Version
Edit in `pom.xml`:
```xml
<version>2.0.0</version>
<app.version>2.0.0</app.version>
```

## 📞 Support

If you encounter problems:
1. Verify that you have Java 21 and Maven installed
2. Run `mvn clean` before trying again
3. Check error logs for more details

## 📄 License

This project is under the [specify license] license.

---

**Developed with ❤️ using JavaFX and Maven** 