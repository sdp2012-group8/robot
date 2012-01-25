package sdp.gui;

import java.util.Observable;
import java.util.Observer;

import au.edu.jcu.v4l4j.V4L4JConstants;
import sdp.common.WorldState;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.ImageVisualInputProvider;
import sdp.vision.Vision;
import sdp.vision.VisualInputObservable;

/**
 * This is a temporary class for carrying out testing of the old GUI interface.
 * 
 * @author Gediminas Liktaras
 */
public class OldGUITest implements Runnable, Observer {
	
	/** The GUI object. */
	private OldGUI gui;
	
	/** The vision subsystem object. */
	private Vision vision;
	/** Visual input source. */
	private VisualInputObservable input;
	
	/** Whether to use camera or offline inputs. */
	private static final boolean USE_CAMERA = false;
	
	
	/**
	 * The main constructor.
	 */
	public OldGUITest() {
		vision = new Vision();
		vision.addObserver(this);
		
		if (USE_CAMERA) {		
			input = new CameraVisualInputProvider("/dev/video0", V4L4JConstants.STANDARD_PAL, 0);
		} else {
			String filenames[] = { "../robot-VISION/data/testImages/pitch2-1.png",
					               "../robot-VISION/data/testImages/pitch2-2.png",
					               "../robot-VISION/data/testImages/pitch2-3.png" };
			input = new ImageVisualInputProvider(filenames, 25);
		}
		input.addObserver(vision);
		
		gui = new OldGUI();
		gui.setVisible(true);
	}
	
	
	/**
	 * The thread's run method.
	 */
	@Override
	public void run() {
		input.startCapture();
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof Vision) {		
			WorldState state = (WorldState) arg;
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
		OldGUITest app = new OldGUITest();
		(new Thread(app)).start();
	}

}
