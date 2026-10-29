package dev.sentinel.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Keeps paging inputs sane so nobody can ask for a million rows at once. */
final class PageParams {

    static final int MAX_SIZE = 100;

    private PageParams() {}

    static PageRequest of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(0, page), Math.min(MAX_SIZE, Math.max(1, size)), sort);
    }
}
