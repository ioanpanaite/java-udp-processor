package secontrol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;





public class KeepServerRun implements Runnable {

	private UDPServer udpServer;
	private DBManager dbManager;
	
	public KeepServerRun( UDPServer _udpServer, DBManager _dbConnect ) {
		// TODO Auto-generated constructor stub
		
		this.udpServer = _udpServer;
		this.dbManager = _dbConnect;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while ( !Thread.currentThread().isInterrupted() ) {
			
			try {
								
				DBManager dbManager = getDBManager();
				UDPServer udpServer = getUDPServer();
				
				List<String> addressList = dbManager.getModemAddressList();
				List<String> portList = dbManager.getModemPortList();
				
				Iterator<String> address = addressList.iterator();
				Iterator<String> port = portList.iterator();
				
        		while ( address.hasNext() ) {

        			try {
						
						InetAddress addressInet = InetAddress.getByName( address.next().replace( "/", "").trim() );
						int portNum = Integer.parseInt( port.next() );
						System.out.println( "========Multi Thread Address: " + addressInet );
						System.out.println( "========Multi Thread Port: " + portNum );
						
						sendKeepDataToServer( udpServer, addressInet, portNum );
						
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
				
				Thread.sleep( 10 * 1000 );
					
				if ( !addressList.isEmpty() )
					addressList.clear();
				if ( !portList.isEmpty() )
					portList.clear();
				addressList = null;
				portList = null;
				address = null;
				port = null;
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println( "Stop Thread" );
				Thread.currentThread().interrupt();
				break;
			} catch (Exception exp) {
				System.out.println( "Exception: " + exp );
			}	
		}
	}

	/**
	 * Send the keep data for open the server socket.
	 * 
	 * @param _udpServer
	 * @param _addressInet
	 * @param _portNum
	 */
	private void sendKeepDataToServer( UDPServer _udpServer, InetAddress _addressInet, int _portNum ) {
		
		try {

			String sendDataHex = "~\n";
			byte[] sendData = sendDataHex.getBytes();
			
			_udpServer.sendPacketDataToModem( _addressInet, _portNum, sendData );
			System.out.println( "For keeping Server sent the Data to Server." );
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the DBManager instance.
	 * @return DBManager
	 */
	public DBManager getDBManager() {
		return dbManager;
	}
	
	/**
	 * Get the UDPServer instance
	 * @return UDPServer
	 */
	public UDPServer getUDPServer() {
		return udpServer;
	}
}
