package integrationTests;

import com.ahicode.AuthenticationServiceRunner;
import com.ahicode.dtos.ConfirmationRegisterRequest;
import com.ahicode.dtos.SignUpRequest;
import com.ahicode.dtos.TemporaryUserDto;
import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
import com.ahicode.services.EmailService;
import com.ahicode.services.EncryptionService;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.UserRepository;
import com.redis.testcontainers.RedisContainer;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@ContextConfiguration(classes = AuthenticationServiceRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationServiceApplicationTest {
    @LocalServerPort
    private Integer port;
    @ServiceConnection
    static RedisContainer redisContainer = new RedisContainer("redis:7.4");
    @ServiceConnection
    static PostgreSQLContainer psqlContainer = new PostgreSQLContainer("postgres:15");

    @MockBean
    private EmailService emailService;
    @SpyBean
    private EncryptionService encryptionService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisTemplate<String, TemporaryUserDto> redisTemplate;

    @BeforeAll
    static void setupAll() {
        String dotenvPath = "../../.env";

        Dotenv dotenv = Dotenv.configure()
                .directory(dotenvPath)
                .filename(".env").load();
        dotenv.entries().forEach(
                entry -> System.setProperty(entry.getKey(), entry.getValue())
        );

        redisContainer.start();
        psqlContainer.start();
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Tests for registration endpoint")
    class RegistrationTests {
        @Test
        void should_Return_200_And_Register_Successfully() {
            SignUpRequest requestBody = getSignUpRequest();

            String expectedResponseBody = "An activation code has been sent to your email, please send the activation code " +
                    "before it expires. The activation code expires in 20 minutes.";

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body(equalTo(expectedResponseBody));
        }

        @Test
        void should_Return_400_For_Invalid_Email() {
            SignUpRequest requestBody = getSignUpRequest();
            requestBody.setEmail("invalid-email.com");

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .log().all()
                    .statusCode(400);
        }

        @Test
        void should_Return_400_For_Not_Unique_Nickname() {
            UserEntity user = UserEntity.builder()
                    .id(1L)
                    .email("example-test@mail.com")
                    .nickname("nick19")
                    .firstname("Firstname")
                    .lastname("Lastname")
                    .role(AppRole.USER)
                    .createAt(Instant.now())
                    .password("password")
                    .build();
            userRepository.save(user);

            SignUpRequest requestBody = getSignUpRequest();

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .body("message", equalTo("User with nickname nick19 is already exists"));
        }

        @Test
        void should_Return_400_For_Not_Unique_Email() {
            UserEntity user = UserEntity.builder()
                    .id(1L)
                    .email("example@mail.com")
                    .nickname("nick1")
                    .firstname("Firstname")
                    .lastname("Lastname")
                    .role(AppRole.USER)
                    .createAt(Instant.now())
                    .password("password")
                    .build();
            userRepository.save(user);

            SignUpRequest requestBody = getSignUpRequest();

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .body("message", equalTo("User with email example@mail.com is already exists"));
        }

        @Test
        void should_Return_500_For_Email_Sending_Error() {
            SignUpRequest requestBody = getSignUpRequest();

            doThrow(new RuntimeException("Email sending failed"))
                    .when(emailService).sendConfirmationCode(anyString(), anyString());

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/register")
                    .then()
                    .log().all()
                    .statusCode(500);
        }
    }

    @Nested
    @DisplayName("Tests for confirmation registration endpoint")
    class ConfirmationTests {
        @Test
        void should_Return_201_And_Confirm_Registration_Successfully() {
            TemporaryUserDto userDto = getTemporaryUserDto();
            String email = userDto.getEmail();
            redisTemplate.opsForValue().set(email, userDto, 3, TimeUnit.MINUTES);

            ConfirmationRegisterRequest requestBody = getConfirmationRegisterRequest();

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/confirmRegister")
                    .then()
                    .log().all()
                    .statusCode(201)
                    .body("email", equalTo(userDto.getEmail()))
                    .body("nickname", equalTo(userDto.getNickname()))
                    .body("firstname", equalTo(userDto.getFirstname()))
                    .body("lastname", equalTo(userDto.getLastname()));
        }

        @Test
        void should_Return_401_Because_Confirmation_Code_Didnot_Matches() {
            TemporaryUserDto userDto = getTemporaryUserDto();
            String email = userDto.getEmail();
            redisTemplate.opsForValue().set(email, userDto, 3, TimeUnit.MINUTES);

            ConfirmationRegisterRequest requestBody = getConfirmationRegisterRequest();
            requestBody.setConfirmationCode("654321");

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/confirmRegister")
                    .then()
                    .log().all()
                    .statusCode(401)
                    .body("message", equalTo("The confirmation code doesn't match what the server generated"));
        }

        @Test
        void should_Return_500_For_Serializing_Problems() {
            doThrow(new AppException("An error occurred while encrypting data", HttpStatus.INTERNAL_SERVER_ERROR))
                    .when(encryptionService).encrypt(anyString());

            TemporaryUserDto userDto = getTemporaryUserDto();
            String email = userDto.getEmail();
            redisTemplate.opsForValue().set(email, userDto, 3, TimeUnit.MINUTES);

            ConfirmationRegisterRequest requestBody = getConfirmationRegisterRequest();

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .when()
                    .post("/api/v1/auth/confirmRegister")
                    .then()
                    .log().all()
                    .statusCode(500);
        }
    }

    private SignUpRequest getSignUpRequest() {
        return SignUpRequest.builder()
                .email("example@mail.com")
                .nickname("nick19")
                .firstname("Firstname")
                .lastname("Lastname")
                .password("password")
                .build();
    }

    private TemporaryUserDto getTemporaryUserDto() {
        return TemporaryUserDto.builder()
                .email("test@mail.com")
                .nickname("nick")
                .firstname("firstname")
                .lastname("lastname")
                .password("1")
                .confirmationCode("123456")
                .build();
    }

    private ConfirmationRegisterRequest getConfirmationRegisterRequest() {
        return ConfirmationRegisterRequest.builder()
                .email("test@mail.com")
                .confirmationCode("123456")
                .build();
    }
}
