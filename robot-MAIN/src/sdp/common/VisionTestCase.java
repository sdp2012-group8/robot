package sdp.common;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import sdp.common.world.Robot;
import sdp.common.world.WorldState;
import sdp.common.xml.XmlUtils;


/**
 * A container for a vision test. 
 * 
 * @author Gediminas Liktaras
 */
public class VisionTestCase {
	
	/** The class' logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.vision.testbench.VisionTestCase");
	
	/** The image to process. */
	private BufferedImage image;
	/** Filename of the test's image. */
	private String imageFilename;
	/** Expected world state. */
	private WorldState expectedState;
	
	
	/**
	 * Create a new test case.
	 * 
	 * @param imageFilename Filename of the image file to use for testing.
	 * @param expectedState The expected world state.
	 */
	public VisionTestCase(String imageFilename, WorldState expectedState) {
		this.imageFilename = imageFilename;
		this.expectedState = expectedState;
		
		image = null;
		try {
			image = ImageIO.read(new File(imageFilename));
		} catch (IOException e) {
			LOGGER.warning("Could not read the image " + imageFilename + " for a test case.");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get the image for the vision system to process.
	 * 
	 * @return The image.
	 */
	public BufferedImage getImage() {
		return image;
	}
	
	/**
	 * Get the filename of the test's image.
	 * 
	 * @return Image's filename.
	 */
	public String getImageFilename() {
		return imageFilename;
	}

	/**
	 * Get the target world state.
	 * 
	 * @return The target world state.
	 */
	public WorldState getExpectedState() {
		return expectedState;
	}
	
	
	/**
	 * Obtain a collection of test cases from the given XML file.
	 * 
	 * @param filename Filename of the XML file in question.
	 * @return An array of vision test cases.
	 */
	public static ArrayList<VisionTestCase> readTestCases(String filename) {
		Document xmlDoc = XmlUtils.openXmlDocument(new File(filename));
		
		ArrayList<VisionTestCase> tests = new ArrayList<VisionTestCase>();
		
		Element root = xmlDoc.getDocumentElement();
		NodeList entries = root.getElementsByTagName("image");
		
		for (int i = 0; i < entries.getLength(); ++i) {
			Element imageEntry = (Element) entries.item(i);
			
			String imageFilename = XmlUtils.getChildText(imageEntry, "filename");
			
			Element locDataElem = (Element) imageEntry.getElementsByTagName("location-data").item(0);
			
			Element ballElem = (Element) locDataElem.getElementsByTagName("ball").item(0);
			double ballX = XmlUtils.getChildDouble(ballElem, "x");
			double ballY = XmlUtils.getChildDouble(ballElem, "y");
			Point2D.Double ball = new Point2D.Double(ballX, ballY);
			
			Element blueElem = (Element) locDataElem.getElementsByTagName("bluerobot").item(0);
			double blueX = XmlUtils.getChildDouble(blueElem, "x");
			double blueY = XmlUtils.getChildDouble(blueElem, "y");
			double blueAngle = XmlUtils.getChildDouble(blueElem, "angle");
			Point2D.Double bluePos = new Point2D.Double(blueX, blueY);
			Robot blueRobot = new Robot(bluePos, blueAngle);
			
			Element yellowElem = (Element) locDataElem.getElementsByTagName("yellowrobot").item(0);
			double yellowX = XmlUtils.getChildDouble(yellowElem, "x");
			double yellowY = XmlUtils.getChildDouble(yellowElem, "y");
			double yellowAngle = XmlUtils.getChildDouble(yellowElem, "angle");
			Point2D.Double yellowPos = new Point2D.Double(yellowX, yellowY);
			Robot yellowRobot = new Robot(yellowPos, yellowAngle);
			
			WorldState expectedState = new WorldState(ball, blueRobot, yellowRobot, null);
			VisionTestCase curTestCase = new VisionTestCase(imageFilename, expectedState);
			tests.add(curTestCase);
		}

		return tests;
	}

}
