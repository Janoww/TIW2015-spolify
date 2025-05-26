package it.polimi.tiw.projects.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

public class Album implements Serializable {

    private int idAlbum;
    @NotBlank
    private String name;
    private int year;
    @NotBlank
    private String artist;
    @JsonIgnore
    private String imageFile;
    @NotNull
    private UUID idUser;

    // Getters and Setters

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String image) {
        this.imageFile = image;
    }

    public int getIdAlbum() {
        return idAlbum;
    }

    public void setIdAlbum(int idAlbum) {
        this.idAlbum = idAlbum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public UUID getIdUser() {
        return idUser;
    }

    public void setIdUser(UUID idUser) {
        this.idUser = idUser;
    }
}
