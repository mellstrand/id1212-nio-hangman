/**
 *
 * @author mellstrand
 * @date 2017-11-23
 */
package hangman.server.net;

import hangman.common.MessageTypes;
import hangman.common.Constants;
import hangman.server.game.GameHandler;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;

/**
 * Class that handles each client that connects to the server for playing
 */
class ClientHandler extends Thread {

    private final HangmanServer server;
    private final SocketChannel clientSocketChannel;
    private final ByteBuffer messageBuffer = ByteBuffer.allocateDirect(Constants.MAX_MESSAGE_LENGTH);
    private final LinkedList<ByteBuffer> outgoingMessagesQueue;
    private final LinkedList<String> incomingMessagesQueue;

    private String clientName;
    private GameHandler gameHandler;


    /**
     * Constructor, opens word file and input/output streams for communication
     * @param server - Reference to the server
     * @param clientSocket - Socket for communication with client
     */
    public ClientHandler(HangmanServer server, SocketChannel clientSocketChannel) {

		this.server = server;
		this.clientSocketChannel = clientSocketChannel;
		incomingMessagesQueue = new LinkedList<>();
		outgoingMessagesQueue = new LinkedList<>();

    }

    /**
	 * Receives messages from client and interprets them to perform correct action
	 */
	@Override
    public void run() {
		
		synchronized(incomingMessagesQueue) {

			while(!incomingMessagesQueue.isEmpty()) {
				String message = incomingMessagesQueue.poll();
				
				//System.out.println(clientName + " sends: " + message);
				
				String[] messageTokens = message.split(Constants.TCP_DELIMETER);
				MessageTypes messageType = MessageTypes.valueOf(messageTokens[0].toUpperCase());

				switch(messageType) {
					case CONNECT:
						clientName = messageTokens[1];
						System.out.println(messageTokens[1] + " connected to the server");
						sendMessage(MessageTypes.INIT, "SERVER: Welcome " + clientName);
					break;
					case INIT:
						System.out.println(clientName + " started a game");
						gameHandler = new GameHandler(clientName);
						sendMessage(MessageTypes.STATUS, gameHandler.getGameStatus());
					break;
					case NEW:
						System.out.println(clientName + " started a new game");
						gameHandler.newGame();
						sendMessage(MessageTypes.STATUS, gameHandler.getGameStatus());
					break;
					case GUESS:
						System.out.println(clientName + " guessed: " + messageTokens[1]);
						try {
							switch(gameHandler.checkString(messageTokens[1])) {
								case COMPLETE:
									gameHandler.updateScore(true);
									sendMessage(MessageTypes.NEW, "SERVER: Correct! Word was: "+ gameHandler.getCorrectWord() + ". New game?");
								break;
								case FRAGMENT:
									sendMessage(MessageTypes.STATUS, gameHandler.getGameStatus());
								break;
								case FAILED:
									if(gameHandler.updateRemainingAttempts()){
										sendMessage(MessageTypes.STATUS, gameHandler.getGameStatus());
									} else {
										gameHandler.updateScore(false);
										sendMessage(MessageTypes.NEW, "SERVER: No more attempts, correct word was: "+gameHandler.getCorrectWord()+".", "Try with a new word?");
									}
								break;
								case PREVIOUS:
									sendMessage(MessageTypes.STATUS, "SERVER: Already guessed character", gameHandler.getGameStatus());
								break;
							}

						} catch(ArrayIndexOutOfBoundsException e) {
							sendMessage(MessageTypes.STATUS, "SERVER: No guess, make a new one!", gameHandler.getGameStatus());
						}
					break;
					case END:
						closeConnection();
					break;
				}
			}
		}
    }

	/**
	 * Read message from client
	 * Execute with ForkJoinPool
	 * @throws IOException - If not able to read
	 */
    public void incomingMessage() throws IOException {

		messageBuffer.clear();
		int numRead;
		numRead = clientSocketChannel.read(messageBuffer);
		if(numRead == -1) {
		    throw new IOException("IOE : incomingMessage() : client closed connection cleanly");
		}

		messageBuffer.flip();
		byte[] bytes = new byte[messageBuffer.remaining()];
		messageBuffer.get(bytes);
		String receivedMessage = new String(bytes);
		synchronized (incomingMessagesQueue) {
		    incomingMessagesQueue.add(receivedMessage);
		}
		ForkJoinPool.commonPool().execute(this);
    }

	/**
	 * Send message to client
	 * @throws IOException - If not able to write
	 */
	public void outgoingMessage() throws IOException {
		ByteBuffer message = null;
		synchronized(outgoingMessagesQueue) {
			while((message = outgoingMessagesQueue.poll()) != null) {
				while(message.hasRemaining()) {
					clientSocketChannel.write(message);
				}
			}
		}
    }

	/**
	 * Close the channel
	 * @throws IOException - If close fails
	 */
    public void disconnect() throws IOException {
		clientSocketChannel.close();
    }

    /**
     * Send messages to client
     *
     * @param mt - enum MessageTypes, for specifying different types of communication
     * @param messages - messages to be sent
     */
  private void sendMessage(MessageTypes mt, String... messages) {
		StringJoiner joiner = new StringJoiner(Constants.TCP_DELIMETER);
		joiner.add(mt.toString());
		for(String message : messages) {
			joiner.add(message);
		}
		String joinedString = joiner.toString();
		ByteBuffer readyToSendMessage = ByteBuffer.wrap(joinedString.getBytes());

		synchronized(outgoingMessagesQueue) {
			outgoingMessagesQueue.add(readyToSendMessage.duplicate());
		}
		server.makeReadyToSend(clientSocketChannel);
   }

    /**
     * Close connection
     */
    private void closeConnection() {
		try {
			System.out.println(clientName + " disconnected from the server");
			clientSocketChannel.close();
		} catch(IOException ioe) {
			System.out.println(ioe);
		}
    }
	
}
