package sdp.vision.testbench;

import java.io.*;
import java.util.ArrayList;
import java.awt.geom.Point2D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sdp.common.Robot;
import sdp.common.WorldState;
import sdp.vision.Vision;
import sdp.vision.processing.ImageProcessorConfig;
import sdp.vision.processing.ProcUtils;
import sdp.common.Utilities;
import sdp.common.xml.XmlUtils;


/**
 * The main test bench logic class.
 * 
 * @author Aaron Cronin
 * @author Gediminas Liktaras
 */
public class TestBench {
	
	/** Active vision instance. */
	private Vision vision;
	
	
	/**
	 * Create a new test bench instance.
	 */
	public TestBench() {
		vision = new Vision();
	}
	
	
	/**
	 * Run a test using the given test specification and using the default
	 * image processor configuration.
	 * 
	 * @param testSpec A path to an XML file that contains the test specification.
	 */
	public void runTest(String testSpec) {
		runTest(testSpec, new ImageProcessorConfig());
	}
	
	/**
	 * Run a test using the given test specification and image processor
	 * configuration.
	 * 
	 * @param testSpec A path to an XML file that contains the test specification.
	 * @param config Image processor configuration to use.
	 */
	public void runTest(String testSpec, ImageProcessorConfig config) {
		vision.setConfiguration(config);
		
		ArrayList<VisionTestCase> tests = readTestCases(testSpec);
		VisionSystemErrorAccumulator errorAcc = new VisionSystemErrorAccumulator();
		
		for (VisionTestCase test : tests) {
			WorldState actualState_norm = vision.extractWorldState(Utilities.deepBufferedImageCopy(test.getImage()));
			WorldState actualState_frame = ProcUtils.stateToFrameCoordinates(config, actualState_norm);
			
			errorAcc.addRecord(test.getImageFilename(), test.getExpectedState(), actualState_frame);
		}

		errorAcc.dumpMetrics(System.out);		
	}

	
	/**
	 * Obtain a collection of test cases from the given XML file.
	 * 
	 * @param filename Filename of the XML file in question.
	 * @return An array of vision test cases.
	 */
	private ArrayList<VisionTestCase> readTestCases(String filename) {
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

	
	/**
	 * The entry point.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		TestBench testBench = new TestBench();
		testBench.runTest("xml/imagedata.xml");
	}
}
