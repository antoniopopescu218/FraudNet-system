package com.student.server;

import com.student.client.ClientHandler;
import com.student.common.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FraudServer {

    private static final int PORT = Protocol.Default_PORT;

    // ThreadPool recycles threads and prevents resource exhaustion
    private static final ExecutorService pool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws IOException {
        System.out.println(">>> FraudNet HQ Initialized. Listening on port " + PORT);

        if (System.getenv("NEON_DB_URL") == null) {
            System.err.println("FATAL: NEON_DB_URL environment variable not set. Shutdown.");
            System.exit(1); // KILL the server here because it's misconfigured
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                //1. block and wait for connection
                Socket clientSocket = serverSocket.accept();

                //2. log the connection
                System.out.println("[+] New client connected from " + clientSocket.getInetAddress());

                //3. hand off to a wOrker thread
                ClientHandler handler = new ClientHandler(clientSocket);
                pool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println("!!! SERVVER CRITICAL FAILURE: "  + e.getMessage());
        }
    }
}
