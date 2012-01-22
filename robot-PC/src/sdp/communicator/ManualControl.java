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
		frmManualNxtCommand.setTitle("Manual NXT Command Sender");
		frmManualNxtCommand.setBounds(100, 100, 450, 103);
		frmManualNxtCommand.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmManualNxtCommand.getContentPane().setLayout(null);
		
		textField = new JTextField();
		textField.setBounds(104, 12, 332, 19);
		frmManualNxtCommand.getContentPane().add(textField);
		textField.setColumns(10);
		
		final JComboBox comboBox = new JComboBox();
		final opcode[] ops = opcode.values();
		for (int i = 0; i < ops.length; i++)
			comboBox.addItem(ops[i]);
		comboBox.setBounds(12, 9, 80, 24);
		frmManualNxtCommand.getContentPane().add(comboBox);
		
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
	}
}
