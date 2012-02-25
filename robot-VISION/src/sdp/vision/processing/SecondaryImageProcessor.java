package sdp.vision.processing;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;


import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Utilities;
import sdp.common.WorldState;

/**
 * Secondary image processor. Built with assumption that it should not need callibration.
 * 
 * It finds the average pixel color and uses that to determine the most - far yellow, blue and red outliers.
 * 
 * It then applies thresholding based on standard deviation outliers.
 * 
 * The angle is done by tracing the contour of the shape about the centre of mass and using gaussian smooth
 * to smooth data and find the furthest point which is regarded as the desired direction.
 * 
 * 
 * @author Martin Marinov
 *
 */
public class SecondaryImageProcessor extends BaseImageProcessor {

	//	private static enum rgb {
	//		red, green, blue
	//	}

	private int start_x = 0, stop_x = 0, start_y = 0, stop_y = 0, height = 0, width = 0, pixels_count = 0, im_width = 0;
	private int avg_r = 0, avg_b = 0, avg_g = 0;
	private byte[] pixels = null;
	private byte[][] yellow_robot_image = null,
			blue_robot_image = null,
			ball_image = null;
	private Point2D.Double
	yellow_pos = new Point2D.Double(0, 0),
	blue_pos  = new Point2D.Double(0, 0),
	ball_pos = new Point2D.Double(0, 0);
	private final double[] smoothed_bucket = new double[angular_acc],
			bucket = new double[angular_acc];

	private WorldState state;
	private Graphics2D graph;

	// how often to take average
	private static final int take_average_every = 1;

	// standard deviation filter. Filter out points more than this amount of sigmas from the st dev
	private static final double blue_color_sigma = 10;
	private static final double red_color_sigma = 10;
	private static final double yellow_color_sigma = 6;
	private static final double robot_sigma = 2;
	private static final double ball_sigma = 1;

	// maximum empty pixels in a feature for angle detection purposes
	private static final int robot_max_radius = 100;

	// angular accuracy. This is the number of iterations that would be done around the robot. To get the
	// accuracy at which an angle could be detected, just divide 360 by this number
	private static final int angular_acc = 360;
	private static final double rot_ang_rad = 2*Math.PI/angular_acc,
			cos_ang = Math.cos(rot_ang_rad),
			sin_ang = Math.sin(rot_ang_rad),
			PI = Math.PI;

	// gaussian filtering for angle
	private static final int gauss_count = 20; // this is the number of points that would be affected by smoothing. Must be even.
	private static final double gauss_amount = 30; // the smoothing factor
	private static final double[] gauss_filter_matrix = new double[gauss_count*2+1]; // the matrix that would be filled in with values

	// gaussian smooth for image
	private static final int gauss_img_count = 1;
	private static final double gauss_img_amount = 3;
	private static final int gauss_img_matrix_size = gauss_img_count*2+1;
	private static final double[][] gauss_img_filter = new double[gauss_img_matrix_size][gauss_img_matrix_size];

	// the radius above which standard deviation is meaningless
	private double robot_radius = 0;
	private double ball_radius = 0;

	// when to quit trying to optimize error and assume the object is not on the field
	private static int max_attempts_before_quitting = 3;

	private long frame_count = 0;

	private long oldtime = -1;

	public SecondaryImageProcessor() {
		super();

		// generate gaussian filter
		double sum = 0;
		for (int x = -gauss_count; x <= gauss_count; x++) {
			gauss_filter_matrix[x+gauss_count] = Math.exp(-x*x/(2*gauss_amount*gauss_amount))/gauss_amount;
			sum += gauss_filter_matrix[x+gauss_count];
		}
		// normalize to 1
		for (int i = 0; i < gauss_filter_matrix.length; i++)
			gauss_filter_matrix[i] = gauss_filter_matrix[i]/sum;

		// image gaussian filter
		sum = 0;
		for (int x = -gauss_img_count; x <= gauss_img_count; x++)
			for (int y = -gauss_img_count; y <= gauss_img_count; y++) {
				final int xid = x+gauss_img_count;
				final int yid = y+gauss_img_count;
				final double dist_sq = x*x + y*y;
				gauss_img_filter[xid][yid] = Math.exp(-dist_sq/(2*gauss_img_amount*gauss_img_amount))/gauss_img_amount;
				sum += gauss_img_filter[xid][yid];
			}
		// normalize
		for (int x = 0; x < gauss_img_matrix_size; x++)
			for (int y = 0; y < gauss_img_matrix_size; y++)
				gauss_img_filter[x][y] = gauss_img_filter[x][y] / sum;
	}

	@Override
	public synchronized WorldState extractWorldState(BufferedImage frame) {
		getCurrentROI();
		frame = preprocessFrame(frame);
		final boolean graph_needed = config.isShowBoundingBoxes() || config.isShowContours() || config.isShowStateData() || !config.isShowWorld();
		if (graph_needed)
			graph = frame.createGraphics();

		frame_count++;


		if (pixels == null) {
			// when first image is received
			pixels = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
			im_width = frame.getWidth();
			yellow_robot_image = new byte[frame.getWidth()][frame.getHeight()];
			blue_robot_image = new byte[frame.getWidth()][frame.getHeight()];
			ball_image = new byte[frame.getWidth()][frame.getHeight()];
		} else
			pixels = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();

		//gaussianSmooth();

		if (frame_count % take_average_every == 0)
			average();

		doThresholding();

		stDevColorFilter(blue_robot_image, blue_pos, blue_color_sigma);
		optimizeStDev(blue_robot_image, blue_pos, robot_sigma, robot_radius);

		stDevColorFilter(yellow_robot_image, yellow_pos, yellow_color_sigma);
		optimizeStDev(yellow_robot_image, yellow_pos, robot_sigma, robot_radius);

		stDevColorFilter(ball_image, ball_pos, red_color_sigma);
		optimizeStDev(ball_image, ball_pos, ball_sigma, ball_radius);
		
		// some manual fixing
		if (yellow_pos.x != -1 && yellow_pos.y != -1) {
			final double dx = yellow_pos.x - blue_pos.x,
					dy = yellow_pos.y - blue_pos.y,
					r2 = dx*dx+dy*dy;
			if (r2 < (Robot.LENGTH * Robot.LENGTH * width * width) / 4) {
				yellow_pos.x = -1;
				yellow_pos.y = -1;
			}
		}

		state = new WorldState(convertTo1(ball_pos),
				new Robot(convertTo1(blue_pos), getAngle(blue_robot_image, blue_pos)),
				new Robot(convertTo1(yellow_pos), getAngle(yellow_robot_image, yellow_pos)),
				frame);

		// drawing part
		if (!config.isShowWorld()) {
			graph.setColor(new Color(avg_r, avg_g, avg_b));
			graph.fillRect(start_x, start_y, width, height);	
		}

		if (config.isShowThresholds()) {
			drawArray(blue_robot_image, 0, 0, 255);
			drawArray(yellow_robot_image, 200, 200, 0);
			drawArray(ball_image, 255, 0, 0);
		}

		if (config.isShowBoundingBoxes())
			drawThings();

		if (config.isShowStateData()) {
			if (oldtime != -1) {
				final long curtime = System.currentTimeMillis();
				graph.drawString(String.format("%.2f fps", 1000d/(curtime-oldtime)), 10, 10);
				oldtime = curtime;
			} else
				oldtime = System.currentTimeMillis();
		} else if (oldtime != -1)
			oldtime = -1;

		// finishing
		if (graph_needed)
			graph.dispose();
		return state;
	}

	private final void optimizeStDev(final byte[][] channel, final Point2D.Double pos, final double sigma, final double required_st_dev) {
		if (pos.x == -1 && pos.y == -1)
			return;
		int attempt = 0;
		while (stDevLengthFilter(channel, pos, sigma)*sigma > required_st_dev)  {	
			if (pos.x == -1 && pos.y == -1)
				return;
			if (attempt > max_attempts_before_quitting) {
				pos.x = -1;
				pos.y = -1;
				return;
			}
			attempt++;
		}
	}

	private final void drawThings() {
		final double height_coeff = height*Tools.PITCH_WIDTH_CM/Tools.PITCH_HEIGHT_CM;
		// draw ball
		if (state.getBallCoords().getX() != -1 && state.getBallCoords().getY() != -1) {
			int ball_y = (int) (state.getBallCoords().getY()*height_coeff+start_y);
			int ball_x = (int) (state.getBallCoords().getX()*width+start_x);
			graph.setColor(Color.red);
			graph.drawLine(start_x, ball_y, start_x+width, ball_y);
			graph.drawLine(ball_x, start_y, ball_x, start_y+height);
		}
		// draw robots
		final Robot[] robots = new Robot[] {
				state.getBlueRobot(),
				state.getYellowRobot()
		};
		for (int i = 0; i < 2; i++) {
			final Robot robot = robots[i];
			if (robot.getCoords().getX() == -1 && robot.getCoords().getY() == -1)
				continue;
			// draw body of robot
			graph.setColor(i == 0 ? Color.blue : Color.yellow);
			graph.drawPolygon(new int[] {
					(int)(robot.getFrontLeft().getX()*width+start_x),
					(int)(robot.getFrontRight().getX()*width+start_x),
					(int)(robot.getBackRight().getX()*width+start_x),
					(int)(robot.getBackLeft().getX()*width+start_x),
					(int)(robot.getFrontLeft().getX()*width+start_x)
			}, new int[] {
					(int)(robot.getFrontLeft().getY()*height_coeff+start_y),
					(int)(robot.getFrontRight().getY()*height_coeff+start_y),
					(int)(robot.getBackRight().getY()*height_coeff+start_y),
					(int)(robot.getBackLeft().getY()*height_coeff+start_y),
					(int)(robot.getFrontLeft().getY()*height_coeff+start_y)
			}, 5);

			// draw direction pointer
			final double ang_rad = robot.getAngle() * PI / 180d;
			final double cos = Math.cos(ang_rad),
					sin = Math.sin(ang_rad);
			final double shift_x = 0.01 * cos;
			final double shift_y = -0.01 * sin;
			graph.setColor(Color.black);

			final double dir_x = 0.04*cos;
			final double dir_y = -0.04*sin;
			graph.drawLine(
					(int)((robot.getCoords().getX()-shift_x)*width+start_x),
					(int)((robot.getCoords().getY()-shift_y)*height_coeff+start_y),
					(int)((robot.getCoords().getX()+dir_x-shift_x)*width+start_x),
					(int)((robot.getCoords().getY()+dir_y-shift_y)*height_coeff+start_y));
			final int rob_cx = (int)(robot.getCoords().getX()*width+start_x),
					rob_cy = (int)(robot.getCoords().getY()*height_coeff+start_y);
			graph.fillOval(rob_cx - 5, rob_cy - 5, 10, 10);
		}
		// draw bounding box
		graph.setColor(Color.white);
		graph.drawRect(start_x, start_y, width, height);
	}

	/**
	 * Get the direction of a T shape
	 * @param channel the channel of the robot normalized
	 * @param center the center of the robot on this channel
	 * @return
	 */
	private final double getAngle(final byte[][] channel, final Point2D.Double center) {
		if (center.x == -1 && center.y == -1)
			return 0;
		// bucket may be used for smoothing, this is not a priority now
		double vx = 1d,
				vy = 0d;
		for (int i = 0; i < angular_acc; i++) {
			// rotate the unit vector one rot_ang_rad more
			vx = vx * cos_ang - vy * sin_ang;
			vy = vy * cos_ang + vx * sin_ang;
			double sector_width = 0;
			int oldx = (int) center.x, oldy = (int) center.y;
			// start going radially onwards from the center
			int count = 0;
			for (int px_id = 1; ; px_id++) {
				final int x = (int) (center.x+vx * px_id);
				final int y = (int) (center.y+vy * px_id);
				if (oldx == x && oldy == y)
					continue;
				if (x >= 0 && y >= 0 && y < channel[0].length && x < channel.length) {
					final int value = channel[x][y] & 0xFF;
					if (value != 0) {
						sector_width += count+1;// value < 200 ? 200/255d : value/255d;
						oldx = x;
						oldy = y;
						count = 0;
					} else {
						count++;
						if (count > robot_max_radius)
							break;
					}
				} else
					break;
			}
			// put in bucket
			bucket[i] = sector_width;
		}
		// gaussian smoothing
		double max = 0;
		int max_id = -1;
		for (int i = 0; i < bucket.length; i++) {
			smoothed_bucket[i] = 0;
			for (int j = 0; j < gauss_filter_matrix.length; j++) {
				int bucket_id = i+j-gauss_count-1;
				if (bucket_id >= bucket.length)
					bucket_id = bucket_id - bucket.length;
				if (bucket_id < 0)
					bucket_id = bucket.length + bucket_id;
				smoothed_bucket[i] += bucket[bucket_id]*gauss_filter_matrix[j];
			}
			if (smoothed_bucket[i] > max) {
				max = smoothed_bucket[i];
				max_id = i;
			}
		}
		if (config.isShowContours()) {
			vx = 1d;
			vy = 0d;
			for (int i = 0; i < angular_acc; i++) {
				// rotate the unit vector one rot_ang_rad more
				vx = vx * cos_ang - vy * sin_ang;
				vy = vy * cos_ang + vx * sin_ang;
				int x = (int) (center.x+vx * bucket[i]);
				int y = (int) (center.y+vy * bucket[i]);
				graph.setColor(Color.black);
				graph.fillOval(x-2, y-2, 4, 4);
				x = (int) (center.x+vx * smoothed_bucket[i]);
				y = (int) (center.y+vy * smoothed_bucket[i]);
				graph.setColor(Color.white);
				graph.fillOval(x-2, y-2, 4, 4);
			}
		}
		return Utilities.normaliseAngle(-max_id*rot_ang_rad*180/PI);
	}

	/**
	 * Does the thressholding
	 * It is based on the average color of the table.
	 */
	private final void doThresholding() {
		final int
		mdb = 255 - avg_b,
		mdr = 255 - avg_r,
		mdg = 255 - avg_g;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				// calculate red, green, blue and yellowness of the pixel at x y
				final int
				id = (x+y*im_width)*3,
				re = pixels[id+2] & 0xFF,//getPixel(x,y, rgb.red),
				gr = pixels[id+1] & 0xFF,//getPixel(x,y, rgb.green),
				bl = pixels[id] & 0xFF;//getPixel(x,y, rgb.blue);
				// get the positive diviation from the avarage values
				int		dr = 255*(re-avg_r)/mdr,
						db = 255*(bl-avg_b)/mdb,
						dg = 255*(gr-avg_g)/mdg;
				// isolate only the positive values
				if (dr < 0) dr = 0;
				else if (dr > 255) dr = 255;
				if (db < 0) db = 0;
				else if (db > 255) db = 255;
				if (dg < 0) dg = 0;
				else if (dg > 255) dg = 255;
				final double abs = dr-dg;
				final double diff_coeff = (255-(abs > 0 ? abs : -abs))/255d;
				final double int_coeff = ((dr+dg)/2d)/255d;
				int dy = (int) (255*diff_coeff*int_coeff);
				if (dy < 0) dy = 0;
				else if (dy > 255) dy = 255;
				// remove pollutants
				final int
				// red is poluted by yellow as well
				clean_r = dr - dy -dg - db,
				clean_b = db - dg - dr, //
				clean_y = dy - dg/2 - dr/4;
				// normalize and buffer
				yellow_robot_image[x][y] = (byte) (clean_y < 0 ? 0 : (clean_y > 255 ? 255 : clean_y));
				blue_robot_image[x][y] =  (byte) (clean_b < 0 ? 0 : (clean_b > 255 ? 255 : clean_b));
				ball_image[x][y] = (byte) (clean_r < 0 ? 0 : (clean_r > 255 ? 255 : clean_r));
				//setPixel(x, y,clean_y, clean_y,clean_y);
			}
	}

	/**
	 * Normalizes the array between 0 and 255 according to {@link #threshold_ratio}
	 * 
	 * @param array the input array
	 * @param centre_out for returning the rough geometrical centre
	 */
	private final void stDevColorFilter(final byte[][] array, final Point2D.Double centre_out, final double color_sigma) {

		// find global average for rough thresholding
		double sum = 0;
		double sq_sum = 0;
		int max = 0;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				int value = array[x][y] & 0xFF;
				sum += value;
				sq_sum += value*value;
				if (value > max)
					max = value;
			}
		final double col_mean = sum / pixels_count;
		final double col_st_dev = Math.sqrt(sq_sum / pixels_count - col_mean * col_mean);

		// apply thresholding and find rough centre
		double cx = 0, cy = 0;
		int pt_count = 0;

		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				final int value = array[x][y] & 0xFF;
				final double abs = value - col_mean;
				if ((abs > 0 ? abs : -abs) > color_sigma*col_st_dev) {
					pt_count++;
					cx += x;
					cy += y;
				} else
					array[x][y] = 0;
			}
		if (pt_count != 0) {
			centre_out.x = cx / pt_count;
			centre_out.y = cy / pt_count;
		} else {
			centre_out.x = -1;
			centre_out.y = -1;
		}
	}

	/**
	 * Filters positions of particles which have been already thresholded
	 * 
	 * @param array
	 * @param centre_out
	 * @param sigma
	 * @return current standard deviation
	 */
	private final double stDevLengthFilter(final byte[][] array, final Point2D.Double centre_out, final double sigma) {
		// calculate standard deviation in length
		double sum = 0;
		double sq_sum = 0;
		int pt_count = 0;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				final int value = array[x][y] & 0xFF;
				if (value != 0) {
					final double
					dist_x = x - centre_out.x,
					dist_y = y - centre_out.y,
					dist_sq = dist_x*dist_x + dist_y*dist_y;
					sum += Math.sqrt(dist_sq);
					sq_sum += dist_sq;
					pt_count++;
				}
			}

		double mean = sum / pt_count;
		double st_dev = Math.sqrt(sq_sum / pt_count - mean * mean);


		// filter data based on st dev and recalculate center
		double cx = 0;
		double cy = 0;
		sum = 0;
		sq_sum = 0;
		pt_count = 0;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				int value = array[x][y] & 0xFF;
				if (value != 0) {
					final double
					dist_x = x - centre_out.x,
					dist_y = y - centre_out.y,
					dist_sq = dist_x*dist_x + dist_y*dist_y,
					dist = Math.sqrt(dist_sq),
					abs = dist - mean;
					if ((abs > 0 ? abs : -abs) >= sigma*st_dev) {
						// disregard point if it is too far away
						array[x][y] = 0;
						continue;
					}
					sum += dist;
					sq_sum += dist_sq;
					pt_count++;
					cx += x;
					cy += y;
				}
			}
		if (pt_count != 0) {
			centre_out.x = cx / pt_count;
			centre_out.y = cy / pt_count;
		} else {
			centre_out.x = -1;
			centre_out.y = -1;
		}
		mean = sum / pt_count;
		st_dev = Math.sqrt(sq_sum / pt_count - mean * mean);
		return st_dev;
	}

	/**
	 * Draws the given array on screen in the color supplied
	 * 
	 * @param channel
	 * @param r
	 * @param g
	 * @param b
	 */
	private final void drawArray(final byte[][] channel, final int r, final int g, final int b) {
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				final double coeff = channel[x][y] != 0 ? 1 : 0;//(channel[x][y] & 0xFF) / 255d;
				final int nr = (int) (r*coeff),
						ng = (int) (g*coeff),
						nb = (int) (b*coeff);
				if (nr != 0 || ng != 0 || nb != 0)
					setPixel(x, y, nr, ng, nb);
			}
	}

	/**
	 * Find avarage color and store it into local variables
	 * 
	 * {@link #pixels} must be initialized beforehand!
	 */
	private final void average() {
		double ag = 0, ar = 0, ab = 0;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				final int id = (x+y*im_width)*3,
						re = pixels[id+2] & 0xFF,//getPixel(x,y, rgb.red),
						gr = pixels[id+1] & 0xFF,//getPixel(x,y, rgb.green),
						bl = pixels[id] & 0xFF;
				ar += re/(double) pixels_count;
				ag += gr/(double) pixels_count;
				ab += bl/(double) pixels_count;
			}
		avg_r = (int) ar;
		avg_g = (int) ag;
		avg_b = (int) ab;

	}

	/**
	 * Sets screen values
	 */
	private final void getCurrentROI() {
		start_x = config.getFieldLowX();
		width = config.getFieldWidth();
		stop_x = start_x+width;
		start_y = config.getFieldLowY();
		height = config.getFieldHeight();
		stop_y = start_y+height;
		pixels_count = width * height;
		robot_radius = width * Robot.LENGTH / 4;
		ball_radius = 3 * width / Tools.PITCH_WIDTH_CM;
	}

	//	/**
	//	 * Gets the value of the particular color for the particular pixel of {@link #pixels}
	//	 * @param x the x position of the pixel
	//	 * @param y the y position
	//	 * @param color which color value should be returned
	//	 * @return value in range 0 .. 255
	//	 */
	//	private final int getPixel(final int x, final int y, final rgb color) {
	//		switch (color) {
	//		case red:
	//			return pixels[(x+y*im_width)*3+2] & 0xFF;
	//		case blue:
	//			return pixels[(x+y*im_width)*3] & 0xFF;
	//		case green:
	//			return pixels[(x+y*im_width)*3+1] & 0xFF;
	//		default:
	//			return -1;
	//		}
	//	}

	/**
	 * Sets the value of a particular pixel and color to a particular value of {@link #pixels}
	 * @param x the x position of the pixel
	 * @param y the y position
	 * @param r the value for red 0 .. 255
	 * @param g the value for green 0 .. 255
	 * @param b the value for blue 0 .. 255
	 */
	private final void setPixel(final int x, final int y, int r, int g, int b) {
		r = r < 0 ? 0 : (r > 255 ? 255 : r);
		g = g < 0 ? 0 : (g > 255 ? 255 : g);
		b = b < 0 ? 0 : (b > 255 ? 255 : b);
		final int id = (x+y*im_width)*3; 
		pixels[id+2] = (byte) r;
		pixels[id+1] = (byte) g;
		pixels[id] = (byte) b;
	}

	/**
	 * Preprocess the frame for world state extraction.
	 * 
	 * @param frame Frame to preprocess.
	 */
	@SuppressWarnings("unused")
	private BufferedImage preprocessFrame(BufferedImage frame) {
		IplImage frame_ipl = IplImage.createFrom(frame);
		frame_ipl = undistortImage(frame_ipl);	
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, 5);
		return frame_ipl.getBufferedImage();
	}

	/**
	 * Does gaussian smooth to the frame
	 */
	@SuppressWarnings("unused")
	private final void gaussianSmooth() {
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				double r = 0, g = 0, b = 0;
				for (int i = -gauss_img_count; i <= gauss_img_count; i++)
					for (int j = -gauss_img_count; j <= gauss_img_count; j++) {
						final int xid = i+gauss_img_count;
						final int yid = j+gauss_img_count;
						final int px2x = x + i;
						final int px2y = y + j;
						final int id = (px2x+px2y*im_width)*3;
						final double gaus_f = gauss_img_filter[xid][yid];
						r += (pixels[id+2] & 0xFF) * gaus_f;
						g += (pixels[id+1] & 0xFF) * gaus_f;
						b += (pixels[id] & 0xFF) * gaus_f;
					}
				final int id = (x+y*im_width)*3; 
				pixels[id+2] = (byte) r;
				pixels[id+1] = (byte) g;
				pixels[id] = (byte) b;
			}
	}

	/**
	 * Converts a "global" vector to 1..0
	 * @param global
	 * @return
	 */
	private final Point2D.Double convertTo1(final Point2D.Double global) {
		if (global.getX() == -1 && global.getY() == -1)
			return global;
		return new Point2D.Double((global.x-start_x)/width, ((global.y-start_y)/height)*Tools.PITCH_HEIGHT_CM/Tools.PITCH_WIDTH_CM);
	}

}
