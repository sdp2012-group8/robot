package sdp.vision.testbench;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.image.BufferedImage;

import sdp.vision.processing.ImageProcessorConfig;

import sdp.common.WorldState;

public class Viewer extends Panel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	BufferedImage image;
	WorldState manstate, visstate;
	ImageProcessorConfig config = new ImageProcessorConfig();
	public Viewer(BufferedImage animage, WorldState astate, WorldState bstate){
		image = animage;
		manstate = astate;
		visstate = bstate;
	}
	
	//Allows the viewer to update on each iteration by passing it a new image and states.
	public void updateImageAndState(BufferedImage animage,WorldState astate, WorldState bstate){
		image = animage;
		manstate = astate;
		visstate = bstate;
		//Viewer is repainted to draw new state information on the new image.
		repaint();
	}
	
	//Used to rescale, now just used for typecasting
	public int correctX (double x){
		return (int) x;
	}
	
	//Used to rescale, now just used for typecasting
	public int correctY (double y){
		return (int) y;
	}
	
	public void paint(Graphics g){
		if (image != null){
			
			//BufferedImage is loaded into graphics object
			g.drawImage( image, 0, 0, null);
			
        	/*
			// MANUAL TAGGING
			//Draw Backgrounds
			//Draw Ball
			g.setColor(Color.red);
			g.fillRect((int)(manstate.getBallCoords().x)-4,(int)(manstate.getBallCoords().y)-4, 8, 8);
			//Draw blue robot
			g.setColor(Color.blue);
			g.fillRect((int)(manstate.getBlueRobot().getCoords().x)-4,(int)(manstate.getBlueRobot().getCoords().y)-4, 8, 8);
			//Draw yellow robot
			g.setColor(Color.yellow);
			g.fillRect((int)(manstate.getYellowRobot().getCoords().x)-4,(int)(manstate.getYellowRobot().getCoords().y)-4, 8, 8);

			//Draw Borders                
			//Draw ball
			g.setColor(Color.green);
			g.drawRect((int)(manstate.getBallCoords().x)-4,(int)(manstate.getBallCoords().y)-4, 8, 8);
			//Draw blue robot
			g.drawRect((int)(manstate.getBlueRobot().getCoords().x)-4,(int)(manstate.getBlueRobot().getCoords().y)-4, 8, 8);
			//Draw yellow robot
			g.drawRect((int)(manstate.getYellowRobot().getCoords().x)-4,(int)(manstate.getYellowRobot().getCoords().y)-4, 8, 8);
        	*/
			
			
		// VISION TAGGING
     	//Draw Backgrounds
			//Draw Ball
			g.setColor(Color.red);
			g.fillRect(correctX(visstate.getBallCoords().x)-4,correctY(visstate.getBallCoords().y)-4, 8, 8);
			//Draw blue robot
			g.setColor(Color.blue);
			g.fillRect(correctX(visstate.getBlueRobot().getCoords().x)-4,correctY(visstate.getBlueRobot().getCoords().y)-4, 8, 8);
			//Draw yellow robot
			g.setColor(Color.yellow);
			g.fillRect(correctX(visstate.getYellowRobot().getCoords().x)-4,correctY(visstate.getYellowRobot().getCoords().y)-4, 8, 8);
     		
		//Draw borders
     		g.setColor(Color.red);
			//Draw ball
			g.drawRect(correctX(visstate.getBallCoords().x)-4,correctY(visstate.getBallCoords().y)-4, 8, 8);
			//Draw blue robot
			g.drawRect(correctX(visstate.getBlueRobot().getCoords().x)-4,correctY(visstate.getBlueRobot().getCoords().y)-4, 8, 8);
			//Draw yellow robot
			g.drawRect(correctX(visstate.getYellowRobot().getCoords().x)-4,correctY(visstate.getYellowRobot().getCoords().y)-4, 8, 8);
		}
	}
}
