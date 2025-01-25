package unitTests.services;

import com.ahicode.services.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.*;

public class EmailServiceImplTest {
    @Mock
    private JavaMailSender javaMailSender;
    @InjectMocks
    private EmailServiceImpl emailService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSendConfirmationCode() {
        String email = "test@mail.com";
        String activationCode = "123456";

        emailService.sendConfirmationCode(email, activationCode);

        SimpleMailMessage expectedMessage = new SimpleMailMessage();
        expectedMessage.setFrom(fromEmail);
        expectedMessage.setTo(email);
        expectedMessage.setSubject("Account confirmation code");
        expectedMessage.setText("Here is your confirmation code for your registration on our website. Confirmation code: " + activationCode);

        verify(javaMailSender, times(1)).send(expectedMessage);
    }
}
