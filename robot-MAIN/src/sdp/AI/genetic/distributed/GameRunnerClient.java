package sdp.AI.genetic.distributed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import sdp.AI.genetic.Game;
import sdp.AI.genetic.GameRunner;
import sdp.AI.genetic.distributed.Server.TCPCommands;

/**
 * This file runs stand-alone on a different machine,
 * supplying data to a {@link GameRunnerServer}.<br/><br/>
 * 
 * Expected command line arguments are hostname and port for TCP connection.
 * @author Martin Marinov
 *
 */
public class GameRunnerClient extends GameRunner {
	
	// the standalone but
	
	private static final String NAME = new File(GameRunnerClient.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
	private static final String USAGE = (NAME.endsWith(".jar") ? "Usage: java -jar "+NAME : "Usage: java "+GameRunner.class.getSimpleName())+" hostname port";
	
	public static void main(String[] args) {
		
		//args = new String[] {"localhost"};
		
		try {
			new GameRunnerClient(args[0], Server.port);
		} catch (NumberFormatException e) {
			System.err.println("Wrong port number supplied.\n\n"+USAGE);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Wrong arguments supplied.\n\n"+USAGE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// the implementation
	
	private Socket tcpClient;
	private Thread listener;
	private DataInputStream in;
	private DataOutputStream out;
	
	public GameRunnerClient(String hostname, int port) throws IOException {
		tcpClient = new Socket(hostname, port);
		in = new DataInputStream(tcpClient.getInputStream());
		out = new DataOutputStream(tcpClient.getOutputStream());
		
		listener = new Thread() {
			@Override
			public void run() {
				
				while (!interrupted()) {
					try {
					
					// if new command is coming, read it
					executeCommand(TCPCommands.values()[in.read()]);
					
					} catch (Exception e) {
						
						try {
							sleep(100);
						} catch (InterruptedException e1) {}
						
					}
				}
				
				try {
					in.close();
					out.close();
					tcpClient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		};
		
		listener.start();
		this.start();
	}
	
	/**
	 * Executes a command comming from server
	 * @param command
	 * @throws IOException 
	 */
	private void executeCommand(TCPCommands command) throws IOException {
		
		
		switch (command) {
		case receive_game:
			// the protocol is: get gameID, get i, get data size for i, get data for i, get j, get data size for j, get data for j
			// if i or j are -1, we don't get data
			final int gameId = in.readInt();
			
			final int[] ids = new int[2];
			final double[][] population = new double[2][];
			for (int i = 0; i < 2; i++) {
				
				// read id
				ids[i] = in.readInt();
				
				if (ids[i] == -1) {
					population[i] = null;
					continue;
				}
				
				// read data size
				final int size = in.readInt();
				population[i] = new double[size];
				
				// fill population data
				for (int j = 0; j < size; j++)
					population[i][j] = in.readDouble();
			}
			add(new Game(ids[0], ids[1], population[0], population[1], gameId));
			
			System.out.println("Received "+gameId+" from server");
			break;
		case close:
			System.out.println("Received close command. Closing...");
			listener.interrupt();
			break;
		}
	}
	
	/**
	 * Send data back to server
	 */
	@Override
	protected void announceFinished(Game caller, long[] fitness) {
		
		System.out.println("Sending game "+caller.gameId+" to server");
		
		try {
			
			// write command
			out.writeByte(TCPCommands.send_game.ordinal());
			
			// write game id
			out.writeInt(caller.gameId);
			
			// write data
			for (int i = 0; i < fitness.length; i++)
				out.writeLong(fitness[i]);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void close() {
		listener.interrupt();
	}

}
