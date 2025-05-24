package it.polimi.tiw.projects.beans;

import java.io.Serializable;
import jakarta.validation.constraints.NotNull;

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

	@NotNull
	public Album getAlbum() {
		return album;
	}

	public void setSong(@NotNull Song song) {
		this.song = song;
	}

	public void setAlbum(@NotNull Album album) {
		this.album = album;
	}

}
