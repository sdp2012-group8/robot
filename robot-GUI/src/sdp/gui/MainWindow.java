package sdp.gui;

import java.awt.image.BufferedImage;

import java.awt.Component;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JSpinner;
import javax.swing.border.TitledBorder;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTabbedPane;

import sdp.AI.AI;
import sdp.AI.AIVisualServoing;
import sdp.AI.AI.mode;
import sdp.common.Communicator;
import sdp.common.FPSCounter;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.communicator.JComm;
import sdp.vision.ImageProcessorConfig;
import sdp.vision.Vision;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JCheckBox;


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
	
	
	/** In what integer range will floats be represented in spinners. */
	private static final int SPINNER_FLOAT_RANGE = 1000;	
	/** The window title. */
	private static final String WINDOW_TITLE = "Battlestation";
	
	
	/** Window's FPS counter. */
	private FPSCounter fpsCounter;
	
	/** Active AI subsystem instance. */
	private AI aiInstance = null;
	/** Flag that indicates whether the robot is running. */
	private boolean robotRunning = false;
	
	/** Active vision subsystem instance. */
	private Vision vision = null;	
	/** A flag that controls whether vision system calibration is enabled. */
	private boolean visionChangesEnabled = true;
	
	/** GUI's world state provider. */
	private WorldStateObserver worldStateObserver;
	
	
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
		
		setSize(new Dimension(1050, 480));
		setTitle(WINDOW_TITLE);
		initComponents();
		
		if (vision != null) {
			updateVisionComponentValues();
		} else {
			robotControlTabbedPanel.remove(visionSettingPanel);
		}
		
		if (testMode) {
			robotControlTabbedPanel.remove(robotSettingPanel);
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
		imageLabel = new javax.swing.JLabel();
		GridBagConstraints gbc_imageLabel = new GridBagConstraints();
		gbc_imageLabel.insets = new Insets(0, 0, 5, 0);
		gbc_imageLabel.fill = GridBagConstraints.BOTH;
		gbc_imageLabel.gridx = 0;
		gbc_imageLabel.gridy = 1;
		cameraImagePanel.add(imageLabel, gbc_imageLabel);
		imageLabel.setAlignmentY(Component.TOP_ALIGNMENT);
		
		imageLabel.setText("Image goes here");
		
		robotControlTabbedPanel = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_robotControlTabbedPanel = new GridBagConstraints();
		gbc_robotControlTabbedPanel.fill = GridBagConstraints.BOTH;
		gbc_robotControlTabbedPanel.gridx = 1;
		gbc_robotControlTabbedPanel.gridy = 0;
		getContentPane().add(robotControlTabbedPanel, gbc_robotControlTabbedPanel);
		
		visionSettingPanel = new JPanel();
		robotControlTabbedPanel.addTab("Vision Calibration", null, visionSettingPanel, null);
		robotControlTabbedPanel.setEnabledAt(0, true);
		visionSettingPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		GridBagLayout gbl_visionSettingPanel = new GridBagLayout();
		gbl_visionSettingPanel.columnWidths = new int[]{200, 200, 0};
		gbl_visionSettingPanel.rowHeights = new int[]{15, 0, 0, 0, 0};
		gbl_visionSettingPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_visionSettingPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		visionSettingPanel.setLayout(gbl_visionSettingPanel);
		
		JPanel fieldWallPanel = new JPanel();
		fieldWallPanel.setBorder(new TitledBorder(null, "Field borders", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_fieldWallPanel = new GridBagConstraints();
		gbc_fieldWallPanel.insets = new Insets(0, 0, 5, 5);
		gbc_fieldWallPanel.fill = GridBagConstraints.BOTH;
		gbc_fieldWallPanel.gridx = 0;
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
		
		ballThreshPanel = new JPanel();
		ballThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Ball settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_ballThreshPanel = new GridBagConstraints();
		gbc_ballThreshPanel.insets = new Insets(0, 0, 5, 0);
		gbc_ballThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_ballThreshPanel.gridx = 1;
		gbc_ballThreshPanel.gridy = 0;
		visionSettingPanel.add(ballThreshPanel, gbc_ballThreshPanel);
		GridBagLayout gbl_ballThreshPanel = new GridBagLayout();
		gbl_ballThreshPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_ballThreshPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_ballThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_ballThreshPanel.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		ballThreshPanel.setLayout(gbl_ballThreshPanel);
		
		ballHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_ballHueLabel = new GridBagConstraints();
		gbc_ballHueLabel.anchor = GridBagConstraints.EAST;
		gbc_ballHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueLabel.gridx = 1;
		gbc_ballHueLabel.gridy = 1;
		ballThreshPanel.add(ballHueLabel, gbc_ballHueLabel);
		
		ballHueMinSpinner = new JSpinner();
		ballHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_ballHueMinSpinner = new GridBagConstraints();
		gbc_ballHueMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueMinSpinner.gridx = 2;
		gbc_ballHueMinSpinner.gridy = 1;
		ballThreshPanel.add(ballHueMinSpinner, gbc_ballHueMinSpinner);
		
		ballHueMaxSpinner = new JSpinner();
		ballHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_ballHueMaxSpinner = new GridBagConstraints();
		gbc_ballHueMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueMaxSpinner.gridx = 3;
		gbc_ballHueMaxSpinner.gridy = 1;
		ballThreshPanel.add(ballHueMaxSpinner, gbc_ballHueMaxSpinner);
		
		ballSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_ballSatLabel = new GridBagConstraints();
		gbc_ballSatLabel.anchor = GridBagConstraints.EAST;
		gbc_ballSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatLabel.gridx = 1;
		gbc_ballSatLabel.gridy = 2;
		ballThreshPanel.add(ballSatLabel, gbc_ballSatLabel);
		
		ballSatMinSpinner = new JSpinner();
		ballSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballSatMinSpinner = new GridBagConstraints();
		gbc_ballSatMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatMinSpinner.gridx = 2;
		gbc_ballSatMinSpinner.gridy = 2;
		ballThreshPanel.add(ballSatMinSpinner, gbc_ballSatMinSpinner);
		
		ballSatMaxSpinner = new JSpinner();
		ballSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballSatMaxSpinner = new GridBagConstraints();
		gbc_ballSatMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatMaxSpinner.gridx = 3;
		gbc_ballSatMaxSpinner.gridy = 2;
		ballThreshPanel.add(ballSatMaxSpinner, gbc_ballSatMaxSpinner);
		
		ballValLabel = new JLabel("VAL");
		GridBagConstraints gbc_ballValLabel = new GridBagConstraints();
		gbc_ballValLabel.anchor = GridBagConstraints.EAST;
		gbc_ballValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballValLabel.gridx = 1;
		gbc_ballValLabel.gridy = 3;
		ballThreshPanel.add(ballValLabel, gbc_ballValLabel);
		
		ballValMinSpinner = new JSpinner();
		ballValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballValMinSpinner = new GridBagConstraints();
		gbc_ballValMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballValMinSpinner.gridx = 2;
		gbc_ballValMinSpinner.gridy = 3;
		ballThreshPanel.add(ballValMinSpinner, gbc_ballValMinSpinner);
		
		ballValMaxSpinner = new JSpinner();
		ballValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballValMaxSpinner = new GridBagConstraints();
		gbc_ballValMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballValMaxSpinner.gridx = 3;
		gbc_ballValMaxSpinner.gridy = 3;
		ballThreshPanel.add(ballValMaxSpinner, gbc_ballValMaxSpinner);
		
		ballSizeLabel = new JLabel("SIZE");
		GridBagConstraints gbc_ballSizeLabel = new GridBagConstraints();
		gbc_ballSizeLabel.anchor = GridBagConstraints.EAST;
		gbc_ballSizeLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballSizeLabel.gridx = 1;
		gbc_ballSizeLabel.gridy = 4;
		ballThreshPanel.add(ballSizeLabel, gbc_ballSizeLabel);
		
		ballSizeMinSpinner = new JSpinner();
		ballSizeMinSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_ballSizeMinSpinner = new GridBagConstraints();
		gbc_ballSizeMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSizeMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSizeMinSpinner.gridx = 2;
		gbc_ballSizeMinSpinner.gridy = 4;
		ballThreshPanel.add(ballSizeMinSpinner, gbc_ballSizeMinSpinner);
		
		ballSizeMaxSpinner = new JSpinner();
		ballSizeMaxSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_ballSizeMaxSpinner = new GridBagConstraints();
		gbc_ballSizeMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_ballSizeMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSizeMaxSpinner.gridx = 3;
		gbc_ballSizeMaxSpinner.gridy = 4;
		ballThreshPanel.add(ballSizeMaxSpinner, gbc_ballSizeMaxSpinner);
		
		blueThreshPanel = new JPanel();
		blueThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Blue T settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_blueThreshPanel = new GridBagConstraints();
		gbc_blueThreshPanel.insets = new Insets(0, 0, 5, 0);
		gbc_blueThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_blueThreshPanel.gridx = 1;
		gbc_blueThreshPanel.gridy = 1;
		visionSettingPanel.add(blueThreshPanel, gbc_blueThreshPanel);
		GridBagLayout gbl_blueThreshPanel = new GridBagLayout();
		gbl_blueThreshPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_blueThreshPanel.rowHeights = new int[]{17, 20, 20, 0, 0};
		gbl_blueThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_blueThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		blueThreshPanel.setLayout(gbl_blueThreshPanel);
		
		blueHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_blueHueLabel = new GridBagConstraints();
		gbc_blueHueLabel.anchor = GridBagConstraints.EAST;
		gbc_blueHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueLabel.gridx = 1;
		gbc_blueHueLabel.gridy = 0;
		blueThreshPanel.add(blueHueLabel, gbc_blueHueLabel);
		
		blueHueMinSpinner = new JSpinner();
		blueHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_blueHueMinSpinner = new GridBagConstraints();
		gbc_blueHueMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueMinSpinner.gridx = 2;
		gbc_blueHueMinSpinner.gridy = 0;
		blueThreshPanel.add(blueHueMinSpinner, gbc_blueHueMinSpinner);
		
		blueHueMaxSpinner = new JSpinner();
		blueHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_blueHueMaxSpinner = new GridBagConstraints();
		gbc_blueHueMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueMaxSpinner.gridx = 3;
		gbc_blueHueMaxSpinner.gridy = 0;
		blueThreshPanel.add(blueHueMaxSpinner, gbc_blueHueMaxSpinner);
		
		blueSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_blueSatLabel = new GridBagConstraints();
		gbc_blueSatLabel.anchor = GridBagConstraints.EAST;
		gbc_blueSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatLabel.gridx = 1;
		gbc_blueSatLabel.gridy = 1;
		blueThreshPanel.add(blueSatLabel, gbc_blueSatLabel);
		
		blueSatMinSpinner = new JSpinner();
		blueSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueSatMinSpinner = new GridBagConstraints();
		gbc_blueSatMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatMinSpinner.gridx = 2;
		gbc_blueSatMinSpinner.gridy = 1;
		blueThreshPanel.add(blueSatMinSpinner, gbc_blueSatMinSpinner);
		
		blueSatMaxSpinner = new JSpinner();
		blueSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueSatMaxSpinner = new GridBagConstraints();
		gbc_blueSatMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatMaxSpinner.gridx = 3;
		gbc_blueSatMaxSpinner.gridy = 1;
		blueThreshPanel.add(blueSatMaxSpinner, gbc_blueSatMaxSpinner);
		
		blueValLabel = new JLabel("VAL");
		GridBagConstraints gbc_blueValLabel = new GridBagConstraints();
		gbc_blueValLabel.anchor = GridBagConstraints.EAST;
		gbc_blueValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueValLabel.gridx = 1;
		gbc_blueValLabel.gridy = 2;
		blueThreshPanel.add(blueValLabel, gbc_blueValLabel);
		
		blueValMinSpinner = new JSpinner();
		blueValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueValMinSpinner = new GridBagConstraints();
		gbc_blueValMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueValMinSpinner.gridx = 2;
		gbc_blueValMinSpinner.gridy = 2;
		blueThreshPanel.add(blueValMinSpinner, gbc_blueValMinSpinner);
		
		blueValMaxSpinner = new JSpinner();
		blueValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueValMaxSpinner = new GridBagConstraints();
		gbc_blueValMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueValMaxSpinner.gridx = 3;
		gbc_blueValMaxSpinner.gridy = 2;
		blueThreshPanel.add(blueValMaxSpinner, gbc_blueValMaxSpinner);
		
		blueSizeLabel = new JLabel("SIZE");
		GridBagConstraints gbc_blueSizeLabel = new GridBagConstraints();
		gbc_blueSizeLabel.anchor = GridBagConstraints.EAST;
		gbc_blueSizeLabel.insets = new Insets(0, 0, 0, 5);
		gbc_blueSizeLabel.gridx = 1;
		gbc_blueSizeLabel.gridy = 3;
		blueThreshPanel.add(blueSizeLabel, gbc_blueSizeLabel);
		
		blueSizeMinSpinner = new JSpinner();
		blueSizeMinSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_blueSizeMinSpinner = new GridBagConstraints();
		gbc_blueSizeMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSizeMinSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_blueSizeMinSpinner.gridx = 2;
		gbc_blueSizeMinSpinner.gridy = 3;
		blueThreshPanel.add(blueSizeMinSpinner, gbc_blueSizeMinSpinner);
		
		blueSizeMaxSpinner = new JSpinner();
		blueSizeMaxSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_blueSizeMaxSpinner = new GridBagConstraints();
		gbc_blueSizeMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_blueSizeMaxSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_blueSizeMaxSpinner.gridx = 3;
		gbc_blueSizeMaxSpinner.gridy = 3;
		blueThreshPanel.add(blueSizeMaxSpinner, gbc_blueSizeMaxSpinner);
		
		yellowThreshPanel = new JPanel();
		yellowThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Yellow T settings", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_yellowThreshPanel = new GridBagConstraints();
		gbc_yellowThreshPanel.insets = new Insets(0, 0, 5, 0);
		gbc_yellowThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_yellowThreshPanel.gridx = 1;
		gbc_yellowThreshPanel.gridy = 2;
		visionSettingPanel.add(yellowThreshPanel, gbc_yellowThreshPanel);
		GridBagLayout gbl_yellowThreshPanel = new GridBagLayout();
		gbl_yellowThreshPanel.columnWidths = new int[]{0, 28, 0, 28, 0, 0};
		gbl_yellowThreshPanel.rowHeights = new int[]{17, 20, 20, 0, 0};
		gbl_yellowThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_yellowThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		yellowThreshPanel.setLayout(gbl_yellowThreshPanel);
		
		yellowHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_yellowHueLabel = new GridBagConstraints();
		gbc_yellowHueLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueLabel.gridx = 1;
		gbc_yellowHueLabel.gridy = 0;
		yellowThreshPanel.add(yellowHueLabel, gbc_yellowHueLabel);
		
		yellowHueMinSpinner = new JSpinner();
		yellowHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_yellowHueMinSpinner = new GridBagConstraints();
		gbc_yellowHueMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueMinSpinner.gridx = 2;
		gbc_yellowHueMinSpinner.gridy = 0;
		yellowThreshPanel.add(yellowHueMinSpinner, gbc_yellowHueMinSpinner);
		
		yellowHueMaxSpinner = new JSpinner();
		yellowHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_yellowHueMaxSpinner = new GridBagConstraints();
		gbc_yellowHueMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueMaxSpinner.gridx = 3;
		gbc_yellowHueMaxSpinner.gridy = 0;
		yellowThreshPanel.add(yellowHueMaxSpinner, gbc_yellowHueMaxSpinner);
		
		yellowSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_yellowSatLabel = new GridBagConstraints();
		gbc_yellowSatLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatLabel.gridx = 1;
		gbc_yellowSatLabel.gridy = 1;
		yellowThreshPanel.add(yellowSatLabel, gbc_yellowSatLabel);
		
		yellowSatMinSpinner = new JSpinner();
		yellowSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowSatMinSpinner = new GridBagConstraints();
		gbc_yellowSatMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatMinSpinner.gridx = 2;
		gbc_yellowSatMinSpinner.gridy = 1;
		yellowThreshPanel.add(yellowSatMinSpinner, gbc_yellowSatMinSpinner);
		
		yellowSatMaxSpinner = new JSpinner();
		yellowSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowSatMaxSpinner = new GridBagConstraints();
		gbc_yellowSatMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatMaxSpinner.gridx = 3;
		gbc_yellowSatMaxSpinner.gridy = 1;
		yellowThreshPanel.add(yellowSatMaxSpinner, gbc_yellowSatMaxSpinner);
		
		yellowValLabel = new JLabel("VAL");
		GridBagConstraints gbc_yellowValLabel = new GridBagConstraints();
		gbc_yellowValLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValLabel.gridx = 1;
		gbc_yellowValLabel.gridy = 2;
		yellowThreshPanel.add(yellowValLabel, gbc_yellowValLabel);
		
		yellowValMinSpinner = new JSpinner();
		yellowValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowValMinSpinner = new GridBagConstraints();
		gbc_yellowValMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValMinSpinner.gridx = 2;
		gbc_yellowValMinSpinner.gridy = 2;
		yellowThreshPanel.add(yellowValMinSpinner, gbc_yellowValMinSpinner);
		
		yellowValMaxSpinner = new JSpinner();
		yellowValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowValMaxSpinner = new GridBagConstraints();
		gbc_yellowValMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValMaxSpinner.gridx = 3;
		gbc_yellowValMaxSpinner.gridy = 2;
		yellowThreshPanel.add(yellowValMaxSpinner, gbc_yellowValMaxSpinner);
		
		yellowSizeLabel = new JLabel("SIZE");
		GridBagConstraints gbc_yellowSizeLabel = new GridBagConstraints();
		gbc_yellowSizeLabel.anchor = GridBagConstraints.EAST;
		gbc_yellowSizeLabel.insets = new Insets(0, 0, 0, 5);
		gbc_yellowSizeLabel.gridx = 1;
		gbc_yellowSizeLabel.gridy = 3;
		yellowThreshPanel.add(yellowSizeLabel, gbc_yellowSizeLabel);
		
		yellowSizeMinSpinner = new JSpinner();
		yellowSizeMinSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_yellowSizeMinSpinner = new GridBagConstraints();
		gbc_yellowSizeMinSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSizeMinSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_yellowSizeMinSpinner.gridx = 2;
		gbc_yellowSizeMinSpinner.gridy = 3;
		yellowThreshPanel.add(yellowSizeMinSpinner, gbc_yellowSizeMinSpinner);
		
		yellowSizeMaxSpinner = new JSpinner();
		yellowSizeMaxSpinner.setModel(new SpinnerNumberModel(new Integer(0), new Integer(0), null, new Integer(1)));
		GridBagConstraints gbc_yellowSizeMaxSpinner = new GridBagConstraints();
		gbc_yellowSizeMaxSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_yellowSizeMaxSpinner.insets = new Insets(0, 0, 0, 5);
		gbc_yellowSizeMaxSpinner.gridx = 3;
		gbc_yellowSizeMaxSpinner.gridy = 3;
		yellowThreshPanel.add(yellowSizeMaxSpinner, gbc_yellowSizeMaxSpinner);
		
		robotSettingPanel = new JPanel();
		robotControlTabbedPanel.addTab("Robot", null, robotSettingPanel, null);
		GridBagLayout gbl_robotSettingPanel = new GridBagLayout();
		gbl_robotSettingPanel.columnWidths = new int[]{0, 0, 0};
		gbl_robotSettingPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_robotSettingPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_robotSettingPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		robotSettingPanel.setLayout(gbl_robotSettingPanel);
		
		robotDebugModeCheckbox = new JCheckBox("Debug mode");
		GridBagConstraints gbc_robotDebugModeCheckbox = new GridBagConstraints();
		gbc_robotDebugModeCheckbox.anchor = GridBagConstraints.WEST;
		gbc_robotDebugModeCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_robotDebugModeCheckbox.gridx = 0;
		gbc_robotDebugModeCheckbox.gridy = 0;
		robotSettingPanel.add(robotDebugModeCheckbox, gbc_robotDebugModeCheckbox);
		
		robotColorLabel = new JLabel("Color");
		GridBagConstraints gbc_robotColorLabel = new GridBagConstraints();
		gbc_robotColorLabel.anchor = GridBagConstraints.WEST;
		gbc_robotColorLabel.insets = new Insets(0, 0, 5, 5);
		gbc_robotColorLabel.gridx = 0;
		gbc_robotColorLabel.gridy = 1;
		robotSettingPanel.add(robotColorLabel, gbc_robotColorLabel);
		
		robotColorBlueButton = new JRadioButton("Blue");
		robotColorBlueButton.setSelected(true);
		robotColorButtonGroup.add(robotColorBlueButton);
		GridBagConstraints gbc_robotColorBlueButton = new GridBagConstraints();
		gbc_robotColorBlueButton.anchor = GridBagConstraints.WEST;
		gbc_robotColorBlueButton.insets = new Insets(0, 0, 5, 5);
		gbc_robotColorBlueButton.gridx = 0;
		gbc_robotColorBlueButton.gridy = 2;
		robotSettingPanel.add(robotColorBlueButton, gbc_robotColorBlueButton);
		
		robotColorYellowButton = new JRadioButton("Yellow");
		robotColorButtonGroup.add(robotColorYellowButton);
		GridBagConstraints gbc_robotColorYellowButton = new GridBagConstraints();
		gbc_robotColorYellowButton.insets = new Insets(0, 0, 5, 5);
		gbc_robotColorYellowButton.anchor = GridBagConstraints.WEST;
		gbc_robotColorYellowButton.gridx = 0;
		gbc_robotColorYellowButton.gridy = 3;
		robotSettingPanel.add(robotColorYellowButton, gbc_robotColorYellowButton);
		
		robotGateLabel = new JLabel("Our gate");
		GridBagConstraints gbc_robotGateLabel = new GridBagConstraints();
		gbc_robotGateLabel.anchor = GridBagConstraints.WEST;
		gbc_robotGateLabel.insets = new Insets(0, 0, 5, 5);
		gbc_robotGateLabel.gridx = 0;
		gbc_robotGateLabel.gridy = 4;
		robotSettingPanel.add(robotGateLabel, gbc_robotGateLabel);
		
		robotGateLeftButton = new JRadioButton("Left");
		robotGateLeftButton.setSelected(true);
		robotGateButtonGroup.add(robotGateLeftButton);
		GridBagConstraints gbc_robotGateLeftButton = new GridBagConstraints();
		gbc_robotGateLeftButton.anchor = GridBagConstraints.WEST;
		gbc_robotGateLeftButton.insets = new Insets(0, 0, 5, 5);
		gbc_robotGateLeftButton.gridx = 0;
		gbc_robotGateLeftButton.gridy = 5;
		robotSettingPanel.add(robotGateLeftButton, gbc_robotGateLeftButton);
		
		robotGateRightButton = new JRadioButton("Right");
		robotGateButtonGroup.add(robotGateRightButton);
		GridBagConstraints gbc_robotGateRightButton = new GridBagConstraints();
		gbc_robotGateRightButton.insets = new Insets(0, 0, 5, 5);
		gbc_robotGateRightButton.anchor = GridBagConstraints.WEST;
		gbc_robotGateRightButton.gridx = 0;
		gbc_robotGateRightButton.gridy = 6;
		robotSettingPanel.add(robotGateRightButton, gbc_robotGateRightButton);
		
		robotConnectButton = new JButton("Connect");
		robotConnectButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!robotRunning) {
					connectToRobot();
				} else {
					disconnectFromRobot();
				}
			}
		});
		GridBagConstraints gbc_robotConnectButton = new GridBagConstraints();
		gbc_robotConnectButton.insets = new Insets(0, 0, 0, 5);
		gbc_robotConnectButton.gridx = 0;
		gbc_robotConnectButton.gridy = 7;
		robotSettingPanel.add(robotConnectButton, gbc_robotConnectButton);
	}
	
	
	private javax.swing.JLabel imageLabel;
	
	private JSpinner fieldLowYSpinner;	
	private JSpinner fieldLowXSpinner;	
	private JSpinner fieldHighXSpinner;	
	private JSpinner fieldHighYSpinner;
	
	private JTabbedPane robotControlTabbedPanel;
	
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
	
	private JPanel robotSettingPanel;
	private JRadioButton robotColorBlueButton;
	private JRadioButton robotColorYellowButton;
	private JLabel robotColorLabel;
	private JRadioButton robotGateLeftButton;
	private JRadioButton robotGateRightButton;
	private JLabel robotGateLabel;
	private JButton robotConnectButton;
	private final ButtonGroup robotColorButtonGroup = new ButtonGroup();
	private final ButtonGroup robotGateButtonGroup = new ButtonGroup();
	private JCheckBox robotDebugModeCheckbox;
	private JPanel visionSettingPanel;
	
	
	/**
	 * Set the camera image to display.
	 * 
	 * @param image New image.
	 */
	public void setImage(BufferedImage image) {
		if (image != null) {
			imageLabel.getGraphics().drawImage(image, 0, 0, null);
		}
	}
	
	
	/**
	 * Connect to our robot.
	 */
	private void connectToRobot() {
		Communicator com;
		if (robotDebugModeCheckbox.isSelected()) {
			com = null;
		} else {
			try {
				com = new JComm();
			} catch (IOException e) {
				LOGGER.warning("Connection with brick failed! Going into test mode.");
				com = null;
			}
		}
		
		aiInstance = new AIVisualServoing(com, vision);
		aiInstance.start(robotColorBlueButton.isSelected(), robotGateLeftButton.isSelected());
		aiInstance.setMode(mode.chase_ball);
		
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
		aiInstance.close();
		aiInstance = null;
		
		robotRunning = false;
		robotConnectButton.setText("Connect");
	}
	
	
	/** 
	 * Update the vision tab components to match vision's configuration.
	 */
	private void updateVisionComponentValues() {
		if (vision == null) {
			LOGGER.info("Tried to read vision configuration when vision subsystem was inactive.");
		} else {
			ImageProcessorConfig config = vision.getConfiguration();
			
			fieldLowXSpinner.setValue(new Integer((int) (config.getRawFieldLowX() * SPINNER_FLOAT_RANGE)));
			fieldLowYSpinner.setValue(new Integer((int) (config.getRawFieldLowY() * SPINNER_FLOAT_RANGE)));
			fieldHighXSpinner.setValue(new Integer((int) (config.getRawFieldHighX() * SPINNER_FLOAT_RANGE)));
			fieldHighYSpinner.setValue(new Integer((int) (config.getRawFieldHighY() * SPINNER_FLOAT_RANGE)));
			
			ballHueMinSpinner.setValue(new Integer(config.getBallHueMinValue()));
			ballSatMinSpinner.setValue(new Integer(config.getBallSatMinValue()));
			ballValMinSpinner.setValue(new Integer(config.getBallValMinValue()));
			ballSizeMinSpinner.setValue(new Integer(config.getBallSizeMinValue()));
			ballHueMaxSpinner.setValue(new Integer(config.getBallHueMaxValue()));
			ballSatMaxSpinner.setValue(new Integer(config.getBallSatMaxValue()));
			ballValMaxSpinner.setValue(new Integer(config.getBallValMaxValue()));
			ballSizeMaxSpinner.setValue(new Integer(config.getBallSizeMaxValue()));
			
			blueHueMinSpinner.setValue(new Integer(config.getBlueHueMinValue()));
			blueSatMinSpinner.setValue(new Integer(config.getBlueSatMinValue()));
			blueValMinSpinner.setValue(new Integer(config.getBlueValMinValue()));
			blueSizeMinSpinner.setValue(new Integer(config.getBlueSizeMinValue()));
			blueHueMaxSpinner.setValue(new Integer(config.getBlueHueMaxValue()));
			blueSatMaxSpinner.setValue(new Integer(config.getBlueSatMaxValue()));
			blueValMaxSpinner.setValue(new Integer(config.getBlueValMaxValue()));
			blueSizeMaxSpinner.setValue(new Integer(config.getBlueSizeMaxValue()));
			
			yellowHueMinSpinner.setValue(new Integer(config.getYellowHueMinValue()));
			yellowSatMinSpinner.setValue(new Integer(config.getYellowSatMinValue()));
			yellowValMinSpinner.setValue(new Integer(config.getYellowValMinValue()));
			yellowSizeMinSpinner.setValue(new Integer(config.getYellowSizeMinValue()));
			yellowHueMaxSpinner.setValue(new Integer(config.getYellowHueMaxValue()));
			yellowSatMaxSpinner.setValue(new Integer(config.getYellowSatMaxValue()));
			yellowValMaxSpinner.setValue(new Integer(config.getYellowValMaxValue()));
			yellowSizeMaxSpinner.setValue(new Integer(config.getYellowSizeMaxValue()));
		}
	}
	
	/**
	 * Set the configuration of the vision subsystem to match the values in
	 * vision tab.
	 */
	private void setNewVisionConfiguration() {
		if (vision == null) {
			LOGGER.info("Tried to set vision configuration when vision subsystem was inactive.");
		} else {
			ImageProcessorConfig config = new ImageProcessorConfig();
			
			config.setRawFieldLowX(((Integer)fieldLowXSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			config.setRawFieldLowY(((Integer)fieldLowYSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			config.setRawFieldHighX(((Integer)fieldHighXSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			config.setRawFieldHighY(((Integer)fieldHighYSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			
			config.setBallHueMinValue(((Integer)ballHueMinSpinner.getValue()).intValue());
			config.setBallSatMinValue(((Integer)ballSatMinSpinner.getValue()).intValue());
			config.setBallValMinValue(((Integer)ballValMinSpinner.getValue()).intValue());
			config.setBallSizeMinValue(((Integer)ballSizeMinSpinner.getValue()).intValue());
			config.setBallHueMaxValue(((Integer)ballHueMaxSpinner.getValue()).intValue());
			config.setBallSatMaxValue(((Integer)ballSatMaxSpinner.getValue()).intValue());
			config.setBallValMaxValue(((Integer)ballValMaxSpinner.getValue()).intValue());
			config.setBallSizeMaxValue(((Integer)ballSizeMaxSpinner.getValue()).intValue());
			
			config.setBlueHueMinValue(((Integer)blueHueMinSpinner.getValue()).intValue());
			config.setBlueSatMinValue(((Integer)blueSatMinSpinner.getValue()).intValue());
			config.setBlueValMinValue(((Integer)blueValMinSpinner.getValue()).intValue());
			config.setBlueSizeMinValue(((Integer)blueSizeMinSpinner.getValue()).intValue());
			config.setBlueHueMaxValue(((Integer)blueHueMaxSpinner.getValue()).intValue());
			config.setBlueSatMaxValue(((Integer)blueSatMaxSpinner.getValue()).intValue());
			config.setBlueValMaxValue(((Integer)blueValMaxSpinner.getValue()).intValue());
			config.setBlueSizeMaxValue(((Integer)blueSizeMaxSpinner.getValue()).intValue());
			
			config.setYellowHueMinValue(((Integer)yellowHueMinSpinner.getValue()).intValue());
			config.setYellowSatMinValue(((Integer)yellowSatMinSpinner.getValue()).intValue());
			config.setYellowValMinValue(((Integer)yellowValMinSpinner.getValue()).intValue());
			config.setYellowSizeMinValue(((Integer)yellowSizeMinSpinner.getValue()).intValue());
			config.setYellowHueMaxValue(((Integer)yellowHueMaxSpinner.getValue()).intValue());
			config.setYellowSatMaxValue(((Integer)yellowSatMaxSpinner.getValue()).intValue());
			config.setYellowValMaxValue(((Integer)yellowValMaxSpinner.getValue()).intValue());
			config.setYellowSizeMaxValue(((Integer)yellowSizeMaxSpinner.getValue()).intValue());
			
			vision.setConfiguration(config);
		}
	}

	
	/* (non-Javadoc)
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
				setNewVisionConfiguration();
			}
			
			System.out.println(String.format(
					"NEXT STATE: Ball at (%.4f, %.4f), Blue at (%.4f, %.4f, %.4f), Yellow at (%.4f, %.4f, %.4f).",
					state.getBallCoords().x, state.getBallCoords().y,
					state.getBlueRobot().getCoords().x, state.getBlueRobot().getCoords().y, 
					state.getBlueRobot().getAngle(), state.getYellowRobot().getCoords().x, 
					state.getYellowRobot().getCoords().y, state.getYellowRobot().getAngle()));
		}
	}
	
}
