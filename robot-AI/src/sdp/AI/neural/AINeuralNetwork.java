package sdp.AI.neural;

import java.io.IOException;

import org.neuroph.core.NeuralNetwork;

import sdp.AI.AI;
import sdp.AI.AIWorldState;
import sdp.common.Communicator;
import sdp.common.NNetTools;
import sdp.common.Robot;
import sdp.common.Tools;
import sdp.common.Vector2D;
import sdp.common.NNetTools.move_modes;
import sdp.common.Communicator.opcode;

public class AINeuralNetwork extends AI {
	
	private final static int network_count = 1;
	private AIWorldState oldState = null;
	private NeuralNetwork[] nets = new NeuralNetwork[network_count];
	private long oldTime = 0;

	public AINeuralNetwork(Communicator comm, String fname) {
		super(comm);
		for (int i = 0; i < nets.length; i++)
			nets[i] = NeuralNetwork.load(fname+"/nn"+i+".nnet");
	}

	@Override
	protected void update(AIWorldState ai_world_state) {
		super.update(ai_world_state);
		if (oldState == null) {
			oldState = ai_world_state;
			oldTime = System.currentTimeMillis()-50; // avoid div by zero
		}
		oldState = ai_world_state;
		oldTime = System.currentTimeMillis();
	}
	
	protected synchronized void chaseBall() {
		// stop if needed
		
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		Vector2D ball_rel = Tools.getLocalVector(ai_world_state.getRobot(), ball);
		ball_rel.setX(ball_rel.getX()-Robot.LENGTH_CM/2);
		if (ball_rel.getLength() < 11) {
			try {
				mComm.sendMessage(opcode.operate, (byte) 0, (byte) 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		// method
		double dt =(System.currentTimeMillis()-oldTime)/1000d;
		nets[0].setInput(NNetTools.generateAIinput(ai_world_state, oldState, dt, ai_world_state.getMyTeamBlue(), ai_world_state.getMyGoalLeft(), 0));
		move_modes[] results = new move_modes[nets.length];
		// get results
		for (int i = 0; i < results.length; i++) {
			nets[i].calculate();
			double[] out = nets[i].getOutput();
			results[i] = NNetTools.recoverOutputMode(out);
		}
		
		try {
			int speed = NNetTools.getDesiredSpeed(results[0]);
			int turn_speed = NNetTools.getDesiredTurningSpeed(results[0]);
			System.out.println("mode "+results[0]);
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void gotBall() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void sit() throws IOException {
		Vector2D ball = new Vector2D(ai_world_state.getBallCoords());
		Vector2D ball_rel = Tools.getLocalVector(ai_world_state.getRobot(), ball);
		ball_rel.setX(ball_rel.getX()-Robot.LENGTH_CM/2);
		if (ball_rel.getLength() < 11) {
			try {
				mComm.sendMessage(opcode.operate, (byte) 0, (byte) 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
	}

}
