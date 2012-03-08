package sdp.gui.filefilters;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A file filter that shows only text files.
 * 
 * @author Gediminas Liktaras
 */
public class TextFileFilter_FC extends FileFilter {

	/**
	 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		
		String filename = file.getAbsolutePath();
		if (filename.endsWith(".txt")) {
			return true;
		}
		
		return false;
	}

	/**
	 * @see javax.swing.filechooser.FileFilter#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Text files";
	}

}
