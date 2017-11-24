/**
 *
 * @author Tobias Mellstrand
 * @date 2017-11-21
 */
package hangman.server.game;

import hangman.common.Constants;
import hangman.file.ReservoirSampling;
import hangman.server.helper.GuessStatus;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Class to handle game related operations
 */
public class GameHandler {

    private static final int SAMPLE_SIZE = 10;
    private File wordFile;

    private ReservoirSampling rs;
    private List<String> wordList;
    private LinkedList<Character> guessedCharacters;
    private String playerName;
    private String correctWord;
    private String guessString;
    private int remainingAttempts;
    private int clientScore;


    /**
     * Create a game handler
     *
	 * @param playerName
     */
    public GameHandler(String playerName) {

		this.playerName = playerName;
		initGame();

    }

	/**
	 * Initialize a game
	 */
	private void initGame() {
		try {
			wordFile = new File("words.txt");
			rs = new ReservoirSampling(wordFile, SAMPLE_SIZE);
			wordList = rs.getSample();
			guessedCharacters = new LinkedList<>();
			clientScore = 0;
			newGame();
		} catch(IOException ioe) {

		}

	}
	
    /**
     * When the client wants to guess another word
     */
    public void newGame() {
		try {
			setNewWord(getNewWord());
			setRemainingAttempts();
			setGuessString();
			guessedCharacters.clear();
		} catch(IOException ioe) {

		}

    }

	/**
	 * @return - player name
	 */
    public String getName() {
		return this.playerName;
    }
	
	/**
	 * @return - correct word
	 */
	public String getCorrectWord() {
		return correctWord;
	}

	/**
	 * @return - game status 
	 */
    public String getGameStatus() {
		StringJoiner joiner = new StringJoiner(Constants.TCP_DELIMETER);
		joiner.add("Word: "+guessString);
		joiner.add("Remaining Attempts: "+remainingAttempts);
		joiner.add("Score: "+clientScore);
		return joiner.toString();
    }

    /**
     * Decreasing number of attempts a client has by one when guessing wrong
     *
     * @return - true for values different (higher) from zero.
     *		 false when zero, i.e. out of attempts
     */
    public boolean updateRemainingAttempts() {
		return --remainingAttempts != 0;
    }

    /**
     * Updating the game score
     *
     * @param increase - true to increase the score, i.e. when completing a game
     *			 else decrease when losing a game
     */
    public void updateScore(boolean increase) {
		if(increase) {
			clientScore++;
		} else {
			clientScore--;
		}
    }

    /**
     * Checks the guess to the correct word.
     *
     * @param clientGuess - guess from the client, one character or a whole word
     * @return enum WordStatus, COMPLETE for the whole word,
     *				FRAGEMENT if a character is correct
     *				FAILED if wrongly guessed
     *				PREVIOUS if client has already guessed that char
     */
    public GuessStatus checkString(String clientGuess) {

		if(clientGuess.length() == 1) {

			char guess = clientGuess.charAt(0);
			if( !(guessedCharacters.contains(guess)) ) {

			guessedCharacters.add(guess);
			boolean fragment = false;
			char[] temp = guessString.toCharArray();

			for(int i=0; i<correctWord.length(); i++) {
				if(correctWord.charAt(i) == guess) {
				temp[i] = guess;
				fragment = true;
				}
			}
			guessString = String.valueOf(temp);

			if(guessString.equalsIgnoreCase(correctWord))
				return GuessStatus.COMPLETE;
			else if(fragment)
				return GuessStatus.FRAGMENT;
			} else {
			return GuessStatus.PREVIOUS;
			}

		} else if (clientGuess.length() == correctWord.length() && clientGuess.equalsIgnoreCase(correctWord)) {
			return GuessStatus.COMPLETE;
		}

		return GuessStatus.FAILED;

    }

    /**
     * Stores the new word in a String variable
     *
     * @param newWord - to be used as the word to guess
     */
    private void setNewWord(String newWord) {
		correctWord = newWord;
    }

    /**
     * Get a new word from the sampling
     *
     * @return the new word to guess
     * @throws IOException if the ResevoirSampling class could not read the file
     */
    private String getNewWord() throws FileNotFoundException, IOException {

		if(!wordList.isEmpty()) {

			String newWord = wordList.get(0);
			wordList.remove(0);
			return newWord.toLowerCase();

		} else {

			wordList = rs.getSample();
			String newWord = wordList.get(0);
			wordList.remove(0);
			return newWord.toLowerCase();
		}
    }

    /**
     * Sets number of attempts to number of characters in word to guess
     */
    private void setRemainingAttempts() {
		remainingAttempts = correctWord.length();
    }

    /**
     * Creates a new string with as many underscores
     * as characters in the word to guess
     */
    private void setGuessString() {
		char[] chars = new char[correctWord.length()];
		Arrays.fill(chars, '_');
		guessString = new String(chars);
    }

}
