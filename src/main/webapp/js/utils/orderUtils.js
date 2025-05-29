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

/**
 * Extracts unique album summaries from a list of songs.
 * De-duplicates based on album title (case-insensitive).
 * @param {Array<Object>} songs - An array of song objects, where each song might have an 'album' property.
 * @returns {Array<{title: string, artist: string, year: number}>} An array of unique album summary objects.
 */
export function extractUniqueAlbumSummaries(songs) {
    if (!songs || !Array.isArray(songs) || songs.length === 0) {
        return [];
    }
    const uniqueAlbumsMap = new Map();
    songs.forEach(songWithAlbum => {
        if (songWithAlbum.album.name) {
            uniqueAlbumsMap.set(songWithAlbum.album.name.toLowerCase(), {
                title: songWithAlbum.album.name,
                artist: songWithAlbum.album.artist,
                year: songWithAlbum.album.year
            });
        }
    });
    return Array.from(uniqueAlbumsMap.values());
}

/**
 * Adds a new album summary to a list of album summaries if it's not already present (case-insensitive title check).
 * @param {Array<{title: string, artist: string, year: number}>} currentAlbumSummaries - The current array of album summaries.
 * @param {Object} newAlbum - The album object from a newly added song (e.g., newSongWithAlbum.album).
 * @returns {{updatedSummaries: Array<{title: string, artist: string, year: number}>, wasAdded: boolean}} An object containing the potentially updated list and a flag.
 */
export function addAlbumSummaryIfNew(currentAlbumSummaries, newAlbum) {
    if (!newAlbum?.name) {
        return {updatedSummaries: currentAlbumSummaries, wasAdded: false};
    }

    const existingAlbumIndex = currentAlbumSummaries.findIndex(
        album => album.title.toLowerCase() === newAlbum.name.toLowerCase()
    );

    if (existingAlbumIndex === -1) {
        const newSummary = {
            title: newAlbum.name,
            artist: newAlbum.artist,
            year: newAlbum.year
        };
        return {updatedSummaries: [...currentAlbumSummaries, newSummary], wasAdded: true};
    }
    return {updatedSummaries: currentAlbumSummaries, wasAdded: false};
}
