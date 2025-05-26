package it.polimi.tiw.projects.beans;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PlaylistAddSongsRequest {
    private @NotNull List<Integer> songIds;

    // Getter and Setter
    public List<Integer> getSongIds() {
        return songIds;
    }

    public void setSongIds(@NotNull List<Integer> songIds) {
        this.songIds = songIds;
    }
}
