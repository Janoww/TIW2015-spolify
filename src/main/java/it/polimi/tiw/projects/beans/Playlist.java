package it.polimi.tiw.projects.beans;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// Consider jakarta.validation.constraints.NotEmpty for songs list if it cannot be empty

public class Playlist implements Serializable {

	private int idPlaylist;
	@NotBlank
	private String name;
	@NotNull
	private Timestamp birthday;
	@NotNull
	private UUID idUser;
	@NotNull
	private List<Integer> songs;

	// Getters and Setters

	public int getIdPlaylist() {
		return idPlaylist;
	}

	public void setIdPlaylist(int idPlaylist) {
		this.idPlaylist = idPlaylist;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Timestamp getBirthday() {
		return birthday;
	}

	public void setBirthday(Timestamp birthday) {
		this.birthday = birthday;
	}

	public UUID getIdUser() {
		return idUser;
	}

	public void setIdUser(UUID idUser) {
		this.idUser = idUser;
	}

	public List<Integer> getSongs() {
		return songs;
	}

	public void setSongs(List<Integer> songs) {
		this.songs = songs;
	}
}
