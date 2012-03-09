package sdp.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import sdp.AI.AIMaster;
import sdp.AI.AIMaster.mode;
import sdp.common.Communicator;
import sdp.common.Communicator.opcode;
import sdp.common.Vector2D;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;
import sdp.simulator.Simulator;
import sdp.simulator.VBrick;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

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

	private static final int max_speed = 35;
	private static final int max_turn_speed = 90;
	private int speed = 0;
	private static int turn_speed = 0;
	
	private double blue_placement, yellow_placement;
	
	private final HashMap<Integer, Timer> key_pressed = new HashMap<Integer, Timer>();
	
	private Integer camera = null;

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
		
		
		final JComboBox comboYellowAI = new JComboBox();
		comboYellowAI.setBounds(662, 379, 117, 24);
		for (int i = 0; i < mode.values().length; i++)
			comboYellowAI.addItem(mode.values()[i]);
		frmAlphaTeamSimulator.getContentPane().add(comboYellowAI);
		
		JButton btnChangeYellowAI = new JButton("Change Yellow AI");
		btnChangeYellowAI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				opponentAI.setState(mode.values()[comboYellowAI.getSelectedIndex()]);
			}
		});
		btnChangeYellowAI.setBounds(663, 412, 136, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnChangeYellowAI);
		
		JButton btnResetField = new JButton("Reset field");
		btnResetField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetField();
			}
		});
		btnResetField.setBounds(662, 156, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnResetField);
		
		JButton btnResetBall = new JButton("Reset ball");
		btnResetBall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mSim.putBallAt();
			}
		});
		btnResetBall.setBounds(662, 193, 117, 25);
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
		btnPause.setBounds(662, 119, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnPause);
		
		JButton btnRandomizeField = new JButton("Randomize");
		btnRandomizeField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				RandomizeField();
			}
		});
		btnRandomizeField.setBounds(662, 230, 117, 25);
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
		btnCamera.setBounds(662, 267, 117, 25);
		frmAlphaTeamSimulator.getContentPane().add(btnCamera);
		
		final JComboBox comboBlueAI = new JComboBox();
		comboBlueAI.setBounds(662, 299, 117, 27);
		for (int i = 0; i < mode.values().length; i++)
			comboBlueAI.addItem(mode.values()[i]);
		frmAlphaTeamSimulator.getContentPane().add(comboBlueAI);
		
		JButton btnChangeBlueAI = new JButton("Change Blue AI");
		btnChangeBlueAI.setBounds(662, 338, 137, 29);
		btnChangeBlueAI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mAI.setState(mode.values()[comboBlueAI.getSelectedIndex()]);
			}
		});
		frmAlphaTeamSimulator.getContentPane().add(btnChangeBlueAI);
		
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
		mSim = new Simulator(true);

		final WorldStateObserver obs = new WorldStateObserver(mSim);
		
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
		

		mAI = new AIMaster(mComm, mSim, AIMaster.AIMode.VISUAL_SERVOING);
		mAI.start(blue_selected, my_goal_left, false);
		
		opponentAI = new AIMaster(opponentComm, mSim, AIMaster.AIMode.VISUAL_SERVOING);
		opponentAI.start(!blue_selected, !my_goal_left, false);

		
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
		if (mAI.getState()!= mode.SIT) mAI.setState(mode.SIT);
		try {
			switch (key_id) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				speed = pressed ? max_speed : 0;
				//turn_speed = 0;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				speed = pressed ? -max_speed : 0;
				//turn_speed = 0;
				break;
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_A:
				turn_speed = pressed ? max_turn_speed : 0;
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_D:
				turn_speed = pressed ? -max_turn_speed : 0;
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
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
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
}

