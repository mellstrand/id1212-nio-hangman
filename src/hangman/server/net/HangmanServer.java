/**
 *
 * @author Tobias Mellstrand
 * @date 2017-11-23
 */

package hangman.server.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Starts the server for the game
 */
public class HangmanServer {

    private static final int SERVER_PORT = 5000;
    private Selector selector;
    private ServerSocketChannel ssc;

	/**
	 * Run the server and derive in any clients is communicating
	 */
	private void runServer() {
		try {

			selector = Selector.open();
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ssc.bind(new InetSocketAddress(SERVER_PORT));
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("Server running...");

			while (true) {

				selector.select();
				Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

				while (keyIterator.hasNext()) {
					SelectionKey selectionKey = keyIterator.next();
					keyIterator.remove();

					if (!selectionKey.isValid()) {
						continue;
					}

					if(selectionKey.isAcceptable()) {
						acceptRequest(selectionKey);
					} else if(selectionKey.isReadable()) {
						messageFromClient(selectionKey);
					} else if(selectionKey.isWritable()) {
						messageToClient(selectionKey);
					}
				}
			}

		} catch(IOException ioe) {
			System.err.println(ioe);
		}
    }
	
	/**
	 * Accept a request from a client
	 * @param selectionKey - Clients corresponding key
	 */
    private void acceptRequest(SelectionKey selectionKey) {

		try {
			ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
			SocketChannel clientSocketChannel = serverChannel.accept();
			clientSocketChannel.configureBlocking(false);
			clientSocketChannel.register(selector, SelectionKey.OP_READ, new ClientHandler(this, clientSocketChannel));
			//clientSocketChannel.setOption(StandardSocketOptions.SO_LINGER, LINGER_TIME);

		} catch(IOException ioe) {
			System.err.println("ERROR - acceptReqeust() : " + ioe);
		}
    }

    /**
     * Receive a message from a client
     * @param selectionKey - Clients corresponding key
     * @throws IOException - Client close connection, close our side as well
     */
    private void messageFromClient(SelectionKey selectionKey) throws IOException {
		ClientHandler ch = (ClientHandler) selectionKey.attachment();
		try {
			ch.incomingMessage();
		} catch(IOException ioe) {
			System.err.println("ERROR - messageFromClient() : " + ioe);
			ch.disconnect();
			selectionKey.cancel();
		}
    }

	/**
	 * Send message to a client
	 * @param selectionKey - Clients corresponding key
	 * @throws IOException - Socket closing
	 */
    private void messageToClient(SelectionKey selectionKey) throws IOException {
		ClientHandler ch = (ClientHandler) selectionKey.attachment();
		try {
			ch.outgoingMessage();
			selectionKey.interestOps(SelectionKey.OP_READ);
		} catch (IOException ioe) {
			System.err.println("ERROR - messageToClient() : " + ioe);
			ch.disconnect();
			selectionKey.cancel();
		}
    }

	/**
	 * Wake thread to make pending changes
	 * Retrieve key and set to write
	 * @param socket - Socket for the client that wants to send
	 */
	public void makeReadyToSend(SocketChannel socket){
		SelectionKey key = socket.keyFor(this.selector);
        key.interestOps(SelectionKey.OP_WRITE);
		selector.wakeup();
	}

	/**
	 * Create and start server
	 * @param args - No argument needed
	 */
	public static void main(String[] args) {
		HangmanServer server = new HangmanServer();
		server.runServer();
	}

}
