package it.polimi.tiw.projects.beans;

import java.io.Serializable;

public class SongWithAlbum implements Serializable {
	private Song song;
	private Album album;

	public SongWithAlbum(Song song, Album album) {
		this.song = song;
		this.album = album;
	}

	public Song getSong() {
		return song;
	}

	public Album getAlbum() {
		return album;
	}

	public void setSong(Song song) {
		this.song = song;
	}

	public void setAlbum(Album album) {
		this.album = album;
	}

}
