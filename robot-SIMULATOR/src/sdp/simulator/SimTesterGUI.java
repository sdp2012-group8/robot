package sdp.simulator;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JLabel;

import sdp.AI.AI;
import sdp.AI.AIVisualServoing;
import sdp.AI.AI.mode;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * This class is intended to display the world as the UI "sees" it.
 * 
 * It also starts the AI.
 * 
 * @author martinmarinov
 *
 */
public class SimTesterGUI {

	private JFrame frmAlphaTeamAi;
	private boolean running = false;
	private AI mAI;
	private Simulator mSim;
	private boolean drag_ball = false;
	private int drag_robot = -1;
	private WorldState lastWS = null;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SimTesterGUI window = new SimTesterGUI();
					window.frmAlphaTeamAi.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public SimTesterGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmAlphaTeamAi = new JFrame();
		frmAlphaTeamAi.setTitle("Alpha Team AI tester");
		frmAlphaTeamAi.setBounds(100, 100, 805, 530);
		frmAlphaTeamAi.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmAlphaTeamAi.getContentPane().setLayout(null);
		
		final JPanel panel = new JPanel() {

			private static final long serialVersionUID = 4129875804950156591L;

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
		panel.setBounds(10, 10, 640, 480);
		frmAlphaTeamAi.getContentPane().add(panel);
	
		
		JLabel lblOurTeam = new JLabel("Our team:");
		lblOurTeam.setBounds(656, 50, 137, 15);
		frmAlphaTeamAi.getContentPane().add(lblOurTeam);
		
		ButtonGroup group_team = new ButtonGroup();
		
		final JRadioButton rdbtnBlue = new JRadioButton("blue");
		rdbtnBlue.setBounds(656, 100, 137, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnBlue);
		rdbtnBlue.setSelected(true);
		
		final JRadioButton rdbtnNewRadioButton = new JRadioButton("yellow");
		rdbtnNewRadioButton.setBounds(658, 73, 137, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnNewRadioButton);
		
		group_team.add(rdbtnBlue);
		group_team.add(rdbtnNewRadioButton);
		
		JLabel lblOurGoal = new JLabel("Our goal:");
		lblOurGoal.setBounds(656, 131, 70, 15);
		frmAlphaTeamAi.getContentPane().add(lblOurGoal);
		
		ButtonGroup group_goal = new ButtonGroup();
		
		final JRadioButton rdbtnLeft = new JRadioButton("left");
		rdbtnLeft.setBounds(658, 154, 149, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnLeft);
		
		final JRadioButton rdbtnRight = new JRadioButton("right");
		rdbtnRight.setSelected(true);
		rdbtnRight.setBounds(658, 181, 149, 23);
		frmAlphaTeamAi.getContentPane().add(rdbtnRight);
		
		group_goal.add(rdbtnLeft);
		group_goal.add(rdbtnRight);
		
		final JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!running) {
					mSim = new Simulator();
					VBrick brick = new VBrick();
					if (rdbtnBlue.isSelected()){
						mSim.registerBlue(brick, 20, 20);
					}
					else{
					mSim.registerYellow(brick, 20, 20);
					}
					mAI = new AIVisualServoing(brick, mSim);
					mAI.start(rdbtnBlue.isSelected(), rdbtnLeft.isSelected());
					final WorldStateObserver obs = new WorldStateObserver(mAI);
					new Thread() {
						public void run() {
							while (true) {
								lastWS = obs.getNextState();
								panel.repaint();
							}
						};
					}.start();

						running =true;
						btnConnect.setText("Disconnect");
				} else {
					mAI.close();
					mSim.stop();
					running = false;
					btnConnect.setText("Connect");
				}

			}
		});
		btnConnect.setBounds(656, 10, 117, 25);
		frmAlphaTeamAi.getContentPane().add(btnConnect);
		
		final JButton btnSendComm = new JButton("Chase Ball");
		btnSendComm.setBounds(656, 212, 117, 25);
		btnSendComm.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				mAI.setMode(mode.chase_ball);
			}
			
		});
		frmAlphaTeamAi.getContentPane().add(btnSendComm);
		
		JButton btnResetBall = new JButton("Reset ball");
		btnResetBall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				mSim.putBallAt();
			}
		});
		btnResetBall.setBounds(662, 391, 117, 25);
		frmAlphaTeamAi.getContentPane().add(btnResetBall);

	}
}
