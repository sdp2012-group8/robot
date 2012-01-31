package sdp.gui;

import java.awt.image.BufferedImage;

import java.awt.Component;
import javax.swing.JPanel;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.border.TitledBorder;
import javax.swing.SpinnerDateModel;
import java.util.Date;
import java.util.Calendar;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTabbedPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


/**
 * The GUI class of the main window.
 */
public class MainWindow extends javax.swing.JFrame{
	
	/** Required by Serializable. */
	private static final long serialVersionUID = 8597348579639499324L;
	public static String className = null;

	
	/**
	 * The main constructor.
	 */
	public MainWindow(){
		setSize(new Dimension(840, 480));
		setPreferredSize(new Dimension(840, 480));
		getContentPane().setMinimumSize(new Dimension(640, 480));
		setTitle("Battlestation");
		initComponents();
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
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.fill = GridBagConstraints.BOTH;
		gbc_tabbedPane.gridx = 1;
		gbc_tabbedPane.gridy = 0;
		getContentPane().add(tabbedPane, gbc_tabbedPane);
		
		JPanel robotControlPanel = new JPanel();
		tabbedPane.addTab("Vision", null, robotControlPanel, null);
		tabbedPane.setEnabledAt(0, true);
		robotControlPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		GridBagLayout gbl_robotControlPanel = new GridBagLayout();
		gbl_robotControlPanel.columnWidths = new int[]{200, 0};
		gbl_robotControlPanel.rowHeights = new int[]{15, 0, 0};
		gbl_robotControlPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_robotControlPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		robotControlPanel.setLayout(gbl_robotControlPanel);
		
		JPanel fieldWallPanel = new JPanel();
		fieldWallPanel.setBorder(new TitledBorder(null, "Field borders", TitledBorder.CENTER, TitledBorder.TOP, null, null));
		GridBagConstraints gbc_fieldWallPanel = new GridBagConstraints();
		gbc_fieldWallPanel.insets = new Insets(0, 0, 5, 0);
		gbc_fieldWallPanel.anchor = GridBagConstraints.NORTH;
		gbc_fieldWallPanel.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldWallPanel.gridx = 0;
		gbc_fieldWallPanel.gridy = 0;
		robotControlPanel.add(fieldWallPanel, gbc_fieldWallPanel);
		GridBagLayout gbl_fieldWallPanel = new GridBagLayout();
		gbl_fieldWallPanel.columnWidths = new int[]{0, 50, 60, 50, 0, 0};
		gbl_fieldWallPanel.rowHeights = new int[]{20, 20, 0, 0};
		gbl_fieldWallPanel.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_fieldWallPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		fieldWallPanel.setLayout(gbl_fieldWallPanel);
		
		JSpinner fieldLowYSpinner = new JSpinner();
		fieldLowYSpinner.setMinimumSize(new Dimension(55, 20));
		fieldLowYSpinner.setModel(new SpinnerNumberModel(0, 0, 1000, 1));
		GridBagConstraints gbc_fieldLowYSpinner = new GridBagConstraints();
		gbc_fieldLowYSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldLowYSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldLowYSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldLowYSpinner.gridx = 2;
		gbc_fieldLowYSpinner.gridy = 0;
		fieldWallPanel.add(fieldLowYSpinner, gbc_fieldLowYSpinner);
		
		JSpinner fieldLowXSpinner = new JSpinner();
		fieldLowXSpinner.setModel(new SpinnerNumberModel(0, 0, 1000, 1));
		fieldLowXSpinner.setMinimumSize(new Dimension(55, 20));
		GridBagConstraints gbc_fieldLowXSpinner = new GridBagConstraints();
		gbc_fieldLowXSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldLowXSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldLowXSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldLowXSpinner.gridx = 1;
		gbc_fieldLowXSpinner.gridy = 1;
		fieldWallPanel.add(fieldLowXSpinner, gbc_fieldLowXSpinner);
		
		JSpinner fieldHighXSpinner = new JSpinner();
		fieldHighXSpinner.setMinimumSize(new Dimension(55, 20));
		fieldHighXSpinner.setModel(new SpinnerNumberModel(0, 0, 1000, 1));
		GridBagConstraints gbc_fieldHighXSpinner = new GridBagConstraints();
		gbc_fieldHighXSpinner.fill = GridBagConstraints.HORIZONTAL;
		gbc_fieldHighXSpinner.anchor = GridBagConstraints.NORTH;
		gbc_fieldHighXSpinner.insets = new Insets(0, 0, 2, 2);
		gbc_fieldHighXSpinner.gridx = 3;
		gbc_fieldHighXSpinner.gridy = 1;
		fieldWallPanel.add(fieldHighXSpinner, gbc_fieldHighXSpinner);
		
		JSpinner fieldHighYSpinner = new JSpinner();
		fieldHighYSpinner.setModel(new SpinnerNumberModel(0, 0, 1000, 1));
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
	
}
