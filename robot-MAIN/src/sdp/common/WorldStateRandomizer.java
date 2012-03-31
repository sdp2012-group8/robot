package sdp.common;

import java.awt.geom.Point2D;
import java.util.Random;

import sdp.common.world.Robot;
import sdp.common.world.WorldState;

public class WorldStateRandomizer {
	
	private final static int RANDOM_ARRAY_SIZE = 1000;
	
	private static double[] randomNumbers = null;
	
	private static Random r = new Random();
	
	private static int index;
	
	public static WorldState randomize(final WorldState state, final double posAmount, final double angAmount) {
		return new WorldState(randomize(state.getBallCoords(), posAmount),
				randomize(state.getBlueRobot(), posAmount, angAmount),
				randomize(state.getYellowRobot(), posAmount, angAmount), 
				state.getWorldImage());
		
	}
	
	private static Point2D.Double randomize(Point2D.Double input, double posAmount) {
		return new Point2D.Double(input.x + posAmount*getRandom(), input.y + posAmount*getRandom());
	}
	
	private static Robot randomize(Robot rob, double posAmount, double angAmount) {
		return new Robot(randomize(rob.getCoords(), posAmount), rob.getAngle()+angAmount*getRandom());
	}
	
	private static void regenerateNumbers() {
		index = 0;
		randomNumbers = new double[RANDOM_ARRAY_SIZE];
		for (int i = 0; i < RANDOM_ARRAY_SIZE; i++) {
			randomNumbers[i] = r.nextGaussian();
			if (randomNumbers[i] > 1)
				randomNumbers[i] = 1;
			if (randomNumbers[i] < -1)
				randomNumbers[i] = -1;
		}
	}
	
	public static double getRandom() {
		
		if (randomNumbers == null)
			regenerateNumbers();
		
		if (index >= randomNumbers.length)
			index = 0;
		return randomNumbers[index++];
	}

}
