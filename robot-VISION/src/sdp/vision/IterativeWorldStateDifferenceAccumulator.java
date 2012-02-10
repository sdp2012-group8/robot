package sdp.vision;

import sdp.common.WorldState;

public class IterativeWorldStateDifferenceAccumulator {
	float ballerror,blueerror,yellowerror;
	
	//The constructor with no errors initially
	public IterativeWorldStateDifferenceAccumulator(){
		ballerror = 0;
		blueerror = 0;
		yellowerror = 0;
	}
	
	//Each iteration, two WorldStates are passed to the class and their respective errors are incremented to the class variables.
	public void iteration(WorldState manual,WorldState vision){
		
		//If an object is not detected it is given negative coordinates and therefore should have the maximum error.
		
		if (vision.getBallCoords().x > 0 && vision.getBallCoords().y > 0){
			ballerror += (float) Math.sqrt( Math.pow(manual.getBallCoords().x - vision.getBallCoords().x,2) + Math.pow(manual.getBallCoords().y  - vision.getBallCoords().y,2));
		} else {
			ballerror += 640;
		}

		if (vision.getBlueRobot().getCoords().x > 0 && vision.getBlueRobot().getCoords().y > 0){
			blueerror += (float) Math.sqrt( Math.pow(manual.getBlueRobot().getCoords().x - vision.getBlueRobot().getCoords().x,2) + Math.pow(manual.getBlueRobot().getCoords().y  - vision.getBlueRobot().getCoords().y,2));
		} else {
			blueerror += 640;
		}

		if (vision.getYellowRobot().getCoords().x > 0 && vision.getYellowRobot().getCoords().y > 0){
			yellowerror += (float) Math.sqrt( Math.pow(manual.getYellowRobot().getCoords().x - vision.getYellowRobot().getCoords().x,2) + Math.pow(manual.getYellowRobot().getCoords().y  - vision.getYellowRobot().getCoords().y,2));
		} else {
			yellowerror += 640;
		}
	}
	
	public float averageBallError(int iterations){
		return ballerror/iterations;
	}

	public float averageBlueError(int iterations){
		return blueerror/iterations;
	}
	
	public float averageYellowError(int iterations){
		return yellowerror/iterations;
	}
}
