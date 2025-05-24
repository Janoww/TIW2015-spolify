package it.polimi.tiw.projects.beans;

import java.util.List;

import jakarta.validation.constraints.NotNull;

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
