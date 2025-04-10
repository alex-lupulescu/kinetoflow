package com.kinetoflow.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending emails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // Get sender email from application properties

    @Value("${app.frontend.url}") // Inject frontend URL for login link
    private String frontendUrl;

    /**
     * Sends an invitation email asynchronously.
     *
     * @param toEmail        The recipient's email address.
     * @param inviterName    The name of the person sending the invitation.
     * @param companyName    The name of the company the invitee is joining (can be null/empty).
     * @param role           The role the invitee is being assigned.
     * @param invitationLink The unique link to accept the invitation.
     */
    @Async // Executes this method in a separate thread (requires @EnableAsync on main app class)
    public void sendInvitationEmail(String toEmail, String inviterName, String companyName, String role, String invitationLink) {
        log.info("Attempting to send invitation email to {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("You're Invited to Join KinetoFlow!");

            String companyInfo = (companyName != null && !companyName.isEmpty()) ? " for " + companyName : "";
            String text = String.format(
                    """
                    Hello,

                    You have been invited by %s to join KinetoFlow%s as a %s.

                    Please click the link below to accept your invitation and set up your account:
                    %s

                    This link will expire in 24 hours.

                    If you did not expect this invitation, please ignore this email.

                    Best regards,
                    The KinetoFlow Team
                    """,
                    inviterName != null ? inviterName : "the KinetoFlow team",
                    companyInfo,
                    role, // Already formatted as string (e.g., "MEDIC")
                    invitationLink
            );
            message.setText(text);

            mailSender.send(message);
            log.info("Invitation email sent successfully to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send invitation email to {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending invitation email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    /**
     * Sends a welcome email after account activation asynchronously.
     *
     * @param toEmail The new user's email.
     * @param name    The new user's name.
     */
    @Async // Also make this asynchronous
    public void sendWelcomeEmail(String toEmail, String name) {
        log.info("Attempting to send welcome email to {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to KinetoFlow!");
            String loginLink = frontendUrl + "/login"; // Construct login link

            String text = String.format(
                    """
                    Hello %s,

                    Welcome to KinetoFlow! Your account has been successfully activated.

                    You can now log in using your email address and the password you set up:
                    %s

                    We're excited to have you on board!

                    Best regards,
                    The KinetoFlow Team
                    """,
                    name,
                    loginLink // Include the login link in the email body
            );
            message.setText(text);
            mailSender.send(message);
            log.info("Welcome email sent successfully to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}