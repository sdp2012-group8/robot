package sdp.common.world;

import java.io.File;

public class WorldStatePlayer {
	
	private File[] listOfFiles;
	private Thread worker;
	
	public WorldStatePlayer(String dir) {
		 File folder = new File(dir);
		 listOfFiles = folder.listFiles(); 
	}
	
	public void start() {
		worker = new Thread() {
			@Override
			public void run() {
				while (!interrupted()) {
					
				}
			}
		};
		worker.start();
	}

}
