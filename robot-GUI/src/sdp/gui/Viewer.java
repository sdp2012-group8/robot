package sdp.gui;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Observable;
import java.util.Observer;

import sdp.common.WorldState;
import sdp.vision.CameraVisualInputProvider;
import sdp.vision.OldImageProcessor;
import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;


public class Viewer implements Runnable, Observer {
	
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
			    /*try {
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
				videoDevice.releaseControlList();*/
			    frameGrabber = videoDevice.getJPEGFrameGrabber(w, h, channel, std, qty);
			    frameGrabber.startCapture();
			    System.out.println("Starting capture at "+frameGrabber.getWidth()+"x"+frameGrabber.getHeight());            
			}

	// Window Variables

	private boolean stopCapturingVideo;

	// Frames per second Variables - Max 25fps
	long prevsec = 0; // this will store previous timestamp
	int framesThisSecond = 0; // this is fps counter
	OldGUI gui;
	int imgwidth;
	int imgheight;
	public static OldImageProcessor imageProcessor;
	
	CameraVisualInputProvider input;

	/**
	 * Builds a WebcamViewer object
	 * 
	 * @param dev
	 *            the video device file to capture from
	 * @param w
	 *            the desired capture width
	 * @param h
	 *            the desired capture height
	 * @param std
	 *            the capture standard
	 * @param channel
	 *            the capture channel
	 * @param qty
	 *            the JPEG compression quality
	 * @throws V4L4JException
	 *             if any parameter if invalid
	 */
	public Viewer(String dev, int w, int h, int std, int channel, int qty) {
//		try {
//			initFrameGrabber(dev, w, h, std, channel, qty);
//		} catch (V4L4JException e) {
//			System.out.println(e.toString());
//			e.printStackTrace();
//		}
		
		input = new CameraVisualInputProvider("/dev/video0", V4L4JConstants.STANDARD_WEBCAM, 0);
		imageProcessor = new OldImageProcessor();
		gui = new OldGUI();
		gui.setVisible(true);
		stopCapturingVideo = false;
		captureThread = new Thread(this, "Capture Thread");
		captureThread.start();
		
	}

	private void calculateFPS() {
		int forwsec = (int) System.currentTimeMillis() / 1000;
		if (prevsec != forwsec) {
			if (framesThisSecond > 24)
				System.out.println("FPS:" + framesThisSecond
						+ "!!! WOOHOO im super fast!");
			else
				System.out.println("FPS:" + framesThisSecond);
			framesThisSecond = 0;
			prevsec = Integer.valueOf(forwsec);
		} else {
			framesThisSecond++;
		}
	}

	/**
	 * Implements the capture thread: get a frame from the FrameGrabber, and
	 * display it
	 */
	public void run() {
		input.addObserver(this);
		input.startCapture();
	}

	/**
	 * Add a line to the printed image for debugging
	 * 
	 * @param p1
	 *            Point from
	 * @param p2
	 *            Point to
	 */
	public void addLine(Point p1, Point p2, int color) {
		imageProcessor.addLineToBeDrawn(p1, p2, color);
	}

	public void dropLine() {
		if (!imageProcessor.lines.isEmpty()) {
			imageProcessor.lines.pop();
			imageProcessor.lines.pop();
			imageProcessor.lineColor.pop();
		}
	}

	public void dropAllLines() {
		while (!imageProcessor.lines.isEmpty()) {
			imageProcessor.lines.pop();
		}
		while (!imageProcessor.lineColor.isEmpty()) {
			imageProcessor.lineColor.pop();
		}
	}

	public static void main(String[] args) {
		String dev = "/dev/video0";
		int w = V4L4JConstants.MAX_WIDTH, h = V4L4JConstants.MAX_HEIGHT, std = V4L4JConstants.STANDARD_PAL, channel = 0, qty = 60;
		new Viewer(dev, w, h, std, channel, qty);
	}

	public static Viewer startVision() {
		String caller = new Throwable().fillInStackTrace().getStackTrace()[1]
				.getClassName();
		OldGUI.className = caller;
		String dev = "/dev/video0";
		int w = 640, h = 480, std = V4L4JConstants.STANDARD_PAL, channel = 0, qty = 60;
		return new Viewer(dev, w, h, std, channel, qty);
	}

	public WorldState getObjectInfos() {
		return imageProcessor.worldState;
	}

	@Override
	public void update(Observable o, Object arg) {
		BufferedImage frame = (BufferedImage) arg;
		
		prevsec = (int) System.currentTimeMillis() / 1000;

		try {
			BufferedImage iq = imageProcessor.process(frame);
			gui.setImage(iq);
			OldImageProcessor.displX = gui.getLocationOnScreen().x;
			OldImageProcessor.displY = gui.getLocationOnScreen().y - 25;
		} catch (NullPointerException e) {
			e.printStackTrace();
			System.out.println("Shutting down...");
			stopCapturingVideo = true;
			frameGrabber.stopCapture();
			videoDevice.releaseFrameGrabber();
		}
		calculateFPS();
	}

}