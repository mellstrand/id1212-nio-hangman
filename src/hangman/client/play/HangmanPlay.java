/**
 *
 * @author Tobias Mellstrand
 * @date 2017-11-09
 */

package hangman.client.play;

import hangman.client.view.HangmanClient;

/**
 *  Class to start the game 
 */
public class HangmanPlay {
 
    /**
     * @param args - The command line argument, should be the players name
     */
    public static void main(String[] args) {
	
		String name;

		if(!(args.length == 0)) {
			name = args[0];
		} else {
			name = "DefaultPlayer";
		}

		new HangmanClient(name).start();

    }
}
