package sdp.vision.testbench;

import java.io.PrintStream;
import java.util.ArrayList;

import sdp.common.WorldState;


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
	/** Ball position measurement error accumulator. */
	private PositionErrorAccumulator ballPosError = new PositionErrorAccumulator();
	/** Blue robot position measurement error accumulator. */
	private PositionErrorAccumulator bluePosError = new PositionErrorAccumulator();
	/** Yellow robot position measurement error accumulator. */
	private PositionErrorAccumulator yellowPosError = new PositionErrorAccumulator();
	
	
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
	 */
	public void addRecord(String testName, WorldState expected, WorldState actual) {
		testNames.add(testName);
		
		ballPosError.addRecord(expected.getBallCoords(), actual.getBallCoords());
		bluePosError.addRecord(expected.getBlueRobot().getCoords(),
				actual.getBlueRobot().getCoords());
		yellowPosError.addRecord(expected.getYellowRobot().getCoords(),
				actual.getYellowRobot().getCoords());
	}
	
	
	/**
	 * Print all accumulated metrics into the specified output stream.
	 * 
	 * @param out Output stream to dump to.
	 */
	public void dumpMetrics(PrintStream out) {
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
		out.format("  Yellow robot position: %d/%d/%d\n",
				yellowPosError.getInvalidRecordCount(),
				yellowPosError.getInaccurateRecordCount(),
				yellowPosError.getAccurateRecordCount());
		out.format("\n");
		out.format("Average ball position error: %.4f pixels.\n",
				ballPosError.averageError());
		out.format("Average blue robot position error: %.4f pixels.\n",
				bluePosError.averageError());
		out.format("Average yellow robot position error: %.4f pixels.\n",
				yellowPosError.averageError());
		
		out.format("\n");
		out.format("\n");
		out.format("=== INDIVIDUAL TEST DATA\n");
		for (int i = 0; i < testNames.size(); ++i) {
			out.format("\n");
			out.format("Test image: %s\n", testNames.get(i));
			out.format("  Ball position error: %.4f pixels - %s\n",
					ballPosError.getRecord(i),
					posErrorMessage(ballPosError.getRecord(i)));
			out.format("  Blue position error: %.4f pixels - %s\n",
					bluePosError.getRecord(i),
					posErrorMessage(bluePosError.getRecord(i)));
			out.format("  Yellow position error: %.4f pixels - %s\n",
					yellowPosError.getRecord(i),
					posErrorMessage(yellowPosError.getRecord(i)));
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

}