package it.polimi.tiw.projects.beans;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
// Consider jakarta.validation.constraints.NotEmpty for songIds list if it cannot be empty

public class PlaylistCreationRequest {
    @NotBlank(message = "Playlist name is required.")
    @Pattern(regexp = "^[a-zA-Z0-9\\s_!?.',-]{1,100}$", message = "Invalid playlist name format.")
    @Size(min = 1, max = 100, message = "Playlist name must be between 1 and 100 characters.")
    private String name;

    @NotNull(message = "Song IDs list cannot be null (can be empty if creating an empty playlist is allowed).")
    private List<Integer> songIds;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Integer> getSongIds() {
        return songIds;
    }

    public void setSongIds(List<Integer> songIds) {
        this.songIds = songIds;
    }
}
