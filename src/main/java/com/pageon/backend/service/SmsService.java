package com.pageon.backend.service;

import com.pageon.backend.config.SmsProperties;
import com.pageon.backend.dto.payload.OtpVerificationPayload;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.exception.NurigoMessageNotReceivedException;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsProperties smsProperties;

    public boolean singleMessageSend(OtpVerificationPayload otpVerificationPayload) {
        DefaultMessageService messageService = NurigoApp.INSTANCE.initialize(smsProperties.getApiKey(), smsProperties.getApiSecret(), smsProperties.getApiDomain());


        Message message = new Message();
        message.setFrom(smsProperties.getFromNumber());
        message.setTo(otpVerificationPayload.getCustomer().getPhoneNumber());
        message.setText(
                String.format("[pageOn 본인인증] 인증번호 [%s]를 입력해 주세요.", otpVerificationPayload.getOtp())
        );

        try {
            messageService.send(message);
            log.info("문자 전송");
            return true;
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new CustomException(ErrorCode.MESSAGE_SEND_FAILED);
        }
    }
}
