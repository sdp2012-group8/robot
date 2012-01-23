package sdp.vision;
import java.util.List;

import au.edu.jcu.v4l4j.Control;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

public abstract class Vision extends Thread {
	// Video capture variables
	protected VideoDevice videoDevice;
	protected FrameGrabber frameGrabber;
	protected Thread captureThread;
	private static final int SATURATION = 65535;
	private static final int BRIGHTNESS = 32768;
	private static final int CONTRAST = 32768;
	private static final int HUE = 32768;
	public static final int FULL_LUMA_RANGE = 1;
	public static final int UV_RATIO = 49;

	public Vision() {
		super();
	}

	/**
	 * Initialises the FrameGrabber object with the given parameters
	     * @param dev the video device file to capture from
	     * @param w the desired capture width
	     * @param h the desired capture height
	     * @param std the capture standard
	     * @param channel the capture channel
	     * @param qty the JPEG compression quality
	     * @throws V4L4JException if any parameter if invalid
	 */
	protected void initFrameGrabber(String dev, int w, int h,
			int std, int channel, int qty) throws V4L4JException {
			    videoDevice = new VideoDevice(dev);
			    try {
					List<Control> controls =  videoDevice.getControlList().getList();
					for(Control c: controls) { 
						if(c.getName().equals("Saturation"))
							if(c.getName().equals("Contrast"))
								c.setValue(CONTRAST);
							if(c.getName().equals("Brightness"))
								c.setValue(BRIGHTNESS);
							if(c.getName().equals("full luma range"))
								c.setValue(FULL_LUMA_RANGE);
							if(c.getName().equals("Hue"))
								c.setValue(HUE);
							if(c.getName().equals("Saturation"))
								c.setValue(SATURATION);
							if(c.getName().equals("uv ratio"))
								c.setValue(UV_RATIO);
							}
					videoDevice.releaseControlList();
			    }
			    catch(V4L4JException e3) { 
			    	System.out.println("Cannot set video device settings!"); 
			    }
				videoDevice.releaseControlList();
			    frameGrabber = videoDevice.getJPEGFrameGrabber(w, h, channel, std, qty);
			    frameGrabber.startCapture();
			    System.out.println("Starting capture at "+frameGrabber.getWidth()+"x"+frameGrabber.getHeight());            
			}
}