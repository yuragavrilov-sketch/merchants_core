package ru.copperside.core.domain;

import java.util.Locale;

public enum TerminalSortField {
    MERC_ID,
    MPS,
    GATE,
    TERMINAL_ID,
    MCC;

    public static TerminalSortField from(String value) {
        if (value == null || value.isBlank()) {
            return MERC_ID;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mercid", "merc_id", "id" -> MERC_ID;
            case "mps" -> MPS;
            case "gate" -> GATE;
            case "terminalid", "terminal_id" -> TERMINAL_ID;
            case "mcc" -> MCC;
            default -> throw new IllegalArgumentException("Unsupported sortBy for terminals: " + value);
        };
    }
}
