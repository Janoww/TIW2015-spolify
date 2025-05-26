package it.polimi.tiw.projects.beans;

import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.http.Part;
import jakarta.validation.constraints.*;

public class SongCreationParameters {

    private final String songTitle;
    private final String albumTitle;
    private final String albumArtist;
    private final int albumYear;
    private final Genre genre;
    private final Part audioFilePart;

    public SongCreationParameters(
            @NotBlank(message = "Song title is required.") @Size(min = 1, max = 100, message = "Song title must be between 1 and 100 characters.") String songTitle,

            @NotBlank(message = "Album title is required.") @Size(min = 1, max = 100, message = "Album title must be between 1 and 100 characters.") String albumTitle,

            @NotBlank(message = "Album artist is required.") @Size(min = 1, max = 100, message = "Album artist must be between 1 and 100 characters.") String albumArtist,

            @Min(value = 1000, message = "Album year must be a valid year (e.g., >= 1000).") @Max(value = 9999, message = "Album year must be a valid year (e.g., <= 9999).") int albumYear,

            @NotNull(message = "Genre is required.") Genre genre,

            @NotNull(message = "Audio file is required.") Part audioFilePart) {
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
