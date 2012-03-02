package sdp.vision.testbench;

import java.io.*;
import java.util.ArrayList;

import sdp.common.VisionTestCase;
import sdp.common.WorldState;
import sdp.vision.Vision;
import sdp.vision.processing.ImageProcessorConfig;
import sdp.vision.processing.ProcUtils;
import sdp.common.Utilities;


/**
 * The main test bench logic class.
 * 
 * @author Aaron Cronin
 * @author Gediminas Liktaras
 */
public class TestBench {
	
	/** Active vision instance. */
	private Vision vision;
	
	
	/**
	 * Create a new test bench instance.
	 */
	public TestBench() {
		vision = new Vision();
	}
	
	
	/**
	 * Run a test using the given test specification and using the default
	 * image processor configuration.
	 * 
	 * @param testSpec A path to an XML file that contains the test specification.
	 */
	public void runTest(String testSpec) {
		runTest(testSpec, new ImageProcessorConfig(), System.out);
	}
	
	/**
	 * Run a test using the given test specification and image processor
	 * configuration.
	 * 
	 * @param testSpec A path to an XML file that contains the test specification.
	 * @param config Image processor configuration to use.
	 * @param out Output stream to dump data to.
	 */
	public void runTest(String testSpec, ImageProcessorConfig config, PrintStream out) {
		vision.setConfiguration(config);
		ArrayList<VisionTestCase> tests = VisionTestCase.readTestCases(testSpec);
		VisionSystemErrorAccumulator errorAcc = new VisionSystemErrorAccumulator();
		
		for (VisionTestCase test : tests) {
			long testStartTime = System.currentTimeMillis();
			WorldState actualState_norm = vision.extractWorldState(Utilities.deepBufferedImageCopy(test.getImage()));
			long testEndTime = System.currentTimeMillis();
			long testExecTime = testEndTime - testStartTime;
			
			WorldState actualState_frame = ProcUtils.stateToFrameCoordinates(config, actualState_norm);
			
			errorAcc.addRecord(test.getImageFilename(), test.getExpectedState(), actualState_frame, testExecTime);
		}
		errorAcc.dumpMetrics(out);		
	}

	
	/**
	 * The entry point.
	 * 
	 * @param args Command-line arguments.
	 */
	public static void main(String[] args) {
		TestBench testBench = new TestBench();
		testBench.runTest("data/tests/friendly1.xml");
	}
}
