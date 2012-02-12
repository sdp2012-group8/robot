package sdp.vision;

import javax.swing.JFrame;
import java.awt.image.BufferedImage;
import java.io.*;
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
import sdp.vision.Viewer;
import sdp.vision.processing.ImageProcessorConfig;
import sdp.common.Utilities;

public class Test extends Vision {
	//Configuration used to convert between relative coordinates and Pixel-Range coordinates
	static ImageProcessorConfig config = new ImageProcessorConfig();
	static ArrayList<String> filelist = new ArrayList<String>();

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

	//returns the text value contained within an XML element
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

	//return the float value contained within an XML element after first reading it's plaintext.
	private static float getFloatValue(Element ele, String tagName) {
		//in production application you would catch the exception
		return Float.parseFloat(getTextValue(ele,tagName));
	}

	//returns a WorldState object with the data from one element of the xml file
	private static WorldState getWorldStateFromElement(Element ws){

		WorldState state;
		//SLIGHT XML NAVIGATION
		//image -> filename
		String filename = getTextValue(ws,"filename");
		filelist.add(filename);


		//attempting to load image from file
		BufferedImage image = null;
		try{
			//filename referenced in XML is loaded as a BufferedImage
			image = ImageIO.read(new File(filename));
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
		Point2D.Double ballpos = new Point2D.Double(getFloatValue(balldata,"x"),getFloatValue(balldata,"y"));

		// location-data -> bluerobot
		Element bluerobotdata = (Element) data.getElementsByTagName("bluerobot").item(0); 

		//defining a blue robot object and passing it bluerobot -> x, bluerobot -> y and bluerobot -> angle
		Robot bluerobot = new Robot(new Point2D.Double(getFloatValue(bluerobotdata,"x"),getFloatValue(bluerobotdata,"y")), (double) getFloatValue(bluerobotdata,"angle") );

		// location-data -> yellowrobot
		Element yellowrobotdata = (Element)data.getElementsByTagName("yellowrobot").item(0); 

		//defining a yellow robot object and passing it yellowrobot -> x, yellowrobot -> y and yellowrobot -> angle
		Robot yellowrobot = new Robot(new Point2D.Double(getFloatValue(yellowrobotdata,"x"),getFloatValue(yellowrobotdata,"y")), (double) getFloatValue(yellowrobotdata,"angle") );

		//WorldState object is created with parsed data passed to the constructor. 

		state = new WorldState(ballpos, bluerobot, yellowrobot, image);

		//WorldState object is returned to whatever called this method.
		return state;
	}

	//Converts all coordinates in a WorldState to the de-normalised versions for use when drawing onto pixel locations.
	public static WorldState convertToPixelRange(WorldState ws){
		Point2D.Double ball = new Point2D.Double(correctX(ws.getBallCoords().x),correctY(ws.getBallCoords().y));
		Robot blue = new Robot(new Point2D.Double(correctX(ws.getBlueRobot().getCoords().x),correctY(ws.getBlueRobot().getCoords().y)),ws.getBlueRobot().getAngle());
		Robot yellow = new Robot(new Point2D.Double(correctX(ws.getYellowRobot().getCoords().x),correctY(ws.getYellowRobot().getCoords().y)),ws.getYellowRobot().getAngle());

		//New WorldState is created by converting all components and then creating a new WorldState
		WorldState ns = new WorldState(ball,blue,yellow,ws.getWorldImage());
		//Pixel-Range WorldState is returned
		return ns;
	}

	//Used for de-normalising the x component of coordinates.
	public static int correctX (double x){
		x*=config.getFieldWidth();
		x+=config.getFieldLowX();
		return (int) x;
	}
	//Used for de-normalising the y component of coordinates.
		public static int correctY (double y){
		y*=config.getFieldWidth();
		y+=config.getFieldLowY();
		return (int) y;
	}

	public static void printMarginsOfError( IterativeWorldStateDifferenceAccumulator difference, ArrayList<WorldState> annotations){
		//Error details are generated
		String ballerror = "Average ball error is " + ((float)difference.averageBallError(annotations.size())) + " pixels.";
		String blueerror = "Average blue robot error is " + ((float)difference.averageBlueError(annotations.size())) + " pixels.";
		String yellowerror = "Average yellow robot error is " + ((float)difference.averageYellowError(annotations.size())) + " pixels.";

		//And output to the terminal
		System.out.println(ballerror);
		System.out.println(blueerror);
		System.out.println(yellowerror);

		//Also written to a file that can be pushed to the repository for metric tracking over time.
		try{
			  FileWriter fw = new FileWriter("metrics.txt");
			  BufferedWriter out = new BufferedWriter(fw);

			  //Number of pixels difference to ignore
			  int tolerance = 20;
			  out.append("Error tolerance is set to "+tolerance+" pixels.\n\n");
			  //Filenames in order so that the cause of high error rates can be investigated
			  out.append("Files used in tests:\n");
			  int index = 0;
			  for (String file : filelist){
				  index++;
				  out.append(index+": ");
				  out.append(file+"\n");
			  }
			  out.append("\n");
			  out.append(ballerror+"\n");
			  index = 0;
			  for (float error : difference.balllist){
				  index++;
				  out.append(index+": ");
				  if (error > tolerance){
				   out.append("[ABOVE TOLERANCE] ");
				  }
				  out.append(error+"\n");
			  }
			  out.append("\n");

			  out.append(blueerror+"\n");
			  index = 0;
			  for (float error : difference.bluelist){
				  index++;
				  out.append(index+": ");
				  if (error > tolerance){
					   out.append("[ABOVE TOLERANCE] ");
					  }
				  out.append(error+"\n");
			  }
			  out.append("\n");

			  out.append(yellowerror+"\n");
			  index = 0;
			  for (float error : difference.yellowlist){
				  index++;
				  out.append(index+": ");
				  if (error > tolerance){
					   out.append("[ABOVE TOLERANCE] ");
					  }
				  out.append(error+"\n");
			  }
			  out.append("\n");

			  //File closed and write finalised
			  out.close();
			  System.out.println("Metrics written to metrics.txt");
		}catch (Exception e){

		}
	}

	public static void main(String[] args) throws InterruptedException{

		//Test constructor for methods
		Test test =new Test();

		//delay in ms between slides being shown.
		int delay = 0;

		//if visual output should be shown when iterating
		boolean visoutput = false;

		//The xml file (currently hard coded location) is parsed by the above voodoo and stored in an ArrayList<WorldState>
		ArrayList<WorldState> annotations = getWorldStateFromDocument(getDocumentFromXML("xml/imagedata.xml"));

		//Init display if showing output
		JFrame frame = null;
		Viewer base = new Viewer(null, null, null);
		if (visoutput){
			frame = new JFrame("Image Display");
			frame.setSize(640,480);
			frame.getContentPane().add(base);
			frame.setVisible(true);
		}

		//Class that accumulates the difference between WorldStates is initialised.
		IterativeWorldStateDifferenceAccumulator difference = new IterativeWorldStateDifferenceAccumulator();

		//For each state documented in the XML
		for (WorldState state : annotations){
			System.out.println(state);

			//Copy of the manual image is made so that the original is not overwritten.
			Utilities utility = new Utilities();
			BufferedImage manualimage = utility.deepBufferedImageCopy(state.getWorldImage());

			//The comparative WorldState object that the vision system will construct.
			WorldState visionimage;

			//The vision system is passed the image from the annotation and generates
			//a WorldState to be compared with the human perception
			visionimage = convertToPixelRange(test.worldImageData(utility.deepBufferedImageCopy(state.getWorldImage())));

			//Update visual output if showing
			if (visoutput){
				base.updateImageAndState(manualimage,state,visionimage);
			}

			//Sleep if delay is configured
			Thread.sleep(delay);

			//Differences between the WorldStates are calculated
			difference.iteration(state, visionimage);
		}

		//Closes display if was open
		if (visoutput){
			frame.setVisible(false);
		}

		//Average errors over iterations are printed
		printMarginsOfError(difference,annotations);		
	}
}
