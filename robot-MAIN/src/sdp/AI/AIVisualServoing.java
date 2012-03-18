package sdp.AI;

import java.awt.geom.Point2D;
import java.io.IOException;

import sdp.common.Robot;
import sdp.common.Utilities;
import sdp.common.Vector2D;

public class AIVisualServoing extends AI {
	
	
	/** The multiplier to the final speed to slow it down */
	private final static double SPEED_MULTIPLIER = 0.7;

	private final static int COLL_SECS_COUNT = 222;
	private final static double SEC_ANGLE = 360d/COLL_SECS_COUNT;
	
	// corner thresholds
	private final static int THRESH_CORN_HIGH = 5;
	private final static int THRESH_CORN_LOW = 2;
	
	// back away threshold
	private final static int THRESH_BACK_HIGH = 10;
	private final static int THRESH_BACK_LOW = 2;
	
	/** Threshold for being at the target point */
	private final static int POINT_ACCURACY = 5;
	public static final int DIST_TO_BALL = 6;

	private final static int MAX_TURN_ANG = 127;
	public final static double DEFAULT_POINT_OFF = 2*Robot.LENGTH_CM;
	private double point_off = DEFAULT_POINT_OFF;
	public final static double TARG_THRESH = 40;
	
	

	/**
	 * True if the robot is chasing the target.
	 * False if the robot is chasing the real ball.
	 */
	private boolean chasing_target = true;
	
	private Vector2D target = null;

	@Override
	protected Command chaseBall() throws IOException {
		Command comm = null;
		
		
		if (ai_world_state.getDistanceToBall() < DIST_TO_BALL){
			return gotBall();
		}

		try {
			
			target = new Vector2D(Utilities.getOptimalPointBehindBall(ai_world_state, point_off));
		
		} catch (NullPointerException e) {
			// Robot can't see a goal
			// TODO: decide on what to do when the robot can't see the goal.
			//System.out.println("Can't see a goal");
			target = new Vector2D(ai_world_state.getBallCoords());
		}


		double targ_dist = distanceTo(target);
		double direction = Utilities.getTurningAngle(ai_world_state.getRobot(), target);

		boolean face_target = point_off != TARG_THRESH;
		
		comm = goTowardsPoint(target, true, face_target);
		
		comm.turning_speed *= 2;
		
		if (Math.abs(direction) < 45 && targ_dist < TARG_THRESH)
			point_off *= 0.7;
		if (point_off < DIST_TO_BALL)
			point_off = DIST_TO_BALL;
		
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		if (ai_world_state.getMyGoalLeft()) {
			if (ball.getX() < ai_world_state.getRobot().getCoords().getX())
				point_off = DEFAULT_POINT_OFF;
		}  else {
			if (ball.getX() > ai_world_state.getRobot().getCoords().getX())
				point_off = DEFAULT_POINT_OFF;
		}
		
//		if (chasing_target) {
//			comm = chasingTarget(targ_dist);
//		} else {
//			comm = chasingBall(targ_dist);
//		}

		normalizeRatio(comm);


		// debugging restrictions
		//comm.turning_speed *= 10;
		comm.speed *= SPEED_MULTIPLIER;

		return comm;

	}
	
	private Command chasingTarget(double targ_dist) {
		Command comm = goTowardsPoint(target, true, false);
		// if oscillating near zero
		if (targ_dist < POINT_ACCURACY)
			chasing_target = false;

		double dir_angle = Vector2D.getDirection(Vector2D.rotateVector(Vector2D.subtract(new Vector2D(ai_world_state.getBallCoords()), target), -ai_world_state.getRobot().getAngle()));
		if (Math.abs(dir_angle) > 20) {
			slowDownSpeed(targ_dist, 20, comm, 2); // limits speed to 30
		}
		normalizeRatio(comm);
		return comm;
	}
	
	private Command chasingBall(double targ_dist) {
		Command comm = null;
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());

		//			double ball_dist = ai_world_state.getDistanceToBall();
		//			if (ball_dist > 20 && ball_dist < 50) {
		//				double dir = Vector2D.getDirection(Vector2D.rotateVector(Vector2D.subtract(ball, target), -ai_world_state.getRobot().getAngle()));
		//				comm = new Command(Math.abs(dir) < 5 ? MAX_SPEED_CM_S : 0, dir*3, false);
		//				comm.acceleration = 200;
		//			} else
		//				comm = goTowardsPoint(ball, false, true);
		
		if (ai_world_state.getDistanceToBall() > Robot.LENGTH_CM*2) {
			chasing_target = true;
			return chasingTarget(targ_dist);
		}
		
		comm = goTowardsPoint(ball, false, true);
		if (Math.abs(comm.getByteTurnSpeed()) > 3)
			comm.speed = 0;
		else
			slowDownSpeed(ai_world_state.getDistanceToBall(), 10, comm, 2);


		if (comm.getByteSpeed() == 0 && comm.getByteTurnSpeed() == 0)
			comm = goTowardsPoint(ball, false, true);

		if (ai_world_state.getMyGoalLeft()) {
			if (ball.getX() < ai_world_state.getRobot().getCoords().getX())
				chasing_target = true;
		}  else {
			if (ball.getX() > ai_world_state.getRobot().getCoords().getX())
				chasing_target = true;
		}
		return comm;
	}

	@Override
	protected Command gotBall() throws IOException {
		point_off = DEFAULT_POINT_OFF;
		//System.out.println("GOT BALL");
		double angle = ai_world_state.getRobot().getAngle();
		if (ai_world_state.getMyGoalLeft()) {
			if (angle > 90 || angle < -90) {
				//System.out.println("REVERSE angle = " + angle);
				return new Command(-MAX_SPEED_CM_S, 0, false);
			}
		} else {
			if (Math.abs(angle) <= 90) {
				//System.out.println("FORWARD");
				return new Command(-MAX_SPEED_CM_S, 0, false);
			}
		}
		chasing_target = true;
		return new Command(MAX_SPEED_CM_S,0,true);
	}

	@Override
	protected Command defendGoal() throws IOException {
		// TODO Auto-generated method stub


		Point2D.Double intercept= Utilities.intersection(ai_world_state.getEnemyRobot().getFrontCenter(), ai_world_state.getEnemyRobot().getCoords(), ai_world_state.getMyGoal().getTop(), ai_world_state.getMyGoal().getBottom());


		if (intercept != null){
			Point2D.Double point;
			Point2D.Double point2 ;
			Point2D.Double point3;
			Command com = new Command(0, 0, false);
			if (ai_world_state.getMyGoalLeft()){
				point= new Point2D.Double(intercept.x+20 , intercept.y);
				point2= new Point2D.Double(ai_world_state.getMyGoal().getBottom().x+20 , ai_world_state.getMyGoal().getBottom().y);
				point3= new Point2D.Double(ai_world_state.getMyGoal().getTop().x+20 , ai_world_state.getMyGoal().getTop().y);
			} else {
				point= new Point2D.Double(intercept.x -20, intercept.y);
				point2= new Point2D.Double(ai_world_state.getMyGoal().getBottom().x-20 , ai_world_state.getMyGoal().getBottom().y);
				point3= new Point2D.Double(ai_world_state.getMyGoal().getTop().x-20 , ai_world_state.getMyGoal().getTop().y);
			}
			double dist = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(point)).getLength();
			double dist2 = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(ai_world_state.getMyGoal().getBottom())).getLength();
			double dist3 = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(ai_world_state.getMyGoal().getTop())).getLength();
		 if((intercept.y < ai_world_state.getMyGoal().getBottom().y+15)  && (intercept.y > ai_world_state.getMyGoal().getTop().y-15))	{
		   if (dist > 5)
				com = goTowardsPoint(new Vector2D(point), false, false);
			slowDownSpeed(dist, 20, com, 0);

			return com;
		} else if(ai_world_state.getEnemyRobot().getAngle()<0 && ai_world_state.getEnemyRobot().getAngle()>-180) {
			if (dist2 > 10)
				com = goTowardsPoint(new Vector2D(point2), false, false);
			    slowDownSpeed(dist2, 20, com, 0);
			return com;
			}

		 else if(ai_world_state.getEnemyRobot().getAngle()>0 && ai_world_state.getEnemyRobot().getAngle()<180 ){
			 if (dist3 > 10)
					com = goTowardsPoint(new Vector2D(point3), false, false);
				    slowDownSpeed(dist3, 20, com, 0);
				return com;
		 }
		}

		return null;
	}

	@Override
	protected Command penaltiesDefend() throws IOException {

		//TODO: Find direction of opposing robot and move into intercept path.
		//
		// Our robot will be placed like shown bellow:
		//
		// *----------------------------------------------------------*
		// |														  |
		// |														  |
		// |_														 _|
		// |  _____		 ______										  |
		// |  |	| |		|_____||									  |
		// |  |_|_| 	|_____||									  |
		// |  |___|		         									  |
		// |														  |
		// |_														 _|
		// |	     												  |
		// |                                                          |
		// *----------------------------------------------------------*
		//
		//
		// 
		//
		// Get the direction of the enemy robot and make that a vector.
		// Find the intersection between our direction vector and the enemies robot direction, this is where our robot will need to be.
		// Make our robot move to that intersection.
		//
		//

		Point2D.Double interceptBall= Utilities.intersection(ai_world_state.getEnemyRobot().getFrontCenter(), ai_world_state.getEnemyRobot().getCoords(), ai_world_state.getRobot().getCoords(), ai_world_state.getRobot().getFrontCenter());
		System.out.println("InterceptDistance: " + interceptBall);
		System.out.println("Our robot's y: " + ai_world_state.getRobot().getCoords().y);

		if (!interceptBall.equals(null)){

			if((interceptBall.y < ai_world_state.getMyGoal().getBottom().y)  && (interceptBall.y > ai_world_state.getMyGoal().getTop().y)){
				if ((interceptBall.y > ai_world_state.getRobot().getCoords().y)  ){
					byte forward_speed = (byte) -20; //Utilities.normaliseToByte((15+(interceptDistance.getLength()/40)*25));
					return new Command(forward_speed, 0, false);
					//mComm.sendMessage(opcode.operate, forward_speed, (byte) 0);
				} else if((interceptBall.y < ai_world_state.getRobot().getCoords().y)) {
					byte forward_speed = (byte) 20; //Utilities.normaliseToByte(-(15+(interceptDistance.getLength()/40)*25));
					return new Command(forward_speed, 0, false);
					//mComm.sendMessage(opcode.operate, forward_speed, (byte) 0);
				}
			}
			else
			{
				return new Command( 0, 0, false);
				//mComm.sendMessage(opcode.operate, (byte) 0, (byte) 0);
			}

		}
		
		return null;

	}

	/* (non-Javadoc)
	 * @see sdp.AI.AI#penaltiesAttack()
	 */
	private Vector2D targetPoint =  new Vector2D(-30,0);
	boolean chase=true;
	@Override
	protected Command penaltiesAttack() throws IOException {

		/**
		 * Penalty mode 1, don't place the robot very close to the ball, do chaseBall and it should work
		 * */
	//	return chaseBall();
		
		/*
		 * Penalty mode 2, go to optimal point and try to shoot
		 * you can choose which goals to prioritize
		 * */
		
/*		
		try {
			int priority = 0;
		
			switch (priority){
				case 0: //shoot with both
					targetPoint = new Vector2D(Utilities.getOptimalPointBehindBall(ai_world_state, 10));
					break;
				case 1: //shoot only with walls
					targetPoint = new Vector2D(Utilities.getOptimalPointBehindBall(ai_world_state, 10, true));
					break;
				case 2: //shoot only with main goal
					targetPoint = new Vector2D(Utilities.getOptimalPointBehindBall(ai_world_state, 10, false));
					break;
			}
		} catch (NullPointerException e) {
			System.out.println("can't find optimal point, i'm going to the ball");
		}*/
		
		Vector2D offset;
		if (ai_world_state.getMyGoalLeft())
			offset = new Vector2D(-30,-10);
		else
			offset = new Vector2D(30,10);
		targetPoint = Vector2D.add(new Vector2D(ai_world_state.getBallCoords()), offset);
		target = targetPoint;
		if (distanceTo(targetPoint) < 5)
			chase = false;
		
		if (!chase){
			System.out.println("chase ball");
			return chaseBall();
		} else {
			System.out.println("go to point");
			target = targetPoint;
			return chasingTarget(distanceTo(targetPoint));
		}
		
		/*
		//the point where the ball would end up if we shoot
		Point2D.Double pointInGoal= 
			Utilities.intersection(ai_world_state.getRobot().getCoords(), 
					ai_world_state.getRobot().getFrontCenter(), ai_world_state.getEnemyGoal().getTop(), 
					ai_world_state.getEnemyGoal().getBottom());
		boolean clear_path = Utilities.isPathClear(pointInGoal, ai_world_state.getBallCoords(), 
				ai_world_state.getEnemyRobot());
	
		if (clear_path)
			return new Command(0,0,true);


		Point2D enemyRobot;
		if(ai_world_state.isGoalVisible())        {
			enemyRobot= ai_world_state.getEnemyRobot().getCoords();
			//if enemy robot in the lower part of the goal then shoot in the upper part
			if( enemyRobot.getY() < ai_world_state.getEnemyGoal().getCentre().y){
				return new Command(0,0,true);
				//mComm.sendMessage(opcode.operate,(byte) 0, (byte) 
				//ai_world_state.getEnemyGoal().getTop().y);
				// mComm.sendMessage(opcode.kick);
				//if enemy robot in the upper part of the goal then shoot in the lower part
			}else if( enemyRobot.getY() <ai_world_state.getEnemyGoal().getCentre().y){
				return new Command(0,0,true);
				//mComm.sendMessage(opcode.operate, (byte) 0, (byte)
				//ai_world_state.getEnemyGoal().getBottom().y);
				// mComm.sendMessage(opcode.kick);
			}	
			//else just kick in upper part of the goal by ...this is the default
			else{
				return new Command(0,0,true);
				//mComm.sendMessage(opcode.operate, (byte) 0, (byte) 
				//ai_world_state.getEnemyGoal().getTop().y);
				//mComm.sendMessage(opcode.kick);
			}
		}  */
	//	return null;
	}

	/**
	 * Makes robot proceed towards a point avoiding obstacles on its way
	 * @param point
	 */
	private Command goTowardsPoint(Vector2D point, boolean include_ball_as_obstacle, boolean need_to_face_point) {

		Command command = new Command(0, 0, false);
		// get relative ball coordinates
		final Vector2D point_rel = Utilities.getLocalVector(ai_world_state.getRobot(), point);

		// get direction and distance to point
		final double point_dir = Vector2D.getDirection(point_rel);
		final double direct_dist = point_rel.getLength();
		final double vis_dist = Utilities.visibility2(ai_world_state, point, ai_world_state.getMyTeamBlue(), include_ball_as_obstacle) + Robot.LENGTH_CM;
		final double other_rob_dist = Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), new Vector2D(ai_world_state.getEnemyRobot().getCoords())).getLength();
		final boolean point_visible = vis_dist >= direct_dist;
		
		double turn_ang = 999;

		double point_dist = point_visible ? direct_dist : other_rob_dist+Robot.LENGTH_CM/2;
		double temp = 999;
		int t = 0;
		while (turn_ang == 999) {
			for (int i = 0; i < COLL_SECS_COUNT; i++) {
				double ang = Utilities.normaliseAngle(((-90+i*SEC_ANGLE)+(-90+(i+1)*SEC_ANGLE))/2);
				Vector2D vec = Vector2D.multiply(Vector2D.rotateVector(new Vector2D(1, 0), ang), point_dist);
				if (Utilities.reachability(ai_world_state, Utilities.getGlobalVector(ai_world_state.getRobot(), vec), ai_world_state.getMyTeamBlue(), include_ball_as_obstacle, 1.5)) {	
					double diff = Utilities.normaliseAngle(ang-point_dir);
					if (Math.abs(diff) < Math.abs(temp)) {
						temp = diff;
						turn_ang = ang;
					}
				}
			}
			t++;
			if (t == 5)
				break;
			point_dist -= Robot.LENGTH_CM;
		}

		if (turn_ang == 999)
			turn_ang = point_dir;
		
		command.turning_speed = turn_ang;

		if (need_to_face_point) { 
			// set forward speed
			if (command.turning_speed > 90 || command.turning_speed < -90){ 
				// ball is behind
				command.speed = -MAX_SPEED_CM_S;
			} else { 
				//ball is in front
				command.speed = MAX_SPEED_CM_S;
			}
		} else {
			// go fastest to a point regardless of direction

			if (command.turning_speed > 90 || command.turning_speed < -90) {
				// ball is behind
				command.speed = -MAX_SPEED_CM_S;

				// go backwards to get to ball as soon as possible
				command.turning_speed = Utilities.normaliseAngle(command.turning_speed-180);
			} else {
				// ball is in front
				command.speed = MAX_SPEED_CM_S;
			}
		}

		// if we get within too close (within coll_start) of an obstacle
		backAwayIfTooClose(command, include_ball_as_obstacle, point_visible ? THRESH_BACK_LOW : THRESH_BACK_HIGH);

		// check if either of the corners are in collision
		nearCollisionCheck(command, point_visible ? THRESH_CORN_LOW : THRESH_CORN_HIGH);

		return command;
	}

	/**
	 * If too close to an obstacle from sectors, back away
	 * @param command
	 */
	private void backAwayIfTooClose(Command command, boolean include_ball_as_obstacle, double threshold) {
		double for_dist = Utilities.getSector(ai_world_state, ai_world_state.getMyTeamBlue(), -10, 10, 20, include_ball_as_obstacle).getLength(); // get collision distance at the front
		double back_dist = Utilities.getSector(ai_world_state, ai_world_state.getMyTeamBlue(), 170, -170, 20, include_ball_as_obstacle).getLength(); // get collision distance at the back

		if (for_dist < threshold){
			if (command.speed >= 0) {
				// go backwards
				double speed_coeff = -1+for_dist/threshold;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				command.speed *= speed_coeff;
			}
		} else if (back_dist < threshold) {
			if (command.speed <= 0) {
				// same as above
				double speed_coeff = -1+back_dist/threshold;
				if (speed_coeff > 0)
					speed_coeff = 0;
				if (speed_coeff < -1)
					speed_coeff = -1;
				command.speed *= speed_coeff;
			}
		}
	}

	/**
	 * Checks whether there is a collision with either corner and tries to react to it
	 * @param command
	 */
	private void nearCollisionCheck(Command command, double threshold) {
		final Vector2D
		front_left = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackLeft()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		front_right = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getBackRight()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_left = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontLeft()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords()))),
		back_right = Utilities.getLocalVector(ai_world_state.getRobot(),Vector2D.add(new Vector2D(ai_world_state.getRobot().getFrontRight()),Utilities.getNearestCollisionPoint(ai_world_state, ai_world_state.getMyTeamBlue(), ai_world_state.getRobot().getCoords())));
		final boolean
		front_left_coll = front_left.getLength() <= threshold,
		front_right_coll = front_right.getLength() <= threshold,
		back_left_coll = back_left.getLength() <= threshold,
		back_right_coll = back_right.getLength() <= threshold,
		any_collision = front_left_coll || front_right_coll || back_left_coll || back_right_coll;

		if (any_collision) {
			//System.out.println("Collision "+(front_left_coll || front_right_coll ? "FRONT" : "BACK")+" "+(front_left_coll || back_left_coll ? "LEFT" : "RIGHT"));
			command.speed = front_left_coll || front_right_coll ? -MAX_SPEED_CM_S : MAX_SPEED_CM_S;
			command.turning_speed += front_left_coll || back_left_coll ? -10 : 10;
		}
		command.turning_speed = Utilities.normaliseAngle(command.turning_speed);
	}


	/**
	 * Distance from centre of robot to a given point on table
	 * @param global
	 * @return
	 */
	private double distanceTo(Vector2D global) {
		return Vector2D.subtract(new Vector2D(ai_world_state.getRobot().getCoords()), global).getLength();
	}


	/**
	 * Normalises speed to be within a upper and lower limit.
	 * @param distance Distance to the ball in which the speed is slowed down.
	 * @param threshold 
	 * @param current_speed The Command to be modified.
	 * @param slow_speed Minimum speed the robot should slow to.
	 */
	private void slowDownSpeed(double distance, double threshold, Command current_speed, double slow_speed) {
		if (distance >= threshold)
			return;
		if (current_speed.speed < 0)
			slow_speed = -slow_speed;
		if (Math.abs(current_speed.speed) < Math.abs(slow_speed)) {
			current_speed.speed = slow_speed;
			return;
		}
		double coeff = distance / threshold;
		current_speed.speed = slow_speed+coeff*(current_speed.speed-slow_speed);
	}

	/**
	 * Changes the speed to be a function of the turning speed.
	 * @param comm The command to be normalised.
	 */
	public void normalizeRatio(Command comm) {
		if (Math.abs(comm.turning_speed) > MAX_TURN_ANG) {
			comm.speed = 0;
			return;
		}
		double rat = Math.abs(comm.turning_speed)/MAX_TURN_ANG;
		comm.speed *= 1-rat;
	}
	
	@Override
	public Command sit() throws IOException {
		point_off = DEFAULT_POINT_OFF;
		return super.sit();
	}
	

}
