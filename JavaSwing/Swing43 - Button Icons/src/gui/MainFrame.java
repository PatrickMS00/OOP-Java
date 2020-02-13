package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import controller.Controller;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private TextPanel textPanel;
	private Toolbar toolbar;
	private FormPanel formPanel;
	private JFileChooser fileChooser;
	private Controller controller;
	private TablePanel tablePanel;
	private PrefsDialog prefsDialog;
	private Preferences prefs;

	public MainFrame() {
		super("Button Icons");

		setLayout(new BorderLayout());

		toolbar = new Toolbar();
		textPanel = new TextPanel();
		formPanel = new FormPanel();
		tablePanel = new TablePanel();
		/*
		 * MainFrame extends JFrame, so 'this' is a JFrame, the right kind of
		 * field for the constructor
		 */
		prefsDialog = new PrefsDialog(this);
		prefs = Preferences.userRoot().node("db");

		controller = new Controller();

		tablePanel.setData(controller.getPeople());

		tablePanel.setPersonTableListener(new PersonTableListener() {
			public void rowDeleted(int row) {
				controller.removePerson(row);
			}
		});

		prefsDialog.setPrefsListener(new PrefsListener() {

			@Override
			public void preferencesSet(String user, String password, int port) {
				prefs.put("user", user);
				prefs.put("password", password);
				prefs.putInt("port", port);
				System.out.println(user + ": " + password + ", port: " + port);
			}

		});
		String user = prefs.get("user", "Pedro");
		String password = prefs.get("password", "here");
		int port = prefs.getInt("port", 3306);
		prefsDialog.setDefaults(user, password, port);

		fileChooser = new JFileChooser();
		fileChooser.addChoosableFileFilter(new PersonFileFilter());
		setJMenuBar(createMenuBar());

		toolbar.setToolbarListener(new ToolbarListener() {

			@Override
			public void saveEventOccurred() {
				
				connect();
				
				try {
					controller.save();
				} catch (SQLException e) {
					JOptionPane.showMessageDialog(MainFrame.this, "Cannot save to Database",
							"Database Connection Problem", JOptionPane.ERROR_MESSAGE);					
				}
			}

			@Override
			public void refreshEventOccurred() {
				
				connect();
				
				try {
					controller.load();
				} catch (SQLException e) {
					JOptionPane.showMessageDialog(MainFrame.this, "Cannot load from Database",
							"Database Connection Problem", JOptionPane.ERROR_MESSAGE);					
				}
				
				tablePanel.refresh();
			}
		});

		formPanel.setFormListener(new FormListener() {
			public void formEventOccurred(FormEvent ev) {
				// textPanel.appendText(name + ": " + occupation + ": "
				// + ageCategory + ", " + empCat + ", " + gender + "\n");
				controller.addPerson(ev);
				tablePanel.refresh();
			}
		});

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				controller.disconnect();
				dispose();
				System.gc();
			}
			
		});
		
		add(toolbar, BorderLayout.NORTH);
		add(formPanel, BorderLayout.WEST);
		// add(textPanel, BorderLayout.CENTER);
		add(tablePanel, BorderLayout.CENTER);

		setSize(800, 500);
		// to avoid fields collapsing and disappearing on resizing
		setMinimumSize(new Dimension(500, 500));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setVisible(true);
	}

	private void connect() {
		try {
			controller.connect();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(MainFrame.this, "Cannot connect to Database",
					"Database Connection Problem", JOptionPane.ERROR_MESSAGE);
		}
		
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		JMenu windowMenu = new JMenu("Window");
		JMenu showMenu = new JMenu("Show");
		JMenuItem prefsItem = new JMenuItem("Preferences...");

		JMenuItem exportDataItem = new JMenuItem("Export data...");
		JMenuItem importDataItem = new JMenuItem("Import data...");
		JMenuItem exitFileMenu = new JMenuItem("Exit");

		fileMenu.add(exportDataItem);
		fileMenu.add(importDataItem);
		fileMenu.addSeparator();
		fileMenu.add(exitFileMenu);

		// this checkbox sets the visibility of the whole form
		JMenuItem showFormItem = new JCheckBoxMenuItem("Person Form");
		showFormItem.setSelected(true);

		showMenu.add(showFormItem);
		windowMenu.add(showMenu);
		windowMenu.add(prefsItem);

		menuBar.add(fileMenu);
		menuBar.add(windowMenu);

		showFormItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) ev.getSource();
				formPanel.setVisible(menuItem.isSelected());
			}
		});

		/*
		 * Mnemonics and Accelerators are ways to increase accessibility and
		 * speed for power users. Mnemonics add a shortcut for menu items. By
		 * pressing Alt, a letter is underlined in the item or menu. They do not
		 * work in OS X.
		 */
		// Alt + F as shortcut for File menu
		fileMenu.setMnemonic(KeyEvent.VK_F);
		// Exit the app with a mnemonic
		exitFileMenu.setMnemonic(KeyEvent.VK_X);
		// Exit the app with an accelerator (Ctrl + X)
		exitFileMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		importDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
		prefsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));

		importDataItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				// this is the way to show the explorer to pickup a file:
				// fileChooser.showOpenDialog(MainFrame.this);
				// if a file is selected and click OK:
				if (fileChooser.showOpenDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
					try {
						controller.loadFromFile(fileChooser.getSelectedFile());
						tablePanel.refresh();
					} catch (IOException e) {
						JOptionPane.showMessageDialog(MainFrame.this, "Could not load data from file", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}

			}
		});

		exportDataItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// same as above, but for saving a file in a location
				if (fileChooser.showSaveDialog(MainFrame.this) == JFileChooser.APPROVE_OPTION) {
					try {
						controller.saveToFile(fileChooser.getSelectedFile());
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(MainFrame.this, "Could not save data to file", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});

		exitFileMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*
				 * the INFORMATION_MESSAGE option sets a different icon. Explore
				 * also WARNING_MESSAGE, QUESTION_MESSAGE, and ERROR_MESSAGE
				 */
				// the text entered can be retrieved through the variable
				// textEntered
				String textEntered = JOptionPane.showInputDialog(MainFrame.this, "Enter your User ID",
						"User Authentication", JOptionPane.OK_OPTION | JOptionPane.INFORMATION_MESSAGE);

				// show a confirmation panel (remember Android?), retrieve the
				// action
				int action = JOptionPane.showConfirmDialog(MainFrame.this, "Do you really want to exit the app?",
						"Confirm Exit", JOptionPane.OK_CANCEL_OPTION);
				if (action == JOptionPane.OK_OPTION) {
					WindowListener[] listeners = getWindowListeners();
					
					for (WindowListener windowListener : listeners) {
						windowListener.windowClosing(new WindowEvent(MainFrame.this, 0));
					}
				}
			}
		});

		prefsItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				prefsDialog.setVisible(true);
			}
		});

		return menuBar;
	}
}
