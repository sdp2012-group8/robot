package sdp.common;

/**
 * Neural network shared tools
 * 
 * @author martinmarinov
 *
 */
public class NNetTools {
	
	public static enum move_modes {
		forward_left, forward, forward_right, stop, backward_left, backward, backward_right, left, right 
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
		Robot me = am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot();
		Robot enemy = am_i_blue ? worldState.getYellowRobot() : worldState.getBlueRobot();
		//Vector2D goal = new Vector2D(my_goal_left ? Tools.PITCH_WIDTH_CM : 0, Tools.GOAL_Y_CM);
		// get coordinates relative to table
		Vector2D my_coords = new Vector2D(me.getCoords());
		Vector2D en_coords = new Vector2D(enemy.getCoords());
		Vector2D ball = new Vector2D(worldState.getBallCoords());
		Vector2D nearest = Tools.getNearestCollisionPoint(worldState, am_i_blue, me.getCoords());
		// rel coords
		Vector2D rel_ball = Tools.getLocalVector(me, ball);
		//Vector2D rel_goal = Tools.getLocalVector(me, goal);
		Vector2D rel_coll = Tools.getLocalVector(me, Vector2D.add(my_coords, nearest));
		Vector2D rel_en = Tools.getLocalVector(me, en_coords);
		// if you change something here, don't forget to change number of inputs in trainer
		switch (id) {
		case 0:
			return /*Tools.concat(*/new double[] {
					AI_normalizeCoordinateTo1(rel_ball.getLength(), Tools.PITCH_WIDTH_CM),
					AI_normalizeAngleTo1(Vector2D.getDirection(rel_ball)),
					AI_normalizeCoordinateTo1(rel_en.getLength(), Tools.PITCH_WIDTH_CM),
					AI_normalizeAngleTo1(Vector2D.getDirection(rel_en)),
					AI_normalizeCoordinateTo1(Tools.raytraceVector(worldState, me, new Vector2D(0,0), new Vector2D(-Robot.WIDTH_CM/2,-Robot.WIDTH_CM/2), am_i_blue).getLength(), 60),
					AI_normalizeCoordinateTo1(Tools.raytraceVector(worldState, me, new Vector2D(0,0), new Vector2D(-Robot.WIDTH_CM/2,Robot.WIDTH_CM/2), am_i_blue).getLength(), 60),
					AI_normalizeCoordinateTo1(Tools.raytraceVector(worldState, me, new Vector2D(0,0), new Vector2D(Robot.WIDTH_CM/2,-Robot.WIDTH_CM/2), am_i_blue).getLength(), 60),
					AI_normalizeCoordinateTo1(Tools.raytraceVector(worldState, me, new Vector2D(0,0), new Vector2D(Robot.WIDTH_CM/2,Robot.WIDTH_CM/2), am_i_blue).getLength(), 60),
					AI_normalizeCoordinateTo1(Tools.raytraceVector(worldState, me, new Vector2D(0,0), new Vector2D(1,0), am_i_blue).getLength(), 60),
					AI_normalizeCoordinateTo1(Tools.raytraceVector(worldState, me, new Vector2D(0,0), new Vector2D(-1,0), am_i_blue).getLength(), 60)
					//AI_normalizeAngleTo1(Vector2D.getDirection(rel_ball))
					//AI_normalizeCoordinateTo1(rel_en.getX(), Tools.PITCH_WIDTH_CM),
					//AI_normalizeCoordinateTo1(rel_coll.getX(), Tools.PITCH_WIDTH_CM)
			};//, getVisionMatrix(20, Tools.PITCH_WIDTH_CM, 35, worldState, oldState, dt, am_i_blue));
		}
		return null;		
	}
	
	/**
	 * Generates series of vectors around the front and back of the robot
	 * @param size the elements in array. Front and back must have odd sizes so a value of 10 would mean 5 in front and 5 in back.
	 * @param threshold
	 * @return distances to nearest collision points in the direction, normalized to threshold
	 */
	private static double[] getVisionMatrix(int size, double threshold_length, double threshold_velocity, WorldState ws, WorldState old, double dt, boolean am_i_blue) {
		Robot oldme = am_i_blue ? old.getBlueRobot() : old.getYellowRobot();
		Robot newme = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		final double[] vels = new double[size];
		final double[] dists = new double[size];
		int i = 0;
		for (double angle = -Math.PI; angle < Math.PI; angle += 2*Math.PI/size) {
			vels[i] = 
					(AI_normalizeCoordinateTo1(Tools.raytraceVector(ws, newme, new Vector2D(0, 0), new Vector2D(Math.cos(angle), Math.sin(angle)), am_i_blue).getLength(), threshold_velocity) -
					AI_normalizeCoordinateTo1(Tools.raytraceVector(old, oldme, new Vector2D(0, 0), new Vector2D(Math.cos(angle), Math.sin(angle)), am_i_blue).getLength(), threshold_velocity))/dt;
			dists[i] = AI_normalizeCoordinateTo1(Tools.raytraceVector(ws, newme, new Vector2D(0, 0), new Vector2D(Math.cos(angle), Math.sin(angle)), am_i_blue).getLength(), threshold_length);
			i++;
		}
		return Tools.concat(vels, dists);
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
		return (Tools.normalizeAngle(angle))/180d;
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
			return move_modes.right;
		if (desired_speed == 0 && desired_turning == 0)
			return move_modes.stop;
		if (desired_speed == 0 && desired_turning < 0)
			return move_modes.left;

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
		case right:
			return 90;
		case forward_left:
		case backward_left:
		case left:
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
