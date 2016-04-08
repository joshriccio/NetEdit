package view;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import model.User;
import network.Request;
import network.RequestCode;
import network.Response;
import network.ResponseCode;
import network.Server;

/**
 * 
 * @author Josh
 *
 */
public class LoginScreen extends JFrame {
	private static final long serialVersionUID = 1L;
	private JPanel loginPanel;
	private JButton loginButton = new JButton("Login");
	private JButton createAccountButton = new JButton("Create Account");
	private JButton forgottenAccountButton = new JButton("Forgot Login");
//	private JButton updateToServer = new JButton("Update Server");
	private JTextField loginTextField = new JTextField(15);
	private JPasswordField passwordField = new JPasswordField(15);
	private JLabel username;
	private JLabel password;
	public static boolean loginButtonState = false; // Means button has not been pressed
	public static boolean createAccountButtonState = false; // Means button has not been pressed
	private static final String ADDRESS = "localhost";
	private Socket socket = null;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private User user;
	
	/**
	 * Constructor
	 */
	public LoginScreen() {
		setTitle("Login");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(400,150);
		setLocation(300,20);
		setLayout(new FlowLayout());
		username = new JLabel("Username");
		password = new JLabel("Password");
		loginPanel = new JPanel();
		loginPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		// Login Constraints
		c.gridx = 0;
		c.gridy = 0;
		loginPanel.add(username,c);
		c.gridy = 1;
		loginPanel.add(password,c);
		c.gridx = 1;
		c.gridy = 0;
		loginPanel.add(loginTextField,c);
		c.gridx = 1;
		c.gridy = 1;
		loginPanel.add(passwordField,c);
		c.gridx = 0;
		c.gridy = 2;
		loginPanel.add(loginButton,c);
		c.gridx = 1;
		c.gridy = 2;
		loginPanel.add(createAccountButton,c);
		c.gridx = 2;
		c.gridy = 2;
		loginPanel.add(forgottenAccountButton,c);
		add(loginPanel);
		/*
		 * Login button will have a different effect in the GUI.
		 */
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				loginButtonState = true;
				user = new User(loginTextField.getText(),String.valueOf(passwordField.getPassword()));
				openConnection(RequestCode.LOGIN);
			}
		});
		createAccountButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JLabel createUsername = new JLabel("Username:");
				JTextField createUsernameField = new JTextField();
				JLabel createPassword = new JLabel("Password:");
				JPasswordField createPasswordField = new JPasswordField();
				Object[] array = { createUsername, createUsernameField, createPassword, createPasswordField };
				int res = JOptionPane.showConfirmDialog(null,array,"Create Account",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
				if (res == JOptionPane.YES_OPTION) {
					createAccountButtonState = true;
					user = new User(createUsernameField.getText(),String.valueOf(createPasswordField.getPassword()));
					openConnection(RequestCode.CREATE_ACCOUNT);
				}
				createAccountButtonState = false;
			}
		});
	}
	
	/**
	 * Opens a new connection between server and client
	 * 
	 * @author cdeeran11 (cdeeran11@email.arizona.edu)
	 */
	private void openConnection(RequestCode requestCode) {
		try {
			// Connect to the Server
			socket = new Socket(ADDRESS,Server.PORT_NUMBER);
			this.oos = new ObjectOutputStream(socket.getOutputStream());
			this.ois = new ObjectInputStream(socket.getInputStream());
			Request clientRequest = new Request(requestCode);
			clientRequest.setUser(user);
			oos.writeObject(clientRequest);
			Response serverResponse = (Response) ois.readObject();
			if (serverResponse.getResponseID() == ResponseCode.LOGIN_SUCCESSFUL) {
				EditorGui editor = new EditorGui(oos, ois, user);
				editor.setVisible(true);
				dispose();
				JOptionPane.showConfirmDialog(null,"Welcome " + clientRequest.getUser().getUsername() + "!","Login Successful",JOptionPane.YES_OPTION);
			}
		}
		catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}