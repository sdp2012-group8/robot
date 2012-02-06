package sdp.AI;

import java.io.IOException;

import sdp.common.Communicator;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Vector2D;
import sdp.common.WorldStateProvider;
import sdp.common.Communicator.opcode;

public class AIVisualServoing extends AI {
	
	// Ball and goal position
	private double distance_to_ball = 0;
	private double distance_to_goal = 0;

	public AIVisualServoing(Communicator Comm, WorldStateProvider Obs) {
		super(Comm, Obs);
	}
	
	/**
	 * This method is fired when a new state is available. Decisions should be done here.
	 * @param new_state the new world state (low-pass filtered)
	 */
	protected synchronized void worldChanged() {
		// worldState is now in centimeters!!!

		distance_to_ball = Tools.getDistanceBetweenPoint(robot.getCoords(), worldState.getBallCoords());
		distance_to_goal = Tools.getDistanceBetweenPoint(robot.getCoords(), enemy_goal);

		
		switch (state) {
		case chase_ball:
			chaseBall();
			break;
			
		case got_ball:
			alignToGoal();
			break;
		}
	}

	/**
	 * Mode to go towards the ball.
	 * When it reaches the ball, transition into the goToGoal state.
	 * WARNING - Doesn't use and prediction to go towards the ball.
	 */
	public void chaseBall() {
		// ged direction from robot to ball
		Vector2D dir = Vector2D.subtract(new Vector2D(worldState.getBallCoords()), new Vector2D(robot.getCoords()));
		// Keep the turning angle between -180 and 180
		double turning_angle = Tools.normalizeAngle(
				-robot.getAngle()
				+Vector2D.getDirection(dir));
		// calculates speed formula:
		// speed_when_robot_next_to_ball+(distance_to_ball/max_distance)*speed_when_over_max_distance
		// every distance between 0 and max_distance will be mapped between speed_when_robot_next_to_ball and speed_when_over_max_distance
		double forward_speed = 5+(distance_to_ball/40)*40;

		System.out.println("Currently going to the ball");
		// do the backwards turn
		if (turning_angle > 90 || turning_angle < -90)
			forward_speed = -10;

		// make turning faster
		turning_angle *= 2;
		// normalize angles
		if (turning_angle > 127) turning_angle = 127;
		if (turning_angle < -127) turning_angle = -127;
		// don't exceed speed limit
		if (forward_speed > MAX_SPEED_CM_S) forward_speed = MAX_SPEED_CM_S;
		if (forward_speed < -MAX_SPEED_CM_S) forward_speed = -MAX_SPEED_CM_S;
		// send command
		try {
			mComm.sendMessage(opcode.operate, (byte) (forward_speed), (byte) (turning_angle));
			// check whether to go into got_ball mode
			if (Math.abs(turning_angle) < TURNING_ACCURACY && distance_to_ball < 4+Robot.LENGTH_CM/2) {
				System.out.println("Got the ball, going to alignToGoal.");
				setMode(mode.got_ball);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Mode set if got ball.
	 * Aims and shoots the ball into the opposing goal. 
	 */
	public void alignToGoal(){
		
		System.out.println("I'm in ALIGN TO GOAL :O");
	
		double angle_between = anglebetween(robot.getCoords(), enemy_goal);
		double turning_angle = angle_between - robot.getAngle();
		byte forward_speed = 5;
		
		//System.out.println("Turning angle: " + turning_angle + " Angle between:" + angle_between + " Robot get angle: " + robot.getAngle());
		//System.out.println(robot.getCoords() + " " + worldState.getBallCoords());
		// Keep the turning angle between -180 and 180
		if (turning_angle > 180) turning_angle -= 360;
		if (turning_angle < -180) turning_angle += 360;
		
		
		if (distance_to_goal < Robot.LENGTH_CM) forward_speed = 0;
		
		System.out.println(distance_to_goal);
		
		
		if (turning_angle > 127) turning_angle = 127; // Needs to reduce the angle as the command can only accept -128 to 127
		if (turning_angle < -128) turning_angle = -128;
		// don't exceed speed limit
		if (forward_speed > MAX_SPEED_CM_S) forward_speed = MAX_SPEED_CM_S;
		if (forward_speed < -MAX_SPEED_CM_S) forward_speed = -MAX_SPEED_CM_S;
		try {
			if (distance_to_ball > Robot.LENGTH_CM) {
				setMode(mode.chase_ball);
			} else if (turning_angle > TURNING_ACCURACY && (distance_to_goal > 1)){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)50);
				System.out.println("Going to goal - Turning: " + turning_angle);
			} 
			else if( turning_angle < -TURNING_ACCURACY && (distance_to_goal > 1)){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)-50);
				System.out.println("Going to goal - Turning: " + turning_angle);	
			}
			
			else if (distance_to_goal > 1) {
				if(!(distance_to_goal <100)){
					mComm.sendMessage(opcode.operate, (byte)60, (byte)0);
				} else{
					mComm.sendMessage(opcode.kick);	
				}
				System.out.println("GOING FORWARD TO GOAL");
				
			} else {
				System.out.println("Lost contact with ball, going back to chaseBall");
				setMode(mode.chase_ball);
			}
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
