package ru.copperside.core.api;

import ru.copperside.core.domain.Terminal;

public record TerminalResponse(
        Long mercId,
        String mps,
        String gate,
        boolean is3ds,
        String terminalId,
        String merchantId,
        String mcc,
        String name,
        String merchantUrl,
        String login,
        boolean hasPassword,
        String apiUrl,
        String merchantName
) {
    public static TerminalResponse from(Terminal t) {
        return new TerminalResponse(
                t.mercId(), t.mps(), t.gate(), t.is3ds(), t.terminalId(), t.merchantId(),
                t.mcc(), t.name(), t.merchantUrl(), t.login(), t.hasPassword(), t.apiUrl(), t.merchantName());
    }
}
