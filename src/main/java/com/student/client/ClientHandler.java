package com.student.client;

import com.student.common.Protocol;
import com.student.server.CurrencyService;
import com.student.server.DatabaseService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CurrencyService currencyService; // Add the service
    private final DatabaseService dbService;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.currencyService = new CurrencyService(); // Initialize it
        this.dbService = new DatabaseService();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("AUTH|Connected to FraudNet Node 1");

            String inputLine;
            while ((inputLine = in.readLine()) != null) {

                // PARSING LOGIC (The "Protocol" Requirement)
                String[] parts = inputLine.split(Protocol.SEPARATOR);
                String command = parts[0];

                if (Protocol.CMD_TXN.equals(command) && parts.length == 2) {
                    // Payload format: user_id;amount;currency
                    String payload = parts[1];
                    processTransaction(payload, out);
                } else {
                    out.println("ERR|Invalid Protocol Format");
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Connection closed.");
        }
    }

    private void processTransaction(String payload, PrintWriter out) {
        try {
            String[] data = payload.split(";");
            String userId = data[0];
            double amount = Double.parseDouble(data[1]);
            String currency = data[2];

            // 1. CALL WEB SERVICE
            double rate = currencyService.getEuroRate(currency);
            double amountInEur = amount * rate;

            // 2. APPLY FRAUD RULES (Business Logic)
            String status = Protocol.STATUS_APPROVED;
            String message = "Transaction Clear";

            if (amountInEur > 2000) {
                status = Protocol.STATUS_FLAGGED;
                message = "High Value (" + String.format("%.2f", amountInEur) + " EUR)";
            }
            if (rate == 0.0) {
                status = "ERROR";
                message = "Currency Unknown";
            }

            // 3. SEND RESPONSE
            System.out.println(">>> ANALYZED: " + userId + " | " + amount + " " + currency + " -> " + status);
            out.println(Protocol.CMD_ACK + Protocol.JOINER + status + ";" + message);

            // 4. SAVE TO CLOUD
            dbService.saveTransaction(userId, amount, currency, status);

        } catch (Exception e) {
            out.println("ERR|Processing Failed");
        }
    }
}