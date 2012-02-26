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
	private WorldState oldstate = null; 
	private Graphics2D graph;

	// how often to take average
	private static final int take_average_every = 1;

	// standard deviation filter. Filter out points more than this amount of sigmas from the st dev
	private static final double blue_color_sigma = 8;
	private static final double red_color_sigma = 12;
	private static final double yellow_color_sigma = 10;
	
	private static final double area_T_cm = 56.32; // cm*cm
	private static final double area_ball_cm = 20; // cm*cm
	private static double area_T_px, area_ball_px;

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
	private static final double gauss_amount = 50; // the smoothing factor
	private static final double[] gauss_filter_matrix = new double[gauss_count*2+1]; // the matrix that would be filled in with values

	// gaussian smooth for image
	private static final int gauss_img_count = 1;
	private static final double gauss_img_amount = 3;
	private static final int gauss_img_matrix_size = gauss_img_count*2+1;
	private static final double[][] gauss_img_filter = new double[gauss_img_matrix_size][gauss_img_matrix_size];
	
	// pixel value constants for calculating blob area
	private static final int PXV_BELOW_THRESHOLD = 0;
	private static final int PXV_ABOVE_THRESHOLD = 1;
	private static final int PXV_BEING_CHECKED = 2;
	private static final int PXV_VALID_THRESHOLD = 255;
	private static final int PXV_AREA_INVALID_RATIO = 50;
	private static int area_so_far = 0;
	private static byte area_value = 0;
	
	// low pass filter
	
	private static double filteredPositionAmount = 0.8;
	private static double filteredAngleAmount = 0.8;

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
	public synchronized WorldState extractWorldState(final BufferedImage frame) {
		getCurrentROI();
		//frame = preprocessFrame(frame);
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

		gaussianSmooth();

		if (frame_count % take_average_every == 0)
			average();

		doThresholding();

		stDevColorFilter(blue_robot_image, blue_color_sigma);
		filterByArea(blue_robot_image, area_T_px, blue_pos);

		stDevColorFilter(yellow_robot_image, yellow_color_sigma);
		//drawArray(yellow_robot_image, 255, 255, 0);
		filterByArea(yellow_robot_image, area_T_px, yellow_pos);

		stDevColorFilter(ball_image, red_color_sigma);
		filterByArea(ball_image, area_ball_px, ball_pos);
		
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
		
		if (oldstate != null) {	
			state = new WorldState(
					lowPass(oldstate.getBallCoords(), state.getBallCoords()),
					lowPass(oldstate.getBlueRobot(),state.getBlueRobot()),
					lowPass(oldstate.getYellowRobot(), state.getYellowRobot()),
					frame);
		}
		oldstate = state;

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
	
	private final void filterByArea(final byte[][] channel, final double area_px, final Point2D.Double center) {
		int biggest_area = 0;
		// find areas of all blobs
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				 final double value = channel[x][y] & 0xFF;
				 if (value == PXV_ABOVE_THRESHOLD) {
					 area_so_far = 1;
					 channel[x][y] = PXV_BEING_CHECKED;
					 recursePixelSize(channel, x, y);
					 int area_temp = (int) (255*Math.abs(area_so_far - area_px)/area_px);
					 if (area_temp > 255)
						 area_temp = 255;
					 area_value = (byte) (255 - area_temp);
					 if ((area_value & 0xFF) < PXV_AREA_INVALID_RATIO)
						 area_value = PXV_BELOW_THRESHOLD;
					 if ((area_value & 0xFF) > biggest_area) {
						 biggest_area = area_value & 0xFF;
					 }
					 setArea(channel);
				 }
			}
		// get the com of the blob with biggest area
		if (biggest_area < PXV_AREA_INVALID_RATIO || biggest_area == 0) {
			// there is no blob on screen
			center.x = -1;
			center.y = -1;
		} else {
			double cx = 0;
			double cy = 0;
			int px_count = 0;
			for (int y = start_y; y <= stop_y; y++)
				for (int x = start_x; x <= stop_x; x++)
					if ((channel[x][y] & 0xFF) == biggest_area) {
						cx += x;
						cy += y;
						px_count++;
						channel[x][y] = (byte) PXV_VALID_THRESHOLD;
					} else
						channel[x][y] = 0;
			center.x = cx/px_count;
			center.y = cy/px_count;
		}
	}
	
	private final void recursePixelSize(final byte[][] channel, final int x, final int y) {
		final int xm1 = x-1,
				  xp1 = x+1,
				  ym1 = y-1,
				  yp1 = y+1;
		final boolean
				  xm1_in_range = (xm1 >= start_x && xm1 <= stop_x),
				  xp1_in_range = (xp1 >= start_x && xp1 <= stop_x),
				  ym1_in_range = (ym1 >= start_y && ym1 <= stop_y),
				  yp1_in_range = (yp1 >= start_y && yp1 <= stop_y),
				  left = xm1_in_range && (channel[xm1][y] & 0xFF) == PXV_ABOVE_THRESHOLD,
				  right = xp1_in_range && (channel[xp1][y] & 0xFF) == PXV_ABOVE_THRESHOLD,
				  above = yp1_in_range && (channel[x][yp1] & 0xFF) == PXV_ABOVE_THRESHOLD,
				  below = ym1_in_range && (channel[x][ym1] & 0xFF) == PXV_ABOVE_THRESHOLD;
		
		// flip surrounding bits
		if (left) {
			channel[xm1][y] = PXV_BEING_CHECKED;
			area_so_far ++;
		}
		if (right) {
			channel[xp1][y] = PXV_BEING_CHECKED;
			area_so_far ++;
		}
		if (above) {
			channel[x][yp1] = PXV_BEING_CHECKED;
			area_so_far ++;
		}
		if (below) {
			channel[x][ym1] = PXV_BEING_CHECKED;
			area_so_far ++;
		}
		
		// recurse
		if (left)
			recursePixelSize(channel, xm1, y);
		if (right)
			recursePixelSize(channel, xp1, y);
		if (above)
			recursePixelSize(channel, x, yp1);
		if (below)
			recursePixelSize(channel, x, ym1);
	}
	
	private final void setArea(final byte[][] channel) {
		
		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				if ((channel[x][y] & 0xFF) == PXV_BEING_CHECKED)
					channel[x][y] = area_value;
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
					if (value == PXV_VALID_THRESHOLD) {
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
				clean_y = dy - dg/5 - dr/6;
				// normalize and buffer
				yellow_robot_image[x][y] = (byte) (clean_y < 0 ? 0 : (clean_y > 255 ? 255 : clean_y));
				blue_robot_image[x][y] =  (byte) (clean_b < 0 ? 0 : (clean_b > 255 ? 255 : clean_b));
				ball_image[x][y] = (byte) (clean_r < 0 ? 0 : (clean_r > 255 ? 255 : clean_r));
				setPixel(x, y, clean_y, clean_y, clean_y);
			}
	}

	/**
	 * Normalizes the array between 0 and 255 according to {@link #threshold_ratio}
	 * 
	 * @param array the input array
	 */
	private final void stDevColorFilter(final byte[][] array, final double color_sigma) {

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

		for (int y = start_y; y <= stop_y; y++)
			for (int x = start_x; x <= stop_x; x++) {
				final int value = array[x][y] & 0xFF;
				final double abs = value - col_mean;
				if ((abs > 0 ? abs : -abs) > color_sigma*col_st_dev) {
					array[x][y] = PXV_ABOVE_THRESHOLD;
				} else
					array[x][y] = PXV_BELOW_THRESHOLD;
			}
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
		final double cmtopix = width / Tools.PITCH_WIDTH_CM;
		area_ball_px = area_ball_cm * cmtopix * cmtopix;
		area_T_px = area_T_cm * cmtopix * cmtopix;
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

	// low pass filtering
	
	/**
	 * A simple low-pass filter
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return a filtered value
	 */
	private final static double lowPass(final double old_value, final double new_value, final double amount) {
		return (old_value+new_value*amount)/((double) (amount+1));
	}

	/**
	 * Low pass for angles
	 * @param old_value
	 * @param new_value
	 * @return the filtered angle
	 */
	private final static double lowPass(final double old_value, final double new_value) {
		return lowPass(old_value, new_value, filteredAngleAmount);
	}
	
	/**
	 * Low pass on position
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return the filtered position
	 */
	private final static Point2D.Double lowPass(final Point2D.Double old_value, final Point2D.Double new_value) {
		if (new_value.x == -1 && new_value.y == -1)
			return new_value;
		if (old_value.x == -1 && old_value.y == -1)
			return new_value;
		return new Point2D.Double (
				lowPass(old_value.getX(), new_value.getX(), filteredPositionAmount),
				lowPass(old_value.getY(), new_value.getY(), filteredPositionAmount));
	}

	/**
	 * Low pass on a robot
	 * @param old_value
	 * @param new_value
	 * @param amount
	 * @return a new robot with low_pass
	 */
	private final static Robot lowPass(final Robot old_value, final Robot new_value) {
		return new Robot(
				lowPass(old_value.getCoords(), new_value.getCoords()),
				lowPass(old_value.getAngle(), new_value.getAngle()));
	}
}
