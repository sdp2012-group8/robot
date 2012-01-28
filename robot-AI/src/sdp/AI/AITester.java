package sdp.AI;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldStateObserver;
import sdp.communicator.JComm;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.Vision;
import sdp.vision.VisualInputProvider;

/**
 * Runs the AI instead of using GUI.
 * 
 * @author Martin Marinov
 *
 */
public class AITester {
	
	/**
	 * Start tester
	 * @param args
	 */
	public static void main(String[] args) {
		Vision vision = new Vision();
		VisualInputProvider input = new CameraVisualInputProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0);
		input.setCallback(vision);
		AI ai = new AI(new JComm(null), new WorldStateObserver(vision));
		input.startCapture();
		// we are blue team, heading for left door
		ai.start(true, true);
	}

}
