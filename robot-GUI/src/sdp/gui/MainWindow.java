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
		
		setSize(new Dimension(840, 500));
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
		gbl_cameraImagePanel.rowHeights = new int[]{480, 0};
		gbl_cameraImagePanel.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_cameraImagePanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		cameraImagePanel.setLayout(gbl_cameraImagePanel);
		imageLabel = new javax.swing.JLabel();
		GridBagConstraints gbc_imageLabel = new GridBagConstraints();
		gbc_imageLabel.fill = GridBagConstraints.BOTH;
		gbc_imageLabel.gridx = 0;
		gbc_imageLabel.gridy = 0;
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
		gbl_visionSettingPanel.columnWidths = new int[]{200, 0};
		gbl_visionSettingPanel.rowHeights = new int[]{15, 0};
		gbl_visionSettingPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_visionSettingPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		visionSettingPanel.setLayout(gbl_visionSettingPanel);
		
		JPanel fieldWallPanel = new JPanel();
		fieldWallPanel.setBorder(new TitledBorder(null, "Field borders", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_fieldWallPanel = new GridBagConstraints();
		gbc_fieldWallPanel.anchor = GridBagConstraints.NORTH;
		gbc_fieldWallPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldWallPanel.gridx = 0;
		gbc_fieldWallPanel.gridy = 0;
		visionSettingPanel.add(fieldWallPanel, gbc_fieldWallPanel);
		GridBagLayout gbl_fieldWallPanel = new GridBagLayout();
		gbl_fieldWallPanel.columnWidths = new int[]{0, 50, 60, 50, 0, 0};
		gbl_fieldWallPanel.rowHeights = new int[]{20, 20, 0, 0};
		gbl_fieldWallPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_fieldWallPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		fieldWallPanel.setLayout(gbl_fieldWallPanel);
		
		fieldLowYSpinner = new JSpinner();
		fieldLowYSpinner.setMinimumSize(new Dimension(55, 20));
		fieldLowYSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		GridBagConstraints gbc_fieldLowYSpinner = new GridBagConstraints();
		gbc_fieldLowYSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldLowYSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldLowYSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldLowYSpinner.gridx = 2;
		gbc_fieldLowYSpinner.gridy = 0;
		fieldWallPanel.add(fieldLowYSpinner, gbc_fieldLowYSpinner);
		
		fieldLowXSpinner = new JSpinner();
		fieldLowXSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		fieldLowXSpinner.setMinimumSize(new Dimension(55, 20));
		GridBagConstraints gbc_fieldLowXSpinner = new GridBagConstraints();
		gbc_fieldLowXSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldLowXSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldLowXSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldLowXSpinner.gridx = 1;
		gbc_fieldLowXSpinner.gridy = 1;
		fieldWallPanel.add(fieldLowXSpinner, gbc_fieldLowXSpinner);
		
		fieldHighXSpinner = new JSpinner();
		fieldHighXSpinner.setMinimumSize(new Dimension(55, 20));
		fieldHighXSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		GridBagConstraints gbc_fieldHighXSpinner = new GridBagConstraints();
		gbc_fieldHighXSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldHighXSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldHighXSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldHighXSpinner.gridx = 3;
		gbc_fieldHighXSpinner.gridy = 1;
		fieldWallPanel.add(fieldHighXSpinner, gbc_fieldHighXSpinner);
		
		fieldHighYSpinner = new JSpinner();
		fieldHighYSpinner.setModel(new SpinnerNumberModel(0, 0, SPINNER_FLOAT_RANGE, 1));
		fieldHighYSpinner.setMinimumSize(new Dimension(55, 20));
		GridBagConstraints gbc_fieldHighYSpinner = new GridBagConstraints();
		gbc_fieldHighYSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldHighYSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldHighYSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldHighYSpinner.gridx = 2;
		gbc_fieldHighYSpinner.gridy = 2;
		fieldWallPanel.add(fieldHighYSpinner, gbc_fieldHighYSpinner);
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
			if (fpsCounter.getTickCount() % 5 == 0) {
				setTitle(String.format("%s - %.1f FPS", WINDOW_TITLE, fpsCounter.getFPS()));
				if (visionChangesEnabled) {
					setNewVisionConfiguration();
				}
			}
			
			System.out.println("NEW STATE: " +
					"Ball at (" + state.getBallCoords().x + ", " + state.getBallCoords().y + "), " +
					"Blue at (" + state.getBlueRobot().getCoords().x +
						", " + state.getBlueRobot().getCoords().y +
						", " + state.getBlueRobot().getAngle() + ") " +
					"Yellow at (" + state.getYellowRobot().getCoords().x +
						", " + state.getYellowRobot().getCoords().y +
						", " + state.getYellowRobot().getAngle() + ").");
		}
	}
	
}
