package sdp.vision.processing;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.LinkedList;
import java.util.Queue;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

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

	private static class col {
		public float r, g, b, total;

		public col(int r, int g, int b) {
			set(r, g, b);
		}

		public final void set(final int rr, final int gg, final int bb) {
			r = rr;
			g = gg;
			b = bb;
			total = (float) Math.sqrt(r*r+g*g+b*b);
			if (total == 0)
				return;
			r /= total;
			g /= total;
			b /= total;
		}

		public static final float diff(final col a, final col b) {
			final float 
			dr = a.r - b.r,
			dg = a.g - b.g,
			db = a.b - b.b;
			return Math.abs(dr*dr+dg*dg+db*db);
		}

		public static final byte ratio(final col a, final col b) {
			final int rat = Math.round(255 - 255*b.total/a.total);
			return rat < 0 ? 0 : (byte) (rat > 255 ? 255 : rat);
		}
	}

	private static final int mask = 0xFF;

	private static final int delayed_start = 2;

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

	// standard deviation filter. Filter out points more than this amount of sigmas from the st dev
	private static final double blue_color_sigma = 50;
	private static final double red_color_sigma = 4;
	private static final double yellow_color_sigma = 8;

	private static int
	col_green_r = 58,
	col_green_g = 86,
	col_green_b = 14;
	private static col[]
			desired = new col[] {
				new col(49, 0, 0),
				new col(0,0,57),
				new col(37, 13, 30)},
			undesired = new col[] {new col(255, 255, 255)};		
	private static col temp = new col(0,0,0);

	private static final double area_T_cm = 80; // cm*cm
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

	// background substraction
	private static double back_sigma = 0.1; // everything below this is considered background
	private static int back_scale_shift_bytes = 1;
	private static byte[][] back_r, back_g, back_b;
	private static boolean[] y_empty;
	private static boolean[][] empty;

	// gaussian filtering for angle
	private static final int gauss_count = 20; // this is the number of points that would be affected by smoothing. Must be even.
	private static final double gauss_amount = 80; // the smoothing factor
	private static final double[] gauss_filter_matrix = new double[gauss_count*2+1]; // the matrix that would be filled in with values


	// pixel value constants for calculating blob area
	private static final int PXV_BELOW_THRESHOLD = 0;
	private static final int PXV_ABOVE_THRESHOLD = 1;
	private static final int PXV_BEING_CHECKED = 2;
	private static final int PXV_VALID_THRESHOLD = mask;
	private static final int PXV_AREA_INVALID_RATIO = 50;
	private static byte area_value = 0;

	//	// low pass filter
	//
	//	private static double filteredPositionAmount = 0.8;
	//	private static double filteredAngleAmount = 0.8;

	private long frame_count = 0;

	private long oldtime = -1;

	private byte[][] channel;
	Queue<Point> queue = new LinkedList<Point>(); // for efficient blob detection

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

		//		try {
		final boolean graph_needed = config.isShowBoundingBoxes() || config.isShowContours() || config.isShowStateData() || !config.isShowWorld();
		if (graph_needed)
			graph = frame.createGraphics();

		frame_count++;

		if (pixels == null) {
			// when first image is received
			pixels = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();
			im_width = frame.getWidth();
			yellow_robot_image = new byte[im_width][frame.getHeight()];
			blue_robot_image = new byte[im_width][frame.getHeight()];
			ball_image = new byte[im_width][frame.getHeight()];
			back_r = new byte[im_width >> back_scale_shift_bytes][frame.getHeight() >> back_scale_shift_bytes];
			back_g = new byte[im_width >> back_scale_shift_bytes][frame.getHeight() >> back_scale_shift_bytes];
			back_b = new byte[im_width >> back_scale_shift_bytes][frame.getHeight() >> back_scale_shift_bytes];
			y_empty = new boolean[frame.getHeight() >> back_scale_shift_bytes];
			empty = new boolean[im_width >> back_scale_shift_bytes][frame.getHeight() >> back_scale_shift_bytes];
		} else
			pixels = ((DataBufferByte) frame.getRaster().getDataBuffer()).getData();

		if (frame_count < delayed_start) {
			for (int y = start_y; y <= stop_y; y++)
				for (int x = start_x; x <= stop_x; x++) {
					back_r[x >> back_scale_shift_bytes][y >> back_scale_shift_bytes] = 0;
					back_g[x >> back_scale_shift_bytes][y >> back_scale_shift_bytes] = 0;
					back_b[x >> back_scale_shift_bytes][y >> back_scale_shift_bytes] = 0;
				}
		}

		backgRoundSub();

		doThresholding();

		channel = blue_robot_image;
		stDevColorFilter(blue_color_sigma);
		filterByArea(area_T_px, blue_pos);

		channel = yellow_robot_image;
		stDevColorFilter(yellow_color_sigma);
		//System.out.println("YELLOW STa");
		filterByArea(area_T_px, yellow_pos);
		//System.out.println("YELLOW STo");

		channel = ball_image;
		stDevColorFilter(red_color_sigma);
		filterByArea(area_ball_px, ball_pos);

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
			for (int y = start_y; y < stop_y; y++)
				for (int x = start_x; x < stop_x; x++) {
					setPixel(x, y, back_r[x >> back_scale_shift_bytes][y >> back_scale_shift_bytes] & mask, back_g[x >> back_scale_shift_bytes][y >> back_scale_shift_bytes] & mask, back_b[x >> back_scale_shift_bytes][y >> back_scale_shift_bytes] & mask);
				}
		}

		if (config.isShowThresholds()) {
			drawArray(blue_robot_image, 0, 0, mask);
			drawArray(yellow_robot_image, 200, 200, 0);
			drawArray(ball_image, mask, 0, 0);
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
		//		} catch (ArrayIndexOutOfBoundsException e) {
		//			pixels = null;
		//			frame_count = 0;
		//			System.out.println("out_of_bounds");
		//			return new WorldState(new Point2D.Double(-1, -1),
		//					new Robot(new Point2D.Double(-1, -1), 0),
		//					new Robot(new Point2D.Double(-1, -1), 0),
		//					frame);
		//		}
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
					if (channel[x][y] == PXV_VALID_THRESHOLD) {
						sector_width += count+1;// value < 200 ? 200/maskd : value/maskd;
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
		for (int i = 0; i - bucket.length + 1 != 0; i++) {
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
			for (int i = 0; i - angular_acc + 1 != 0; i++) {
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
	 * Generates background averages
	 */
	private final void backgRoundSub() {

		// find st dev
		long sumg = 0;
		long sq_sumg = 0;
		for (int y = start_y; y - stop_y != 0; y++) {
			final int y_sc = y >> back_scale_shift_bytes; 
				y_empty[y_sc] = false;
				for (int x = start_x; x - stop_x != 0; x++) {
					yellow_robot_image[x][y] = 0;
					blue_robot_image[x][y] = 0;
					ball_image[x][y] = 0;
					final int id = (x+y*im_width)*3,
							re = pixels[id+2] & mask,
							gr = pixels[id+1] & mask,
							bl = pixels[id] & mask,
							dr = re - col_green_r, dg = gr - col_green_g, db = bl - col_green_b,
							value = dr*dr+dg*dg+db*db;
					sumg += value;
					sq_sumg += value*value;
					final int x_sc = x >> back_scale_shift_bytes;
					empty[x_sc][y_sc] = false;
				}
		}
		final double px_ct = (double) pixels_count,
				iniy_avg_g = sumg / px_ct,
				col_st_devg = sq_sumg / px_ct - iniy_avg_g * iniy_avg_g;
		long avr = 0, avg = 0, avb = 0;
		boolean should_i = false;
		int pix_ct = 0;
		// start averaging
		final int scaled_start_y = start_y >> back_scale_shift_bytes,
				scaled_start_x = start_x >> back_scale_shift_bytes,
				scaled_stop_x = stop_x >> back_scale_shift_bytes,
				scaled_stop_y = stop_y >> back_scale_shift_bytes;
				for (int sc_y = scaled_start_y; sc_y < scaled_stop_y; sc_y++) {
					for (int sc_x = scaled_start_x; sc_x < scaled_stop_x; sc_x++) {
						final int id = ((sc_x+sc_y*im_width)*3) << back_scale_shift_bytes,
								re = pixels[id+2] & mask,
								gr = pixels[id+1] & mask,
								bl = pixels[id] & mask,
								value = (re-58)*(re-58)+(gr-86)*(gr-86)+(bl-14)*(bl-14);;
								final double dvalue = value - iniy_avg_g;
								// check st div
								if (dvalue*dvalue < col_st_devg*back_sigma) {
									int r = back_r[sc_x][sc_y] & mask,
											g = back_g[sc_x][sc_y] & mask,
											b = back_b[sc_x][sc_y] & mask;
									if (r != 0 && b != 0 && g != 0) {
										r = (r+re)/2;
										g = (g+gr)/2;
										b = (b+bl)/2;
									} else {
										r = re;
										g = gr;
										b = bl;
									}
									back_r[sc_x][sc_y] = (byte) r;
									back_g[sc_x][sc_y] = (byte) g;
									back_b[sc_x][sc_y] = (byte) b;
									avr += r;
									avg += g;
									avb += b;
									pix_ct++;
								} else {
									y_empty[sc_y] = true;
									empty[sc_x][sc_y] = true;
									if (back_r[sc_x][sc_y] == 0 && back_g[sc_x][sc_y] == 0 && back_b[sc_x][sc_y] == 0)
										should_i = true;
								}
					}
				}

				avg_r = (int) (avr/pix_ct);
				avg_g = (int) (avg/pix_ct);
				avg_b = (int) (avb/pix_ct);
				if (should_i) {
					for (int y = scaled_start_y; y < scaled_stop_y; y++) {
						if (!y_empty[y])
							continue;
						for (int x = scaled_start_x; x < scaled_stop_x; x++) {
							if (!empty[x][y])
								continue;
							if (back_r[x][y] == 0 || back_g[x][y] == 0 || back_b [x][y] == 0) {
								back_r[x][y] = (byte) avg_r;
								back_g[x][y] = (byte) avg_g;
								back_b[x][y] = (byte) avg_b;
							}
						}
					}
				}
				//				// smooth
				//				for (int y = scaled_start_y; y < scaled_stop_y; y++) {
				//					if (!y_empty[y])
				//						continue;
				//					for (int x = scaled_start_x; x < scaled_stop_x; x++) {
				//						if (!x_empty[x] || !empty[x][y])
				//							continue;
				//						final int
				//						xm1 = x - scaled_start_x == 0 ? scaled_start_x : x-1,
				//								xp1 = x - scaled_stop_x + 1 == 0 ? scaled_stop_x - 1 : x+1,
				//										ym1 = y - scaled_start_y == 0 ? scaled_start_y : y-1,
				//												yp1 = y - scaled_stop_y + 1 == 0 ? scaled_stop_y - 1 : y+1,
				//														l_r = back_r[xm1][y] & mask,
				//														u_r = back_r[x][yp1] & mask,
				//														r_r = back_r[xp1][y] & mask,
				//														d_r = back_r[x][ym1] & mask,
				//														l_g = back_g[xm1][y] & mask,
				//														u_g = back_g[x][yp1] & mask,
				//														r_g = back_g[xp1][y] & mask,
				//														d_g = back_g[x][ym1] & mask,
				//														l_b = back_b[xm1][y] & mask,
				//														u_b = back_b[x][yp1] & mask,
				//														r_b = back_b[xp1][y] & mask,
				//														d_b = back_b[x][ym1] & mask,
				//														c_r = back_r[x][y] & mask,
				//														c_g = back_g[x][y] & mask,
				//														c_b = back_b[x][y] & mask;
				//						final boolean x_right = x > (scaled_start_x+scaled_stop_x)/2,
				//								y_top = y > (scaled_start_y+scaled_stop_y)/2;
				//								back_r[x][y] = (byte) (Math.round((x_right ? l_r : r_r)+(y_top ? d_r : u_r)+c_r)/3);
				//								back_g[x][y] = (byte) (Math.round((x_right ? l_g : r_g)+(y_top ? d_g : u_g)+c_g)/3);
				//								back_b[x][y] = (byte) (Math.round((x_right ? l_b : r_b)+(y_top ? d_b : u_b)+c_b)/3);
				//					}
				//				}
	}

	/**
	 * Does the thressholding
	 * It is based on the average color of the table.
	 */
	private final void doThresholding() {

		for (int y = start_y; y - stop_y != 0; y++) {
			final int sc_y = y >> back_scale_shift_bytes;
						if (!y_empty[sc_y])
							continue;
						for (int x = start_x; x - stop_x != 0; x++) {
							final int sc_x = x >> back_scale_shift_bytes;
								if (!empty[sc_x][sc_y])
									continue;
								// calculate red, green, blue and yellowness of the pixel at x y
								final int
								id = (x+y*im_width)*3,
								u_dr = (pixels[id+2] & mask)-(back_r[sc_x][sc_y] & mask),
								u_db = (pixels[id] & mask)-(back_b[sc_x][sc_y] & mask),
								u_dg = (pixels[id+1] & mask)-(back_g[sc_x][sc_y] & mask);
								temp.set(u_dr, u_dg, u_db);
								float min = 999;
								col colres = null;
								int cid = -1;
								float sum = 0;
								for (int i = 0; i < desired.length; i++) {
									float val = col.diff(temp, desired[i]);
									sum+=val;
									if (val < min) {
										min = val;
										colres = desired[i];
										cid = i;
									}
								}
								for (int i = 0; i < undesired.length; i++) {
									float val = col.diff(temp, undesired[i]);
									if (val < min) {
										min = val;
										colres = null;
										cid = -1;
									}
								}
								byte vr = 0, vb = 0, vy = 0;
								if (colres != null) {
									final byte value = (byte) (255-255*min/sum);//col.ratio(temp, colres);
									switch (cid) {
									case 0: // red
										vr = value;
										break;
									case 1: // blue
										vb = value;
										break;
									case 2: // yellow
										vy = value;
										break;
									}
								}
								// normalize and buffer
								yellow_robot_image[x][y] = vy;
								blue_robot_image[x][y] = vb;
								ball_image[x][y] = vr;
								setPixel(x, y, (int) (temp.r*255), (int) (temp.g*255), (int) (temp.b*255));
						}
		}
	}

	/**
	 * Normalizes the array between 0 and mask according to {@link #threshold_ratio}
	 * 
	 * @param array the input array
	 */
	private final void stDevColorFilter(final double color_sigma) {

		// find global average for rough thresholding
		long sum = 0;
		long sq_sum = 0;
		int px_ct = 0;
		for (int y = start_y; y - stop_y != 0; y++) {
			final int sc_y = y >> back_scale_shift_bytes;
									if (!y_empty[sc_y])
										continue;
									for (int x = start_x; x - stop_x != 0; x++) {
										final int sc_x = x >> back_scale_shift_bytes;
									if (empty[sc_x ][sc_y]) {
										final float value = channel[x][y];
										if (value != PXV_BELOW_THRESHOLD) {
											sum += value;
											sq_sum += value*value;
											px_ct++;
										}
									}
									}
		}
		final double col_mean = sum / (double) px_ct;
		final double col_st_dev = Math.sqrt(sq_sum / (double) px_ct - col_mean * col_mean);

		// apply thresholding and find rough centre

		for (int y = start_y; y - stop_y != 0; y++) {
			final int sc_y = y >> back_scale_shift_bytes;
						if (!y_empty[sc_y])
							continue;
						for (int x = start_x; x - stop_x != 0; x++) {
							final int sc_x = x >> back_scale_shift_bytes;
				if (empty[sc_x ][sc_y]) {
					final float value = channel[x][y];
					if (value != PXV_BELOW_THRESHOLD) {
						final double abs = value - col_mean;
						if (abs*abs > color_sigma*col_st_dev) {
							channel[x][y] = PXV_ABOVE_THRESHOLD;
//							if (color_sigma == yellow_color_sigma)
//								setPixel(x, y, 255, 255, 0);
//							if (color_sigma == blue_color_sigma)
//								setPixel(x, y, 0, 0, 255);
//							if (color_sigma == red_color_sigma)
//								setPixel(x, y, 255, 0, 0);
						} else
							channel[x][y] = PXV_BELOW_THRESHOLD;
					}
				}
						}
		}
	}


	private final void filterByArea(final double area_px, final Point2D.Double center) {
		byte biggest_area = 0;
		// find areas of all blobs
		for (int y = start_y; y - stop_y != 0; y++) {
			final int sc_y = y >> back_scale_shift_bytes;
			if (!y_empty[sc_y])
				continue;
			for (int x = start_x; x - stop_x != 0; x++) {
				final int sc_x = x >> back_scale_shift_bytes;
				if (empty[sc_x ][sc_y]) {
					if (channel[x][y] == PXV_ABOVE_THRESHOLD) {
						queue.add(new Point(x, y));

						int pixelCount = 0;

						while (!queue.isEmpty()) {
							Point p = queue.remove();
							if ((p.x >= start_x) && (p.x < stop_x) && (p.y >= start_y) && (p.y < stop_y) && empty[p.x >> back_scale_shift_bytes][p.y >> back_scale_shift_bytes]) {
								if (channel[p.x][p.y] == PXV_ABOVE_THRESHOLD) {
									channel[p.x][p.y] = PXV_BEING_CHECKED;
									pixelCount++;
									queue.add(new Point(p.x + 1, p.y));
									queue.add(new Point(p.x - 1, p.y));
									queue.add(new Point(p.x, p.y + 1));
									queue.add(new Point(p.x, p.y - 1));
								} else if (channel[p.x][p.y] != PXV_BEING_CHECKED)
									channel[p.x][p.y] = PXV_BELOW_THRESHOLD;
							}
						}



						int area_temp = (int) (mask*Math.abs(pixelCount - area_px)/area_px);
						if (area_temp > mask)
							area_temp = mask;

						area_value = (byte) (mask - area_temp);
						//System.out.println("area "+pixelCount+" expected "+area_px+" area_val "+(area_value & mask));
						if ((area_value & mask) < PXV_AREA_INVALID_RATIO)
							area_value = PXV_BELOW_THRESHOLD;
						if ((area_value & mask) > (biggest_area & mask)) {
							biggest_area = area_value;
						}
						// set area
						for (int yy = start_y; yy < stop_y; yy++) {
							final int sc_yy = y >> back_scale_shift_bytes;
						if (!y_empty[sc_yy])
							continue;
						for (int xx = start_x; xx < stop_x; xx++) {
							if (empty[xx >> back_scale_shift_bytes][sc_yy] && (channel[xx][yy] & mask) == PXV_BEING_CHECKED)
								channel[xx][yy] = area_value;
						}
						}
					}
				}
			}
		}
		// get the com of the blob with biggest area

		if ((biggest_area & mask) < PXV_AREA_INVALID_RATIO || biggest_area == 0) {
			// there is no blob on screen
			center.x = -1;
			center.y = -1;
		} else {
			long cx = 0;
			long cy = 0;
			int px_count = 0;
			for (int y = start_y; y - stop_y != 0; y++) {
				final int sc_y = y >> back_scale_shift_bytes;
		if (!y_empty[sc_y])
			continue;
		for (int x = start_x; x - stop_x != 0; x++) {
			final int sc_x = x >> back_scale_shift_bytes;
			if (empty[sc_x ][sc_y]) {
				if (channel[x][y] == biggest_area) {
					cx += x;
					cy += y;
					px_count++;
					channel[x][y] = (byte) PXV_VALID_THRESHOLD;
				} else
					channel[x][y] = PXV_BELOW_THRESHOLD;
			}
		}
			}
			if (px_count != 0) {
				center.x = cx/px_count;
				center.y = cy/px_count;
			} else {
				center.x = -1;
				center.y = -1;
			}
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
		for (int y = start_y; y < stop_y; y++)
			for (int x = start_x; x < stop_x; x++) {
				final float coeff = channel[x][y] / 255f;
				final int nr = (int) (r*coeff),
						ng = (int) (g*coeff),
						nb = (int) (b*coeff);
				if (nr != 0 || ng != 0 || nb != 0)
					setPixel(x, y, nr, ng, nb);
			}
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
	 * @param r the value for red 0 .. mask
	 * @param g the value for green 0 .. mask
	 * @param b the value for blue 0 .. mask
	 */
	private final void setPixel(final int x, final int y, int r, int g, int b) {
		r = r < 0 ? 0 : (r > mask ? mask : r);
		g = g < 0 ? 0 : (g > mask ? mask : g);
		b = b < 0 ? 0 : (b > mask ? mask : b);
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
	private BufferedImage preprocessFrame(BufferedImage frame) {
		IplImage frame_ipl = IplImage.createFrom(frame);
		//frame_ipl = undistortImage(frame_ipl);
		//cvSetImageROI(frame_ipl, cvRect(config.getFieldLowX(), config.getFieldLowY(),
		//	config.getFieldWidth(), config.getFieldHeight()));		
		cvSmooth(frame_ipl, frame_ipl, CV_GAUSSIAN, 5);
		return frame_ipl.getBufferedImage();
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

	//	// low pass filtering
	//
	//	/**
	//	 * A simple low-pass filter
	//	 * @param old_value
	//	 * @param new_value
	//	 * @param amount
	//	 * @return a filtered value
	//	 */
	//	private final static double lowPass(final double old_value, final double new_value, final double amount) {
	//		return (old_value+new_value*amount)/((double) (amount+1));
	//	}
	//
	//	/**
	//	 * Low pass for angles
	//	 * @param old_value
	//	 * @param new_value
	//	 * @return the filtered angle
	//	 */
	//	private final static double lowPass(final double old_value, final double new_value) {
	//		return lowPass(old_value, new_value, filteredAngleAmount);
	//	}
	//
	//	/**
	//	 * Low pass on position
	//	 * @param old_value
	//	 * @param new_value
	//	 * @param amount
	//	 * @return the filtered position
	//	 */
	//	private final static Point2D.Double lowPass(final Point2D.Double old_value, final Point2D.Double new_value) {
	//		if (new_value.x == -1 && new_value.y == -1)
	//			return new_value;
	//		if (old_value.x == -1 && old_value.y == -1)
	//			return new_value;
	//		return new Point2D.Double (
	//				lowPass(old_value.getX(), new_value.getX(), filteredPositionAmount),
	//				lowPass(old_value.getY(), new_value.getY(), filteredPositionAmount));
	//	}
	//
	//	/**
	//	 * Low pass on a robot
	//	 * @param old_value
	//	 * @param new_value
	//	 * @param amount
	//	 * @return a new robot with low_pass
	//	 */
	//	private final static Robot lowPass(final Robot old_value, final Robot new_value) {
	//		return new Robot(
	//				lowPass(old_value.getCoords(), new_value.getCoords()),
	//				lowPass(old_value.getAngle(), new_value.getAngle()));
	//	}
}
