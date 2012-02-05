package sdp.common;

/**
 * A simple FPS counter.
 * 
 * Produces an FPS estimation, based on the sample of ticks.
 * 
 * It begins recording frame from the first tick, not since the object's
 * creation. The FPS value returned should be representative of the last
 * SAMPLE_SIZE (50 when this was written) frames, after the said number
 * of frames had passed.
 * 
 * @author Gediminas Liktaras
 */
public class FPSCounter {
	
	/** How many frames to sample for FPS computations. */
	private static final int SAMPLE_SIZE = 50;
	
	/** The buffer that contains the last SAMPLE_SIZE tick lengths. */
	private int tickLength[];	
	/** Index to the next tick sample. */
	private int tickIndex;	
	/** Current sum of the last SAMPLE_SIZE tick lengths. */
	private int tickSum;	
	/** The number of ticks so far. */
	private int tickCount;	
	/** Timestamp of the last tick. */
	private long lastTick;
	
	
	/**
	 * Create a new FPS counter.
	 */
	public FPSCounter() {
		tickLength = new int[SAMPLE_SIZE];
		tickIndex = 0;
		tickSum = 0;
		tickCount = 0;
		lastTick = -1;
	}
	
	
	/**
	 * Get the number of FPS within the last SAMPLE_SIZE (50) frames.
	 * 
	 * @return Frames per second.
	 */
	public double getFPS() {
		return ((SAMPLE_SIZE * 1000) / (double)(tickSum));
	}
	
	/**
	 * Get the number of ticks registered.
	 * 
	 * @return The number of ticks so far.
	 */
	public int getTickCount() {
		return tickCount;
	}
	
	
	/**
	 * Register a tick.
	 */
	public void tick() {
		if (lastTick < 0) {
			lastTick = System.currentTimeMillis();
		} else {
			long curTick = System.currentTimeMillis();
			
			tickSum -= tickLength[tickIndex];
			tickLength[tickIndex] = (int)(curTick - lastTick);
			tickSum += tickLength[tickIndex];
			
			tickIndex = (tickIndex + 1) % SAMPLE_SIZE;
			lastTick = curTick;
			++tickCount;
		}
	}

}
