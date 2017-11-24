/**
 *
 * @author mellstrand
 * @date 2017-11-23
 */
package hangman.client.view;

import hangman.client.net.ServerHandler;
import hangman.common.Constants;
import hangman.common.MessageTypes;
import static java.lang.Thread.sleep;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringJoiner;


public class HangmanClient implements Runnable {

    private static final String PROMPT = ">> ";
    private ServerHandler serverHandler;
    private final Scanner scanner = new Scanner(System.in);
    private String playerName;
    private boolean connected = false;

    public HangmanClient(String playerName) {

		this.playerName = playerName;
	}

    public void start() {

		if(connected) {
			return;
		}
		serverHandler = new ServerHandler(this);
		new Thread(this).start();
	}

    /**
     * Receives messages from the server and interprets them
     * and perform the correct action to it
     */
    @Override
    public void run() {

		printLocal("-- Welcome to Hangman Game ---", "Usage: \t 'NEW' for a new word.",
				"\t 'END' to stop playing", "\t 'GUESS' to make a guess\n");

		serverHandler.connect();
		while(!serverHandler.isConnected()) {
			/**
			* Give time to finish the connecting process
			* Before sending first message
			*/
		   try {
			   sleep(1);
		   } catch(Exception e) {
			   //Do nothing
		   }
		}
		connected = true;
		sendMessage(MessageTypes.CONNECT, playerName);

		while(connected) {

			String userInput = readUserInput();
			if(userInput==null || userInput.equals("")) break;
			String[] requestToken = userInput.split(Constants.LOCAL_DELIMETER);
			try {
				MessageTypes msgType = MessageTypes.valueOf(requestToken[0].toUpperCase());

				switch(msgType) {
					case NEW:
						sendMessage(MessageTypes.NEW);
					break;
					case END:
						quitPlaying();
					break;
					case GUESS:
						sendMessage(MessageTypes.GUESS, requestToken[1]);
					break;
					default:
						printLocal("Dont understand the request \n Usage 'NEW', 'END' or 'GUESS'");
					break;
				}

			} catch(IllegalArgumentException iae){
				printLocal("Invalid command, " + iae);
			} catch(ArrayIndexOutOfBoundsException aiooe) {
				printLocal("No guess, "+ aiooe);
			}

		}
	}

    /**
     * Send message consisting of only type of message
     *
     * @param mt - enum MessageTypes, to specify type of message
     */
    private void sendMessage(MessageTypes mt) {
		serverHandler.prepareMessageToSend(mt.toString());
    }

    /**
     * Joins type and message to one string with DELIMETER and sends to server
     *
     * @param mt - enum MessageTypes, to specify type of message
     * @param line  - message to server
     */
    private void sendMessage(MessageTypes mt, String line) {

		StringJoiner joiner = new StringJoiner(Constants.TCP_DELIMETER);
		joiner.add(mt.toString());
		joiner.add(line);
		serverHandler.prepareMessageToSend(joiner.toString());
	}

    /**
     * For printing local messages, i.e. on client side
     *
     * @param parts - String to be printed
     */
    private synchronized void printLocal(String... parts) {
		for (String part: parts) {
			System.out.println(part);
		}
    }

    /**
     * Print prompt and read user input from the console
     *
     * @return The user input
     */
    private String readUserInput() {
		printLocal(PROMPT);
		return scanner.nextLine();
    }

    /**
     * Client wants to stop playing, terminate program
     */
    private void quitPlaying() {
		printLocal("Closing session...");
		connected = false;
		serverHandler.disconnect();
		sendMessage(MessageTypes.END);
		System.exit(0);
    }

    public void messageHandler(String serverMessage) {

		String[] requestToken = serverMessage.split(Constants.TCP_DELIMETER);
		MessageTypes msgType = MessageTypes.valueOf(requestToken[0].toUpperCase());

		switch(msgType) {
			case INIT:
				printLocal(Arrays.copyOfRange(requestToken, 1, requestToken.length));
				sendMessage(MessageTypes.INIT);
				printLocal("Connected to the server, lets play.");
			break;
	/*
			case STATUS:
			printLocal(Arrays.copyOfRange(requestToken, 1, requestToken.length));
			break;
			case NEW:
			printLocal(Arrays.copyOfRange(requestToken, 1, requestToken.length));
			break;
	*/
			default:
				printLocal(Arrays.copyOfRange(requestToken, 1, requestToken.length));
			break;
		}
    }

}
