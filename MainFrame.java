package secontrol;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.awt.event.ActionEvent;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Dimension;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JRadioButton;





public class MainFrame {

	private JFrame frame;
	private JTextField dbAddress;
	private JTextField dbUser;
	private JTextField dbPort;
	private JPasswordField dbPassword;
	private JTextField udpAddress;
	private JTextField udpPort;
	private JTextField database;
	private JEditorPane sendData;
	
	private JButton btnConnect;
	private JButton btnDisconnect;
	private JButton btnSend;
	
	private JRadioButton rdbtnAscii;
	private JRadioButton rdbtnHex;
	private JCheckBox chckbxAutoSendTo;
	private JComboBox<String> sendAddress;
	
	protected DBManager dbConnect;
	private Thread udpThread;
	private UDPServer udpServer;
	private UDPClient udpClient;
	
	private boolean isSend = false;
	private boolean isASCII = false;
	private boolean isHEX = false;
	
	private KeepServerRun keepServerInstance;
	private Thread keepServerThread;
	private JCheckBox keepServer;
	
	private static final String DEVICE_CHECKBOX_LABEL = "Device Info Getting";
	private static final String DEVICE_PERIOD_LABEL = "Cycle Period (seconds) : ";
	private static final String SOFTWARE_CHECKBOX_LABEL = "Software Info Getting";
	private static final String SOFTWARE_PERIOD_LABEL = "Cycle Period (seconds) : ";
	private static final String NVLINK_CHECKBOX_LABEL  = "NVLink Info Getting";
	private static final String NVLINK_PERIOD_LABEL = "Cycle Period (seconds) : ";
		
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame window = new MainFrame();
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
	public MainFrame() {
		
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		frame = new JFrame( "Secontrols UDP Processor" );
		frame.setResizable( false );
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Point centerPoint = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		int centerX = centerPoint.x - (626 / 2);
		int centerY = centerPoint.y - (595 / 2);
		frame.setBounds(centerX, centerY, 626, 595);
		try {
			
			File file = new File( "res/secontrol.png" );
			BufferedImage image = ImageIO.read( file );
	        BufferedImage resized = resize(image, 1500, 1500);
			frame.setIconImage( resized );
			resized = null;
			
		} catch ( IOException exp ) {
			// TODO Exception processing...
			exp.printStackTrace();
		}
		
		JPanel panelDBSetting = new JPanel();
		panelDBSetting.setBorder( new TitledBorder ( new EtchedBorder(), "DataBase Connection Settings" ) );

		
		JPanel panelUDPSetting = new JPanel();
		panelUDPSetting.setBorder( new TitledBorder( new EtchedBorder(), "UDP Server Connection Settings" ) );

		JPanel panelUDPSender = new JPanel();
		panelUDPSender.setBorder( new TitledBorder( new EtchedBorder(), "UDP Send" ) );
		
		/**
		 * Event processing when click the "Connect" Button. 
		 */
		btnConnect = new JButton("Connect");
		btnConnect.setBackground(Color.LIGHT_GRAY);
		btnConnect.setEnabled( true );
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				String dbAddressText = dbAddress.getText().trim();
				String dbUserText = dbUser.getText().trim();
				String dbPortText = dbPort.getText().trim();
				char[] dbPasswordValue = dbPassword.getPassword();
				String dbName = database.getText().trim();
								
				String udpAddressText = udpAddress.getText().trim();
				String udpPortText = udpPort.getText().trim();
				
				if ( dbAddressText.isEmpty() ) {					
					dbAddress.requestFocusInWindow();
					return;
				}
				if ( dbUserText.isEmpty() ) {					
					dbUser.requestFocusInWindow();
					return;
				}
				if ( dbPortText.isEmpty() ) {
					dbPort.requestFocusInWindow();
					return;
				}
				if ( dbName.isEmpty() ) {
					database.requestFocusInWindow();
					return;
				}
												
				if ( udpAddressText.isEmpty() ) {
					udpAddress.requestFocusInWindow();
					return;
				}
				if ( udpPortText.isEmpty() ) {
					udpPort.requestFocusInWindow();
					return;
				}
				
				try {
					// Connecting in the DB.	
					dbConnect = new DBManager( dbAddressText, dbUserText, dbPortText, dbPasswordValue, dbName );
					dbConnect.setTableInDB();
					
					if ( !dbConnect.getStatusALLSetting() )
						dbConnect.insertIncomingInTable( "ALL", 900, 900, 900, false, false, false, true);
										
					try {
						
						int port = Integer.parseInt( udpPortText );						
						udpServer = new UDPServer( port );						
						udpThread = new Thread( udpServer );
						udpThread.start();						
						udpServer.setDBSetting( dbConnect );

						btnConnect.setEnabled( false );
						btnDisconnect.setEnabled( true );
						
						dbAddress.setEnabled( false );
						dbUser.setEnabled( false );
						dbPort.setEnabled( false );
						dbPassword.setEnabled( false );
						database.setEnabled( false );
						udpAddress.setEnabled( false );
						udpPort.setEnabled( false );
						
						isSend = true;
						
						dbAddress.setDisabledTextColor( Color.BLACK );
						dbUser.setDisabledTextColor( Color.BLACK );
						dbPort.setDisabledTextColor( Color.BLACK );
						dbPassword.setDisabledTextColor( Color.BLACK );
						database.setDisabledTextColor( Color.BLACK );
						udpAddress.setDisabledTextColor( Color.BLACK );
						udpPort.setDisabledTextColor( Color.BLACK );	
												
						// Checking the checkbox status.
						if ( chckbxAutoSendTo.isSelected() ) {
							chckbxAutoSendTo.setSelected( false );
						}
						if ( keepServer.isSelected() ) {
							keepServer.setSelected( false );
						}
																		
					} catch (SocketException socketExp) {
						
						socketExp.printStackTrace();
						JOptionPane.showMessageDialog( null, "Please confirm the UDP connection setting value. " + socketExp, "UDP Server Connection Fault", 2 );
					}
				} catch ( ClassNotFoundException | SQLException exp ) {
					// TODO DB connection exception.
					System.out.println( "Exception: " + exp );
					JOptionPane.showMessageDialog( null, "Please confirm the DB setting value. " + exp, "DB Connection Fault", 2 );
				}
			}
		});

		/**
		 * Event processing when click the "Disconnect" Button. 
		 */
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setBackground(Color.LIGHT_GRAY);
		btnDisconnect.setEnabled( false );
		btnDisconnect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				
				if ( udpThread != null && udpServer != null ) {
					
					// Server service stop.
					udpServer.stopService();
					udpThread.interrupt();
					
					// Thread stop.
					if ( udpClient != null ) {

						udpClient.stopClient();
						chckbxAutoSendTo.setSelected(false);	
					}
					if ( keepServerThread != null ) {

						keepServerThread.interrupt();
						keepServer.setSelected( false );
					}
					
					// Remote field initialize.
					sendAddress.removeAllItems();
					
					// Button status control.
					btnConnect.setEnabled( true );
					btnDisconnect.setEnabled( false );
					
					dbAddress.setEnabled( true );
					dbUser.setEnabled( true );
					dbPort.setEnabled( true );
					dbPassword.setEnabled( true );
					database.setEnabled( true );
					udpAddress.setEnabled( true );
					udpPort.setEnabled( true );
					
					isSend = false;

					System.out.print( "All Service Stopped.\n" );

				}
			}
		});
		
		btnSend = new JButton("Send");
		btnSend.addActionListener(new ActionListener() {
			
			public void actionPerformed( ActionEvent actionEvent ) {

				System.out.println( "Send: " + rdbtnAscii.isSelected() );
				System.out.println( "Send: " + rdbtnHex.isSelected() );
				System.out.println( "Send: " + chckbxAutoSendTo.isSelected() );
				
				// Connection checking...
				if ( isSend ) {

					String sendDataValue = sendData.getText();
					String dataType = rdbtnAscii.isSelected() ? "ASCII" : rdbtnHex.isSelected() ? "HEX" : "";
					
					System.out.println( "SendData: " + sendDataValue );
					
					if ( sendDataValue.isEmpty() ) {
						
						JOptionPane.showMessageDialog( null, "No value. Input the value you want to send.", "Value Empty", 2 );
						sendData.requestFocusInWindow();
						return;
					}
					
					// Send the data individually to server.
					if ( sendAddress.getSelectedItem() == null ) {
						
						JOptionPane.showMessageDialog( null, "Input the address you want to send.", "Address Empty", 2 );
						sendAddress.requestFocusInWindow();
						return;
					} 

					try {
						
						String send_address = sendAddress.getSelectedItem().toString();
						String[] address_port = send_address.split( ":" );
						if ( address_port.length != 2 ) {

							JOptionPane.showMessageDialog( null, "Enter the correct Address and Port.", "Address Fault", 2 );
							return;
						}
							
						InetAddress address = address_port[0] != null ? InetAddress.getByName( address_port[0].trim() ) : null;
						int sendPort = address_port[1] != null ? Integer.parseInt( address_port[1].trim() ) : 0;

						UDPClient udpClient = new UDPClient();
						udpClient.setUDPServer(udpServer);
						udpClient.sendDataToServer( address, sendPort, sendDataValue, dataType );
						udpClient = null;
						
					} catch (UnknownHostException e) {
						// Exception processing.
						System.out.println( "Exception: " + e );
						ReferenceHandle.writeReportFile( "MainFrame actionPerformed UnknownHostException", e.getLocalizedMessage() );
					}
					
				} else {
					
					JOptionPane.showMessageDialog( null, "Send fail. no connection available.", "Send Error", 2 );
				}
			}
		});
		btnSend.setBackground( Color.LIGHT_GRAY );
				
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(37)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(panelUDPSender, GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)
						.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
							.addComponent(panelUDPSetting, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(panelDBSetting, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)))
					.addGap(6)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
							.addComponent(btnConnect, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(btnDisconnect, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addComponent(btnSend, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(26)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(panelDBSetting, GroupLayout.PREFERRED_SIZE, 184, GroupLayout.PREFERRED_SIZE)
							.addGap(18)
							.addComponent(panelUDPSetting, GroupLayout.PREFERRED_SIZE, 125, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(8)
							.addComponent(btnConnect)
							.addGap(18)
							.addComponent(btnDisconnect)))
					.addPreferredGap(ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(panelUDPSender, GroupLayout.PREFERRED_SIZE, 162, GroupLayout.PREFERRED_SIZE)
							.addGap(24))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(btnSend, GroupLayout.PREFERRED_SIZE, 53, GroupLayout.PREFERRED_SIZE)
							.addGap(37))))
		);
		panelUDPSender.setLayout(null);
		
		sendData = new JEditorPane();
		sendData.addInputMethodListener(null);
		sendData.setBounds(10, 92, 439, 59);
		sendData.setBorder( BorderFactory.createLineBorder( Color.LIGHT_GRAY ));
		sendData.requestFocusInWindow();
		sendData.setDisabledTextColor( Color.GRAY );
		panelUDPSender.add(sendData);
		
		rdbtnAscii = new JRadioButton("ASCII");
		rdbtnAscii.setSelected( true );
		rdbtnAscii.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
								
				rdbtnAscii.setSelected( true );
				rdbtnHex.setSelected( false );
				isHEX = false;

				if ( !isASCII ) {

					String sendDataValue = sendData.getText();
					if ( !sendDataValue.isEmpty() ) {
						
						try {

							boolean isHex = ReferenceHandle.isHexValue( sendDataValue.trim() );
							System.out.println( "=========ASCII check: " + sendDataValue.trim() );
							
							if ( isHex ) {

								byte[] bytes = ReferenceHandle.hexStringToByteArray( sendDataValue );
								String result= new String( bytes, "UTF-8" );
								sendData.setText( result );
								
								result = null;
								System.out.println( "ASCII Value: " + result );
							}
							
						} catch (UnsupportedEncodingException e) {
							// TODO Encoding Exception processing...
							e.printStackTrace();
							ReferenceHandle.writeReportFile( "MainFrame rdbtnAscii UnsupportedEncodingException", e.getLocalizedMessage() );
						}
					}
					isASCII = true;
				}
			}
		});
		rdbtnAscii.setBounds(10, 19, 56, 23);
		panelUDPSender.add(rdbtnAscii);
		
		rdbtnHex = new JRadioButton("HEX");
		rdbtnHex.setSelected( false );
		rdbtnHex.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				rdbtnHex.setSelected( true );
				rdbtnAscii.setSelected( false );
				isASCII = false;
				
				if ( !isHEX ) {

					String sendDataValue = sendData.getText();
					boolean isHex = ReferenceHandle.isHexValue( sendDataValue.trim() );
										
					if ( !isHex ) {

						if ( !sendDataValue.isEmpty() ) {

							String hexValue = ReferenceHandle.toHex( sendDataValue );
							sendData.setText( hexValue );	
							System.out.println( "HEX Value: " + hexValue );
						}
						isHEX = true;	
					}
				}
			}
		});
		rdbtnHex.setBounds(10, 45, 56, 23);
		panelUDPSender.add(rdbtnHex);
		
		JLabel lblRemote = new JLabel("Remote:");
		lblRemote.setBounds(104, 39, 51, 20);
		panelUDPSender.add(lblRemote);

		sendAddress = new JComboBox<String>();
		sendAddress.setBounds(158, 39, 148, 23);
		sendAddress.setBackground( Color.white );
		sendAddress.setEditable(true);
		((JTextField)sendAddress.getEditor().getEditorComponent()).setDisabledTextColor( Color.black );
		panelUDPSender.add( sendAddress );
		
		chckbxAutoSendTo = new JCheckBox("Send to all site");
		chckbxAutoSendTo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if ( chckbxAutoSendTo.isSelected() ) {

					if ( isSend )
						this.setAllSiteSettingWindow();
					
				} else {

					if ( udpClient == null ) throw new NullPointerException( "UDPClient stop error" );
					udpClient.stopClient();
				}
				System.out.println( "Auto ALL chckbxAutoSendTo: " + chckbxAutoSendTo.isSelected() );
			}

			// Creating the window for setting the period in the All site.
			private void setAllSiteSettingWindow() {

		        JPanel panel = new JPanel( new GridLayout( 3, 3, 30, 2 ));
		        panel.setPreferredSize( new Dimension( 600, 90 ));
		        
		        JTextField devicePeriodValue = null;
		        JTextField softPeriodValue = null;
		        JTextField nvlinkPeriodValue = null;
		        
		        JCheckBox device_box = null;
				JCheckBox soft_box = null;
				JCheckBox nvlink_box = null;

        		try {

        			HashMap<String, String> res = dbConnect.getAllDataFromIncomingTable();
					if ( !res.isEmpty() ) {
							
						// Checkbox for getting the activation status.
						device_box = new JCheckBox( DEVICE_CHECKBOX_LABEL );
						soft_box = new JCheckBox( SOFTWARE_CHECKBOX_LABEL );
						nvlink_box = new JCheckBox( NVLINK_CHECKBOX_LABEL );
						
						boolean device_info = res.get( "device_info" ).equals("1") ? true : false;
						boolean software_info = res.get( "software_info" ).equals("1") ? true : false;
						boolean nvlink_info = res.get( "nvlink_info" ).equals("1") ? true : false;
						
						device_box.setSelected( device_info );
						soft_box.setSelected( software_info );
						nvlink_box.setSelected( nvlink_info );
				        
						// Set the labels.
						JLabel devicePeriodLable = new JLabel( DEVICE_PERIOD_LABEL );
						JLabel softPeriodLabel = new JLabel( SOFTWARE_PERIOD_LABEL );
				        JLabel nvlinkLabel = new JLabel( NVLINK_PERIOD_LABEL );
				        
				        // Set the fields for getting the period value.
				        devicePeriodValue = new JTextField( res.get( "device_period" ) );
				        softPeriodValue = new JTextField( res.get( "software_period" ) );
				        nvlinkPeriodValue = new JTextField( res.get( "nvlink_period" ) );
				        
						panel.add( device_box );
						panel.add( devicePeriodLable );
						panel.add( devicePeriodValue );
						
						panel.add( soft_box );
						panel.add( softPeriodLabel );
						panel.add( softPeriodValue );

						panel.add( nvlink_box );
						panel.add( nvlinkLabel );
						panel.add( nvlinkPeriodValue );
					}
					res.clear();
					res = null;

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					ReferenceHandle.writeReportFile( "MainFrame setAllSiteSettingWindow SQLException", e.getLocalizedMessage() );
				}					
		        
				int result = JOptionPane.showConfirmDialog( null, panel, "All site setting", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE );							
				if ( result == JOptionPane.OK_OPTION ) {
					
					if ( devicePeriodValue != null && softPeriodValue != null && nvlinkPeriodValue != null ) {

						if ( (devicePeriodValue.getText().isEmpty() || !ReferenceHandle.isInteger( devicePeriodValue.getText() )) ||
								(softPeriodValue.getText().isEmpty() || !ReferenceHandle.isInteger( softPeriodValue.getText() )) || 
								(nvlinkPeriodValue.getText().isEmpty() || !ReferenceHandle.isInteger( nvlinkPeriodValue.getText() )) ) {
							
							JOptionPane.showMessageDialog( null, "Input the correct value", "Incorrect value", 2 );
							chckbxAutoSendTo.setSelected( false );
							return;
						}
						
						int devicePeriod = Integer.parseInt( devicePeriodValue.getText() );
						int softPeriodVal = Integer.parseInt( softPeriodValue.getText() );
						int nvlinkPeriodVal = Integer.parseInt( nvlinkPeriodValue.getText() );
												
						boolean deviceInfo = device_box.isSelected() ? true : false;
						boolean softwareInfo = soft_box.isSelected() ? true : false;
						boolean nvlinkInfo = nvlink_box.isSelected() ? true : false;
						
						dbConnect.insertIncomingInTable( "ALL", devicePeriod, softPeriodVal, nvlinkPeriodVal, deviceInfo, softwareInfo, nvlinkInfo, true );
												
						udpClient = new UDPClient();
						udpClient.setUDPServer(udpServer);
						udpClient.setDBConnect(dbConnect);
						udpClient.startClient();
					}	
					
				} else {
					
					System.out.println( "Cancel" );
					chckbxAutoSendTo.setSelected( false );
					sendAddress.setEnabled( true );
				}
				
			}
		});
		chckbxAutoSendTo.setBounds(327, 45, 122, 23);
		panelUDPSender.add(chckbxAutoSendTo);
		
		keepServer = new JCheckBox("Keep Server");
		keepServer.setBounds(327, 19, 122, 23);
		panelUDPSender.add(keepServer);
		keepServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				if ( isSend ) {
					
					
					if ( keepServer.isSelected() ) {
						
						keepServerInstance = new KeepServerRun( udpServer, dbConnect );
						keepServerThread = new Thread( keepServerInstance );
						keepServerThread.setPriority(1);
						keepServerThread.setDaemon( true );
						keepServerThread.start();
						
					} else {

						if ( keepServerThread != null ) 
							keepServerThread.interrupt();
					}
				}
			}
		});
		panelUDPSetting.setLayout(null);


		
		JLabel lblUdpHostAddresss = new JLabel("UDP host address");
		lblUdpHostAddresss.setBounds(35, 36, 135, 14);
		panelUDPSetting.add(lblUdpHostAddresss);
		
		udpAddress = new JTextField();
		udpAddress.setBounds(196, 33, 213, 20);
		panelUDPSetting.add(udpAddress);
		udpAddress.setColumns(10);
		
		JLabel lblPort_1 = new JLabel("UDP Port");
		lblPort_1.setBounds(35, 61, 86, 14);
		panelUDPSetting.add(lblPort_1);
		
		udpPort = new JTextField();
		udpPort.setBounds(196, 64, 86, 20);
		panelUDPSetting.add(udpPort);
		udpPort.setColumns(10);
		panelDBSetting.setLayout(null);
		
		JLabel lblHostAddresss = new JLabel("MySQL host address");
		lblHostAddresss.setBounds(35, 30, 150, 26);
		panelDBSetting.add(lblHostAddresss);
		
		dbAddress = new JTextField();
		dbAddress.setBounds(195, 33, 217, 20);
		panelDBSetting.add(dbAddress);
		dbAddress.setColumns(10);
		
		JLabel lblPort = new JLabel("Port");
		lblPort.setBounds(35, 126, 46, 14);
		panelDBSetting.add(lblPort);
		
		dbUser = new JTextField();
		dbUser.setBounds(195, 64, 217, 20);
		panelDBSetting.add(dbUser);
		dbUser.setColumns(10);
		
		JLabel lblNewLabel = new JLabel("Username");
		lblNewLabel.setBounds(35, 67, 92, 20);
		panelDBSetting.add(lblNewLabel);
		
		dbPort = new JTextField();
		dbPort.setBounds(195, 123, 86, 20);
		panelDBSetting.add(dbPort);
		dbPort.setColumns(10);
		
		JLabel lblPassword = new JLabel("Password");
		lblPassword.setBounds(35, 98, 92, 20);
		panelDBSetting.add(lblPassword);
		
		dbPassword = new JPasswordField();
		dbPassword.setForeground(Color.BLACK);
		dbPassword.setBounds(195, 95, 86, 20);
		panelDBSetting.add(dbPassword);
		
		database = new JTextField();
		database.setBounds(195, 151, 86, 20);
		panelDBSetting.add(database);
		database.setColumns(10);
		
		JLabel lblDatabase = new JLabel("Database");
		lblDatabase.setBounds(35, 154, 92, 14);
		panelDBSetting.add(lblDatabase);
		frame.getContentPane().setLayout(groupLayout);		
	}

	/**
	 * Resizing the icon.
	 * 
	 * @param img
	 * @param height
	 * @param width
	 * @return Object
	 */
	private static BufferedImage resize( BufferedImage img, int height, int width ) {
		
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        
        return resized;
    }
}
