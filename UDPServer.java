package secontrol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import java.net.DatagramSocket;
import java.net.InetAddress;





public class UDPServer implements Runnable {
	
	public static final int ZERO = 0;
	private static final long EXPIRED_TIME_IN_SEC = 15l;
	private int PORT = 5555;
	private Map<Integer, ProxyProcess> serverMap = new ConcurrentHashMap<>();
		
	private DatagramSocket serverSocket;
	private boolean isRunning;

    private byte[] in;
    private byte[] out;
    
    private DBManager dbManager;
	private JButton btnDisonnect;
	private ExecutorService internalPool = Executors.newWorkStealingPool(500);
	private final RejectedExecutionHandlerImplement rejectedHandler;
	private final ThreadFactory threadFactory;
	private final ThreadPoolExecutor poolExecutor;
    

	/**
     * Our constructor which instantiates our serverSocket
     */
    public UDPServer( int udpPort ) throws SocketException {
    	
    	this.PORT = udpPort;
        serverSocket = new DatagramSocket(PORT);        
        
        threadFactory = Executors.defaultThreadFactory();
        rejectedHandler = new RejectedExecutionHandlerImplement(); 
        poolExecutor = new ThreadPoolExecutor(10, 50, 5, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(50), threadFactory, rejectedHandler);
    }
    

	@Override
	public void run() {
		// Thread Starting...
		this.isRunning = true;
		while ( this.isRunning ) {

			try {
				
				// Set the byte array size.
				in = new byte[1024];
                
                /**
                 * Create our inbounds datagram packet
                 */
                DatagramPacket receivedPacket = new DatagramPacket(in, in.length);
                serverSocket.receive(receivedPacket);


                /**
                 * Get the data from the packet we've just received
                 */  
                out = new byte[receivedPacket.getLength()];
                System.arraycopy( receivedPacket.getData(), receivedPacket.getOffset(), out, 0, receivedPacket.getLength() );

                /**
                 * Retrieve the IP Address and port number of the datagram packet
                 * we've just received
                 */
                InetAddress IPAddress = receivedPacket.getAddress();
                int portNum = receivedPacket.getPort();
                                              
                // Incoming UDP data analyze and save in DB.
            	if ( out.length > 0 ) {
            		
            		

            		if ( Integer.parseInt( Integer.toHexString( ( 0xff & out[0] ) ), 16 ) == 0x53 ) {
            			
            			// Send Raw Data to OTHER Server.		            		
	        			String siteIDHex = Integer.toHexString( 0xff & out[9] ) + ReferenceHandle.toHexCustom( out[8] );
	        		    String siteID = String.valueOf( Integer.parseInt( siteIDHex, 16 ) );
	        			String nvLinkIDHex = Integer.toHexString( 0xff & out[7] ) + ReferenceHandle.toHexCustom( out[6] );
	        			String nvLinkID = String.valueOf( Integer.parseInt( nvLinkIDHex, 16 ) );
            			this.sendRawDataToOtherServer( siteID, nvLinkID, receivedPacket );
        				        		    	
        				// Process the UDP data in background. 
        				BackgroundProcess backgroundProcess = new BackgroundProcess( IPAddress, portNum );
        				backgroundProcess.setUDPData(out);
        				backgroundProcess.setDBSetting(this.dbManager);
        				backgroundProcess.setPriority(8);  
        				poolExecutor.execute( backgroundProcess );
            			                		
        			} else {
        				
        				// Send the Raw data for the sceptre. 
        				this.sendRawDataToOtherServer( IPAddress, portNum, receivedPacket );
        				
        				int localPort = serverSocket.getLocalPort();	
                    	String report = "UDPServer Modem to Reply data Local Port: " + localPort + " Incoming IP:"+ IPAddress +" Incoming Port:" + portNum;
                    	ReferenceHandle.writeReportFile( report, ReferenceHandle.bytesToHexString( out ) );	
                    	// Send the data that is from OTHER Server to Modem.
                        this.sendReplyDataToModem( localPort, out );
        			}
            		
            	}
                
                in = null;
                out = null;
        	            	    
			} catch (SocketException exp) {
				
				if ( poolExecutor != null )
					poolExecutor.shutdownNow();
				if ( internalPool != null ) 
					internalPool.shutdownNow();
				ReferenceHandle.writeReportFile( "UDPServer Stop", "" );
				break;
			} catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
				ReferenceHandle.writeReportFile( "UDPServer run() IOException", e.getLocalizedMessage() );
			} catch (NullPointerException e) {
				ReferenceHandle.writeReportFile( "UDPServer run() NullPointerException", e.getLocalizedMessage() );
			} catch (Exception e) {
				ReferenceHandle.writeReportFile( "UDPServer run() Exception", e.getLocalizedMessage() );				
			}
        }
	}

	
	/**
	 * Send the command of server to the Modem.
	 * @param _localPort
	 * @param _replyData
	 */
	private void sendReplyDataToModem(int _localPort, byte[] _replyData) {

		try {
    			    			    	
    		// Get the Modem's IP and Port by mapped IP and port.	    	
			DBManager dbManager = getDBManager();        	
			HashMap<String, String> res = dbManager.getIPandPortByInternalPort( _localPort );
			
			if ( !res.isEmpty() ) {

		    	String sendData = ReferenceHandle.bytesToHexString( _replyData );
				String ipAddress = res.get( "address" );
				int port = Integer.parseInt( res.get( "port" ) );
							
				// Send the Other Server's data to Modem. 
				if ( !ipAddress.isEmpty() && port != 0 ) {

					InetAddress sendIP = InetAddress.getByName( ipAddress.replace( "/", "" ) );
		            
		            this.sendPacketDataToModem( sendIP, port, _replyData );
					String report = "UDPServer sendReplyDataToModem -> Sent the Reply Data to the Modem IP: " + sendIP + " Port: " + port;
					ReferenceHandle.writeReportFile( report, sendData );
				}
				
				res.clear();
				res = null;
			}
				
		} catch (Exception e) {
			e.printStackTrace();
	    	ReferenceHandle.writeReportFile( "UDPServer sendReplyDataToModem() Exception", e.getLocalizedMessage() );	
		}
	}


	/**
	 * Send the raw data to Other Server with first time.
	 * @param _IPAddress
	 * @param _portNum
	 * @param receivedPacket
	 */
	private void sendRawDataToOtherServer(InetAddress _IPAddress, int _portNum, DatagramPacket receivedPacket) {

		try {
			
			// Get the mapped address and port by Site ID from DB.				
			DBManager _dbManager = getDBManager();
			List<String> getInfo = _dbManager.getMapInfoInternalPortByIpPort( _IPAddress.toString(), _portNum );
			
			if ( !getInfo.isEmpty() ) {
				
				String addressName = getInfo.get(0);
				int portNum = Integer.parseInt(getInfo.get(1));
				int internalPort = Integer.parseInt(getInfo.get(2));
				
				if ( addressName != null && portNum != 0 ) {
					
					// Get the address from String address.
					InetAddress address = InetAddress.getByName( addressName );
	
					// Control when the internal port is ZERO.
					if ( internalPort == ZERO ) {
						
						JOptionPane.showMessageDialog( null, "Internal Port: " + internalPort + ". Please set the Internal Port.", "Internal Port Fault", JOptionPane.WARNING_MESSAGE );					
						if ( btnDisonnect != null) {
							
							btnDisonnect.doClick();
							serverSocket.setReuseAddress( true );
							
						} else System.exit(0);
					}
					
					// Get the ProxyProcess instance existing status in serverMap.
					boolean existStatus = getProxyProcessExistStatus( internalPort );			
					
					if ( !existStatus ) {
						
						ProxyProcess internalServer = serverMap.get( internalPort );
						if ( internalServer == null ) {
							
							internalServer = new ProxyProcess( internalPort );
							internalServer.setDBSetting( _dbManager );
							internalServer.setUDPServer( this );
							internalServer.setPriority(10);
						}
						
						if ( internalServer != null ) {
							
							serverMap.put( internalPort, internalServer );
							internalPool.submit( internalServer ); 
						}
					}
					
					// Send data to mapped server.
					DatagramPacket sendPacket = new DatagramPacket( receivedPacket.getData(), receivedPacket.getLength(), address, portNum );
					ProxyProcess proxyServer = serverMap.get( internalPort );
					proxyServer.sendUDPDataToOtherServer( sendPacket );
					ReferenceHandle.writeReportFile( "UDPServer with ProxyProcess sendRawDataToOtherServer 1 -> Sent the UDP data to the Other Server{IP: "+ address +" Port: "+ portNum +" }", ReferenceHandle.bytesToHexString(receivedPacket.getData()) );
				}
					
				getInfo.clear();
				getInfo = null;
			}
			
		} catch (IOException e) {
			// TODO Exception processing..
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer First sendRawDataToOtherServer() 1 IOException", e.getLocalizedMessage() );
		} catch (NullPointerException e) {
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer First sendRawDataToOtherServer() 1 NullPointerException", e.getLocalizedMessage() );
		} catch (Exception e) {
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer First sendRawDataToOtherServer() 1 Exception", e.getLocalizedMessage() );
		}		
	}
	
	/**
	 * Send the raw data to Other Server with first time.
	 * @param _siteID
	 * @param _nvLinkID
	 * @param _udpData
	 */
	private void sendRawDataToOtherServer(String _siteID, String _nvLinkID, DatagramPacket receivedPacket) {

		try {
			
			// Get the mapped address and port by Site ID from DB.				
			DBManager _dbManager = getDBManager();
			List<String> getInfo = _dbManager.getMapInfoInternalPortBySiteID( _siteID, _nvLinkID );
			
			if ( !getInfo.isEmpty() ) {
				
				String addressName = getInfo.get(0);
				int portNum = Integer.parseInt(getInfo.get(1));
				int internalPort = Integer.parseInt(getInfo.get(2));
				
				if ( addressName != null && portNum != 0 ) {
					
					// Get the address from String address.
					InetAddress address = InetAddress.getByName( addressName );
	
					// Control when the internal port is ZERO.
					if ( internalPort == ZERO ) {
						
						JOptionPane.showMessageDialog( null, "Internal Port: " + internalPort + ". Please set the Internal Port.", "Internal Port Fault", JOptionPane.WARNING_MESSAGE );					
						if ( btnDisonnect != null) {
							
							btnDisonnect.doClick();
							serverSocket.setReuseAddress( true );
							
						} else System.exit(0);
					}
					
					// Get the ProxyProcess instance existing status in serverMap.
					boolean existStatus = getProxyProcessExistStatus( internalPort );			
					
					if ( !existStatus ) {
						
						ProxyProcess internalServer = serverMap.get( internalPort );
						if ( internalServer == null ) {
							
							internalServer = new ProxyProcess( internalPort );
							internalServer.setDBSetting( _dbManager );
							internalServer.setUDPServer( this );
							internalServer.setPriority(10);
						}
						
						if ( internalServer != null ) {
							
							serverMap.put( internalPort, internalServer );
							internalPool.submit( internalServer ); 
						}
					}
					
					// Send data to mapped server.
					DatagramPacket sendPacket = new DatagramPacket( receivedPacket.getData(), receivedPacket.getLength(), address, portNum );
					ProxyProcess proxyServer = serverMap.get( internalPort );
					proxyServer.sendUDPDataToOtherServer( sendPacket );
					ReferenceHandle.writeReportFile( "UDPServer with ProxyProcess sendRawDataToOtherServer 2 -> Sent the UDP data to the Other Server{IP: "+ address +" Port: "+ portNum +" }", ReferenceHandle.bytesToHexString(receivedPacket.getData()) );
				}
					
				getInfo.clear();
				getInfo = null;
			}
			
		} catch (IOException e) {
			// TODO Exception processing..
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer First sendRawDataToOtherServer() 2 IOException", e.getLocalizedMessage() );
		} catch (NullPointerException e) {
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer First sendRawDataToOtherServer() 2 NullPointerException", e.getLocalizedMessage() );
		} catch (Exception e) {
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer First sendRawDataToOtherServer() 2 Exception", e.getLocalizedMessage() );
		}		
	}
	
	/**
	 * Checking existing status of the UDPServer Instance.
	 * @param _keyName
	 * @return
	 */
	private boolean getProxyProcessExistStatus( int _keyName ) {
		
		ProxyProcess udpTmpServer = serverMap.get( _keyName );
		if ( udpTmpServer != null ) {
			return true;
		}
		
		return false;
	}
	
		
	/**
	 * Sending the data for request in the client.
	 * 
	 * @param _address
	 * @param _port
	 * @param _sendData
	 */
	public void sendPacketDataToModem( InetAddress _address, int _port, byte[] _sendData ) {

		try {
							
			DatagramPacket sendPacket = new DatagramPacket( _sendData, _sendData.length, _address, _port );
			DatagramSocket serverSocket = getServerSocket();
			serverSocket.send( sendPacket );
	    	
	    	String report = "UDPServer sendPacketDataToModem() IP: " + _address + " Port: " + _port + " Socket: " + serverSocket.getLocalSocketAddress();
	    	ReferenceHandle.writeReportFile( report, ReferenceHandle.bytesToHexString( _sendData ) );	
			sendPacket = null;
			
		} catch (IOException e) {
			// TODO Exception processing..
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer sendPacketDataToModem() IOException", e.getLocalizedMessage() );
		} catch (NullPointerException exp) {
			ReferenceHandle.writeReportFile( "UDPServer sendPacketDataToModem() NullPointerException", exp.getLocalizedMessage() );
		}
	}
	
	/**
	 * Stop the Socket and Thread Service.
	 */
	public void stopService() {

		try {

			serverSocket.close();
			this.isRunning = false;
						
			for ( ProxyProcess val: serverMap.values() ) {
				val.stopProxyProcess();
			}
			internalPool.shutdownNow();	
			serverMap = null;
			
		} catch (Exception e) {
			// TODO: handle exception
			ReferenceHandle.writeReportFile( "UDPServer stopService() Exception", e.getLocalizedMessage() );
		}
	}
	
	
	/**
	 * DB setting method.
	 * 
	 * @param _dbConnect
	 */
	public void setDBSetting( DBManager _dbConnect ) {
		
		try {
			
			this.dbManager = _dbConnect;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ReferenceHandle.writeReportFile( "UDPServer setDBSetting() Exception", e.getLocalizedMessage() );
		}
	}
	
	/**
	 * Get the DB setting instance.
	 * @return DB Manager
	 */
	public DBManager getDBManager() {
		
		return dbManager;
	}
	
	/**
	 * Get the UDP ServerSocket instance.
	 * @return
	 */
	public DatagramSocket getServerSocket() {
		
		return this.serverSocket;
	}
}

class RejectedExecutionHandlerImplement implements RejectedExecutionHandler {
	@Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println(r.toString() + " is rejected");
        String monitor = String.format("[Monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getActiveCount(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount(),
                executor.isShutdown(),
                executor.isTerminated());
        ReferenceHandle.writeReportFile( monitor + " Rejected Thread: ", r.toString() );
    }
}

