package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void login_missingUsername_returnsBadRequest() {
        Map<String, String> body = Map.of("password", "pwd");

        ResponseEntity<Object> res = userController.login(body);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>)res.getBody()).get("error")).isEqualTo("Username requis");
    }

    @Test
    void login_missingPassword_returnsBadRequest() {
        Map<String, String> body = Map.of("username", "u");

        ResponseEntity<Object> res = userController.login(body);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>)res.getBody()).get("error")).isEqualTo("Password requis");
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() {
        Map<String, String> body = Map.of("username", "u", "password", "p");

        when(userService.authenticate(anyString(), anyString())).thenReturn(null);

        ResponseEntity<Object> res = userController.login(body);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(((Map<?, ?>)res.getBody()).get("error")).isEqualTo("Identifiants incorrects");
    }

    @Test
    void login_success_returnsUserMap() {
        Map<String, String> body = Map.of("username", "alice", "password", "secret");

        User user = new User("alice", "secret");
        user.setId(42L);
        when(userService.authenticate("alice", "secret")).thenReturn(user);

        ResponseEntity<Object> res = userController.login(body);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?,?> map = (Map<?,?>) res.getBody();
        assertThat(map.get("id")).isEqualTo(42L);
        assertThat(map.get("username")).isEqualTo("alice");
    }

    @Test
    void register_success_returnsCreated() {
        User input = new User("bob","pw");
        User created = new User("bob","pw");
        created.setId(7L);

        when(userService.registerUser(anyString(), anyString())).thenReturn(created);

        ResponseEntity<Object> res = userController.register(input);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<?,?> body = (Map<?,?>) res.getBody();
        assertThat(body.get("id")).isEqualTo(7L);
        assertThat(body.get("username")).isEqualTo("bob");
    }

    @Test
    void register_failure_returnsBadRequest() {
        User input = new User("bob","pw");
        when(userService.registerUser(anyString(), anyString())).thenThrow(new RuntimeException("dup"));

        ResponseEntity<Object> res = userController.register(input);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?,?>)res.getBody()).get("error")).isEqualTo("dup");
    }

    @Test
    void getAll_returnsSimpleList() {
        User u1 = new User("a","p"); u1.setId(1L);
        User u2 = new User("b","p"); u2.setId(2L);

        when(userService.getAllUsers()).thenReturn(List.of(u1,u2));

        ResponseEntity<List<Map<String, Object>>> res = userController.getAll();

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(2);
        assertThat(res.getBody().get(0).get("id")).isEqualTo(1L);
    }

    @Test
    void getByUsername_notFound_returns404() {
        when(userService.getUserByUsername("notfound")).thenReturn(null);

        ResponseEntity<Object> res = userController.getByUsername("notfound");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getByUsername_found_returnsUser() {
        User u = new User("c","p"); u.setId(5L);
        when(userService.getUserByUsername("c")).thenReturn(u);

        ResponseEntity<Object> res = userController.getByUsername("c");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(u);
    }
}