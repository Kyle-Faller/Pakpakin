package db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static boolean tableReady, warned;

    public record Player(int id, String name, int highScore) {

        public boolean isSaved() {
            return id >= 0;
        }
    }

    private Database() {
    }

    public static Player loginOrCreate(String name) {

        try (Connection c = open()) {

            Player existing = find(c, name, -1);

            if (existing != null) {
                return existing;
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO players (name, highest_score) VALUES (?, 0)",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {

                    if (keys.next()) {
                        return new Player(
                                keys.getInt(1),
                                name,
                                0
                        );
                    }
                }
            }

        } catch (SQLException e) {
            report(e);
        }

        return new Player(-1, name, 0);
    }

  public static Player rename(Player current, String newName) {

    newName = newName.trim();

    if (newName.isBlank()) {
        return current;
    }

    if (newName.equals(current.name())) {
        return current;
    }

    if (!current.isSaved()) {
        System.out.println("[Database] Player is not saved. Cannot rename.");
        return current;
    }

    try (Connection c = open()) {

        
        Player taken = find(c, newName, current.id());

        if (taken != null) {
            System.out.println("[Database] Name already exists: " + newName);
            return current;
        }

        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE players SET name = ? WHERE PK_number_id = ?")) {

            ps.setString(1, newName);
            ps.setInt(2, current.id());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println(
                    "[Database] Renamed " +
                    current.name() + " -> " + newName
                );

                return new Player(
                    current.id(),
                    newName,
                    current.highScore()
                );
            }
        }

    } catch (SQLException e) {
        report(e);
    }

    return current;
}

    public static Player submitScore(Player p, int score) {

        int best = Math.max(p.highScore(), score);

        if (p.isSaved() && score > p.highScore()) {

            try (
                Connection c = open();
                PreparedStatement ps = c.prepareStatement(
                    "UPDATE players SET highest_score = ? " +
"WHERE PK_number_id = ? AND highest_score < ?"
                )
            ) {

                ps.setInt(1, score);
                ps.setInt(2, p.id());
                ps.setInt(3, score);

                ps.executeUpdate();

            } catch (SQLException e) {
                report(e);
            }
        }

        return new Player(
                p.id(),
                p.name(),
                best
        );
    }
public static Player findPlayer(String name) {
    try (Connection c = open()) {
        Player p = find(c, name.trim(), -1);

        System.out.println("[Database] Searching for: [" + name + "]");

        if (p != null) {
            System.out.println("[Database] FOUND: " + p.name()
                    + " | ID: " + p.id()
                    + " | Score: " + p.highScore());
        } else {
            System.out.println("[Database] NOT FOUND: [" + name + "]");
        }

        return p;

    } catch (SQLException e) {
        report(e);
        return null;
    }
}
    private static Player find(
            Connection c,
            String name,
            int excludeId) throws SQLException {

        try (PreparedStatement ps = c.prepareStatement(
                "SELECT PK_number_id, name, highest_score " +
"FROM players " +
"WHERE name = ? AND PK_number_id <> ?")) {

            ps.setString(1, name);
            ps.setInt(2, excludeId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    return new Player(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getInt(3)
                    );
                }

                return null;
            }
        }
    }

    private static Connection open() throws SQLException {

        Connection c = JDBC.getConnection();

        ensureTable(c);

        return c;
    }

    private static synchronized void ensureTable(
            Connection c) throws SQLException {

        if (tableReady) {
            return;
        }

        try (Statement st = c.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS players (" +
               "PK_number_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(30) NOT NULL UNIQUE, " +
                "highest_score INT NOT NULL DEFAULT 0)"
            );
        }

        tableReady = true;
    }

    private static void report(SQLException e) {

        System.err.println("[Database] " + e.getMessage());

        if (!warned) {

            warned = true;

            System.err.println(
                "[Database] The game keeps working, but scores and names " +
                "are not saved. Check that MySQL is running, that " +
                "JDBC.java connects to the pakpakin database, and that " +
                "the MySQL JDBC driver (mysql-connector-j) is on the classpath."
            );
        }
    }
}