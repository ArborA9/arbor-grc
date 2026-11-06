package com.a9.arbor.grc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class NotificationMailConfiguration {

    @Bean
    @ConditionalOnProperty(name = "arbor.notifications.email-delivery", havingValue = "SMTP")
    JavaMailSender notificationJavaMailSender(ArborNotificationProperties properties) {
        ArborNotificationProperties.Smtp smtp = properties.getSmtp();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        if (smtp.getUsername() != null && !smtp.getUsername().isBlank()) {
            sender.setUsername(smtp.getUsername());
            sender.setPassword(smtp.getPassword());
        }
        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", String.valueOf(smtp.getUsername() != null && !smtp.getUsername().isBlank()));
        javaMailProps.put("mail.smtp.starttls.enable", String.valueOf(smtp.isStarttls()));
        return sender;
    }
}
