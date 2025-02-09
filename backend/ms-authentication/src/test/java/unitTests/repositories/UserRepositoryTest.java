package unitTests.repositories;

import com.ahicode.AuthenticationServiceRunner;
import com.ahicode.enums.AppRole;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ContextConfiguration(classes = AuthenticationServiceRunner.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class UserRepositoryTest {

    private UserEntity user;
    @Autowired
    private UserRepository repository;

    @BeforeEach
    void setup() {
        user = UserEntity.builder()
                .id(1L)
                .email("user@mail.com")
                .nickname("user_test")
                .firstname("firstname")
                .lastname("lastname")
                .role(AppRole.USER)
                .password("1")
                .createAt(Instant.now())
                .build();
    }

    @Test
    void UserRepository_Save_ShouldSaveUser() {
        UserEntity savedUser = repository.save(user);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isGreaterThan(0L);
    }

    @Test
    void UserRepository_Save_ShouldGetValidationError() {
        user.setEmail("user");

        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class, () -> {repository.save(user);}
        );

        assertThat(exception.getConstraintViolations())
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email");
    }

    @Test
    void UserRepository_FindById_ShouldReturnsUser() {
        UserEntity savedUser = repository.save(user);

        Optional<UserEntity> foundUser = repository.findById(1L);

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get()).isEqualTo(savedUser);
    }

    @Test
    void UserRepository_FindById_ShouldReturnsNull() {
        Optional<UserEntity> foundUser = repository.findById(228L);

        assertThat(foundUser).isEmpty();
    }

    @Test
    void UserRepository_FindByNickname_ShouldReturnsUser() {
        UserEntity savedUser = repository.save(user);

        Optional<UserEntity> foundUser = repository.findByNickname("user_test");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get()).isEqualTo(savedUser);
    }

    @Test
    void UserRepository_FindByNickname_ShouldReturnsNull() {
        Optional<UserEntity> foundUser = repository.findByNickname("nico_belik");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void UserRepository_FindByEmail_ShouldReturnsUser() {
        UserEntity savedUser = repository.save(user);

        Optional<UserEntity> foundUser = repository.findByEmail("user@mail.com");

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get()).isEqualTo(savedUser);
    }

    @Test
    void UserRepository_FindByEmail_ShouldReturnsNull() {
        Optional<UserEntity> foundUser = repository.findByEmail("nico_belik@mail.sr");

        assertThat(foundUser).isEmpty();
    }

    @Test
    void UserRepository_Save_ShouldGetUniquenessError() {
        repository.save(user);

        UserEntity duplicatedUser = user;
        duplicatedUser.setId(2L);
        duplicatedUser.setEmail("user@mail.org");

        assertThrows(DataIntegrityViolationException.class, () -> {
                repository.save(duplicatedUser);
        });
    }
}
