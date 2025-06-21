package com.pulsestack.repositories;

import com.pulsestack.TestDataUtil;
import com.pulsestack.model.User;
import com.pulsestack.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class UserRepositoryIntegrationTests {

    @Autowired
    private UserRepository underTest;

    @Test
    public void testThatUserCanBeCreatedAndRecalled() {
        User user = TestDataUtil.createNormalUser1();
        underTest.save(user);
        Optional<User> result = underTest.findById(user.getId());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user);
    }

    @Test
    public void testThatMultipleUsersCanBeCreatedAndRecalled() {
        User user1 = TestDataUtil.createNormalUser1();
        underTest.save(user1);
        User user2 = TestDataUtil.createNormalUser2();
        underTest.save(user2);
        User user3 = TestDataUtil.createNormalUser3();
        underTest.save(user3);

        Iterable<User> result = underTest.findAll();
        assertThat(result).hasSize(3);
        assertThat(result).contains(user1, user2, user3);
    }

    @Test
    public void testThatUserCanBeUpdated() {
        User user1 = TestDataUtil.createNormalUser1();
        underTest.save(user1);
        user1.setUsername("updated_username");
        underTest.save(user1);
        Optional<User> result = underTest.findById(user1.getId());
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user1);
    }

    @Test
    public void testThatUserCanBeDeleted() {
        User user1 = TestDataUtil.createNormalUser2();
        underTest.save(user1);

        underTest.deleteById(user1.getId());
        Optional<User> result = underTest.findById(user1.getId());
        assertThat(result).isEmpty();
    }

    @Test
    public void testThatUserCanBeRecalledUsingUserName() {
        User user1 = TestDataUtil.createNormalUser1();
        underTest.save(user1);

        Optional<User> result = underTest.findByUsername(user1.getUsername());
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(user1);
    }
}
