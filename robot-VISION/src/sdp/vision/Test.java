package sdp.vision;

import java.awt.*;
import javax.swing.JFrame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import sdp.common.Robot;
import sdp.common.WorldState;
import sdp.vision.ImageProcessorConfiguration;
import sdp.vision.Viewer;
import sdp.common.Utilities;


public class Test extends Vision {
	static ImageProcessorConfiguration config = new ImageProcessorConfiguration();
	
	
	//gets the document from the xml file
	private static Document getDocumentFromXML(String filename){
		//Something about factories. 
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try {
			//getting  an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
			//parse using builder to get  document representation of the XML file
			 dom = db.parse(filename);
			System.out.println("Loaded XML file.");
		//In the unlikely event that something breaks
		}catch(ParserConfigurationException pce) {
			pce.printStackTrace();
		}catch(SAXException se) {
			se.printStackTrace();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
		return dom;
	}
	//return an array list with the elements required from the xml document
	private static ArrayList<WorldState> getWorldStateFromDocument(Document dom){
		//An ArrayList of WordStates is created and then states can be added as they are parsed
		ArrayList<WorldState> states = new ArrayList<WorldState>();
		//get the root element from the document
		Element annotations = dom.getDocumentElement();
		//get a nodelist of  elements 
		NodeList nl = annotations.getElementsByTagName("image");
		//if there are elements then iterate through them to extract each state
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				//get the individual state data node
				Element ws = (Element)nl.item(i);
				System.out.println("State object found");
				//Element passed to method for parsing individual elements
				WorldState annotated = getWorldStateFromElement(ws);
				//parsed state added to ArrayList
				states.add(annotated);
			}
		}
		//Debug
		System.out.print(states.size());
		System.out.println(" world states extracted from XML file.");
		//ArrayList of parsed states returned to whatever called method
		return states;
	}
	
	/**
	 * Next two methods stolen from: http://totheriver.com/learn/xml/xmltutorial.html#6.1
	 */
	//returns the text value from the xml document
	private static String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		//Pretty printed linebreaks within the XML broke EVERYTHING but they are gone now
		return textVal.replace("\n", "");
	}


	/**
	 * Calls getTextValue and returns a <s>int</s> FLOAT value
	 */
	//return the float elements from the xml file
	private static float getFloatValue(Element ele, String tagName) {
		//in production application you would catch the exception
		return Float.parseFloat(getTextValue(ele,tagName));
	}
	
	/**
	 * End of stolen methods
	 */
	
	//returns a WorldState object with the data from the xml file
	private static WorldState getWorldStateFromElement(Element ws){
		
		WorldState state;
		//SLIGHT XML NAVIGATION
		//image -> filename
		String filename = getTextValue(ws,"filename");
		BufferedImage image = null;
		try{
			//filename referenced in XML is loaded as a BufferedImage
			image = ImageIO.read(new File(filename));
			System.out.printf("Image read from XML: %s\n",filename);
		}catch (Exception e){
			//This flags up when you FORGET TO REMOVE LEADING NEWLINES FROM FILENAMES
			System.out.print(e);
			System.out.printf("Error loading image from XML: %s\n",filename);
		}
		//XML NAVIGATION GOING DEEPER
		// image -> location-data
		Element data = (Element)ws.getElementsByTagName("location-data").item(0);
		// location-data -> ball
		Element balldata = (Element)data.getElementsByTagName("ball").item(0);
		// ball -> x and ball -> y. Passed into a Point2D.Double ready to be passed to WorldState
		Point2D.Double ballpos = new Point2D.Double(normX(getFloatValue(balldata,"x")),normY(getFloatValue(balldata,"y")));
		// location-data -> bluerobot
		Element bluerobotdata = (Element) data.getElementsByTagName("bluerobot").item(0); 
		//defining a blue robot object and passing it bluerobot -> x, bluerobot -> y and bluerobot -> angle
		Robot bluerobot = new Robot(new Point2D.Double(normX(getFloatValue(bluerobotdata,"x")),normY(getFloatValue(bluerobotdata,"y"))), (double) getFloatValue(bluerobotdata,"angle") );
		// location-data -> yellowrobot
		Element yellowrobotdata = (Element)data.getElementsByTagName("yellowrobot").item(0); 
		//defining a yellow robot object and passing it yellowrobot -> x, yellowrobot -> y and yellowrobot -> angle
		Robot yellowrobot = new Robot(new Point2D.Double(normX(getFloatValue(yellowrobotdata,"x")),normY(getFloatValue(yellowrobotdata,"y"))), (double) getFloatValue(yellowrobotdata,"angle") );
		//WorldState object is created with parsed data passed to the constructor. 
		state = new WorldState(ballpos, bluerobot, yellowrobot, image);
		//WorldState object is returned to whatever called this method.
		return state;
	}
	
	//IGNORE THE FOLLOWING TWO METHODS
	public static float normX(float x){
		return x; //(float) ((x)/(double)config.getFieldWidth());
	}
	public static float normY(float y){
		return y; //(float) ((y)/(double)config.getFieldWidth());
	}
	
	public static WorldState convertToPixelRange(WorldState ws){
		Point2D.Double ball = new Point2D.Double(correctX(ws.getBallCoords().x),correctY(ws.getBallCoords().y));
		Robot blue = new Robot(new Point2D.Double(correctX(ws.getBlueRobot().getCoords().x),correctY(ws.getBlueRobot().getCoords().y)),ws.getBlueRobot().getAngle());
		Robot yellow = new Robot(new Point2D.Double(correctX(ws.getYellowRobot().getCoords().x),correctY(ws.getYellowRobot().getCoords().y)),ws.getYellowRobot().getAngle());
		WorldState ns = new WorldState(ball,blue,yellow,ws.getWorldImage());
		return ns;
	}
	
	public static int correctX (double x){
		//x+=config.getRawFieldLowX();
		x*=config.getFieldWidth();
		x+=config.getFieldLowX();
		return (int) x;
	}
		public static int correctY (double y){
		//y+=config.getRawFieldLowY();
		y*=config.getFieldWidth();
		y+=config.getFieldLowY();
		return (int) y;
	}
	
	//Compares features of the WorldState(s)
	public static void compareWorldStates(WorldState reference, WorldState visionresult){
		System.out.printf("WorldState comparison in format Human | Vision\n");
		System.out.printf("Ball positions: %s | %s \n", reference.getBallCoords(), visionresult.getBallCoords());
		// Pythagorean distance. Sqrt ((x1-x2)^2 + (y1-y2)^2)
		float balldist = (float) Math.sqrt( Math.pow(reference.getBallCoords().x - visionresult.getBallCoords().x,2) + Math.pow(reference.getBallCoords().y  - visionresult.getBallCoords().y,2));
		System.out.printf("Distance between perceptions: %f\n", balldist);
		System.out.printf("Blue Robot positions: %s | %s \n", reference.getBlueRobot().getCoords(), visionresult.getBlueRobot().getCoords());
		// Pythagorean distance. Sqrt ((x1-x2)^2 + (y1-y2)^2)
		float bluedist = (float) Math.sqrt( Math.pow(reference.getBlueRobot().getCoords().x- visionresult.getBlueRobot().getCoords().x,2) + Math.pow(reference.getBlueRobot().getCoords().y  - visionresult.getBlueRobot().getCoords().y,2));
		System.out.printf("Distance between perceptions: %f\n", bluedist);
		System.out.printf("Blue Robot angles: %s | %s \n", reference.getBlueRobot().getAngle(), visionresult.getBlueRobot().getAngle());
		System.out.printf("Distance between perceptions: %f\n", reference.getBlueRobot().getAngle() - visionresult.getBlueRobot().getAngle());
		System.out.printf("Yellow Robot positions: %s | %s \n", reference.getYellowRobot().getCoords(), visionresult.getYellowRobot().getCoords());
		// Pythagorean distance. Sqrt ((x1-x2)^2 + (y1-y2)^2)
		float yellowdist = (float) Math.sqrt( Math.pow(reference.getYellowRobot().getCoords().x- visionresult.getYellowRobot().getCoords().x,2) + Math.pow(reference.getYellowRobot().getCoords().y  - visionresult.getYellowRobot().getCoords().y,2));
		System.out.printf("Distance between perceptions: %f\n", yellowdist);
		System.out.printf("Yellow Robot angles: %s | %s \n", reference.getYellowRobot().getAngle(), visionresult.getYellowRobot().getAngle());
		System.out.printf("Distance between perceptions: %f\n", reference.getYellowRobot().getAngle() - visionresult.getYellowRobot().getAngle());
	}

	public static void main(String[] args) throws InterruptedException{
		//The xml file (currently hard coded location) is parsed by the above voodoo and stored in an ArrayList<WorldState>
		ArrayList<WorldState> Annotations = getWorldStateFromDocument(getDocumentFromXML("xml/imagedata.xml"));
		//For each state documented in the XML
		JFrame frame = new JFrame("Image Display");
		frame.setSize(640,480);
		Viewer base = new Viewer(null, null, null);
		frame.getContentPane().add(base);
		frame.setVisible(true);
		while(true){
			int ballerror = 0;
			for (WorldState state : Annotations){
				Utilities utility = new Utilities();
				BufferedImage manualimage = utility.deepBufferedImageCopy(state.getWorldImage());
				System.out.printf("Getting Vision WorldState\n");
				//The comparative WorldState object that the vision system will construct.
				WorldState visionimage;
				Test test =new Test();
				//The vision system is passed the image from the annotation and generates
				//a WorldState to be compared with the human perception
				visionimage = convertToPixelRange(test.worldImageData(utility.deepBufferedImageCopy(state.getWorldImage())));
				base.updateImageAndState(manualimage,state,visionimage);
				//Thread.sleep(1000);
				//Differences between the WorldStates are calculated
				//compareWorldStates(state,visionimage);
				ballerror += (float) Math.sqrt( Math.pow(state.getBallCoords().x - visionimage.getBallCoords().x,2) + Math.pow(state.getBallCoords().y  - visionimage.getBallCoords().y,2));
			}
			System.out.printf("Average ball error is %f\n", (float)ballerror/Annotations.size());
		}
			
	}


}
