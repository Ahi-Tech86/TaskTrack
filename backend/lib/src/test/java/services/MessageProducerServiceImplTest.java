package services;

import com.ahicode.services.MessageProducerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class MessageProducerServiceImplTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private MessageProducerServiceImpl messageProducerService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldSendMessage() {
        List<String> messages = Arrays.asList("token1", "token2", "token3");

        messageProducerService.sendMessage(messages);

        verify(kafkaTemplate, times(messages.size())).send(eq("blacklist_tokens_topic"), anyString());
    }
}
