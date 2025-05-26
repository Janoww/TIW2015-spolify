package it.polimi.tiw.projects.beans;

import it.polimi.tiw.projects.utils.Genre;
import jakarta.servlet.http.Part;
import jakarta.validation.constraints.*;

public record SongCreationParameters(
        @NotBlank(message = "Song title is required.") @Size(min = 1, max = 100, message = "Song title must be between 1 and 100 characters.") String songTitle,
        @NotBlank(message = "Album title is required.") @Size(min = 1, max = 100, message = "Album title must be between 1 and 100 characters.") String albumTitle,
        @NotBlank(message = "Album artist is required.") @Size(min = 1, max = 100, message = "Album artist must be between 1 and 100 characters.") String albumArtist,
        @Min(value = 1000, message = "Album year must be a valid year (e.g., >= 1000).") @Max(value = 9999, message = "Album year must be a valid year (e.g., <= 9999).") int albumYear,
        @NotNull(message = "Genre is required.") Genre genre,
        @NotNull(message = "Audio file is required.") Part audioFilePart) {
}
