package com.example.IGORPROYECTO.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.IGORPROYECTO.model.Usuario;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    // Buscar usuario por el campo "usuario"
    Optional<Usuario> findByUsuario(String usuario);
    
    // Buscar usuario por correo (necesario para Gmail)
    Optional<Usuario> findByCorreo(String correo);
}