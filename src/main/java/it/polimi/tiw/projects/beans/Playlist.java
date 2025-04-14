package it.polimi.tiw.projects.beans;

import java.util.List;
import java.util.UUID;

public class Playlist {

    private int idPlaylist;
    private String name;
    private String birthday; // Corresponds to the 'birthday' column in the DB
    private UUID idUser;
    private List<Song> songs;

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

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public UUID getIdUser() {
        return idUser;
    }

    public void setIdUser(UUID idUser) {
        this.idUser = idUser;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
    }
}
