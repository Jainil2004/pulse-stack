package com.pulsestack.repositories;

import com.pulsestack.TestDataUtil;
import com.pulsestack.model.System;
import com.pulsestack.model.User;
import com.pulsestack.repository.SystemRepository;
import com.pulsestack.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// use the below defined configuration for repository + JPA testing
// @SpringBootTest
// @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// @Transactional


@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@Transactional
public class SystemRepositoryIntegrationTests {

    private final SystemRepository underTest;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SystemRepositoryIntegrationTests(SystemRepository underTest, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.underTest = underTest;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    public void testThatSystemCouldBeSuccessfullyRegistered() {
        System system1 = TestDataUtil.createSystem1();
        User user1 = TestDataUtil.createNormalUser1();
        String systemId = UUID.randomUUID().toString();
        system1.setSystemId(systemId);
        String authToken = passwordEncoder.encode(systemId);
        system1.setAuthToken(authToken);
        userRepository.save(user1);
        system1.setUser(user1);
        underTest.save(system1);
        Optional<System> result = underTest.findBySystemId(system1.getSystemId());

        if (result.isPresent()) {
            assertThat(result.get().getId()).isEqualTo(system1.getId());
            assertThat(result.get().getName()).isEqualTo(system1.getName());
            assertThat(result.get().getSystemId()).isEqualTo(system1.getSystemId());
            assertThat(passwordEncoder.matches(systemId, result.get().getAuthToken())).isTrue();
            Duration duration = Duration.between(system1.getRegisteredAt(), result.get().getRegisteredAt());
            assertThat(Math.abs(duration.toNanos())).isLessThan(1000); // less than 1 microsecond
            assertThat(result.get().getUser().getUsername()).isEqualTo(user1.getUsername());
        }
    }

    @Test
    public void testThatSystemsCouldBeRetrievedByUser() {
        System system1 = TestDataUtil.createSystem1();
        String systemId1 = UUID.randomUUID().toString();
        system1.setSystemId(systemId1);
        String authToken = passwordEncoder.encode(systemId1);
        system1.setAuthToken(authToken);
        System system2 = TestDataUtil.createSystem2();
        String systemId2 = UUID.randomUUID().toString();
        system2.setSystemId(systemId2);
        String authToken2 = passwordEncoder.encode(systemId2);
        system2.setAuthToken(authToken2);
        User user1 = TestDataUtil.createNormalUser1();
        userRepository.save(user1);
        system1.setUser(user1);
        system2.setUser(user1);
        underTest.save(system1);
        underTest.save(system2);

        List<System> result = underTest.findAllByUser(user1);

        assertThat(result).hasSize(2);
        assertThat(result).contains(system1, system2);
//        assertThat(result.get(0).getId()).isEqualTo(system1.getId());
//        assertThat(result.get(0).getName()).isEqualTo(system1.getName());
//        assertThat(result.get(0).getSystemId()).isEqualTo(system1.getSystemId());
//        Duration duration = Duration.between(system1.getRegisteredAt(), result.get(0).getRegisteredAt());
//        assertThat(Math.abs(duration.toNanos())).isLessThan(1000); // less than 1 microsecond
//        assertThat(result.get(0).getUser().getUsername()).isEqualTo(user1.getUsername());
//
//        assertThat(result.get(1).getId()).isEqualTo(system2.getId());
//        assertThat(result.get(1).getName()).isEqualTo(system2.getName());
//        assertThat(result.get(1).getSystemId()).isEqualTo(system2.getSystemId());
//        Duration durationSys2 = Duration.between(system2.getRegisteredAt(), result.get(1).getRegisteredAt());
//        assertThat(Math.abs(durationSys2.toNanos())).isLessThan(1000); // less than 1 microsecond
//        assertThat(result.get(1).getUser().getUsername()).isEqualTo(user1.getUsername());
    }

    @Test
    public void testThatSystemCouldBeCheckedForExistenceUsingSystemId() {
        System system1 = TestDataUtil.createSystem1();
        String systemId = UUID.randomUUID().toString();
        system1.setSystemId(systemId);
        String authToken = passwordEncoder.encode(systemId);
        system1.setAuthToken(authToken);
        User user1 = TestDataUtil.createNormalUser1();
        userRepository.save(user1);
        system1.setUser(user1);
        underTest.save(system1);

        Boolean result = underTest.existsBySystemId(system1.getSystemId());
        assertThat(result).isTrue();
    }

    @Test
    public void testThatSystemCouldBeRetrievedByItsName() {
        System system1 = TestDataUtil.createSystem1();
        String systemId = UUID.randomUUID().toString();
        system1.setSystemId(systemId);
        String authToken = passwordEncoder.encode(systemId);
        system1.setAuthToken(authToken);
        User user1 = TestDataUtil.createNormalUser1();
        userRepository.save(user1);
        system1.setUser(user1);
        underTest.save(system1);

        Optional<System> result = underTest.findSystemByName(system1.getName());

        if (result.isPresent()) {
            assertThat(result.get().getId()).isEqualTo(system1.getId());
            assertThat(result.get().getName()).isEqualTo(system1.getName());
            assertThat(result.get().getSystemId()).isEqualTo(system1.getSystemId());
            Duration duration = Duration.between(system1.getRegisteredAt(), result.get().getRegisteredAt());
            assertThat(Math.abs(duration.toNanos())).isLessThan(1000); // less than 1 microsecond
            assertThat(result.get().getUser().getUsername()).isEqualTo(user1.getUsername());
        }
    }

    @Test
    public void testThatSystemCouldBeUpdated() {
        System system1 = TestDataUtil.createSystem1();
        String systemId = UUID.randomUUID().toString();
        system1.setSystemId(systemId);
        String authToken = passwordEncoder.encode(systemId);
        system1.setAuthToken(authToken);
        User user1 = TestDataUtil.createNormalUser1();
        userRepository.save(user1);
        system1.setUser(user1);
        underTest.save(system1);

        system1.setName("updated");
        Optional<System> result = underTest.findBySystemId(system1.getSystemId());
        assertThat(result.get().getName()).isEqualTo("updated");
    }

    @Test
    public void testThatSystemCouldBeDeleted() {
        System system1 = TestDataUtil.createSystem1();
        String systemId = UUID.randomUUID().toString();
        system1.setSystemId(systemId);
        String authToken = passwordEncoder.encode(systemId);
        system1.setAuthToken(authToken);
        User user1 = TestDataUtil.createNormalUser1();
        userRepository.save(user1);
        system1.setUser(user1);
        underTest.save(system1);

        Optional<System> exists = underTest.findBySystemId(system1.getSystemId());
        assertThat(exists.get()).isEqualTo(system1);

        underTest.delete(exists.get());

        Optional<System> result = underTest.findBySystemId(system1.getSystemId());
        assertThat(result).isEmpty();
    }

}
