package sdp.AI.neural;
//package sdp.AI.neural;
//
//import java.io.IOException;
//
//import org.neuroph.core.NeuralNetwork;
//
//import sdp.AI.AI;
//import sdp.AI.AIWorldState;
//import sdp.common.Communicator;
//import sdp.common.NNetTools;
//import sdp.common.Robot;
//import sdp.common.Tools;
//import sdp.common.Vector2D;
//import sdp.common.NNetTools.move_modes;
//import sdp.common.Communicator.opcode;
//
//public class AINeuralNetwork extends AI {
//	
//	
//	public static final int got_ball_dist = 20;
//	
//	private static final int network_count = 2;
//	private AIWorldState oldState = null;
//	private NeuralNetwork[] nets = new NeuralNetwork[network_count];
//	private long oldTime = 0;
//
//	public AINeuralNetwork(Communicator comm, String fname) {
//		super(comm);
//		for (int i = 0; i < nets.length; i++)
//			nets[i] = NeuralNetwork.load(fname+"/nn"+i+".nnet");
//	}
//
//	@Override
//	protected void update(AIWorldState ai_world_state) {
//		super.update(ai_world_state);
//		if (oldState == null) {
//			oldState = ai_world_state;
//			oldTime = System.currentTimeMillis()-50; // avoid div by zero
//		}
//		oldState = ai_world_state;
//		oldTime = System.currentTimeMillis();
//	}
//	
//	protected synchronized void chaseBall() {
//		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
//		Vector2D ball_rel = Tools.getLocalVector(ai_world_state.getRobot(), ball);
//		ball_rel.setX(ball_rel.getX()-Robot.LENGTH_CM/2);
//		if (ball_rel.getLength() < got_ball_dist)
//			ai_world_state.setState(mode.got_ball);
//		operate();
//		
//		
//	}
//
//	@Override
//	protected void gotBall() throws IOException {
//		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
//		Vector2D ball_rel = Tools.getLocalVector(ai_world_state.getRobot(), ball);
//		ball_rel.setX(ball_rel.getX()-Robot.LENGTH_CM/2);
//		if (ball_rel.getLength() >= got_ball_dist)
//			ai_world_state.setState(mode.chase_ball);
//		operate();
//		
//	}
//	
//	private void operate() {
//		// method
//		double dt =(System.currentTimeMillis()-oldTime)/1000d;
//		
//		double[] result;
//		
//		try {
//			int speed = 0, turn_speed = 0;
//			switch (ai_world_state.getMode()) {
//			case chase_ball:
//				nets[0].setInput(NNetTools.generateAIinput(ai_world_state, oldState, dt, ai_world_state.getMyTeamBlue(), ai_world_state.getMyGoalLeft(), 0));
//				nets[0].calculate();
//				result = nets[0].getOutput();
//				speed = NNetTools.getDesiredSpeed(NNetTools.recoverMoveOutputMode(result));
//				turn_speed= NNetTools.getDesiredTurningSpeed(NNetTools.recoverMoveOutputMode(result));
//				break;
//			case got_ball:
//				nets[1].setInput(NNetTools.generateAIinput(ai_world_state, oldState, dt, ai_world_state.getMyTeamBlue(), ai_world_state.getMyGoalLeft(), 1));
//				nets[1].calculate();
//				result = nets[1].getOutput();
//				speed = NNetTools.getDesiredSpeed(NNetTools.recoverGotBallOutputMode(result));
//				turn_speed= NNetTools.getDesiredTurningSpeed(NNetTools.recoverGotBallOutputMode(result));
//				break;
//			}
//			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	@Override
//	public void sit() throws IOException {
//	}
//
//	@Override
//	protected void penaltiesDefend() throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//
//	@Override
//	protected void penaltiesAttack() throws IOException {
//		// TODO Auto-generated method stub
//		
//	}
//
//}
