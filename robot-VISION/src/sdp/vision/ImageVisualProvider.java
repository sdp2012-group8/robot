package sdp.vision;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import sdp.common.VisualCallback;


/**
 * Provides visual information from a list of image files.
 * 
 * @author Gediminas Liktaras
 */
public class ImageVisualProvider implements VisualProvider, Runnable {
	
	/** A list of images to present to the application. */
	private BufferedImage images[];
	/** The index of the next image to return. */
	private int nextImageIndex;
	
	/** How many milliseconds to sleep before presenting the next image. */
	private int sleepTime;
	
	/** The object's thread. */
	private Thread thread;
	/** The callback object. */
	private VisualCallback callback;
	
	
	/**
	 * The main constructor.
	 * 
	 * @param filenames A list of filenames to images that will be shown.
	 * @param fps How many frames per second to show.
	 */
	public ImageVisualProvider(String filenames[], int fps) {
		images = new BufferedImage[filenames.length];		
		try {
			for (int i = 0; i < filenames.length; ++i) {
				images[i] = ImageIO.read(new File(filenames[i]));
			}
		} catch(IOException e) {
			System.err.println("Could not read image files.");
			e.printStackTrace();
		}
		
		thread = new Thread(this, "ImageVisualProvider thread");
		
		sleepTime = 1000 / fps;
		nextImageIndex = 0;
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (!Thread.interrupted()) {
			callback.nextFrame(images[nextImageIndex]);
			nextImageIndex = (nextImageIndex + 1) % images.length;
			
			try {
				Thread.sleep(sleepTime);
			} catch(InterruptedException e) { }
		}
	}

	/* (non-Javadoc)
	 * @see sdp.vision.VisualInputProvider#startCapture()
	 */
	@Override
	public void startCapture() {
		if (callback == null) {
			System.err.println("The callback has not been set in ImageVisualProvider object.");
		}
		thread.start();
	}

	@Override
	public void setCallback(VisualCallback callback) {
		this.callback = callback;
	}

}
