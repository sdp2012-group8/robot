package sdp.vision;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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


public class Test extends Vision {
	//gets the document from the xml file
	private static Document getDocumentFromXML(String filename){
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document dom = null;
		try {

			//getting  an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get  document representation of the XML file
			 dom = db.parse(filename);
			System.out.println("Loaded XML file.");

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
		ArrayList<WorldState> states = new ArrayList<WorldState>();
		//get the root element from the document
		Element annotations = dom.getDocumentElement();
		//get a nodelist of  elements 
		NodeList nl = annotations.getElementsByTagName("image");
		if(nl != null && nl.getLength() > 0) {
			for(int i = 0 ; i < nl.getLength();i++) {
				//get the worldstate element
				Element ws = (Element)nl.item(i);
				System.out.println("Image object found");
				WorldState annotated = getWorldStateFromElement(ws);
				
				states.add(annotated);
			}
		}
		System.out.print(states.size());
		System.out.println(" world states extracted from XML file.");
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

		return textVal;
	}


	/**
	 * Calls getTextValue and returns a int value
	 */
	//return the float elements from the xml file
	private static float getFloatValue(Element ele, String tagName) {
		//in production application you would catch the exception
		return Float.parseFloat(getTextValue(ele,tagName));
	}
	
	/**
	 * End of stolen methods
	 */
	
	//returns a world state object with the data from the xml file
	private static WorldState getWorldStateFromElement(Element ws){
		
		WorldState state;
		String filename = getTextValue(ws,"filename").replace("\n", "");
		BufferedImage image = null;
		try{
			image = ImageIO.read(new File(filename));
			System.out.printf("Image read from XML: %s\n",filename);
		}catch (Exception e){
			System.out.print(e);
			System.out.printf("Error loading image from XML: %s\n",filename);
		}
		//passing the data by tag name
		Element data = (Element)ws.getElementsByTagName("location-data").item(0);
		Element balldata = (Element)data.getElementsByTagName("ball").item(0);
		//setting the ball coords.
		Point2D.Double ballpos = new Point2D.Double(getFloatValue(balldata,"x"),getFloatValue(balldata,"y"));
		Element bluerobotdata = (Element) data.getElementsByTagName("bluerobot").item(0); 
		//defining a blue robot object and passing the data
		Robot bluerobot = new Robot(new Point2D.Double(getFloatValue(bluerobotdata,"x"),getFloatValue(bluerobotdata,"y")), (double) getFloatValue(bluerobotdata,"angle") );
		Element yellowrobotdata = (Element)data.getElementsByTagName("yellowrobot").item(0); 
		//defining a yellow robot object and passing the data
		Robot yellowrobot = new Robot(new Point2D.Double(getFloatValue(yellowrobotdata,"x"),getFloatValue(yellowrobotdata,"y")), (double) getFloatValue(yellowrobotdata,"angle") );
		state = new WorldState(ballpos, bluerobot, yellowrobot, image);
		return state;
	}
	
	public static void main(String[] args){
		ArrayList<WorldState> Annotations = getWorldStateFromDocument(getDocumentFromXML("xml/fakedocument.xml"));
		
		for (WorldState state : Annotations){	
			System.out.printf("Getting Vision WorldState\n");
			WorldState visionimage;
			Test test =new Test();
			BufferedImage frame;
			frame =  state.getWorldImage();
			visionimage = test.worldImageData(frame);
			System.out.println("Ball Data");
			System.out.printf("Manual location:");
			System.out.print(state.getBallCoords());
			System.out.printf("\nVision location:");
			System.out.print(visionimage.getBallCoords());
			System.out.println();

		}
			
	}


}
