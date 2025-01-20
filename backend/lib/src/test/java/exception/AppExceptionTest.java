package exception;

import com.ahicode.exceptions.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AppExceptionTest {

    @Test
    void shouldInitializeAppExceptionCorrectly() {
        String message = "Test exception message";
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        LocalDateTime timestamp = LocalDateTime.now();

        AppException exception = new AppException(message, httpStatus);

        assertEquals(message, exception.getMessage());
        assertEquals(httpStatus, exception.getHttpStatus());
        assertNotNull(exception.getTimestamp());

        long timestampDifference = java.time.Duration.between(timestamp, exception.getTimestamp()).toMillis();
        assertTrue(timestampDifference < 1000, "Timestamp difference should be less than 1 second");
    }
}
