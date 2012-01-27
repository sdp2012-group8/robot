// 
// Dear SDP 2012 student,
// 
// Once you are done trying to 'optimize' this class,
// and have realized what a terrible mistake that was,
// please increment the following counter as a warning
// to the next guy:
// 
// total_hours_wasted_here = 29
// 

package sdp.vision;

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.LinkedList;

import sdp.common.Robot;
import sdp.common.WorldState;
import sdp.common.Tools;

public class OldImageProcessor {

	public static int DEBUG_LEVEL = 2;

	// ---Mouse mode constants.
	public static int displX = 0;
	public static int displY = 0;
	public static boolean useMouse = false;

	// ---Constants for Colour Accessing
	public static int RED = 0;
	public static int GREEN = 1;
	public static int BLUE = 2;

	// ---Perfect Colours
	private int[] red = new int[] { 255, 0, 0 };
	private int[] yell = new int[] { 255, 255, 0 };
	private int[] blue = new int[] { 0, 0, 255 };

	protected static int height = 480;
	protected static int width = 640;

	// --- Barrel Distortion Correction
	private static final double barrelCorrectionX = -0.016;
	private static final double barrelCorrectionY = -0.06;

	public static boolean useBarrelDistortion = false;
	public static boolean cameraView = true;
	public static boolean testmode = true;

	
	public static int xlowerlimit = 0;
	public static int xupperlimit = 630;
	public static int ylowerlimit = 85;
	public static int yupperlimit = 410;

	public static int blueThreshold = 350;
	public static int yellThreshold = 150;

	public static int searchdistance = 200;
	public static int rayOfLight = 35;

	Point lastBallPos = new Point(-1, -1);

	// this will store ball coordinates
	Point btPos = new Point(-1, -1);
	Point ytPos = new Point(-1, -1);
	Point ourPos = new Point(-1, -1);

	public LinkedList<Point> lines = new LinkedList<Point>();
	public LinkedList<Integer> lineColor = new LinkedList<Integer>();
	
	BufferedImage image;
	public static int mode = 5;
	private int[] blueAngles = new int[mode]; // method 1
	private int[] yellAngles = new int[mode]; // method 1
	private int lastYellPos = -1; // method 2
	private int lastBluePos = -1; // method 2
	public static int method = 1; // method 1 for normalisation of 10 numbers, method 2
	// for last position checking.

	// ~~~~~OUTPUT FOR OTHER TEAMS~~~~~~//
	public WorldState worldState;

	/**
	 * @param image
	 * @return image
	 */
	public BufferedImage process(BufferedImage image) {

		// if (intercept.weAreBlueTeam)
		//if (Main2.weAreBlueTeam)
		if (true)
			ourPos = btPos;
		else
			ourPos = ytPos;
		// create raster from given image
		Raster data = null;
		try {
			data = image.getData();
		} catch (NullPointerException e) {
			System.out.println(e.toString());
			return null;
		}

		Point ballPos = new Point(-1, -1);

		// create a new blank raster of the same size
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		ColorModel cm = new ComponentColorModel(cs, false, false,
				Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
		WritableRaster wraster = data.createCompatibleWritableRaster();

		// this will be used to find red pixel (one!)
		int closestRedFound = 255 * 3;

		// this will be used to find blue robot centroid
		int btCentroidCount = 0;
		Point btCentroid = new Point(0, 0);

		// this for yellow
		int ytCentroidCount = 0;
		Point ytCentroid = new Point(0, 0);
		// ray of light processing
		for (int i = xlowerlimit; i < xupperlimit; i = i + 1) { // for every
			for (int j = ylowerlimit; j < yupperlimit; j = j + 1) {
				if (((i % 2 == 0) || (j % 2 == 0))
						&& (i < (ourPos.x - rayOfLight)
								|| i > (ourPos.x + rayOfLight)
								|| j < (ourPos.y - rayOfLight) || j > (ourPos.y + rayOfLight)))
					continue;

				// get RGB values for a pixel
				int[] pixelColour = new int[3];
				data.getPixel(i, j, pixelColour);

				// finds how red is a pixel
				int ballDifference = getColourDifference(red, pixelColour);

				// barrel distortion
				Point out = convertToBarrelCorrected(new Point(i, j));

				// if converted pixel is still within pict then set it on the
				// new raster
				if (DEBUG_LEVEL < 4)
					drawPixel(wraster, out, pixelColour);

				// this will try to find the "reddest" point. this works better
				// for the ball than centroid
				if (ballDifference < closestRedFound && pixelColour[RED] > 150
						&& pixelColour[GREEN] < 50) {
					closestRedFound = ballDifference;
					ballPos = out;
				}

				// Calculate Centroid of blue Co-ordinates
				if (isBlue(pixelColour)) {
					if (DEBUG_LEVEL > 1)
						drawPixel(wraster, out, blue);
					btCentroidCount++;
					btCentroid.x += out.x;
					btCentroid.y += out.y;
				}

				// Calculate Centroid of yellow Co-ordinates
				if (isYellow(pixelColour)) {
					if (DEBUG_LEVEL > 1)
						drawPixel(wraster, out, yell);
					ytCentroidCount++;
					ytCentroid.x += out.x;
					ytCentroid.y += out.y;
				}
				if (isBrightGreen(pixelColour)) {
					if (DEBUG_LEVEL > 1)
						drawPixel(wraster, convertToBarrelCorrected(new Point(
								i, j)), new int[] { 0, 255, 0 });
				}
			}
		}
		
		// TODO: Fix this bandaid.
		Point ballCoords;
		Robot blueRobot, yellowRobot;
		
		if (worldState != null) {
			ballCoords = worldState.getBallCoords();
			blueRobot = worldState.getBlueRobot();
			yellowRobot = worldState.getYellowRobot();
		} else {
			ballCoords = new Point(0, 0);
			blueRobot = new Robot(new Point(0, 0), 0.0);
			yellowRobot = new Robot(new Point(0, 0), 0.0);
		}

		// where 5 is just some minimal number of pixels found
		if (btCentroidCount > 5) {
			btPos = new Point(btCentroid.x / btCentroidCount, btCentroid.y
					/ btCentroidCount);
			int btAngle = findAngle(data, wraster, btPos, blue);
			blueRobot = new Robot(btPos, btAngle);
		}

		if (ytCentroidCount > 5) {
			ytPos = new Point(ytCentroid.x / ytCentroidCount, ytCentroid.y
					/ ytCentroidCount);
			int ytAngle = findAngle(data, wraster, ytPos, yell);
			yellowRobot = new Robot(ytPos, ytAngle);
		}
		if (useMouse) {
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			ballPos = new Point(mouse.x - 5 - displX, mouse.y - 50 - displY);
			drawCross(wraster, ballPos, red);
		} else {
			findBall(wraster, ballPos);
		}
		
		int lineColors = 0;
		if (DEBUG_LEVEL > 0)
			for (int i = 0; i < lines.size(); i += 2) {
				try {
					Color c = new Color(lineColor.get(lineColors));
					lineColors++;
					int color[] = new int[] { c.getRed(), c.getGreen(),
							c.getBlue() };
					try {
						drawLine(wraster, (Point) lines.get(i), (Point) lines
								.get(i + 1), color);
					} catch (NullPointerException e) {

					}
				} catch (IndexOutOfBoundsException e) {
					//
				}
			}

		BufferedImage img = new BufferedImage(cm, wraster, false, null);
		worldState = new WorldState(ballPos, blueRobot, yellowRobot, img);
		return img;
	}

	/**
	 * 
	 * @param raster
	 * @param ballPos
	 */
	private void findBall(WritableRaster raster, Point ballPos) {
		if (ballPos != null) { // if we can see the ball, initiate lastpos value
			if (lastBallPos.x == -1) {
				lastBallPos = ballPos;

			}
			// ignores the movement when it is higher than expected or very
			// small
			double ballmovement = Tools.getDistanceBetweenPoint(ballPos,
					lastBallPos);
			if (ballmovement < searchdistance && ballmovement > 3) {
				lastBallPos = ballPos;
			} else {
				ballPos = lastBallPos;
			}
			if (DEBUG_LEVEL > 0)
				drawCross(raster, ballPos, red);
		} else {
			// if we can't see the ball
			if (lastBallPos.x != -1) {
				// try last values
				ballPos = lastBallPos;
				if (DEBUG_LEVEL > 0)
					drawCross(raster, ballPos, red);
			}
			// and if we can't see the ball AND there are no previous values, do
			// nothing.
		}
	}

	/**
	 * This function attempts to find current robot rotation angle.
	 * 
	 * @param image
	 *            original image
	 * @param raster
	 *            writable raster
	 * @param p1
	 *            centre of the robot
	 * @param colour
	 *            of the robot
	 * @return angle in degrees
	 */
	private int findAngle(Raster image, WritableRaster raster, Point p1,
			int[] colour) {
		// x and y values of the centre of the robot
		int x = p1.x;
		int y = p1.y;
		// this defines x and y values of checkpoints around the centre of the
		// robot
		// they will be used to check if they are of an expected value,
		// specified in Ts (types)
		// 0 stands for 'T' sign colour (blue or yellow)
		// 1 stands for the dark spot
		// 2 stands for bright green
		int[] Xs = new int[] { x, x, x + 2, x - 2, x - 8, x + 8, x + 8, x - 8,
				x + 12, x - 12, x + 2, x - 2, x + 2, x - 2, x + 12, x + 12,
				x - 12, x - 12, x + 8, x - 8 };
		int[] Ys = new int[] { y, y - 8, y + 14, y + 14, y + 14, y + 14, y - 8,
				y - 8, y + 8, y + 8, y - 24, y - 24, y - 20, y - 20, y - 20,
				y + 18, y - 20, y + 18, y + 2, y + 2 };
		int[] Ts = new int[] { 0, 0, 0, 0, 2, 2, 0, 0, 2, 2, 1, 1, 1, 1, 2, 2,
				2, 2, 2, 2 };

		int maxscore = 0;
		int bestangle = -1;
		int len = Xs.length;
		int step3 = 1;

		// iterate over all 360 degrees, jumping by every 'step' degrees.
		// step=1 might be tempting, but this is not that accurate anyway
		for (int angle = 0; angle < 360; angle += step3) {
			int score = 0; // a score is given to each tested angle, later we
			// check which was best
			int dark = 0;
			for (int i = 0; i < len; i++) // iterate over all checkpoints
			{
				// create a new, rotated, temporary point
				Point tmp = Tools.rotatePoint(new Point(x, y), new Point(Xs[i],
						Ys[i]), angle);
				int[] pointcolour = new int[3];
				// try to read the colour of the pixel under our rotated
				// checkpoint
				try {
					image.getPixel(tmp.x, tmp.y, pointcolour);
				} catch (ArrayIndexOutOfBoundsException e) {
					continue; // just go to next
				}
				// see which type of pixel should it be
				switch (Ts[i]) {
				case 0: // T sign: blue/yell
					if (colour.equals(blue)) {
						if (isBlue(pointcolour)) {
							score += 2;
						}
					} else {
						if (isYellow(pointcolour)) {
							score += 2;
						}
					}
					break;
				case 1: // dark dot
					int brightness = pointcolour[0] + pointcolour[1]
							+ pointcolour[2];
					if (brightness < 240) {
						score += 1;
						dark++;
					}
					break;
				case 2: // plate: bright green
					if (isBrightGreen(pointcolour)) {
						score++;
					}
					break;
				default:
					break;
				}
			}
			// if checkpoints at this angle scored higher than current max,
			// update.
			if (score > maxscore) {
				maxscore = score;
				bestangle = angle;
			}
		}

		int ignoreStep = 8;
		if (colour.equals(blue) && method == 1) {
			blueAngles = Tools.push(blueAngles, bestangle);
			bestangle = Tools.goodAvg(blueAngles);
		} else if (colour.equals(yell) && method == 1) {
			yellAngles = Tools.push(yellAngles, bestangle);
			bestangle = Tools.goodAvg(yellAngles);
		} else if (colour.equals(blue) && method == 2) {
			if (lastBluePos == -1) {
				lastBluePos = bestangle;
			} else if (lastBluePos + ignoreStep < bestangle
					|| lastBluePos - ignoreStep > bestangle) {
				lastBluePos = bestangle;
			} else {
				bestangle = lastBluePos;
			}
		} else if (colour.equals(yell) && method == 2) {
			if (lastYellPos == -1) {
				lastYellPos = bestangle;
			} else if (lastYellPos + ignoreStep < bestangle
					|| lastYellPos - ignoreStep > bestangle) {
				lastYellPos = bestangle;
			} else {
				bestangle = lastYellPos;
			}
		}
//		if (DEBUG_LEVEL > 3) {
//			if (colour.equals(yell)) {
//				GUI.setDebugOutputYell("Yell robot at x:" + x + ", y:" + y
//						+ " Angle:" + bestangle);
//				// System.out.println("Yellow robot at "+x+":"+y+" Angle:"+bestangle);
//			} else {
//				GUI.setDebugOutputBlue("Blue robot at x:" + x + ", y:" + y
//						+ " Angle:" + bestangle);
//				// System.out.println("Blue robot at "+x+":"+y+" Angle:"+bestangle);
//			}
//		}

		if (DEBUG_LEVEL > 2) {
			// this will mark the checkpoints
			for (int i = 0; i < len; i++) {
				Point tmp = Tools.rotatePoint(new Point(x, y), new Point(Xs[i],
						Ys[i]), bestangle);
				drawLittleCross(raster, convertToBarrelCorrected(tmp), red);
			}
		}
		if (DEBUG_LEVEL > 0) {
			// this will dr95aw a box over the robot
			Point corner1 = Tools.rotatePoint(new Point(x, y), new Point(
					Xs[14], Ys[14] - 6), bestangle);
			Point corner2 = Tools.rotatePoint(new Point(x, y), new Point(
					Xs[15], Ys[15]), bestangle);
			Point corner3 = Tools.rotatePoint(new Point(x, y), new Point(
					Xs[16], Ys[16] - 6), bestangle);
			Point corner4 = Tools.rotatePoint(new Point(x, y), new Point(
					Xs[17], Ys[17]), bestangle);

			drawLine(raster, corner1, corner2, red);
			drawLine(raster, corner2, corner4, red);
			drawLine(raster, corner4, corner3, red);
			drawLine(raster, corner3, corner1, red);

			corner1 = Tools.rotatePoint(new Point(x + 1, y + 1), new Point(
					Xs[14], Ys[14] - 6), bestangle);
			corner2 = Tools.rotatePoint(new Point(x + 1, y + 1), new Point(
					Xs[15], Ys[15]), bestangle);
			corner3 = Tools.rotatePoint(new Point(x + 1, y + 1), new Point(
					Xs[16], Ys[16] - 6), bestangle);
			corner4 = Tools.rotatePoint(new Point(x + 1, y + 1), new Point(
					Xs[17], Ys[17]), bestangle);

			drawLine(raster, corner1, corner2, red);
			drawLine(raster, corner2, corner4, red);
			drawLine(raster, corner4, corner3, red);
			drawLine(raster, corner3, corner1, red);

			corner1 = Tools.rotatePoint(new Point(x - 1, y - 1), new Point(
					Xs[14], Ys[14] - 6), bestangle);
			corner2 = Tools.rotatePoint(new Point(x - 1, y - 1), new Point(
					Xs[15], Ys[15]), bestangle);
			corner3 = Tools.rotatePoint(new Point(x - 1, y - 1), new Point(
					Xs[16], Ys[16] - 6), bestangle);
			corner4 = Tools.rotatePoint(new Point(x - 1, y - 1), new Point(
					Xs[17], Ys[17]), bestangle);

			drawLine(raster, corner1, corner2, red);
			drawLine(raster, corner2, corner4, red);
			drawLine(raster, corner4, corner3, red);
			drawLine(raster, corner3, corner1, red);

			corner1 = Tools.rotatePoint(new Point(x + 1, y - 1), new Point(
					Xs[14], Ys[14] - 6), bestangle);
			corner2 = Tools.rotatePoint(new Point(x + 1, y - 1), new Point(
					Xs[15], Ys[15]), bestangle);
			corner3 = Tools.rotatePoint(new Point(x + 1, y - 1), new Point(
					Xs[16], Ys[16] - 6), bestangle);
			corner4 = Tools.rotatePoint(new Point(x + 1, y - 1), new Point(
					Xs[17], Ys[17]), bestangle);

			drawLine(raster, corner1, corner2, red);
			drawLine(raster, corner2, corner4, red);
			drawLine(raster, corner4, corner3, red);
			drawLine(raster, corner3, corner1, red);

			corner1 = Tools.rotatePoint(new Point(x - 1, y + 1), new Point(
					Xs[14], Ys[14] - 6), bestangle);
			corner2 = Tools.rotatePoint(new Point(x - 1, y + 1), new Point(
					Xs[15], Ys[15]), bestangle);
			corner3 = Tools.rotatePoint(new Point(x - 1, y + 1), new Point(
					Xs[16], Ys[16] - 6), bestangle);
			corner4 = Tools.rotatePoint(new Point(x - 1, y + 1), new Point(
					Xs[17], Ys[17]), bestangle);

			drawLine(raster, corner1, corner2, red);
			drawLine(raster, corner2, corner4, red);
			drawLine(raster, corner4, corner3, red);
			drawLine(raster, corner3, corner1, red);
		}

		return (360 + 270 - bestangle) % 360;

	}

	/*
	 * Basic Functions
	 */
	/**
	 * The following three functions check if a given colour is classified as
	 * bright green/yellow/blue respectively.
	 */
	boolean isBrightGreen(int[] colour) {
		return (colour[RED] < 65 && colour[GREEN] > 180 && colour[BLUE] < 170);
	}

	boolean isYellow(int[] colour) {
		int ytDifference = getColourDifference(yell, colour);
		return (ytDifference < yellThreshold && colour[RED] > 150 && colour[GREEN] > 170);
	}

	boolean isBlue(int[] colour) {
		int btDifference = getColourDifference(blue, colour);
		return (btDifference < blueThreshold && colour[BLUE] > 130);
	}

	/**
	 * Barrel Distortion Correction 'straightens' the distorted image.
	 * But doesn't quite work properly. But it is not really needed anyway.
	 * 
	 * @param p1
	 *            point coordinates
	 * @return if useBarrelDistorion is false returns p1, otherwise, returns
	 *         adjusted coordinates
	 */
	public Point convertToBarrelCorrected(Point p1) {
		if (!useBarrelDistortion)
			return p1;
		// first normalise pixel
		double px = (2 * p1.x - width) / (double) width;
		double py = (2 * p1.y - height) / (double) height;

		// then compute the radius of the pixel you are working with
		double rad = px * px + py * py;

		// then compute new pixel'
		double px1 = px * (1 - barrelCorrectionX * rad);
		double py1 = py * (1 - barrelCorrectionY * rad);

		// then convert back
		int pixi = (int) ((px1 + 1) * width / 2);
		int pixj = (int) ((py1 + 1) * height / 2);
		// System.out.println("New Pixel: (" + pixi + ", " + pixj + ")");
		return new Point(pixi, pixj);
	}

	public static Point barrelCorrected(Point p1) {
		// System.out.println("Pixel: (" + x + ", " + y + ")");
		// first normalise pixel
		double px = (2 * p1.x - width) / (double) width;
		double py = (2 * p1.y - height) / (double) height;

		// System.out.println("Norm Pixel: (" + px + ", " + py + ")");
		// then compute the radius of the pixel you are working with
		double rad = px * px + py * py;

		// then compute new pixel'
		double px1 = px * (1 - barrelCorrectionX * rad);
		double py1 = py * (1 - barrelCorrectionY * rad);

		// then convert back
		int pixi = (int) ((px1 + 1) * width / 2);
		int pixj = (int) ((py1 + 1) * height / 2);
		// System.out.println("New Pixel: (" + pixi + ", " + pixj + ")");
		return new Point(pixi, pixj);
	}

	/**
	 * Calculate difference between two colours
	 * 
	 * @param colour1
	 * @param colour2
	 * @return absolute difference
	 */
	public int getColourDifference(int[] colour1, int[] colour2) {
		return Math.abs(colour1[RED] - colour2[RED])
				+ Math.abs(colour1[GREEN] - colour2[GREEN])
				+ Math.abs(colour1[BLUE] - colour2[BLUE]);
	}

	/**
	 * Draw line from p1 to p2 of specified colour on given raster.
	 * 
	 * @param raster
	 *            draw on writable raster
	 * @param p1
	 *            start point
	 * @param p2
	 *            end point
	 * @param colour
	 *            - colour as three integers
	 */
	public void drawLine(WritableRaster raster, Point p1, Point p2, int[] colour) {
		int x0 = p1.x;
		int y0 = p1.y;
		int x1 = p2.x;
		int y1 = p2.y;
		int dx = x1 - x0;
		int dy = y1 - y0;
		drawPixel(raster, new Point(x0, y0), colour);
		if (dx != 0) {
			float m = (float) dy / (float) dx;
			float b = y0 - m * x0;
			dx = (x1 > x0) ? 1 : -1;
			while (x0 != x1) {
				x0 += dx;
				y0 = Math.round(m * x0 + b);
				drawPixel(raster, new Point(x0, y0), colour);
			}
		}
	}

	public void addLineToBeDrawn(Point p1, Point p2, int color) {
		lines.add(p1);
		lines.add(p2);
		lineColor.add(color);
	}

	/**
	 * A safe way to draw a pixel
	 * 
	 * @param raster
	 *            draw on writable raster
	 * @param p1
	 *            point coordinates
	 * @param colour
	 *            colour
	 */
	private void drawPixel(WritableRaster raster, Point p1, int[] colour) {
		if (p1.x >= 0 && p1.x < width && p1.y >= 0 && p1.y < height)
			raster.setPixel(p1.x, p1.y, colour);
	}

	/**
	 * Draws 2 perpendicular lines intersecting at point p1
	 * 
	 * @param raster
	 *            draw on writable raster
	 * @param p1
	 *            point coordinates
	 * @param colour
	 *            colour
	 */
	private void drawCross(WritableRaster raster, Point p1, int[] colour) {
		for (int i = 0; i < width; i++) {
			drawPixel(raster, new Point(i, p1.y), colour);
		}
		for (int i = 0; i < height; i++) {
			drawPixel(raster, new Point(p1.x, i), colour);
		}
	}

	/**
	 * The drawLittleCross function is for drawing a little cross.
	 * 
	 * @param raster
	 *            draw on writable raster
	 * @param p
	 *            point coordinates
	 * @param colour
	 *            colour
	 */
	private void drawLittleCross(WritableRaster raster, Point p, int[] colour) {
		drawPixel(raster, p, colour);
		drawPixel(raster, new Point(p.x + 1, p.y), colour);
		drawPixel(raster, new Point(p.x - 1, p.y), colour);
		drawPixel(raster, new Point(p.x, p.y + 1), colour);
		drawPixel(raster, new Point(p.x, p.y - 1), colour);
	}

	public OldImageProcessor() {	}

}