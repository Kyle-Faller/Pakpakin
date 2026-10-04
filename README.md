# Pakpakin

## How to Run

1. Make sure **MySQL is running in XAMPP**.
2. Make sure the **`pakpakin` database** exists in MySQL.
3. Make sure the required **MySQL Connector/J** driver is included in the project.
4. Open the project in VS Code.
5. Run **`Main.java`**.

### Main Entry Point

The game starts from:

```text
Main.java
```

You **do not need to run `Name.java`, `Easy.java`, `Medium.java`, `Hard.java`, or the other screens separately**.

`Main.java` connects the game's screens together:

```text
Main.java
   ↓
Name
   ↓
Loading Screen
   ↓
Main Menu
   ↓
Easy / Medium / Hard
   ↓
Victory / Defeat
   ↓
Main Menu
```

### Database

Pakpakin uses MySQL to store the player's:

- Name
- Highest score

The database connection is handled by:

```text
db/JDBC.java
db/Database.java
```

The database is:

```text
pakpakin
```

The table is:

```text
players
```

with the following columns:

```text
PK_number_id
name
highest_score
```

### Starting the Game

Simply run:

```text
Main.java
```

The game will open with the **Name screen**. Enter a name and press **ENTER** to continue.
