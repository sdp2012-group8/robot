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

import sdp.common.FPSCounter;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.vision.ImageProcessorConfiguration;
import sdp.vision.Vision;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.border.LineBorder;
import java.awt.Color;


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
	
	/** Active vision subsystem instance. */
	private Vision vision = null;	
	/** A flag that controls whether vision system calibration is enabled. */
	private boolean visionChangesEnabled;
	
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
	public MainWindow(WorldStateObserver worldStateObserver, Vision vision) {
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
			int visionIdx = robotControlTabbedPanel.indexOfTab("Vision");
			robotControlTabbedPanel.setEnabledAt(visionIdx, false);
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
		
		JPanel visionSettingPanel = new JPanel();
		robotControlTabbedPanel.addTab("Vision", null, visionSettingPanel, null);
		robotControlTabbedPanel.setEnabledAt(0, true);
		visionSettingPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		GridBagLayout gbl_visionSettingPanel = new GridBagLayout();
		gbl_visionSettingPanel.columnWidths = new int[]{200, 200, 0};
		gbl_visionSettingPanel.rowHeights = new int[]{15, 0, 0, 0};
		gbl_visionSettingPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_visionSettingPanel.rowWeights = new double[]{0.0, 0.0, 1.0, Double.MIN_VALUE};
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
		ballThreshPanel.setBorder(new TitledBorder(null, "Ball thresholds", TitledBorder.CENTER, TitledBorder.TOP, null, null));
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
		gbc_ballHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueLabel.gridx = 1;
		gbc_ballHueLabel.gridy = 1;
		ballThreshPanel.add(ballHueLabel, gbc_ballHueLabel);
		
		ballSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_ballSatLabel = new GridBagConstraints();
		gbc_ballSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatLabel.gridx = 2;
		gbc_ballSatLabel.gridy = 1;
		ballThreshPanel.add(ballSatLabel, gbc_ballSatLabel);
		
		ballValLabel = new JLabel("VAL");
		GridBagConstraints gbc_ballValLabel = new GridBagConstraints();
		gbc_ballValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_ballValLabel.gridx = 3;
		gbc_ballValLabel.gridy = 1;
		ballThreshPanel.add(ballValLabel, gbc_ballValLabel);
		
		ballHueMinSpinner = new JSpinner();
		ballHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_ballHueMinSpinner = new GridBagConstraints();
		gbc_ballHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueMinSpinner.gridx = 1;
		gbc_ballHueMinSpinner.gridy = 2;
		ballThreshPanel.add(ballHueMinSpinner, gbc_ballHueMinSpinner);
		
		ballSatMinSpinner = new JSpinner();
		ballSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballSatMinSpinner = new GridBagConstraints();
		gbc_ballSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatMinSpinner.gridx = 2;
		gbc_ballSatMinSpinner.gridy = 2;
		ballThreshPanel.add(ballSatMinSpinner, gbc_ballSatMinSpinner);
		
		ballValMinSpinner = new JSpinner();
		ballValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballValMinSpinner = new GridBagConstraints();
		gbc_ballValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballValMinSpinner.gridx = 3;
		gbc_ballValMinSpinner.gridy = 2;
		ballThreshPanel.add(ballValMinSpinner, gbc_ballValMinSpinner);
		
		ballHueMaxSpinner = new JSpinner();
		ballHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_ballHueMaxSpinner = new GridBagConstraints();
		gbc_ballHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballHueMaxSpinner.gridx = 1;
		gbc_ballHueMaxSpinner.gridy = 3;
		ballThreshPanel.add(ballHueMaxSpinner, gbc_ballHueMaxSpinner);
		
		ballSatMaxSpinner = new JSpinner();
		ballSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballSatMaxSpinner = new GridBagConstraints();
		gbc_ballSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballSatMaxSpinner.gridx = 2;
		gbc_ballSatMaxSpinner.gridy = 3;
		ballThreshPanel.add(ballSatMaxSpinner, gbc_ballSatMaxSpinner);
		
		ballValMaxSpinner = new JSpinner();
		ballValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_ballValMaxSpinner = new GridBagConstraints();
		gbc_ballValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_ballValMaxSpinner.gridx = 3;
		gbc_ballValMaxSpinner.gridy = 3;
		ballThreshPanel.add(ballValMaxSpinner, gbc_ballValMaxSpinner);
		
		ballThreshCheckbox = new JCheckBox("Show threshold");
		GridBagConstraints gbc_ballThreshCheckbox = new GridBagConstraints();
		gbc_ballThreshCheckbox.anchor = GridBagConstraints.WEST;
		gbc_ballThreshCheckbox.gridwidth = 3;
		gbc_ballThreshCheckbox.insets = new Insets(0, 0, 5, 5);
		gbc_ballThreshCheckbox.gridx = 1;
		gbc_ballThreshCheckbox.gridy = 4;
		ballThreshPanel.add(ballThreshCheckbox, gbc_ballThreshCheckbox);
		
		blueThreshPanel = new JPanel();
		blueThreshPanel.setBorder(new TitledBorder(null, "Blue T tresholds", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_blueThreshPanel = new GridBagConstraints();
		gbc_blueThreshPanel.insets = new Insets(0, 0, 5, 5);
		gbc_blueThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_blueThreshPanel.gridx = 0;
		gbc_blueThreshPanel.gridy = 1;
		visionSettingPanel.add(blueThreshPanel, gbc_blueThreshPanel);
		GridBagLayout gbl_blueThreshPanel = new GridBagLayout();
		gbl_blueThreshPanel.columnWidths = new int[]{0, 28, 28, 44, 0, 0};
		gbl_blueThreshPanel.rowHeights = new int[]{17, 20, 20, 25, 0};
		gbl_blueThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_blueThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		blueThreshPanel.setLayout(gbl_blueThreshPanel);
		
		blueHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_blueHueLabel = new GridBagConstraints();
		gbc_blueHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueLabel.gridx = 1;
		gbc_blueHueLabel.gridy = 0;
		blueThreshPanel.add(blueHueLabel, gbc_blueHueLabel);
		
		blueSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_blueSatLabel = new GridBagConstraints();
		gbc_blueSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatLabel.gridx = 2;
		gbc_blueSatLabel.gridy = 0;
		blueThreshPanel.add(blueSatLabel, gbc_blueSatLabel);
		
		blueValLabel = new JLabel("VAL");
		GridBagConstraints gbc_blueValLabel = new GridBagConstraints();
		gbc_blueValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_blueValLabel.gridx = 3;
		gbc_blueValLabel.gridy = 0;
		blueThreshPanel.add(blueValLabel, gbc_blueValLabel);
		
		blueHueMinSpinner = new JSpinner();
		blueHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_blueHueMinSpinner = new GridBagConstraints();
		gbc_blueHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueMinSpinner.gridx = 1;
		gbc_blueHueMinSpinner.gridy = 1;
		blueThreshPanel.add(blueHueMinSpinner, gbc_blueHueMinSpinner);
		
		blueSatMinSpinner = new JSpinner();
		blueSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueSatMinSpinner = new GridBagConstraints();
		gbc_blueSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatMinSpinner.gridx = 2;
		gbc_blueSatMinSpinner.gridy = 1;
		blueThreshPanel.add(blueSatMinSpinner, gbc_blueSatMinSpinner);
		
		blueValMinSpinner = new JSpinner();
		blueValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueValMinSpinner = new GridBagConstraints();
		gbc_blueValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueValMinSpinner.gridx = 3;
		gbc_blueValMinSpinner.gridy = 1;
		blueThreshPanel.add(blueValMinSpinner, gbc_blueValMinSpinner);
		
		blueHueMaxSpinner = new JSpinner();
		blueHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_blueHueMaxSpinner = new GridBagConstraints();
		gbc_blueHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueHueMaxSpinner.gridx = 1;
		gbc_blueHueMaxSpinner.gridy = 2;
		blueThreshPanel.add(blueHueMaxSpinner, gbc_blueHueMaxSpinner);
		
		blueSatMaxSpinner = new JSpinner();
		blueSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueSatMaxSpinner = new GridBagConstraints();
		gbc_blueSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueSatMaxSpinner.gridx = 2;
		gbc_blueSatMaxSpinner.gridy = 2;
		blueThreshPanel.add(blueSatMaxSpinner, gbc_blueSatMaxSpinner);
		
		blueValMaxSpinner = new JSpinner();
		blueValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_blueValMaxSpinner = new GridBagConstraints();
		gbc_blueValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_blueValMaxSpinner.gridx = 3;
		gbc_blueValMaxSpinner.gridy = 2;
		blueThreshPanel.add(blueValMaxSpinner, gbc_blueValMaxSpinner);
		
		blueThreshCheckbox = new JCheckBox("Show threshold");
		GridBagConstraints gbc_blueThreshCheckbox = new GridBagConstraints();
		gbc_blueThreshCheckbox.insets = new Insets(0, 0, 0, 5);
		gbc_blueThreshCheckbox.anchor = GridBagConstraints.WEST;
		gbc_blueThreshCheckbox.gridwidth = 3;
		gbc_blueThreshCheckbox.gridx = 1;
		gbc_blueThreshCheckbox.gridy = 3;
		blueThreshPanel.add(blueThreshCheckbox, gbc_blueThreshCheckbox);
		
		yellowThreshPanel = new JPanel();
		yellowThreshPanel.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Yellow T thresholds", TitledBorder.CENTER, TitledBorder.TOP, null, new Color(51, 51, 51)));
		GridBagConstraints gbc_yellowThreshPanel = new GridBagConstraints();
		gbc_yellowThreshPanel.insets = new Insets(0, 0, 5, 0);
		gbc_yellowThreshPanel.fill = GridBagConstraints.BOTH;
		gbc_yellowThreshPanel.gridx = 1;
		gbc_yellowThreshPanel.gridy = 1;
		visionSettingPanel.add(yellowThreshPanel, gbc_yellowThreshPanel);
		GridBagLayout gbl_yellowThreshPanel = new GridBagLayout();
		gbl_yellowThreshPanel.columnWidths = new int[]{0, 28, 28, 44, 0, 0};
		gbl_yellowThreshPanel.rowHeights = new int[]{17, 20, 20, 25, 0};
		gbl_yellowThreshPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_yellowThreshPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		yellowThreshPanel.setLayout(gbl_yellowThreshPanel);
		
		yellowHueLabel = new JLabel("HUE");
		GridBagConstraints gbc_yellowHueLabel = new GridBagConstraints();
		gbc_yellowHueLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueLabel.gridx = 1;
		gbc_yellowHueLabel.gridy = 0;
		yellowThreshPanel.add(yellowHueLabel, gbc_yellowHueLabel);
		
		yellowSatLabel = new JLabel("SAT");
		GridBagConstraints gbc_yellowSatLabel = new GridBagConstraints();
		gbc_yellowSatLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatLabel.gridx = 2;
		gbc_yellowSatLabel.gridy = 0;
		yellowThreshPanel.add(yellowSatLabel, gbc_yellowSatLabel);
		
		yellowValLabel = new JLabel("VAL");
		GridBagConstraints gbc_yellowValLabel = new GridBagConstraints();
		gbc_yellowValLabel.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValLabel.gridx = 3;
		gbc_yellowValLabel.gridy = 0;
		yellowThreshPanel.add(yellowValLabel, gbc_yellowValLabel);
		
		yellowHueMinSpinner = new JSpinner();
		yellowHueMinSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_yellowHueMinSpinner = new GridBagConstraints();
		gbc_yellowHueMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueMinSpinner.gridx = 1;
		gbc_yellowHueMinSpinner.gridy = 1;
		yellowThreshPanel.add(yellowHueMinSpinner, gbc_yellowHueMinSpinner);
		
		yellowSatMinSpinner = new JSpinner();
		yellowSatMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowSatMinSpinner = new GridBagConstraints();
		gbc_yellowSatMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatMinSpinner.gridx = 2;
		gbc_yellowSatMinSpinner.gridy = 1;
		yellowThreshPanel.add(yellowSatMinSpinner, gbc_yellowSatMinSpinner);
		
		yellowValMinSpinner = new JSpinner();
		yellowValMinSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowValMinSpinner = new GridBagConstraints();
		gbc_yellowValMinSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValMinSpinner.gridx = 3;
		gbc_yellowValMinSpinner.gridy = 1;
		yellowThreshPanel.add(yellowValMinSpinner, gbc_yellowValMinSpinner);
		
		yellowHueMaxSpinner = new JSpinner();
		yellowHueMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 360, 1));
		GridBagConstraints gbc_yellowHueMaxSpinner = new GridBagConstraints();
		gbc_yellowHueMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowHueMaxSpinner.gridx = 1;
		gbc_yellowHueMaxSpinner.gridy = 2;
		yellowThreshPanel.add(yellowHueMaxSpinner, gbc_yellowHueMaxSpinner);
		
		yellowSatMaxSpinner = new JSpinner();
		yellowSatMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowSatMaxSpinner = new GridBagConstraints();
		gbc_yellowSatMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowSatMaxSpinner.gridx = 2;
		gbc_yellowSatMaxSpinner.gridy = 2;
		yellowThreshPanel.add(yellowSatMaxSpinner, gbc_yellowSatMaxSpinner);
		
		yellowValMaxSpinner = new JSpinner();
		yellowValMaxSpinner.setModel(new SpinnerNumberModel(0, 0, 100, 1));
		GridBagConstraints gbc_yellowValMaxSpinner = new GridBagConstraints();
		gbc_yellowValMaxSpinner.insets = new Insets(0, 0, 5, 5);
		gbc_yellowValMaxSpinner.gridx = 3;
		gbc_yellowValMaxSpinner.gridy = 2;
		yellowThreshPanel.add(yellowValMaxSpinner, gbc_yellowValMaxSpinner);
		
		yellowThreshCheckbox = new JCheckBox("Show threshold");
		GridBagConstraints gbc_yellowThreshCheckbox = new GridBagConstraints();
		gbc_yellowThreshCheckbox.anchor = GridBagConstraints.WEST;
		gbc_yellowThreshCheckbox.gridwidth = 3;
		gbc_yellowThreshCheckbox.insets = new Insets(0, 0, 0, 5);
		gbc_yellowThreshCheckbox.gridx = 1;
		gbc_yellowThreshCheckbox.gridy = 3;
		yellowThreshPanel.add(yellowThreshCheckbox, gbc_yellowThreshCheckbox);
	}
	
	
	/** The label that will contain the camera's image. */
	private javax.swing.JLabel imageLabel;
	
	/** Spinner that contains the field's low Y value. */
	private JSpinner fieldLowYSpinner;
	
	/** Spinner that contains the field's low X value. */
	private JSpinner fieldLowXSpinner;
	
	/** Spinner that contains the field's high X value. */
	private JSpinner fieldHighXSpinner;
	
	/** Spinner that contains the field's high Y value. */
	private JSpinner fieldHighYSpinner;
	
	/** Tabbed pane that contains robot's controls. */
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
	private JCheckBox ballThreshCheckbox;
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
	private JCheckBox blueThreshCheckbox;
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
	private JCheckBox yellowThreshCheckbox;
	
	
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
	 * Update the vision tab components to match vision's configuration.
	 */
	private void updateVisionComponentValues() {
		if (vision == null) {
			LOGGER.info("Tried to read vision configuration when vision subsystem was inactive.");
		} else {
			ImageProcessorConfiguration config = vision.getConfiguration();		
			
			fieldLowXSpinner.setValue(new Integer((int) (config.getRawFieldLowX() * SPINNER_FLOAT_RANGE)));
			fieldLowYSpinner.setValue(new Integer((int) (config.getRawFieldLowY() * SPINNER_FLOAT_RANGE)));
			fieldHighXSpinner.setValue(new Integer((int) (config.getRawFieldHighX() * SPINNER_FLOAT_RANGE)));
			fieldHighYSpinner.setValue(new Integer((int) (config.getRawFieldHighY() * SPINNER_FLOAT_RANGE)));
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
			ImageProcessorConfiguration config = new ImageProcessorConfiguration();
			
			config.setRawFieldLowX(((Integer)fieldLowXSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			config.setRawFieldLowY(((Integer)fieldLowYSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			config.setRawFieldHighX(((Integer)fieldHighXSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			config.setRawFieldHighY(((Integer)fieldHighYSpinner.getValue()).intValue() / ((double) SPINNER_FLOAT_RANGE));
			
			vision.setConfiguration(config);
		}
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		setVisible(true);
				
		while (!Thread.interrupted()) {
			WorldState state = worldStateObserver.getNextState();
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
