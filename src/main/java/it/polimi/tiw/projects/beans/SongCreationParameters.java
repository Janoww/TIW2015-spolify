package it.polimi.tiw.projects.beans;

import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.http.Part;

public class SongCreationParameters {
    private final String songTitle;
    private final String albumTitle;
    private final String albumArtist;
    private final int albumYear;
    private final Genre genre;
    private final Part audioFilePart;

    public SongCreationParameters(String songTitle, String albumTitle, String albumArtist,
            int albumYear, Genre genre, Part audioFilePart) {
        this.songTitle = songTitle;
        this.albumTitle = albumTitle;
        this.albumArtist = albumArtist;
        this.albumYear = albumYear;
        this.genre = genre;
        this.audioFilePart = audioFilePart;
    }

    // Getters
    public String getSongTitle() {
        return songTitle;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public int getAlbumYear() {
        return albumYear;
    }

    public Genre getGenre() {
        return genre;
    }

    public Part getAudioFilePart() {
        return audioFilePart;
    }
}
