package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import model.EditableDocument;
import model.ToolBar;
import model.User;
import network.Request;
import network.RequestCode;
import network.Response;
import network.ResponseCode;

/**
 * 
 * @author Brittany, Josh, Steven, Cody
 *
 */
public class EditorGui extends JFrame {
	private static final long serialVersionUID = 1L;
	private JTextPane textpane = new JTextPane();
	// set up JtoolBar with buttons and drop downs
	private JToolBar javaToolBar = new JToolBar();
	private JButton boldFontButton, italicFontButton, underlineFontButton, colorButton;
	private JToggleButton bulletListButton;
	private Integer[] fontSizes = { 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 26, 48, 72 };
	@SuppressWarnings("rawtypes")
	private JComboBox sizeFontDropDown, fontDropDown;
	private String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private User user;
	private ToolBar myToolBar = new ToolBar();
	private String docName;
	private UsersOnline userslist;
	private TabbedPane tabbedpane;

	/**
	 * Constructor for when New Document is begun
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EditorGui(ObjectOutputStream oos, ObjectInputStream ois, User user, String documentName) {
		this.oos = oos;
		this.ois = ois;
		this.user = user;
		docName = documentName;
		ServerListener serverListener = new ServerListener();
		serverListener.start();

		// Set Frame
		this.setTitle("Collaborative Editing: "); // Removed docName, since we
													// have tabs with the
													// document labels
		this.setSize(1350, 700);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setFont(new Font("Courier New", Font.ITALIC, 12));
		sizeFontDropDown = new JComboBox(fontSizes);
		fontDropDown = new JComboBox(fonts);
		// initialize the file menu
		setupMenuBar();
		// initialize the text area
		setTextArea("");
		// initialize the JToolbar
		setJToolBar();
		// add listeners to buttons and drop boxes
		setButtonListeners();
		// Add Timer for saving every period: 5s
		Timer timer = new Timer();
		timer.schedule(new BackupDocument(), 0, 5000);
		setUsersWindow();
	}

	/**
	 * Constructor for when previous document is loaded
	 */

	public EditorGui(ObjectOutputStream oos, ObjectInputStream ois, User user, EditableDocument doc) {
		this.oos = oos;
		this.ois = ois;
		this.user = user;
		ServerListener serverListener = new ServerListener();
		serverListener.start();
		docName = doc.getName();

		// Set Frame
		this.setTitle("Collaborative Editing");
		this.setSize(1350, 700);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setFont(new Font("Courier New", Font.ITALIC, 12));
		// initialize the text area
		try {
			setTextArea(doc.getDocument().getText(0, doc.getDocument().getLength()));
		} catch (BadLocationException e) {
			setTextArea("");
			e.printStackTrace();
		}
		// initialize the JToolbar
		setJToolBar();
		// add listeners to buttons and drop boxes
		setButtonListeners();
		// Add Timer for saving every period: 5s
		Timer timer = new Timer();
		timer.schedule(new BackupDocument(), 0, 5000);
	}

	/**
	 * This method sets up the text area.
	 */
	public void setTextArea(String startingText) {
		
		tabbedpane = new TabbedPane(docName);
		tabbedpane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				myToolBar.setIsBold(false);
				myToolBar.setIsItalic(false);
				myToolBar.setIsUnderlined(false);
			}

		});
		this.add(tabbedpane);
		StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
		Style style = tabbedpane.getCurrentTextPane().addStyle("Indent", null);
		StyleConstants.setLeftIndent(style, 30);
		StyleConstants.setRightIndent(style, 30);
		doc.setParagraphAttributes(0, doc.getLength(), style, false);
	}
	/**
	 * This method sets up the tool bar.
	 */
	private void setJToolBar() {
		// initialize images
		Image boldImage = null;
		Image italicImage = null;
		Image underlineImage = null;
		Image colorImage = null;
		Image bulletImage = null;
		// load images
		try {
			boldImage = ImageIO.read(new File("./images/boldImage.png"));
			italicImage = ImageIO.read(new File("./images/italicImage.png"));
			underlineImage = ImageIO.read(new File("./images/underlineImage.png"));
			colorImage = ImageIO.read(new File("./images/colorImage.png"));
			bulletImage = ImageIO.read(new File("./images/bulletImage.png"));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Couldn't load an image on the toolbar");
		}
		// set buttons with icons in them
		boldFontButton = new JButton(new ImageIcon(boldImage));
		italicFontButton = new JButton(new ImageIcon(italicImage));
		underlineFontButton = new JButton(new ImageIcon(underlineImage));
		colorButton = new JButton(new ImageIcon(colorImage));
		bulletListButton = new JToggleButton(new ImageIcon(bulletImage));
		// add buttons to the tool bar
		javaToolBar.add(boldFontButton);
		javaToolBar.add(italicFontButton);
		javaToolBar.add(underlineFontButton);
		javaToolBar.add(bulletListButton);
		javaToolBar.add(colorButton);
		// add drop down menus to the tool bar
		javaToolBar.addSeparator();
		javaToolBar.add(sizeFontDropDown);
		javaToolBar.addSeparator();
		javaToolBar.add(fontDropDown);
		// add the tool bar to the frame
		this.add(javaToolBar, BorderLayout.NORTH);
	}

	private void setUsersWindow() {
		userslist = new UsersOnline(oos);
		userslist.init();
		this.add(userslist, BorderLayout.EAST);
		this.addWindowListener(new LogOffListener(this.user.getUsername(), oos));
	}

	private void setupMenuBar() {

		// Set up the file menu
		JMenu file = new JMenu("File");
		JMenuItem createNewDocument = new JMenuItem("New Document");
		file.add(createNewDocument);
		JMenuItem loadDocument = new JMenuItem("Load Document");
		file.add(loadDocument);
		JMenuItem refreshDocument = new JMenuItem("Refresh Document");
		file.add(refreshDocument);

		// Set up the options menu
		JMenu options = new JMenu("Options");
		JMenuItem changePassword = new JMenuItem("Change Password");
		options.add(changePassword);
		JMenuItem signout = new JMenuItem("Sign Out");
		options.add(signout);

		// Create the menu bar and add the Items
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(file);
		menuBar.add(options);

		// Add the file menu Listeners
		MenuItemListener menuListener = new MenuItemListener();
		createNewDocument.addActionListener(menuListener);
		loadDocument.addActionListener(menuListener);
		refreshDocument.addActionListener(menuListener);

		// Add the options menu listeners
		changePassword.addActionListener(menuListener);
		signout.addActionListener(menuListener);

	}

	private class MenuItemListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String text = ((JMenuItem) e.getSource()).getText();
			if (text.equals("New Document")) {
				String newDocumentName = JOptionPane.showInputDialog("What would you like to name your new document?");
				tabbedpane.addNewTab(newDocumentName);
			} else if (text.equals("Change Password")) {
				JLabel newPassword = new JLabel("New Password:");
				JPasswordField newPasswordField = new JPasswordField();
				Object[] forgotPasswordFields = { newPassword, newPasswordField };
				int response = JOptionPane.showConfirmDialog(null, forgotPasswordFields, "Forgot Password",
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (response == JOptionPane.YES_OPTION) {
					try {
						Request clientRequest = new Request(RequestCode.RESET_PASSWORD);
						clientRequest.setUser(user);
						clientRequest.setPassword(String.valueOf(newPasswordField.getPassword()));
						oos.writeObject(clientRequest);
						Response serverResponse = (Response) ois.readObject();
						if (serverResponse.getResponseID() == ResponseCode.ACCOUNT_RESET_PASSWORD_SUCCESSFUL) {
							JOptionPane.showConfirmDialog(null,
									"Your password has been successfully resest. Please sign out and back in for changes to take place.",
									"Password Successfully Reset", JOptionPane.OK_OPTION);
						} else if (serverResponse.getResponseID() == ResponseCode.ACCOUNT_RESET_PASSWORD_FAILED) {
							JOptionPane.showConfirmDialog(null, "Oops! Something went wrong :( Please try again!",
									"Password Failed to Reset", JOptionPane.OK_OPTION);
						}
					} catch (IOException | ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

			} else if (text.equals("Sign Out")) {
				int userResponse = JOptionPane.showConfirmDialog(null, "Are you sure you want to sign out?", "Sign Out",
						JOptionPane.YES_NO_OPTION);
				if (userResponse == JOptionPane.YES_OPTION) {
					LoginScreen ls = new LoginScreen();
					ls.setVisible(true);
					dispose();
				}
			}

		}
	}

	private class BackupDocument extends TimerTask {
		public void run() {

			// String newDocName = docName + new
			// SimpleDateFormat("dd-yyyy-MM-dd'T'HH:mm:ss.SSSZ-yyyy").format(new
			// Date());;
			String newDocName = "UpdatedSaveFile";// FIXME: title will always be
			// UpdatedSaveFile
			EditableDocument currentDoc = new EditableDocument(textpane.getStyledDocument(), docName);
			try {
				FileOutputStream outFile = new FileOutputStream(newDocName);
				ObjectOutputStream outputStream = new ObjectOutputStream(outFile);
				outputStream.writeObject(currentDoc);
				System.out.println("I just saved!");
				// Do NOT forget to close the output stream!
				outputStream.close();
				outFile.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Couldn't create a new save file!");
				// e.printStackTrace();
			}

		}
	}

	/**
	 * This method adds listeners to the buttons and drop down boxes on the tool
	 * bar
	 */
	public void setButtonListeners() {
		boldFontButton.addActionListener(new boldListener());
		italicFontButton.addActionListener(new italicListener());
		underlineFontButton.addActionListener(new underlineListener());
		sizeFontDropDown.addActionListener(new sizeFontDropDownListener());
		fontDropDown.addActionListener(new fontDropDownListener());
		bulletListButton.addActionListener(new listListener());
		colorButton.addActionListener(new colorListener());
	}

	private class boldListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			// text is selected
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("Bold", null);
				StyleConstants.setBold(style, true);
				if (!myToolBar.isBold()) {
					StyleConstants.setBold(style, true);
					myToolBar.setIsBold(true);
				} else {
					StyleConstants.setBold(style, false);
					myToolBar.setIsBold(false);
				}
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		}
	}

	private class italicListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			// text is selected
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("Italic", null);
				if (!myToolBar.isItalic()) {
					StyleConstants.setItalic(style, true);
					myToolBar.setIsItalic(true);
				} else {
					StyleConstants.setItalic(style, false);
					myToolBar.setIsItalic(false);
				}
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		}
	}

	private class underlineListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			// text is selected
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("UnderLine", null);
				StyleConstants.setUnderline(style, true);

				if (!myToolBar.isUnderlined()) {
					StyleConstants.setUnderline(style, true);
					myToolBar.setIsUnderlined(true);
				} else {
					StyleConstants.setUnderline(style, false);
					myToolBar.setIsUnderlined(false);
				}
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		}
	}

	private class sizeFontDropDownListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			Integer fontSize = (int) sizeFontDropDown.getSelectedItem();

			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("FontSize", null);
				StyleConstants.setFontSize(style, fontSize);
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		}
	}

	private class fontDropDownListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String stringFont = (String) fontDropDown.getSelectedItem();
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("FontFamily", null);
				StyleConstants.setFontFamily(style, stringFont);
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		}
	}

	private class colorListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			JColorChooser colorChooser = new JColorChooser();
			Color newColor = JColorChooser.showDialog(colorChooser, "Choose Text Color", Color.BLACK);
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("FontColor", null);
				StyleConstants.setForeground(style, newColor);
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		}
	}

	private class listListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (bulletListButton.isSelected()) {
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				try {
					doc.insertString(doc.getLength(), "\u2022  ", null);
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}

				tabbedpane.getCurrentTextPane().addKeyListener(new KeyListener() {
					@Override
					public void keyPressed(KeyEvent arg0) {
					}

					@Override
					public void keyReleased(KeyEvent arg0) {
						if (arg0.getKeyCode() == KeyEvent.VK_ENTER && bulletListButton.isSelected()) {
							StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
							try {
								doc.insertString(doc.getLength(), "\u2022  ", null);
							} catch (BadLocationException e) {
								e.printStackTrace();
							}

						} else if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
							KeyListener[] ls = tabbedpane.getCurrentTextPane().getKeyListeners();
							if (ls.length > 0) {
								tabbedpane.getCurrentTextPane().removeKeyListener(ls[0]);
							}

						}

					}

					@Override
					public void keyTyped(KeyEvent arg0) {
					}
				});
			}
		}
	}

	private class ServerListener extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Response response = (Response) ois.readObject();
					if (response.getResponseID() == ResponseCode.DOCUMENT_SENT) {
						EditorGui.this.tabbedpane.getCurrentTextPane().setStyledDocument(response.getStyledDocument());
						EditorGui.this.tabbedpane.getCurrentTextPane()
								.setCaretPosition(tabbedpane.getCurrentTextPane().getText().length());
					}
					if (response.getResponseID() == ResponseCode.USER_LIST_SENT) {
						EditorGui.this.userslist.updateUsers(response.getUserList());
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
