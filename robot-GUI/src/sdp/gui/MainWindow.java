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
		gridBagLayout.rowHeights = new int[]{480, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 4.9E-324};
		gridBagLayout.rowWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		getContentPane().setLayout(gridBagLayout);
		
		JPanel cameraImagePanel = new JPanel();
		GridBagConstraints gbc_cameraImagePanel = new GridBagConstraints();
		gbc_cameraImagePanel.fill = GridBagConstraints.BOTH;
		gbc_cameraImagePanel.insets = new Insets(0, 0, 5, 0);
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
		
		JPanel robotControlPanel = new JPanel();
		robotControlPanel.setAlignmentY(Component.TOP_ALIGNMENT);
		GridBagConstraints gbc_robotControlPanel = new GridBagConstraints();
		gbc_robotControlPanel.insets = new Insets(5, 5, 5, 5);
		gbc_robotControlPanel.fill = GridBagConstraints.BOTH;
		gbc_robotControlPanel.gridx = 1;
		gbc_robotControlPanel.gridy = 0;
		getContentPane().add(robotControlPanel, gbc_robotControlPanel);
		GridBagLayout gbl_robotControlPanel = new GridBagLayout();
		gbl_robotControlPanel.columnWidths = new int[]{200, 0};
		gbl_robotControlPanel.rowHeights = new int[]{15, 16, 25, 0};
		gbl_robotControlPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_robotControlPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		robotControlPanel.setLayout(gbl_robotControlPanel);
		
		JLabel sliderLabel = new JLabel("A label!");
		GridBagConstraints gbc_sliderLabel = new GridBagConstraints();
		gbc_sliderLabel.anchor = GridBagConstraints.NORTH;
		gbc_sliderLabel.insets = new Insets(0, 0, 5, 0);
		gbc_sliderLabel.gridx = 0;
		gbc_sliderLabel.gridy = 0;
		robotControlPanel.add(sliderLabel, gbc_sliderLabel);
		
		JSlider slider = new JSlider();
		slider.setMaximum(255);
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.anchor = GridBagConstraints.NORTH;
		gbc_slider.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider.insets = new Insets(0, 0, 5, 0);
		gbc_slider.gridx = 0;
		gbc_slider.gridy = 1;
		robotControlPanel.add(slider, gbc_slider);
		sliderLabel.setLabelFor(slider);
		
		JButton theButton = new JButton("Button!");
		GridBagConstraints gbc_theButton = new GridBagConstraints();
		gbc_theButton.anchor = GridBagConstraints.NORTHEAST;
		gbc_theButton.gridx = 0;
		gbc_theButton.gridy = 2;
		robotControlPanel.add(theButton, gbc_theButton);
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
