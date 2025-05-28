export function getSongsOrdered(songs) {
    songs.sort((a, b) => {
        // Compare artist names case-insensitive
        const artistA = a.album.artist.toLowerCase();
        const artistB = b.album.artist.toLowerCase();

        if (artistA < artistB) return -1;
        if (artistA > artistB) return 1;

        // If artist names are equal, compare album year
        return a.album.year - b.album.year;
    });
    return songs;
}