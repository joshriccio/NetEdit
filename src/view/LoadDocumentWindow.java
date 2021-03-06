package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;

import model.EditableDocument;
import model.User;
import network.Request;
import network.RequestCode;
import network.Server;
import network.Response;

/**
 * The intermediary gui between the main gui and the login screen
 * 
 * @author Stephen Connolly
 *
 */
public class LoadDocumentWindow extends JFrame {

	private static final long serialVersionUID = -7332966050995052441L;
	private User user;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
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
	 * The constructor when Streams are also being sent into the gui
	 * 
	 * @param objectOutputStream
	 *            The current stream that has been authenticated
	 * @param objectInputStream
	 *            The current stream that has been authenticated
	 * @param user
	 *            The user that has been authenticated
	 */
	public LoadDocumentWindow(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream, User user) {
		this.oos = objectOutputStream;
		this.ois = objectInputStream;
		this.user = user;
		organizeLayout();
		assignListeners();
		loadDocuments();
	}

	/**
	 * Functionality for opening documents into a new tab
	 */
	private void loadDocuments() {
		ObjectOutputStream documentOutput = null;
		ObjectInputStream documentInput = null;
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
	 * Assemble the layout of the SubGUI
	 */
	private void organizeLayout() {
		this.setTitle("Welcome: " + user.getUsername());
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
	 * Assign listeners for various SubGUI aspects
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

	/** Listener for testing saving and loading files
	 * 
	 * @author Stevo
	 *
	 */
	private class LoadButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			loadDocuments();
		}
	}

	/**
	 * Listener for NewDocument Button
	 * @author Stevo
	 *
	 */
	private class CreateNewDocumentListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			String newDocumentName = JOptionPane.showInputDialog("What would you like to name your new document?");
			EditableDocument doc = new EditableDocument(new DefaultStyledDocument(), user, newDocumentName);
			EditorGui editor = new EditorGui(oos, ois, user, doc);
			editor.setVisible(true);
			dispose();
		}
	}

	/**
	 * Updates the document list with all currently available documents
	 * 
	 * @param documentList
	 *            The document list
	 * @param listmodel
	 *            the default list model
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
	 * Adds a document tab to the EditorGUI
	 * @param documentName
	 * 				The name of the document to open
	 */
	private void launchDocument(String documentName) {
		Request requestDocument = new Request(RequestCode.REQUEST_DOCUMENT);
		requestDocument.setRequestedName(documentName);
		requestDocument.setUser(user);

		try {
			oos.writeObject(requestDocument);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			Response serverRequest = (Response) ois.readObject();
			EditableDocument openedDocument = serverRequest.getEditableDocument();
			EditorGui editor = new EditorGui(oos, ois, user, openedDocument);
			editor.setVisible(true);
			dispose();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}