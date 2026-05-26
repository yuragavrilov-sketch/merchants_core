package ru.copperside.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageWindowTest {

    @Test
    void acceptsLimitAndOffsetInsideBounds() {
        PageWindow page = PageWindow.of(100, 25);

        assertThat(page.limit()).isEqualTo(100);
        assertThat(page.offset()).isEqualTo(25);
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> PageWindow.of(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be greater than 0");
        assertThatThrownBy(() -> PageWindow.of(PageWindow.MAX_LIMIT + 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be less than or equal to " + PageWindow.MAX_LIMIT);
        assertThatThrownBy(() -> PageWindow.of(100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("offset must be greater than or equal to 0");
    }
}
