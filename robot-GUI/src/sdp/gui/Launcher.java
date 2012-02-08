package sdp.gui;

import javax.swing.JFrame;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldStateObserver;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.ImageVisualInputProvider;
import sdp.vision.Vision;
import javax.swing.JButton;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JPanel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Dimension;

/**
 * This is a temporary class for carrying out testing of the old GUI interface.
 * 
 * @author Gediminas Liktaras
 */
public class Launcher extends JFrame implements Runnable {
	
	/** Required by Serializable. */
	private static final long serialVersionUID = -2969477954373112982L;
	
	
	/** Text field for camera device file. */
	private JTextField deviceFileTextField;
	
	/** Combobox for selecting camera capture standard. */
	private JComboBox standardComboBox;
	
	/** A spinner for selecting the camera channel. */
	private JSpinner channelSpinner;
	
	
	/**
	 * The main constructor.
	 */
	public Launcher() {
		setSize(new Dimension(370, 130));
		setTitle("Launcher");
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{150, 0, 0};
		gridBagLayout.rowHeights = new int[]{25, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel startPanel = new JPanel();
		GridBagConstraints gbc_startPanel = new GridBagConstraints();
		gbc_startPanel.insets = new Insets(0, 0, 0, 5);
		gbc_startPanel.fill = GridBagConstraints.BOTH;
		gbc_startPanel.gridx = 0;
		gbc_startPanel.gridy = 0;
		getContentPane().add(startPanel, gbc_startPanel);
		GridBagLayout gbl_startPanel = new GridBagLayout();
		gbl_startPanel.columnWidths = new int[]{0, 0};
		gbl_startPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0};
		gbl_startPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_startPanel.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		startPanel.setLayout(gbl_startPanel);
		
		JButton startGameButton = new JButton("Start game");
		startGameButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
			}
		});
		GridBagConstraints gbc_startGameButton = new GridBagConstraints();
		gbc_startGameButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_startGameButton.insets = new Insets(0, 0, 5, 0);
		gbc_startGameButton.gridx = 0;
		gbc_startGameButton.gridy = 1;
		startPanel.add(startGameButton, gbc_startGameButton);
		
		JButton cameraVisionTestButton = new JButton("Vision test (camera)");
		cameraVisionTestButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				startCameraVisionTest();
			}
		});
		GridBagConstraints gbc_cameraVisionTestButton = new GridBagConstraints();
		gbc_cameraVisionTestButton.insets = new Insets(0, 0, 5, 0);
		gbc_cameraVisionTestButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_cameraVisionTestButton.gridx = 0;
		gbc_cameraVisionTestButton.gridy = 2;
		startPanel.add(cameraVisionTestButton, gbc_cameraVisionTestButton);
		
		JButton imageVisionTestButton = new JButton("Vision test (images)");
		imageVisionTestButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				startImageVisionTest();
			}
		});
		GridBagConstraints gbc_imageVisionTestButton = new GridBagConstraints();
		gbc_imageVisionTestButton.insets = new Insets(0, 0, 5, 0);
		gbc_imageVisionTestButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_imageVisionTestButton.gridx = 0;
		gbc_imageVisionTestButton.gridy = 3;
		startPanel.add(imageVisionTestButton, gbc_imageVisionTestButton);
		
		JPanel settingsPanel = new JPanel();
		GridBagConstraints gbc_settingsPanel = new GridBagConstraints();
		gbc_settingsPanel.fill = GridBagConstraints.BOTH;
		gbc_settingsPanel.gridx = 1;
		gbc_settingsPanel.gridy = 0;
		getContentPane().add(settingsPanel, gbc_settingsPanel);
		GridBagLayout gbl_settingsPanel = new GridBagLayout();
		gbl_settingsPanel.columnWidths = new int[]{235, 0};
		gbl_settingsPanel.rowHeights = new int[]{0, 0};
		gbl_settingsPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_settingsPanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		settingsPanel.setLayout(gbl_settingsPanel);
		
		JPanel cameraSettingsPanel = new JPanel();
		cameraSettingsPanel.setBorder(new TitledBorder(null, "Camera Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_cameraSettingsPanel = new GridBagConstraints();
		gbc_cameraSettingsPanel.fill = GridBagConstraints.BOTH;
		gbc_cameraSettingsPanel.gridx = 0;
		gbc_cameraSettingsPanel.gridy = 0;
		settingsPanel.add(cameraSettingsPanel, gbc_cameraSettingsPanel);
		GridBagLayout gbl_cameraSettingsPanel = new GridBagLayout();
		gbl_cameraSettingsPanel.columnWidths = new int[]{0, 0, 0};
		gbl_cameraSettingsPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_cameraSettingsPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_cameraSettingsPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		cameraSettingsPanel.setLayout(gbl_cameraSettingsPanel);
		
		JLabel deviceFileLabel = new JLabel("Device file");
		GridBagConstraints gbc_deviceFileLabel = new GridBagConstraints();
		gbc_deviceFileLabel.insets = new Insets(0, 0, 5, 5);
		gbc_deviceFileLabel.anchor = GridBagConstraints.EAST;
		gbc_deviceFileLabel.gridx = 0;
		gbc_deviceFileLabel.gridy = 0;
		cameraSettingsPanel.add(deviceFileLabel, gbc_deviceFileLabel);
		
		deviceFileTextField = new JTextField();
		deviceFileTextField.setText("/dev/video0");
		deviceFileLabel.setLabelFor(deviceFileTextField);
		GridBagConstraints gbc_deviceFileTextField = new GridBagConstraints();
		gbc_deviceFileTextField.insets = new Insets(0, 0, 5, 0);
		gbc_deviceFileTextField.fill = GridBagConstraints.HORIZONTAL;
		gbc_deviceFileTextField.gridx = 1;
		gbc_deviceFileTextField.gridy = 0;
		cameraSettingsPanel.add(deviceFileTextField, gbc_deviceFileTextField);
		deviceFileTextField.setColumns(10);
		
		JLabel standardLabel = new JLabel("Standard");
		GridBagConstraints gbc_standardLabel = new GridBagConstraints();
		gbc_standardLabel.anchor = GridBagConstraints.EAST;
		gbc_standardLabel.insets = new Insets(0, 0, 5, 5);
		gbc_standardLabel.gridx = 0;
		gbc_standardLabel.gridy = 1;
		cameraSettingsPanel.add(standardLabel, gbc_standardLabel);
		
		standardComboBox = new JComboBox();
		standardComboBox.setModel(new DefaultComboBoxModel(new String[] {"NTSC", "PAL", "SECAM", "WEBCAM"}));
		standardComboBox.setSelectedIndex(1);
		standardLabel.setLabelFor(standardComboBox);
		GridBagConstraints gbc_standardComboBox = new GridBagConstraints();
		gbc_standardComboBox.insets = new Insets(0, 0, 5, 0);
		gbc_standardComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_standardComboBox.gridx = 1;
		gbc_standardComboBox.gridy = 1;
		cameraSettingsPanel.add(standardComboBox, gbc_standardComboBox);
		
		JLabel channelLabel = new JLabel("Channel");
		GridBagConstraints gbc_channelLabel = new GridBagConstraints();
		gbc_channelLabel.anchor = GridBagConstraints.EAST;
		gbc_channelLabel.insets = new Insets(0, 0, 0, 5);
		gbc_channelLabel.gridx = 0;
		gbc_channelLabel.gridy = 2;
		cameraSettingsPanel.add(channelLabel, gbc_channelLabel);
		
		channelSpinner = new JSpinner();
		channelLabel.setLabelFor(channelSpinner);
		channelSpinner.setModel(new SpinnerNumberModel(0, 0, 99, 1));
		GridBagConstraints gbc_channelSpinner = new GridBagConstraints();
		gbc_channelSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_channelSpinner.gridx = 1;
		gbc_channelSpinner.gridy = 2;
		cameraSettingsPanel.add(channelSpinner, gbc_channelSpinner);
	}
	
	
	/**
	 * Start camera vision test. 
	 */
	private void startCameraVisionTest() {
		Vision vision = new Vision();
		WorldStateObserver visionObserver = new WorldStateObserver(vision);		
		CameraVisualInputProvider input = createCameraInputProvider();
		input.setCallback(vision);
		MainWindow mainGui = new MainWindow(visionObserver, vision);
		
		input.startCapture();
		(new Thread(mainGui, "GUI")).start();
		killLauncherWindow();
	}
	
	/**
	 * Start image vision test.
	 */
	private void startImageVisionTest() {
		Vision vision = new Vision();
		WorldStateObserver visionObserver = new WorldStateObserver(vision);
		
		ImageVisualInputProvider input = createImageInputProvider();
		input.setCallback(vision);
		
		MainWindow mainGui = new MainWindow(visionObserver, vision);
		
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
	 */
	private ImageVisualInputProvider createImageInputProvider() {
		String filenames[] = {
				"../robot-VISION/data/testImages/pitch2-1.png",
				"../robot-VISION/data/testImages/pitch2-2.png",
				"../robot-VISION/data/testImages/pitch2-3.png"
		};
		return new ImageVisualInputProvider(filenames, 25);
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

}
