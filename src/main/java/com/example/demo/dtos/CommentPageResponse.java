package com.example.demo.dtos;

import java.util.List;

public class CommentPageResponse {
    private List<CommentDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    public CommentPageResponse(List<CommentDTO> content, int pageNumber, int pageSize, 
                              long totalElements, int totalPages) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<CommentDTO> getContent() { return content; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
