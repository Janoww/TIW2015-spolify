package it.polimi.tiw.projects.beans;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Album implements Serializable {

	private int idAlbum;
	private String name;
	private int year;
	private String artist;
	@JsonIgnore
	private String imageFile;
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
