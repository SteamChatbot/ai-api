package com.sanavi.aiapi.common.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class PageResponse<T> {
    private final List<T> contents;
    private final int page;
    private final int size;
    private final int totalCount;
    private final int totalPages;

    public PageResponse(List<T> contents, int page, int size, int totalCount) {
        this.contents = contents;
        this.page = page;
        this.size = size;
        this.totalCount = totalCount;
        this.totalPages = (int) Math.ceil((double) totalCount / size);
    }
}
