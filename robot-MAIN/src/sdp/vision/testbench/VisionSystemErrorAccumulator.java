package sdp.vision.testbench;

import java.io.PrintStream;
import java.util.ArrayList;

import sdp.common.world.WorldState;


/**
 * Accumulates vision system recognition errors and provides various
 * data about them.
 * 
 * This class can report the average recognition distance for each of the
 * object locations and the number of invalid recognitions. A recoginition
 * record is said to be invalid if it finds an object when it is not there
 * and vice-versa.
 * 
 * @author Aaron Cronin
 * @author Gediminas Liktaras
 */
public class VisionSystemErrorAccumulator {

	/** A list with accumulated test names. */
	private ArrayList<String> testNames = new ArrayList<String>();
	/** Execution time accumulator. */
	private ArrayList<Long> testTimes = new ArrayList<Long>();

	/** Ball position measurement error accumulator. */
	private PositionErrorAccumulator ballPosError = new PositionErrorAccumulator();
	/** Blue robot position measurement error accumulator. */
	private PositionErrorAccumulator bluePosError = new PositionErrorAccumulator();
	/** Yellow robot position measurement error accumulator. */
	private PositionErrorAccumulator yellowPosError = new PositionErrorAccumulator();
	
	/** Blue robot direction measurement error accumulator. */
	private DirectionErrorAccumulator blueDirError = new DirectionErrorAccumulator();
	/** Yellow robot direction measurement error accumulator. */
	private DirectionErrorAccumulator yellowDirError = new DirectionErrorAccumulator();
	
	
	/**
	 * Create a new recognition error accumulator.
	 * 
	 * @param config Image processor configuration to use.
	 */
	public VisionSystemErrorAccumulator() { }
	
	
	/**
	 * Add a recognition error entry.
	 * 
	 * @param expected Expected world state *in frame coordinates*.
	 * @param actual Actual world state *in frame coordinates*.
	 * @param time Time the system took to process the frame, in ms.
	 */
	public void addRecord(String testName, WorldState expected, WorldState actual,
			long time) {
		testNames.add(testName);
		testTimes.add(time);
		
		ballPosError.addRecord(expected.getBallCoords(), actual.getBallCoords());
		bluePosError.addRecord(expected.getBlueRobot().getCoords(),
				actual.getBlueRobot().getCoords());
		yellowPosError.addRecord(expected.getYellowRobot().getCoords(),
				actual.getYellowRobot().getCoords());
		
		blueDirError.addRecord(expected.getBlueRobot().getAngle(),
				actual.getBlueRobot().getAngle());
		yellowDirError.addRecord(expected.getYellowRobot().getAngle(),
				actual.getYellowRobot().getAngle());
	}
	
	
	/**
	 * Print all accumulated metrics into the specified output stream.
	 * 
	 * @param out Output stream to dump to.
	 */
	public void dumpMetrics(PrintStream out) {
		double avgExecTime = getAverageExecutionTime();
		double avgFps = 1000.0 / avgExecTime;
		
		out.format("=== TEST SUMMARY\n");
		out.format("\n");
		out.format("Number of tests: %d\n", testNames.size());
		out.format("Position accuracy error tolerance: %.4f pixels.\n",
				PositionErrorAccumulator.POS_ERROR_TOLERANCE);
		out.format("\n");
		out.format("Measurement stats (invalid/inaccurate/ok):\n");
		out.format("  Ball position: %d/%d/%d\n",
				ballPosError.getInvalidRecordCount(),
				ballPosError.getInaccurateRecordCount(),
				ballPosError.getAccurateRecordCount());
		out.format("  Blue robot position: %d/%d/%d\n",
				bluePosError.getInvalidRecordCount(),
				bluePosError.getInaccurateRecordCount(),
				bluePosError.getAccurateRecordCount());
		out.format("  Blue robot direction: %d/%d/%d\n",
				blueDirError.getInvalidRecordCount(),
				blueDirError.getInaccurateRecordCount(),
				blueDirError.getAccurateRecordCount());
		out.format("  Yellow robot position: %d/%d/%d\n",
				yellowPosError.getInvalidRecordCount(),
				yellowPosError.getInaccurateRecordCount(),
				yellowPosError.getAccurateRecordCount());
		out.format("  Yellow robot direction: %d/%d/%d\n",
				yellowDirError.getInvalidRecordCount(),
				yellowDirError.getInaccurateRecordCount(),
				yellowDirError.getAccurateRecordCount());
		
		out.format("\n");
		out.format("Average execution time: %.2f ms, %.2f fps.\n", avgExecTime, avgFps);
		out.format("Average ball position error: %.4f pixels.\n",
				ballPosError.averageError());
		out.format("Average blue robot position error: %.4f pixels.\n",
				bluePosError.averageError());
		out.format("Average blue robot direction error: %.4f degrees.\n",
				blueDirError.averageError());
		out.format("Average yellow robot position error: %.4f pixels.\n",
				yellowPosError.averageError());
		out.format("Average yellow robot direction error: %.4f degrees.\n",
				yellowDirError.averageError());
		
		out.format("\n");
		out.format("\n");
		out.format("=== INDIVIDUAL TEST DATA\n");
		for (int i = 0; i < testNames.size(); ++i) {
			out.format("\n");
			out.format("Test image: %s\n", testNames.get(i));
			out.format("  Execution time: %d ms\n", testTimes.get(i));
			out.format("  Ball position error: %.4f pixels - %s\n",
					ballPosError.getRecord(i),
					posErrorMessage(ballPosError.getRecord(i)));
			out.format("  Blue position error: %.4f pixels - %s\n",
					bluePosError.getRecord(i),
					posErrorMessage(bluePosError.getRecord(i)));
			out.format("  Blue direction error: %.4f degrees - %s\n",
					blueDirError.getRecord(i),
					posErrorMessage(blueDirError.getRecord(i)));
			out.format("  Yellow position error: %.4f pixels - %s\n",
					yellowPosError.getRecord(i),
					posErrorMessage(yellowPosError.getRecord(i)));
			out.format("  Yellow direction error: %.4f degrees - %s\n",
					yellowDirError.getRecord(i),
					posErrorMessage(yellowDirError.getRecord(i)));
		}
	}
	
	/**
	 * Get an appropriate message for a position measurement error.
	 * 
	 * @param error Position measure error.
	 * @return A message for dumping.
	 */
	private String posErrorMessage(double error) {
		if (error < 0.0) {
			return "INVALID";
		} else if (error > PositionErrorAccumulator.POS_ERROR_TOLERANCE) {
			return "INACCURATE";
		} else {
			return "OK";
		}
	}
	
	/**
	 * Get average test execution time.
	 * 
	 * @return Average time in milliseconds.
	 */
	private double getAverageExecutionTime() {
		double sum = 0.0;
		for (long t : testTimes) {
			sum += t;
		}
		
		if (testTimes.size() == 0) {
			return -1.0;
		} else {
			return sum / testTimes.size();
		}
	}

}
