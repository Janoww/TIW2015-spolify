package it.polimi.tiw.projects.beans;

public class SongWithAlbum {
    private Song song;
    private Album album;

    public SongWithAlbum(Song song, Album album) {
        this.song = song;
        this.album = album;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

}
