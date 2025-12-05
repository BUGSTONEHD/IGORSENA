package com.example.IGORPROYECTO.controller;

import com.example.IGORPROYECTO.dto.EmailDTO;
import com.example.IGORPROYECTO.dto.EmailDetailDTO;
import com.example.IGORPROYECTO.model.Usuario;
import com.example.IGORPROYECTO.repository.UsuarioRepository;
import com.example.IGORPROYECTO.service.GmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailService gmailService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(Authentication authentication) {
        try {
            String username = authentication.getName();
            
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            String authUrl = gmailService.getAuthorizationUrl(usuario.getCorreo());
            return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/emails")
    public ResponseEntity<?> listEmails(
            Authentication authentication,
            @RequestParam(required = false) Integer maxResults) {
        try {
            String username = authentication.getName();
            
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            List<EmailDTO> emails = gmailService.listEmails(usuario.getCorreo(), maxResults);
            return ResponseEntity.ok(emails);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/emails/{messageId}")
    public ResponseEntity<?> getEmailDetail(
            Authentication authentication,
            @PathVariable String messageId) {
        try {
            String username = authentication.getName();
            
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            EmailDetailDTO email = gmailService.getEmailDetail(usuario.getCorreo(), messageId);
            return ResponseEntity.ok(email);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/emails/{messageId}/read")
    public ResponseEntity<?> markAsRead(
            Authentication authentication,
            @PathVariable String messageId) {
        try {
            String username = authentication.getName();
            
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            gmailService.markAsRead(usuario.getCorreo(), messageId);
            return ResponseEntity.ok(Map.of("message", "Correo marcado como leído"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}