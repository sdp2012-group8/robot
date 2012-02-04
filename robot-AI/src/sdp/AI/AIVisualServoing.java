package sdp.AI;

import java.io.IOException;

import sdp.common.Communicator;
import sdp.common.Tools;
import sdp.common.WorldState;
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
	protected synchronized void worldChanged(WorldState worldState) {

		distance_to_ball = Tools.getDistanceBetweenPoint(robot.getCoords(), worldState.getBallCoords());
		distance_to_goal = Tools.getDistanceBetweenPoint(toCentimeters(robot.getCoords()), enemy_goal);

		
		switch (state) {
		case chase_ball:
			chaseBall();
			break;
			
		case got_ball:
			alignToGoal();
			break;
		}
	}

	public void chaseBall() {
		// System.out.println("Chasing ball");
		double angle_between = anglebetween(robot.getCoords(), worldState.getBallCoords());
		double turning_angle = angle_between - robot.getAngle();
		byte forward_speed = 40;
		
		System.out.println("I'm in chase ball :)");
		//System.out.println("Turning angle: " + turning_angle + " Angle between:" + angle_between + " Robot get angle: " + robot.getAngle());
		//System.out.println(robot.getCoords() + " " + worldState.getBallCoords());
		// Keep the turning angle between -180 and 180
		if (turning_angle > 180) turning_angle -= 360;
		if (turning_angle < -180) turning_angle += 360;
		
		if (distance_to_ball < robot.getSize()) forward_speed = 0;
			
		if (turning_angle > 127) turning_angle = 127; // Needs to reduce the angle as the command can only accept -128 to 127
		if (turning_angle < -128) turning_angle = -128;
		try {
			if (turning_angle > TURNING_ACCURACY){
				
				mComm.sendMessage(opcode.operate, forward_speed, (byte)127);
				//System.out.println("Chasing ball - Turning: " + turning_angle);
			} 
			else if( turning_angle < -TURNING_ACCURACY){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)-127);
				//System.out.println("Chasing ball - Turning: " + turning_angle);	
			}
			else if (distance_to_ball > robot.getSize()/2) {
				
				mComm.sendMessage(opcode.operate, (byte)(20 + 50*distance_to_ball), (byte)0);
				//System.out.println("Chasing ball - Moving Forward");
			} else {
				//System.out.println("Chasing ball - At Ball: " + distance + " " + robot.getCoords() + " " + worldState.getBallCoords());
				setMode(mode.got_ball);
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void alignToGoal(){
		
		System.out.println("I'm in ALIGN TO GOAL :O");
	
		double angle_between = anglebetween(toCentimeters(robot.getCoords()), enemy_goal);
		double turning_angle = angle_between - robot.getAngle();
		byte forward_speed = 20;
		
		//System.out.println("Turning angle: " + turning_angle + " Angle between:" + angle_between + " Robot get angle: " + robot.getAngle());
		//System.out.println(robot.getCoords() + " " + worldState.getBallCoords());
		// Keep the turning angle between -180 and 180
		if (turning_angle > 180) turning_angle -= 360;
		if (turning_angle < -180) turning_angle += 360;
		
		
		if (distance_to_goal < robot.getSize()) forward_speed = 0;
		
		System.out.println(distance_to_goal);
		
		
		if (turning_angle > 127) turning_angle = 127; // Needs to reduce the angle as the command can only accept -128 to 127
		if (turning_angle < -128) turning_angle = -128;
		try {
			if (distance_to_ball > robot.getSize()) {
				setMode(mode.chase_ball);
			} else if (turning_angle > TURNING_ACCURACY && (distance_to_goal > 1)){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)127);
				System.out.println("Going to goal - Turning: " + turning_angle);
			} 
			else if( turning_angle < -TURNING_ACCURACY && (distance_to_goal > 1)){
				mComm.sendMessage(opcode.operate, forward_speed, (byte)-128);
				System.out.println("Going to goal - Turning: " + turning_angle);	
			}
			else if (distance_to_goal > 1) {
				mComm.sendMessage(opcode.operate, (byte)60, (byte)0);
				System.out.println("GOING FORWARD TO GOAL");
				
			} else {
				System.out.println("The old man the boat");
				setMode(mode.chase_ball);
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
