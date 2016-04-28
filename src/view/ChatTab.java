package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import network.Request;
import network.RequestCode;
import network.Response;
import network.Server;

public class ChatTab extends JPanel {

	private static final long serialVersionUID = 1L;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private ChatMessages messages;
	private ChatTextArea chatArea;
	private JTextPane chatpane;
	private String conversation;
	private String name;
	private Socket socket;

	/**
	 * The constructor to build a new chat tab @param username the clients
	 * username
	 */
	public ChatTab(String username) {
		try {
			this.socket = new Socket(Server.ADDRESS, Server.PORT_NUMBER);
			this.oos = new ObjectOutputStream(socket.getOutputStream());
			this.ois = new ObjectInputStream(socket.getInputStream());
			Request request = new Request(RequestCode.START_CHAT_HANDLER);
			this.name = username;
			request.setUsername(this.name);
			oos.writeObject(request);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.messages = new ChatMessages();
		this.name = username;
		this.conversation = new String();
		this.chatpane = new JTextPane();
		this.chatArea = new ChatTextArea(chatpane);
		this.chatArea.setPreferredSize(new Dimension(1000, 350));
		this.add(messages, BorderLayout.CENTER);
		this.add(chatArea, BorderLayout.SOUTH);
		setListeners();
		ChatServerListener csl = new ChatServerListener();
		csl.start();

	}

	private void setListeners() {
		this.chatpane.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent event) {
			}

			@Override
			public void keyReleased(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_ENTER) {
					String message = "";
					if (chatArea.getMessage().length() > 1) {
						message = chatArea.getMessage().substring(0, chatArea.getMessage().length() - 2);
					}
					try {
						Request request = new Request(RequestCode.SEND_MESSAGE);
						request.setUsername(name);
						request.setMessage(message);
						chatArea.clearText();
						oos.writeObject(request);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void keyTyped(KeyEvent event) {
			}
		});
	}

	/**
	 * Updates the conversation with the latest message @param name the message
	 * senders name @param message the message that was sent
	 */
	public void updateConversation(String name, String message) {
		conversation = conversation + name + ": " + message + "\n";
		messages.setText(conversation);
	}

	private class ChatServerListener extends Thread {
		@Override
		public void run() {
			boolean isRunning = true;
			while (isRunning) {
				try {
					Response response = ((Response) ois.readObject());
					updateConversation(response.getUsername(), response.getMessage());
				} catch (IOException | ClassNotFoundException e) {
					isRunning = false;
					e.printStackTrace();
				}
			}
		}
	}
}