package sdp.common.filefilters;

import java.io.File;

import java.io.FileFilter;


/**
 * A file filter that shows only images.
 * 
 * @author Gediminas Liktaras
 */
public class ImageFileFilter_IO implements FileFilter {
	
	/** Image extensions to accept. */
	private static final String[] SUPPORTED_EXTENSIONS = {
		".bmp", ".jpeg", ".jpg", ".png", "ppm"
	};
	

	/**
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return false;
		}
		
		String filename = file.getAbsolutePath();
		for (String ext : SUPPORTED_EXTENSIONS) {
			if (filename.endsWith(ext)) {
				return true;
			}
		}
		
		return false;
	}

}
