
/**
 * Wrapper for setTimeout function
 * @param {number} [time] - The time in milliseconds to delay or default to 100ms
 */
export async function delay(time) {
    return new Promise(resolve => setTimeout(resolve, time || 100));
}
