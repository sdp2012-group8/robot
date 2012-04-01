package sdp.common;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;

public class WorldStatePlayer extends WorldStateProvider {

	private static final int REALFPS = 30;
	private static final int REALWAIT = 1000/REALFPS;
	private static int DEFAULT_MOVIE_FPS;
	
	private Thread worker;
	private volatile double fps;
	private volatile double waitTime;
	private double frameId;
	private WorldState[] frames;
	private String[] subtitles;
	
	public WorldStatePlayer(int default_movie_fps) {
		DEFAULT_MOVIE_FPS = default_movie_fps;
		fps = DEFAULT_MOVIE_FPS;
		waitTime = 1d/fps;
		frameId = 0;
		worker = new Thread() {
			@Override
			public void run() {
				while (!interrupted()) {
					setChanged();
					notifyObservers(getNextFrame());
					
					if (frames != null) {
						if (fps != 0) {
							if (fps > 0)
								frameId += (REALWAIT/1000d)/waitTime;
							else
								frameId -= (REALWAIT/1000d)/waitTime;
						}

						if (frameId >= frames.length)
							frameId = 0;

						if (frameId < 0)
							frameId = frames.length-1;
					}
					try {
						sleep(REALWAIT);
					} catch (InterruptedException e) {}
				}
			}
		};
		worker.start();
	}
	
	public void loadMovie(String dir) throws FileNotFoundException {
		frameId = 0;
		setFPS(0);
		
		 int i = 0;
		 ArrayList<WorldState> frames = new ArrayList<WorldState>();
		 ArrayList<String> subtitles = new ArrayList<String>();
		 while (true) {
			 final String fileName = dir+"/frame"+(i++)+".xml";
			 if (!new File(fileName).exists()) {
				 if (i == 1)
					 throw new FileNotFoundException("Directory "+dir+" does not contain frames (namely "+fileName+")");
				 break;
			 }
			 final StringBuilder build = new StringBuilder();
			 frames.add(WorldState.loadWorldState(fileName, build));
			 subtitles.add(build.toString());
			 
		 }
		 this.frames = frames.toArray(new WorldState[0]);
		 this.subtitles = subtitles.toArray(new String[0]);
		
		setFPS(DEFAULT_MOVIE_FPS);
		
	}
	
	public String getSubtitle() {
		if (frameId > frames.length - 1)
			frameId = frames.length - 1;
		if (frameId < 0)
			frameId = 0;
		return subtitles[(int) frameId];
	}
	
	public void setFPS(double fps) {
		this.fps = fps;
		if (fps != 0)
			waitTime = Math.abs(1d/fps);
	}
	
	public void stop() {
		worker.interrupt();
	}
	
	public double getFrame() {
		return frameId;
	}
	
	public void setFrame(double frameId) {
		if (frames == null)
			return;
			
		this.frameId = frameId;
		
		if (frameId > frames.length - 1)
			frameId = frames.length - 1;
		if (frameId < 0)
			frameId = 0;
	}
	
	public int getFrameCount() {
		if (frames == null)
			return 0;
		return frames.length;
	}
	
	private WorldState getNextFrame() {
		
		if (frames == null || frames.length == 0) {
			return new WorldState(new Point2D.Double(10000, 0),
					new Robot(new Point2D.Double(10000, 0), 0),
					new Robot(new Point2D.Double(10000, 0), 0),
					null);
		}
		
		if (frameId > frames.length - 1)
			frameId = frames.length - 1;
		if (frameId < 0)
			frameId = 0;
		
		int lowId;
		double coeff;
		WorldState fr1, fr2;
		if (fps >= 0) {
			
			lowId = (int) frameId;
			coeff = frameId - lowId;
			fr1 = frames[lowId];
			if (frameId == frames.length-1)
				return fr1;
			fr2 = frames[lowId+1];
			
		} else {
			if (frameId < 1)
				frameId = 1;
			lowId = (int) (frameId-1);
			fr1 = frames[lowId];
			if (lowId == 0)
				return fr1;
			coeff = (frameId-1) - lowId;
			fr2 = frames[lowId+1];
			
		}
		return new WorldState(interpolate(fr1.getBallCoords(), fr2.getBallCoords(), coeff),
				interpolate(fr1.getBlueRobot(), fr2.getBlueRobot(), coeff),
				interpolate(fr1.getYellowRobot(), fr2.getYellowRobot(), coeff),
				fr1.getWorldImage());
	}
	
	private Robot interpolate(Robot fr1, Robot fr2, double coeff) {
		return new Robot(interpolate(fr1.getCoords(), fr2.getCoords(), coeff), interpolateAngle(fr1.getAngle(), fr2.getAngle(), coeff));
	}
	
	private Point2D.Double interpolate(Point2D.Double fr1, Point2D.Double fr2, double coeff) {
		return new Point2D.Double(interpolate(fr1.x, fr2.x, coeff), interpolate(fr1.y, fr2.y, coeff));
	}
	
	private double interpolate(double fr1, double fr2, double coeff) {
		return fr1+(fr2-fr1)*coeff;
	}
	
	private double interpolateAngle(double fr1, double fr2, double coeff) {
		Vector2D oldVec = Vector2D.rotateVector(new Vector2D(1, 0), fr1);
		Vector2D newVec = Vector2D.rotateVector(new Vector2D(1, 0), fr2);
		Vector2D sum = Vector2D.add(oldVec, Vector2D.multiply(Vector2D.subtract(newVec, oldVec), coeff));
		return sum.getDirection();
	}
	

}
