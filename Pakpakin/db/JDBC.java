package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBC {

    public static Connection getConnection() throws SQLException {

        String URL = "jdbc:mysql://localhost:3306/pakpakin";
        String username = "root";
        String password = "";

        return DriverManager.getConnection(URL, username, password);
    }
}