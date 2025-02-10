package integrationTests;

import com.ahicode.AuthenticationServiceRunner;
import com.ahicode.dtos.SignUpRequest;
import com.ahicode.enums.AppRole;
import com.ahicode.services.EmailService;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.UserRepository;
import com.redis.testcontainers.RedisContainer;
import io.github.cdimascio.dotenv.Dotenv;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Instant;

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

    @Autowired
    private UserRepository userRepository;
    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        userRepository.deleteAll();
    }

    static {
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

    @Test
    void should_Return_200_And_Register_Successfully() throws Exception {
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
