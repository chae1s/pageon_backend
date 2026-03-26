package com.pageon.backend.service;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    //메일 전송
    public void sendTemporaryPassword(String email, String tempPassword) {
        log.info("메일 전송");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[pageOn]임시 비밀번호 안내");
        message.setText(
                "임시 비밀번호는 다음과 같습니다.\n" + tempPassword + "\n로그인 후 반드시 비밀번호를 변경해주세요."
        );
        try {
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
        }

    }
}
