package ru.copperside.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TerminalSortFieldTest {

    @Test
    void defaultsToMercIdWhenBlank() {
        assertThat(TerminalSortField.from(null)).isEqualTo(TerminalSortField.MERC_ID);
        assertThat(TerminalSortField.from("")).isEqualTo(TerminalSortField.MERC_ID);
        assertThat(TerminalSortField.from("id")).isEqualTo(TerminalSortField.MERC_ID);
    }

    @Test
    void mapsKnownAliases() {
        assertThat(TerminalSortField.from("mercid")).isEqualTo(TerminalSortField.MERC_ID);
        assertThat(TerminalSortField.from("merc_id")).isEqualTo(TerminalSortField.MERC_ID);
        assertThat(TerminalSortField.from("mps")).isEqualTo(TerminalSortField.MPS);
        assertThat(TerminalSortField.from("GATE")).isEqualTo(TerminalSortField.GATE);
        assertThat(TerminalSortField.from("terminalId")).isEqualTo(TerminalSortField.TERMINAL_ID);
        assertThat(TerminalSortField.from("terminal_id")).isEqualTo(TerminalSortField.TERMINAL_ID);
        assertThat(TerminalSortField.from("mcc")).isEqualTo(TerminalSortField.MCC);
    }

    @Test
    void rejectsUnknown() {
        assertThatThrownBy(() -> TerminalSortField.from("bogus"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bogus");
    }
}
