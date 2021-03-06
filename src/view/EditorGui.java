package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
//import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import controller.DocumentExporter;
import model.EditableDocument;
import model.ToolBar;
import model.User;
import network.Request;
import network.RequestCode;
import network.Response;
import network.ResponseCode;
import network.Server;

/**
 * The main editor interface
 * 
 * @author Brittany Paielli
 * @author Josh Riccio
 * @author Steven Connolly
 * @author Cody Deeran
 *
 */
public class EditorGui extends JFrame {
	private static final long serialVersionUID = 5134447391484363694L;
	private JToolBar javaToolBar = new JToolBar();
	private JButton boldFontButton, italicFontButton, underlineFontButton, colorButton, imageButton;
	private JToggleButton bulletListButton;
	private JComboBox<Integer> sizeFontDropDown;
	private JComboBox<String> fontDropDown;
	private Socket socket = null;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private ObjectOutputStream documentOutput;
	private User user;
	private ToolBar myToolBar = new ToolBar();
	private UsersOnline userslist;
	private TabbedPane tabbedpane;
	private ChatTab chat;
	private SummaryCollector summary;
	private int charCount = 0;
	private final int SAVE_FREQUENCY = 20;
	private LoadDoc loadDocumentWindow;

	/**
	 * Constructor
	 */

	public EditorGui(ObjectOutputStream oos, ObjectInputStream ois, User user, EditableDocument doc) {
		startServerListener(oos, ois);
		initializeEditor(user, doc);
		setupMenuBar();
		setupChatTab();
		setJToolBar();
		setButtonListeners();
		startDocumentStream();
		setUsersWindow();
	}

	/**
	 * @param oos
	 * @param ois
	 * @param user
	 * @param doc
	 */
	private void startServerListener(ObjectOutputStream oos, ObjectInputStream ois) {
		this.oos = oos;
		this.ois = ois;
		ServerListener serverListener = new ServerListener();
		serverListener.start();
	}

	/**
	 * @param user
	 * @param doc
	 */
	private void initializeEditor(User user, EditableDocument doc) {
		this.user = user;
		this.setTitle("Collaborative Editing:" + user.getUsername());
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setFont(new Font("Courier New", Font.ITALIC, 12));
		Integer[] fontSizes = { 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 26, 48, 72 };
		String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		sizeFontDropDown = new JComboBox<Integer>(fontSizes);
		fontDropDown = new JComboBox<String>(fonts);

		try {
			String temp = doc.getDocument().getText(0, doc.getDocument().getLength());
			setTextArea(temp, doc);
			tabbedpane.getCurrentTextPane().setDocument(doc.getDocument());
		} catch (BadLocationException e) {
			setTextArea("", doc);
			e.printStackTrace();
		}

		this.summary = new SummaryCollector(user.getUsername());
	}

	/**
	 * 
	 */
	private void startDocumentStream() {
		try {
			Request r = new Request(RequestCode.START_DOCUMENT_STREAM);
			socket = new Socket(Server.ADDRESS, Server.PORT_NUMBER);
			documentOutput = new ObjectOutputStream(socket.getOutputStream());
			documentOutput.writeObject(r);
		} catch (IOException e1) {
			System.out.println("Error: Couldn't start stream");
			e1.printStackTrace();
		}
	}

	/**
	 * This method sets up the text area.
	 */
	public void setTextArea(String startingText, EditableDocument document) {
		this.loadDocumentWindow = new LoadDoc();
		this.loadDocumentWindow.setVisible(false);
		tabbedpane = new TabbedPane(document.getName());
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
		StyleConstants.setRightIndent(style, 100);
		doc.setParagraphAttributes(0, doc.getLength(), style, false);
		tabbedpane.getCurrentTextPane().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				charCount++;
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if (charCount > SAVE_FREQUENCY) {
					backupDocument();
					charCount = 0;
				}

			}
		});
	}

	/**
	 * Sets up the new chat tab for the client
	 */
	public void setupChatTab() {
		chat = new ChatTab(user.getUsername());
		tabbedpane.addTab("Chat", chat);
		chat.updateConversation("D-R-P-C TEAM", "Welcome to the Global Chat Room!" + "\n");
	}

	/**
	 * This method sets up the tool bar.
	 */
	private void setJToolBar() {
		Image boldImage = null;
		Image italicImage = null;
		Image underlineImage = null;
		Image colorImage = null;
		Image bulletImage = null;
		Image imageImage = null;

		try {
			boldImage = ImageIO.read(new File("./images/boldImage.png"));
			italicImage = ImageIO.read(new File("./images/italicImage.png"));
			underlineImage = ImageIO.read(new File("./images/underlineImage.png"));
			colorImage = ImageIO.read(new File("./images/colorImage.png"));
			bulletImage = ImageIO.read(new File("./images/bulletImage.png"));
			imageImage = ImageIO.read(new File("./images/imageImage.png"));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error: Couldn't load an image on the toolbar");
		}

		boldFontButton = new JButton(new ImageIcon(boldImage));
		italicFontButton = new JButton(new ImageIcon(italicImage));
		underlineFontButton = new JButton(new ImageIcon(underlineImage));
		colorButton = new JButton(new ImageIcon(colorImage));
		imageButton = new JButton(new ImageIcon(imageImage));
		bulletListButton = new JToggleButton(new ImageIcon(bulletImage));

		javaToolBar.add(boldFontButton);
		javaToolBar.add(italicFontButton);
		javaToolBar.add(underlineFontButton);
		javaToolBar.add(bulletListButton);
		javaToolBar.add(imageButton);
		javaToolBar.add(colorButton);

		javaToolBar.addSeparator();
		javaToolBar.add(sizeFontDropDown);
		javaToolBar.addSeparator();
		javaToolBar.add(fontDropDown);

		this.add(javaToolBar, BorderLayout.NORTH);
	}

	/**
	 * Assemble the layout of the menuBar
	 */
	private void setupMenuBar() {
		JMenu file = new JMenu("File");
		JMenuItem createNewDocument = new JMenuItem("New Document");
		file.add(createNewDocument);
		JMenuItem loadDocument = new JMenuItem("Load Document");
		loadDocument.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				loadDocumentWindow.setVisible(true);
				loadDocumentWindow.loadDocuments();
			}

		});
		file.add(loadDocument);

		JMenu options = new JMenu("Options");
		JMenuItem changePassword = new JMenuItem("Change Password");
		options.add(changePassword);
		JMenuItem signout = new JMenuItem("Sign Out");
		options.add(signout);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(file);
		menuBar.add(options);

		MenuItemListener menuListener = new MenuItemListener();
		createNewDocument.addActionListener(menuListener);
		loadDocument.addActionListener(menuListener);

		changePassword.addActionListener(menuListener);
		signout.addActionListener(menuListener);

		JMenu exportMenu = new JMenu("Export Document");

		JMenuItem exportToPDFMenuItem = new JMenuItem("Export as PDF");
		exportToPDFMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DocumentExporter.printToPDF(tabbedpane.getCurrentTextPane(), tabbedpane.getName());
			}
		});
		exportMenu.add(exportToPDFMenuItem);

		JMenuItem exportToRTFMenuItem = new JMenuItem("Export as RTF");
		exportToRTFMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DocumentExporter.printToRTF(tabbedpane.getCurrentTextPane(), tabbedpane.getName());
			}
		});
		exportMenu.add(exportToRTFMenuItem);

		JMenuItem exportToHTMLMenuItem = new JMenuItem("Export as HTML");
		exportToHTMLMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				DocumentExporter.printToHTML(tabbedpane.getCurrentTextPane(), tabbedpane.getName());
			}
		});
		exportMenu.add(exportToHTMLMenuItem);
		file.addSeparator();
		file.add(exportMenu);

	}

	/**
	 * Assigns listeners to buttons in menu @author Stevo
	 *
	 */
	private class MenuItemListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String text = ((JMenuItem) e.getSource()).getText();
			if (text.equals("New Document")) {
				String newDocumentName = JOptionPane.showInputDialog("What would you like to name your new document?");
				if (newDocumentName != null) {
					tabbedpane.addNewTab(newDocumentName,
							new EditableDocument(new DefaultStyledDocument(), newDocumentName));
					StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
					Style style = tabbedpane.getCurrentTextPane().addStyle("Indent", null);
					StyleConstants.setLeftIndent(style, 30);
					StyleConstants.setRightIndent(style, 100);
					doc.setParagraphAttributes(0, doc.getLength(), style, false);
					tabbedpane.getCurrentTextPane().addKeyListener(new KeyAdapter() {
						@Override
						public void keyPressed(KeyEvent arg0) {
							charCount++;

						}

						@Override
						public void keyReleased(KeyEvent arg0) {
							if (charCount > SAVE_FREQUENCY) {
								backupDocument();
								charCount = 0;
							}

						}
					});
				}
			} else if (text.equals("Change Password")) {

				JLabel newPassword = new JLabel("New Password:");
				JPasswordField newPasswordField = new JPasswordField();
				Object[] forgotPasswordFields = { newPassword, newPasswordField };
				int response = JOptionPane.showConfirmDialog(null, forgotPasswordFields, "Change Password",
						JOptionPane.YES_NO_OPTION);
				if (response == JOptionPane.YES_OPTION) {
					System.out.println("Client: OPTION YES from " + user.getUsername());
					String clientUsername = user.getUsername();
					String clientPassword = String.valueOf(newPasswordField.getPassword());
					try {
						Request clientRequest = new Request(RequestCode.RESET_PASSWORD);
						clientRequest.setUsername(clientUsername);
						clientRequest.setPassword(clientPassword);
						oos.writeObject(clientRequest);

					} catch (IOException e1) {
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

	/**
	 * Handles assignments and functions associated with UserWindow
	 */
	private void setUsersWindow() {
		DefaultListModel<String> listmodel = new DefaultListModel<String>();
		final JList<String> list = new JList<String>(listmodel);
		JPopupMenu menu = new JPopupMenu();
		JMenuItem editorItem = new JMenuItem("Add as Editor");
		menu.add(editorItem);
		JMenuItem ownerItem = new JMenuItem("Make Owner");
		menu.add(ownerItem);
		JMenuItem messageItem = new JMenuItem("Send private message");
		menu.add(messageItem);

		editorItem.addActionListener((e) -> {
				ObjectOutputStream documentOutput = null;
				ObjectInputStream documentInput = null;
				Socket socket = null;
				try {
					Request r = new Request(RequestCode.START_DOCUMENT_STREAM);
					socket = new Socket(Server.ADDRESS, Server.PORT_NUMBER);
					documentOutput = new ObjectOutputStream(socket.getOutputStream());
					documentInput = new ObjectInputStream(socket.getInputStream());
					documentOutput.writeObject(r);
				} catch (IOException e1) {
					System.out.println("Error: Couldn't start stream");
					e1.printStackTrace();
				}
				Request request = new Request(RequestCode.ADD_USER_AS_EDITOR);
				request.setUsername(list.getSelectedValue());
				request.setDocumentName(tabbedpane.getTitleAt(tabbedpane.getSelectedIndex()));
				try {
					documentOutput.writeObject(request);
					Response response = (Response) documentInput.readObject();
					if (response.getResponseID() == ResponseCode.USER_ADDED) {
						System.out.println("Client: " + list.getSelectedValue() + " successfully added as editor");
					} else {
						System.out.println("Client: " + list.getSelectedValue() + " failed to be added as editor");
					}
					socket.close();
				} catch (IOException | ClassNotFoundException e1) {
					e1.printStackTrace();
				}
		});

		ownerItem.addActionListener((e) ->{
				ObjectOutputStream documentOutput = null;
				ObjectInputStream documentInput = null;
				Socket socket = null;
				try {
					Request r = new Request(RequestCode.START_DOCUMENT_STREAM);
					socket = new Socket(Server.ADDRESS, Server.PORT_NUMBER);
					documentOutput = new ObjectOutputStream(socket.getOutputStream());
					documentInput = new ObjectInputStream(socket.getInputStream());
					documentOutput.writeObject(r);
				} catch (IOException e1) {
					System.out.println("Error: Couldn't start stream");
					e1.printStackTrace();
				}

				Request request = new Request(RequestCode.CHANGE_OWNER);
				request.setUsername(list.getSelectedValue());
				request.setDocumentName(tabbedpane.getTitleAt(tabbedpane.getSelectedIndex()));
				try {
					documentOutput.writeObject(request);
					Response response = (Response) documentInput.readObject();
					if (response.getResponseID() == ResponseCode.USER_ADDED) {
						System.out.println(list.getSelectedValue() + " successfully added as owner");
					} else {
						System.out.println(list.getSelectedValue() + " failed to be added as owner");
					}
					socket.close();
				} catch (IOException | ClassNotFoundException e2) {
					e2.printStackTrace();
				}
		});

		messageItem.addActionListener((e)->{
				chat.sendPrivateMessage(user.getUsername(), list.getSelectedValue());
			});

		userslist = new UsersOnline(oos, listmodel, list, menu);
		userslist.init();
		JTabbedPane sidebar = new JTabbedPane();
		sidebar.add("Users Online", userslist);
		RevisionList revisionlist = new RevisionList(user, tabbedpane);
		sidebar.add("Revision History", revisionlist);
		this.add(sidebar, BorderLayout.EAST);
		this.addWindowListener(new LogOffListener(this.user.getUsername(), oos));
	}

	/**
	 * Functionality for saving the current document
	 */
	private void backupDocument() {
		if (!EditorGui.this.tabbedpane.getTitleAt(EditorGui.this.tabbedpane.getSelectedIndex()).equals("Chat")
				&& EditorGui.this.tabbedpane.getCurrentTextPane() != null) {

			Request r = new Request(RequestCode.DOCUMENT_SENT);
			EditableDocument currentDoc = new EditableDocument(tabbedpane.getCurrentTextPane().getStyledDocument(),
					user, tabbedpane.getName());
			if (currentDoc != null && summary != null)
				currentDoc.setSummary(summary.getSummary());
			r.setDocument(currentDoc);
			try {
				documentOutput.writeObject(r);
			} catch (IOException e1) {
				System.out.println("Error: Couldn't send document to server");
				e1.printStackTrace();
			}
		}
	}

	/**
	 * @author Brittany
	 * @author Stevo
	 * 
	 * This method adds listeners to the buttons and drop down boxes on the tool
	 * bar
	 */
	public void setButtonListeners() {
		// Assigns listener for boldButton
		boldFontButton.addActionListener((e) -> {
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				summary.boldEvent();
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
		});

		// Assigns listener for italicButton
		italicFontButton.addActionListener((e) -> {
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				summary.italicEvent();
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
		});

		// Assigns listener for underlineButton
		underlineFontButton.addActionListener((e) -> {
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				summary.underLineEvent();
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
		});

		// Assign listener for font size drop down menu
		sizeFontDropDown.addActionListener((e) -> {
			Integer fontSize = (int) sizeFontDropDown.getSelectedItem();

			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				summary.fontSizeEvent();
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("FontSize", null);
				StyleConstants.setFontSize(style, fontSize);
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		});

		// Assigns listener for font style drop down menu
		fontDropDown.addActionListener((e) -> {
			String stringFont = (String) fontDropDown.getSelectedItem();
			if (tabbedpane.getCurrentTextPane().getSelectedText() != null) {
				summary.fontEvent();
				int selectStart = tabbedpane.getCurrentTextPane().getSelectionStart();
				int selectEnd = tabbedpane.getCurrentTextPane().getSelectionEnd();
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				Style style = tabbedpane.getCurrentTextPane().addStyle("FontFamily", null);
				StyleConstants.setFontFamily(style, stringFont);
				doc.setCharacterAttributes(selectStart, selectEnd - selectStart, style, false);
			}
		});

		// Assigns listener for bullet list button
		bulletListButton.addActionListener((e) -> {
			if (bulletListButton.isSelected()) {
				StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
				try {
					doc.insertString(doc.getLength(), "\u2022  ", null);
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}

				tabbedpane.getCurrentTextPane().addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent arg0) {
						if (arg0.getKeyCode() == KeyEvent.VK_ENTER && bulletListButton.isSelected()) {
							summary.bulletEvent();
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
				});
			}
		});

		// Assigns listener to add image button
		imageButton.addActionListener((e) -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileNameExtensionFilter("JPG & GIF Images", "jpg", "gif"));

			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				try {
					BufferedImage image = ImageIO.read(chooser.getSelectedFile());
					StyledDocument doc = (StyledDocument) tabbedpane.getCurrentTextPane().getStyledDocument();
					Style style = doc.addStyle("image", null);
					StyleConstants.setIcon(style, new ImageIcon(image));
					doc.insertString(doc.getLength(), "ignored text", style);

				} catch (IOException e2) {
					e2.printStackTrace();
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	/**
	 * Handles incoming responses from the server to the client @author Stevo
	 *
	 */
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
					if (response.getResponseID() == ResponseCode.DOCUMENT_REFRESH) {
						openDocumentInCurrentTab(response.getEditableDocument());
					}
					if (response.getResponseID() == ResponseCode.ACCOUNT_RESET_PASSWORD_SUCCESSFUL) {
						JOptionPane.showConfirmDialog(null,
								"Your password has been successfully resest. Please sign outand back in for changes to take place.",
								"Password Successfully Reset", JOptionPane.OK_OPTION);
					} else if (response.getResponseID() == ResponseCode.ACCOUNT_RESET_PASSWORD_FAILED) {
						JOptionPane.showConfirmDialog(null, "Oops! Something went wrong :( Please try again!",
								"Password Failed to Reset", JOptionPane.OK_OPTION);
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * When a document is updated from another client, update local
		 * document @param doc
		 */
		private void openDocumentInCurrentTab(EditableDocument doc) {
			if (EditorGui.this.tabbedpane.getTitleAt(EditorGui.this.tabbedpane.getSelectedIndex())
					.equals(doc.getName())) {
				EditorGui.this.tabbedpane.getCurrentTextPane().setStyledDocument(doc.getDocument());
				EditorGui.this.tabbedpane.getCurrentTextPane().setCaretPosition(doc.getDocument().getLength());
			}

		}
	}

	/**
	 * Create a new SubGUI to handle loading documents @author Stevo
	 *
	 */
	class LoadDoc extends JFrame {

		private static final long serialVersionUID = -7332966050995052441L;
		private ObjectOutputStream documentOutput;
		private ObjectInputStream documentInput;
		private JPanel bottomPanel = new JPanel();
		private JButton newDocumentButton = new JButton("Create New Document");
		private JButton loadDocumentButton = new JButton("Refresh Document List");
		private Socket socket = null;
		private JTabbedPane openDocumentSelectorPane = new JTabbedPane();

		private JList<String> editorlist = new JList<String>();
		private JList<String> ownerlist = new JList<String>();
		private DefaultListModel<String> olistmodel = new DefaultListModel<String>();
		private DefaultListModel<String> elistmodel = new DefaultListModel<String>();

		/**
		 * A constructor for testing
		 * 
		 * @param user
		 *            the authenticated user
		 */
		public LoadDoc() {
			organizeLayout();
			assignListeners();
		}

		/**
		 * Functionality for opening document into a tab
		 */
		public void loadDocuments() {
			try {
				Request r = new Request(RequestCode.START_DOCUMENT_STREAM);
				socket = new Socket(Server.ADDRESS, Server.PORT_NUMBER);
				documentOutput = new ObjectOutputStream(socket.getOutputStream());
				documentInput = new ObjectInputStream(socket.getInputStream());
				documentOutput.writeObject(r);
			} catch (IOException e1) {
				System.out.println("Error: Couldn't start stream");
				e1.printStackTrace();
			}

			Request request = new Request(RequestCode.REQUEST_DOCUMENT_LIST);
			request.setUsername(user.getUsername());

			try {
				documentOutput.writeObject(request);
				Response response = (Response) documentInput.readObject();
				updateDocumentList(response.getEditorList(), elistmodel);
				updateDocumentList(response.getOwnerList(), olistmodel);
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Assemble the layout for the loading GUI
		 */
		private void organizeLayout() {
			this.setTitle("Open Document");
			this.setSize(400, 450);
			JScrollPane escrollpane;
			elistmodel = new DefaultListModel<String>();
			editorlist = new JList<String>(elistmodel);
			escrollpane = new JScrollPane(editorlist, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			escrollpane.setPreferredSize(new Dimension(120, 100));
			setLayout(new BorderLayout());

			JScrollPane oscrollpane;
			ownerlist = new JList<String>(this.olistmodel);
			oscrollpane = new JScrollPane(ownerlist, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oscrollpane.setPreferredSize(new Dimension(120, 100));
			setLayout(new BorderLayout());

			openDocumentSelectorPane.addTab("Owned By You", oscrollpane);
			openDocumentSelectorPane.addTab("Editable By You", escrollpane);

			this.add(openDocumentSelectorPane);
			bottomPanel.add(loadDocumentButton);

			bottomPanel.add(newDocumentButton);
			this.add(bottomPanel, BorderLayout.SOUTH);
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
		}

		/**
		 * Assign listeners to various components
		 */
		private void assignListeners() {
			loadDocumentButton.addActionListener(new LoadButtonListener());
			newDocumentButton.addActionListener(new CreateNewDocumentListener());
			ownerlist.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					if (event.getClickCount() == 2) {
						launchDocument(ownerlist.getSelectedValue());
					}
				}

			});

			editorlist.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					if (event.getClickCount() == 2) {
						launchDocument(editorlist.getSelectedValue());
					}
				}
			});
		}

		/**
		 * Listener for testing saving and loading files @author Stevo
		 *
		 */
		private class LoadButtonListener implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadDocuments();
			}
		}

		/**
		 * Listener for newDocument button @author Stevo
		 *
		 */
		private class CreateNewDocumentListener implements ActionListener {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newDocumentName = JOptionPane.showInputDialog("What would you like to name your new document?");
				tabbedpane.addNewTab(newDocumentName,
						new EditableDocument(new DefaultStyledDocument(), newDocumentName));
				tabbedpane.getCurrentTextPane().addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent arg0) {
						charCount++;
					}

					@Override
					public void keyReleased(KeyEvent arg0) {
						if (charCount > SAVE_FREQUENCY) {
							backupDocument();
							charCount = 0;
						}
					}
				});
			}
		}

		/**
		 * Updates the document list with all currently available documents
		 * 
		 * @param documentList
		 *            The document list @param listmodel the default list model
		 */
		public void updateDocumentList(String[] documentList, DefaultListModel<String> listmodel) {
			for (int i = 0; i < documentList.length; i++) {
				if (documentList[i].substring(0, 1).equals("-")) {
					if (listmodel.contains(documentList[i].substring(1, documentList[i].length()))) {
						listmodel.removeElement(documentList[i].substring(1, documentList[i].length()));
					}
				} else if (!listmodel.contains(documentList[i])) {
					listmodel.addElement(documentList[i]);
				}
			}
		}

		/**
		 * Functionality for loading selected document into a new tab @param
		 * documentName
		 */
		private void launchDocument(String documentName) {
			Request requestDocument = new Request(RequestCode.REQUEST_DOCUMENT);
			requestDocument.setRequestedName(documentName);
			requestDocument.setUser(user);

			try {
				documentOutput.writeObject(requestDocument);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			try {
				Response serverRequest = (Response) documentInput.readObject();
				EditableDocument openedDocument = serverRequest.getEditableDocument();
				tabbedpane.addNewTab(openedDocument.getName(), openedDocument);
				tabbedpane.getCurrentTextPane().addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent arg0) {
						charCount++;
					}

					@Override
					public void keyReleased(KeyEvent arg0) {
						if (charCount > SAVE_FREQUENCY) {
							backupDocument();
							charCount = 0;
						}
					}
				});
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

	}
}