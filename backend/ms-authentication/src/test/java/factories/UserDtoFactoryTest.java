package factories;

import com.ahicode.dtos.UserDto;
import com.ahicode.enums.AppRole;
import com.ahicode.factories.UserDtoFactory;
import com.ahicode.storage.entities.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class UserDtoFactoryTest {

    private UserEntity user;

    @InjectMocks
    private UserDtoFactory factory;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

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
    }

    @Test
    void shouldMakeUserDto() {
        UserDto userDto = factory.makeUserDto(user);

        assertNotNull(userDto);
        assertEquals(userDto.getEmail(), user.getEmail());
        assertEquals(userDto.getNickname(), user.getNickname());
        assertEquals(userDto.getFirstname(), user.getFirstname());
        assertEquals(userDto.getLastname(), user.getLastname());
    }
}
