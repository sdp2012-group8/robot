package sdp.common;

/**
 * Neural network shared tools
 * 
 * @author martinmarinov
 *
 */
public class NNetTools {

	/**
	 * Generates input array for the AI
	 * 
	 * @param worldState in centimeters
	 * @param am_i_blue
	 * @param my_goal_left
	 * @param id which network is receiving it
	 * @return the input array
	 */
	public static double[] generateAIinput(WorldState worldState, boolean am_i_blue, boolean my_goal_left, int id) {
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
			return new double[] {
					AI_normalizeCoordinateTo1(rel_ball.getX(), Tools.PITCH_WIDTH_CM)
					//AI_normalizeCoordinateTo1(rel_en.getX(), Tools.PITCH_WIDTH_CM),
					//AI_normalizeCoordinateTo1(rel_coll.getX(), Tools.PITCH_WIDTH_CM)
			};
		case 1:
			return new double[] {
					AI_normalizeAngleTo1(Vector2D.getDirection(rel_ball))
					//AI_normalizeAngleTo1(Vector2D.getDirection(rel_en)),
					//AI_normalizeAngleTo1(Vector2D.getDirection(rel_coll))
			};
		}
		return null;		
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

	/**
	 * What was the original condition
	 * 
	 * @param output array with two outputs from neural network
	 * @return the state of the original condition
	 */
	public static int recoverOutput(double[] output) {
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
