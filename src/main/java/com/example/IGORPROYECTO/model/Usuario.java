package com.example.IGORPROYECTO.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "usuarios")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario {

    @Id
    private String id;
    private String nombre_completo;
    private String usuario;
    private String contrasena;   // sin tilde, Java no se lleva bien con ñ o tildes
    private String rol;
    private String correo;
    private String no_documento;
    private String telefono;
    private String direccion;
    private Date fecha_registro;  // Mongo lo guarda como ISODate

    // Agregar estos campos a tu clase Usuario
private String gmailAccessToken;
private String gmailRefreshToken;
private Long gmailTokenExpiry;
    
}


