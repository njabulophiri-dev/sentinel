package dev.sentinel.config;

import dev.sentinel.persistence.Role;
import dev.sentinel.persistence.UserEntity;
import dev.sentinel.persistence.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin, analyst and service accounts from configuration if they do not exist yet. */
@Component
class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final BootstrapProperties properties;
    private final Clock clock;

    DataInitializer(UserRepository users, PasswordEncoder encoder, BootstrapProperties properties, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        create("admin", properties.adminPassword(), Role.ADMIN);
        create("analyst", properties.analystPassword(), Role.ANALYST);
        create("service", properties.servicePassword(), Role.SERVICE);
    }

    private void create(String username, String password, Role role) {
        if (password == null || password.isBlank() || users.findByUsername(username).isPresent()) {
            return;
        }
        users.save(new UserEntity(username, encoder.encode(password), role, clock.instant()));
        log.info("Created {} user '{}'", role, username);
    }
}
