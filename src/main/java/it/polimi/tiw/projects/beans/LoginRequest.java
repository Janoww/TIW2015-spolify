package it.polimi.tiw.projects.beans;

import java.io.Serializable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest implements Serializable {
    @NotBlank(message = "Username is required and cannot be empty.")
    @Pattern(regexp = "^\\w{3,100}$", message = "Invalid username format. Use alphanumeric characters or underscores (3-100 characters).")
    private String username;

    @NotBlank(message = "Password is required and cannot be empty.")
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters.")
    private String password;

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
}
