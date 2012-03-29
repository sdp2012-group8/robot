package sdp.AI.genetic;

public interface GameCallback {
	
	/**
	 * When game finishes, this gets called
	 * @param fitness of network 0 and 1
	 * @param ids
	 */
	public void onFinished(final long[] fitness, final int[] ids);

}
