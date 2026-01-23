package com.example.demo.dtos;

public class CreateCommentDTO {
    private String text;

    public CreateCommentDTO() {}

    public CreateCommentDTO(String text) {
        this.text = text;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
