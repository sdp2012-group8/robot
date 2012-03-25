package sdp.gui;

import java.awt.image.BufferedImage;

import java.awt.Component;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;

import javax.swing.JSpinner;
import javax.swing.border.TitledBorder;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTabbedPane;

import sdp.AI.AIMaster;
import sdp.common.Communicator;
import sdp.common.Communicator.opcode;
import sdp.common.world.WorldState;
import sdp.common.FPSCounter;
import sdp.common.Utilities;
import sdp.common.WorldStateObserver;
import sdp.communicator.AIComm;
import sdp.gui.filefilters.TextFileFilter_FC;
import sdp.gui.filefilters.XmlFileFilter_FC;
import sdp.vision.Vision;
import sdp.vision.processing.ImageProcessorConfig;
import sdp.vision.testbench.TestBench;

import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import java.awt.Color;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * The GUI class of the main window.
 */
public class MainWindow extends javax.swing.JFrame implements Runnable {
	
	/** The class' logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.gui.MainWindow");
	
	/** Required by Serializable. */
	private static final long serialVersionUID = 8597348579639499324L;	
	/** Class name. */
	public static String className = null;
	
	
	/** Path to the default vision configuration file. */
	private static final String DEFAULT_CONFIG_PATH = "data/configs/Default.xml";
	
	/** In what integer range will floats be represented in spinners. */
	private static final int SPINNER_FLOAT_RANGE = 1000;	
	/** The window title. */
	private static final String WINDOW_TITLE = "Battlestation";
	
	
	/** Window's FPS counter. */
	private FPSCounter fpsCounter;
	
	/** The vision configuration file chooser. */
	private JFileChooser visionConfigFileChooser;
	/** Test bench test case file chooser. */
	private JFileChooser testBenchTestFileChooser;
	/** Test bench output file chooser. */
	private JFileChooser testBenchOutputFileChooser;
	
	/** Active AI subsystem instance. */
	private AIMaster aiInstance = null;
	/** Robot communicator instance. */
	private Communicator communicator = null;
	/** Flag that indicates whether the robot is running. */
	private boolean robotRunning = false;
	
	/** Active vision subsystem instance. */
	private Vision vision = null;	
	/** A flag that controls whether vision system calibration is enabled. */
	private boolean visionChangesEnabled = true;
	
	/** Active test bench instance. */
	private TestBench testBench;
	
	/** GUI's world state provider. */
	private WorldStateObserver worldStateObserver;
	
	/** Mouse pointer position on the canvas image. */
	private Point imageMousePos = null;
	
	
	/**
	 * Create the main GUI with the specified components.
	 * 
	 * @param worldStateProvider The object that provides world state to the 
	 * 		GUI. Cannot be null.
	 * @param vision The active instance of the vision subsystem. If null, the
	 * 		vision subsystem will be assumed to be online and the GUI will not
	 * 		let you adjust vision settings.
	 */
	public MainWindow(boolean testMode, WorldStateObserver worldStateObserver, Vision vision) {
		if (worldStateObserver == null) {
			throw new NullPointerException("Main window's state provider cannot be null.");
		} else {
			this.worldStateObserver = worldStateObserver;
		}
		
		this.vision = vision;
		fpsCounter = new FPSCounter();
		visionChangesEnabled = true;
		
		visionConfigFileChooser = new JFileChooser("data/configs");
		visionConfigFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		visionConfigFileChooser.setAcceptAllFileFilterUsed(false);
		visionConfigFileChooser.addChoosableFileFilter(new XmlFileFilter_FC());
		
		testBenchTestFileChooser = new JFileChooser("data/tests");
		testBenchTestFileChooser.setDialogTitle("Select test case");
		testBenchTestFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		testBenchTestFileChooser.setAcceptAllFileFilterUsed(false);
		testBenchTestFileChooser.addChoosableFileFilter(new XmlFileFilter_FC());
		
		testBenchOutputFileChooser = new JFileChooser("..");
		testBenchOutputFileChooser.setDialogTitle("Save test bench output");
		testBenchOutputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		testBenchOutputFileChooser.setAcceptAllFileFilterUsed(false);
		testBenchOutputFileChooser.addChoosableFileFilter(new TextFileFilter_FC());
		
		testBench = new TestBench();
		
		setSize(new Dimension(1050, 710));
		setTitle(WINDOW_TITLE);
		initComponents();
		
		if (vision != null) {
			if (new File(DEFAULT_CONFIG_PATH).exists()) {
				ImageProcessorConfig defaultConfig = ImageProcessorConfig.loadConfiguration(DEFAULT_CONFIG_PATH);
				setGUIConfiguration(defaultConfig);
				setVisionConfiguration();
			} else {
				LOGGER.warning("Could not find default vision configuration, using built-in defaults.");
				getVisionConfiguration();
			}
		} else {
			robotControlTabbedPanel.remove(visionSettingPanel);
		}
		
		if (testMode) {
			robotControlTabbedPanel.remove(robotSettingPanel);
		} else {
			robotControlTabbedPanel.remove(testBenchPanel);
		}
		
		for (int i = 0; i < AIMaster.AIState.values().length; i++) {
			robotBehaviourCombobox.addItem(AIMaster.AIState.values()[i]);
		}
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					if (communicator != null) {
						communicator.sendMessage(opcode.exit);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	
	
	/**
	 * Set the camera image to display.
	 * 
	 * @param image New image.
	 */
	public void setImage(BufferedImage image) {
		if (image != null) {
			imageCanvasPanel.getGraphics().drawImage(image, 0, 0, null);
		}
	}
	
	
	/**
	 * Connect to our robot.
	 */
	private void connectToRobot() {
		if (robotDebugModeCheckbox.isSelected()) {
			communicator = null;
		} else {
			try {
				communicator = new AIComm();
			} catch (IOException e) {
				LOGGER.warning("Connection with brick failed! Going into test mode.");
				communicator = null;
			}
		}
		
		aiInstance = new AIMaster(communicator, vision, AIMaster.AIType.VISUAL_SERVOING);
		aiInstance.start(robotColorBlueButton.isSelected(), robotGateLeftButton.isSelected());
		
		WorldStateObserver aiObserver = new WorldStateObserver(aiInstance);
		synchronized (worldStateObserver) {
			worldStateObserver = aiObserver;
		}
		
		robotRunning = true;
		robotConnectButton.setText("Disconnect");
	}
	
	/**
	 * Disconnect from our robot.
	 */
	private void disconnectFromRobot() {
		aiInstance = null;
		
		robotRunning = false;
		robotConnectButton.setText("Connect");
	}
	
	
	/**
	 * Register a mouse click on the image canvas.
	 * 
	 * The caller must update imageMousePos to the click location before calling
	 * this function.
	 */
	private void registerCanvasClick() {
		System.err.println(imageMousePos.x + " " + imageMousePos.y);
	}
	
	
	/**
	 * Load the vision system configuration, selected by user.
	 */
	private void loadConfiguration() {
		visionConfigFileChooser.setDialogTitle("Load configuration");
		int retValue = visionConfigFileChooser.showOpenDialog(this);
		
		if (retValue == JFileChooser.APPROVE_OPTION) {
			String chosenFile = visionConfigFileChooser.getSelectedFile().getAbsolutePath();
			ImageProcessorConfig config = ImageProcessorConfig.loadConfiguration(chosenFile);
			setGUIConfiguration(config);
		}
	}
	
	/**
	 * Save the current vision configuration into a file.
	 */
	private void saveConfiguration() {
		visionConfigFileChooser.setDialogTitle("Save configuration");
		int retValue = visionConfigFileChooser.showSaveDialog(this);
		
		if (retValue == JFileChooser.APPROVE_OPTION) {
			String chosenFile = visionConfigFileChooser.getSelectedFile().getAbsolutePath();
			ImageProcessorConfig config = getGUIConfiguration();
			ImageProcessorConfig.saveConfiguration(config, chosenFile);
		}
	}
	
	
	/** 
	 * Update the vision tab components to match vision's configuration.
	 */
	private void getVisionConfiguration() {
		if (vision == null) {
			LOGGER.info("Tried to read vision configuration when vision subsystem was inactive.");
		} else {
			ImageProcessorConfig config = vision.getConfiguration();
			setGUIConfiguration(config);
		}
	}
	
	/**
	 * Set the configuration of the vision subsystem to match the values in
	 * vision tab.
	 */
	private void setVisionConfiguration() {
		if (vision == null) {
			LOGGER.info("Tried to set vision configuration when vision subsystem was inactive.");
		} else {
			ImageProcessorConfig config = getGUIConfiguration();
			vision.setConfiguration(config);
			if (aiInstance != null) {
				aiInstance.setConfiguration(config);
			}
		}
	}

	
	/**
	 * Create a ImageProcessorConfig from the values in GUI components.
	 * 
	 * @return GUI's image processor configuration.
	 */
	private ImageProcessorConfig getGUIConfiguration() {
		ImageProcessorConfig config = new ImageProcessorConfig();
		
		config.setRawFieldLowX(((Integer)fieldLowXSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
		config.setRawFieldLowY(((Integer)fieldLowYSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
		config.setRawFieldHighX(((Integer)fieldHighXSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
		config.setRawFieldHighY(((Integer)fieldHighYSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
		
		config.getBallThreshs().setHueMin(((Integer)ballHueMinSpinner.getValue()).intValue());
		config.getBallThreshs().setSatMin(((Integer)ballSatMinSpinner.getValue()).intValue());
		config.getBallThreshs().setValMin(((Integer)ballValMinSpinner.getValue()).intValue());
		config.setBallSizeMin(((Integer)ballSizeMinSpinner.getValue()).intValue());
		config.getBallThreshs().setHueMax(((Integer)ballHueMaxSpinner.getValue()).intValue());
		config.getBallThreshs().setSatMax(((Integer)ballSatMaxSpinner.getValue()).intValue());
		config.getBallThreshs().setValMax(((Integer)ballValMaxSpinner.getValue()).intValue());
		config.setBallSizeMax(((Integer)ballSizeMaxSpinner.getValue()).intValue());
		
		config.setDetectBlue(enableBlueCheckbox.isSelected());
		config.getBlueThreshs().setHueMin(((Integer)blueHueMinSpinner.getValue()).intValue());
		config.getBlueThreshs().setSatMin(((Integer)blueSatMinSpinner.getValue()).intValue());
		config.getBlueThreshs().setValMin(((Integer)blueValMinSpinner.getValue()).intValue());
		config.setBlueSizeMin(((Integer)blueSizeMinSpinner.getValue()).intValue());
		config.getBlueThreshs().setHueMax(((Integer)blueHueMaxSpinner.getValue()).intValue());
		config.getBlueThreshs().setSatMax(((Integer)blueSatMaxSpinner.getValue()).intValue());
		config.getBlueThreshs().setValMax(((Integer)blueValMaxSpinner.getValue()).intValue());
		config.setBlueSizeMax(((Integer)blueSizeMaxSpinner.getValue()).intValue());
		config.setCorrectBlueHeight(correctHeightBlueCheckbox.isSelected());
		config.setBlueHeightFactor(((Integer)blueHeightFactorSpinner.getValue()).doubleValue());
		
		config.setDetectYellow(enableYellowCheckbox.isSelected());
		config.getYellowThreshs().setHueMin(((Integer)yellowHueMinSpinner.getValue()).intValue());
		config.getYellowThreshs().setSatMin(((Integer)yellowSatMinSpinner.getValue()).intValue());
		config.getYellowThreshs().setValMin(((Integer)yellowValMinSpinner.getValue()).intValue());
		config.setYellowSizeMin(((Integer)yellowSizeMinSpinner.getValue()).intValue());
		config.getYellowThreshs().setHueMax(((Integer)yellowHueMaxSpinner.getValue()).intValue());
		config.getYellowThreshs().setSatMax(((Integer)yellowSatMaxSpinner.getValue()).intValue());
		config.getYellowThreshs().setValMax(((Integer)yellowValMaxSpinner.getValue()).intValue());
		config.setYellowSizeMax(((Integer)yellowSizeMaxSpinner.getValue()).intValue());
		config.setCorrectYellowHeight(correctHeightYellowCheckbox.isSelected());
		config.setYellowHeightFactor(((Integer)yellowHeightFactorSpinner.getValue()).doubleValue());
		
		config.setUndistortFrame(enableUndistortionCheckbox.isSelected());
		try {
			config.setUndistort_cx(Double.valueOf(cxTextfield.getText()));
		} catch (NumberFormatException e) {	}
		try {
			config.setUndistort_cy(Double.valueOf(cyTextfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_fx(Double.valueOf(fxTextfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_fy(Double.valueOf(fyTextfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_k1(Double.valueOf(k1Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_k2(Double.valueOf(k2Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_p1(Double.valueOf(p1Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_p2(Double.valueOf(p2Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_k3(Double.valueOf(k3Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_k4(Double.valueOf(k4Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_k5(Double.valueOf(k5Textfield.getText()));
		} catch (NumberFormatException e) { }
		try {
			config.setUndistort_k6(Double.valueOf(k6Textfield.getText()));
		} catch (NumberFormatException e) { }
		
		config.setShowWorld(showWorldCheckbox.isSelected());
		config.setShowThresholds(showThreshCheckbox.isSelected());
		config.setShowContours(showContoursCheckbox.isSelected());
		config.setShowBoundingBoxes(showBoxesCheckbox.isSelected());
		config.setShowStateData(showStateDataCheckbox.isSelected());
		
		return config;
	}
	
	/**
	 * Update the vision tab components to match the given configuration.
	 * @param config Configuration to take values from.
	 */
	private void setGUIConfiguration(ImageProcessorConfig config) {
		fieldLowXSpinner.setValue(new Integer((int) (config.getRawFieldLowX() * SPINNER_FLOAT_RANGE)));
		fieldLowYSpinner.setValue(new Integer((int) (config.getRawFieldLowY() * SPINNER_FLOAT_RANGE)));
		fieldHighXSpinner.setValue(new Integer((int) (config.getRawFieldHighX() * SPINNER_FLOAT_RANGE)));
		fieldHighYSpinner.setValue(new Integer((int) (config.getRawFieldHighY() * SPINNER_FLOAT_RANGE)));
		
		ballHueMinSpinner.setValue(new Integer(config.getBallThreshs().getHueMin()));
		ballSatMinSpinner.setValue(new Integer(config.getBallThreshs().getSatMin()));
		ballValMinSpinner.setValue(new Integer(config.getBallThreshs().getValMin()));
		ballSizeMinSpinner.setValue(new Integer(config.getBallSizeMin()));
		ballHueMaxSpinner.setValue(new Integer(config.getBallThreshs().getHueMax()));
		ballSatMaxSpinner.setValue(new Integer(config.getBallThreshs().getSatMax()));
		ballValMaxSpinner.setValue(new Integer(config.getBallThreshs().getValMax()));
		ballSizeMaxSpinner.setValue(new Integer(config.getBallSizeMax()));
		
		enableBlueCheckbox.setSelected(config.isDetectBlue());
		blueHueMinSpinner.setValue(new Integer(config.getBlueThreshs().getHueMin()));
		blueSatMinSpinner.setValue(new Integer(config.getBlueThreshs().getSatMin()));
		blueValMinSpinner.setValue(new Integer(config.getBlueThreshs().getValMin()));
		blueSizeMinSpinner.setValue(new Integer(config.getBlueSizeMin()));
		blueHueMaxSpinner.setValue(new Integer(config.getBlueThreshs().getHueMax()));
		blueSatMaxSpinner.setValue(new Integer(config.getBlueThreshs().getSatMax()));
		blueValMaxSpinner.setValue(new Integer(config.getBlueThreshs().getValMax()));
		blueSizeMaxSpinner.setValue(new Integer(config.getBlueSizeMax()));
		correctHeightBlueCheckbox.setSelected(config.isCorrectBlueHeight());
		blueHeightFactorSpinner.setValue(new Integer((int)config.getBlueHeightFactor()));
		
		enableYellowCheckbox.setSelected(config.isDetectYellow());
		yellowHueMinSpinner.setValue(new Integer(config.getYellowThreshs().getHueMin()));
		yellowSatMinSpinner.setValue(new Integer(config.getYellowThreshs().getSatMin()));
		yellowValMinSpinner.setValue(new Integer(config.getYellowThreshs().getValMin()));
		yellowSizeMinSpinner.setValue(new Integer(config.getYellowSizeMin()));
		yellowHueMaxSpinner.setValue(new Integer(config.getYellowThreshs().getHueMax()));
		yellowSatMaxSpinner.setValue(new Integer(config.getYellowThreshs().getSatMax()));
		yellowValMaxSpinner.setValue(new Integer(config.getYellowThreshs().getValMax()));
		yellowSizeMaxSpinner.setValue(new Integer(config.getYellowSizeMax()));
		correctHeightYellowCheckbox.setSelected(config.isCorrectYellowHeight());
		yellowHeightFactorSpinner.setValue(new Integer((int)config.getYellowHeightFactor()));
		
		enableUndistortionCheckbox.setSelected(config.isUndistortFrame());
		cxTextfield.setText(Double.toString(config.getUndistort_cx()));
		cyTextfield.setText(Double.toString(config.getUndistort_cy()));
		fxTextfield.setText(Double.toString(config.getUndistort_fx()));
		fyTextfield.setText(Double.toString(config.getUndistort_fy()));
		k1Textfield.setText(Double.toString(config.getUndistort_k1()));
		k2Textfield.setText(Double.toString(config.getUndistort_k2()));
		p1Textfield.setText(Double.toString(config.getUndistort_p1()));
		p2Textfield.setText(Double.toString(config.getUndistort_p2()));
		k3Textfield.setText(Double.toString(config.getUndistort_k3()));
		k4Textfield.setText(Double.toString(config.getUndistort_k4()));
		k5Textfield.setText(Double.toString(config.getUndistort_k5()));
		k6Textfield.setText(Double.toString(config.getUndistort_k6()));
		
		showWorldCheckbox.setSelected(config.isShowWorld());
		showThreshCheckbox.setSelected(config.isShowThresholds());
		showContoursCheckbox.setSelected(config.isShowContours());
		showBoxesCheckbox.setSelected(config.isShowBoundingBoxes());
		showStateDataCheckbox.setSelected(config.isShowStateData());
	}
	
	
	/**
	 * Open a dialog to select a test bench test file and modify the appropriate
	 * field on the GUI.
	 */
	private void selectTestCase() {
		int retValue = testBenchTestFileChooser.showOpenDialog(this);
		
		if (retValue == JFileChooser.APPROVE_OPTION) {
			String chosenFile = testBenchTestFileChooser.getSelectedFile().getAbsolutePath();
			testCaseTextfield.setText(chosenFile);
		}
	}
	
	/**
	 * Run the test bench on the selected test case.
	 */
	private void runTestBench() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		
		String testCase = Utilities.stripString(testCaseTextfield.getText());
		
		vision.setEnabled(false);
		testBench.runTest(testCase, getGUIConfiguration(), ps);
		vision.setEnabled(true);
		
		testBenchOutputTextarea.setText(baos.toString());
	}
	
	/**
	 * Allow the user to save test bench output into a file.
	 */
	private void saveTestBenchOutput() {
		int retValue = testBenchOutputFileChooser.showSaveDialog(this);
		
		if (retValue == JFileChooser.APPROVE_OPTION) {
			String chosenFile = testBenchOutputFileChooser.getSelectedFile().getAbsolutePath();
			
			try {
				PrintWriter fout = new PrintWriter(chosenFile);
				fout.write(testBenchOutputTextarea.getText());
				fout.close();
			} catch (FileNotFoundException e) {
				JOptionPane.showMessageDialog(this, "Could not open file for writing, output not saved.",
						"FileNotFoundException", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		WorldState state = null;
		
		setVisible(true);
				
		while (!Thread.interrupted()) {			
			synchronized (worldStateObserver) {
				state = worldStateObserver.getNextState();
			}
			
			setImage(state.getWorldImage());
			
			fpsCounter.tick();
			setTitle(String.format("%s - %.1f FPS", WINDOW_TITLE, fpsCounter.getFPS()));
			if (visionChangesEnabled) {
				setVisionConfiguration();
			} 
			
			if (imageMousePos != null) {
				Graphics g = imageCanvasPanel.getGraphics();
				g.setColor(Color.white);
				g.drawRect(imageMousePos.x - 3, imageMousePos.y - 3, 7, 7);
			}
			
//			System.err.println(String.format(
//					"NEXT STATE: Ball at (%.4f, %.4f), Blue at (%.4f, %.4f, %.4f), Yellow at (%.4f, %.4f, %.4f).",
//					state.getBallCoords().x, state.getBallCoords().y,
//					state.getBlueRobot().getCoords().x, state.getBlueRobot().getCoords().y, 
//					state.getBlueRobot().getAngle(), state.getYellowRobot().getCoords().x, 
//					state.getYellowRobot().getCoords().y, state.getYellowRobot().getAngle()));
		}
	}
	
	
	/**
	 * Initialise GUI components.
	 */
	private void initComponents() {
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setName("Vision");
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{640, 200};
		gridBagLayout.rowHeights = new int[]{480, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 4.9E-324};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel cameraImagePanel = new JPanel();
		GridBagConstraints gbc_cameraImagePanel = new GridBagConstraints();
		gbc_cameraImagePanel.fill = GridBagConstraints.BOTH;
		gbc_cameraImagePanel.insets = new Insets(0, 0, 0, 5);
		gbc_cameraImagePanel.gridx = 0;
		gbc_cameraImagePanel.gridy = 0;
		getContentPane().add(cameraImagePanel, gbc_cameraImagePanel);
		GridBagLayout gbl_cameraImagePanel = new GridBagLayout();
		gbl_cameraImagePanel.columnWidths = new int[]{640, 0};
		gbl_cameraImagePanel.rowHeights = new int[]{0, 480, 0, 0};
		gbl_cameraImagePanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_cameraImagePanel.rowWeights = new double[]{1.0, 0.0, 1.0, Double.MIN_VALUE};
		cameraImagePanel.setLayout(gbl_cameraImagePanel);
		
		imageCanvasPanel = new JPanel();
		imageCanvasPanel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				imageMousePos = e.getPoint();
			}
		});
		imageCanvasPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				imageMousePos = e.getPoint();
			}
			@Override
			public void mouseExited(MouseEvent e) {
				imageMousePos = null;
			}
			@Override
			public void mouseClicked(MouseEvent e) {
				imageMousePos = e.getPoint();
				registerCanvasClick();
			}
		});
		GridBagConstraints gbc_imageCanvasPanel = new GridBagConstraints();
		gbc_imageCanvasPanel.insets = new Insets(0, 0, 5, 0);
		gbc_imageCanvasPanel.fill = GridBagConstraints.BOTH;
		gbc_imageCanvasPanel.gridx = 0;
		gbc_imageCanvasPanel.gridy = 1;
		cameraImagePanel.add(imageCanvasPanel, gbc_imageCanvasPanel);
		GridBagLayout gbl_imageCanvasPanel = new GridBagLayout();
		gbl_imageCanvasPanel.columnWidths = new int[]{0};
		gbl_imageCanvasPanel.rowHeights = new int[]{0};
		gbl_imageCanvasPanel.columnWeights = new double[]{Double.MIN_VALUE};
		gbl_imageCanvasPanel.rowWeights = new double[]{Double.MIN_VALUE};
		imageCanvasPanel.setLayout(gbl_imageCanvasPanel);
		
		robotControlTabbedPanel = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_robotControlTabbedPanel = new GridBagConstraints();
		gbc_robotControlTabbedPanel.fill = GridBagConstraints.BOTH;
		gbc_robotControlTabbedPanel.gridx = 1;
		gbc_robotControlTabbedPanel.gridy = 0;
		getContentPane().add(robotControlTabbedPanel, gbc_robotControlTabbedPanel);
		
		visionSettingPanel = new JPanel();
		robotControlTabbedPanel.addTab("Vision", null, visionSettingPanel, null);
		robotControlTabbedPanel.setEnabledAt(0, true);
		visionSettingPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		GridBagLayout gbl_visionSettingPanel = new GridBagLayout();
		gbl_visionSettingPanel.columnWidths = new int[]{200, 200, 0, 0};
		gbl_visionSettingPanel.rowHeights = new int[]{15, 0, 0, 0, 0, 0};
		gbl_visionSettingPanel.columnWeights = new double[]{1.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_visionSettingPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		visionSettingPanel.setLayout(gbl_visionSettingPanel);
		
		generalSettingPanel = new JPanel();
		generalSettingPanel.setBorder(new TitledBorder(null, "General settings", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_generalSettingPanel = new GridBagConstraints();
		gbc_generalSettingPanel.gridheight = 2;
		gbc_generalSettingPanel.insets = new Insets(0, 0, 5, 5);
		gbc_generalSettingPanel.fill = GridBagConstraints.BOTH;
		gbc_generalSettingPanel.gridx = 0;
		gbc_generalSettingPanel.gridy = 0;
		visionSettingPanel.add(generalSettingPanel, gbc_generalSettingPanel);
		GridBagLayout gbl_generalSettingPanel = new GridBagLayout();
		gbl_generalSettingPanel.columnWidths = new int[]{0, 0, 0};
		gbl_generalSettingPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_generalSettingPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_generalSettingPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		generalSettingPanel.setLayout(gbl_generalSettingPanel);
		
		loadConfigButton = new JButton("Load");
		loadConfigButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				loadConfiguration();
			}
		});
		GridBagConstraints gbc_loadConfigButton = new GridBagConstraints();
		gbc_loadConfigButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_loadConfigButton.insets = new Insets(0, 0, 5, 5);
		gbc_loadConfigButton.gridx = 0;
		gbc_loadConfigButton.gridy = 0;
		generalSettingPanel.add(loadConfigButton, gbc_loadConfigButton);
		
		saveConfigButton = new JButton("Save");
		saveConfigButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveConfiguration();
			}
		});
		GridBagConstraints gbc_saveConfigButton = new GridBagConstraints();
		gbc_saveConfigButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_saveConfigButton.insets = new Insets(0, 0, 5, 0);
		gbc_saveConfigButton.gridx = 1;
		gbc_saveConfigButton.gridy = 0;
		generalSettingPanel.add(saveConfigButton, gbc_saveConfigButton);
		
		showWorldCheckbox = new JCheckBox("Show world");
		GridBagConstraints gbc_showWorldCheckbox = new GridBagConstraints();
		gbc_showWorldCheckbox.anchor = GridBagConstraints.WEST;
		gbc_showWorldCheckbox.gridwidth = 2;
		gbc_showWorldCheckbox.insets = new Insets(0, 0, 5, 0);
		gbc_showWorldCheckbox.gridx = 0;
		gbc_showWorldCheckbox.gridy = 1;
		generalSettingPanel.add(showWorldCheckbox, gbc_showWorldCheckbox);
		
		showThreshCheckbox = new JCheckBox("Show thresholds");
		GridBagConstraints gbc_showThreshCheckbox = new GridBagConstraints();
		gbc_showThreshCheckbox.gridwidth = 2;
		gbc_showThreshCheckbox.anchor = GridBagConstraints.WEST;
		gbc_showThreshCheckbox.insets = new Insets(0, 0, 5, 0);
		gbc_showThreshCheckbox.gridx = 0;
		gbc_showThreshCheckbox.gridy = 2;
		generalSettingPanel.add(showThreshCheckbox, gbc_showThreshCheckbox);
		
		showContoursCheckbox = new JCheckBox("Show contours");
		GridBagConstraints gbc_showContoursCheckbox = new GridBagConstraints();
		gbc_showContoursCheckbox.gridwidth = 2;
		gbc_showContoursCheckbox.anchor = GridBagConstraints.WEST;
		gbc_showContoursCheckbox.insets = new Insets(0, 0, 5, 0);
		gbc_showContoursCheckbox.gridx = 0;
		gbc_showContoursCheckbox.gridy = 3;
		generalSettingPanel.add(showContoursCheckbox, gbc_showContoursCheckbox);
		
		showBoxesCheckbox = new JCheckBox("Show boxes");
		GridBagConstraints gbc_showBoxesCheckbox = new GridBagConstraints();
		gbc_showBoxesCheckbox.gridwidth = 2;
		gbc_showBoxesCheckbox.insets = new Insets(0, 0, 5, 0);
		gbc_showBoxesCheckbox.anchor = GridBagConstraints.WEST;
		gbc_showBoxesCheckbox.gridx = 0;
		gbc_showBoxesCheckbox.gridy = 4;
		generalSettingPanel.add(showBoxesCheckbox, gbc_showBoxesCheckbox);
		
		showStateDataCheckbox = new JCheckBox("Show state data");
		GridBagConstraints gbc_showStateDataCheckbox = new GridBagConstraints();
		gbc_showStateDataCheckbox.gridwidth = 2;
		gbc_showStateDataCheckbox.anchor = GridBagConstraints.WEST;
		gbc_showStateDataCheckbox.gridx = 0;
		gbc_showStateDataCheckbox.gridy = 5;
		generalSettingPanel.add(showStateDataCheckbox, gbc_showStateDataCheckbox);
		
		ballThreshPanel = new JPanel();
		ballThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Ball settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_ballThreshPanel = new GridBagConstraints();
		gbc_ballThreshPanel.insets = new Insets(0, 0, 5, 5);
		gbc_ballThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_ballThreshPanel.gridx = 1;
		gbc_ballThreshPanel.gridy = 1;
		visionSettingPanel.add(ballThreshPanel, gbc_ballThreshPanel);
		GridBagLayout gbl_ballThreshPanel = new GridBagLayout();
		gbl_ballThreshPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_ballThreshPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_ballThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_ballThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 1.0, 1.0, Double.MIN_VALUE};
		ballThreshPanel.setLayout(gbl_ballThreshPanel);
		
		ballHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_ballHueLabel = new GridBagConstraints();
		gbc_ballHueLabel.anchor = GridBagConstraints.EAST;
		gbc_ballHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueLabel.gridx = 1;
		gbc_ballHueLabel.gridy = 0;
		ballThreshPanel.add(ballHueLabel, gbc_ballHueLabel);
		
		ballHueMinSpinner = new JSpinner();
		ballHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_ballHueMinSpinner = new GridBagConstraints();
		gbc_ballHueMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueMinSpinner.gridx = 2;
		gbc_ballHueMinSpinner.gridy = 0;
		ballThreshPanel.add(ballHueMinSpinner, gbc_ballHueMinSpinner);
		
		ballHueMaxSpinner = new JSpinner();
		ballHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_ballHueMaxSpinner = new GridBagConstraints();
		gbc_ballHueMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueMaxSpinner.gridx = 3;
		gbc_ballHueMaxSpinner.gridy = 0;
		ballThreshPanel.add(ballHueMaxSpinner, gbc_ballHueMaxSpinner);
		
		ballSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_ballSatLabel = new GridBagConstraints();
		gbc_ballSatLabel.anchor = GridBagConstraints.EAST;
		gbc_ballSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatLabel.gridx = 1;
		gbc_ballSatLabel.gridy = 1;
		ballThreshPanel.add(ballSatLabel, gbc_ballSatLabel);
		
		ballSatMinSpinner = new JSpinner();
		ballSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballSatMinSpinner = new GridBagConstraints();
		gbc_ballSatMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatMinSpinner.gridx = 2;
		gbc_ballSatMinSpinner.gridy = 1;
		ballThreshPanel.add(ballSatMinSpinner, gbc_ballSatMinSpinner);
		
		ballSatMaxSpinner = new JSpinner();
		ballSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballSatMaxSpinner = new GridBagConstraints();
		gbc_ballSatMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatMaxSpinner.gridx = 3;
		gbc_ballSatMaxSpinner.gridy = 1;
		ballThreshPanel.add(ballSatMaxSpinner, gbc_ballSatMaxSpinner);
		
		ballValLabel = new JLabel("VAL");
		GridBagConstraints gbc_ballValLabel = new GridBagConstraints();
		gbc_ballValLabel.anchor = GridBagConstraints.EAST;
		gbc_ballValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballValLabel.gridx = 1;
		gbc_ballValLabel.gridy = 2;
		ballThreshPanel.add(ballValLabel, gbc_ballValLabel);
		
		ballValMinSpinner = new JSpinner();
		ballValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballValMinSpinner = new GridBagConstraints();
		gbc_ballValMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballValMinSpinner.gridx = 2;
		gbc_ballValMinSpinner.gridy = 2;
		ballThreshPanel.add(ballValMinSpinner, gbc_ballValMinSpinner);
		
		ballValMaxSpinner = new JSpinner();
		ballValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballValMaxSpinner = new GridBagConstraints();
		gbc_ballValMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballValMaxSpinner.gridx = 3;
		gbc_ballValMaxSpinner.gridy = 2;
		ballThreshPanel.add(ballValMaxSpinner, gbc_ballValMaxSpinner);
		
		ballSizeLabel = new JLabel("SIZE");
		GridBagConstraints gbc_ballSizeLabel = new GridBagConstraints();
		gbc_ballSizeLabel.anchor = GridBagConstraints.EAST;
		gbc_ballSizeLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballSizeLabel.gridx = 1;
		gbc_ballSizeLabel.gridy = 3;
		ballThreshPanel.add(ballSizeLabel, gbc_ballSizeLabel);
		
		ballSizeMinSpinner = new JSpinner();
		ballSizeMinSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_ballSizeMinSpinner = new GridBagConstraints();
		gbc_ballSizeMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSizeMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSizeMinSpinner.gridx = 2;
		gbc_ballSizeMinSpinner.gridy = 3;
		ballThreshPanel.add(ballSizeMinSpinner, gbc_ballSizeMinSpinner);
		
		ballSizeMaxSpinner = new JSpinner();
		ballSizeMaxSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_ballSizeMaxSpinner = new GridBagConstraints();
		gbc_ballSizeMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSizeMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSizeMaxSpinner.gridx = 3;
		gbc_ballSizeMaxSpinner.gridy = 3;
		ballThreshPanel.add(ballSizeMaxSpinner, gbc_ballSizeMaxSpinner);
		
		undistortionPanel = new JPanel();
		undistortionPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Undistortion", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_undistortionPanel = new GridBagConstraints();
		gbc_undistortionPanel.gridheight = 2;
		gbc_undistortionPanel.insets = new Insets(0, 0, 5, 5);
		gbc_undistortionPanel.fill = GridBagConstraints.BOTH;
		gbc_undistortionPanel.gridx = 0;
		gbc_undistortionPanel.gridy = 2;
		visionSettingPanel.add(undistortionPanel, gbc_undistortionPanel);
		GridBagLayout gbl_undistortionPanel = new GridBagLayout();
		gbl_undistortionPanel.columnWidths = new int[]{0, 0, 100, 0, 0};
		gbl_undistortionPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_undistortionPanel.columnWeights = new double[]{1.0, 0.0, 1.0, 1.0, Double.MIN_VALUE};
		gbl_undistortionPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		undistortionPanel.setLayout(gbl_undistortionPanel);
		
		enableUndistortionCheckbox = new JCheckBox("Enable undistortion");
		GridBagConstraints gbc_enableUndistortionCheckbox = new GridBagConstraints();
		gbc_enableUndistortionCheckbox.anchor = GridBagConstraints.WEST;
		gbc_enableUndistortionCheckbox.gridwidth = 2;
		gbc_enableUndistortionCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_enableUndistortionCheckbox.gridx = 1;
		gbc_enableUndistortionCheckbox.gridy = 0;
		undistortionPanel.add(enableUndistortionCheckbox, gbc_enableUndistortionCheckbox);
		
		intristicLabel = new JLabel("Intristic Coefficients");
		GridBagConstraints gbc_intristicLabel = new GridBagConstraints();
		gbc_intristicLabel.gridwidth = 2;
		gbc_intristicLabel.insets = new Insets(0, 0, 5, 5);
		gbc_intristicLabel.gridx = 1;
		gbc_intristicLabel.gridy = 1;
		undistortionPanel.add(intristicLabel, gbc_intristicLabel);
		
		fxLabel = new JLabel("f_x");
		fxLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_fxLabel = new GridBagConstraints();
		gbc_fxLabel.anchor = GridBagConstraints.EAST;
		gbc_fxLabel.insets = new Insets(0, 0, 5, 5);
		gbc_fxLabel.gridx = 1;
		gbc_fxLabel.gridy = 2;
		undistortionPanel.add(fxLabel, gbc_fxLabel);
		
		fxTextfield = new JTextField();
		GridBagConstraints gbc_fxTextfield = new GridBagConstraints();
		gbc_fxTextfield.insets = new Insets(0, 0, 5, 5);
		gbc_fxTextfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_fxTextfield.gridx = 2;
		gbc_fxTextfield.gridy = 2;
		undistortionPanel.add(fxTextfield, gbc_fxTextfield);
		fxTextfield.setColumns(10);
		
		fyLabel = new JLabel("f_y");
		fyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_fyLabel = new GridBagConstraints();
		gbc_fyLabel.anchor = GridBagConstraints.EAST;
		gbc_fyLabel.insets = new Insets(0, 0, 5, 5);
		gbc_fyLabel.gridx = 1;
		gbc_fyLabel.gridy = 3;
		undistortionPanel.add(fyLabel, gbc_fyLabel);
		
		fyTextfield = new JTextField();
		GridBagConstraints gbc_fyTextfield = new GridBagConstraints();
		gbc_fyTextfield.insets = new Insets(0, 0, 5, 5);
		gbc_fyTextfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_fyTextfield.gridx = 2;
		gbc_fyTextfield.gridy = 3;
		undistortionPanel.add(fyTextfield, gbc_fyTextfield);
		fyTextfield.setColumns(10);
		
		cxLabel = new JLabel("c_x");
		cxLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_cxLabel = new GridBagConstraints();
		gbc_cxLabel.anchor = GridBagConstraints.EAST;
		gbc_cxLabel.insets = new Insets(0, 0, 5, 5);
		gbc_cxLabel.gridx = 1;
		gbc_cxLabel.gridy = 4;
		undistortionPanel.add(cxLabel, gbc_cxLabel);
		
		cxTextfield = new JTextField();
		GridBagConstraints gbc_cxTextfield = new GridBagConstraints();
		gbc_cxTextfield.insets = new Insets(0, 0, 5, 5);
		gbc_cxTextfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_cxTextfield.gridx = 2;
		gbc_cxTextfield.gridy = 4;
		undistortionPanel.add(cxTextfield, gbc_cxTextfield);
		cxTextfield.setColumns(10);
		
		cyLabel = new JLabel("c_y");
		cyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_cyLabel = new GridBagConstraints();
		gbc_cyLabel.anchor = GridBagConstraints.EAST;
		gbc_cyLabel.insets = new Insets(0, 0, 5, 5);
		gbc_cyLabel.gridx = 1;
		gbc_cyLabel.gridy = 5;
		undistortionPanel.add(cyLabel, gbc_cyLabel);
		
		cyTextfield = new JTextField();
		GridBagConstraints gbc_cyTextfield = new GridBagConstraints();
		gbc_cyTextfield.insets = new Insets(0, 0, 5, 5);
		gbc_cyTextfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_cyTextfield.gridx = 2;
		gbc_cyTextfield.gridy = 5;
		undistortionPanel.add(cyTextfield, gbc_cyTextfield);
		cyTextfield.setColumns(10);
		
		distortionLabel = new JLabel("Distortion Coefficients");
		distortionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_distortionLabel = new GridBagConstraints();
		gbc_distortionLabel.gridwidth = 2;
		gbc_distortionLabel.insets = new Insets(0, 0, 5, 5);
		gbc_distortionLabel.gridx = 1;
		gbc_distortionLabel.gridy = 6;
		undistortionPanel.add(distortionLabel, gbc_distortionLabel);
		
		k1Label = new JLabel("k_1");
		k1Label.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_k1Label = new GridBagConstraints();
		gbc_k1Label.anchor = GridBagConstraints.EAST;
		gbc_k1Label.insets = new Insets(0, 0, 5, 5);
		gbc_k1Label.gridx = 1;
		gbc_k1Label.gridy = 7;
		undistortionPanel.add(k1Label, gbc_k1Label);
		
		k1Textfield = new JTextField();
		GridBagConstraints gbc_k1Textfield = new GridBagConstraints();
		gbc_k1Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_k1Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_k1Textfield.gridx = 2;
		gbc_k1Textfield.gridy = 7;
		undistortionPanel.add(k1Textfield, gbc_k1Textfield);
		k1Textfield.setColumns(10);
		
		k2Label = new JLabel("k_2");
		k2Label.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_k2Label = new GridBagConstraints();
		gbc_k2Label.anchor = GridBagConstraints.EAST;
		gbc_k2Label.insets = new Insets(0, 0, 5, 5);
		gbc_k2Label.gridx = 1;
		gbc_k2Label.gridy = 8;
		undistortionPanel.add(k2Label, gbc_k2Label);
		
		k2Textfield = new JTextField();
		GridBagConstraints gbc_k2Textfield = new GridBagConstraints();
		gbc_k2Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_k2Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_k2Textfield.gridx = 2;
		gbc_k2Textfield.gridy = 8;
		undistortionPanel.add(k2Textfield, gbc_k2Textfield);
		k2Textfield.setColumns(10);
		
		p1Label = new JLabel("p_1");
		p1Label.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_p1Label = new GridBagConstraints();
		gbc_p1Label.anchor = GridBagConstraints.EAST;
		gbc_p1Label.insets = new Insets(0, 0, 5, 5);
		gbc_p1Label.gridx = 1;
		gbc_p1Label.gridy = 9;
		undistortionPanel.add(p1Label, gbc_p1Label);
		
		p1Textfield = new JTextField();
		GridBagConstraints gbc_p1Textfield = new GridBagConstraints();
		gbc_p1Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_p1Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_p1Textfield.gridx = 2;
		gbc_p1Textfield.gridy = 9;
		undistortionPanel.add(p1Textfield, gbc_p1Textfield);
		p1Textfield.setColumns(10);
		
		p2Label = new JLabel("p_2");
		p2Label.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_p2Label = new GridBagConstraints();
		gbc_p2Label.anchor = GridBagConstraints.EAST;
		gbc_p2Label.insets = new Insets(0, 0, 5, 5);
		gbc_p2Label.gridx = 1;
		gbc_p2Label.gridy = 10;
		undistortionPanel.add(p2Label, gbc_p2Label);
		
		p2Textfield = new JTextField();
		GridBagConstraints gbc_p2Textfield = new GridBagConstraints();
		gbc_p2Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_p2Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_p2Textfield.gridx = 2;
		gbc_p2Textfield.gridy = 10;
		undistortionPanel.add(p2Textfield, gbc_p2Textfield);
		p2Textfield.setColumns(10);
		
		k3Label = new JLabel("k_3");
		GridBagConstraints gbc_k3Label = new GridBagConstraints();
		gbc_k3Label.insets = new Insets(0, 0, 5, 5);
		gbc_k3Label.anchor = GridBagConstraints.EAST;
		gbc_k3Label.gridx = 1;
		gbc_k3Label.gridy = 11;
		undistortionPanel.add(k3Label, gbc_k3Label);
		
		k3Textfield = new JTextField();
		GridBagConstraints gbc_k3Textfield = new GridBagConstraints();
		gbc_k3Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_k3Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_k3Textfield.gridx = 2;
		gbc_k3Textfield.gridy = 11;
		undistortionPanel.add(k3Textfield, gbc_k3Textfield);
		k3Textfield.setColumns(10);
		
		k4Label = new JLabel("k_4");
		GridBagConstraints gbc_k4Label = new GridBagConstraints();
		gbc_k4Label.insets = new Insets(0, 0, 5, 5);
		gbc_k4Label.anchor = GridBagConstraints.EAST;
		gbc_k4Label.gridx = 1;
		gbc_k4Label.gridy = 12;
		undistortionPanel.add(k4Label, gbc_k4Label);
		
		k4Textfield = new JTextField();
		GridBagConstraints gbc_k4Textfield = new GridBagConstraints();
		gbc_k4Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_k4Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_k4Textfield.gridx = 2;
		gbc_k4Textfield.gridy = 12;
		undistortionPanel.add(k4Textfield, gbc_k4Textfield);
		k4Textfield.setColumns(10);
		
		k5Label = new JLabel("k_5");
		GridBagConstraints gbc_k5Label = new GridBagConstraints();
		gbc_k5Label.insets = new Insets(0, 0, 5, 5);
		gbc_k5Label.anchor = GridBagConstraints.EAST;
		gbc_k5Label.gridx = 1;
		gbc_k5Label.gridy = 13;
		undistortionPanel.add(k5Label, gbc_k5Label);
		
		k5Textfield = new JTextField();
		GridBagConstraints gbc_k5Textfield = new GridBagConstraints();
		gbc_k5Textfield.insets = new Insets(0, 0, 5, 5);
		gbc_k5Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_k5Textfield.gridx = 2;
		gbc_k5Textfield.gridy = 13;
		undistortionPanel.add(k5Textfield, gbc_k5Textfield);
		k5Textfield.setColumns(10);
		
		k6Label = new JLabel("k_6");
		GridBagConstraints gbc_k6Label = new GridBagConstraints();
		gbc_k6Label.insets = new Insets(0, 0, 0, 5);
		gbc_k6Label.anchor = GridBagConstraints.EAST;
		gbc_k6Label.gridx = 1;
		gbc_k6Label.gridy = 14;
		undistortionPanel.add(k6Label, gbc_k6Label);
		
		k6Textfield = new JTextField();
		GridBagConstraints gbc_k6Textfield = new GridBagConstraints();
		gbc_k6Textfield.insets = new Insets(0, 0, 0, 5);
		gbc_k6Textfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_k6Textfield.gridx = 2;
		gbc_k6Textfield.gridy = 14;
		undistortionPanel.add(k6Textfield, gbc_k6Textfield);
		k6Textfield.setColumns(10);
		
		blueThreshPanel = new JPanel();
		blueThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Blue T settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_blueThreshPanel = new GridBagConstraints();
		gbc_blueThreshPanel.insets = new Insets(0, 0, 5, 5);
		gbc_blueThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_blueThreshPanel.gridx = 1;
		gbc_blueThreshPanel.gridy = 2;
		visionSettingPanel.add(blueThreshPanel, gbc_blueThreshPanel);
		GridBagLayout gbl_blueThreshPanel = new GridBagLayout();
		gbl_blueThreshPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_blueThreshPanel.rowHeights = new int[]{0, 17, 20, 20, 0, 0, 0, 0};
		gbl_blueThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_blueThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		blueThreshPanel.setLayout(gbl_blueThreshPanel);
		
		enableBlueCheckbox = new JCheckBox("Enable detection");
		GridBagConstraints gbc_enableBlueCheckbox = new GridBagConstraints();
		gbc_enableBlueCheckbox.anchor = GridBagConstraints.WEST;
		gbc_enableBlueCheckbox.gridwidth = 3;
		gbc_enableBlueCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_enableBlueCheckbox.gridx = 1;
		gbc_enableBlueCheckbox.gridy = 0;
		blueThreshPanel.add(enableBlueCheckbox, gbc_enableBlueCheckbox);
		
		blueHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_blueHueLabel = new GridBagConstraints();
		gbc_blueHueLabel.anchor = GridBagConstraints.EAST;
		gbc_blueHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueLabel.gridx = 1;
		gbc_blueHueLabel.gridy = 1;
		blueThreshPanel.add(blueHueLabel, gbc_blueHueLabel);
		
		blueHueMinSpinner = new JSpinner();
		blueHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_blueHueMinSpinner = new GridBagConstraints();
		gbc_blueHueMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueMinSpinner.gridx = 2;
		gbc_blueHueMinSpinner.gridy = 1;
		blueThreshPanel.add(blueHueMinSpinner, gbc_blueHueMinSpinner);
		
		blueHueMaxSpinner = new JSpinner();
		blueHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_blueHueMaxSpinner = new GridBagConstraints();
		gbc_blueHueMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueMaxSpinner.gridx = 3;
		gbc_blueHueMaxSpinner.gridy = 1;
		blueThreshPanel.add(blueHueMaxSpinner, gbc_blueHueMaxSpinner);
		
		blueSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_blueSatLabel = new GridBagConstraints();
		gbc_blueSatLabel.anchor = GridBagConstraints.EAST;
		gbc_blueSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatLabel.gridx = 1;
		gbc_blueSatLabel.gridy = 2;
		blueThreshPanel.add(blueSatLabel, gbc_blueSatLabel);
		
		blueSatMinSpinner = new JSpinner();
		blueSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueSatMinSpinner = new GridBagConstraints();
		gbc_blueSatMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatMinSpinner.gridx = 2;
		gbc_blueSatMinSpinner.gridy = 2;
		blueThreshPanel.add(blueSatMinSpinner, gbc_blueSatMinSpinner);
		
		blueSatMaxSpinner = new JSpinner();
		blueSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueSatMaxSpinner = new GridBagConstraints();
		gbc_blueSatMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatMaxSpinner.gridx = 3;
		gbc_blueSatMaxSpinner.gridy = 2;
		blueThreshPanel.add(blueSatMaxSpinner, gbc_blueSatMaxSpinner);
		
		blueValLabel = new JLabel("VAL");
		GridBagConstraints gbc_blueValLabel = new GridBagConstraints();
		gbc_blueValLabel.anchor = GridBagConstraints.EAST;
		gbc_blueValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueValLabel.gridx = 1;
		gbc_blueValLabel.gridy = 3;
		blueThreshPanel.add(blueValLabel, gbc_blueValLabel);
		
		blueValMinSpinner = new JSpinner();
		blueValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueValMinSpinner = new GridBagConstraints();
		gbc_blueValMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueValMinSpinner.gridx = 2;
		gbc_blueValMinSpinner.gridy = 3;
		blueThreshPanel.add(blueValMinSpinner, gbc_blueValMinSpinner);
		
		blueValMaxSpinner = new JSpinner();
		blueValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueValMaxSpinner = new GridBagConstraints();
		gbc_blueValMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueValMaxSpinner.gridx = 3;
		gbc_blueValMaxSpinner.gridy = 3;
		blueThreshPanel.add(blueValMaxSpinner, gbc_blueValMaxSpinner);
		
		blueSizeLabel = new JLabel("SIZE");
		GridBagConstraints gbc_blueSizeLabel = new GridBagConstraints();
		gbc_blueSizeLabel.anchor = GridBagConstraints.EAST;
		gbc_blueSizeLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueSizeLabel.gridx = 1;
		gbc_blueSizeLabel.gridy = 4;
		blueThreshPanel.add(blueSizeLabel, gbc_blueSizeLabel);
		
		blueSizeMinSpinner = new JSpinner();
		blueSizeMinSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_blueSizeMinSpinner = new GridBagConstraints();
		gbc_blueSizeMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSizeMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSizeMinSpinner.gridx = 2;
		gbc_blueSizeMinSpinner.gridy = 4;
		blueThreshPanel.add(blueSizeMinSpinner, gbc_blueSizeMinSpinner);
		
		blueSizeMaxSpinner = new JSpinner();
		blueSizeMaxSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_blueSizeMaxSpinner = new GridBagConstraints();
		gbc_blueSizeMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSizeMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSizeMaxSpinner.gridx = 3;
		gbc_blueSizeMaxSpinner.gridy = 4;
		blueThreshPanel.add(blueSizeMaxSpinner, gbc_blueSizeMaxSpinner);
		
		correctHeightBlueCheckbox = new JCheckBox("Correct height");
		GridBagConstraints gbc_correctHeightBlueCheckbox = new GridBagConstraints();
		gbc_correctHeightBlueCheckbox.anchor = GridBagConstraints.WEST;
		gbc_correctHeightBlueCheckbox.gridwidth = 3;
		gbc_correctHeightBlueCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_correctHeightBlueCheckbox.gridx = 1;
		gbc_correctHeightBlueCheckbox.gridy = 5;
		blueThreshPanel.add(correctHeightBlueCheckbox, gbc_correctHeightBlueCheckbox);
		
		blueHeightFactorLabel = new JLabel("Height factor");
		GridBagConstraints gbc_blueHeightFactorLabel = new GridBagConstraints();
		gbc_blueHeightFactorLabel.anchor = GridBagConstraints.EAST;
		gbc_blueHeightFactorLabel.gridwidth = 2;
		gbc_blueHeightFactorLabel.insets = new Insets(0, 0, 0, 5);
		gbc_blueHeightFactorLabel.gridx = 1;
		gbc_blueHeightFactorLabel.gridy = 6;
		blueThreshPanel.add(blueHeightFactorLabel, gbc_blueHeightFactorLabel);
		
		blueHeightFactorSpinner = new JSpinner();
		blueHeightFactorSpinner.setModel(new SpinnerNumberModel(1, 1, 999, 1));
		GridBagConstraints gbc_blueHeightFactorSpinner = new GridBagConstraints();
		gbc_blueHeightFactorSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueHeightFactorSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_blueHeightFactorSpinner.gridx = 3;
		gbc_blueHeightFactorSpinner.gridy = 6;
		blueThreshPanel.add(blueHeightFactorSpinner, gbc_blueHeightFactorSpinner);
		
		JPanel fieldWallPanel = new JPanel();
		fieldWallPanel.setBorder(new TitledBorder(null, "Field borders", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_fieldWallPanel = new GridBagConstraints();
		gbc_fieldWallPanel.insets = new Insets(0, 0, 5, 5);
		gbc_fieldWallPanel.fill = GridBagConstraints.BOTH;
		gbc_fieldWallPanel.gridx = 1;
		gbc_fieldWallPanel.gridy = 0;
		visionSettingPanel.add(fieldWallPanel, gbc_fieldWallPanel);
		GridBagLayout gbl_fieldWallPanel = new GridBagLayout();
		gbl_fieldWallPanel.columnWidths = new int[]{0, 50, 60, 50, 0, 0};
		gbl_fieldWallPanel.rowHeights = new int[]{0, 20, 20, 0, 0, 0};
		gbl_fieldWallPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_fieldWallPanel.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		fieldWallPanel.setLayout(gbl_fieldWallPanel);
		
		fieldLowYSpinner = new JSpinner();
		fieldLowYSpinner.setMinimumSize(new Dimension(55, 20));
		fieldLowYSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		GridBagConstraints gbc_fieldLowYSpinner = new GridBagConstraints();
		gbc_fieldLowYSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldLowYSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldLowYSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_fieldLowYSpinner.gridx = 2;
		gbc_fieldLowYSpinner.gridy = 1;
		fieldWallPanel.add(fieldLowYSpinner, gbc_fieldLowYSpinner);
		
		fieldLowXSpinner = new JSpinner();
		fieldLowXSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		fieldLowXSpinner.setMinimumSize(new Dimension(55, 20));
		GridBagConstraints gbc_fieldLowXSpinner = new GridBagConstraints();
		gbc_fieldLowXSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldLowXSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldLowXSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_fieldLowXSpinner.gridx = 1;
		gbc_fieldLowXSpinner.gridy = 2;
		fieldWallPanel.add(fieldLowXSpinner, gbc_fieldLowXSpinner);
		
		fieldHighXSpinner = new JSpinner();
		fieldHighXSpinner.setMinimumSize(new Dimension(55, 20));
		fieldHighXSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		GridBagConstraints gbc_fieldHighXSpinner = new GridBagConstraints();
		gbc_fieldHighXSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldHighXSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldHighXSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_fieldHighXSpinner.gridx = 3;
		gbc_fieldHighXSpinner.gridy = 2;
		fieldWallPanel.add(fieldHighXSpinner, gbc_fieldHighXSpinner);
		
		fieldHighYSpinner = new JSpinner();
		fieldHighYSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		fieldHighYSpinner.setMinimumSize(new Dimension(55, 20));
		GridBagConstraints gbc_fieldHighYSpinner = new GridBagConstraints();
		gbc_fieldHighYSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldHighYSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_fieldHighYSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldHighYSpinner.gridx = 2;
		gbc_fieldHighYSpinner.gridy = 3;
		fieldWallPanel.add(fieldHighYSpinner, gbc_fieldHighYSpinner);
		
		yellowThreshPanel = new JPanel();
		yellowThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Yellow T settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_yellowThreshPanel = new GridBagConstraints();
		gbc_yellowThreshPanel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_yellowThreshPanel.gridx = 1;
		gbc_yellowThreshPanel.gridy = 3;
		visionSettingPanel.add(yellowThreshPanel, gbc_yellowThreshPanel);
		GridBagLayout gbl_yellowThreshPanel = new GridBagLayout();
		gbl_yellowThreshPanel.columnWidths = new int[]{0, 28, 0, 28, 0, 0};
		gbl_yellowThreshPanel.rowHeights = new int[]{0, 17, 20, 20, 0, 0, 0, 0};
		gbl_yellowThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_yellowThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		yellowThreshPanel.setLayout(gbl_yellowThreshPanel);
		
		enableYellowCheckbox = new JCheckBox("Enable detection");
		GridBagConstraints gbc_enableYellowCheckbox = new GridBagConstraints();
		gbc_enableYellowCheckbox.anchor = GridBagConstraints.WEST;
		gbc_enableYellowCheckbox.gridwidth = 3;
		gbc_enableYellowCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_enableYellowCheckbox.gridx = 1;
		gbc_enableYellowCheckbox.gridy = 0;
		yellowThreshPanel.add(enableYellowCheckbox, gbc_enableYellowCheckbox);
		
		yellowHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_yellowHueLabel = new GridBagConstraints();
		gbc_yellowHueLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueLabel.gridx = 1;
		gbc_yellowHueLabel.gridy = 1;
		yellowThreshPanel.add(yellowHueLabel, gbc_yellowHueLabel);
		
		yellowHueMinSpinner = new JSpinner();
		yellowHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_yellowHueMinSpinner = new GridBagConstraints();
		gbc_yellowHueMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueMinSpinner.gridx = 2;
		gbc_yellowHueMinSpinner.gridy = 1;
		yellowThreshPanel.add(yellowHueMinSpinner, gbc_yellowHueMinSpinner);
		
		yellowHueMaxSpinner = new JSpinner();
		yellowHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_yellowHueMaxSpinner = new GridBagConstraints();
		gbc_yellowHueMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueMaxSpinner.gridx = 3;
		gbc_yellowHueMaxSpinner.gridy = 1;
		yellowThreshPanel.add(yellowHueMaxSpinner, gbc_yellowHueMaxSpinner);
		
		yellowSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_yellowSatLabel = new GridBagConstraints();
		gbc_yellowSatLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatLabel.gridx = 1;
		gbc_yellowSatLabel.gridy = 2;
		yellowThreshPanel.add(yellowSatLabel, gbc_yellowSatLabel);
		
		yellowSatMinSpinner = new JSpinner();
		yellowSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowSatMinSpinner = new GridBagConstraints();
		gbc_yellowSatMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatMinSpinner.gridx = 2;
		gbc_yellowSatMinSpinner.gridy = 2;
		yellowThreshPanel.add(yellowSatMinSpinner, gbc_yellowSatMinSpinner);
		
		yellowSatMaxSpinner = new JSpinner();
		yellowSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowSatMaxSpinner = new GridBagConstraints();
		gbc_yellowSatMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatMaxSpinner.gridx = 3;
		gbc_yellowSatMaxSpinner.gridy = 2;
		yellowThreshPanel.add(yellowSatMaxSpinner, gbc_yellowSatMaxSpinner);
		
		yellowValLabel = new JLabel("VAL");
		GridBagConstraints gbc_yellowValLabel = new GridBagConstraints();
		gbc_yellowValLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValLabel.gridx = 1;
		gbc_yellowValLabel.gridy = 3;
		yellowThreshPanel.add(yellowValLabel, gbc_yellowValLabel);
		
		yellowValMinSpinner = new JSpinner();
		yellowValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowValMinSpinner = new GridBagConstraints();
		gbc_yellowValMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValMinSpinner.gridx = 2;
		gbc_yellowValMinSpinner.gridy = 3;
		yellowThreshPanel.add(yellowValMinSpinner, gbc_yellowValMinSpinner);
		
		yellowValMaxSpinner = new JSpinner();
		yellowValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowValMaxSpinner = new GridBagConstraints();
		gbc_yellowValMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValMaxSpinner.gridx = 3;
		gbc_yellowValMaxSpinner.gridy = 3;
		yellowThreshPanel.add(yellowValMaxSpinner, gbc_yellowValMaxSpinner);
		
		yellowSizeLabel = new JLabel("SIZE");
		GridBagConstraints gbc_yellowSizeLabel = new GridBagConstraints();
		gbc_yellowSizeLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowSizeLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSizeLabel.gridx = 1;
		gbc_yellowSizeLabel.gridy = 4;
		yellowThreshPanel.add(yellowSizeLabel, gbc_yellowSizeLabel);
		
		yellowSizeMinSpinner = new JSpinner();
		yellowSizeMinSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_yellowSizeMinSpinner = new GridBagConstraints();
		gbc_yellowSizeMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSizeMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSizeMinSpinner.gridx = 2;
		gbc_yellowSizeMinSpinner.gridy = 4;
		yellowThreshPanel.add(yellowSizeMinSpinner, gbc_yellowSizeMinSpinner);
		
		yellowSizeMaxSpinner = new JSpinner();
		yellowSizeMaxSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_yellowSizeMaxSpinner = new GridBagConstraints();
		gbc_yellowSizeMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSizeMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSizeMaxSpinner.gridx = 3;
		gbc_yellowSizeMaxSpinner.gridy = 4;
		yellowThreshPanel.add(yellowSizeMaxSpinner, gbc_yellowSizeMaxSpinner);
		
		correctHeightYellowCheckbox = new JCheckBox("Correct height");
		GridBagConstraints gbc_correctHeightYellowCheckbox = new GridBagConstraints();
		gbc_correctHeightYellowCheckbox.anchor = GridBagConstraints.WEST;
		gbc_correctHeightYellowCheckbox.gridwidth = 3;
		gbc_correctHeightYellowCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_correctHeightYellowCheckbox.gridx = 1;
		gbc_correctHeightYellowCheckbox.gridy = 5;
		yellowThreshPanel.add(correctHeightYellowCheckbox, gbc_correctHeightYellowCheckbox);
		
		yellowHeightFactorLabel = new JLabel("Height factor");
		GridBagConstraints gbc_yellowHeightFactorLabel = new GridBagConstraints();
		gbc_yellowHeightFactorLabel.gridwidth = 2;
		gbc_yellowHeightFactorLabel.insets = new Insets(0, 0, 0, 5);
		gbc_yellowHeightFactorLabel.gridx = 1;
		gbc_yellowHeightFactorLabel.gridy = 6;
		yellowThreshPanel.add(yellowHeightFactorLabel, gbc_yellowHeightFactorLabel);
		
		yellowHeightFactorSpinner = new JSpinner();
		yellowHeightFactorSpinner.setModel(new SpinnerNumberModel(1, 1, 999, 1));
		GridBagConstraints gbc_yellowHeightFactorSpinner = new GridBagConstraints();
		gbc_yellowHeightFactorSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_yellowHeightFactorSpinner.gridx = 3;
		gbc_yellowHeightFactorSpinner.gridy = 6;
		yellowThreshPanel.add(yellowHeightFactorSpinner, gbc_yellowHeightFactorSpinner);
		
		robotSettingPanel = new JPanel();
		robotControlTabbedPanel.addTab("Robot", null, robotSettingPanel, null);
		GridBagLayout gbl_robotSettingPanel = new GridBagLayout();
		gbl_robotSettingPanel.columnWidths = new int[]{0, 0, 0};
		gbl_robotSettingPanel.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_robotSettingPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_robotSettingPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		robotSettingPanel.setLayout(gbl_robotSettingPanel);
		
		robotConnectionPanel = new JPanel();
		robotConnectionPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Connection", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_robotConnectionPanel = new GridBagConstraints();
		gbc_robotConnectionPanel.fill = GridBagConstraints.BOTH;
		gbc_robotConnectionPanel.insets = new Insets(0, 0, 5, 5);
		gbc_robotConnectionPanel.gridx = 0;
		gbc_robotConnectionPanel.gridy = 0;
		robotSettingPanel.add(robotConnectionPanel, gbc_robotConnectionPanel);
		GridBagLayout gbl_robotConnectionPanel = new GridBagLayout();
		gbl_robotConnectionPanel.columnWidths = new int[]{0, 0};
		gbl_robotConnectionPanel.rowHeights = new int[]{0, 0, 0};
		gbl_robotConnectionPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_robotConnectionPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		robotConnectionPanel.setLayout(gbl_robotConnectionPanel);
		
		robotDebugModeCheckbox = new JCheckBox("Debug mode");
		GridBagConstraints gbc_robotDebugModeCheckbox = new GridBagConstraints();
		gbc_robotDebugModeCheckbox.anchor = GridBagConstraints.WEST;
		gbc_robotDebugModeCheckbox.insets = new Insets(0, 0, 5, 0);
		gbc_robotDebugModeCheckbox.gridx = 0;
		gbc_robotDebugModeCheckbox.gridy = 0;
		robotConnectionPanel.add(robotDebugModeCheckbox, gbc_robotDebugModeCheckbox);
		
		robotConnectButton = new JButton("Connect");
		GridBagConstraints gbc_robotConnectButton = new GridBagConstraints();
		gbc_robotConnectButton.gridx = 0;
		gbc_robotConnectButton.gridy = 1;
		robotConnectionPanel.add(robotConnectButton, gbc_robotConnectButton);
		robotConnectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!robotRunning) {
					connectToRobot();
				} else {
					disconnectFromRobot();
				}
			}
		});
		
		robotOptionPanel = new JPanel();
		robotOptionPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_robotOptionPanel = new GridBagConstraints();
		gbc_robotOptionPanel.insets = new Insets(0, 0, 5, 5);
		gbc_robotOptionPanel.fill = GridBagConstraints.BOTH;
		gbc_robotOptionPanel.gridx = 0;
		gbc_robotOptionPanel.gridy = 1;
		robotSettingPanel.add(robotOptionPanel, gbc_robotOptionPanel);
		GridBagLayout gbl_robotOptionPanel = new GridBagLayout();
		gbl_robotOptionPanel.columnWidths = new int[]{0, 0};
		gbl_robotOptionPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_robotOptionPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_robotOptionPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		robotOptionPanel.setLayout(gbl_robotOptionPanel);
		
		robotOverrideVisionButton = new JButton("Overlay Vision");
		GridBagConstraints gbc_robotOverrideVisionButton = new GridBagConstraints();
		gbc_robotOverrideVisionButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotOverrideVisionButton.gridx = 0;
		gbc_robotOverrideVisionButton.gridy = 0;
		robotOptionPanel.add(robotOverrideVisionButton, gbc_robotOverrideVisionButton);
		robotOverrideVisionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.toggleDrawingOnWorldImage();
				}
			}
		});
		
		robotColorLabel = new JLabel("Color");
		GridBagConstraints gbc_robotColorLabel = new GridBagConstraints();
		gbc_robotColorLabel.anchor = GridBagConstraints.WEST;
		gbc_robotColorLabel.insets = new Insets(0, 0, 5, 0);
		gbc_robotColorLabel.gridx = 0;
		gbc_robotColorLabel.gridy = 1;
		robotOptionPanel.add(robotColorLabel, gbc_robotColorLabel);
		
		robotColorBlueButton = new JRadioButton("Blue");
		GridBagConstraints gbc_robotColorBlueButton = new GridBagConstraints();
		gbc_robotColorBlueButton.anchor = GridBagConstraints.WEST;
		gbc_robotColorBlueButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotColorBlueButton.gridx = 0;
		gbc_robotColorBlueButton.gridy = 2;
		robotOptionPanel.add(robotColorBlueButton, gbc_robotColorBlueButton);
		robotColorBlueButton.setSelected(true);
		robotColorButtonGroup.add(robotColorBlueButton);
		
		robotColorYellowButton = new JRadioButton("Yellow");
		GridBagConstraints gbc_robotColorYellowButton = new GridBagConstraints();
		gbc_robotColorYellowButton.anchor = GridBagConstraints.WEST;
		gbc_robotColorYellowButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotColorYellowButton.gridx = 0;
		gbc_robotColorYellowButton.gridy = 3;
		robotOptionPanel.add(robotColorYellowButton, gbc_robotColorYellowButton);
		robotColorButtonGroup.add(robotColorYellowButton);
		
		robotGateLabel = new JLabel("Our gate");
		GridBagConstraints gbc_robotGateLabel = new GridBagConstraints();
		gbc_robotGateLabel.anchor = GridBagConstraints.WEST;
		gbc_robotGateLabel.insets = new Insets(0, 0, 5, 0);
		gbc_robotGateLabel.gridx = 0;
		gbc_robotGateLabel.gridy = 4;
		robotOptionPanel.add(robotGateLabel, gbc_robotGateLabel);
		
		robotGateLeftButton = new JRadioButton("Left");
		GridBagConstraints gbc_robotGateLeftButton = new GridBagConstraints();
		gbc_robotGateLeftButton.anchor = GridBagConstraints.WEST;
		gbc_robotGateLeftButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotGateLeftButton.gridx = 0;
		gbc_robotGateLeftButton.gridy = 5;
		robotOptionPanel.add(robotGateLeftButton, gbc_robotGateLeftButton);
		robotGateLeftButton.setSelected(true);
		robotGateButtonGroup.add(robotGateLeftButton);
		
		robotGateRightButton = new JRadioButton("Right");
		GridBagConstraints gbc_robotGateRightButton = new GridBagConstraints();
		gbc_robotGateRightButton.anchor = GridBagConstraints.WEST;
		gbc_robotGateRightButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotGateRightButton.gridx = 0;
		gbc_robotGateRightButton.gridy = 6;
		robotOptionPanel.add(robotGateRightButton, gbc_robotGateRightButton);
		robotGateButtonGroup.add(robotGateRightButton);
		
		robotChangeColorGoal = new JButton("Change Settings");
		GridBagConstraints gbc_robotChangeColorGoal = new GridBagConstraints();
		gbc_robotChangeColorGoal.gridx = 0;
		gbc_robotChangeColorGoal.gridy = 7;
		robotOptionPanel.add(robotChangeColorGoal, gbc_robotChangeColorGoal);
		robotChangeColorGoal.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.setOwnGoalLeft(robotGateLeftButton.isSelected());
					aiInstance.setOwnTeamBlue(robotColorBlueButton.isSelected());
				}
			}
		});
		
		robotBehaviourPanel = new JPanel();
		robotBehaviourPanel.setBorder(new TitledBorder(null, "Behaviour", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_robotBehaviourPanel = new GridBagConstraints();
		gbc_robotBehaviourPanel.insets = new Insets(0, 0, 5, 5);
		gbc_robotBehaviourPanel.fill = GridBagConstraints.BOTH;
		gbc_robotBehaviourPanel.gridx = 0;
		gbc_robotBehaviourPanel.gridy = 2;
		robotSettingPanel.add(robotBehaviourPanel, gbc_robotBehaviourPanel);
		GridBagLayout gbl_robotBehaviourPanel = new GridBagLayout();
		gbl_robotBehaviourPanel.columnWidths = new int[]{0, 0, 0};
		gbl_robotBehaviourPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
		gbl_robotBehaviourPanel.columnWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		gbl_robotBehaviourPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		robotBehaviourPanel.setLayout(gbl_robotBehaviourPanel);
		
		robotBehMainLabel = new JLabel("Main");
		GridBagConstraints gbc_robotBehMainLabel = new GridBagConstraints();
		gbc_robotBehMainLabel.anchor = GridBagConstraints.WEST;
		gbc_robotBehMainLabel.gridwidth = 2;
		gbc_robotBehMainLabel.insets = new Insets(0, 0, 5, 0);
		gbc_robotBehMainLabel.gridx = 0;
		gbc_robotBehMainLabel.gridy = 0;
		robotBehaviourPanel.add(robotBehMainLabel, gbc_robotBehMainLabel);
		
		robotPlayButton = new JButton("Play");
		robotPlayButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.setState(AIMaster.AIState.PLAY);
				}
			}
		});
		GridBagConstraints gbc_robotPlayButton = new GridBagConstraints();
		gbc_robotPlayButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_robotPlayButton.insets = new Insets(0, 0, 5, 5);
		gbc_robotPlayButton.gridx = 0;
		gbc_robotPlayButton.gridy = 1;
		robotBehaviourPanel.add(robotPlayButton, gbc_robotPlayButton);
		
		robotSitButton = new JButton("Sit");
		robotSitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.setState(AIMaster.AIState.SIT);
				}
			}
		});
		GridBagConstraints gbc_robotSitButton = new GridBagConstraints();
		gbc_robotSitButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_robotSitButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotSitButton.gridx = 1;
		gbc_robotSitButton.gridy = 1;
		robotBehaviourPanel.add(robotSitButton, gbc_robotSitButton);
		
		robotPenaltiesLabel = new JLabel("Penalties");
		GridBagConstraints gbc_robotPenaltiesLabel = new GridBagConstraints();
		gbc_robotPenaltiesLabel.anchor = GridBagConstraints.WEST;
		gbc_robotPenaltiesLabel.gridwidth = 2;
		gbc_robotPenaltiesLabel.insets = new Insets(0, 0, 5, 0);
		gbc_robotPenaltiesLabel.gridx = 0;
		gbc_robotPenaltiesLabel.gridy = 2;
		robotBehaviourPanel.add(robotPenaltiesLabel, gbc_robotPenaltiesLabel);
		
		robotShootPenaltyButton = new JButton("Shoot");
		robotShootPenaltyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.setState(AIMaster.AIState.SHOOT_PENALTIES);
				}
			}
		});
		GridBagConstraints gbc_robotShootPenaltyButton = new GridBagConstraints();
		gbc_robotShootPenaltyButton.insets = new Insets(0, 0, 5, 5);
		gbc_robotShootPenaltyButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_robotShootPenaltyButton.gridx = 0;
		gbc_robotShootPenaltyButton.gridy = 3;
		robotBehaviourPanel.add(robotShootPenaltyButton, gbc_robotShootPenaltyButton);
		
		robotDefendPenaltyButton = new JButton("Defend");
		robotDefendPenaltyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.setState(AIMaster.AIState.DEFEND_PENALTIES);
				}
			}
		});
		GridBagConstraints gbc_robotDefendPenaltyButton = new GridBagConstraints();
		gbc_robotDefendPenaltyButton.insets = new Insets(0, 0, 5, 0);
		gbc_robotDefendPenaltyButton.gridx = 1;
		gbc_robotDefendPenaltyButton.gridy = 3;
		robotBehaviourPanel.add(robotDefendPenaltyButton, gbc_robotDefendPenaltyButton);
		
		robotOtherLabel = new JLabel("Other");
		GridBagConstraints gbc_robotOtherLabel = new GridBagConstraints();
		gbc_robotOtherLabel.anchor = GridBagConstraints.WEST;
		gbc_robotOtherLabel.insets = new Insets(0, 0, 5, 0);
		gbc_robotOtherLabel.gridwidth = 2;
		gbc_robotOtherLabel.gridx = 0;
		gbc_robotOtherLabel.gridy = 4;
		robotBehaviourPanel.add(robotOtherLabel, gbc_robotOtherLabel);
		
		robotBehaviourCombobox = new JComboBox();
		GridBagConstraints gbc_robotBehaviourCombobox = new GridBagConstraints();
		gbc_robotBehaviourCombobox.fill = GridBagConstraints.HORIZONTAL;
		gbc_robotBehaviourCombobox.gridwidth = 2;
		gbc_robotBehaviourCombobox.insets = new Insets(0, 0, 5, 0);
		gbc_robotBehaviourCombobox.gridx = 0;
		gbc_robotBehaviourCombobox.gridy = 5;
		robotBehaviourPanel.add(robotBehaviourCombobox, gbc_robotBehaviourCombobox);
		robotBehaviourCombobox.setBounds(662, 342, 117, 24);
		
		JButton robotChangeBehaviourButton = new JButton("Change Behaviour");
		GridBagConstraints gbc_robotChangeBehaviourButton = new GridBagConstraints();
		gbc_robotChangeBehaviourButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_robotChangeBehaviourButton.gridwidth = 2;
		gbc_robotChangeBehaviourButton.gridx = 0;
		gbc_robotChangeBehaviourButton.gridy = 6;
		robotBehaviourPanel.add(robotChangeBehaviourButton, gbc_robotChangeBehaviourButton);
		robotChangeBehaviourButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (aiInstance != null) {
					aiInstance.setState(AIMaster.AIState.values()[robotBehaviourCombobox.getSelectedIndex()]);
				}
			}
		});
		robotChangeBehaviourButton.setBounds(662, 378, 117, 25);
		
		testBenchPanel = new JPanel();
		robotControlTabbedPanel.addTab("Test Bench", null, testBenchPanel, null);
		robotControlTabbedPanel.setEnabledAt(2, true);
		GridBagLayout gbl_testBenchPanel = new GridBagLayout();
		gbl_testBenchPanel.columnWidths = new int[]{0, 0, 0, 0, 0};
		gbl_testBenchPanel.rowHeights = new int[]{0, 0, 0, 0};
		gbl_testBenchPanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_testBenchPanel.rowWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		testBenchPanel.setLayout(gbl_testBenchPanel);
		
		JButton runTestBenchButton = new JButton("Run Test");
		runTestBenchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				runTestBench();
			}
		});
		
		JButton selectTestRunButton = new JButton("...");
		selectTestRunButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selectTestCase();
			}
		});
		
		testCaseLabel = new JLabel("Test Case");
		GridBagConstraints gbc_testCaseLabel = new GridBagConstraints();
		gbc_testCaseLabel.insets = new Insets(0, 0, 5, 5);
		gbc_testCaseLabel.anchor = GridBagConstraints.EAST;
		gbc_testCaseLabel.gridx = 0;
		gbc_testCaseLabel.gridy = 0;
		testBenchPanel.add(testCaseLabel, gbc_testCaseLabel);
		
		testCaseTextfield = new JTextField();
		GridBagConstraints gbc_testCaseTextfield = new GridBagConstraints();
		gbc_testCaseTextfield.insets = new Insets(0, 0, 5, 5);
		gbc_testCaseTextfield.fill = GridBagConstraints.HORIZONTAL;
		gbc_testCaseTextfield.gridx = 1;
		gbc_testCaseTextfield.gridy = 0;
		testBenchPanel.add(testCaseTextfield, gbc_testCaseTextfield);
		testCaseTextfield.setColumns(10);
		GridBagConstraints gbc_selectTestRunButton = new GridBagConstraints();
		gbc_selectTestRunButton.insets = new Insets(0, 0, 5, 5);
		gbc_selectTestRunButton.gridx = 2;
		gbc_selectTestRunButton.gridy = 0;
		testBenchPanel.add(selectTestRunButton, gbc_selectTestRunButton);
		GridBagConstraints gbc_runTestBenchButton = new GridBagConstraints();
		gbc_runTestBenchButton.insets = new Insets(0, 0, 5, 0);
		gbc_runTestBenchButton.gridx = 3;
		gbc_runTestBenchButton.gridy = 0;
		testBenchPanel.add(runTestBenchButton, gbc_runTestBenchButton);
		
		testBenchOutputPanel = new JPanel();
		testBenchOutputPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Test Bench Output", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_testBenchOutputPanel = new GridBagConstraints();
		gbc_testBenchOutputPanel.fill = GridBagConstraints.BOTH;
		gbc_testBenchOutputPanel.gridwidth = 5;
		gbc_testBenchOutputPanel.insets = new Insets(0, 0, 5, 0);
		gbc_testBenchOutputPanel.gridx = 0;
		gbc_testBenchOutputPanel.gridy = 1;
		testBenchPanel.add(testBenchOutputPanel, gbc_testBenchOutputPanel);
		GridBagLayout gbl_testBenchOutputPanel = new GridBagLayout();
		gbl_testBenchOutputPanel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_testBenchOutputPanel.rowHeights = new int[]{0, 0};
		gbl_testBenchOutputPanel.columnWeights = new double[]{1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_testBenchOutputPanel.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		testBenchOutputPanel.setLayout(gbl_testBenchOutputPanel);
		
		testBenchOutputScrollPane = new JScrollPane();
		GridBagConstraints gbc_testBenchOutputScrollPane = new GridBagConstraints();
		gbc_testBenchOutputScrollPane.fill = GridBagConstraints.BOTH;
		gbc_testBenchOutputScrollPane.gridwidth = 3;
		gbc_testBenchOutputScrollPane.insets = new Insets(0, 0, 0, 5);
		gbc_testBenchOutputScrollPane.gridx = 0;
		gbc_testBenchOutputScrollPane.gridy = 0;
		testBenchOutputPanel.add(testBenchOutputScrollPane, gbc_testBenchOutputScrollPane);
		
		testBenchOutputTextarea = new JTextArea();
		testBenchOutputScrollPane.setViewportView(testBenchOutputTextarea);
		testBenchOutputTextarea.setEditable(false);
		
		JButton saveTestBenchOutputButton = new JButton("Save to File");
		saveTestBenchOutputButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveTestBenchOutput();
			}
		});
		GridBagConstraints gbc_saveTestBenchOutputButton = new GridBagConstraints();
		gbc_saveTestBenchOutputButton.insets = new Insets(0, 0, 0, 5);
		gbc_saveTestBenchOutputButton.anchor = GridBagConstraints.EAST;
		gbc_saveTestBenchOutputButton.gridwidth = 4;
		gbc_saveTestBenchOutputButton.gridx = 0;
		gbc_saveTestBenchOutputButton.gridy = 2;
		testBenchPanel.add(saveTestBenchOutputButton, gbc_saveTestBenchOutputButton);
	}
	
	private JTabbedPane robotControlTabbedPanel;
	
	private JPanel visionSettingPanel;
	private JPanel generalSettingPanel;
	private JButton loadConfigButton;
	private JButton saveConfigButton;
	private JCheckBox showThreshCheckbox;
	private JCheckBox showContoursCheckbox;
	private JCheckBox showBoxesCheckbox;
	private JCheckBox showStateDataCheckbox;
	private JCheckBox showWorldCheckbox;
	
	private JSpinner fieldLowYSpinner;	
	private JSpinner fieldLowXSpinner;	
	private JSpinner fieldHighXSpinner;	
	private JSpinner fieldHighYSpinner;
	
	private JPanel ballThreshPanel;	
	private JSpinner ballHueMinSpinner;
	private JLabel ballHueLabel;
	private JLabel ballSatLabel;
	private JLabel ballValLabel;
	private JSpinner ballHueMaxSpinner;
	private JSpinner ballSatMinSpinner;
	private JSpinner ballSatMaxSpinner;
	private JSpinner ballValMinSpinner;
	private JSpinner ballValMaxSpinner;
	private JLabel ballSizeLabel;
	private JSpinner ballSizeMinSpinner;
	private JSpinner ballSizeMaxSpinner;
	
	private JPanel blueThreshPanel;
	private JLabel blueHueLabel;
	private JLabel blueSatLabel;
	private JLabel blueValLabel;
	private JSpinner blueHueMinSpinner;
	private JSpinner blueSatMinSpinner;
	private JSpinner blueValMinSpinner;
	private JSpinner blueHueMaxSpinner;
	private JSpinner blueSatMaxSpinner;
	private JSpinner blueValMaxSpinner;
	private JLabel blueSizeLabel;
	private JSpinner blueSizeMinSpinner;
	private JSpinner blueSizeMaxSpinner;
	private JCheckBox enableBlueCheckbox;
	private JCheckBox correctHeightBlueCheckbox;
	private JLabel blueHeightFactorLabel;
	private JSpinner blueHeightFactorSpinner;
	
	private JPanel yellowThreshPanel;
	private JLabel yellowHueLabel;
	private JLabel yellowSatLabel;
	private JLabel yellowValLabel;
	private JSpinner yellowHueMinSpinner;
	private JSpinner yellowSatMinSpinner;
	private JSpinner yellowValMinSpinner;
	private JSpinner yellowHueMaxSpinner;
	private JSpinner yellowSatMaxSpinner;
	private JSpinner yellowValMaxSpinner;
	private JLabel yellowSizeLabel;
	private JSpinner yellowSizeMinSpinner;
	private JSpinner yellowSizeMaxSpinner;
	private JCheckBox enableYellowCheckbox;
	private JCheckBox correctHeightYellowCheckbox;
	private JLabel yellowHeightFactorLabel;
	private JSpinner yellowHeightFactorSpinner;
	
	private JPanel undistortionPanel;
	private JLabel fxLabel;
	private JLabel fyLabel;
	private JLabel cxLabel;
	private JLabel cyLabel;
	private JLabel k1Label;
	private JLabel k2Label;
	private JLabel p1Label;
	private JLabel p2Label;
	private JTextField fxTextfield;
	private JTextField fyTextfield;
	private JTextField cxTextfield;
	private JTextField cyTextfield;
	private JTextField k1Textfield;
	private JTextField k2Textfield;
	private JTextField p1Textfield;
	private JTextField p2Textfield;
	private JLabel intristicLabel;
	private JLabel distortionLabel;
	private JTextField k3Textfield;
	private JTextField k4Textfield;
	private JTextField k5Textfield;
	private JTextField k6Textfield;
	private JLabel k3Label;
	private JLabel k4Label;
	private JLabel k5Label;
	private JLabel k6Label;
	private JCheckBox enableUndistortionCheckbox;

	private JPanel robotSettingPanel;
	private JPanel robotOptionPanel;
	private JPanel robotBehaviourPanel;
	private JPanel robotConnectionPanel;
	private JRadioButton robotColorBlueButton;
	private JRadioButton robotColorYellowButton;
	private JLabel robotColorLabel;
	private JRadioButton robotGateLeftButton;
	private JRadioButton robotGateRightButton;
	private JLabel robotGateLabel;
	private JButton robotConnectButton;
	private JButton robotOverrideVisionButton;
	private JButton robotChangeColorGoal;
	private final ButtonGroup robotColorButtonGroup = new ButtonGroup();
	private final ButtonGroup robotGateButtonGroup = new ButtonGroup();
	private JComboBox robotBehaviourCombobox;
	private JCheckBox robotDebugModeCheckbox;
	private JButton robotPlayButton;
	private JButton robotSitButton;
	private JButton robotShootPenaltyButton;
	private JButton robotDefendPenaltyButton;
	private JLabel robotPenaltiesLabel;
	private JLabel robotBehMainLabel;
	private JLabel robotOtherLabel;
	
	private JPanel testBenchPanel;
	private JTextField testCaseTextfield;
	private JTextArea testBenchOutputTextarea;
	private JScrollPane testBenchOutputScrollPane;
	private JPanel testBenchOutputPanel;
	private JLabel testCaseLabel;
	
	private JPanel imageCanvasPanel;
}
