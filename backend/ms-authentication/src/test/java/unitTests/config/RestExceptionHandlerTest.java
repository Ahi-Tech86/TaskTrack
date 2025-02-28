package unitTests.config;

import com.ahicode.config.RestExceptionHandler;
import com.ahicode.dtos.ErrorDto;
import com.ahicode.exceptions.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RestExceptionHandlerTest {

    @InjectMocks
    private RestExceptionHandler restExceptionHandler;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldHandleAppException() {
        AppException exception = new AppException("Test error message", HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorDto> response = restExceptionHandler.exceptionHandling(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Test error message", response.getBody().getMessage());
        assertEquals(exception.getTimestamp(), response.getBody().getTimestamp());
    }
}
