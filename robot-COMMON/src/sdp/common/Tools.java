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

	public static boolean isLeft(Point2D.Double a, Point2D.Double b, Point2D.Double c){
		return ((b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x)) > 0;
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
		return getNearestCollisionPoint(ls, am_i_blue, point, true);
	}
	
	/**
	 * Returns the vector to the closest collision point in the world (wall or enemy)
	 * 
	 * @param ls current world in centimeters
	 * @param am_i_blue true if my robot is blue, false otherwise; prevents testing with itself
	 * @param point the point to be tested, usually a point inside the robot (more usually edges of my robot)
	 * @param include_enemy whether to include enemy
	 * @return the vector to the closest point when collision may occur
	 */
	public static Vector2D getNearestCollisionPoint(WorldState ls, boolean am_i_blue, Point2D.Double point, boolean include_enemy) {
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
		if (include_enemy) {
		temp = closestDistance(enemy_pts, new Vector2D(point));
		if (temp.getLength() < min.getLength())
			min = temp;
		}
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
	 * Gets how many degrees should a robot turn in order to face a point
	 * Units don't matter as long as they are consistent.
	 * @param me
	 * @param point
	 * @return
	 */
	public static double getTurningAngle(Robot me, Vector2D point) {
		return Utilities.normaliseAngle(-me.getAngle()+Vector2D.getDirection(new Vector2D(-me.getCoords().getX()+point.getX(), -me.getCoords().getY()+point.getY())));
	}
	
	/**
	 * Transforms a vector from table coordinates to robot coordinates
	 * @param me
	 * @param vector
	 * @return
	 */
	public static Vector2D getLocalVector(Robot me, Vector2D vector) {
		return Vector2D.rotateVector(Vector2D.subtract(vector, new Vector2D(me.getCoords())), -me.getAngle());
	}
	
	/**
	 * Converts local coordinate (generated by {@link #getLocalVector(Robot, Vector2D)}) to a table coordinate.
	 * @param me
	 * @param local
	 * @return
	 */
	public static Vector2D getGlobalVector(Robot me, Vector2D local) {
		return  Vector2D.add(
				Vector2D.rotateVector(local, me.getAngle()),
				new Vector2D(me.getCoords()));
	}
	
	public static Vector2D getNearestCollisionPointFromMyPerspective(Robot me, Point2D.Double my_pos, WorldState worldState, boolean am_i_blue) {
		return getLocalVector(me, Vector2D.add(new Vector2D(my_pos), Tools.getNearestCollisionPoint(worldState, am_i_blue, new Vector2D(my_pos))));
	}

	
	/**
	 * Starting from origin in the given direction, find the first point of collision in the scene
	 * @param origin the start of the vector
	 * @param direction size doesn't matter, only the angle is relevant
	 * @param ignore_blue true to ignore blue robot, false to ignore yellow, null to include both
	 * @return a {@link Vector2D} in the same direction as direction but with greater length (distance from origin to the nearest collision point, raytraced along direction's direction)
	 */
	public static Vector2D raytraceVector(WorldState ws, Vector2D origin, Vector2D direction, Boolean ignore_blue) {
		if (origin.getX() <= 0 || origin.getY() <= 0 || origin.getX() >= PITCH_WIDTH_CM || origin.getY() >= PITCH_HEIGHT_CM)
			return Vector2D.ZERO();
		Vector2D near;
		Vector2D temp = vectorLineIntersection(origin, direction, new Vector2D(0, 0), new Vector2D(PITCH_WIDTH_CM, 0));
		near = temp;
		temp = vectorLineIntersection(origin, direction, new Vector2D(PITCH_WIDTH_CM, 0), new Vector2D(PITCH_WIDTH_CM, PITCH_HEIGHT_CM));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		temp = vectorLineIntersection(origin, direction, new Vector2D(PITCH_WIDTH_CM, PITCH_HEIGHT_CM), new Vector2D(0, PITCH_HEIGHT_CM));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		temp = vectorLineIntersection(origin, direction, new Vector2D(0, PITCH_HEIGHT_CM), new Vector2D(0, 0));
		if (temp != null && (near == null || temp.getLength() < near.getLength()))
			near = temp;
		// collision with a Robot
		for (int i = 0; i <= 1; i++) {
			if (ignore_blue != null && ((ignore_blue ? 0 : 1) == i))
				continue;
			Robot robot = i == 0 ? ws.getBlueRobot() : ws.getYellowRobot();
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getFrontLeft()), new Vector2D(robot.getFrontRight()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getFrontRight()), new Vector2D(robot.getBackRight()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getBackRight()), new Vector2D(robot.getBackLeft()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
			temp = vectorLineIntersection(origin, direction, new Vector2D(robot.getBackLeft()), new Vector2D(robot.getFrontLeft()));
			if (temp != null && (near == null || temp.getLength() < near.getLength()))
				near = temp;
		}
		if (near != null) 
			return near;
		return
				Vector2D.change_length(direction, PITCH_WIDTH_CM);
	}
	
	/**
	 * Raytrace vector with relation to a robot
	 * @param ws
	 * @param robot
	 * @param local_origin In robot's coordinate system. Make sure it is outside robot to avoid false readings!
	 * @param local_direction In robot's coordinate system
	 * @return same output as {@link #raytraceVector(WorldState, Vector2D, Vector2D)} - in table coordinates
	 */
	public static Vector2D raytraceVector(WorldState ws, Robot robot, Vector2D local_origin, Vector2D local_direction) {
		return raytraceVector(ws, robot, local_origin, local_direction, null);
	}
	
	
	/**
	 * Raytrace vector with relation to a robot
	 * @param ws
	 * @param robot
	 * @param local_origin In robot's coordinate system. Make sure it is outside robot to avoid false readings!
	 * @param local_direction In robot's coordinate system
	 * @param am_i_blue if true, ignores blue if false ignores yellow. To include all robots, use {@link #raytraceVector(WorldState, Robot, Vector2D, Vector2D)}
	 * @return same output as {@link #raytraceVector(WorldState, Vector2D, Vector2D)} - in table coordinates
	 */
	public static Vector2D raytraceVector(WorldState ws, Robot robot, Vector2D local_origin, Vector2D local_direction,  Boolean am_i_blue) {
		Vector2D origin = Tools.getGlobalVector(robot, local_origin);
		Vector2D direction = Vector2D.subtract(origin, Tools.getGlobalVector(robot, local_direction));
		return raytraceVector(ws, origin, direction, am_i_blue);
	}
	
	/**
	 * Whether there is direct visibility from a point of the pitch to another one.
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static boolean visibility(WorldState ws, Vector2D endPt, boolean am_i_blue) {
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		Vector2D ray = raytraceVector(ws, startPt, dir, am_i_blue);
		return ray.getLength() >= dir.getLength();
	}
	
	
	/**
	 * Whether you could reach this point if you turn into its direction and go into straight line.
	 * @param ws
	 * @param robot
	 * @param startPt
	 * @param endPt
	 * @return
	 */
	public static boolean reachability(WorldState ws, Vector2D endPt, boolean am_i_blue) {
		if (!visibility(ws, endPt, am_i_blue))
			return false;
		Robot robot = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot(); 
		Vector2D startPt = new Vector2D(robot.getCoords());
		Vector2D dir =  Vector2D.subtract(endPt, startPt);
		double dir_l = dir.getLength();
		double angle = (-Vector2D.getDirection(dir)+90)*Math.PI/180d;
		final double length = Robot.LENGTH_CM/2;
		double cos = Math.cos(angle)*length;
		double sin = Math.sin(angle)*length;
		Vector2D left = new Vector2D(cos, sin);
		Vector2D right = new Vector2D(-cos, -sin);

		Vector2D ray_left = Tools.raytraceVector(ws, Vector2D.add(startPt, left), dir, am_i_blue);
		if (ray_left.getLength() < dir_l)
			return false;
		Vector2D ray_right = Tools.raytraceVector(ws, Vector2D.add(startPt, right), dir, am_i_blue);
		return ray_right.getLength() >= dir_l;
	}
	
	/**
	 * Return the intersection of a vector in the given direction, originating from origin.
	 * @param origin
	 * @param direction
	 * @param lineStart
	 * @param lineEnd
	 * @return the direction vector with correct length if there is intersection, otherwise returns null
	 */
	public static Vector2D vectorLineIntersection(Vector2D origin, Vector2D direction, Vector2D lineStart, Vector2D lineEnd) {
		Vector2D loc_start = Vector2D.rotateVector(Vector2D.subtract(lineStart, new Vector2D(origin)), -Vector2D.getDirection(direction));
		Vector2D loc_end = Vector2D.rotateVector(Vector2D.subtract(lineEnd, new Vector2D(origin)), -Vector2D.getDirection(direction));
		if (loc_start.getX() < 0 && loc_end.getX() < 0)
			return null; // if the vector is facing the other way
		if (loc_start.getY() * loc_end.getY() <= 0 ) {
			// if there is intersection i.e. the local coordinates are on different sides of the local axis
			double length = loc_start.getX()-(loc_start.getY()*(loc_end.getX()-loc_start.getX())/(loc_end.getY()-loc_start.getY()));
			if (length < 0)
				return null;
			return Vector2D.change_length(direction, length);
		} else
			return null;
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
	 * Just for printing arrays
	 * @param array
	 * @return
	 */
	public static String printArray(Object[] array) {
		if (array == null || array.length == 0)
			return "[EMPTY]";
		if (array.length == 1)
			return "["+array[0]+"]";
		String ans = "["+array[0];
		for (int i = 1; i < array.length; i++)
			ans=ans+"\t"+array[i];
		return ans+"]";
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
	
	/**
	 * Join given arrays into one in the given order
	 * @param arrays
	 * @return
	 */
	public static double[] concat(double[]...arrays) {
		int sum = 0;
		for (int i = 0; i < arrays.length; i++)
			sum += arrays[i].length;
		double[] ans = new double[sum];
		int id = 0;
		for (int i = 0; i < arrays.length; i++)
			for (int j = 0; j < arrays[i].length; j++) {
				ans[id] = arrays[i][j];
				if (ans[id] == Double.NaN)
					ans[id] = 0;
				id++;
			}
		return ans;
	}

	/**
	 * Sweeps a scanning vector sensor between the given angles and returns the distance to the closest object. Angles are wrt to the current robot
	 * where angle 0 means forward, 180 or -180 means backwards, 90 means left, -90 means right of robot. <br/>
	 * The result could be interpreted as: <i>the distance to nearest obstacle in the specified region about the current robot</i>
	 * @param ws current world state
	 * @param am_i_blue true if my robot is blue, false if it is yellow
	 * @param start_angle the starting angle of the segment (the smallest arc will be selected)
	 * @param end_angle the ending angle of the segment (the smallest arc will be selected)
	 * @param scan_count how many parts the sector should be devided for scanning
	 * @return the vector distance to the closest collision point a.k.a. the minimum distance determined by the scanning vector which swept the sector scan_count times.
	 */
	public static Vector2D getSector(WorldState ws, boolean am_i_blue, double start_angle, double end_angle, int scan_count) {
		start_angle = Utilities.normaliseAngle(start_angle);
		end_angle = Utilities.normaliseAngle(end_angle);
		final Robot me = am_i_blue ? ws.getBlueRobot() : ws.getYellowRobot();
		final Vector2D zero = Vector2D.ZERO();
		Double min_dist = null;
		Vector2D min_vec = null;
		final double sector_angle = Utilities.normaliseAngle(end_angle-start_angle);
		final double scan_angle = sector_angle/scan_count;
		for (double angle = start_angle; Utilities.normaliseAngle(end_angle-angle) * sector_angle >= 0; angle+=scan_angle) {
			final double ang_rad = angle*Math.PI/180d;
			final Vector2D distV = raytraceVector(ws, me, zero, new Vector2D(-Math.cos(ang_rad), Math.sin(ang_rad)), am_i_blue);
			final double dist = distV.getLength();
			if (min_dist == null || dist < min_dist) {
				min_dist = dist;
				min_vec = distV;
			}
		}
		return min_vec;
	}

	public static double[] getSectors(WorldState ws, boolean am_i_blue, int scan_count, int sector_count, boolean normalize_to_1) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = normalize_to_1 ?
					NNetTools.AI_normalizeDistanceTo1(getSector(ws, am_i_blue, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle), scan_count), PITCH_WIDTH_CM) :
					getSector(ws, am_i_blue, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle), scan_count).getLength();
		return ans;
	}

	static double[] getTargetInSectors(Vector2D relative, int sector_count) {
		if (sector_count % 2 != 0 || (sector_count / 2) % 2 == 0) {
			System.out.println("Sectors must be even number which halves should be odd!");
			return null;
		}
		double[] ans = new double[sector_count];
		double sec_angle = 360d/sector_count;
		for (int i = 0; i < sector_count; i++)
			ans[i] = NNetTools.AI_normalizeDistanceTo1(NNetTools.targetInSector(relative, Utilities.normaliseAngle(-90+i*sec_angle), Utilities.normaliseAngle(-90+(i+1)*sec_angle)), PITCH_WIDTH_CM);
		return ans;
	}
}
