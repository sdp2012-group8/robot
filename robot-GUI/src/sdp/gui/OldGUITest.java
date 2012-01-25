/**
 * 
 */
package sdp.gui;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldState;
import sdp.common.WorldStateCallback;
import sdp.vision.CameraVisualProvider;
import sdp.vision.ImageVisualProvider;
import sdp.vision.Vision;
import sdp.vision.VisualProvider;

/**
 * This is a temporary class for carrying out testing of the old GUI interface.
 * 
 * @author Gediminas Liktaras
 */
public class OldGUITest implements Runnable, WorldStateCallback {
	
	/** The GUI object. */
	private OldGUI gui;
	
	/** The vision subsystem object. */
	private Vision vision;
	/** Visual input source. */
	VisualProvider input;
	
	
	/**
	 * The main constructor.
	 */
	public OldGUITest() {
		vision = new Vision();
		vision.setCallback(this);
		
		//input = new CameraVisualProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0);
		String filenames[] = { "../robot-VISION/data/testImages/pitch2-1.png",
				               "../robot-VISION/data/testImages/pitch2-2.png",
				               "../robot-VISION/data/testImages/pitch2-3.png" };
		input = new ImageVisualProvider(filenames, 25);		
		input.setCallback(vision);
		
		gui = new OldGUI();
		gui.setVisible(true);
	}


	/**
	 * This method is called when the next world state becomes available.
	 * 
	 * @param state The next world state.
	 */
	@Override
	public void nextWorldState(WorldState state, BufferedImage frame) {
		gui.setImage(frame);
	}
	
	
	/**
	 * The thread's run method.
	 */
	@Override
	public void run() {
		input.startCapture();
	}
	
	
	/**
	 * The main method.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		OldGUITest app = new OldGUITest();
		(new Thread(app)).start();
	}

}
