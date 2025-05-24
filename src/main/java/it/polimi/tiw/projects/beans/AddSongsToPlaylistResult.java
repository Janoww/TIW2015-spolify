package it.polimi.tiw.projects.beans;

import java.util.ArrayList;
import java.util.List;

public class AddSongsToPlaylistResult {
    private List<Integer> addedSongIds = new ArrayList<>();
    private List<Integer> duplicateSongIds = new ArrayList<>();

    // Getters
    public List<Integer> getAddedSongIds() {
        return addedSongIds;
    }

    public List<Integer> getDuplicateSongIds() {
        return duplicateSongIds;
    }

    // Methods to populate lists (used by DAO)
    public void addSuccessfullyAddedSong(int songId) {
        this.addedSongIds.add(songId);
    }

    public void addDuplicateSong(int songId) {
        this.duplicateSongIds.add(songId);
    }

}
