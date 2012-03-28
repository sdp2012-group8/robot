package sdp.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import sdp.AI.AIMaster;
import sdp.AI.AIMaster.AIType;
import sdp.AI.AIMaster.AIState;
import sdp.common.Communicator;
import sdp.common.Communicator.opcode;
import sdp.common.geometry.Vector2D;
import sdp.common.world.Robot;
import sdp.common.world.WorldState;
import sdp.common.WorldStateObserver;
import sdp.simulator.Simulator;
import sdp.simulator.SimulatorPhysicsEngine;
import sdp.simulator.VBrick;

/**
 * Manual simulator control and in the same time AI tester
 * 
 * @author martinmarinov
 *
 */
public class SimTesterGUI {

	private static final double PLACEMENT_LEFT = 20; // in cm
	private static final double PLACEMENT_RIGHT = WorldState.PITCH_WIDTH_CM - PLACEMENT_LEFT; // in cm
	
	private JFrame frmAlphaTeamSimulator;
	
	private WorldState lastWS = null;
	
	private AIMaster mAI;
	 //will be used for the opponent(yellow) robot
	private AIMaster opponentAI;
	
	private Simulator mSim;
	private Communicator mComm;
	private Communicator opponentComm; 	//will be used for the opponent(yellow) robot
	private boolean drag_ball = false;
	private int drag_robot = -1;
	private JPanel panel;
	private int speed = 0;
	private static int turn_speed = 0;
	
	private double blue_placement, yellow_placement;
	
	private final HashMap<Integer, Timer> key_pressed = new HashMap<Integer, Timer>();
	
	private Integer camera = null;
	
	public JComboBox comboBlueAIs;
	public JComboBox comboYellowAIs;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SimTesterGUI window = new SimTesterGUI();
					window.frmAlphaTeamSimulator.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @wbp.parser.entryPoint
	 */
	public SimTesterGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmAlphaTeamSimulator = new JFrame();
		frmAlphaTeamSimulator.setTitle("Alpha Team Simulator and AI tester");
		frmAlphaTeamSimulator.setBounds(100,100,817,480);
		frmAlphaTeamSimulator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmAlphaTeamSimulator.getContentPane().setLayout(null);
		
		final JComboBox combo_team = new JComboBox();
		combo_team.setModel(new DefaultComboBoxModel(new String[] {"ME = Blue", "ME = Yellow"}));
		combo_team.setBounds(662, 47, 117, 24);
		frmAlphaTeamSimulator.getContentPane().add(combo_team);
//		
//		final JComboBox combo_goal = new JComboBox();
//		combo_goal.setModel(new DefaultComboBoxModel(new String[] {"ME : AI", "AI : ME"}));
//		combo_goal.setBounds(662, 83, 117, 24);
//		frmAlphaTeamSimulator.getContentPane().add(combo_goal);
		
		panel = new JPanel() {
			private static final long serialVersionUID = 8430961287318430359L;

			@Override
			protected void paintComponent(Graphics g) {
				Dimension d = this.getSize();
				if (lastWS != null) {
					synchronized (lastWS) {
						g.drawImage(lastWS.getWorldImage(), 0, 0, null);
					}
				} else {
					g.setColor(Color.gray);
					g.fillRect(0, 0, d.width, d.height);
				}
			}
		};
		panel.setFocusable(true);
		panel.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (mSim == null)
					return;
				drag_ball = false;
				mSim.highlightBall(drag_ball);
				drag_robot = -1;
				mSim.highlightRobot(drag_robot);
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (mSim == null)
					return;
				double sx = e.getX()/(double)panel.getWidth();
				double sy = e.getY()/(double) panel.getWidth();
				drag_ball = mSim.isInsideBall(sx, sy);
				drag_robot = mSim.isInsideRobot(sx, sy);
				mSim.highlightRobot(drag_robot);
			}

			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});
		panel.addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				if (mSim == null)
					return;
				double sx = e.getX()/(double)panel.getWidth();
				double sy = e.getY()/(double) panel.getWidth();
				mSim.highlightBall(drag_ball || mSim.isInsideBall(sx, sy));
				mSim.highlightRobot(drag_robot >= 0 ? drag_robot : mSim.isInsideRobot(sx, sy));
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (mSim == null)
					return;
				double sx = e.getX()/(double)panel.getWidth();
				double sy = e.getY()/(double) panel.getWidth();
				if (drag_ball)
					mSim.putBallAt(sx, sy);
				if (drag_robot != -1)
					mSim.putAt(sx, sy, drag_robot);
			}
		});
		panel.setBackground(Color.BLACK);
		panel.setBounds(10, 10, 640, 393);
		frmAlphaTeamSimulator.getContentPane().add(panel);
		
		final JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnConnect.setText("Wait...");
				btnConnect.setEnabled(false);
				//Connect(combo_team.getSelectedIndex() == 0, combo_goal.getSelectedIndex() == 0);
				Connect(combo_team.getSelectedIndex() == 0, true);
				btnConnect.setText("Ready!");
			}
		});
		btnConnect.setBounds(662, 10, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnConnect);
		
		/**
		 * Choose between the modes/states of the AIs for the enemy robot
		 */
		final JComboBox comboYellowModes = new JComboBox();
		comboYellowModes.setBounds(662, 388, 136, 24);
		for (int i = 0; i < AIState.values().length; i++)
			comboYellowModes.addItem(AIState.values()[i]);
		frmAlphaTeamSimulator.getContentPane().add(comboYellowModes);
		
		
		
		JButton btnResetField = new JButton("Reset field");
		btnResetField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetField();
			}
		});
		btnResetField.setBounds(662, 120, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnResetField);
		
		JButton btnResetBall = new JButton("Reset ball");
		btnResetBall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mSim.putBallAt();
			}
		});
		btnResetBall.setBounds(662, 157, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnResetBall);
		
		final JButton btnPause = new JButton("Pause");
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (mSim.getPause()) {
					mSim.setPause(false);
					btnPause.setText("Pause");
				} else {
					mSim.setPause(true);
					btnPause.setText("Resume");
				}
			}
		});
		btnPause.setBounds(662, 83, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnPause);
		
		JButton btnRandomizeField = new JButton("Randomize");
		btnRandomizeField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RandomizeField();
			}
		});
		btnRandomizeField.setBounds(662, 194, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnRandomizeField);
		
		JButton btnCamera = new JButton("Camera");
		btnCamera.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (camera == null)
					camera = -1;
				camera ++;
				if (camera == 2)
					camera = null;
				mSim.centerViewAround(camera);
			}
		});
		btnCamera.setBounds(662, 231, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnCamera);
		
		/**
		 * Choose between the modes/states of our robot
		 */
		final JComboBox comboBlueModes = new JComboBox();
		comboBlueModes.setBounds(662, 288, 136, 27);
		for (int i = 0; i < AIState.values().length; i++)
			comboBlueModes.addItem(AIState.values()[i]);
		frmAlphaTeamSimulator.getContentPane().add(comboBlueModes);
		
		/**
		 * Start the AI of our robot
		 */
		JButton btnStartBlueAI = new JButton("Start Blue AI");
		btnStartBlueAI.setBounds(662, 319, 136, 29);
		btnStartBlueAI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mAI.setState(AIState.values()[comboBlueModes.getSelectedIndex()]);
			}
		});
		frmAlphaTeamSimulator.getContentPane().add(btnStartBlueAI);
		
		/**
		 * Start both robots at the same time
		 * The default mode is visual servoing, if you want to change that,
		 * select the AI first, then press start 
		 */
		JButton btnStartBoth = new JButton("Start Game");
		btnStartBoth.setBounds(533, 415, 117, 29);
		btnStartBoth.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				mAI.setState(AIState.values()[comboBlueModes.getSelectedIndex()]);
				opponentAI.setState(AIState.values()[comboYellowModes.getSelectedIndex()]);				
			}
		});
		frmAlphaTeamSimulator.getContentPane().add(btnStartBoth);
		
		/**
		 * Change the AI of our robot
		 * Add another case statement if implementing a new AI
		 */
		comboBlueAIs = new JComboBox();
		comboBlueAIs.setBounds(662, 259, 136, 27);
		for (int i = 0; i < AIType.values().length; i++)
			comboBlueAIs.addItem(AIType.values()[i]);
		comboBlueAIs.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				mAI.setAIType(checkModesBlue());	
			}
		});
		frmAlphaTeamSimulator.getContentPane().add(comboBlueAIs);
		
		/**
		 * Change the AI of the enemy robot
		 * Add another case statement if implementing a new AI
		 */
		comboYellowAIs = new JComboBox();
		comboYellowAIs.setBounds(662, 360, 136, 27);
		for (int i = 0; i < AIType.values().length; i++)
			comboYellowAIs.addItem(AIType.values()[i]);
		comboYellowAIs.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				opponentAI.setAIType(checkModesYellow());
			}
		});
		frmAlphaTeamSimulator.getContentPane().add(comboYellowAIs);

		/**
		 * Start the AI  of the enemy robot
		 */
		JButton btnStartYellowAI = new JButton("Start Yellow AI");
		btnStartYellowAI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				opponentAI.setState(AIState.values()[comboYellowModes.getSelectedIndex()]);
			}
		});
		btnStartYellowAI.setBounds(662, 417, 136, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnStartYellowAI);
		
		JButton btnAiVisionOnoff = new JButton("AI vision on/off");
		btnAiVisionOnoff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (mAI != null)
					mAI.toggleDrawingOnWorldImage();
			}
		});
		btnAiVisionOnoff.setBounds(10, 415, 153, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnAiVisionOnoff);
		
		// key listener
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			
			@Override
			public boolean dispatchKeyEvent(final KeyEvent e) {
				switch (e.getID()) {
				case KeyEvent.KEY_PRESSED:
					Timer t = key_pressed.get(e.getKeyCode());
					if (t != null)
						t.cancel();
					else
						keyAction(e.getKeyCode(), true);
					key_pressed.put(e.getKeyCode(), null);
					break;
				case KeyEvent.KEY_RELEASED:
					t = new Timer();
					t.schedule(new TimerTask() {
						
						@Override
						public void run() {
							key_pressed.remove(e.getKeyCode());
							keyAction(e.getKeyCode(), false);
						}
					}, 35);
					key_pressed.put(e.getKeyCode(), t);
					break;
				default:
					break;
				}
				return false;
			}
		});
	}
	
	private void Connect(boolean blue_selected, boolean my_goal_left) {
		mComm = new VBrick();
		opponentComm = new VBrick();
		mSim = new SimulatorPhysicsEngine(true);
		
		if (blue_selected) {
			if (my_goal_left) {
				blue_placement = PLACEMENT_LEFT;
				yellow_placement = PLACEMENT_RIGHT;
			} else {
				blue_placement = PLACEMENT_RIGHT;
				yellow_placement = PLACEMENT_LEFT;
			}
		} else {
			if (my_goal_left) {
				blue_placement = PLACEMENT_RIGHT;
				yellow_placement = PLACEMENT_LEFT;
			} else {
				blue_placement = PLACEMENT_LEFT;
				yellow_placement = PLACEMENT_RIGHT;
			}
		}
		
		mSim.registerBlue(blue_selected ? (VBrick) mComm : (VBrick) opponentComm,
				blue_placement,
				WorldState.PITCH_HEIGHT_CM/2,
				blue_placement == PLACEMENT_LEFT ? 0 : 180);
		mSim.registerYellow(blue_selected ? (VBrick) opponentComm : (VBrick) mComm,
				yellow_placement,
				WorldState.PITCH_HEIGHT_CM/2,
				yellow_placement == PLACEMENT_LEFT ? 0 : 180);
		

		mAI = new AIMaster(mComm, mSim, checkModesBlue());
		mAI.start(blue_selected, my_goal_left);
		
		final WorldStateObserver obs = new WorldStateObserver(mAI);
		
		opponentAI = new AIMaster(opponentComm, mSim, checkModesYellow());
		opponentAI.start(!blue_selected, !my_goal_left);

		
		new Thread() {
			public void run() {
				while (true) {
					lastWS = obs.getNextState();
					panel.repaint();
				}
			};
		}.start();
	}
	
	/**
	 * Reset field
	 */
	private void resetField() {
		mAI.setState(AIMaster.AIState.SIT);
		opponentAI.setState(AIMaster.AIState.SIT);
		mSim.putBallAt();
		mSim.putAt(blue_placement/WorldState.PITCH_WIDTH_CM, WorldState.PITCH_HEIGHT_CM/(2*WorldState.PITCH_WIDTH_CM), 0, blue_placement == PLACEMENT_LEFT ?  0 : 180);
		mSim.putAt(yellow_placement/WorldState.PITCH_WIDTH_CM, WorldState.PITCH_HEIGHT_CM/(2*WorldState.PITCH_WIDTH_CM), 1, yellow_placement == PLACEMENT_LEFT ?  0 : 180);
	}
	
	/**
	 * Performs a key action
	 * @param key_id key id
	 * @param pressed true if pressed, false if released
	 */
	private void keyAction(final int key_id, final boolean pressed) {
		if (mAI == null) {
			return;
		}
		
		if (mAI.getState() != AIState.MANUAL_CONTROL)
			mAI.setState(AIState.MANUAL_CONTROL);
		try {
			switch (key_id) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				speed = pressed ? Robot.MAX_DRIVING_SPEED : 0;
				//turn_speed = 0;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				speed = pressed ? -Robot.MAX_DRIVING_SPEED : 0;
				//turn_speed = 0;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				turn_speed = pressed ? Robot.MAX_TURNING_SPEED : 0;
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				turn_speed = pressed ? -Robot.MAX_TURNING_SPEED : 0;
				break;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_E:
				if (pressed)
					mComm.sendMessage(opcode.kick);
				return;
			}
			if (speed > 127 || speed < -128)
				System.out.println("ERROR: CURRENT SPEED OVERFLOW!!! = "+speed);
			if (turn_speed > 127 || turn_speed < -128)
				System.out.println("ERROR: TURN SPEED OVERFLOW!!! = "+turn_speed);
			mComm.sendMessage(opcode.operate, (short) speed, (short) turn_speed);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void RandomizeField() {
		Random r = new Random();
		Vector2D ballpos, robot2;
		Vector2D robot1 = new Vector2D(
				(25 + r.nextDouble()*(WorldState.PITCH_WIDTH_CM-50))/WorldState.PITCH_WIDTH_CM,
				(25 + r.nextDouble()*(WorldState.PITCH_HEIGHT_CM-50))/WorldState.PITCH_WIDTH_CM);
		while (true) {
			robot2 = new Vector2D(
					(25 + r.nextDouble()*(WorldState.PITCH_WIDTH_CM-50))/WorldState.PITCH_WIDTH_CM,
					(25 + r.nextDouble()*(WorldState.PITCH_HEIGHT_CM-50))/WorldState.PITCH_WIDTH_CM);
			if (Vector2D.subtract(robot1, robot2).getLength() > 35/WorldState.PITCH_WIDTH_CM)
				break;
		}
		while (true) {
			ballpos = new Vector2D(
					(7.5 + r.nextDouble()*(WorldState.PITCH_WIDTH_CM-30))/WorldState.PITCH_WIDTH_CM,
					(7.5 + r.nextDouble()*(WorldState.PITCH_HEIGHT_CM-30))/WorldState.PITCH_WIDTH_CM);
			if (Vector2D.subtract(robot1, ballpos).getLength() > 35/WorldState.PITCH_WIDTH_CM &&
					Vector2D.subtract(robot1, ballpos).getLength() > 35/WorldState.PITCH_WIDTH_CM)
				break;
		}
		mSim.putAt(robot1.getX(), robot1.getY(), 0, 180-r.nextInt(360));
		mSim.putAt(robot2.getX(), robot2.getY(), 1, 180-r.nextInt(360));
		mSim.putBallAt(ballpos.getX(), ballpos.getY());
	}
	
	private AIType checkModesBlue(){	
		return AIType.values()[comboBlueAIs.getSelectedIndex()];
	}
	
	private AIType checkModesYellow(){	
		return AIType.values()[comboYellowAIs.getSelectedIndex()];
	}
}

