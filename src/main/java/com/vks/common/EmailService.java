package com.vks.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtp(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your OTP for Password Reset");
            message.setText("Your OTP is: " + otp + "\n\nThis OTP is valid for 5 minutes. Do not share it with anyone.");
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}, error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email");
        }
    }

    public void sendBookingPendingEmail(String toEmail, String bookingId, String eventName) {
        sendEmail(toEmail, "Your booking is reserved",
                "Your booking " + bookingId + " for " + eventName + " is reserved and currently awaiting payment confirmation.");
    }

    public void sendBookingConfirmedEmail(String toEmail, String bookingId, String amount) {
        sendEmail(toEmail, "Booking confirmed",
                "Your booking " + bookingId + " has been confirmed. Amount paid: " + amount + ".");
    }

    public void sendBookingCancelledEmail(String toEmail, String bookingId, String eventName) {
        sendEmail(toEmail, "Your booking has been cancelled",
                "Your booking " + bookingId + " for " + eventName + " has been cancelled.");
    }

    public void sendAdminOnboardingEmail(String toEmail, String tenantId) {
        sendEmail(toEmail, "Tenant admin onboarding successful",
                "You have been onboarded as a tenant admin for tenant " + tenantId + ".");
    }

    private void sendEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent to: {} with subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to: {}, error: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }
}
