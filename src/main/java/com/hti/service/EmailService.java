package com.hti.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger("tracklogger");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetLink(String toEmail, String username, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request");
            message.setText(
                "Hello " + username + ",\n\n" +
                "Click the link below to reset your password (valid for 15 minutes):\n\n" +
                resetLink + "\n\n" +
                "If you did not request this, ignore this email.\n\n" +
                "Regards,\nHTI Team"
            );
            mailSender.send(message);
            logger.info("Reset link sent | to={}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send reset link | to={}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendOtpEmail(String toEmail, String username, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your OTP for Password Reset");
            message.setText(
                "Hello " + username + ",\n\n" +
                "Your OTP for password reset is:\n\n" +
                otp + "\n\n" +
                "Valid for 15 minutes. Do not share with anyone.\n\n" +
                "Regards,\nHTI Team"
            );
            mailSender.send(message);
            logger.info("OTP sent | to={}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP | to={}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}