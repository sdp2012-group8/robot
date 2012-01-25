package sdp.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import sdp.vision.OldImageProcessor;


/*
 * Vision test class. Works without a cam feed or v4l4j (but needs test images).
 */

public class ViewerTest implements Runnable {

        private boolean stopCapturingVideo;
		private int pitch = 2;
		private int timeout = 1000;
        
    public ViewerTest() {
        stopCapturingVideo = false;
        Thread simThread = new Thread(this, "Simulation Thread");
        simThread.run();
    }

    public static void main(String[] args) throws IOException {

        new ViewerTest();
    }
    public void run() {
    	OldImageProcessor ip = new OldImageProcessor();
    	OldGUI gui = new OldGUI();
        gui.setVisible(true);
		BufferedImage image1 = null, image2 = null, image3 = null;
		int count=0;
		try {
			if(pitch==2) {
				image1 = ImageIO.read(new File("data/testImages/pitch2-1.png"));
				image2 = ImageIO.read(new File("data/testImages/pitch2-2.png"));
				image3 = ImageIO.read(new File("data/testImages/pitch2-3.png")); }
			else {
				image1 = ImageIO.read(new File("data/testImages/pitch1-1.png"));
				image2 = ImageIO.read(new File("data/testImages/pitch1-2.png"));
				image3 = ImageIO.read(new File("data/testImages/pitch1-3.png"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	while(stopCapturingVideo==false) {
    		if(count%3==0) {
    			BufferedImage iq = ip.process(image1);
            	gui.setImage(iq);
            	try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		else if(count%3==1) {
    			BufferedImage iq = ip.process(image2);
    			gui.setImage(iq);
            	try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		else {
    			BufferedImage iq = ip.process(image3);
    			gui.setImage(iq);
            	try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		if (count < 9) count++; 
    		else count=0;
    	}
    	
    }
}