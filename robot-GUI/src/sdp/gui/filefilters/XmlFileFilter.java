package sdp.gui.filefilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * A file filter that shows only XML files.
 * 
 * @author Gediminas Liktaras
 */
public class XmlFileFilter extends FileFilter {

	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		
		String filename = file.getAbsolutePath();
		if (filename.endsWith(".xml")) {
			return true;
		}
		
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	@Override
	public String getDescription() {
		return "XML files";
	}

}
