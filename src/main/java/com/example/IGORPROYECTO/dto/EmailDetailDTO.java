package com.example.IGORPROYECTO.dto;

import lombok.Data;

@Data
public class EmailDetailDTO {
    private String id;
    private String from;
    private String to;
    private String subject;
    private String body;
    private String date;
    private boolean unread;
}