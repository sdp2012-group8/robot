package sdp.vision.visualinput;

import java.util.logging.Logger;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.StateException;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


/**
 * Provides visual information from the camera.
 * 
 * @author Gediminas Liktaras
 */
public class CameraVisualInputProvider extends VisualInputProvider implements CaptureCallback {
	
	/** Video device, whose input will be captured. */
	private VideoDevice videoDevice;
	/** The device's frame grabber. */
	private FrameGrabber frameGrabber;

	/** The class' logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.vision.CameraVisualInputProvider");
	
	
	/**
	 * The main constructor.
	 * 
	 * @param deviceFile Absolute path to the camera's device file.
	 * @param standard Capture standard to use.
	 * @param channel Capture channel to use.
	 */
	public CameraVisualInputProvider(String deviceFile, int standard, int channel) {
		int width = 640;
		int height = 480;
		int quality = 80;
		
        try {
            videoDevice = new VideoDevice(deviceFile);
            frameGrabber = videoDevice.getJPEGFrameGrabber(width, height, standard, channel, quality);
            frameGrabber.setCaptureCallback(this);
        } catch (V4L4JException e) {
        	LOGGER.warning("Error setting up capture.");
            e.printStackTrace();            
            cleanup();
            return;
        }
	}
	

	/**
	 * Begin video capture, using the provided object as a callback.
	 * 
	 * @param callback The callback object.
	 */
	@Override
	public void startCapture() {
		try {
	        frameGrabber.startCapture();
	        LOGGER.info("Starting capture at " + frameGrabber.getWidth() + "x" + frameGrabber.getHeight());
	    } catch (V4L4JException e) {
	        LOGGER.warning("Error starting the capture.");
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


	/**
	 * This method is called if there is an error during capture.
	 * 
	 * @param e The exception.
	 */
	@Override
	public void exceptionReceived(V4L4JException e) {
		LOGGER.warning("Error occured during capture.");
		e.printStackTrace();
	}

	/**
	 * This method is called whenever there is a new frame available from
	 * the camera.
	 */
	@Override
	public void nextFrame(VideoFrame frame) {
		sendNextFrame(frame.getBufferedImage());
		frame.recycle();
	}

}
