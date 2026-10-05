# Pakpakin 🐦

**Pakpakin** is a Java Swing-based Flappy Bird-style game where players control a bird and try to achieve the highest score by avoiding obstacles.

## 🎮 How to Run

### 1. Install the Requirements

Make sure you have:

- Java JDK installed
- VS Code or another Java IDE
- XAMPP
- MySQL Connector/J

### 2. Start XAMPP

Open **XAMPP Control Panel** and start:

- **Apache**
- **MySQL**

MySQL must be running because Pakpakin uses a database to store game information such as player names and scores.

### 3. Import the Database

The database file is located inside the project's `db` package.

Extract/import the database into MySQL before running the game.

You can import the `.sql` database file using **phpMyAdmin**:

1. Open XAMPP.
2. Start **Apache** and **MySQL**.
3. Open phpMyAdmin.
4. Create or select the `pakpakin` database.
5. Go to the **Import** tab.
6. Select the SQL file from the `db` package.
7. Click **Import**.

### 4. Open the Project

Open the Pakpakin project folder in VS Code.

Make sure the project contains the required packages and files, including:

```text
Pakpakin/
├── src/
│   ├── db/
│   ├── difficulty/
│   ├── ui/
│   └── Main.java
├── image/
├── ...
└── README.md
```

### 5. Run the Game

Run:

```text
Main.java
```

`Main.java` is the **main entry point** of the game.

You can run it directly from VS Code using the **Run** button or Java extension.

## 🗄️ Database

Pakpakin uses **MySQL** to store player/game data.

Before running the game, make sure:

- XAMPP is running.
- MySQL is started.
- The `pakpakin` database has been imported.
- The MySQL Connector/J driver is included in the project.
- The database connection settings in the `db` package are correct.

## 🕹️ Game Features

- Flappy Bird-style gameplay
- Multiple difficulty levels
- Player name entry
- Score tracking
- High-score/database system
- Custom pixel-art graphics
- Sound effects and background music
- Java Swing user interface

## 🛠️ Technologies Used

- **Java**
- **Java Swing**
- **MySQL**
- **XAMPP**
- **JDBC**
- **VS Code**

## 📌 Important

Do not run the difficulty classes directly.

Start the game through:

```text
Main.java
```

Make sure **MySQL is running in XAMPP before launching the game**, otherwise the game may not be able to connect to the database.

## 👥 Project

**Pakpakin**  
A Java-based Flappy Bird-style game project.
