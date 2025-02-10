package integrationTests;

import com.ahicode.AuthenticationServiceRunner;
import com.ahicode.dtos.ConfirmationRegisterRequest;
import com.ahicode.dtos.SignInRequest;
import com.ahicode.dtos.SignUpRequest;
import com.ahicode.dtos.TemporaryUserDto;
import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
import com.ahicode.services.EmailService;
import com.ahicode.services.EncryptionService;
import com.ahicode.services.JwtService;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.UserRepository;
import com.redis.testcontainers.RedisContainer;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
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
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RedisTemplate<String, TemporaryUserDto> redisTemplate;
    @Autowired
    private RedisTemplate<String, Integer> integerRedisTemplate;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        userRepository.deleteAll();
    }

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

    @AfterEach
    void tearDown() {
        String lockKey = "locked:non-existent@mail.com";
        integerRedisTemplate.delete(lockKey);
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

        private SignUpRequest getSignUpRequest() {
            return SignUpRequest.builder()
                    .email("example@mail.com")
                    .nickname("nick19")
                    .firstname("Firstname")
                    .lastname("Lastname")
                    .password("password")
                    .build();
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

    @Nested
    @DisplayName("Tests for login operation endpoint")
    class LoginTests {
        private final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
        private final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

        @Test
        void should_Return_200_And_Log_In_Successfully() {
            UserEntity user = getUserEntity();
            userRepository.save(user);

            SignInRequest requestBody = getSignInRequest();

            Response response = RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .post("/api/v1/auth/login")
                    .then()
                    .log().all()
                    .statusCode(200)
                    .body("email", equalTo(user.getEmail()))
                    .body("nickname", equalTo(user.getNickname()))
                    .body("firstname", equalTo(user.getFirstname()))
                    .body("lastname", equalTo(user.getLastname()))
                    .extract().response();

            String accessTokenCookie = response.getCookie(ACCESS_TOKEN_COOKIE_NAME);
            String refreshTokenCookie = response.getCookie(REFRESH_TOKEN_COOKIE_NAME);

            Assertions.assertNotNull(accessTokenCookie);
            Assertions.assertNotNull(refreshTokenCookie);

            Assertions.assertEquals("/", response.getDetailedCookie(ACCESS_TOKEN_COOKIE_NAME).getPath());
            Assertions.assertEquals("/", response.getDetailedCookie(REFRESH_TOKEN_COOKIE_NAME).getPath());
            Assertions.assertTrue(response.getDetailedCookie(ACCESS_TOKEN_COOKIE_NAME).getMaxAge() > 0);
            Assertions.assertTrue(response.getDetailedCookie(REFRESH_TOKEN_COOKIE_NAME).getMaxAge() > 0);
        }

        @Test
        void should_Return_400_For_Locked_Log_In() {
            UserEntity user = getUserEntity();
            userRepository.save(user);

            SignInRequest requestBody = getSignInRequest();
            requestBody.setEmail("non-existent@mail.com");

            String lockKey = "locked:" + requestBody.getEmail();
            integerRedisTemplate.opsForValue().set(lockKey, 2, 5, TimeUnit.MINUTES);

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .post("/api/v1/auth/login")
                    .then()
                    .log().all()
                    .statusCode(400)
                    .body("message", equalTo("Due to an incorrect password entry, we have temporarily " +
                            "blocked you from logging into your account. Come back later")
                    );
        }

        @Test
        void should_Return_401_For_Wrong_Password() {
            UserEntity user = getUserEntity();
            userRepository.save(user);

            SignInRequest requestBody = getSignInRequest();
            requestBody.setPassword("invalid-password");

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .post("/api/v1/auth/login")
                    .then()
                    .log().all()
                    .statusCode(401)
                    .body("message", equalTo("Incorrect password"));
        }

        @Test
        void should_Return_404_For_Non_Existent_Email() {
            UserEntity user = getUserEntity();
            userRepository.save(user);

            SignInRequest requestBody = getSignInRequest();
            requestBody.setEmail("non-existent@mail.com");

            RestAssured.given()
                    .contentType("application/json")
                    .body(requestBody)
                    .post("/api/v1/auth/login")
                    .then()
                    .log().all()
                    .statusCode(404)
                    .body(
                            "message",
                            equalTo(String.format("User with email %s doesn't exists", requestBody.getEmail()))
                    );
        }

        private UserEntity getUserEntity() {
            return UserEntity.builder()
                    .id(1L)
                    .email("test@mail.com")
                    .nickname("nick")
                    .firstname("firstname")
                    .lastname("lastname")
                    .createAt(Instant.now())
                    .password(passwordEncoder.encode("password"))
                    .role(AppRole.USER)
                    .build();
        }

        private SignInRequest getSignInRequest() {
            return SignInRequest.builder()
                    .email("test@mail.com")
                    .password("password")
                    .build();
        }
    }

    @Nested
    @DisplayName("Tests for logout operation endpoint")
    class LogoutTests {
        @Test
        void should_Return_204_And_Successful_Logout() {
            Long userId = 1L;
            String email = "test@mail.com";
            AppRole userRole = AppRole.USER;
            String accessToken = jwtService.generateAccessToken(userId, email, userRole);
            String refreshToken = jwtService.generateRefreshToken(userId, email, userRole);

            Cookie accessTokenCookie = new Cookie.Builder("accessToken", accessToken)
                    .setHttpOnly(true)
                    .setPath("/")
                    .setMaxAge(60 * 60)
                    .build();

            Cookie refreshTokenCookie = new Cookie.Builder("refreshToken", refreshToken)
                    .setHttpOnly(true)
                    .setPath("/")
                    .setMaxAge(7 * 24 * 60 * 60)
                    .build();

            RestAssured.given()
                    .contentType("application/json")
                    .cookie(accessTokenCookie)
                    .cookie(refreshTokenCookie)
                    .post("/api/v1/auth/logout")
                    .then()
                    .log().all()
                    .statusCode(204);
        }
    }
}
