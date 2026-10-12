package com.example.todo.support;

import com.example.todo.repository.UserRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Stand-ins for beans the security chain needs but web slices do not load. */
@TestConfiguration(proxyBeanMethods = false)
public class ApiSliceTestBeans {

    @Bean
    UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }
}
