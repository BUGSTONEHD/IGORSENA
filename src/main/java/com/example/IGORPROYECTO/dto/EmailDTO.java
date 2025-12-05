package com.example.IGORPROYECTO.dto;

import lombok.Data;

@Data
public class EmailDTO {
    private String id;
    private String from;
    private String subject;
    private String snippet;
    private String date;
    private boolean unread;
}