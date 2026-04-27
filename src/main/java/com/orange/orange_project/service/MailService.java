package com.orange.orange_project.service;

import com.orange.orange_project.model.ReservationForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.admin-email:}")
    private String adminEmail;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReservationCompleteMailToCustomer(
            ReservationForm form,
            String reservationCode
    ) {
        System.out.println("MAIL FROM = " + fromEmail);
        System.out.println("CUSTOMER TO = " + form.getEmail());

        if (!StringUtils.hasText(fromEmail)) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(form.getEmail());
        message.setSubject("【猫ちゃんホテル】ご予約を受け付けました");

        message.setText("""
                %s 様

                猫ちゃんホテルへのご予約ありがとうございます。
                以下の内容でご予約を受け付けました。

                予約番号：%s
                飼い主さまのお名前：%s
                猫ちゃんのお名前：%s
                チェックイン日：%s
                チェックアウト日：%s
                お部屋：%s

                ご予約内容の確認・取り消しには、予約番号とメールアドレスをご利用ください。

                猫ちゃんホテル
                """.formatted(
                form.getOwnerName(),
                reservationCode,
                form.getOwnerName(),
                form.getCatName(),
                form.getCheckInDate(),
                form.getCheckOutDate(),
                form.getRoomType()
        ));

        mailSender.send(message);
    }

    public void sendReservationNoticeMailToAdmin(
            ReservationForm form,
            String reservationCode
    ) {
        if (!StringUtils.hasText(fromEmail) || !StringUtils.hasText(adminEmail)) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("【猫ちゃんホテル】新しい予約が入りました");

        message.setText("""
                新しい予約が入りました。

                予約番号：%s
                飼い主さまのお名前：%s
                電話番号：%s
                メールアドレス：%s
                猫ちゃんのお名前：%s
                チェックイン日：%s
                チェックアウト日：%s
                お部屋：%s
                備考：
                %s
                """.formatted(
                reservationCode,
                form.getOwnerName(),
                form.getPhone(),
                form.getEmail(),
                form.getCatName(),
                form.getCheckInDate(),
                form.getCheckOutDate(),
                form.getRoomType(),
                form.getNote()
        ));

        mailSender.send(message);
    }
}