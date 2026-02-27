package com.project.rupayaBackend.repository;

import com.project.rupayaBackend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void getAllUsers() {
        List<User> users = userRepository.findAll();
        System.out.println(users);
    }

    @Test
    public void findByEmail() {
        Optional<User> user = userRepository.findByEmail("demo@example.com");
        user.stream().map(User::getName).forEach(System.out::println);
    }
}
