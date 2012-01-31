package sdp.vision;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import sdp.common.Robot;
import sdp.common.WorldState;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;


/**
 * The main object recognition class.
 *  
 * @author Andrei-ionut Manolache
 * @author Gediminas Liktaras
 * @author Laura Mihaela Ionescu
 */
public class ImageProcessor {
	
	/** The processor's configuration. */
	private ImageProcessorConfiguration config;


	private static int RED = 0;
	private static int GREEN = 1;
	private static int BLUE = 2;
	private static int YELLOW = 2;
	
	Point2D btPos = new Point2D.Double(-1, -1);
	Point2D ytPos = new Point2D.Double(-1, -1);
	Point2D ourPos = new Point2D.Double(-1, -1);
	Point2D lastBallPos = new Point2D.Double(-1, -1);
	
	
	/**
	 * Create a new image processor with the default configuration.
	 */
	public ImageProcessor() {
		config = new ImageProcessorConfiguration();
	}
	
	/**
	 * Create a new image processor with the specified configuration.
	 * @param config Configuration to use.
	 */
	public ImageProcessor(ImageProcessorConfiguration config) {
		this();
		this.config = config;
	}
	
	
	/**
	 * Extract the world state from the supplied image.
	 * 
	 * @param frame The image to process.
	 * @return The world state, present in the image.
	 */
	public WorldState extractWorldState(BufferedImage frame) {
		IplImage background = cvLoadImage("../robot-VISION/data/testImages/bg.jpg");		
		IplImage image = IplImage.createFrom(frame);
		
		IplImage differenceImage = IplImage.createFrom(getDifferenceImage(
				image.getBufferedImage(), background.getBufferedImage()));

		IplImage redChannel = getChannels(differenceImage, RED);
		IplImage greenChannel = getChannels(differenceImage, GREEN);
		IplImage blueChannel = getChannels(differenceImage, BLUE);
		
		IplImage blueThreshold = thresholdChannel(blueChannel, BLUE);
		IplImage redThreshold = thresholdChannel(redChannel, RED);
		IplImage greenThreshold = thresholdChannel(greenChannel, GREEN);

		IplImage grayImage = cvCreateImage(cvGetSize(image), 8, 1);
		cvCvtColor(image, grayImage,CV_RGB2GRAY);
	//	IplImage thresholdImage = ip.thresholdChannel(grayImage, RED);
		
		findBall(image);		
		
		Point2D.Double ballPos = new Point2D.Double(0.0, 0.0);
		Robot blueRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		Robot yellowRobot = new Robot(new Point2D.Double(0.0, 0.0), 0.0);
		
		BufferedImage worldImage = image.getBufferedImage();
		Graphics2D wiGraphics = worldImage.createGraphics();
		wiGraphics.setColor(Color.white);
		wiGraphics.drawRect(config.getFieldLowX(), config.getFieldLowY(),
				config.getFieldWidth(), config.getFieldHeight());
		
		return new WorldState(ballPos, blueRobot, yellowRobot, worldImage);
	}
	
	


	/**
	 * @param image
	 * @param background
	 * @return BufferedImage Takes an image and the background and performs
	 *         background subtraction The image returned is normalized, to
	 *         compensate for shadows and the differences in lighting
	 */
	public BufferedImage getDifferenceImage(BufferedImage image,
			BufferedImage background) {
		Raster backgroundData = null;
		Raster data = null;

		try {
			data = image.getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}

		try {
			backgroundData = background.getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}

		for (int i = 0; i < image.getWidth(); i = i + 1) { // for every
			for (int j = 0; j < image.getHeight(); j = j + 1) {

				int[] backgroundPixel = new int[3];
				data.getPixel(i, j, backgroundPixel);

				int[] imagePixel = new int[3];
				backgroundData.getPixel(i, j, imagePixel);

				if (i < config.getFieldLowX()
						|| i > config.getFieldHighX()
						|| j < config.getFieldLowY()
						|| j > config.getFieldHighY()) {
					Color colour = new Color(0, 0, 0);
					int rgb = colour.getRGB();
					image.setRGB(i, j, rgb);
				} else {

					int[] difference = getDifference(imagePixel,
							backgroundPixel);

					// create a new colour with the rgb of the difference
					int r = 0;
					int b = 0;
					int g = 0;

					r = difference[0] > 10 ? difference[0] : 0;
					g = difference[1] > 10 ? difference[1] : 0;
					b = difference[2] > 10 ? difference[2] : 0;

					int sum = r + g + b;

					Color colour;
					if (sum < 40) {
						colour = new Color(0, 0, 0);
						r = 0;
						g = 0;
						b = 0;
					} else {
						//normalize
						colour = new Color(r * 255 / sum, g * 255 / sum, b
								* 255 / sum);
						r = r * 255 / sum;
						g = g * 255 / sum;
						b = b * 255 / sum;
						colour = new Color(r,g,b);
					}
					
					int rgb = colour.getRGB();

					image.setRGB(i, j, rgb);

				}

			}
		}

		return image;
	}

	/**
	 * @param image
	 * @param colour
	 * @return IplImage
	 * splits the image into red, green and blue channels, returning the channel specified by the colour argument
	 */
	public IplImage getChannels(IplImage image, int colour) {

		IplImage blueChannel = cvCreateImage(cvGetSize(image), 8, 1);
		IplImage greenChannel = cvCreateImage(cvGetSize(image), 8, 1);
		IplImage redChannel = cvCreateImage(cvGetSize(image), 8, 1);

		cvSplit(image, blueChannel, greenChannel, redChannel, null);

		if (colour == RED)
			return redChannel;
		else if (colour == GREEN)
			return greenChannel;
		else
			return blueChannel;
	}

	/**
	 * @param image
	 * @param colour
	 * @return IplImage The function takes a binary image representing one of
	 *         the r,g,b channels and an int specifying the channel. It returns
	 *         the thresholded image
	 * */
	public IplImage thresholdChannel(IplImage image, int colour) {
		IplImage thresholdImage = cvCreateImage(cvGetSize(image), 8, 1);
		int min;
		int max;
		if (colour == RED) {
			min = 100;
			max = 200;
		} else if (colour == GREEN) {
			min = 100;
			max = 200;
		} else {
			min = 100;
			max = 200;
		}

		cvThreshold(thresholdImage, thresholdImage, min, max, CV_THRESH_BINARY);

		cvSmooth(thresholdImage, thresholdImage, CV_MEDIAN, 13);

		return thresholdImage;
	}

	// returns the difference between two arrays of 3 elements
	public int[] getDifference(int[] colour1, int[] colour2) {
		int[] difference = { 0, 0, 0 };
		difference[0] = colour1[0] - colour2[0];
		difference[1] = colour1[1] - colour2[1];
		difference[2] = colour1[2] - colour2[2];

		return difference;
	}
    
	/**
	 * Finds the centroid of the yellow or the blue robot, depending on the colour argument
	 * @param thresholdImage
	 * @param colour
	 */
    public void findCentroid(IplImage thresholdImage, int colour){

    	double posX = 0;
        double posY = 0;        
        CvMoments moments = new CvMoments();
        cvMoments(thresholdImage, moments, 1);
      
        double momX10 = cvGetSpatialMoment(moments, 1, 0); 
        double momY01 = cvGetSpatialMoment(moments, 0, 1);
        double area = cvGetCentralMoment(moments, 0, 0);
        posX = momX10 / area;
        posY = momY01 / area;
       
        if (colour == BLUE)
        	btPos.setLocation(posX, posY);
        else
        	if (colour == YELLOW)
        	ytPos.setLocation(posX,posY);
        
        System.out.print("Position " + posX + " " + posY);
    
    }
	
    /**
     * Finds the centre of the ball
     * @param image
     */
    public void findBall(IplImage image){
    	
    	CvScalar min = cvScalar(0, 0, 130, 0);
        CvScalar max= cvScalar(140, 110, 255, 0);  
        
    	//create binary image of original size
        IplImage imgThreshold = cvCreateImage(cvGetSize(image), 8, 1);
        
        //apply thresholding  
        cvInRangeS(image, min, max, imgThreshold);
        
        //smooth filter- median
        cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 13);
        
        //set the position of the ball
        findCentroid(imgThreshold, RED);
    }
    
    
	/**
	 * Get the image processor's configuration.
	 * 
	 * @return The configuration.
	 */
	public synchronized ImageProcessorConfiguration getConfiguration() {
		return config;
	}

	/**
	 * Set the image processor's configuration.
	 * 
	 * @param config The new configuration.
	 */
	public synchronized void setConfiguration(ImageProcessorConfiguration config) {
		this.config = config;
	}
	
	
	/**
	 * The main method. Used primarily for testing.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String args[]) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(new File("data/testImages/start_positions.jpg"));
		} catch (IOException e) {
			System.err.println("Could not read image.");
			System.exit(1);
		}
		
		ImageProcessor ip = new ImageProcessor();
		WorldState state = ip.extractWorldState(image);
		
		final CanvasFrame canvas = new CanvasFrame("My Image");
		canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		canvas.showImage(state.getWorldImage());
	}
    
}
