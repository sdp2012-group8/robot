package sdp.common;

import java.awt.Point;
import java.awt.geom.Point2D;

public class Tools {
	
	// pitch constants
	public final static double PITCH_WIDTH_CM = 244;
	public final static double PITCH_HEIGHT_CM = 113.7;
	public final static double GOAL_Y_CM = PITCH_HEIGHT_CM/2;

	public static double getDistBetweenPoints(Point p1, Point p2)
    {
    	return Math.sqrt((double) (Math.pow(p1.x-p2.x,2) + (Math.pow(p1.y-p2.y,2))));
    }
    
    public static Point getRelativePos(Point startPoint, Point relativePoint)
    {
    	return new Point(relativePoint.x - startPoint.x,relativePoint.y-startPoint.y);
    }
    
    
    public static double getAngleFrom0_0(Point pos)
    {
    	//deals with cases where pos is on the x-axis
    	if (pos.y == 0)
    	{
    		return (pos.x > 0 ? 0: Math.PI);
    	}
    	else
    	{
    		if (pos.x > 0)
    			return (Math.atan(((float) pos.y) / pos.x));
    		else
    			return (Math.PI + Math.atan(((float) pos.y) / pos.x));
    	}
    }
    
    public static void printCoors(String s, Point c)
    {
    	System.out.println(s + c.x + ", " + c.y);
    }
    
    public static void rest(int howLong)
    {
    	try {
			Thread.sleep(howLong);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    public static double getAngleToFacePoint(Point ourCoor, double angle, Point target) {

		// first I want to find where the target is in relation to our robot
		Point targetRelativePos = Tools.getRelativePos(ourCoor, target);
		
		double targetFromNxt = Tools.getAngleFrom0_0(targetRelativePos);

		
		if (targetFromNxt < 0)
			targetFromNxt = 2 * Math.PI + targetFromNxt;

		// now find how much our robot has to turn to face target
		// (turning by negative getAngle returns it to face 0 then add on ball Angle
		double howMuchToTurn = angle - targetFromNxt;

		// now adjust it so that it turns in the shortest direction (clockwise or counter clockwise)
		if (howMuchToTurn < -Math.PI)
			howMuchToTurn  = 2*Math.PI + howMuchToTurn;
		else if (howMuchToTurn > Math.PI)
			howMuchToTurn = - (2 * Math.PI - howMuchToTurn);

		
		return howMuchToTurn;

	}
	
    /**
     * Normalizes the given angle
     * @param initial given angle in degrees
     * @return normalized angle between -Pi and Pi
     */
    public static double normalizeAngle(double initial) {
    	initial = initial % 360;
    	if (initial > 180)
    		initial -= 360;
    	if (initial < -180)
    		initial += 360;
    	return initial;
    }

	/**
	 * Calculates distance between two points.
	 * @param p1 start point
	 * @param p2 end point
	 * @return sqrt((x1-x2)^2+(y1-y2)^2)
	 */
	public static double getDistanceBetweenPoint(Point2D.Double p1, Point2D.Double p2)
	{
		return Math.sqrt((double)(Math.pow(p1.x-p2.x,2)+(Math.pow(p1.y-p2.y,2))));
	}
    /**
     * Rotate point p2 around point p1 by deg degrees
     * @param p1 rotation point
     * @param p2 point to be rotated
     * @param deg degrees
     * @return new rotated coordinates
     */
    public static Point rotatePoint(Point p1, Point p2, int deg)
    {
    	p2.x -= p1.x;
    	p2.y -= p1.y;
    	double rad = (double) Math.toRadians(deg);
    	int xtemp;
    	xtemp = (int) Math.round((p2.x * (double)Math.cos(rad)) - (p2.y * (double)Math.sin(rad)));
    	p2.y = (int) Math.round((p2.x * (double)Math.sin(rad)) + (p2.y * (double)Math.cos(rad)));
    	p2.x = xtemp;
    	return new Point (p2.x+p1.x, p2.y+p1.y);
    }
    
    /**
     * Find the most common value in the given array
     * @param in the input array
     * @return the most common value
     */
    public static int goodAvg(int[] in){
    	int[] values = new int[in.length];
    	int[] counts = new int[in.length];
    	for(int i = 0; i < in.length; i++){
    		int c = gotInt(values, in[i]);
    		if(c != -1){
    			counts[c]++;
    		} else {
    	    	for(int ii = 0; ii < in.length; ii++){
    	    		if(values[ii] == 0)
    	    			values[ii] = in[ii];
    	    	}
    		}
    	}
    	int max = 0;
    	int maxi = -1;
    	for(int i = 0; i < in.length; i++){
    		if(counts[i] > max){
    			max = counts[i];
    			maxi = i;
    		}
    	}
    	return values[maxi];
    }
    /**
     * Check if array in contains integer n
     * @param in input array of ints
     * @param n search for this number
     * @return position or -1 if not found
     */
    public static int gotInt(int[] in, int n){
    	for(int i = 0; i < in.length; i++){
    		if(in[i] == n)
    			return i;
    	}
    	return -1;
    }
    /**
     * Push int n into array in. 0th element will be the new element.
     * last element will be lost
     * @param in array to be appended
     * @param n int to be inserted
     * @return new array
     */
    public static int[] push(int[] in, int n) {
		for(int i = in.length-1; i > 0; i--){
			in[i] = in[i-1];
		}
		in[0] = n;
		return in;
    }
    
	private static Point2D.Double toCentimeters(Point2D.Double original) {
		return new Point2D.Double(original.getX()*PITCH_WIDTH_CM, original.getY()*PITCH_WIDTH_CM);
	}
	
	private static Robot toCentimeters(Robot orig) {
		Robot robot = new Robot(toCentimeters(orig.getCoords()), orig.getAngle());
		robot.setCoords(true);
		return robot;
	}
	
	public static WorldState toCentimeters(WorldState orig) {
		return new WorldState(
				toCentimeters(orig.getBallCoords()),
				toCentimeters(orig.getBlueRobot()),
				toCentimeters(orig.getYellowRobot()),
				orig.getWorldImage());
	}
	
	/**
	 * Returns the vector to the closest collision point in the world (wall or enemy)
	 * 
	 * @param ls current world in centimeters
	 * @param am_i_blue true if my robot is blue, false otherwise; prevents testing with itself
	 * @param point the point to be tested, usually a point inside the robot (more usually edges of my robot)
	 * @return the vector to the closest point when collision may occur
	 */
	public static Vector2D getNearestCollisionPoint(WorldState ls, boolean am_i_blue, Point2D.Double point) {
		Robot enemy = am_i_blue ? ls.getYellowRobot() : ls.getBlueRobot();
		Vector2D[] enemy_pts = new Vector2D[] {
				new Vector2D(enemy.getFrontLeft()),
				new Vector2D(enemy.getFrontRight()),
				new Vector2D(enemy.getBackLeft()),
				new Vector2D(enemy.getBackRight())
		};
		// top wall test
		Vector2D temp = Vector2D.subtract(new Vector2D(0, PITCH_HEIGHT_CM), new Vector2D(0, point.getY()));
		Vector2D min = temp;
		// bottom wall test
		temp = Vector2D.subtract(new Vector2D(0, 0), new Vector2D(0, point.getY()));
		if (temp.getLength() < min.getLength())
			min = temp;
		// left wall test
		temp = Vector2D.subtract(new Vector2D(0, 0), new Vector2D(point.getX(), 0));
		if (temp.getLength() < min.getLength())
			min = temp;
		// right wall test
		temp = Vector2D.subtract(new Vector2D(PITCH_WIDTH_CM, 0), new Vector2D(point.getX(), 0));
		if (temp.getLength() < min.getLength())
			min = temp;
		// closest distance to enemy
		temp = closestDistance(enemy_pts, new Vector2D(point));
		if (temp.getLength() < min.getLength())
			min = temp;
		// we have our point
		return min;
	}
	
	/**
	 * Return the distance to the closest point in the set
	 * @param pts set of points
	 * @param pt the point we are standing at
	 * @return the distance from my point to the closest one in the set
	 */
	private static Vector2D closestDistance(Vector2D[] pts, Vector2D pt) {
		Vector2D min = null;
		for (int i = 0; i < pts.length; i++) {
			Vector2D temp = Vector2D.subtract(pts[i], pt);
			if (min == null || temp.getLength() < min.getLength())
				min = temp;
		}
		return min;
	}
	
	/**
	 * Generates input array for the AI
	 * 
	 * @param worldState in centimeters
	 * @param am_i_blue
	 * @param my_goal_left
	 * @return the input array
	 */
	public static double[] generateAIinput(WorldState worldState, boolean am_i_blue, boolean my_goal_left) {
		Robot me = am_i_blue ? worldState.getBlueRobot() : worldState.getYellowRobot();
		//Robot enemy = am_i_blue ? worldState.getYellowRobot() : worldState.getBlueRobot();
		Vector2D goal = new Vector2D(my_goal_left ? Tools.PITCH_WIDTH_CM : 0, Tools.GOAL_Y_CM);
		// get coordinates relative to table
		Vector2D my_coords = new Vector2D(me.getCoords());
		//Vector2D en_coords = new Vector2D(enemy.getCoords());
		Vector2D ball = new Vector2D(worldState.getBallCoords());
		Vector2D nearest = Tools.getNearestCollisionPoint(worldState, am_i_blue, me.getCoords());
		// rel angles
		double angle_to_ball = getTurningAngle(me, ball);
		double dist_to_ball = Vector2D.subtract(Vector2D.divide(Vector2D.add(new Vector2D(me.getFrontLeft()), new Vector2D(me.getFrontRight())),2), ball).getLength();
		double angle_to_goal = getTurningAngle(me, goal);
		double dist_to_goal = Vector2D.subtract(Vector2D.divide(Vector2D.add(new Vector2D(me.getFrontLeft()), new Vector2D(me.getFrontRight())),2), goal).getLength();
		//double angle_to_en = getTurningAngle(me, en_coords);
		//double dist_to_en = Vector2D.subtract(my_coords, en_coords).getLength();
		//double angle_collis = getTurningAngle(me, coll);
		double dist_near = nearest.getLength();
		double angle_near = getTurningAngle(me, Vector2D.add(my_coords, nearest));
		// if you change something here, don't forget to change number of inputs in trainer
		return new double[] {
				AI_normalizeAngleTo1(angle_to_ball),
				AI_normalizeCoordinateTo1(dist_to_ball, PITCH_WIDTH_CM),
				AI_normalizeAngleTo1(angle_to_goal),
				AI_normalizeCoordinateTo1(dist_to_goal, PITCH_WIDTH_CM),
				AI_normalizeAngleTo1(angle_near),
				AI_normalizeCoordinateTo1(dist_near, PITCH_WIDTH_CM),
				
		};
	}
	
	/**
	 * Gets how many degrees should a robot turn in order to face a point
	 * Units don't matter as long as they are consistent.
	 * @param me
	 * @param point
	 * @return
	 */
	public static double getTurningAngle(Robot me, Vector2D point) {
		return Tools.normalizeAngle(-me.getAngle()+Vector2D.getDirection(new Vector2D(-me.getCoords().getX()+point.getX(), -me.getCoords().getY()+point.getY())));
	}
	
	/**
	 * FOR AI ONLY, DON'T USE FOR ANYTHING ELSE!
	 * Maps distance between 0 and 1
	 * @param length the length in centimeters
	 * @param threshold the normalization; if length is meaningful only for short distances, use smaller one
	 * @return mapped between 0 and 1 wrt width of pitch
	 */
	private static double AI_normalizeCoordinateTo1(double length, double threshold) {
		if (length > threshold)
			return 0;
		length = (threshold-length)/threshold;
		if (length < 0)
			length = 0;
		if (length > 1)
			length = 1;
		return length;
	}
	
	private static double AI_normalizeAngleTo1(double angle) {
		return (180+normalizeAngle(angle))/360;
	}
	
	/**
	 * Change in state of a robot
	 */
	private static double delta(Robot old_r, Robot new_r) {
		return old_r.getCoords().distance(new_r.getCoords())+Math.abs(new_r.getAngle()-old_r.getAngle());
	}
	
	/**
	 * Returns differences in two world states. If nothing changed a lot, the number would be very small
	 * 
	 * @param old_w
	 * @param new_w
	 * @return
	 */
	public static double delta(WorldState old_w, WorldState new_w) {
		return delta(old_w.getBlueRobot(), new_w.getBlueRobot())+
				delta(old_w.getYellowRobot(), new_w.getYellowRobot())+
				new_w.getBallCoords().distance(old_w.getBallCoords());
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
		int id = 0;
		for (int i = 0; i < output.length; i++) {
			if (output[i] == Double.NaN)
				return -1;
			if (output[i] > max) {
				max = output[i];
				id = i;
			}
		}
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
	
	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(Object[] array) {
		if (array == null || array.length == 0)
			return "[\tEMPTY\t]";
		if (array.length == 1)
			return "[\t"+array[0]+"\t]";
		String ans = "["+array[0];
		for (int i = 1; i < array.length; i++)
			ans=ans+"\t"+array[i];
		return ans+"\t]";
	}
	
	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(Double[] array) {
		Double[] ans = new Double[array.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = array[i];
		return printArray(ans);
	}
	
	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(int[] array) {
		Integer[] ans = new Integer[array.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = array[i];
		return printArray(ans);
	}
	
	/**
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(double[] array) {
		Double[] ans = new Double[array.length];
		for (int i = 0; i < ans.length; i++)
			ans[i] = array[i];
		return printArray(ans);
	}

}
