package com.example.IGORPROYECTO.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.example.IGORPROYECTO.dto.EmailDTO;
import com.example.IGORPROYECTO.dto.EmailDetailDTO;
import com.example.IGORPROYECTO.model.Usuario;
import com.example.IGORPROYECTO.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GmailService {

    private final UsuarioRepository usuarioRepository;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_MODIFY
    );

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uri}")
    private String redirectUri;

    @Value("${google.application.name}")
    private String applicationName;

    public String getAuthorizationUrl(String userEmail) throws GeneralSecurityException, IOException {
    NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientId, clientSecret, SCOPES)
            .setAccessType("offline")
            .setApprovalPrompt("force")
            .build();

    return flow.newAuthorizationUrl()
            .setRedirectUri(redirectUri)
            .setState(userEmail)  // Aquí debe ir el correo, no el nombre
            .build();
}

    public void handleOAuthCallback(String code, String userEmail) throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                httpTransport,
                JSON_FACTORY,
                clientId,
                clientSecret,
                code,
                redirectUri
        ).execute();

        Usuario usuario = usuarioRepository.findByCorreo(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setGmailAccessToken(tokenResponse.getAccessToken());
        usuario.setGmailRefreshToken(tokenResponse.getRefreshToken());
        usuario.setGmailTokenExpiry(System.currentTimeMillis() + (tokenResponse.getExpiresInSeconds() * 1000));
        
        usuarioRepository.save(usuario);
    }

    private Gmail getGmailService(Usuario usuario) throws GeneralSecurityException, IOException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setAccessToken(usuario.getGmailAccessToken())
                .setRefreshToken(usuario.getGmailRefreshToken());

        if (System.currentTimeMillis() >= usuario.getGmailTokenExpiry()) {
            credential.refreshToken();
            usuario.setGmailAccessToken(credential.getAccessToken());
            usuario.setGmailTokenExpiry(System.currentTimeMillis() + 3600000);
            usuarioRepository.save(usuario);
        }

        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    public List<EmailDTO> listEmails(String userEmail, Integer maxResults) throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getGmailAccessToken() == null) {
            throw new RuntimeException("Usuario no ha conectado su cuenta de Gmail");
        }

        Gmail service = getGmailService(usuario);
        
        ListMessagesResponse response = service.users().messages()
                .list("me")
                .setMaxResults(maxResults != null ? Long.valueOf(maxResults) : 20L)
                .setQ("in:inbox")
                .execute();

        List<Message> messages = response.getMessages();
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        return messages.stream()
                .map(msg -> {
                    try {
                        Message fullMessage = service.users().messages()
                                .get("me", msg.getId())
                                .setFormat("metadata")
                                .setMetadataHeaders(Arrays.asList("From", "Subject", "Date"))
                                .execute();
                        
                        return convertToEmailDTO(fullMessage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public EmailDetailDTO getEmailDetail(String userEmail, String messageId) throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Gmail service = getGmailService(usuario);
        
        Message message = service.users().messages()
                .get("me", messageId)
                .setFormat("full")
                .execute();

        return convertToEmailDetailDTO(message);
    }

    public void markAsRead(String userEmail, String messageId) throws Exception {
        Usuario usuario = usuarioRepository.findByCorreo(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Gmail service = getGmailService(usuario);
        
        service.users().messages()
                .modify("me", messageId, 
                    new com.google.api.services.gmail.model.ModifyMessageRequest()
                        .setRemoveLabelIds(Collections.singletonList("UNREAD")))
                .execute();
    }

    private EmailDTO convertToEmailDTO(Message message) {
        EmailDTO dto = new EmailDTO();
        dto.setId(message.getId());
        dto.setSnippet(message.getSnippet());
        dto.setUnread(message.getLabelIds() != null && message.getLabelIds().contains("UNREAD"));

        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            switch (header.getName()) {
                case "From":
                    dto.setFrom(header.getValue());
                    break;
                case "Subject":
                    dto.setSubject(header.getValue());
                    break;
                case "Date":
                    dto.setDate(header.getValue());
                    break;
            }
        }

        return dto;
    }

    private EmailDetailDTO convertToEmailDetailDTO(Message message) {
        EmailDetailDTO dto = new EmailDetailDTO();
        dto.setId(message.getId());
        dto.setUnread(message.getLabelIds() != null && message.getLabelIds().contains("UNREAD"));

        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            switch (header.getName()) {
                case "From":
                    dto.setFrom(header.getValue());
                    break;
                case "To":
                    dto.setTo(header.getValue());
                    break;
                case "Subject":
                    dto.setSubject(header.getValue());
                    break;
                case "Date":
                    dto.setDate(header.getValue());
                    break;
            }
        }

        dto.setBody(getMessageBody(message.getPayload()));

        return dto;
    }

    private String getMessageBody(MessagePart part) {
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] bodyBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
            return new String(bodyBytes);
        }

        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                if (subPart.getMimeType().equals("text/plain") || subPart.getMimeType().equals("text/html")) {
                    String body = getMessageBody(subPart);
                    if (body != null) return body;
                }
            }
        }

        return "";
    }
}
