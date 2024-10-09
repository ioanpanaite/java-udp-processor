package secontrol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.HashMap;

public class ProxyProcess extends Thread {

	public DBManager dbManager;
	private int port = 0;
	private boolean isRunning;
	private DatagramSocket proxySocket;
	private UDPServer udpServer;

	public ProxyProcess(int internalPort) throws SocketException {
		
		this.port = internalPort;
		this.proxySocket = new DatagramSocket(port);
	}

	@Override
	public void run() {
		
		this.isRunning = true;
		while ( this.isRunning ) {

            try {
				// Set the byte array size.
				byte[] in = new byte[1024];
	            
	            DatagramPacket receivedPacket = new DatagramPacket(in, in.length);
	            DatagramSocket serverSocket = getServerSocket();
				serverSocket.receive(receivedPacket);
	            
				byte[] out = new byte[receivedPacket.getLength()];
	            System.arraycopy( receivedPacket.getData(), receivedPacket.getOffset(), out, 0, receivedPacket.getLength() );
		            
	            int portNum = getPort();
	            sendReplyDataToModem( portNum, out );
	            
			} catch (SocketException exp) {				
				ReferenceHandle.writeReportFile( "ProxyServer Stop", "" );
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ReferenceHandle.writeReportFile( "ProxyServer run() Exception", e.getLocalizedMessage() );
			}
		}
	}
	
	/**
	 * Send the Data from Other Server to Modem.  
	 * 
	 * @param _portNum
	 * @param _replyData
	 */
	public void sendReplyDataToModem( int _portNum, byte[] _replyData ) {

    	try {
    		
    		DBManager dbManager = getDBManager();        	
	    	String sendData = ReferenceHandle.bytesToHexString( _replyData );
	    			    	
    		// Get the Modem's IP and Port by mapped IP and port.	    	
			HashMap<String, String> res = dbManager.getIPandPortByInternalPort( _portNum );
				
			if ( res.isEmpty() ) throw new NullPointerException("UDPServer HashMap Empty Throw Port: " + _portNum); 
			String ipAddress = res.get( "address" );
			int port = Integer.parseInt( res.get( "port" ) );
						
			// Send the Other Server's data to Modem. 
			if ( !ipAddress.isEmpty() && port != 0 ) {

				InetAddress sendIP = InetAddress.getByName( ipAddress.replace( "/", "" ) );

	            UDPServer udpServer = getUDPServer();
	            udpServer.sendPacketDataToModem( sendIP, port, _replyData );
				String report = "ProxyProcess sendReplyDataToModem -> Sent the Reply Data to the Modem IP: " + sendIP + " Port: " + port;
				ReferenceHandle.writeReportFile( report, sendData );
			}
			
			res.clear();
			res = null;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	    	ReferenceHandle.writeReportFile( "ProxyProcess sendReplyDataToModem() SQLException", e.getLocalizedMessage() );	
		} catch (Exception e) {
			e.printStackTrace();
	    	ReferenceHandle.writeReportFile( "ProxyProcess sendReplyDataToModem() Exception", e.getLocalizedMessage() );	
		}
	}
	
	/**
	 * Set the DBManager instance.
	 * @param _dbManager
	 */
	public void setDBSetting(DBManager _dbManager) {
		this.dbManager = _dbManager;
	}
	
	/**
	 * Get the DBManager instance.
	 * @return
	 */
	public DBManager getDBManager() {
		return this.dbManager;
	}

	/**
	 * Get the internal port for sending the UDP data to the Other Server.
	 * @return
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Stop the Proxy Socket Service.
	 */
	public void stopProxyProcess() {
		// TODO Auto-generated method stub
		DatagramSocket serverSocektStop = getServerSocket();
		serverSocektStop.close();
		this.isRunning = false;
	}

	/**
	 * Set the UDPServer instance
	 * @param _udpServer
	 */
	public void setUDPServer(UDPServer _udpServer) {
		// TODO Auto-generated method stub
		this.udpServer = _udpServer;
	}
	
	/**
	 * Get the UDPServer instance
	 * @return
	 */
	public UDPServer getUDPServer() {
		return this.udpServer;
	}

	/**
	 * Get the DatagramSocket ServerSocket instance
	 * @return
	 */
	public DatagramSocket getServerSocket() {		
		return this.proxySocket;
	}

	/**
	 * Send the UDP raw data to the Other server
	 * @param sendPacket
	 * @throws IOException
	 */
	public void sendUDPDataToOtherServer(DatagramPacket sendPacket) throws IOException {

		DatagramSocket proxyServer = getServerSocket();
		proxyServer.send(sendPacket);
	}
	
}
