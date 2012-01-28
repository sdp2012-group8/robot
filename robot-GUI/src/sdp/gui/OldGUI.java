package sdp.gui;

import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.SwingConstants;

/**
 * The GUI class of the main window.
 */
public class OldGUI extends javax.swing.JFrame{
	
	/** Required by Serializable. */
	private static final long serialVersionUID = 8597348579639499324L;
	public static String className = null;

	public OldGUI(){
		setTitle("Battlestation");
		initComponents();
	}
	
	private void initComponents() {
		imageLabel = new javax.swing.JLabel();
		imageLabel.setAlignmentY(Component.TOP_ALIGNMENT);
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setName("Vision");
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

		imageLabel.setText("Image goes here");
		getContentPane().add(imageLabel);
	}
	
	private javax.swing.JLabel imageLabel;
	
	public javax.swing.JLabel getImage() {
		return imageLabel;
	}
	
	public void setImage(BufferedImage image) {
		if (image != null) {
			imageLabel.setSize(image.getWidth(), image.getHeight());
			imageLabel.getGraphics().drawImage(image, 0, 0, null);
		}
	}
	
}
