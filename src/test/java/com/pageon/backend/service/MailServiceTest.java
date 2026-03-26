package com.pageon.backend.service;

import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@Transactional
@ActiveProfiles("test")
@DisplayName("mailService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @InjectMocks
    private MailService mailService;
    @Mock
    private JavaMailSender javaMailSender;
    
    @Test
    @DisplayName("임시 비밀번호 메일이 정상적으로 전송된다.")
    void sendTemporaryPassword_shouldSendMail() {
        // given
        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        String email = "test@mail.com";
        String tempPassword = "testPassword";

        //when
        mailService.sendTemporaryPassword(email, tempPassword);
        
        // then
        verify(javaMailSender).send(mailCaptor.capture());

        SimpleMailMessage simpleMailMessage = mailCaptor.getValue();

        assertEquals(email, simpleMailMessage.getTo()[0]);
        assertEquals("[pageOn]임시 비밀번호 안내", simpleMailMessage.getSubject());
        assertTrue(simpleMailMessage.getText().contains(tempPassword));
        
    }


    @Test
    @DisplayName("임시 비밀번호 메일 전송에 실패해 CustomException 발생")
    void sendTemporaryPassword_shouldThrowCustomException() {
        // given
        String email = "test@mail.com";
        String tempPassword = "testPassword";

        doThrow(new CustomException(ErrorCode.MAIL_SEND_FAILED)).when(javaMailSender).send(any(SimpleMailMessage.class));

        //when
        CustomException exception = assertThrows(CustomException.class, () -> {
            mailService.sendTemporaryPassword(email, tempPassword);
        });

        // then
        assertEquals("메일 전송에 실패했습니다.", exception.getErrorMessage());
        assertEquals(ErrorCode.MAIL_SEND_FAILED, ErrorCode.valueOf(exception.getErrorCode()));


    }
    
    

}