package sdp.simulator.neural;

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

import sdp.AI.AI;
import sdp.AI.AI.mode;
import sdp.AI.neural.AINeuralNetwork;
import sdp.AI.AIVisualServoing;
import sdp.common.Communicator;
import sdp.common.Communicator.opcode;
import sdp.common.Robot;
import sdp.common.Tools;
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
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JEditorPane;

public class NeuralTrainer {

	private static final double placement_right = 20; // in cm
	private static final double placement_left = Simulator.pitch_width_cm - placement_right; // in cm
	private static final double mouse_control_delay = 3; // how many seconds should pass before the robot reaches the mouse
	
	JFrame frame;
	
	private WorldState lastWS = null;
	
	private AI mAI;
	private Simulator mSim;
	private Communicator mComm;
	private boolean drag_ball = false;
	private int drag_robot = -1;
	private JPanel panel;
	private JCheckBox chckbxMouseControl;
	private JComboBox combo_AI;
	private JEditorPane dtrpnhomemartinmarinov;

	private static final int max_speed = 35;
	private static final int max_turn_speed = 90;
	private int speed = 0;
	private static int turn_speed = 0;
	
	private double mouse_scaled_x, mouse_scaled_y;
	
	private boolean blue_selected = false;
	
	private double blue_placement, yellow_placement;
	
	private NeuralNetworkTrainingGenerator trainer = null;
	
	private final HashMap<Integer, Timer> key_pressed = new HashMap<Integer, Timer>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					NeuralTrainer window = new NeuralTrainer();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public NeuralTrainer() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 802, 447);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		final JComboBox combo_team = new JComboBox();
		combo_team.setModel(new DefaultComboBoxModel(new String[] {"Blue", "Yellow"}));
		combo_team.setBounds(662, 47, 117, 24);
		frame.getContentPane().add(combo_team);
		
		final JComboBox combo_goal = new JComboBox();
		combo_goal.setModel(new DefaultComboBoxModel(new String[] {"Mine left", "Mine right"}));
		combo_goal.setBounds(662, 83, 117, 24);
		frame.getContentPane().add(combo_goal);
		
		chckbxMouseControl = new JCheckBox("Mouse control");
		chckbxMouseControl.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				if (!chckbxMouseControl.isSelected()) {
					if (mComm != null)
						try {
							mComm.sendMessage(opcode.operate, (byte) 0, (byte) 0);
						} catch (Exception e) {}
				}
			}
		});
		chckbxMouseControl.setBounds(662, 187, 129, 23);
		frame.getContentPane().add(chckbxMouseControl);
		
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
				mouse_scaled_x = e.getX()/(double)panel.getWidth();
				mouse_scaled_y = e.getY()/(double) panel.getWidth();
				drag_ball = mSim.isInsideBall(mouse_scaled_x, mouse_scaled_y);
				drag_robot = mSim.isInsideRobot(mouse_scaled_x, mouse_scaled_y);
				mSim.highlightRobot(drag_robot);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				switch (e.getButton()) {
				case MouseEvent.BUTTON1:
					if (chckbxMouseControl.isSelected()) {
						if (mComm != null)
							try {
								mComm.sendMessage(opcode.kick);
							} catch (Exception e2) {}
					}
					break;
				case MouseEvent.BUTTON3:
					chckbxMouseControl.setSelected(!chckbxMouseControl.isSelected());
					break;
				}

			}

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
				mouse_scaled_x = e.getX()/(double)panel.getWidth();
				mouse_scaled_y = e.getY()/(double) panel.getWidth();
				mSim.highlightBall(drag_ball || mSim.isInsideBall(mouse_scaled_x, mouse_scaled_y));
				mSim.highlightRobot(drag_robot >= 0 ? drag_robot : mSim.isInsideRobot(mouse_scaled_x, mouse_scaled_y));
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (mSim == null)
					return;
				mouse_scaled_x = e.getX()/(double)panel.getWidth();
				mouse_scaled_y = e.getY()/(double) panel.getWidth();
				if (drag_ball)
					mSim.putBallAt(mouse_scaled_x, mouse_scaled_y);
				if (drag_robot != -1)
					mSim.putAt(mouse_scaled_x, mouse_scaled_y, drag_robot);
			}
		});
		panel.setBackground(Color.BLACK);
		panel.setBounds(10, 10, 640, 393);
		frame.getContentPane().add(panel);
		
		final JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnConnect.setText("Wait...");
				btnConnect.setEnabled(false);
				blue_selected = combo_team.getSelectedIndex() == 0;
				Connect(combo_goal.getSelectedIndex() != 0);
				btnConnect.setText("Ready!");
			}
		});
		btnConnect.setBounds(662, 10, 117, 25);
		frame.getContentPane().add(btnConnect);
		
		JButton btnChaseBall = new JButton("AI: Chase");
		btnChaseBall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mAI.setMode(mode.chase_ball);
			}
		});
		btnChaseBall.setBounds(662, 153, 117, 25);
		frame.getContentPane().add(btnChaseBall);
		
		JButton btnResetField = new JButton("Reset field");
		btnResetField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resetField();
			}
		});
		btnResetField.setBounds(662, 119, 117, 25);
		frame.getContentPane().add(btnResetField);
		
		dtrpnhomemartinmarinov = new JEditorPane();
		dtrpnhomemartinmarinov.setText("/home/martinmarinov/netowrk.tset");
		dtrpnhomemartinmarinov.setBounds(662, 266, 117, 63);
		frame.getContentPane().add(dtrpnhomemartinmarinov);
		
		final JButton btnNewButton = new JButton("Record");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (trainer != null) {
					if (trainer.isRecording()) {
						trainer.Stop();
						btnNewButton.setText("Record");
					} else {
						trainer.Record(blue_selected);
						btnNewButton.setText("Stop");
					}
				}
			}
		});
		btnNewButton.setBounds(662, 341, 117, 25);
		frame.getContentPane().add(btnNewButton);
		
		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (trainer != null)
					trainer.Save();
			}
		});
		btnSave.setBounds(662, 378, 117, 25);
		frame.getContentPane().add(btnSave);
		
		combo_AI = new JComboBox();
		combo_AI.setModel(new DefaultComboBoxModel(new String[] {"VisualServoing", "NeuralNetwork"}));
		combo_AI.setBounds(662, 218, 117, 24);
		frame.getContentPane().add(combo_AI);
		

		
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
	
	private void Connect(boolean my_door_right) {
		
		mSim = new Simulator();
		trainer = new NeuralNetworkTrainingGenerator(mSim, dtrpnhomemartinmarinov.getText());
		mComm = (Communicator) trainer;
		final WorldStateObserver obs = new WorldStateObserver(mSim);
		VBrick bot = new VBrick();
		blue_placement = blue_selected ? (my_door_right ? placement_left : placement_right) : (my_door_right ? placement_right : placement_left);
		yellow_placement = blue_placement == placement_left ? placement_right : placement_left;
		mSim.registerBlue(blue_selected ? (VBrick) mComm : bot,
				blue_placement,
				Simulator.pitch_height_cm/2,
				blue_placement == placement_left ? 180: 0);
		mSim.registerYellow(blue_selected ? bot : (VBrick) mComm,
				yellow_placement,
				Simulator.pitch_height_cm/2,
				yellow_placement == placement_left ? 180 : 0);
		switch (combo_AI.getSelectedIndex()) {
		case 0:
			mAI = new AIVisualServoing(bot, mSim);
			break;
		case 1:
			mAI = new AINeuralNetwork(bot, mSim, dtrpnhomemartinmarinov.getText(), my_door_right);
			break;
		}
		
		mAI.start(!blue_selected, my_door_right);
		new Thread() {
			public void run() {
				while (true) {
					lastWS = obs.getNextState();
					// mouse control
					if (chckbxMouseControl.isSelected() && mComm != null) {
						Robot my = blue_selected ? lastWS.getBlueRobot() : lastWS.getYellowRobot();
						Vector2D d = new Vector2D(
								(-my.getCoords().getX()+mouse_scaled_x)*Simulator.pitch_width_cm,
								(-my.getCoords().getY()+mouse_scaled_y)*Simulator.pitch_width_cm);
						double angle = Tools.normalizeAngle(-my.getAngle()+Vector2D.getDirection(d));
						double speed = (d.getLength()/200)*3*max_speed;
						if (speed < 0)
							speed = 0;
						if (angle > 128)
							angle = 128;
						if (angle < -127)
							angle = -127;
						if (speed > 127)
							speed = 127;
						if (speed < -127)
							speed = -127;
						if (angle > 120 || angle < -120)
							speed = -speed;
						try {
							mComm.sendMessage(
									opcode.operate, (byte) (speed),
									(byte) (angle));
						} catch (Exception e) {}
					}
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
		mSim.putAt(blue_placement/Simulator.pitch_width_cm, Simulator.pitch_height_cm/(2*Simulator.pitch_width_cm), 0, blue_placement == placement_left ? 180: 0);
		mSim.putAt(yellow_placement/Simulator.pitch_width_cm, Simulator.pitch_height_cm/(2*Simulator.pitch_width_cm), 1, yellow_placement == placement_left ? 180: 0);
	}
	
	/**
	 * Performs a key action
	 * @param key_id key id
	 * @param pressed true if pressed, false if released
	 */
	private void keyAction(final int key_id, final boolean pressed) {
		if (mComm == null)
			return;
		try {
			switch (key_id) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				speed = pressed ? max_speed : 0;
				turn_speed = 0;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_S:
				speed = pressed ? -max_speed : 0;
				turn_speed = 0;
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
				if (pressed)
					mComm.sendMessage(opcode.kick);
				return;
			}
			if (speed > 128 || speed < -127)
				System.out.println("ERROR: CURRENT SPEED OVERFLOW!!! = "+speed);
			if (turn_speed > 128 || turn_speed < -127)
				System.out.println("ERROR: TURN SPEED OVERFLOW!!! = "+turn_speed);
			mComm.sendMessage(opcode.operate, (byte) speed, (byte) turn_speed);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
