package com.student.client;

import com.student.common.Protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class FraudClient {

    private static final String SERVER_IP = System.getenv("SERVER_IP");
    private static final int SERVER_PORT = Protocol.Default_PORT;

    public static void main(String[] args) {
        System.out.println("--- FRAUDNET ATM TERMINAL STARTING ---");

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner console = new Scanner(System.in)) {

            // 1. Read Server Welcome Message
            System.out.println("SERVER SAYS: " + in.readLine());

            // 2. Interactive Loop
            while (true) {
                System.out.println("Enter Transaction (Format: UserID;Amount;Currency) OR type 'EXIT':");
                System.out.println("Example: alice;10100;RON");
                System.out.print("> ");

                String input = console.nextLine();
                if ("EXIT".equalsIgnoreCase(input)) break;

                // Wrap in Protocol
                String packet = Protocol.CMD_TXN + Protocol.JOINER + input;
                out.println(packet);

                // Read Response
                String response = in.readLine();
                System.out.println("SERVER RESPONSE: " + response);
            }

        } catch (Exception e) {
            System.err.println("CONNECTION FAILED: Is the Server running?");
        }
    }
}