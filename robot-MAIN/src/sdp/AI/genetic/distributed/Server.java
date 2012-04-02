package sdp.AI.genetic.distributed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


/**
 * This is the only server that runs and distributes queries.
 * 
 * @author Martin Marinov
 *
 */
public class Server {
	
	public enum TCPCommands { send_game, close, receive_game }
	public enum ACKs { OK, WAIT }
	
	/** THIS IS THE PORT THAT THE SERVER IS LISTENING AT */
	public static final int port = 7843;
	
	private static ServerSocket server;
	private static Map<GameRunnerServer, DataOutputStream> server_client_map = new HashMap<GameRunnerServer, DataOutputStream>();
	
	private static DataOutputStream out = null;
	
	public static void registerServer(GameRunnerServer gserv) {
		
		if (server == null)
			initServer();
		
		server_client_map.put(gserv, null);
	}
	
	/**
	 * Initialize server and start listening
	 */
	private static void initServer() {
		try {
			server = new ServerSocket(port);

			new Thread() {
				public void run() {

					while (!interrupted()) {
						try {
							final Socket tcpServer = server.accept();
							tcpServer.setTcpNoDelay(true);
							final DataInputStream in = new DataInputStream(tcpServer.getInputStream());

							GameRunnerServer serv = null;
							// map the client to server
							for (GameRunnerServer gserv : server_client_map.keySet()){
								final DataOutputStream s = server_client_map.get(gserv);
								if (s == null) {
									server_client_map.put(gserv, new DataOutputStream(tcpServer.getOutputStream()));
									serv = gserv;
									break;
								}
							}
							
							if (serv == null)
								System.err.println("WE HAVE A BIG ERROR HERE");
							
							final GameRunnerServer gameServer = serv;
							
							System.out.println("New client assigned to "+gameServer.clientName);
							gameServer.assigned = true;

							new Thread() {
								public void run() {
									while (!interrupted()) {
										
										try {
											gameServer.executeCommand(TCPCommands.values()[in.read()], in);
										} catch (IOException e) {
											e.printStackTrace();
										}
										
									}
								};
							}.start();

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
					try {
						
						// send stop command
						out.writeByte(TCPCommands.close.ordinal());
						
						server.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				};
			}.start();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static DataOutputStream needToSend(GameRunnerServer gserv) {
		return server_client_map.get(gserv);
	}
	
	public static InetAddress getIP() {
		return server.getInetAddress();
	}
	
	public static int getPort() {
		return server.getLocalPort();
	}
	
}
