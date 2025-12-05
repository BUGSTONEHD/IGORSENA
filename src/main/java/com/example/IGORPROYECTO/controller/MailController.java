package com.example.IGORPROYECTO.controller;

import com.example.IGORPROYECTO.service.EmailSenderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mail")
public class MailController {

    private final EmailSenderService emailSenderService;

    public MailController(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @GetMapping("/form")
    public String mostrarFormulario() {
        return "AnalisisYReportes/form";
    }

    @PostMapping("/send")
    public String enviarCorreo(String destinatario, String asunto, String mensaje, Model model) {

        try {
            emailSenderService.enviarCorreo(destinatario, asunto, mensaje);
            model.addAttribute("respuesta", "Correo enviado exitosamente.");
        } catch (Exception e) {
            model.addAttribute("respuesta", "Error al enviar el correo: " + e.getMessage());
        }

        // CORREGIDO: antes decía "mail/form" (que NO existe)
        return "AnalisisYReportes/form";
    }
}
