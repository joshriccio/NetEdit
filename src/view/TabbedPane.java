package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import model.EditableDocument;

/**
 * TabbedPane extends JTabbedPane to add additional functionality, including
 * getting context regarding with tab is currently open
 * 
 * @author Joshua Riccio
 *
 */
public class TabbedPane extends JTabbedPane {

	private static final long serialVersionUID = 1L;
	private HashMap<String, JTextPane> textpanemap;
	private JPopupMenu menu;

	/**
	 * The constructor takes in the name of the new document
	 * 
	 * @param docName the name of the new document
	 */
	public TabbedPane(String docName) {
		textpanemap = new HashMap<>();
		JTextPane textpane = new JTextPane();
		textpanemap.put(docName, textpane);
		this.menu = new JPopupMenu();
		this.setupMenu();
		textpane.setPreferredSize(new Dimension(100, 100));
		textpane.setBackground(Color.WHITE);
		JScrollPane scrollpane = new JScrollPane(textpane);
		Border borderOutline = BorderFactory.createLineBorder(Color.GRAY);
		textpane.setBorder(borderOutline);
		this.addTab(docName, scrollpane);
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) && !TabbedPane.this.getName().equals("Chat")) {
					TabbedPane.this.menu.show(e.getComponent(), e.getX(), e.getY());
				} 
			}
		});
	}

	/**
	 * Makes tab closable with right click
	 */
	private void setupMenu() {
		JMenuItem item = new JMenuItem("Close Tab");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				TabbedPane.this.remove(TabbedPane.this.getSelectedIndex());

			}

		});
		this.menu.add(item);

	}

	/**
	 * Adds a new tab with the new document name
	 * 
	 * @param docName the name of the new document
	 */
	public void addNewTab(String docName, EditableDocument doc) {
		JTextPane textpane = new JTextPane();
		textpane.setStyledDocument(doc.getDocument());
		textpanemap.put(docName, textpane);
		textpane.setPreferredSize(new Dimension(100, 100));
		textpane.setBackground(Color.WHITE);
		JScrollPane scrollpane = new JScrollPane(textpane);
		Border borderOutline = BorderFactory.createLineBorder(Color.GRAY);
		textpane.setBorder(borderOutline);
		this.addTab(docName, scrollpane);
		this.setSelectedComponent(scrollpane);
	}

	/**
	 * Gets the TextPane of the currently viewed tab
	 * 
	 * @return returns the textpane of the currently viewed tab
	 */
	public JTextPane getCurrentTextPane() {
		if (this.getSelectedIndex() == -1 || this.getTitleAt(this.getSelectedIndex()).equals("Chat"))
			return null;
		return textpanemap.get(this.getTitleAt(this.getSelectedIndex()));
	}

	/**
	 * Gets the title of the currently vied tab
	 * 
	 * @return returns the title of the currently viewed tab
	 */
	public String getName() {
		return this.getTitleAt(this.getSelectedIndex());
	}

}