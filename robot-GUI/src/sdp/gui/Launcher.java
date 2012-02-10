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
import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
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
	private JSpinner testFpsSpinner;
	
	
	/**
	 * The main constructor.
	 */
	public Launcher() {
		setSize(new Dimension(198, 243));
		setTitle("Launcher");
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{150, 0};
		gridBagLayout.rowHeights = new int[]{25, 0, 0};
		gridBagLayout.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel competitionModePanel = new JPanel();
		GridBagConstraints gbc_competitionModePanel = new GridBagConstraints();
		gbc_competitionModePanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_competitionModePanel.anchor = GridBagConstraints.NORTH;
		gbc_competitionModePanel.insets = new Insets(0, 0, 5, 0);
		gbc_competitionModePanel.gridx = 0;
		gbc_competitionModePanel.gridy = 0;
		getContentPane().add(competitionModePanel, gbc_competitionModePanel);
		competitionModePanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Competition mode", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagLayout gbl_competitionModePanel = new GridBagLayout();
		gbl_competitionModePanel.columnWidths = new int[]{80, 0, 0};
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
		gbc_channelSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_channelSpinner.fill = GridBagConstraints.HORIZONTAL;
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
		gbc_testModePanel.anchor = GridBagConstraints.NORTH;
		gbc_testModePanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_testModePanel.gridx = 0;
		gbc_testModePanel.gridy = 1;
		getContentPane().add(testModePanel, gbc_testModePanel);
		GridBagLayout gbl_testModePanel = new GridBagLayout();
		gbl_testModePanel.columnWidths = new int[]{80, 0, 0};
		gbl_testModePanel.rowHeights = new int[]{0, 0, 0};
		gbl_testModePanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_testModePanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		testModePanel.setLayout(gbl_testModePanel);
		
		JLabel testFpsLabel = new JLabel("FPS");
		GridBagConstraints gbc_testFpsLabel = new GridBagConstraints();
		gbc_testFpsLabel.anchor = GridBagConstraints.EAST;
		gbc_testFpsLabel.insets = new Insets(0, 0, 5, 5);
		gbc_testFpsLabel.gridx = 0;
		gbc_testFpsLabel.gridy = 0;
		testModePanel.add(testFpsLabel, gbc_testFpsLabel);
		
		testFpsSpinner = new JSpinner();
		testFpsSpinner.setModel(new SpinnerNumberModel(25, 1, 60, 1));
		GridBagConstraints gbc_testFpsSpinner = new GridBagConstraints();
		gbc_testFpsSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_testFpsSpinner.insets = new Insets(0, 0, 5, 0);
		gbc_testFpsSpinner.gridx = 1;
		gbc_testFpsSpinner.gridy = 0;
		testModePanel.add(testFpsSpinner, gbc_testFpsSpinner);
		
		JButton startTestButton = new JButton("Start testing");
		GridBagConstraints gbc_startTestButton = new GridBagConstraints();
		gbc_startTestButton.gridwidth = 2;
		gbc_startTestButton.insets = new Insets(0, 0, 0, 5);
		gbc_startTestButton.gridx = 0;
		gbc_startTestButton.gridy = 1;
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
	
	/**
	 * Start camera vision test. 
	 */
	private void startCompetitionMode() {
		Vision vision = new Vision();
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
		Vision vision = new Vision();
		WorldStateObserver visionObserver = new WorldStateObserver(vision);
		
		ImageVisualInputProvider input = createImageInputProvider();
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
	 */
	private ImageVisualInputProvider createImageInputProvider() {
		String filenames[] = {
				"../robot-VISION/data/testImages/pitch2-1.png",
				"../robot-VISION/data/testImages/pitch2-2.png",
				"../robot-VISION/data/testImages/pitch2-3.png"
		};
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

}
