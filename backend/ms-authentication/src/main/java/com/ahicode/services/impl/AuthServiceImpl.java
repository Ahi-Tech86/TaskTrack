package com.ahicode.services.impl;

import com.ahicode.dtos.*;
import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
import com.ahicode.factories.AuthResponseFactory;
import com.ahicode.factories.TemporaryUserDtoFactory;
import com.ahicode.factories.UserDtoFactory;
import com.ahicode.factories.UserEntityFactory;
import com.ahicode.services.AuthService;
import com.ahicode.services.EmailService;
import com.ahicode.services.JwtService;
import com.ahicode.services.TokenService;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final UserDtoFactory dtoFactory;
    private final UserRepository repository;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final UserEntityFactory entityFactory;
    private final PasswordEncoder passwordEncoder;
    private final AuthResponseFactory authResponseFactory;
    private final TemporaryUserDtoFactory temporaryUserDtoFactory;

    private final RedisTemplate<String, Integer> integerRedisTemplate;
    private final RedisTemplate<String, TemporaryUserDto> redisTemplate;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    @Autowired
    public AuthServiceImpl(
            JwtService jwtService,
            UserDtoFactory dtoFactory,
            UserRepository repository,
            TokenService tokenService,
            EmailService emailService,
            UserEntityFactory entityFactory,
            PasswordEncoder passwordEncoder,
            AuthResponseFactory authResponseFactory,
            TemporaryUserDtoFactory temporaryUserDtoFactory,
            @Qualifier("redisTemplate") RedisTemplate<String, TemporaryUserDto> redisTemplate,
            @Qualifier("integerRedisTemplate") RedisTemplate<String, Integer> integerRedisTemplate
    ) {
        this.jwtService = jwtService;
        this.dtoFactory = dtoFactory;
        this.repository = repository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.entityFactory = entityFactory;
        this.passwordEncoder = passwordEncoder;
        this.authResponseFactory = authResponseFactory;
        this.temporaryUserDtoFactory = temporaryUserDtoFactory;
        this.redisTemplate = redisTemplate;
        this.integerRedisTemplate = integerRedisTemplate;
    }

    @Override
    public String register(SignUpRequest signUpRequest) {
        String email = signUpRequest.getEmail();
        String nickname = signUpRequest.getNickname();

        if (!isEmailValid(email)) {
            log.error("Attempt register with invalid email {}", email);
            throw new AppException("Email is invalid", HttpStatus.BAD_REQUEST);
        }

        isEmailUniqueness(email);
        isNicknameUniqueness(nickname);

        String confirmationCode = generateCode();

        TemporaryUserDto temporaryUserDto = temporaryUserDtoFactory.makeTemporaryUserDto(signUpRequest, confirmationCode);
        redisTemplate.opsForValue().set(email, temporaryUserDto, 20, TimeUnit.MINUTES);
        log.info("User information with email {} is temporarily saved", email);

        try {
            emailService.sendConfirmationCode(email, confirmationCode);
            log.info("Message with activation code was send to email {}", email);
        } catch (RuntimeException exception) {
            log.error("Attempt to send message was unsuccessful", exception);
            throw new AppException("There was an error sending the message", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return "An activation code has been sent to your email, please send the activation code before it expires. " +
                "The activation code expires in 20 minutes.";
    }

    @Override
    @Transactional
    public UserDto confirm(ConfirmationRegisterRequest confirmationRegisterRequest) {
        String email = confirmationRegisterRequest.getEmail();
        String confirmationCode = confirmationRegisterRequest.getConfirmationCode();

        TemporaryUserDto temporaryUserDto = redisTemplate.opsForValue().get(email);
        return confirmUser(email, confirmationCode, temporaryUserDto);
    }

    @Override
    public AuthResponse login(SignInRequest signInRequest) {
        String email = signInRequest.getEmail();

        String failedLoginKey = "failedLogin:" + email;
        String lockKey = "locked:" + email;

        if (isLockedLogin(lockKey)) {
            log.info("Attempting to log into your account {} when the limit of attempts has been exceeded", email);
            throw new AppException(
                    "Due to an incorrect password entry, we have temporarily blocked you from " +
                            "logging into your account. Come back later", HttpStatus.BAD_REQUEST
            );
        }

        UserEntity user = isUserExistsByEmail(email);
        String nickname = user.getNickname();

        if (passwordEncoder.matches(signInRequest.getPassword(), user.getPassword())) {
            integerRedisTemplate.delete(failedLoginKey);

            Long userId = user.getId();
            AppRole role = user.getRole();
            UserDto userDto = dtoFactory.makeUserDto(user);

            String accessToken = jwtService.generateAccessToken(userId, nickname, role);
            String refreshToken = jwtService.generateRefreshToken(userId, nickname, role);

            AuthResponse response = authResponseFactory.makeAuthResponse(userDto, accessToken, refreshToken);

            log.info("Successful login to {} account", email);
            return response;
        } else {
            handleFailedLogin(failedLoginKey, lockKey);

            log.error("Attempt to log with incorrect password to {} account", email);
            throw new AppException(
                    "Incorrect password", HttpStatus.UNAUTHORIZED
            );
        }
    }

    private String generateCode() {
        Random random = new Random();

        int number = 1 + random.nextInt(1000000);

        return String.format("%6d", number);
    }

    private boolean isEmailValid(String email) {
        if (email == null) {
            return false;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    private void isEmailUniqueness(String email) {
        checkUniqueness(
                "email", email, repository::findByEmail, "User with email %s is already exists"
        );
    }

    private boolean isLockedLogin(String lockKey) {
        Boolean isLocked = integerRedisTemplate.hasKey(lockKey);

        return isLocked != null && isLocked;
    }

    private void isNicknameUniqueness(String nickname) {
        checkUniqueness(
                "nickname", nickname,
                repository::findByNickname, "User with nickname %s is already exists"
        );
    }

    private UserEntity isUserExistsByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(
                () -> {
                    log.error("Attempt to log into an account with non-existent email {}", email);
                    throw new AppException(String.format("User with email %s doesn't exists", email), HttpStatus.NOT_FOUND);
                }
        );
    }

    private void handleFailedLogin(String failedLoginKey, String lockKey) {
        Integer failedAttempts = integerRedisTemplate.opsForValue().get(failedLoginKey);

        if (failedAttempts == null) {
            integerRedisTemplate.opsForValue().set(failedLoginKey, 1);
        } else {
            integerRedisTemplate.opsForValue().set(failedLoginKey, failedAttempts + 1);

            if (failedAttempts % 3 == 2) {
                integerRedisTemplate.opsForValue().set(lockKey, 5, 5, TimeUnit.MINUTES);
            }
        }
    }

    private UserDto confirmUser(String email, String confirmationCode, TemporaryUserDto temporaryUserDto) {
        if (!confirmationCode.equals(temporaryUserDto.getConfirmationCode())) {
            log.error("Attempting to activate an account with an incorrect confirmation code to email {}", email);
            throw new AppException("The confirmation code doesn't match what the server generated", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = entityFactory.makeUserEntity(temporaryUserDto);

        user.setPassword(passwordEncoder.encode(temporaryUserDto.getPassword()));

        UserEntity savedUser = repository.saveAndFlush(user);
        log.info("User with email {} was successfully saved", email);

        tokenService.createAndSaveToken(user);
        log.info("Refresh token for {} user was successfully saved", email);

        return dtoFactory.makeUserDto(savedUser);
    }

    private void checkUniqueness(
            String varName, String value, Function<String, Optional<UserEntity>> findFunction, String errorMessage
    ) {
        findFunction.apply(value)
                .ifPresent(
                        user -> {
                            log.error("Attempt to register with an existing {} {}", varName, value);
                            throw new AppException(String.format(errorMessage, value), HttpStatus.BAD_REQUEST);
                        }
                );
    }
}
