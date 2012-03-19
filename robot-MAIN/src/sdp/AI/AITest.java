package sdp.AI;

import java.io.IOException;

public class AITest extends AI{
	/**
	 * This is here only to test what would happen if we had more than one AI
	 */
	@Override
	protected Command chaseBall() throws IOException {
		// TODO Auto-generated method stub
	//	System.out.println("i'm chasing the ball in the test AI");
		return null;
	}

	@Override
	protected Command gotBall() throws IOException {
		// TODO Auto-generated method stub
	//	System.out.println("i'm in gotBall in the test AI");
		return null;
	}

	@Override
	protected Command defendGoal() throws IOException {
		// TODO Auto-generated method stub
	//	System.out.println("i'm defending the goal in the test AI");
		return null;
	}

	@Override
	protected Command penaltiesDefend() throws IOException {
		// TODO Auto-generated method stub
	//	System.out.println("i'm defending penalties in the test AI");
		return null;
	}

	@Override
	protected Command penaltiesAttack() throws IOException {
		// TODO Auto-generated method stub
	//	System.out.println("i'm attacking penalties in the test AI");
		return null;
	}

	@Override
	protected void changedState() {
		// TODO Auto-generated method stub
		
	}

}
