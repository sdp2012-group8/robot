/**
 * 
 */
package sdp.gui;

import java.awt.image.BufferedImage;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldState;
import sdp.common.WorldStateCallback;
import sdp.vision.CameraInputProvider;
import sdp.vision.Vision;

/**
 * @author s0905195
 *
 */
public class OldGUITest implements Runnable, WorldStateCallback {
	
	/** The GUI object. */
	private OldGUI gui;
	
	/** The vision subsystem object. */
	private Vision vision;
	/** Visual input source. */
	CameraInputProvider input;
	
	
	/**
	 * The main constructor.
	 */
	public OldGUITest() {
		vision = new Vision();
		vision.setCallback(this);
		
		input = new CameraInputProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0);
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
