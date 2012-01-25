package sdp.vision;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.StateException;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * Provides visual information from the camera.
 * 
 * @author Gediminas Liktaras
 */
public class CameraInputProvider implements VisualInputProvider {
	
	/** Video device, whose input will be captured. */
	private VideoDevice videoDevice;
	/** The device's frame grabber. */
	private FrameGrabber frameGrabber;
	
	/** Whether the callback object is set. */
	private boolean callbackSet;
	
	
	/**
	 * The main constructor.
	 * 
	 * @param deviceFile Absolute path to the camera's device file.
	 * @param standard Capture standard to use.
	 * @param channel Capture channel to use.
	 */
	public CameraInputProvider(String deviceFile, int standard, int channel) {
		int width = V4L4JConstants.MAX_WIDTH;
		int height = V4L4JConstants.MAX_HEIGHT;
		int quality = 80;
		
		callbackSet = false;
		
        try {
            videoDevice = new VideoDevice(deviceFile);
            frameGrabber = videoDevice.getJPEGFrameGrabber(width, height, standard, channel, quality);
        } catch (V4L4JException e) {
            System.err.println("Error setting up capture."); 	// TODO: Replace w/ logging.
            e.printStackTrace();            
            cleanup();
            return;
        }
	}
	
	
	/**
	 * Set the provider's callback object.
	 * 
	 * @param callback The callback object.
	 */
	@Override
	public void setCallback(CaptureCallback callback) {
		frameGrabber.setCaptureCallback(callback);
		callbackSet = (callback != null);
	}

	/**
	 * Begin video capture, using the provided object as a callback.
	 * 
	 * @param callback The callback object.
	 */
	@Override
	public void startCapture() {
		if (!callbackSet) {
			System.err.println("Warning! The callback in the CameraInputProvider is unset.");
		}
		
		try {
	        frameGrabber.startCapture();
	        System.out.println("Starting capture at " + frameGrabber.getWidth() + "x" + frameGrabber.getHeight());
	    } catch (V4L4JException e) {
	        System.err.println("Error starting the capture.");	// TODO: replace with logging.
	        e.printStackTrace();
	    }
	}
	
	
	/**
	 * Clean up resources.
	 */
	private void cleanup() {
        try {
            frameGrabber.stopCapture();
        } catch (StateException ex) {
        	// Frame grabber is not running, continue.
        }

        videoDevice.releaseFrameGrabber();
        videoDevice.release();
	}

}
