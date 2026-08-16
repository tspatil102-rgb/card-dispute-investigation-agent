package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HomeControllerTest {

    @Test
    void homeEndpointReturnsGreeting() {
        HomeController controller = new HomeController();

        assertThat(controller.home()).isEqualTo("Hello from Spring Boot!");
    }
}
