package sdp.AI.genetic.distributed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import sdp.AI.genetic.Game;
import sdp.AI.genetic.GameRunner;
import sdp.AI.genetic.GeneticAlgorithm;
import sdp.AI.genetic.distributed.Server.TCPCommands;

/**
 * 
 * This is distributed version of {@link GameRunner}, that could be
 * directly used inside {@link GeneticAlgorithm} instead of a normal {@link GameRunner}
 * with the exception that this one runs on a different computer.
 * 
 * @author Martin Marinov
 *
 */
public class GameRunnerServer extends GameRunner {
	
	public final String clientName;
	
	public volatile boolean assigned = false;
	
	public void establishConnectionWithClient(InetAddress ip, int port, String clientName, String uname, String password) {
		System.out.println("Server listening on "+ip.getHostAddress()+" ("+ip.getHostName()+") : "+port+" for "+clientName+" ssh login: "+uname);
	}

	public GameRunnerServer(String clientName, String uname, String password) {
		this.clientName = clientName;
		try {
			
			Server.registerServer(this);
			
			establishConnectionWithClient(Server.getIP(), Server.getPort(), clientName, uname, password);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public void start() {
	// do nothing	
	}
	
	/**
	 * Executes a command comming from client
	 * @param command
	 * @throws IOException 
	 */
	public void executeCommand(Server.TCPCommands command, DataInputStream in) throws IOException {
		
		
		
		switch (command) {
		case send_game:
			// read game id
			int gameid = in.readInt();
			
			System.out.println("Receiving game with id "+gameid+" from "+clientName);
			
			// find the referenced gamie
			Game game = null;
			for (Game g : games_to_run) {
				if (g.gameId == gameid)
					game = g;
			}
			if (game == null) {
				System.err.println("Game with id "+gameid+" does not exist");
			}
			
			// receive fitnesses
			final long[] fitnesses = new long[2];
			for (int i = 0; i < 2; i++)
				fitnesses[i] = in.readLong();
			
			// simulate game done
			announceFinished(game, fitnesses);
			games_to_run.remove(game);
			
			System.out.println("Game "+gameid+" received from "+clientName);
			
			break;
		}
	}
		
	
	public void add(Game game) {
		super.add(game);
		send(game, Server.needToSend(this));
	}
	
	public void send(Game game, DataOutputStream out) {
		
		// wait for out to be ready
		while (out == null) {
			try {
				sleep(50);
			} catch (InterruptedException e) {}
		}
		
		// send game
		try {
			
			// write command
			out.writeByte(TCPCommands.receive_game.ordinal());
			
			// write gameid
			out.writeInt(game.gameId);
			
			for (int i = 0; i < 2; i++) {
				
				// write player id
				out.writeInt(game.ids[i]);
				
				if (game.ids[i] == -1)
					continue;
				
				double[] data = i == 0 ? game.weights_i : game.weights_j;
				
				// write population size
				out.writeInt(data.length);
				
				// write data
				for (int j = 0; j < data.length; j++)
					out.writeDouble(data[j]);
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	


}
