package mg.bank.backend.service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.model.TokenAuth;
import mg.bank.backend.model.Utilisateur;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(
            String to,
            String subject,
            String text
    ) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    public void sendActivationEmail(
            Utilisateur utilisateur,
            TokenAuth tokenAuth
    ) {
        sendEmail(
                utilisateur.getEmail(),
                "Activation de votre compte",
                "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                + "Votre compte vient d'être créé.\n\n"
                + "Voici votre token d'activation :\n\n"
                + tokenAuth.getToken() + "\n\n"
                + "Ce token est temporaire et expirera automatiquement.\n\n"
                + "Cordialement,\n"
                + "Service Administration"
        );
    }

    public void sendResetPasswordEmail(
            Utilisateur utilisateur,
            TokenAuth tokenAuth
    ) {
        sendEmail(
                utilisateur.getEmail(),
                "Réinitialisation de votre mot de passe",
                "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                + "Une demande de réinitialisation de votre mot de passe a été effectuée.\n\n"
                + "Votre token de réinitialisation est :\n\n"
                + tokenAuth.getToken() + "\n\n"
                + "Ce token est temporaire et expirera automatiquement.\n\n"
                + "Cordialement,\n"
                + "Service Administration"
        );
    }
}
