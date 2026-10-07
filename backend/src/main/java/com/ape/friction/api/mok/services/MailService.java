package com.ape.friction.api.mok.services;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.ape.friction.api.interceptors.MethodCallLogged;

import java.time.ZonedDateTime;
import java.util.Locale;

@Service
@ConfigurationProperties(prefix = "mail")
@NoArgsConstructor
@MethodCallLogged
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MessageSource messageSource;

    private String username;

    private int inactiveDaysNumber;

    public void setUsername(@Value("${mail.username}") String username) {
        this.username = username;
        System.out.println("Setter !" + username + " ?");
    }

    public void setInactiveDaysNumber(@Value("${account.inactivity.limit.days}") int days) {
        this.inactiveDaysNumber = days;
        System.out.println("Setter !" + username + " ?");
    }

    public void sendResetPasswordMail(String to, String link, Locale locale) {
        System.out.println("FROM = [" + username + "]");


        System.out.println(messageSource.getMessage("reset.email.subject", null, Locale.forLanguageTag("pl")));

        String subject = messageSource.getMessage("reset.email.subject", null, locale);
        String text = messageSource.getMessage("reset.email.text", new Object[]{link}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }


    public void sendNotification(String to, String cause, Locale locale) {
        System.out.println("FROM = [" + username + "]");


        String translatedCause = messageSource.getMessage("notify.email." + cause, null, locale);

        System.out.println(translatedCause);
        System.out.println(messageSource.getMessage("notify.email.subject", null, Locale.forLanguageTag("pl")));

        String subject = messageSource.getMessage("notify.email.subject", null, locale);
        String text = messageSource.getMessage("notify.email.text", new Object[]{translatedCause}, locale);

        System.out.println(subject + " * " + text);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    public void send2FA(String to, String token, Locale locale) {


        String subject = messageSource.getMessage("twofa.email.subject", null, locale);
        String text = messageSource.getMessage("twofa.email.text", new Object[]{token}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);

    }

    // MOK.16
    @PreAuthorize("hasAnyAuthority('MOK15_CHANGE_OWN_EMAIL', 'MOK16_CHANGE_OTHER_EMAIL')")
    public void sendChangeNewEmailOfAccount(String to, String link, Locale locale) {
        System.out.println("FROM = [" + username + "]");


        System.out.println(messageSource.getMessage("change.otheraccount.email.subject", null, Locale.forLanguageTag("pl")));

        String subject = messageSource.getMessage("change.otheraccount.email.subject", null, locale);
        String text = messageSource.getMessage("change.otheraccount.email.text", new Object[]{link}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    // MOK.16
    public void sendChangeToOldEmailOfAccount(String to, String newEmail,String link, Locale locale) {
        System.out.println("FROM = [" + username + "]");


        System.out.println(messageSource.getMessage("change.toold.email.subject", null, Locale.forLanguageTag("pl")));

        String subject = messageSource.getMessage("change.toold.email.subject", null, locale);
        String text = messageSource.getMessage("change.toold.email.text", new Object[]{link, newEmail}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }


    // MOK.1
    public void sendVerificationMail(String to, String link, Locale locale) {


        String subject = messageSource.getMessage("verification.email.subject", null, locale);
        String text = messageSource.getMessage("verification.email.text", new Object[]{link}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    public void sendReminderVerificationMail(String to, String link, Locale locale) {


        String subject = messageSource.getMessage("verification.reminder.email.subject", null, locale);
        String text = messageSource.getMessage("verification.reminder.email.text", new Object[]{link}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    // MOK.1
    public void sendAccountDeletedAfterNoVerificationMail(String to, Locale locale) {


        String subject = messageSource.getMessage(
                "account.deleted.noverification.subject",
                null,
                locale
        );

        String text = messageSource.getMessage(
                "account.deleted.noverification.text",
                null,
                locale
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    //
    public void sendActivationMail(String to, String link, Locale locale) {

        String subject = messageSource.getMessage("activate.account.subject", null, locale);
        String text = messageSource.getMessage("activate.account.text", new Object[]{inactiveDaysNumber, link}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    //CODELOGIN
    public void sendLoginCode(String to, String code, Locale locale) {

        String subject = messageSource.getMessage("code_login.email.subject", null, locale);
        String text = messageSource.getMessage("code_login.email.text", new Object[]{code}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    public void sendNotificationToAdmin(String to, String login , String ip, ZonedDateTime time, Locale locale) {

        String subject = messageSource.getMessage("admin.logged.subject", null, locale);
        String text = messageSource.getMessage("admin.logged.text", new Object[]{login,time.toLocalDate(),time.toLocalTime(),ip}, locale);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(username);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

}
