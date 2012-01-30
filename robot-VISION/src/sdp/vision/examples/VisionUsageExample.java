package sdp.vision.examples;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.ImageVisualInputProvider;
import sdp.vision.Vision;
import sdp.vision.VisualInputProvider;


/**
 * This class is an example of how to connect to the vision system and
 * receive world state updates from it in asynchronous fashion.
 * 
 * As you can see, the whole setup takes X steps:
 * 
 * 1) Create a Vision instance.
 * 2) Create a VisualInputProvider instance.
 * 3) Set visual input provider's callback.
 * 4) Create a WorldStateObserver.
 * 5) Start visual input capture.
 * 6) Use WorldStateObserver object to access the most recent world state.
 * 
 * Note that a WorldStateObserver objects are asynchronous: they will return
 * only the most recent frame and will block if frames are consumed too fast.
 * It is also the only thing that other parts of the system need to be aware of
 * in order to interface with the vision system.
 * 
 * @author Gediminas Liktaras
 */
public class VisionUsageExample implements Runnable {
	
	/** The vision subsystem object. */
	private Vision vision;
	/** Visual input source. */
	private VisualInputProvider input;
	/** Vision sybsystem observer. */
	private WorldStateObserver visionObserver;
	
	/** Whether to use camera or offline inputs. */
	private static final boolean USE_CAMERA = false;
	
	
	/**
	 * The main constructor.
	 */
	public VisionUsageExample() {
		// Create the new vision world state provider.
		vision = new Vision();
		
		// Create visual input provider.
		if (USE_CAMERA) {		
			input = new CameraVisualInputProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0);
		} else {
			String filenames[] = { "data/testImages/pitch2-1.png",
					               "data/testImages/pitch2-2.png",
					               "data/testImages/pitch2-3.png" };
			input = new ImageVisualInputProvider(filenames, 25);
		}
		
		// Set visual input provider callback.
		// Do NOT set the vision object as a callback for multiple visual
		// input providers.
		input.setCallback(vision);

		// Create the world state observer, connect it to the world state provider.
		visionObserver = new WorldStateObserver(vision);
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// Start the vision thread.
		input.startCapture();
		
		while (!Thread.interrupted()) {
			// Get the next world state. 
			WorldState state = visionObserver.getNextState();
			
			// Do something with the world state.
			System.out.println("NEW STATE: " +
					"Ball at (" + state.getBallCoords().x +
						", " + state.getBallCoords().y + "), " +
					"Blue at (" + state.getBlueRobot().getCoords().x +
						", " + state.getBlueRobot().getCoords().y +
						", " + state.getBlueRobot().getAngle() + "), " +
					"Yellow at (" + state.getYellowRobot().getCoords().x +
						", " + state.getYellowRobot().getCoords().y +
						", " + state.getYellowRobot().getAngle() + ").");
		}
	}
	
	
	/**
	 * The main method.
	 * 
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		VisionUsageExample example = new VisionUsageExample();
		(new Thread(example, "Vision example")).start();
	}

}
