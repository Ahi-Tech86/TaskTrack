package factories;

import com.ahicode.dtos.TemporaryUserDto;
import com.ahicode.enums.AppRole;
import com.ahicode.factories.UserEntityFactory;
import com.ahicode.storage.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

public class UserEntityFactoryTest {

    private TemporaryUserDto userDto;

    @InjectMocks
    private UserEntityFactory factory;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        userDto = TemporaryUserDto.builder()
                .email("example@mail.com")
                .nickname("test_nickname")
                .firstname("test_firstname")
                .lastname("test_lastname")
                .password("test_password")
                .confirmationCode("123456")
                .build();
    }

    @Test
    void shouldMakeUserEntity() {
        UserEntity user = factory.makeUserEntity(userDto);

        assertNotNull(user);
        assertEquals(user.getNickname(), userDto.getNickname());
        assertEquals(user.getEmail(), userDto.getEmail());
        assertEquals(user.getFirstname(), userDto.getFirstname());
        assertEquals(user.getLastname(), userDto.getLastname());
        assertEquals(user.getRole(), AppRole.USER);
    }

    @Test
    void shouldMakeAdminUserEntity() {
        UserEntity user = factory.makeAdminUserEntity(userDto);

        assertNotNull(user);
        assertEquals(user.getNickname(), userDto.getNickname());
        assertEquals(user.getEmail(), userDto.getEmail());
        assertEquals(user.getFirstname(), userDto.getFirstname());
        assertEquals(user.getLastname(), userDto.getLastname());
        assertEquals(user.getRole(), AppRole.ADMIN);
    }
}
