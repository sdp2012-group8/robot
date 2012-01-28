package sdp.gui;

import java.awt.image.BufferedImage;

import javax.swing.JLabel;

public class OldGUI extends javax.swing.JFrame{
	
	public static String className = null;

	public OldGUI(){
		initComponents();
	}
	
	private void initComponents(){
		
		
		onScreenImage = new javax.swing.JLabel();
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setName("Vision"); // NOI18N
		setResizable(false);

		onScreenImage.setText("Image goes here");
		
		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								layout
										.createSequentialGroup()
										.addComponent(
												onScreenImage,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												640,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										
										.addContainerGap()));
		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								layout
										.createSequentialGroup()
										.addGroup(
												layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
							
														.addComponent(
																onScreenImage,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																480,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												Short.MAX_VALUE)
										.addContainerGap()));

		pack();
		
		
		
	}
	
	private javax.swing.JLabel onScreenImage;
	
	public javax.swing.JLabel getImage() {
		return onScreenImage;
	}
	
	public void setImage(BufferedImage image) {
		if (image != null)
			onScreenImage.getGraphics().drawImage(image, 0, 0,
					image.getWidth(), image.getHeight(), null);
	}
	
}
