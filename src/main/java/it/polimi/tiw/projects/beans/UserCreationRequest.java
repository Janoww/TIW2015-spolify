package it.polimi.tiw.projects.beans;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public class UserCreationRequest implements Serializable {
    @NotBlank(message = "Username is required.")
    @Pattern(regexp = "^\\w{3,100}$", message = "Invalid username format.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters.")
    private String password;

    @NotBlank(message = "Name is required.")
    @Pattern(regexp = "^[a-zA-Z\\s'-]{3,100}$", message = "Invalid name format.")
    private String name;

    @NotBlank(message = "Surname is required.")
    @Pattern(regexp = "^[a-zA-Z\\s'-]{3,100}$", message = "Invalid surname format.")
    private String surname;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}
