package sdp.common.xml;

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

import sdp.common.Utilities;


/**
 * A collection of methods for managing XML DOM trees.
 * 
 * @author Gediminas Liktaras
 */
public class XmlUtils {
	
	/** Class's logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.common.XmlUtils");
	
	
	/**
	 * Create and return new blank XML document.
	 * 
	 * @return New XML document.
	 */
	public static Document createBlankXmlDocument() {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
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
	public static Document openXmlDocument(File xmlFile) {
		Document doc = null;
		try {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile);			
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
	public static void writeXmlDocument(Document doc, String filename) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
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
	 * Get the text contents of the specified child node as a boolean.
	 * 
	 * @param elem Current node.
	 * @param childName Child node to get text out of.
	 * @return Child's text contents as a boolean.
	 */
	public static boolean getChildBoolean(Element elem, String childName) {
		return Boolean.valueOf(getChildText(elem, childName)).booleanValue();
	}
	
	/**
	 * Get the text contents of the specified child node as a double.
	 * 
	 * @param elem Current node.
	 * @param childName Child node to get text out of.
	 * @return Child's text contents as a double.
	 */
	public static double getChildDouble(Element elem, String childName) {
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
	public static int getChildInt(Element elem, String childName) {
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
	public static String getChildText(Element elem, String childName) {
		String content = elem.getElementsByTagName(childName).item(0).getTextContent();
		return Utilities.stripString(content);
	}
	
	
	/**
	 * Add a child element, whose text contents are the given boolean value.
	 * 
	 * @param doc Document object, used to build new elements.
	 * @param elem Element to append new child to.
	 * @param childName Name of the new child element.
	 * @param childValue Value of the new child element.
	 */
	public static void addChildBoolean(Document doc, Element elem, String childName,
			boolean childValue) {
		addChildText(doc, elem, childName, Boolean.toString(childValue));
	}
	
	/**
	 * Add a child element, whose text contents are the given double value.
	 * 
	 * @param doc Document object, used to build new elements.
	 * @param elem Element to append new child to.
	 * @param childName Name of the new child element.
	 * @param childValue Value of the new child element.
	 */
	public static void addChildDouble(Document doc, Element elem, String childName,
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
	public static void addChildInt(Document doc, Element elem, String childName,
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
	public static void addChildText(Document doc, Element elem, String childName,
			String childText) {
		Element newChild = doc.createElement(childName);
		newChild.setTextContent(childText);
		elem.appendChild(newChild);
	}
}
