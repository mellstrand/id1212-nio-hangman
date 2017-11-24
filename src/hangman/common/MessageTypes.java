/*
 * @author mellstrand
 * @date 2017-11-14
 */

package hangman.common;

/**
 * Defines types of messages when sending messages between server and client
 */
public enum MessageTypes {

    /**
     * To set up the game in the beginning
     */
    INIT,
    /**
     * To start a new game after a complete game or faulty one
     */
    NEW,
    /**
     * When guessing a letter or a word
     */
    GUESS,
    /**
     * When server is sending status of the current game
     */
    STATUS,
    /**
     * Closing the connection
     */
    END,
    /**
     * To establish a connection to the server
     */
    CONNECT


}
