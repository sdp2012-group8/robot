package sdp.vision;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * This class contains a selection of methods for performing I/O on
 * ImageProcessorConfiguration class objects.
 * 
 * @author Gediminas Liktaras
 */
public class ImageProcessorConfigIO {
	
	/** Class's logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.vision.ImageProcessorConfigIO");
	
	/** Local instance of DocumentBuilderFactory. */
	private static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	/** Local instance of TransformerFactory. */
	private static TransformerFactory tFactory = TransformerFactory.newInstance();
	
	
	/**
	 * Reads an image processor configuration from a file and returns it as
	 * a ImageProcessorConfiguration instance.
	 * 
	 * @param filename Name of the file that contains the configuration.
	 * @return An appropriate ImageProcessorConfiguration instance.
	 */
	public static ImageProcessorConfig loadConfiguration(String filename) {
		File configFile = new File(filename);
		if (!configFile.exists()) {
			LOGGER.info("The given image processor configuration file does not exist.");
			return null;
		}
				
		Document doc = openXmlDocument(configFile);		
		ImageProcessorConfig config = new ImageProcessorConfig();
		
		Element rootElement = (Element)doc.getElementsByTagName("config").item(0);
		
		Element fieldElement = (Element)rootElement.getElementsByTagName("field").item(0);
		config.setRawFieldLowX(getChildDouble(fieldElement, "lowX"));
		config.setRawFieldLowY(getChildDouble(fieldElement, "lowY"));
		config.setRawFieldHighX(getChildDouble(fieldElement, "highX"));
		config.setRawFieldHighY(getChildDouble(fieldElement, "highY"));
		
		Element ballElement = (Element)rootElement.getElementsByTagName("ball").item(0);
		config.setBallHueMinValue(getChildInt(ballElement, "hueMin"));
		config.setBallHueMaxValue(getChildInt(ballElement, "hueMax"));
		config.setBallSatMinValue(getChildInt(ballElement, "satMin"));
		config.setBallSatMaxValue(getChildInt(ballElement, "satMax"));
		config.setBallValMinValue(getChildInt(ballElement, "valMin"));
		config.setBallValMaxValue(getChildInt(ballElement, "valMax"));
		config.setBallSizeMinValue(getChildInt(ballElement, "sizeMin"));
		config.setBallSizeMaxValue(getChildInt(ballElement, "sizeMax"));
		
		Element blueElement = (Element)rootElement.getElementsByTagName("blue").item(0);
		config.setBlueHueMinValue(getChildInt(blueElement, "hueMin"));
		config.setBlueHueMaxValue(getChildInt(blueElement, "hueMax"));
		config.setBlueSatMinValue(getChildInt(blueElement, "satMin"));
		config.setBlueSatMaxValue(getChildInt(blueElement, "satMax"));
		config.setBlueValMinValue(getChildInt(blueElement, "valMin"));
		config.setBlueValMaxValue(getChildInt(blueElement, "valMax"));
		config.setBlueSizeMinValue(getChildInt(blueElement, "sizeMin"));
		config.setBlueSizeMaxValue(getChildInt(blueElement, "sizeMax"));
		
		Element yellowElement = (Element)rootElement.getElementsByTagName("yellow").item(0);
		config.setYellowHueMinValue(getChildInt(yellowElement, "hueMin"));
		config.setYellowHueMaxValue(getChildInt(yellowElement, "hueMax"));
		config.setYellowSatMinValue(getChildInt(yellowElement, "satMin"));
		config.setYellowSatMaxValue(getChildInt(yellowElement, "satMax"));
		config.setYellowValMinValue(getChildInt(yellowElement, "valMin"));
		config.setYellowValMaxValue(getChildInt(yellowElement, "valMax"));
		config.setYellowSizeMinValue(getChildInt(yellowElement, "sizeMin"));
		config.setYellowSizeMaxValue(getChildInt(yellowElement, "sizeMax"));
		
		return config;
	}
	
	
	/**
	 * Writes the given image processor configuration into an XML file.
	 * 
	 * @param config Configuration to output.
	 * @param filename Output filename.
	 */
	public static void saveConfiguration(ImageProcessorConfig config, String filename) {
		Document doc = createBlankXmlDocument();
		
		Element rootElement = doc.createElement("config");
		doc.appendChild(rootElement);
		
		Element fieldElement = doc.createElement("field");
		addChildDouble(doc, fieldElement, "lowX", config.getRawFieldLowX());
		addChildDouble(doc, fieldElement, "lowY", config.getRawFieldLowY());
		addChildDouble(doc, fieldElement, "highX", config.getRawFieldHighX());
		addChildDouble(doc, fieldElement, "highY", config.getRawFieldHighY());
		rootElement.appendChild(fieldElement);
		
		Element ballElement = doc.createElement("ball");
		addChildInt(doc, ballElement, "hueMin", config.getBallHueMinValue());
		addChildInt(doc, ballElement, "hueMax", config.getBallHueMaxValue());
		addChildInt(doc, ballElement, "satMin", config.getBallSatMinValue());
		addChildInt(doc, ballElement, "satMax", config.getBallSatMaxValue());
		addChildInt(doc, ballElement, "valMin", config.getBallValMinValue());
		addChildInt(doc, ballElement, "valMax", config.getBallValMaxValue());
		addChildInt(doc, ballElement, "sizeMin", config.getBallSizeMinValue());
		addChildInt(doc, ballElement, "sizeMax", config.getBallSizeMaxValue());
		rootElement.appendChild(ballElement);
		
		Element blueElement = doc.createElement("blue");
		addChildInt(doc, blueElement, "hueMin", config.getBlueHueMinValue());
		addChildInt(doc, blueElement, "hueMax", config.getBlueHueMaxValue());
		addChildInt(doc, blueElement, "satMin", config.getBlueSatMinValue());
		addChildInt(doc, blueElement, "satMax", config.getBlueSatMaxValue());
		addChildInt(doc, blueElement, "valMin", config.getBlueValMinValue());
		addChildInt(doc, blueElement, "valMax", config.getBlueValMaxValue());
		addChildInt(doc, blueElement, "sizeMin", config.getBlueSizeMinValue());
		addChildInt(doc, blueElement, "sizeMax", config.getBlueSizeMaxValue());
		rootElement.appendChild(blueElement);
		
		Element yellowElement = doc.createElement("yellow");
		addChildInt(doc, yellowElement, "hueMin", config.getYellowHueMinValue());
		addChildInt(doc, yellowElement, "hueMax", config.getYellowHueMaxValue());
		addChildInt(doc, yellowElement, "satMin", config.getYellowSatMinValue());
		addChildInt(doc, yellowElement, "satMax", config.getYellowSatMaxValue());
		addChildInt(doc, yellowElement, "valMin", config.getYellowValMinValue());
		addChildInt(doc, yellowElement, "valMax", config.getYellowValMaxValue());
		addChildInt(doc, yellowElement, "sizeMin", config.getYellowSizeMinValue());
		addChildInt(doc, yellowElement, "sizeMax", config.getYellowSizeMaxValue());
		rootElement.appendChild(yellowElement);
		
		writeXmlDocument(doc, filename);
	}
	
	
	/**
	 * Create and return new blank XML document.
	 * 
	 * @return New XML document.
	 */
	private static Document createBlankXmlDocument() {
		Document doc = null;
		try {
			doc = dbFactory.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			LOGGER.warning("ParserConfigurationException thrown when creating blank XML document.");
			e.printStackTrace();
		}
		
		return doc;
	}
	
	/**
	 * Open an XML document.
	 * 
	 * @param xmlFile File object of the XML document to open.
	 * @return Corresponding Document object, ready for use.
	 */
	private static Document openXmlDocument(File xmlFile) {
		Document doc = null;
		try {
			doc = dbFactory.newDocumentBuilder().parse(xmlFile);			
		} catch (SAXException e) {
			LOGGER.warning("SAXException thrown when reading image processor's configuration.");
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.warning("Could not open a file for reading XML document.");
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			LOGGER.warning("ParserConfigurationException thrown when reading image processor's configuration.");
			e.printStackTrace();
		}
		
		return doc;
	}
	
	/**
	 * Output an XML document to the given file.
	 * 
	 * @param doc XML document to write.
	 * @param filename File to write to.
	 */
	private static void writeXmlDocument(Document doc, String filename) {
		try {
			Transformer transformer = tFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new FileOutputStream(new File(filename)));
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			LOGGER.warning("TransformerConfigurationException thrown when writing XML document to a file.");
			e.printStackTrace();
		} catch (FileNotFoundException e1) {
			LOGGER.warning("Could open a file from writing an XML document.");
			e1.printStackTrace();
		} catch (TransformerException e) {
			LOGGER.warning("Transformer exception thrown when writing XML document to a file.");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get the text contents of the specified child node as a double.
	 * 
	 * @param elem Current node.
	 * @param childName Child node to get text out of.
	 * @return Child's text contents as a double.
	 */
	private static double getChildDouble(Element elem, String childName) {
		double value = 0.0;
		try {
			value = Double.valueOf(getChildText(elem, childName)).doubleValue();
		} catch (NumberFormatException e) {
			LOGGER.warning("Bad number format when reading image processor configuration.");
			e.printStackTrace();
		}
		
		return value;
	}
	
	/**
	 * Get the text contents of the specified child node as an int.
	 * 
	 * @param elem Current node.
	 * @param childName Child node to get text out of.
	 * @return Child's text contents as an int.
	 */
	private static int getChildInt(Element elem, String childName) {
		int value = 0;
		try {
			value = Integer.valueOf(getChildText(elem, childName)).intValue();
		} catch (NumberFormatException e) {
			LOGGER.warning("Bad number format when reading image processor configuration.");
			e.printStackTrace();
		}
		
		return value;
	}
	
	/**
	 * Get the text contents of the specified child node.
	 * 
	 * @param elem Current node.
	 * @param childName Child node to get text out of.
	 * @return Child's text contents.
	 */
	private static String getChildText(Element elem, String childName) {
		return elem.getElementsByTagName(childName).item(0).getTextContent();
	}
	
	
	/**
	 * Add a child element, whose text contents are the given double value.
	 * 
	 * @param doc Document object, used to build new elements.
	 * @param elem Element to append new child to.
	 * @param childName Name of the new child element.
	 * @param childText Text contents of the new child element.
	 */
	private static void addChildDouble(Document doc, Element elem, String childName,
			double childValue) {
		addChildText(doc, elem, childName, Double.toString(childValue));
	}
	
	/**
	 * Add a child element, whose text contents are the given int value.
	 * 
	 * @param doc Document object, used to build new elements.
	 * @param elem Element to append new child to.
	 * @param childName Name of the new child element.
	 * @param childText Text contents of the new child element.
	 */
	private static void addChildInt(Document doc, Element elem, String childName,
			int childValue) {
		addChildText(doc, elem, childName, Integer.toString(childValue));
	}
	
	/**
	 * Add a child element with the given text contents to the specified element.
	 * 
	 * @param doc Document object, used to build new elements.
	 * @param elem Element to append new child to.
	 * @param childName Name of the new child element.
	 * @param childText Text contents of the new child element.
	 */
	private static void addChildText(Document doc, Element elem, String childName,
			String childText) {
		Element newChild = doc.createElement(childName);
		newChild.setTextContent(childText);
		elem.appendChild(newChild);
	}
	
}
