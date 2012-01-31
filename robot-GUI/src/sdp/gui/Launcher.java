package sdp.gui;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.ImageVisualInputProvider;
import sdp.vision.Vision;
import sdp.vision.VisualInputProvider;

/**
 * This is a temporary class for carrying out testing of the old GUI interface.
 * 
 * @author Gediminas Liktaras
 */
public class Launcher implements Runnable {
	
	/** The GUI object. */
	private MainWindow gui;
	
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
	public Launcher() {
		vision = new Vision();		
		visionObserver = new WorldStateObserver(vision);
		
		if (USE_CAMERA) {		
			input = new CameraVisualInputProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0);
		} else {
			String filenames[] = { "../robot-VISION/data/testImages/pitch2-1.png",
					               "../robot-VISION/data/testImages/pitch2-2.png",
					               "../robot-VISION/data/testImages/pitch2-3.png" };
			input = new ImageVisualInputProvider(filenames, 25);
		}
		input.setCallback(vision);
		
		gui = new MainWindow();
		gui.setVisible(true);
	}
	
	
	/**
	 * The thread's run method.
	 */
	@Override
	public void run() {
		input.startCapture();
		
		while (!Thread.interrupted()) {
			WorldState state = visionObserver.getNextState();
			gui.setImage(state.getWorldImage());
			
			System.out.println("NEW STATE: " +
					"Ball at (" + state.getBallCoords().x + ", " + state.getBallCoords().y + "), " +
					"Blue at (" + state.getBlueRobot().getCoords().x +
						", " + state.getBlueRobot().getCoords().y +
						", " + state.getBlueRobot().getAngle() + ") " +
					"Yellow at (" + state.getYellowRobot().getCoords().x +
						", " + state.getYellowRobot().getCoords().y +
						", " + state.getYellowRobot().getAngle() + ").");
		}
	}
	
	
	/**
	 * The main method.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		Launcher app = new Launcher();
		(new Thread(app, "Launcher")).start();
	}

}
