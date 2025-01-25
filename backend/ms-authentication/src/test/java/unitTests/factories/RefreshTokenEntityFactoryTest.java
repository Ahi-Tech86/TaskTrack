package unitTests.factories;

import com.ahicode.enums.AppRole;
import com.ahicode.factories.RefreshTokenEntityFactory;
import com.ahicode.storage.entities.RefreshTokenEntity;
import com.ahicode.storage.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class RefreshTokenEntityFactoryTest {

    private Date expTime;
    private String token;
    private UserEntity user;

    @InjectMocks
    private RefreshTokenEntityFactory factory;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        token = "test_token";

        user = UserEntity.builder()
                .id(1L)
                .email("example@mail.com")
                .nickname("test_nickname")
                .firstname("test_firstname")
                .lastname("test_lastname")
                .password("test_password")
                .role(AppRole.USER)
                .createAt(Instant.now())
                .build();

        expTime = new Date(System.currentTimeMillis() + 360000L);
    }

    @Test
    void shouldMakeRefreshTokenEntity() {
        RefreshTokenEntity refreshToken = factory.makeRefreshTokenEntity(user, token, expTime);

        assertNotNull(refreshToken);
        assertEquals(refreshToken.getToken(), token);
        assertEquals(refreshToken.getUser(), user);
        assertEquals(expTime.getTime(), refreshToken.getExpiresAt().getTime());
    }
}
