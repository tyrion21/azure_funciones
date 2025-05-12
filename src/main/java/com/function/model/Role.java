package com.function.model;

import java.util.UUID;

public class Role {
    private String id;
    private String name;
    private String description;
    private boolean isActive;

    // Constructores
    public Role() {
        this.id = UUID.randomUUID().toString();
        this.isActive = true;
    }

    public Role(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}