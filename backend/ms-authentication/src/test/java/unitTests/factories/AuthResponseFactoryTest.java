package unitTests.factories;

import com.ahicode.dtos.AuthResponse;
import com.ahicode.dtos.UserDto;
import com.ahicode.factories.AuthResponseFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

public class AuthResponseFactoryTest {

    private UserDto userDto;
    private String accessToken;
    private String refreshToken;

    @InjectMocks
    private AuthResponseFactory factory;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        accessToken = "test_access_token";
        refreshToken = "test_refresh_token";

        userDto = UserDto.builder()
                .email("test@email.com")
                .nickname("test_nick")
                .firstname("test_firstname")
                .lastname("test_lastname")
                .build();
    }

    @Test
    void shouldMakeAuthResponse() {
        AuthResponse authResponse = factory.makeAuthResponse(userDto, accessToken, refreshToken);

        assertNotNull(authResponse);
        assertEquals(authResponse.getUserDto(), userDto);
        assertTrue(authResponse.getAccessToken().equals(accessToken));
        assertTrue(authResponse.getRefreshToken().equals(refreshToken));
    }
}
