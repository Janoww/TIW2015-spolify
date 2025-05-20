package it.polimi.tiw.projects.beans;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.polimi.tiw.projects.utils.Genre;

public class Song implements Serializable {

	private int idSong;
	private String title;
	private int idAlbum;
	private int year;
	private Genre genre;
	@JsonIgnore
	private String audioFile;
	private UUID idUser;

	// Getters and Setters

	public int getIdSong() {
		return idSong;
	}

	public void setIdSong(int idSong) {
		this.idSong = idSong;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getIdAlbum() {
		return idAlbum;
	}

	public void setIdAlbum(int idAlbum) {
		this.idAlbum = idAlbum;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public Genre getGenre() {
		return genre;
	}

	public void setGenre(Genre genre) {
		this.genre = genre;
	}

	public String getAudioFile() {
		return audioFile;
	}

	public void setAudioFile(String audioFile) {
		this.audioFile = audioFile;
	}

	public UUID getIdUser() {
		return idUser;
	}

	public void setIdUser(UUID idUser) {
		this.idUser = idUser;
	}
}
