package sdp.AI;

import java.io.IOException;


/**
 * A test AI that performs no actions.
 */
public class NullAI extends BaseAI {
	
	/**
	 * @see sdp.AI.BaseAI#chaseBall()
	 */
	@Override
	protected Command chaseBall() throws IOException {
	//	System.out.println("i'm chasing the ball in the test AI");
		return null;
	}

	/**
	 * @see sdp.AI.BaseAI#gotBall()
	 */
	@Override
	protected Command gotBall() throws IOException {
	//	System.out.println("i'm in gotBall in the test AI");
		return null;
	}

	/**
	 * @see sdp.AI.BaseAI#defendGoal()
	 */
	@Override
	protected Command defendGoal() throws IOException {
	//	System.out.println("i'm defending the goal in the test AI");
		return null;
	}

	/**
	 * @see sdp.AI.BaseAI#penaltiesDefend()
	 */
	@Override
	protected Command penaltiesDefend() throws IOException {
	//	System.out.println("i'm defending penalties in the test AI");
		return null;
	}

	/**
	 * @see sdp.AI.BaseAI#penaltiesAttack()
	 */
	@Override
	protected Command penaltiesAttack() throws IOException {
	//	System.out.println("i'm attacking penalties in the test AI");
		return null;
	}

	/**
	 * @see sdp.AI.BaseAI#changedState()
	 */
	@Override
	protected void changedState() { }

}
