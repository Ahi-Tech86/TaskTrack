package com.ahicode.services.impl;

import com.ahicode.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    @Value("${spring.mail.username}")
    private String username;
    private final JavaMailSender mailSender;

    @Override
    public void sendConfirmationCode(String sendTo, String confirmationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(username);
        message.setTo(sendTo);
        message.setSubject("Account confirmation code");
        message.setText(
                String.format(
                        "Here is your confirmation code for your registration on our website. Confirmation code: %s",
                        confirmationCode
                )
        );
        mailSender.send(message);
    }
}
