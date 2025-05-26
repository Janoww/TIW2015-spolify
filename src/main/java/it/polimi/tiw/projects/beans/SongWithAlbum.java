package it.polimi.tiw.projects.beans;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class SongWithAlbum implements Serializable {
    @NotNull
    private Song song;
    @NotNull
    private Album album;

    public SongWithAlbum(@NotNull Song song, @NotNull Album album) {
        this.song = song;
        this.album = album;
    }

    @NotNull
    public Song getSong() {
        return song;
    }

    public void setSong(@NotNull Song song) {
        this.song = song;
    }

    @NotNull
    public Album getAlbum() {
        return album;
    }

    public void setAlbum(@NotNull Album album) {
        this.album = album;
    }

}
