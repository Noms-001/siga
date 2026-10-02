package mg.bank.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import mg.bank.backend.model.TokenAuth;
import mg.bank.backend.model.Utilisateur;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

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
        String activationLink =
                frontendUrl + "/activation?token=" + tokenAuth.getToken();

        sendEmail(
                utilisateur.getEmail(),
                "Activation de votre compte",
                "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                + "Votre compte vient d'être créé.\n\n"
                + "Pour activer votre compte, cliquez sur le lien suivant :\n\n"
                + activationLink + "\n\n"
                + "Ce lien est temporaire et expirera automatiquement.\n\n"
                + "Cordialement,\n"
                + "Service Administration"
        );
    }

    public void sendResetPasswordEmail(
            Utilisateur utilisateur,
            TokenAuth tokenAuth
    ) {
        String resetLink =
                frontendUrl + "/reset-password?token=" + tokenAuth.getToken();

        sendEmail(
                utilisateur.getEmail(),
                "Réinitialisation de votre mot de passe",
                "Bonjour " + utilisateur.getPrenom() + ",\n\n"
                + "Une demande de réinitialisation de votre mot de passe a été effectuée.\n\n"
                + "Pour réinitialiser votre mot de passe, cliquez sur le lien suivant :\n\n"
                + resetLink + "\n\n"
                + "Ce lien est temporaire et expirera automatiquement.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.\n\n"
                + "Cordialement,\n"
                + "Service Administration"
        );
    }
}