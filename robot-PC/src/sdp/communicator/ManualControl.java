package sdp.communicator;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JButton;

import sdp.common.Communicator;
import sdp.common.MessageListener;
import sdp.common.Communicator.opcode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 
 * This is the class that gives easy GUI way of sending raw commands to the brick.
 * 
 * @author s0932707
 *
 */
public class ManualControl {

	private JFrame frmManualNxtCommand;
	private JTextField textField;
	private JButton btnConnect;
	private Communicator mComm;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ManualControl window = new ManualControl();
					window.frmManualNxtCommand.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ManualControl() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmManualNxtCommand = new JFrame();
		frmManualNxtCommand.getContentPane().setFocusable(true);
		final JButton btn_W = new JButton("FORW. (W)");
		btn_W.setEnabled(false);
		btn_W.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("W command not set yet");
			}
		});
		btn_W.setBounds(166, 84, 117, 25);
		frmManualNxtCommand.getContentPane().add(btn_W);
		
		final JButton btn_Space = new JButton("STOP (SPACE)");
		btn_Space.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("SPACE command not set yet");
			}
		});
		btn_Space.setEnabled(false);
		btn_Space.setBounds(295, 84, 141, 25);
		frmManualNxtCommand.getContentPane().add(btn_Space);
		
		final JButton btn_S = new JButton("BACK (S)");
		btn_S.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("S command not set yet");
			}
		});
		btn_S.setEnabled(false);
		btn_S.setBounds(166, 118, 117, 25);
		frmManualNxtCommand.getContentPane().add(btn_S);
		
		final JButton btn_A = new JButton("LEFT (A)");
		btn_A.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("A command not set yet");
			}
		});
		btn_A.setEnabled(false);
		btn_A.setBounds(33, 118, 117, 25);
		frmManualNxtCommand.getContentPane().add(btn_A);
		
		final JButton btn_D = new JButton("RIGHT (D)");
		btn_D.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("D command not set yet");
			}
		});
		btn_D.setEnabled(false);
		btn_D.setBounds(295, 118, 117, 25);
		
		final JButton btn_control_on = new JButton("Joypad ON");
		btn_control_on.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				frmManualNxtCommand.getContentPane().requestFocus();
			}
		});
		btn_control_on.setBounds(33, 84, 117, 25);
		
		frmManualNxtCommand.getContentPane().addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				btn_W.setEnabled(false);
				btn_A.setEnabled(false);
				btn_S.setEnabled(false);
				btn_D.setEnabled(false);
				btn_Space.setEnabled(false);
				btn_control_on.setEnabled(true);
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
				btn_W.setEnabled(true);
				btn_A.setEnabled(true);
				btn_S.setEnabled(true);
				btn_D.setEnabled(true);
				btn_Space.setEnabled(true);
				btn_control_on.setEnabled(false);
			}
		});
		
		frmManualNxtCommand.getContentPane().add(btn_D);
		frmManualNxtCommand.getContentPane().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				switch (arg0.getKeyCode()) {
				case KeyEvent.VK_W: 
					btn_W.doClick();
					break;
				case KeyEvent.VK_A: 
					btn_A.doClick();
					break;
				case KeyEvent.VK_S: 
					btn_S.doClick();
					break;
				case KeyEvent.VK_D: 
					btn_D.doClick();
					break;
				case KeyEvent.VK_SPACE: 
					btn_Space.doClick();
					break;
				}
			}
		});
		frmManualNxtCommand.setTitle("Manual NXT Command Sender");
		frmManualNxtCommand.setBounds(100, 100, 450, 188);
		frmManualNxtCommand.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmManualNxtCommand.getContentPane().setLayout(null);
		
		final JComboBox comboBox = new JComboBox();
		final opcode[] ops = opcode.values();
		for (int i = 0; i < ops.length; i++)
			comboBox.addItem(ops[i]);
		comboBox.setBounds(12, 9, 80, 24);
		frmManualNxtCommand.getContentPane().add(comboBox);
		
		textField = new JTextField();
		textField.setBounds(104, 12, 332, 19);
		frmManualNxtCommand.getContentPane().add(textField);
		textField.setColumns(10);
		

		
		final JButton btnNewButton = new JButton("Send");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				opcode op = ops[comboBox.getSelectedIndex()];
				if (textField.getText().trim().length() == 0) {
					try {
					mComm.sendMessage(op);
					} catch (Exception e) {
						System.out.println("Can't send message");
					} finally {
						System.out.println("Message "+op+" sent with NO arguments");
					}
				}
				String[] sargs = textField.getText().split(",");
				byte[] args = new byte[sargs.length];
				for (int i = 0; i < args.length; i++) {
					try {
						args[i] = (byte) (int) Integer.parseInt(sargs[i].trim());
					} catch (Exception e) {
						System.out.println("Error sending message. Cannot parse argument '"+sargs[i].trim()+"'");
						return;
					}
				}
				try {
				mComm.sendMessage(op, args);
				} catch (Exception e) {
					System.out.println("Can't send message");
				} finally {
					System.out.println("Message "+op+" sent with "+args.length+" arguments");
				}
			}
		});
		btnNewButton.setEnabled(false);
		btnNewButton.setBounds(12, 38, 117, 25);
		frmManualNxtCommand.getContentPane().add(btnNewButton);
		
		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnConnect.setText("Wait...");
				btnConnect.setEnabled(false);
				mComm = new JComm(new MessageListener() {
					
					@Override
					public void receiveMessage(opcode op, byte[] args, Communicator controler) {
						System.out.println("New message "+op+" from BRICK");
						
					}
				});
				btnConnect.setText("Ready!");
				btnNewButton.setEnabled(true);
			}
		});
		btnConnect.setBounds(319, 38, 117, 25);
		frmManualNxtCommand.getContentPane().add(btnConnect);
		
		frmManualNxtCommand.getContentPane().add(btn_control_on);
		

	}
}
