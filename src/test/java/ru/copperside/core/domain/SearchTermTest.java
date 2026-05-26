package ru.copperside.core.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTermTest {

    @Test
    void blankSearchIsAbsent() {
        SearchTerm term = SearchTerm.of("   ");

        assertThat(term.isPresent()).isFalse();
        assertThat(term.likePattern()).isNull();
    }

    @Test
    void searchTermIsTrimmedLowercasedAndWrappedForLike() {
        SearchTerm term = SearchTerm.of("  Alpha  ");

        assertThat(term.isPresent()).isTrue();
        assertThat(term.value()).isEqualTo("alpha");
        assertThat(term.likePattern()).isEqualTo("%alpha%");
    }
}
