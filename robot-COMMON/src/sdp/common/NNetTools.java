package sdp.common;

/**
 * Neural network shared tools
 * 
 * @author martinmarinov
 *
 */
public class NNetTools {
	
	public static enum move_modes {
		forward_left, forward_right,  backward_left, backward_right, forward, backward
		//stop,  left, right 
	}

	/**
	 * Generates input array for the AI
	 * 
	 * @param worldState in centimeters
	 * @param am_i_blue
	 * @param my_goal_left
	 * @param id which network is receiving it
	 * @return the input array
	 */
	public static double[] generateAIinput(WorldState worldState, WorldState oldState, double dt, boolean am_i_blue, boolean my_goal_left, int id) {
		// getTargetDirection(worldState, am_i_blue, 10, new Vector2D(0, Tools.PITCH_HEIGHT_CM/2d - 60/2), new Vector2D(0, Tools.PITCH_HEIGHT_CM/2d + 60/2)) for left goal
		// ball getTargetDirection(worldState, am_i_blue, 50, ball, ball))
		
		Vector2D ball = new Vector2D(worldState.getBallCoords());
		Vector2D ball_rel = Tools.getLocalVector(am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot(), ball);
		ball_rel.setX(ball_rel.getX()-Robot.LENGTH_CM/2);
		switch (id) {
		case 0:
			return Tools.concat(
					// TODO! Tweaking those parameters
					getAvailableSectors(worldState, am_i_blue, 20, 30, 30),
					new double[] {
						AI_normalizeAngleTo1(Vector2D.getDirection(ball_rel)),
						AI_normalizeCoordinateTo1(ball_rel.getLength(), Tools.PITCH_WIDTH_CM)
					});
		}
		return null;		
	}
	
	/**
	 * Gets the available sectors. Sectors are the space where the robot can go without encountering obstacles. This is done by a "scanning"
	 * sensor that rotates around the robot at a small angle. If it detects an object it sets the current sector that the sensor is pointing at
	 * to be unavailable and goes to the next sector until it reaches the beginning.
	 * @param ws current worldState
	 * @param am_i_blue true when my robot is blue, false if it is yellow
	 * @param sec_count number of sectors
	 * @param scan_count number of scans
	 * @param sensor_threshold the threshold at which the sensor triggers
	 * @return a array containing -1 to 1 for every sector: whether it is available or it is blocked by an object
	 */
	public static double[] getAvailableSectors(WorldState ws, boolean am_i_blue, int sec_count, int scan_count, double sensor_threshold) {
		Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		int sector_angle = 360/sec_count;
		double scan_angle = 360d/scan_count;
		Vector2D zero = Vector2D.ZERO();
		int sector_id = 0;
		double[] ans = new double[sec_count];
		// set all sectors available
		for (int i = 0; i < ans.length; i++)
			ans[i] = -1;
		for (double angle = 0; angle < 360; angle+=scan_angle) {
			sector_id = ((int) angle) /  sector_angle;
			if (sector_id >= sec_count)
				break;
			double ang_rad = angle*Math.PI/180d;
			double dist = Tools.raytraceVector(ws, me, zero, new Vector2D(Math.cos(ang_rad), Math.sin(ang_rad)), am_i_blue).getLength();
			if (dist < sensor_threshold) {
				ans[sector_id] = 1;//AI_normalizeDistanceTo1(dist, sensor_threshold);
				// skip to next sector
				angle = (sector_id+1)*sector_angle-scan_angle;
			}
		}
		return ans;
	}
	
	/**
	 * 
	 * @param ws current worldState
	 * @param am_i_blue true when my robot is blue, false if it is yellow
	 * @param sec_count number of sectors
	 * @param target_start start of target line in table coordinates
	 * @param target_end end of target line in table coordinates
	 * @return a array containing -1 to 1 for every sector: whether there is a target in given sector
	 */
	public static double[] getTargetDirection(WorldState ws, boolean am_i_blue, int sec_count, Vector2D target_start, Vector2D target_end) {
		Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		Vector2D rel_target_start = Tools.getLocalVector(me, target_start);
		double target_start_angle = Vector2D.getDirection(rel_target_start);
		Vector2D rel_target_end = Tools.getLocalVector(me, target_end);
		double target_end_angle = Vector2D.getDirection(rel_target_end);
		if (Utilities.normaliseAngle(target_start_angle - target_end_angle) > 0) {
			double temp = target_start_angle;
			target_start_angle = target_end_angle;
			target_end_angle = temp;
		}
		double sector_angle = 360d/sec_count;
		double[] ans = new double[sec_count];
		for (int i = 0; i < ans.length; i++) {
			double sec_start_ang = sector_angle*i;
			double sec_end_ang = sector_angle*(i+1);
			boolean target_inside = ! (Utilities.normaliseAngle(sec_start_ang-target_end_angle) >= 0 || Utilities.normaliseAngle(sec_end_ang-target_start_angle) < 0); 
			ans[i] = target_inside ? 1 : -1;
		}
		return ans;
	}
	
	/**
	 * Converts dist within the scale -1 to 1 taking account threshold
	 * @param dist
	 * @param threshold
	 * @return if dist >= threshold returns -1, if dist = 0 returns 1, otherwise returns between -1 and 1
	 */
	private static double AI_normalizeDistanceTo1(double dist, double threshold) {
		if (dist >= threshold)
			return -1;
		double frac = dist/threshold;
		double dis = 1-frac*2;
		return dis;
	}
	
	/**
	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
	 * Maps distance between 0 and 1
	 * @param length the length in centimeters
	 * @param threshold the normalization; if length is meaningful only for short distances, use smaller one
	 * @return mapped between 0 and 1 wrt width of pitch
	 */
	private static double AI_normalizeCoordinateTo1(double length, double threshold) {
		length = length/threshold;//(threshold-length)/threshold;
		if (length < -1)
			length = -1;
		if (length > 1)
			length = 1;
		return length;
	}

	/**
	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
	 * Maps angle between 0 and 1
	 * @param angle the angle in deg
	 * @return mapped between -1 and 1 wrt width of pitch
	 */
	private static double AI_normalizeAngleTo1(double angle) {
		return (Utilities.normaliseAngle(angle))/180d;
	}
	
	/**
	 * Generate training output
	 * @param condition condition to be encoded
	 * @return output
	 */
	public static double[] generateOutput(int current_id, int max) {
		double[] ans  = new double[max];
		for (int i = 0; i < ans.length; i++)
			ans[i] = i == current_id ? 1 : 0;
		return ans;
	}
	
	public static double[] generateOutput(move_modes mode) {
		if (mode == null)
			return null;
		return generateOutput(mode.ordinal(), move_modes.values().length);
	}

	/**
	 * What was the original condition
	 * 
	 * @param output array with two outputs from neural network
	 * @return the state of the original condition
	 */
	public static int recoverOutputInt(double[] output) {
		double max = 0;
		double sum = 0;
		int id = 0;
		for (int i = 0; i < output.length; i++) {
			sum+=output[i];
			if (output[i] == Double.NaN)
				return -1;
			if (output[i] > max) {
				max = output[i];
				id = i;
			}
		}
		if (sum == 0)
			return -1;
		return id;
	}
	
	public static move_modes recoverOutputMode(double[] output) {
		int id = recoverOutputInt(output);
		return move_modes.values()[id];
	}
	
	public static move_modes getMode(int desired_speed, int desired_turning) {
		if (desired_speed > 0 && desired_turning > 0)
			return move_modes.forward_right;
		if (desired_speed > 0 && desired_turning == 0)
			return move_modes.forward;
		if (desired_speed > 0 && desired_turning < 0)
			return move_modes.forward_left;
		
		if (desired_speed == 0 && desired_turning > 0)
			return null;//move_modes.right;
		if (desired_speed == 0 && desired_turning == 0)
			return null;//move_modes.stop;
		if (desired_speed == 0 && desired_turning < 0)
			return null;//move_modes.left;

		if (desired_speed < 0 && desired_turning > 0)
			return move_modes.backward_right;
		if (desired_speed < 0 && desired_turning == 0)
			return move_modes.backward;
		if (desired_speed < 0 && desired_turning < 0)
			return move_modes.backward_left;
		
		return null;
	}
	
	public static int getDesiredSpeed(move_modes mode) {
		switch (mode) {
		case forward:
		case forward_left:
		case forward_right:
			return 35;
		case backward:
		case backward_left:
		case backward_right:
			return -35;
		default:
			return 0;
		}
	}
	
	public static int getDesiredTurningSpeed(move_modes mode) {
		switch (mode) {
		case forward_right:
		case backward_right:
		//case right:
			return 90;
		case forward_left:
		case backward_left:
		//case left:
			return -90;
		default:
			return 0;
		}
	}
	
	/**
	 * AI:
	 * Gives the calculated probability of taking action item
	 * give network output
	 * @param item
	 * @param output
	 * @return
	 */
	public static double probability(int item, double[] output) {
		double sum = 0;
		for (int i = 0; i < output.length; i++)
			sum+=output[i];
		return output[item]/sum;
	}
	
}
