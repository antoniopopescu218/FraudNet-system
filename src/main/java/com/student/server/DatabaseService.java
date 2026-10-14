package com.student.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DatabaseService {

    // Read the connection string from the environment variable
    private static final String DB_URL = System.getenv("NEON_DB_URL");

    private static final String SQL =
            "INSERT INTO transactions(user_id, amount, currency, status) VALUES(?,?,?,?)";

    // Asynchronous audit writer: ONE background thread with a BOUNDED queue.
    // - Client handler threads hand off the write and return immediately, so a slow or
    //   hanging database can never block transaction processing.
    // - A single writer keeps log entries in arrival order and uses one DB connection at a time.
    // - Bounded queue: during a long DB outage, entries beyond 1000 are dropped and logged
    //   (fail-open) instead of piling up until the JVM runs out of memory.
    private static final ThreadPoolExecutor AUDIT_WRITER = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1000),
            runnable -> {
                Thread t = new Thread(runnable, "audit-writer");
                t.setDaemon(true);
                return t;
            },
            (task, executor) -> System.err.println("[AUDIT] Queue full or shutting down - log entry dropped (fail-open)")
    );

    /** Queues the audit write and returns immediately. */
    public void saveTransactionAsync(String userId, double amount, String currency, String status) {
        AUDIT_WRITER.execute(() -> {
            try {
                saveTransaction(userId, amount, currency, status);
            } catch (RuntimeException e) {
                System.err.println("[AUDIT] Write failed (fail-open): " + e.getMessage());
            }
        });
    }

    /** Called on server shutdown: stop accepting writes and flush what is already queued. */
    public static void shutdown() {
        AUDIT_WRITER.shutdown();
        try {
            if (!AUDIT_WRITER.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("[AUDIT] Shutdown timeout - " + AUDIT_WRITER.getQueue().size() + " pending entries lost");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Runs on the audit-writer thread only.
    private void saveTransaction(String userId, double amount, String currency, String status) {

        // Quick safety check if variable exists
        if (DB_URL == null || DB_URL.trim().isEmpty()) {
            System.err.println("[DB FAILURE] CRITICAL: 'NEON_DB_URL' environment variable is missing");
            System.err.println("Please set it using: export NEON_DB_URL=\"jdbc:postgresql://...\"");
            return;
        }

        // Default timeouts (seconds) so an unreachable or hanging DB fails fast instead of
        // stalling the writer. Parameters set in the URL itself take precedence over these.
        Properties props = new Properties();
        props.setProperty("connectTimeout", "5");
        props.setProperty("socketTimeout", "10");

        // Connect using the URL (Neon bundles user/pass inside it)
        try (Connection conn = DriverManager.getConnection(DB_URL, props);
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

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
