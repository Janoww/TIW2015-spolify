package it.polimi.tiw.projects.utils;

public enum Genre {
    AFRICAN("Music of Africa"), ALTERNATIVE_ROCK("Alternative rock"), AMBIENT(
            "Ambient music"), AMERICAN_FOLK("American folk music"), ASIAN("Music of Asia"), BLUES(
                    "Blues"), CHRISTIAN("Christian music"), CLASSICAL(
                            "Classical music"), COMMERCIAL("Commercial"), COUNTRY(
                                    "Country music"), DANCE("Dance music"), DISCO(
                                            "Disco"), EASY_LISTENING("Easy listening"), EDM(
                                                    "Electronic dance music"), ELECTRONIC(
                                                            "Electronic music"), EXPERIMENTAL(
                                                                    "Experimental music"), FOLK(
                                                                            "Folk music"), FUNK(
                                                                                    "Funk"), GENEALOGY(
                                                                                            "Genealogy of musical genres"), GOSPEL(
                                                                                                    "Gospel music"), HARDCORE(
                                                                                                            "Hardcore"), HEAVY_METAL(
                                                                                                                    "Heavy metal"), HIPHOP(
                                                                                                                            "Hip-hop"), HIPHOP_CULTURE(
                                                                                                                                    "Hip-hop culture"), HOUSE(
                                                                                                                                            "House music"), INDEPENDENT(
                                                                                                                                                    "Independent music"), INDIE_POP(
                                                                                                                                                            "Indie pop"), INDIE_ROCK(
                                                                                                                                                                    "Indie rock"), JAZZ(
                                                                                                                                                                            "Jazz"), KPOP(
                                                                                                                                                                                    "K-pop"), LATIN_AMERICAN(
                                                                                                                                                                                            "Music of Latin America"), MIDDLE_EASTERN(
                                                                                                                                                                                                    "Middle Eastern music"), MODERNISM(
                                                                                                                                                                                                            "Modernism"), NEW_AGE(
                                                                                                                                                                                                                    "New-age music"), NEW_WAVE(
                                                                                                                                                                                                                            "New wave"), POP(
                                                                                                                                                                                                                                    "Pop music"), PSYCHEDELIC(
                                                                                                                                                                                                                                            "Psychedelic music"), PUNK_ROCK(
                                                                                                                                                                                                                                                    "Punk rock"), REGGAE(
                                                                                                                                                                                                                                                            "Reggae"), ROCK_AND_ROLL(
                                                                                                                                                                                                                                                                    "Rock and roll"), SKA(
                                                                                                                                                                                                                                                                            "Ska"), SOCA(
                                                                                                                                                                                                                                                                                    "Soca music"), SOUL(
                                                                                                                                                                                                                                                                                            "Soul music"), SYNTH_POP(
                                                                                                                                                                                                                                                                                                    "Synth-pop"), TECHNO(
                                                                                                                                                                                                                                                                                                            "Techno"), THRASH_METAL(
                                                                                                                                                                                                                                                                                                                    "Thrash metal"), VAPOR_WAVE(
                                                                                                                                                                                                                                                                                                                            "Vapor-wave"), VOCAL(
                                                                                                                                                                                                                                                                                                                                    "Vocal music"), WESTERN(
                                                                                                                                                                                                                                                                                                                                            "Western music"), WORLD(
                                                                                                                                                                                                                                                                                                                                                    "World music");

    private final String description;

    Genre(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
