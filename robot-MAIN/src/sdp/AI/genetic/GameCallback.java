package sdp.AI.genetic;

public interface GameCallback {
	
	/**
	 * When game finishes, this gets called
	 * @param the game that the result is coming from
	 * @param fitness of network 0 and 1
	 */
	public void onFinished(final Game caller, final long[] fitness);

}
