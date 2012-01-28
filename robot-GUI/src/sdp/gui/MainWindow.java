package sdp.gui;

import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import java.awt.Component;


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
		setTitle("Battlestation");
		initComponents();
	}
	
	/**
	 * Initialise GUI components.
	 */
	private void initComponents() {
		imageLabel = new javax.swing.JLabel();
		imageLabel.setAlignmentY(Component.TOP_ALIGNMENT);
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setName("Vision");
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

		imageLabel.setText("Image goes here");
		getContentPane().add(imageLabel);
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
			imageLabel.setSize(image.getWidth(), image.getHeight());
			imageLabel.getGraphics().drawImage(image, 0, 0, null);
		}
	}
	
}
