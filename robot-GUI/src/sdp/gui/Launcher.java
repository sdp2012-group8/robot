package sdp.gui;

import javax.swing.JFrame;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.Utilities;
import sdp.common.WorldStateObserver;
import sdp.gui.filefilters.ImageFileFilter_IO;
import sdp.vision.Vision;
import sdp.vision.processing.MainImageProcessor;
import sdp.vision.processing.SecondaryImageProcessor;
import sdp.vision.visualinput.CameraVisualInputProvider;
import sdp.vision.visualinput.ImageVisualInputProvider;

import javax.swing.JButton;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.io.File;


/**
 * This is a temporary class for carrying out testing of the old GUI interface.
 * 
 * @author Gediminas Liktaras
 */
public class Launcher extends JFrame implements Runnable {
	
	/** Required by Serializable. */
	private static final long serialVersionUID = -2969477954373112982L;
	
	/** The window's test image directory chooser. */
	private JFileChooser imageDirChooser;
	
	
	/**
	 * The main constructor.
	 */
	public Launcher() {
		imageDirChooser = new JFileChooser("../robot-VISION/data");
		imageDirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		initComponents();
	}
	
	
	/**
	 * Show a dialog to select the image directory.
	 */
	private void selectImageDirectory() {
		int retValue = imageDirChooser.showOpenDialog(this);
		
		if (retValue == JFileChooser.APPROVE_OPTION) {
			String chosenDir = imageDirChooser.getSelectedFile().getAbsolutePath();
			testImageDirTextfield.setText(chosenDir);
		}
	}
	

	/**
	 * Start camera vision test. 
	 */
	private void startCompetitionMode() {
		Vision vision = null;
		switch (processorComboBox.getSelectedIndex()) {
		case 0:
			vision = new Vision(new MainImageProcessor());
			break;
		case 1:
			vision = new Vision(new SecondaryImageProcessor());
			break;
		}
		
		WorldStateObserver visionObserver = new WorldStateObserver(vision);		
		CameraVisualInputProvider input = createCameraInputProvider();
		input.setCallback(vision);
		MainWindow mainGui = new MainWindow(false, visionObserver, vision);
		
		input.startCapture();
		(new Thread(mainGui, "GUI")).start();
		killLauncherWindow();
	}
	
	/**
	 * Start image vision test.
	 */
	private void startTestingMode() {
		String imageDir = Utilities.stripString(testImageDirTextfield.getText());
		if (imageDir.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No image directory specified.",
					"No Directory", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		Vision vision = null;
		switch (processorComboBox.getSelectedIndex()) {
		case 0:
			vision = new Vision(new MainImageProcessor());
			break;
		case 1:
			vision = new Vision(new SecondaryImageProcessor());
			break;
		}
		
		WorldStateObserver visionObserver = new WorldStateObserver(vision);
		
		ImageVisualInputProvider input = createImageInputProvider(imageDir);
		if (input == null) {
			return;
		}
		input.setCallback(vision);
		
		MainWindow mainGui = new MainWindow(true, visionObserver, vision);
		
		input.startCapture();
		(new Thread(mainGui, "GUI")).start();		
		killLauncherWindow();
	}

		
	/**
	 * Create camera input provider with the values given in the GUI components.
	 */
	private CameraVisualInputProvider createCameraInputProvider() {
		String deviceFile = deviceFileTextField.getText();
		int channel = ((Integer)channelSpinner.getValue()).intValue();
		
		int standard = 0;
		String standardString = (String)standardComboBox.getSelectedItem();
		if (standardString == "NTSC") {
			standard = V4L4JConstants.STANDARD_NTSC;
		} else if (standardString == "PAL") {
			standard = V4L4JConstants.STANDARD_PAL;
		} else if (standardString == "SECAM") {
			standard = V4L4JConstants.STANDARD_SECAM;
		} else if (standardString == "WEBCAM") {
			standard = V4L4JConstants.STANDARD_WEBCAM;
		}
		
		return new CameraVisualInputProvider(deviceFile, standard, channel);
	}
	
	/**
	 * Create image input provider with the values given in the GUI components.
	 * 
	 * @param imageDir Directory with images to use for testing.
	 */
	private ImageVisualInputProvider createImageInputProvider(String imageDir) {
		File[] files = null;
		try {
			files = (new File(imageDir)).listFiles(new ImageFileFilter_IO());
		} catch (NullPointerException e) {
			JOptionPane.showMessageDialog(this,	"Non-existent directory specified.",
					"Bad Directory", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		
		if (files.length == 0) {
			JOptionPane.showMessageDialog(this,	"No images found in the specified directory.",
					"No Images", JOptionPane.WARNING_MESSAGE);
			return null;
		}
		
		String filenames[] = new String[files.length];
		for (int i = 0; i < files.length; ++i) {
			filenames[i] = files[i].getAbsolutePath();
		}
		
		int fps = ((Integer)testFpsSpinner.getValue()).intValue();
		return new ImageVisualInputProvider(filenames, fps);
	}

	
	/**
	 * Dispose of the launcher window.
	 */
	private void killLauncherWindow() {
		setVisible(false);
		dispose();
	}
	
	
	/**
	 * The thread's run method.
	 */
	@Override
	public void run() {
		setVisible(true);
	}
	
	
	/**
	 * The main method.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		Launcher app = new Launcher();
		SwingUtilities.invokeLater(app);
	}
	
	
	/**
	 * Initialise GUI components.
	 */
	private void initComponents() {
		setSize(new Dimension(250, 300));
		setTitle("Launcher");
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 200, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 25, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, 0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel generalSettingPanel = new JPanel();
		GridBagConstraints gbc_generalSettingPanel = new GridBagConstraints();
		gbc_generalSettingPanel.fill = GridBagConstraints.BOTH;
		gbc_generalSettingPanel.insets = new Insets(0, 0, 5, 5);
		gbc_generalSettingPanel.gridx = 1;
		gbc_generalSettingPanel.gridy = 1;
		getContentPane().add(generalSettingPanel, gbc_generalSettingPanel);
		GridBagLayout gbl_generalSettingPanel = new GridBagLayout();
		gbl_generalSettingPanel.columnWidths = new int[]{0, 0, 0};
		gbl_generalSettingPanel.rowHeights = new int[]{24, 0};
		gbl_generalSettingPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_generalSettingPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		generalSettingPanel.setLayout(gbl_generalSettingPanel);
		
		JLabel processorLabel = new JLabel("Processor");
		GridBagConstraints gbc_processorLabel = new GridBagConstraints();
		gbc_processorLabel.insets = new Insets(0, 0, 0, 5);
		gbc_processorLabel.anchor = GridBagConstraints.EAST;
		gbc_processorLabel.gridx = 0;
		gbc_processorLabel.gridy = 0;
		generalSettingPanel.add(processorLabel, gbc_processorLabel);
		
		processorComboBox = new JComboBox();
		GridBagConstraints gbc_processorComboBox = new GridBagConstraints();
		gbc_processorComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_processorComboBox.anchor = GridBagConstraints.NORTH;
		gbc_processorComboBox.gridx = 1;
		gbc_processorComboBox.gridy = 0;
		generalSettingPanel.add(processorComboBox, gbc_processorComboBox);
		processorComboBox.setModel(new DefaultComboBoxModel(new String[] {"Main", "Secondary"}));
		
		JPanel competitionModePanel = new JPanel();
		GridBagConstraints gbc_competitionModePanel = new GridBagConstraints();
		gbc_competitionModePanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_competitionModePanel.anchor = GridBagConstraints.NORTH;
		gbc_competitionModePanel.insets = new Insets(0, 0, 5, 5);
		gbc_competitionModePanel.gridx = 1;
		gbc_competitionModePanel.gridy = 2;
		getContentPane().add(competitionModePanel, gbc_competitionModePanel);
		competitionModePanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Competition mode", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagLayout gbl_competitionModePanel = new GridBagLayout();
		gbl_competitionModePanel.columnWidths = new int[]{0, 0, 0};
		gbl_competitionModePanel.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_competitionModePanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_competitionModePanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		competitionModePanel.setLayout(gbl_competitionModePanel);
		
		JLabel deviceFileLabel = new JLabel("Device file");
		GridBagConstraints gbc_deviceFileLabel = new GridBagConstraints();
		gbc_deviceFileLabel.insets = new Insets(0, 0, 5, 5);
		gbc_deviceFileLabel.anchor = GridBagConstraints.EAST;
		gbc_deviceFileLabel.gridx = 0;
		gbc_deviceFileLabel.gridy = 0;
		competitionModePanel.add(deviceFileLabel, gbc_deviceFileLabel);
		
		deviceFileTextField = new JTextField();
		deviceFileTextField.setText("/dev/video0");
		deviceFileLabel.setLabelFor(deviceFileTextField);
		GridBagConstraints gbc_deviceFileTextField = new GridBagConstraints();
		gbc_deviceFileTextField.insets = new Insets(0, 0, 5, 0);
		gbc_deviceFileTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_deviceFileTextField.gridx = 1;
		gbc_deviceFileTextField.gridy = 0;
		competitionModePanel.add(deviceFileTextField, gbc_deviceFileTextField);
		deviceFileTextField.setColumns(10);
		
		JLabel standardLabel = new JLabel("Standard");
		GridBagConstraints gbc_standardLabel = new GridBagConstraints();
		gbc_standardLabel.anchor = GridBagConstraints.EAST;
		gbc_standardLabel.insets = new Insets(0, 0, 5, 5);
		gbc_standardLabel.gridx = 0;
		gbc_standardLabel.gridy = 1;
		competitionModePanel.add(standardLabel, gbc_standardLabel);
		
		standardComboBox = new JComboBox();
		standardComboBox.setModel(new DefaultComboBoxModel(new String[] {"NTSC", "PAL", "SECAM", "WEBCAM"}));
		standardComboBox.setSelectedIndex(3);
		standardLabel.setLabelFor(standardComboBox);
		GridBagConstraints gbc_standardComboBox = new GridBagConstraints();
		gbc_standardComboBox.insets = new Insets(0, 0, 5, 0);
		gbc_standardComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_standardComboBox.gridx = 1;
		gbc_standardComboBox.gridy = 1;
		competitionModePanel.add(standardComboBox, gbc_standardComboBox);
		
		JLabel channelLabel = new JLabel("Channel");
		GridBagConstraints gbc_channelLabel = new GridBagConstraints();
		gbc_channelLabel.anchor = GridBagConstraints.EAST;
		gbc_channelLabel.insets = new Insets(0, 0, 5, 5);
		gbc_channelLabel.gridx = 0;
		gbc_channelLabel.gridy = 2;
		competitionModePanel.add(channelLabel, gbc_channelLabel);
		
		channelSpinner = new JSpinner();
		channelLabel.setLabelFor(channelSpinner);
		channelSpinner.setModel(new SpinnerNumberModel(0, 0, 99, 1));
		GridBagConstraints gbc_channelSpinner = new GridBagConstraints();
		gbc_channelSpinner.anchor = GridBagConstraints.EAST;
		gbc_channelSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_channelSpinner.gridx = 1;
		gbc_channelSpinner.gridy = 2;
		competitionModePanel.add(channelSpinner, gbc_channelSpinner);
		
		JButton startCompButton = new JButton("Start competition");
		GridBagConstraints gbc_startCompButton = new GridBagConstraints();
		gbc_startCompButton.gridwidth = 2;
		gbc_startCompButton.gridx = 0;
		gbc_startCompButton.gridy = 3;
		competitionModePanel.add(startCompButton, gbc_startCompButton);
		
		JPanel testModePanel = new JPanel();
		testModePanel.setBorder(new TitledBorder(null, "Test mode", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_testModePanel = new GridBagConstraints();
		gbc_testModePanel.insets = new Insets(0, 0, 5, 5);
		gbc_testModePanel.anchor = GridBagConstraints.NORTH;
		gbc_testModePanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_testModePanel.gridx = 1;
		gbc_testModePanel.gridy = 3;
		getContentPane().add(testModePanel, gbc_testModePanel);
		GridBagLayout gbl_testModePanel = new GridBagLayout();
		gbl_testModePanel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_testModePanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_testModePanel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_testModePanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		testModePanel.setLayout(gbl_testModePanel);
		
		JLabel testImagesLabel = new JLabel("Images");
		GridBagConstraints gbc_testImagesLabel = new GridBagConstraints();
		gbc_testImagesLabel.anchor = GridBagConstraints.EAST;
		gbc_testImagesLabel.insets = new Insets(0, 0, 5, 5);
		gbc_testImagesLabel.gridx = 0;
		gbc_testImagesLabel.gridy = 0;
		testModePanel.add(testImagesLabel, gbc_testImagesLabel);
		
		testImageDirTextfield = new JTextField();
		testImageDirTextfield.setText("../robot-VISION/data/friendly");
		GridBagConstraints gbc_testImageDirTextfield = new GridBagConstraints();
		gbc_testImageDirTextfield.insets = new Insets(0, 0, 5, 5);
		gbc_testImageDirTextfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_testImageDirTextfield.gridx = 1;
		gbc_testImageDirTextfield.gridy = 0;
		testModePanel.add(testImageDirTextfield, gbc_testImageDirTextfield);
		testImageDirTextfield.setColumns(10);
		
		JButton testDirSelectButton = new JButton("...");
		testDirSelectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectImageDirectory();
			}
		});
		GridBagConstraints gbc_testDirSelectButton = new GridBagConstraints();
		gbc_testDirSelectButton.insets = new Insets(0, 0, 5, 0);
		gbc_testDirSelectButton.gridx = 2;
		gbc_testDirSelectButton.gridy = 0;
		testModePanel.add(testDirSelectButton, gbc_testDirSelectButton);
		
		JLabel testFpsLabel = new JLabel("FPS");
		GridBagConstraints gbc_testFpsLabel = new GridBagConstraints();
		gbc_testFpsLabel.anchor = GridBagConstraints.EAST;
		gbc_testFpsLabel.insets = new Insets(0, 0, 5, 5);
		gbc_testFpsLabel.gridx = 0;
		gbc_testFpsLabel.gridy = 1;
		testModePanel.add(testFpsLabel, gbc_testFpsLabel);
		
		testFpsSpinner = new JSpinner();
		testFpsSpinner.setModel(new SpinnerNumberModel(25, 1, 60, 1));
		GridBagConstraints gbc_testFpsSpinner = new GridBagConstraints();
		gbc_testFpsSpinner.anchor = GridBagConstraints.EAST;
		gbc_testFpsSpinner.gridwidth = 2;
		gbc_testFpsSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_testFpsSpinner.gridx = 1;
		gbc_testFpsSpinner.gridy = 1;
		testModePanel.add(testFpsSpinner, gbc_testFpsSpinner);
		
		JButton startTestButton = new JButton("Start testing");
		GridBagConstraints gbc_startTestButton = new GridBagConstraints();
		gbc_startTestButton.insets = new Insets(0, 0, 0, 5);
		gbc_startTestButton.gridwidth = 3;
		gbc_startTestButton.gridx = 0;
		gbc_startTestButton.gridy = 2;
		testModePanel.add(startTestButton, gbc_startTestButton);
		startTestButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				startTestingMode();
			}
		});
		startCompButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				startCompetitionMode();
			}
		});
	}
	
	
	private JTextField deviceFileTextField;
	private JComboBox standardComboBox;
	private JSpinner channelSpinner;
	private JSpinner testFpsSpinner;
	private JComboBox processorComboBox;
	private JTextField testImageDirTextfield;
}
