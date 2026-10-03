package com.vks.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class OtpDeliveryService {
    private final String mode;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final boolean trial;
    private final HttpClient client = HttpClient.newHttpClient();

    public OtpDeliveryService(
            @Value("${otp.delivery-mode:console}") String mode,
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.from-number:}") String fromNumber,
            @Value("${twilio.trial:true}") boolean trial) {
        this.mode = mode;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.trial = trial;
    }

    public boolean sendsSms() {
        return "twilio".equalsIgnoreCase(mode);
    }

    public boolean isTrial() {
        return trial;
    }

    public void deliver(String mobile, String otp) {
        if (!sendsSms()) {
            System.out.println("[LOCAL OTP] mobile=" + mobile + " otp=" + otp);
            return;
        }
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new IllegalStateException("Twilio OTP delivery is not configured");
        }
        String body = trial ? "sms_2fa" : "Your Evently verification code is " + otp + ". It expires in 5 minutes.";
        String form = "To=" + encode("+91" + mobile) + "&From=" + encode(fromNumber)
                + "&Body=" + encode(body);
        String credentials = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Twilio rejected the OTP message (HTTP " + response.statusCode() + "): " + response.body());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OTP delivery was interrupted", exception);
        } catch (Exception exception) {
            String reason = exception.getMessage() == null ? "unknown Twilio error" : exception.getMessage();
            throw new IllegalStateException("Unable to send OTP: " + reason, exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
