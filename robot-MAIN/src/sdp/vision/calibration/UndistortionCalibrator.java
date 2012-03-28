package sdp.vision.calibration;

import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import sdp.common.filefilters.ImageFileFilter_IO;


/**
 * Processes a set of images and generate intristic and distortion
 * coefficients for the camera. 
 * 
 * @author Gediminas Liktaras
 */
public class UndistortionCalibrator {
	
	/** The class' logger. */
	private static final Logger LOGGER = Logger.getLogger("sdp.gui.MainWindow");
	
	
	/**
	 * Reads an returns images in the calibration image directory.
	 * 
	 * ImageIO is used for reading image files to reduce the number of
	 * external dependencies. Our compiled OpenCV libraries require specific
	 * image libraries for cvLoadImage functionality.
	 * 
	 * @return Calibration images.
	 * @throws IOException Thrown if there are any I/O issues.
	 */
	private IplImage[] readImages(String dir) throws IOException {
		File[] files = null;
		try {
			files = (new File(dir)).listFiles(new ImageFileFilter_IO());
		} catch (NullPointerException e) {
			throw new IOException("Tried to access non-existent directory.");
		}
		
		if ((files == null) || (files.length == 0)) {
			throw new IOException("No images found in the specified directory.");
		}
		
		IplImage[] images = new IplImage[files.length];
		for (int i = 0; i < files.length; ++i) {
			BufferedImage bi = ImageIO.read(files[i]);
			images[i] = IplImage.createFrom(bi);
		}
		
		return images;
	}
	
	/**
	 * Find all chessboard corners in a set of calibration images.
	 * 
	 * @param images Calibration images.
	 * @param width Number of chessboard corners along board's width.
	 * @param length Number of chessboard corners along board's height.
	 * @return A list with all corners in the images.
	 */
	private ArrayList<CvPoint2D32f> findCorners(IplImage[] images, int width, int length) {
		ArrayList<CvPoint2D32f> allCorners = new ArrayList<CvPoint2D32f>();
		
		CvSize boardSize = cvSize(width, length);
		int cornerCount = width * length;
		
		for (IplImage image : images) {
			IplImage grayImage = cvCreateImage(cvGetSize(image), 8, 1);
			
			CvPoint2D32f curCorners = new CvPoint2D32f(width * length);
			int[] curCornerCount = new int[] { 0 };
			
			int found = cvFindChessboardCorners(image, boardSize, curCorners,
					curCornerCount, CV_CALIB_CB_ADAPTIVE_THRESH | CV_CALIB_CB_NORMALIZE_IMAGE);
			
			cvCvtColor(image, grayImage, CV_BGR2GRAY);
			cvFindCornerSubPix(grayImage, curCorners, found, cvSize(11, 11), cvSize(-1, -1),
					cvTermCriteria(CV_TERMCRIT_EPS | CV_TERMCRIT_ITER, 30, 0.1));
			
			if ((found < 0) || (curCornerCount[0] != cornerCount)) {
				continue;
			}

			allCorners.add(curCorners);
		}
		
		return allCorners;
	}
	
	/**
	 * Compute camera distortion coefficients.
	 * 
	 * @param corners Chessboard corners in the images.
	 * @param imageSize Size of the calibration images.
	 * @param width Number of chessboard corners along board's width.
	 * @param length Number of chessboard corners along board's height.
	 * @param intristic Matrix that will store intristic coefficients.
	 * @param distortion Matrix that will store distortion coefficients.
	 */
	private void findCoefficients(ArrayList<CvPoint2D32f> corners, CvSize imageSize,
			int width, int length, CvMat intristic, CvMat distortion) {
		if (corners.size() == 0) {
			LOGGER.warning("No chessboard corners have been found in the images.");
		}
		
		int cornerCount = width * length;

		CvMat imagePoints = CvMat.create(cornerCount * corners.size(), 2, CV_32FC1);
		CvMat objectPoints = CvMat.create(cornerCount * corners.size(), 3, CV_32FC1);
		CvMat pointCounts = CvMat.create(corners.size(), 1, CV_32SC1);
		
		for (int i = 0; i < corners.size(); ++i) {
			for (int j = 0; j < cornerCount; ++j) {
				int idx = i * cornerCount + j;
				imagePoints.put(idx, 0, corners.get(i).position(j).x());
				imagePoints.put(idx, 1, corners.get(i).position(j).y());
				objectPoints.put(idx, 0, j / width);
				objectPoints.put(idx, 1, j % width);
				objectPoints.put(idx, 2, 0);
			}
			pointCounts.put(i, cornerCount);
		}
		
		cvCalibrateCamera2(objectPoints, imagePoints, pointCounts, imageSize, intristic,
				distortion, null, null, 0);
	}
	
	
	/**
	 * Calibrate the camera using the images in the specified directory.
	 * 
	 * Image are all expected to be of the same size.
	 * 
	 * @param dir Directory with calibration images.
	 * @param width Number of chessboard corners along board's width.
	 * @param length Number of chessboard corners along board's height.
	 * @param intristic Matrix that will store intristic coefficients.
	 * @param distortion Matrix that will store distortion coefficients.
	 * @throws IOException Thrown if there are any I/O issues.
	 */
	private void calibrate(String dir, int width, int length, CvMat intristic,
			CvMat distortion) throws IOException {
		IplImage images[] = readImages(dir);
		CvSize imageSize = images[0].cvSize();
		
		ArrayList<CvPoint2D32f> corners = findCorners(images, width, length);
		findCoefficients(corners, imageSize, width, length, intristic, distortion);
	}
	
	
	/**
	 * The entry point.
	 * 
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		CvMat intristic = CvMat.create(3, 3);
		CvMat distortion = CvMat.create(8, 1);
		
		UndistortionCalibrator calibrator = new UndistortionCalibrator();		
		try {
			calibrator.calibrate("data/images/checkers main 2", 9, 6,
					intristic, distortion);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		System.out.println("Intristic coefficients:");
		System.out.println("f_x = " + intristic.get(0, 0));
		System.out.println("f_y = " + intristic.get(1, 1));
		System.out.println("c_x = " + intristic.get(0, 2));
		System.out.println("c_y = " + intristic.get(1, 2));
		System.out.println();
		System.out.println("Distortion coefficients:");
		System.out.println("k_1 = " + distortion.get(0));
		System.out.println("k_2 = " + distortion.get(1));
		System.out.println("p_1 = " + distortion.get(2));
		System.out.println("p_2 = " + distortion.get(3));
		System.out.println("k_3 = " + distortion.get(4));
		System.out.println("k_4 = " + distortion.get(5));
		System.out.println("k_5 = " + distortion.get(6));
		System.out.println("k_6 = " + distortion.get(7));
	}
}
