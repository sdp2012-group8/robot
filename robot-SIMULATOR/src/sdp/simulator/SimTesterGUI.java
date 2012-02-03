package sdp.simulator;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import sdp.AI.AI;
import sdp.AI.AI.mode;
import sdp.common.Communicator;
import sdp.common.WorldState;
import sdp.common.WorldStateObserver;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JPanel;

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
		frmAlphaTeamAi.setBounds(100, 100, 805, 547);
		frmAlphaTeamAi.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmAlphaTeamAi.getContentPane().setLayout(null);
		
		final JPanel 		panel = new JPanel() {
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
		panel.setBackground(Color.BLACK);
		panel.setBounds(10, 10, 640, 480);
		frmAlphaTeamAi.getContentPane().add(panel);
		
		ButtonGroup group_input = new ButtonGroup();
		
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
		
		JLabel lblRefreshRate = new JLabel("refresh rate");
		lblRefreshRate.setBounds(10, 496, 108, 15);
		frmAlphaTeamAi.getContentPane().add(lblRefreshRate);
		
		final JLabel lblFps = new JLabel("15 fps");
		lblFps.setBounds(304, 496, 70, 15);
		frmAlphaTeamAi.getContentPane().add(lblFps);
		
		final JSlider slider = new JSlider();
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lblFps.setText(slider.getValue()+" fps");
			}
		});
		slider.setValue(15);
		slider.setMaximum(30);
		slider.setMinimum(2);
		slider.setBounds(104, 495, 200, 16);
		frmAlphaTeamAi.getContentPane().add(slider);
		
		final JButton btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (!running) {
					Simulator sim = new Simulator();
					VBrick brick = new VBrick();
					if(rdbtnBlue.isSelected()){
						sim.registerBlue(brick, 20, 20);
					}
					else{
					sim.registerYellow(brick, 20, 20);
					}
					mAI = new AI(brick, sim);
					mAI.start(rdbtnBlue.isSelected(), rdbtnLeft.isSelected());
						new Thread() {
							public void run() {
								while (running)
									if (mAI != null) {
										lastWS = mAI.getLatestWorldState();
										if (lastWS != null)
											// System.out.println("dir "+lastWS.getYellowRobot().getAngle());
										panel.repaint();
										try {
											Thread.sleep(1000/slider.getValue());
										} catch (InterruptedException e) {}
									}
							};
						}.start();
						running =true;
						btnConnect.setText("Disconnect");
				} else {
					mAI.close();
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

	}
}
