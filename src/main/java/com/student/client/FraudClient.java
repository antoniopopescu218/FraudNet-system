package com.student.client;

import com.student.common.Protocol;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

public class FraudClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    // Usage: FraudClient [host] [port]
    // host: 1st argument, else the SERVER_IP environment variable, else localhost
    // port: 2nd argument, else the protocol default (5000)
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : System.getenv().getOrDefault("SERVER_IP", "localhost");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Protocol.Default_PORT;

        System.out.println("--- FRAUDNET ATM TERMINAL STARTING ---");
        System.out.println("Connecting to " + host + ":" + port + " ...");

        try (Socket socket = new Socket()) {
            // Fail after 5s instead of hanging if the server is down or a firewall drops the traffic
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 Scanner console = new Scanner(System.in)) {

                // 1. Read Server Welcome Message
                System.out.println("SERVER SAYS: " + in.readLine());

                // 2. Interactive Loop
                while (true) {
                    System.out.println("Enter Transaction (Format: UserID;Amount;Currency) OR type 'EXIT':");
                    System.out.println("Example: alice;10100;RON");
                    System.out.print("> ");

                    if (!console.hasNextLine()) break;
                    String input = console.nextLine();
                    if ("EXIT".equalsIgnoreCase(input)) break;

                    // Wrap in Protocol
                    String packet = Protocol.CMD_TXN + Protocol.JOINER + input;
                    out.println(packet);

                    // Read Response
                    String response = in.readLine();
                    if (response == null) {
                        System.err.println("Server closed the connection.");
                        break;
                    }
                    System.out.println("SERVER RESPONSE: " + response);
                }
            }

        } catch (Exception e) {
            System.err.println("CONNECTION FAILED (" + host + ":" + port + "): " + e.getMessage());
            System.err.println("Check that the server is running and that port " + port + " is reachable.");
        }
    }
}
