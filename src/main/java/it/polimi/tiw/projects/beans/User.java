package it.polimi.tiw.projects.beans;

import java.util.UUID;

public class User {

	private UUID idUser;
	private String username;
	private String name;
	private String surname;

	// Getters and Setters

	public UUID getIdUser() {
		return idUser;
	}

	public void setIdUser(UUID idUser) {
		this.idUser = idUser;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

}
