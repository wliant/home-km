package com.homekm.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationTest {

    @Test
    void clampSize_inRange_returnsAsIs() {
        assertThat(Pagination.clampSize(20)).isEqualTo(20);
        assertThat(Pagination.clampSize(1)).isEqualTo(1);
        assertThat(Pagination.clampSize(99)).isEqualTo(99);
        assertThat(Pagination.clampSize(100)).isEqualTo(100);
    }

    @Test
    void clampSize_aboveMax_clampsToMax() {
        assertThat(Pagination.clampSize(101)).isEqualTo(Pagination.MAX_SIZE);
        assertThat(Pagination.clampSize(1_000)).isEqualTo(Pagination.MAX_SIZE);
        assertThat(Pagination.clampSize(Integer.MAX_VALUE)).isEqualTo(Pagination.MAX_SIZE);
    }

    @Test
    void clampSize_zeroOrNegative_returnsDefault() {
        assertThat(Pagination.clampSize(0)).isEqualTo(Pagination.DEFAULT_SIZE);
        assertThat(Pagination.clampSize(-1)).isEqualTo(Pagination.DEFAULT_SIZE);
        assertThat(Pagination.clampSize(Integer.MIN_VALUE)).isEqualTo(Pagination.DEFAULT_SIZE);
    }

    @Test
    void clampPage_negative_collapsesToZero() {
        assertThat(Pagination.clampPage(-1)).isZero();
        assertThat(Pagination.clampPage(Integer.MIN_VALUE)).isZero();
    }

    @Test
    void clampPage_zeroOrPositive_returnsAsIs() {
        assertThat(Pagination.clampPage(0)).isZero();
        assertThat(Pagination.clampPage(1)).isEqualTo(1);
        assertThat(Pagination.clampPage(1_000)).isEqualTo(1_000);
    }
}
