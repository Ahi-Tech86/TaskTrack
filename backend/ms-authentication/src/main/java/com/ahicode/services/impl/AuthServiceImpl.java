package com.ahicode.services.impl;

import com.ahicode.dtos.*;
import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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
    private final TemporaryUserDtoFactory temporaryUserDtoFactory;

    private final RedisTemplate<String, Integer> integerRedisTemplate;
    private final RedisTemplate<String, TemporaryUserDto> redisTemplate;

    @Autowired
    public AuthServiceImpl(
            JwtService jwtService,
            UserDtoFactory dtoFactory,
            UserRepository repository,
            TokenService tokenService,
            EmailService emailService,
            UserEntityFactory entityFactory,
            PasswordEncoder passwordEncoder,
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
        return confirmUser(email, confirmationCode, temporaryUserDto, false);
    }

    @Override
    @Transactional
    public UserDto confirmAdmin(ConfirmationRegisterRequest confirmationRegisterRequest) {
        String email = confirmationRegisterRequest.getEmail();
        String confirmationCode = confirmationRegisterRequest.getConfirmationCode();

        TemporaryUserDto temporaryUserDto = redisTemplate.opsForValue().get(email);
        return confirmUser(email, confirmationCode, temporaryUserDto, true);
    }

    @Override
    public List<Object> login(SignInRequest signInRequest) {
        List<Object> response = new ArrayList<>();
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
            String refreshToken = tokenService.getTokenByUserNickname(nickname);

            response.add(userDto);
            response.add(accessToken);
            response.add(refreshToken);

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
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";

        Pattern pattern = Pattern.compile(emailRegex);

        if (email == null) {
            return false;
        }

        return pattern.matcher(email).matches();
    }

    private void isEmailUniqueness(String email) {
        Optional<UserEntity> optionalUser = repository.findByEmail(email);

        if (optionalUser.isPresent()) {
            log.error("Attempt to register with an existing email {}", email);
            throw new AppException(String.format("User with email %s is already exists", email), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isLockedLogin(String lockKey) {
        Boolean isLocked = integerRedisTemplate.hasKey(lockKey);

        return isLocked != null && isLocked;
    }

    private void isNicknameUniqueness(String nickname) {
        Optional<UserEntity> optionalUser = repository.findByNickname(nickname);

        if (optionalUser.isPresent()) {
            log.error("Attempt to with an existing nickname {}", nickname);
            throw new AppException(String.format("User with nickname %s is already exists", nickname), HttpStatus.BAD_REQUEST);
        }
    }

    private UserEntity isUserExistsByEmail(String email) {
        Optional<UserEntity> optionalUser = repository.findByEmail(email);

        if (optionalUser.isPresent()) {
            return optionalUser.get();
        } else {
            log.error("Attempt to log into an account with non-existent email {}", email);
            throw new AppException(String.format("User with email %s doesn't exists", email), HttpStatus.NOT_FOUND);
        }
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

    private UserDto confirmUser(String email, String confirmationCode, TemporaryUserDto temporaryUserDto, boolean isAdmin) {
        if (!confirmationCode.equals(temporaryUserDto.getConfirmationCode())) {
            log.error("Attempting to activate an account with an incorrect confirmation code to email {}", email);
            throw new AppException("The confirmation code doesn't match what the server generated", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user;
        if (!isAdmin) {
            user = entityFactory.makeUserEntity(temporaryUserDto);
        } else {
            user = entityFactory.makeAdminUserEntity(temporaryUserDto);
        }

        user.setPassword(passwordEncoder.encode(temporaryUserDto.getPassword()));

        UserEntity savedUser = repository.saveAndFlush(user);
        log.info("User with email {} was successfully saved", email);

        tokenService.createAndSaveToken(user);
        log.info("Refresh token for {} user was successfully saved", email);

        return dtoFactory.makeUserDto(savedUser);
    }
}
