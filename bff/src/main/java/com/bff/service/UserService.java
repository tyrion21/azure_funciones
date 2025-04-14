package com.bff.service;

import com.bff.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class UserService {

    private final RestTemplate restTemplate;
    private final String functionsBaseUrl;

    @Autowired
    public UserService(RestTemplate restTemplate, @Value("${azure.functions.baseUrl}") String functionsBaseUrl) {
        this.restTemplate = restTemplate;
        this.functionsBaseUrl = functionsBaseUrl;
    }

    public List<User> getAllUsers() {
        ResponseEntity<List<User>> response = restTemplate.exchange(
                functionsBaseUrl + "/users",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<User>>() {
                });
        return response.getBody();
    }

    public User getUserById(Long id) {
        return restTemplate.getForObject(functionsBaseUrl + "/users/" + id, User.class);
    }

    public User createUser(User user) {
        return restTemplate.postForObject(functionsBaseUrl + "/users", user, User.class);
    }

    public User updateUser(Long id, User user) {
        HttpEntity<User> requestEntity = new HttpEntity<>(user);
        ResponseEntity<User> response = restTemplate.exchange(
                functionsBaseUrl + "/users/" + id,
                HttpMethod.PUT,
                requestEntity,
                User.class);
        return response.getBody();
    }

    public void deleteUser(Long id) {
        restTemplate.delete(functionsBaseUrl + "/users/" + id);
    }
}