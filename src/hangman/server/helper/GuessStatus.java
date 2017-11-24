/**
 *
 * @author mellstrand
 * @date 2017-11-16
 */
package hangman.server.helper;

/**
 * Defines different states when checking a guess
 */
public enum GuessStatus {
    /**
     * When correct word has been guessed
     */
    COMPLETE,
    /**
     * When a character is correct, not the whole word is correct yet
     */
    FRAGMENT,
    /**
     * When guessed character is not amongst the characters of the correct word
     */
    FAILED,
    /**
     * Used when the client are guessing an already guessed character
     */
    PREVIOUS
}
