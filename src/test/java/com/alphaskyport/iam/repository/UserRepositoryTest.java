package com.alphaskyport.iam.repository;

import com.alphaskyport.iam.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testSaveUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setUserType("private");
        user.setFirstName("John");
        user.setLastName("Doe");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getUserId()).isNotNull();
        assertThat(savedUser.getVersion()).isEqualTo(0); // Should be 0 initially (or null depending on hibernate
                                                         // config, but usually 0 or 1)
    }
}
