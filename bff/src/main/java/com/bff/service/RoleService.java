package com.bff.service;

import com.bff.model.Role;
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
public class RoleService {

    private final RestTemplate restTemplate;
    private final String functionsBaseUrl;

    @Autowired
    public RoleService(RestTemplate restTemplate, @Value("${azure.functions.baseUrl}") String functionsBaseUrl) {
        this.restTemplate = restTemplate;
        this.functionsBaseUrl = functionsBaseUrl;
    }

    public List<Role> getAllRoles() {
        ResponseEntity<List<Role>> response = restTemplate.exchange(
                functionsBaseUrl + "/roles",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Role>>() {
                });
        return response.getBody();
    }

    public Role getRoleById(Long id) {
        return restTemplate.getForObject(functionsBaseUrl + "/roles/" + id, Role.class);
    }

    public Role createRole(Role role) {
        return restTemplate.postForObject(functionsBaseUrl + "/roles", role, Role.class);
    }

    public Role updateRole(Long id, Role role) {
        HttpEntity<Role> requestEntity = new HttpEntity<>(role);
        ResponseEntity<Role> response = restTemplate.exchange(
                functionsBaseUrl + "/roles/" + id,
                HttpMethod.PUT,
                requestEntity,
                Role.class);
        return response.getBody();
    }

    public void deleteRole(Long id) {
        restTemplate.delete(functionsBaseUrl + "/roles/" + id);
    }

    public List<Role> getUserRoles(Long userId) {
        ResponseEntity<List<Role>> response = restTemplate.exchange(
                functionsBaseUrl + "/users/" + userId + "/roles",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Role>>() {
                });
        return response.getBody();
    }

    public void assignRoleToUser(Long userId, Long roleId) {
        restTemplate.postForObject(functionsBaseUrl + "/users/" + userId + "/roles/" + roleId, null, String.class);
    }

    public void removeRoleFromUser(Long userId, Long roleId) {
        restTemplate.delete(functionsBaseUrl + "/users/" + userId + "/roles/" + roleId);
    }
}