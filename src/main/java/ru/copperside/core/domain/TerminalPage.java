package ru.copperside.core.domain;

import java.util.List;

public record TerminalPage(List<Terminal> lines, long total) {
}
