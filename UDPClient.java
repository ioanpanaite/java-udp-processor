package secontrol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;





public class UDPClient extends ReferenceHandle {
	
	private UDPServer server;	
	private DBManager dbConnect;
	private int devicePeriod;
	private int softwarePeriod;
	private int nvlinkPeriod;
	
	private boolean deviceInfo;
	private boolean softwareInfo;
	private boolean nvlinkInfo;
	
	private ExecutorService executorService;


	String addressLists = "";
	String portLists = "";
	String deviceLists = "";
	String[] addressItem = null;
	String[] portItem = null;
	
	
	
	UDPClient() {}
		
	/**
	 * Starting the control method.
	 */
	public void startClient() {
		
		try {

			this.setAllSiteData();
			
			/**
			 * Creating the Thread for sending the data to all site's server socket.
			 * This will have max 3 threads.
			 * It is for OS2 device info getting thread, Software Version and Serial Number getting thread, NVLink info getting thread.   
			 * 
			 * @author M.C
			 */
			executorService = Executors.newFixedThreadPool( 3 );
			
			// Create the thread for getting OS2 device info.
			if ( deviceInfo && devicePeriod != 0 )
				this.deviceWork( devicePeriod );
			
			// Create the thread for getting Software Version and Serial Number.
			if ( softwareInfo && softwarePeriod != 0 )
				this.softwareWork( softwarePeriod );
			
			// Create the thread for getting the NVLink Info.
			if ( nvlinkInfo && nvlinkPeriod != 0 )
				this.nvlinkWork( nvlinkPeriod );
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			writeReportFile( "startClient()", e.getLocalizedMessage() );
		}
	}
	
	/**
	 * Creating sending data thread for getting the Device info.  
	 * @param _kindPeriod
	 */
	public void deviceWork( int _kindPeriod ) {

		
		// Create thread for the device info. 
		Runnable deviceWorkThread = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while ( !Thread.currentThread().isInterrupted() ) {

					try {
											
						Thread.currentThread().setPriority(6);
						
						// Get the info in DB.
						DBManager dbConnect = getDBConnect();
						List<String> deviceAddressLists = dbConnect.getModemAddressList();
						List<String> devicePortLists = dbConnect.getModemPortList();
						List<String> siteLists = dbConnect.getSitesList();
						
						Iterator<String> addressItem = deviceAddressLists.iterator();
						Iterator<String> portItem = devicePortLists.iterator();
						Iterator<String> sitesItem = siteLists.iterator();
						
						List<String> deviceCountLists = null;
						Iterator<String> deviceItem = null;
						String site = "";
						while( addressItem.hasNext() ) {
		    				
		    				try {
		    					
		    					String siteItem = sitesItem.next();
		    					if ( !site.equals(siteItem) ) {

									deviceCountLists = dbConnect.getSiteDeviceList( siteItem );
									deviceItem = deviceCountLists.iterator();
		    					}

		    					InetAddress device_address = InetAddress.getByName( addressItem.next().replace( "/", "" ).trim() );
			    				int device_port = Integer.parseInt( portItem.next() );
			    				
		    					while( deviceItem.hasNext() ) {

			    					int num = Integer.parseInt(deviceItem.next());
									String deviceNomer = Integer.toHexString(num).length() > 1 ? Integer.toHexString(num).toUpperCase() : "0" + Integer.toHexString(num).toUpperCase();
									
									StringBuffer sendData = new StringBuffer();
									sendData.append( "<" );
									sendData.append( deviceNomer );
									sendData.append( "7E0A8E0A00000000000000\\n" );
			    					
									sendDataToServer( device_address, device_port, sendData.toString(), "ASCII" );
									
									StringBuffer a = new StringBuffer();
									a.append( "Device Info:" );
									a.append( device_address );
									a.append( ":" );
									a.append( device_port ); 
									writeReportFile( a.toString(), sendData.toString() );
									sendData = null;			
									a = null;
		    					}
		    			
								
								site = siteItem;
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								writeReportFile( "deviceWork() UnknownHostException", e.getLocalizedMessage() );
							}
		    			}

						Thread.sleep( _kindPeriod * 1000 );
												
						if ( !deviceAddressLists.isEmpty() )
							deviceAddressLists.clear();
						if ( !devicePortLists.isEmpty() )
							devicePortLists.clear();
						if ( !deviceCountLists.isEmpty() )
							deviceCountLists.clear();
						if ( !siteLists.isEmpty() )
							siteLists.clear();
						
						deviceAddressLists = null;
						devicePortLists = null;
						deviceCountLists = null;
						siteLists = null;
											
						addressItem = null;
						portItem = null;
						deviceItem = null;
													
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						Thread.currentThread().interrupt();
						break;
					} catch (NullPointerException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						writeReportFile( "deviceWork() NullPointerException", e.getLocalizedMessage() );
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						writeReportFile( "deviceWork() SQLException", e.getLocalizedMessage() );
					} catch (Exception exp) {
						exp.printStackTrace();
						writeReportFile( "deviceWork() Exception", exp.getLocalizedMessage() );
					}
				}
			}
		};
		
		executorService.submit(deviceWorkThread);
	}

	/**
	 * Creating data sending thread for getting the Software Version and Serial Number.
	 * @param _softwarePeriod
	 */
	private void softwareWork( int _softwarePeriod ) {
		
		// Create thread for the Software Info.
		Runnable softwareWorkThread = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while ( !Thread.currentThread().isInterrupted() ) {
					
					try {
						
						Thread.currentThread().setPriority(6);
						
						// Get the info in DB.
						DBManager dbConnect = getDBConnect();
						List<String> softAddressLists = dbConnect.getModemAddressList();
						List<String> softPortLists = dbConnect.getModemPortList();
						List<String> siteLists = dbConnect.getSitesList();
												
						Iterator<String> addressItem = softAddressLists.iterator();
						Iterator<String> portItem = softPortLists.iterator();
						Iterator<String> sitesItem = siteLists.iterator();
						
						List<String> softCountLists = null;
						Iterator<String> softItem = null;
						String site = "";
						
						while( addressItem.hasNext() ) {
		    				
							try {
								
								String siteItem = sitesItem.next();
		    					if ( !site.equals(siteItem) ) {

		    						softCountLists = dbConnect.getSiteDeviceList( siteItem );
		    						softItem = softCountLists.iterator();
		    					}
		    					
								InetAddress device_address = InetAddress.getByName( addressItem.next().replace( "/", "" ).trim() );
			    				int device_port = Integer.parseInt( portItem.next() );
								
			    				while( softItem.hasNext() ) {

			    					int num = Integer.parseInt(softItem.next());
									String deviceNomer = Integer.toHexString(num).length() > 1 ? Integer.toHexString(num).toUpperCase() : "0" + Integer.toHexString(num).toUpperCase();
									
									StringBuffer sendData = new StringBuffer();
									sendData.append( "<" );
									sendData.append( deviceNomer );
									sendData.append( "7E00880000000000000000\\n" );

									sendDataToServer( device_address, device_port, sendData.toString(), "ASCII" );
									
									StringBuffer a = new StringBuffer();
									a.append( "Software Info:" );
									a.append( device_address );
									a.append( ":" );
									a.append( device_port ); 
									writeReportFile( a.toString(), sendData.toString() );
									a = null;
									sendData = null;
								}	

								site = siteItem;
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								writeReportFile( "softwareWork() UnknownHostException", e.getLocalizedMessage() );
							}
		    			}

						Thread.sleep( _softwarePeriod * 1000 );

						if ( !softAddressLists.isEmpty() )
							softAddressLists.clear();
						if ( !softPortLists.isEmpty() )
							softPortLists.clear();
						if ( !softCountLists.isEmpty() )
							softCountLists.clear();
						if ( !siteLists.isEmpty() )
							siteLists.clear();
						
						softAddressLists = null;
						softPortLists = null;
						softCountLists = null;
						siteLists = null;
						
						addressItem = null;
						portItem = null;
						softItem = null;
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						Thread.currentThread().interrupt();
						break;
					} catch (NullPointerException exp) {
						// TODO Auto-generated catch block
						writeReportFile( "softwareWork() NullPointerException", exp.getLocalizedMessage() );
					} catch (SQLException exp) {
						// TODO Auto-generated catch block
						writeReportFile( "softwareWork() SQLException", exp.getLocalizedMessage() );
					} catch (Exception exp) {
						exp.printStackTrace();
						writeReportFile( "softwareWork() Exception", exp.getLocalizedMessage() );
					}
				}
			}
		};
		
		
		executorService.submit(softwareWorkThread);
	}

	/**
	 * Creating data sending thread for getting the NVLink info.
	 * @param _nvlinkPeriod
	 */
	private void nvlinkWork( int _nvlinkPeriod ) {

		// Create thread for the NVLink Info.
		Runnable nvlinkWorkThread = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while ( !Thread.currentThread().isInterrupted() ) {
					
					try {

						Thread.currentThread().setPriority(6);
						
						// Get the info in DB.
						DBManager dbConnect = getDBConnect();
						List<String> nvlinkAddressLists = dbConnect.getModemAddressList();
						List<String> nvlinkPortLists = dbConnect.getModemPortList();
												
						Iterator<String> addressItem = nvlinkAddressLists.iterator();
						Iterator<String> portItem = nvlinkPortLists.iterator();

						while( addressItem.hasNext() ) {
		    				
		    				try {

								InetAddress device_address = InetAddress.getByName( addressItem.next().replace( "/", "" ).trim() );
			    				int device_port = Integer.parseInt( portItem.next() );
								
								String sendData = "<7C7E00880000000000000000\\n";
								sendDataToServer( device_address, device_port, sendData, "ASCII" );	
								
								StringBuffer a = new StringBuffer();
								a.append( "NVLink Info" );
								a.append( device_address );
								a.append( ":" );
								a.append( device_port ); 
								writeReportFile( a.toString(), sendData.toString() );
								sendData = null;
								a = null;
								
							} catch (UnknownHostException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								writeReportFile( "nvlinkWork() UnknownHostException", e.getLocalizedMessage() );
							}
		    			}
						
						Thread.sleep( _nvlinkPeriod * 1000 );

						if ( !nvlinkAddressLists.isEmpty() )
							nvlinkAddressLists.clear();
						if ( !nvlinkPortLists.isEmpty() ) 
							nvlinkPortLists.clear();
												
						nvlinkAddressLists = null;
						nvlinkPortLists = null;
						addressItem = null;
						portItem = null;
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						Thread.currentThread().interrupt();
						break;
					} catch (NullPointerException e) {
						// TODO Auto-generated catch block
						writeReportFile( "nvlinkWork() NullPointerException", e.getLocalizedMessage() );
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						writeReportFile( "nvlinkWork() SQLException", e.getLocalizedMessage() );
					} catch (Exception exp) {
						exp.printStackTrace();
						writeReportFile( "nvlinkWork() Exception", exp.getLocalizedMessage() );
					}
				}
			}
		};
		
		executorService.submit(nvlinkWorkThread);		
	}
	
	/**
	 * Method for stopping the threads. 
	 */
	public void stopClient() {

		System.out.println("Thread Stop");
		if ( executorService == null ) throw new NullPointerException( "stopClient" );
		executorService.shutdownNow();
	}
	
	/**
	 * Set the address, port, device count, period, active status of All Site from DB.
	 * This will work for all site.
	 */
	private void setAllSiteData() {
				
		try {

			DBManager dbConnect = getDBConnect();
			HashMap<String, String> allSites = dbConnect.getAllDataFromIncomingTable();
			if ( !allSites.isEmpty() ) {
						
				// Get the Device, Software, NVLink info.
				devicePeriod = Integer.parseInt(allSites.get("device_period"));
				softwarePeriod = Integer.parseInt(allSites.get("software_period"));
				nvlinkPeriod = Integer.parseInt(allSites.get("nvlink_period"));

				deviceInfo = allSites.get("device_info").equals("1") ? true : false;
				softwareInfo = allSites.get("software_info").equals("1") ? true : false;
				nvlinkInfo = allSites.get("nvlink_info").equals("1") ? true : false;
			}
			allSites.clear();
			allSites = null;
			
		} catch (SQLException e) {
			// SQLException processing
			writeReportFile( "setAllSiteData() SQLException", e.getLocalizedMessage() );
		}
	}
		
	/**
	 * Send the data manually.
	 * 
	 * @param _udpServer
	 * @param _address
	 * @param _port
	 * @param _sendData
	 * @param _dataType
	 */
	public void sendDataToServer( InetAddress _address, int _port, String _sendData, String _dataType ) {
		
		UDPServer _udpServer = getUDPServer();
		byte[] sendData = _sendData.getBytes();
		if ( _dataType == "HEX")
			sendData = super.hexStringToByteArray( _sendData );
				
		_udpServer.sendPacketDataToModem( _address, _port, sendData );
	}

	/**
	 * Set the UDPServer instance in UDPClient.
	 * @param _udpServer
	 */
	public void setUDPServer(UDPServer _udpServer) {
		this.server = _udpServer;
	}

	/**
	 * Set the DBManager instance in UDPClient.
	 * @param _dbConnect
	 */
	public void setDBConnect(DBManager _dbConnect) {
		this.dbConnect = _dbConnect;
	}
	
	/**
	 * Get the UDPServer instance.
	 * @return UDPServer
	 */
	public UDPServer getUDPServer() {
		return server;
	}
	
	/**
	 * Get the DBManager instance.
	 * @return DBManager
	 */
	public DBManager getDBConnect() {
		return dbConnect;
	}
}
