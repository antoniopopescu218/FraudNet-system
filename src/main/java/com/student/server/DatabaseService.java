package com.student.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseService {

    // Read the connection string from the environment variable
    private final String dbUrl = System.getenv("NEON_DB_URL");

    public void saveTransaction(String userId, double amount, String currency, String status) {
        
        // Quick safety check if variable exists
        if(dbUrl == null || dbUrl.trim().isEmpty()){
          System.err.println("[DB FAILURE] CRITICAL: 'NEON_DB_URL' environment variable is missing");
          System.err.println("Please set it using: export DB_URL=\"jdbc:postgresql://...\"");
          return;
        }
        
        String sql = "INSERT INTO transactions(user_id, amount, currency, status) VALUES(?,?,?,?)";

        // Connect using just the URL (NEON bundes user/pass inside it)
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, currency);
            pstmt.setString(4, status);

            pstmt.executeUpdate();
            System.out.println("[DB] Transaction persisted to Cloud.");

        } catch (SQLException e) {
            System.err.println("[DB FAILURE] Could not log to cloud: " + e.getMessage());
            // Fail-open: We don't crash the app if DB is down, we just log the error.
        }
    }
}