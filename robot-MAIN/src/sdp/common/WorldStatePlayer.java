package sdp.common;

import java.io.File;
import java.io.FileNotFoundException;

import sdp.common.world.WorldState;

public class WorldStatePlayer extends WorldStateProvider {

	private Thread worker;
	private int fps = 15;
	private long waitTime = 1000/fps;
	private int frameId = 0;
	private String dir;
	
	public WorldStatePlayer(String dir) throws FileNotFoundException {
		 this.dir = dir;
	}
	
	public void setFPS(int fps) {
		this.fps = fps;
		waitTime = 1000/fps;
	}
	
	public void start() {
		worker = new Thread() {
			@Override
			public void run() {
				while (!interrupted()) {
					String fileName = dir+"/frame"+(frameId++)+".xml";
					if (!new File(fileName).exists()) {
						if (frameId == 1) {
							System.err.println("Movie does not exist ("+fileName+")");
							return;
						}
						frameId = 0;
						fileName = dir+"/frame"+(frameId++)+".xml";
					}
					try {
						sleep(waitTime);
					} catch (InterruptedException e) {}
					setChanged();
					notifyObservers(WorldState.loadWorldState(fileName));
				}
			}
		};
		worker.start();
	}
	
	public void stop() {
		worker.interrupt();
	}
	
	public int getFrame() {
		return frameId;
	}

}
