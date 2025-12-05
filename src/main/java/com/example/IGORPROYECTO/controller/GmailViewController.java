package com.example.IGORPROYECTO.controller;

import com.example.IGORPROYECTO.dto.EmailDTO;
import com.example.IGORPROYECTO.dto.EmailDetailDTO;
import com.example.IGORPROYECTO.model.Usuario;
import com.example.IGORPROYECTO.repository.UsuarioRepository;
import com.example.IGORPROYECTO.service.GmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/gmail")
@RequiredArgsConstructor
public class GmailViewController {

    private final GmailService gmailService;
    private final UsuarioRepository usuarioRepository;

    // Callback de OAuth2 - Aquí llega después de autorizar en Google
    @GetMapping("/oauth2callback")
    public String oauth2Callback(@RequestParam String code, @RequestParam String state, RedirectAttributes redirectAttributes) {
        try {
            String userEmail = state;
            gmailService.handleOAuthCallback(code, userEmail);
            redirectAttributes.addFlashAttribute("mensaje", "Gmail conectado exitosamente");
            return "redirect:/gmail/correos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al conectar Gmail: " + e.getMessage());
            return "redirect:/home";
        }
    }

    // Vista principal: Lista de correos
    @GetMapping("/correos")
    public String listarCorreos(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar si el usuario ha conectado Gmail
            if (usuario.getGmailAccessToken() == null) {
                redirectAttributes.addFlashAttribute("error", "Debes conectar tu cuenta de Gmail primero");
                return "redirect:/home";
            }
            
            List<EmailDTO> correos = gmailService.listEmails(usuario.getCorreo(), 20);
            model.addAttribute("correos", correos);
            
            return "gmail/correos";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar correos: " + e.getMessage());
            return "redirect:/home";
        }
    }

    // Vista de detalle: Ver un correo específico
    @GetMapping("/correo/{messageId}")
    public String verDetalle(@PathVariable String messageId, Principal principal, Model model, RedirectAttributes redirectAttributes) {
        try {
            String username = principal.getName();
            Usuario usuario = usuarioRepository.findByUsuario(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            EmailDetailDTO detalle = gmailService.getEmailDetail(usuario.getCorreo(), messageId);
            model.addAttribute("correo", detalle);
            
            return "gmail/detalle";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el correo: " + e.getMessage());
            return "redirect:/gmail/correos";
        }
    }
}