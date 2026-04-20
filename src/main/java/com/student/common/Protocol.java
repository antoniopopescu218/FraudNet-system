package com.student.common;

public class Protocol {
    // Commands
    public static final String CMD_TXN = "TXN"; // Transaction Request
    public static final String CMD_ACK = "ACK"; // Acknowledgment

    // Delimiter
    public static final String SEPARATOR = "\\|"; // Escaped for regex split
    public static final String JOINER = "|";

    // Responses
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_FLAGGED = "FLAGGED";

    // Port Host
    public static final int Default_PORT = 500;
}