package sdp.vision.processing;

import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import com.googlecode.javacv.cpp.opencv_core.IplImage;


import sdp.common.Robot;
import sdp.common.Tools;
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
	
	private static enum rgb {
		red, green, blue
	}
	
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
	private static final double color_sigma = 10;
	private static final double robot_sigma = 2;
	private static final double ball_sigma = 1;
	
	// maximum empty pixels in a feature for angle detection purposes
	private static final int robot_max_radius = 100;
	
	// angular accuracy. This is the number of iterations that would be done around the robot. To get the
	// accuracy at which an angle could be detected, just divide 360 by this number
	private static final int angular_acc = 360;
	
	// gaussian filtering for angle
	private static final int gauss_count = 10; // this is the number of points that would be affected by smoothing. Must be even.
	private static final double gauss_amount = 20; // the smoothing factor
	private static final double[] gauss_filter_matrix = new double[gauss_count*2+1]; // the matrix that would be filled in with values
	
	// the radius above which standard deviation is meaningless
	private double robot_radius = 0;
	private double ball_radius = 0;
	
	// when to quit trying to optimize error and assume the object is not on the field
	private static int max_attempts_before_quitting = 15;
	
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
	}
	
	@Override
	public synchronized WorldState extractWorldState(BufferedImage frame) {
		getCurrentROI();
		frame = preprocessFrame(frame);
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
		
		if (frame_count % take_average_every == 0)
			average();
		doThresholding();
		stDevColorFilter(blue_robot_image, blue_pos);
		optimizeStDev(blue_robot_image, blue_pos, robot_sigma, robot_radius);
		stDevColorFilter(yellow_robot_image, yellow_pos);
		optimizeStDev(yellow_robot_image, yellow_pos, robot_sigma, robot_radius);
		stDevColorFilter(ball_image, ball_pos);
		optimizeStDev(ball_image, ball_pos, ball_sigma, ball_radius);
		
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
		
		// finishing
		if (oldtime != -1) {
			final long curtime = System.currentTimeMillis();
			graph.drawString(String.format("%.2f fps", 1000d/(curtime-oldtime)), 10, 10);
			oldtime = curtime;
		} else
			oldtime = System.currentTimeMillis();
		
		graph.dispose();
		return state;
	}
	
	private void optimizeStDev(byte[][] channel, Point2D.Double pos, double sigma, double required_st_dev) {
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
		};
	}
	
	private void drawThings() {
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
			final double ang_rad = robot.getAngle() * Math.PI / 180d;
			final double shift_x = 0.01 * Math.cos(ang_rad);
			final double shift_y = -0.01 * Math.sin(ang_rad);
			graph.setColor(Color.black);

			final double dir_x = 0.04*Math.cos(ang_rad);
			final double dir_y = -0.04*Math.sin(ang_rad);
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
	private double getAngle(byte[][] channel, Point2D.Double center) {
		// TODO! correct approach, be careful with centre of mass!
		if (center.x == -1 && center.y == -1)
			return 0;
		// bucket may be used for smoothing, this is not a priority now
		final double rot_ang_rad = 2*Math.PI/angular_acc,
				cos = Math.cos(rot_ang_rad),
				sin = Math.sin(rot_ang_rad);
		double vx = 1d,
			   vy = 0d;
		for (int i = 0; i < angular_acc; i++) {
			// rotate the unit vector one rot_ang_rad more
			vx = vx * cos - vy * sin;
			vy = vy * cos + vx * sin;
			int sector_width = 0;
			int oldx = (int) center.x, oldy = (int) center.y;
			// start going radially onwards from the center
			int count = 0;
			for (int px_id = 1; ; px_id++) {
				int x = (int) (center.x+vx * px_id);
				int y = (int) (center.y+vy * px_id);
				if (oldx == x && oldy == y)
					continue;
				if (x >= 0 && y >= 0 && y < channel[0].length && x < channel.length) {
					if ((channel[x][y] & 0xFF) != 0) {
						sector_width++;
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
			// debugging
			vx = 1d;
			vy = 0d;
			for (int i = 0; i < angular_acc; i++) {
				// rotate the unit vector one rot_ang_rad more
				vx = vx * cos - vy * sin;
				vy = vy * cos + vx * sin;
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
		return Tools.normalizeAngle(-max_id*rot_ang_rad*180/Math.PI);
	}
	
	/**
	 * Does the thressholding
	 * It is based on the average color of the table.
	 */
	private void doThresholding() {
		final int
				mdb = 255 - avg_b,
				mdr = 255 - avg_r,
				mdg = 255 - avg_g;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				// calculate red, green, blue and yellowness of the pixel at x y
				final int
						re = getPixel(x,y, rgb.red),
						gr = getPixel(x,y, rgb.green),
						bl = getPixel(x,y, rgb.blue);
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
				double diff_coeff = (255-Math.abs(dr-dg))/255d;
				double int_coeff = ((dr+dg)/2d - db/3)/255d;
				int dy = (int) (255*diff_coeff*int_coeff);
				if (dy < 0) dy = 0;
				else if (dy > 255) dy = 255;
				// remove pollutants
				final int
					// red is poluted by yellow as well
					clean_r = dr - dy -dg - db,
					clean_b = db - dg - dr, //
					clean_y = dy;
				// normalize and buffer
				yellow_robot_image[x][y] = (byte) (clean_y < 0 ? 0 : (clean_y > 255 ? 255 : clean_y));
				blue_robot_image[x][y] =  (byte) (clean_b < 0 ? 0 : (clean_b > 255 ? 255 : clean_b));
				ball_image[x][y] = (byte) (clean_r < 0 ? 0 : (clean_r > 255 ? 255 : clean_r));
				//setPixel(x, y,dy, dy,dy);
			}
	}
	
	/**
	 * Normalizes the array between 0 and 255 according to {@link #threshold_ratio}
	 * 
	 * @param array the input array
	 * @param centre_out for returning the rough geometrical centre
	 */
	private void stDevColorFilter(byte[][] array, Point2D.Double centre_out) {
		
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
				int value = array[x][y] & 0xFF;
				if (Math.abs(value - col_mean) > color_sigma*col_st_dev) {
					pt_count++;
					cx += x;
					cy += y;
					array[x][y] = (byte) 255;
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
	private double stDevLengthFilter(byte[][] array, Point2D.Double centre_out, double sigma) {
		// calculate standard deviation in length
		double sum = 0;
		double sq_sum = 0;
		int pt_count = 0;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				int value = array[x][y] & 0xFF;
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
					dist = Math.sqrt(dist_sq);
					if (Math.abs(dist - mean) >= sigma*st_dev) {
						// disregard point if it is too far away
						array[x][y] = 0;
						continue;
					}
					;
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
	private void drawArray(byte[][] channel, int r, int g, int b) {
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				final double coeff = (channel[x][y] & 0xFF) / 255d;
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
	private void average() {
		double ag = 0, ar = 0, ab = 0;
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				ar += getPixel(x,y, rgb.red)/(double) pixels_count;
				ag += getPixel(x,y, rgb.green)/(double) pixels_count;
				ab += getPixel(x,y, rgb.blue)/(double) pixels_count;
			}
		avg_r = (int) ar;
		avg_g = (int) ag;
		avg_b = (int) ab;
		
	}
	
	/**
	 * Sets screen values
	 */
	private void getCurrentROI() {
		start_x = config.getFieldLowX();
		width = config.getFieldWidth();
		stop_x = start_x+width;
		start_y = config.getFieldLowY();
		height = config.getFieldHeight();
		stop_y = start_y+height;
		pixels_count = width * height;
		robot_radius = width * Robot.LENGTH;
		ball_radius = 10 * width / Tools.PITCH_WIDTH_CM;
	}
	
	/**
	 * Gets the value of the particular color for the particular pixel of {@link #pixels}
	 * @param x the x position of the pixel
	 * @param y the y position
	 * @param color which color value should be returned
	 * @return value in range 0 .. 255
	 */
	private final int getPixel(final int x, final int y, final rgb color) {
		int ord = 2;
		if (color == rgb.green)
			ord = 1;
		if (color == rgb.blue)
			ord = 0;
		return pixels[(x+y*im_width)*3+ord] & 0xFF;
	}
	
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
		pixels[(x+y*im_width)*3+2] = (byte) r;
		pixels[(x+y*im_width)*3+1] = (byte) g;
		pixels[(x+y*im_width)*3] = (byte) b;
	}
	
	/**
	 * Preprocess the frame for world state extraction.
	 * 
	 * @param frame Frame to preprocess.
	 */
	private BufferedImage preprocessFrame(BufferedImage frame) {
		IplImage frame_ipl = IplImage.createFrom(frame);
		frame_ipl = undistortImage(frame_ipl);	
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, 5);
		return frame_ipl.getBufferedImage();
	}
	
	/**
	 * Converts a "global" vector to 1..0
	 * @param global
	 * @return
	 */
	private Point2D.Double convertTo1(Point2D.Double global) {
		return new Point2D.Double((global.x-start_x)/width, ((global.y-start_y)/height)*Tools.PITCH_HEIGHT_CM/Tools.PITCH_WIDTH_CM);
	}

}
