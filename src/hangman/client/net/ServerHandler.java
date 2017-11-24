/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hangman.client.net;

import hangman.client.view.HangmanClient;
import hangman.common.Constants;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;

/**
 *
 * @author mellstrand
 * @date 2017-11-23
 */
public class ServerHandler implements Runnable {
	private static final String SERVER_NAME = "localhost";
	private static final int SERVER_PORT = 5000;

	private final ByteBuffer messageBuffer = ByteBuffer.allocateDirect(Constants.MAX_MESSAGE_LENGTH);
	private final LinkedList<ByteBuffer> messagesToSend = new LinkedList<>();

	private InetSocketAddress addressToServer;
	private Selector selector;
	private SocketChannel socketChannel;
	private HangmanClient client;

	private boolean connected = false;
	private boolean connectionCompleted = false;
	volatile boolean timeToSend = false;

	/**
	 *
	 * @param client
	 */
	public ServerHandler(HangmanClient client) {
		this.client = client;
	}

	/**
	 *
	 */
	@Override
	public void run() {

		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(addressToServer);
			connected = true;

			selector = Selector.open();
			socketChannel.register(selector, SelectionKey.OP_CONNECT);

			while(connected || !messagesToSend.isEmpty()) {
				if (timeToSend) {
                    socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
                    timeToSend = false;
                }

				selector.select();
				for (SelectionKey key : selector.selectedKeys()) {
					selector.selectedKeys().remove(key);

					if (!key.isValid()) {
						continue;
					}

					if (key.isConnectable()) {
						completeConnection(key);
					} else if (key.isReadable()) {
						messageFromServer(key);
					} else if (key.isWritable()) {
						messageToServer(key);
					}
				}
			}

		} catch (IOException ioe) {
			System.out.println("ERROR - ServerHandler.run() : " + ioe);
		}

		runDisconnect();

	}

	/**
	 * Connect to the server
	 */
	public void connect() {
		addressToServer = new InetSocketAddress(SERVER_NAME, SERVER_PORT);
        new Thread(this).start();
    }
	
	public boolean isConnected() {
		return connectionCompleted;
	}

	/**
	 * Set to not connected
	 */
    public void disconnect() {
		connected = false;
    }

    /**
     * Close socket to server
     */
    public void runDisconnect() {
		try {
			socketChannel.close();
			socketChannel.keyFor(selector).cancel();
		} catch (IOException ioe) {
			System.err.println("ERROR - disconnect() : " + ioe);
		}

	}

	/**
	 * Prepare to send a message
	 * @param message
	 */
	public void prepareMessageToSend(String message) {
		synchronized(messagesToSend) {
			messagesToSend.add(ByteBuffer.wrap(message.getBytes()));
		}

		//socketChannel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
		timeToSend = true;
		selector.wakeup();
	}

	/**
	 * See that the connection to server is finished
	 * @param selectionKey - Clients corresponding key
	 * @throws IOException
	 */
	private void completeConnection(SelectionKey selectionKey) throws IOException {
		socketChannel.finishConnect();
        selectionKey.interestOps(SelectionKey.OP_READ);
		connectionCompleted = true;
	}

	/**
	 * Get message from server
	 * @param selectionKey - Clients corresponding key
	 * @exception IOException - close or reading fails
	 */
	private void messageFromServer(SelectionKey selectionKey) throws IOException {

		messageBuffer.clear();
		int numRead;
		try {
			numRead = socketChannel.read(messageBuffer);
			if(numRead == -1) {
				throw new IOException("ERROR : messageFromServer() : closed connection cleanly");
			}
		} catch(IOException ioe) {
			System.err.println("ERROR - messageFromServer() : " + ioe);
			socketChannel.close();
		}

		messageBuffer.flip();
		byte[] bytes = new byte[messageBuffer.remaining()];
		messageBuffer.get(bytes);

		ForkJoinPool.commonPool().execute(new Runnable() {
			@Override
			public void run() {
				client.messageHandler(new String(bytes));
			}
		});
	}

    /**
     * To send server a message
	 * @param selectionKey - Clients corresponding key
	 * @exception IOException - Writing fails
     */
    private void messageToServer(SelectionKey selectionKey) throws IOException {
		ByteBuffer message;
		synchronized (messagesToSend) {
			message = messagesToSend.poll();
			while(message.hasRemaining()) {
				socketChannel.write(message);
			}
			selectionKey.interestOps(SelectionKey.OP_READ);
		}
	}

}
