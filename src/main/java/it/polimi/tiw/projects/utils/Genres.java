package it.polimi.tiw.projects.utils;

public enum Genres {
    AFRICAN("Music of Africa"), ALTERNATIVEROCK("Alternative rock"), AMBIENT("Ambient music"),
    AMERICANFOLK("American folk music"), ASIAN("Music of Asia"), BLUES("Blues"), CHRISTIAN("Christian music"),
    CLASSICAL("Classical music"), COMMERCIAL("Commercial"), COUNTRY("Country music"), DANCE("Dance music"),
    DISCO("Disco"), EASYLISTENING("Easy listening"), EDM("Electronic dance music"), ELECTRONIC("Electronic music"),
    EXPERIMENTAL("Experimental music"), FOLK("Folk music"), FUNK("Funk"), GENEALOGY("Genealogy of musical genres"),
    GOSPEL("Gospel music"), HARDCORE("Hardcore"), HEAVYMETAL("Heavy metal"), HIPHOP("Hip-hop"),
    HIPHOPCULTURE("Hip-hop culture"), HOUSE("House music"), INDEPENDENT("Independent music"), INDIEPOP("Indie pop"),
    INDIEROCK("Indie rock"), JAZZ("Jazz"), KPOP("K-pop"), LATINAMERICAN("Music of Latin America"),
    MIDDLEEASTERN("Middle Eastern music"), MODERNISM("Modernism"), NEWAGE("New-age music"), NEWWAVE("New wave"),
    POP("Pop music"), PSYCHEDELIC("Psychedelic music"), PUNKROCK("Punk rock"), REGGAE("Reggae"),
    ROCKANDROLL("Rock and roll"), SKA("Ska"), SOCA("Soca music"), SOUL("Soul music"), SYNTHPOP("Synth-pop"),
    TECHNO("Techno"), THRASHMETAL("Thrash metal"), VAPORWAVE("Vaporwave"), VOCAL("Vocal music"),
    WESTERN("Western music"), WORLD("World music");

    private final String description;

    Genres(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
