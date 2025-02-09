package unitTests.repositories;

import com.ahicode.AuthenticationServiceRunner;
import com.ahicode.enums.AppRole;
import com.ahicode.storage.entities.RefreshTokenEntity;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.TokenRepository;
import com.ahicode.storage.repositories.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@ContextConfiguration(classes = AuthenticationServiceRunner.class)
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class TokenRepositoryTest {

    private UserEntity user;
    private RefreshTokenEntity token;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository repository;

    @BeforeEach
    void setup() {
        long now = System.currentTimeMillis();

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

        token = RefreshTokenEntity.builder()
                .id(1L)
                .user(user)
                .token("test_refresh_token")
                .createAt(new Date(now))
                .expiresAt(new Date(now + 3600000L))
                .build();
    }

    @Test
    void TokenRepository_Save_ShouldSaveToken() {
        userRepository.save(user);
        RefreshTokenEntity savedToken = repository.save(token);

        assertThat(savedToken).isNotNull();
        assertThat(savedToken.getId()).isGreaterThan(0L);
    }

    @Test
    void TokenRepository_Save_ShouldGetValidationError() {
        userRepository.save(user);
        token.setCreateAt(null);

        assertThrows(DataIntegrityViolationException.class, () -> {repository.save(token);});
    }

    @Test
    void TokenRepository_FindById_ShouldReturnsToken() {
        userRepository.save(user);
        RefreshTokenEntity savedToken = repository.save(token);

        Optional<RefreshTokenEntity> foundToken = repository.findById(1L);

        assertThat(foundToken).isPresent();
        assertThat(foundToken.get()).isEqualTo(savedToken);
    }

    @Test
    void TokenRepository_FindById_ShouldReturnsNull() {
        Optional<RefreshTokenEntity> foundToken = repository.findById(228L);

        assertThat(foundToken).isEmpty();
    }

    @Test
    void TokenRepository_FindByNickname_ShouldReturnsToken() {
        userRepository.save(user);
        RefreshTokenEntity savedToken = repository.save(token);

        Optional<RefreshTokenEntity> foundToken = repository.findByNickname("user_test");

        assertThat(foundToken).isPresent();
        assertThat(foundToken.get()).isEqualTo(savedToken);
    }

    @Test
    void TokenRepository_FindByNickname_ShouldReturnsNull() {
        Optional<RefreshTokenEntity> foundToken = repository.findByNickname("Peter Griffin");

        assertThat(foundToken).isEmpty();
    }
}
