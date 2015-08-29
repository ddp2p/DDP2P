/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2015 Christopher Widmer and Marius C. Silaghi
		Author: Christophet Widmer cwidmer@my.fit.edu and Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

package net.ddp2p.common.network.upnp;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.ddp2p.common.util.Logger;

public class Client_UPNP
{
    private static final int MAX_MESSAGE_SIZE = 10000; // arbitrary for this application.
    public final int SERVER_TIMEOUT_MS = 10000;
    public static final int UPNP_UDP_BROADCAST_PORT = 1900;
	public final static String NAT_MULTICAST = "239.255.255.250";
    private static final int HTTP_STATUS_OK = 200;

    // Custom Exception for tracking problems related to peer communication.
    private class PeerCommunicationException extends Exception
    {
        public PeerCommunicationException( String message )
        {
            super( message );
        }
    }

    private Logger logger = null;

    private int tcpPort = 0;
    private String upnpDescriptionURL = null;
    private String upnpDeviceAddress = null;
    private String upnpIpControlURL = null;

    public int getTcpPort() { return tcpPort; }
    public void setTcpPort(int tcpPort) { this.tcpPort = tcpPort; }

    public String getUpnpDescriptionURL() { return upnpDescriptionURL; }
    public void setUpnpDescriptionURL(String upnpDescriptionURL)
    {
        this.upnpDescriptionURL = upnpDescriptionURL;
    }

    public String getUpnpDeviceAddress() { return upnpDeviceAddress; }
    public void setUpnpDeviceAddress(String upnpDeviceAddress)
    {
        this.upnpDeviceAddress = upnpDeviceAddress;
    }

    public String getUpnpIpControlURL() { return upnpIpControlURL; }
    public void setUpnpIpControlURL(String upnpIpControlURL)
    {
        this.upnpIpControlURL = upnpIpControlURL;
    }

    public Client_UPNP()
    {
        //logger = new Logger( true, true, true, true );
        logger = new Logger( false, false, false, false );
    }

    /**
     * By a connection to upnp device
     * @return
     * @throws SocketException
     */
    public String getLocalIPAddress()
        throws SocketException
    {
        int port = getTcpPort();
        String upnpDeviceAddressText = getUpnpDeviceAddress();

        // Make a temporary UDP connection to the UPnP Device to get local IP address.
        InetSocketAddress address = new InetSocketAddress( upnpDeviceAddressText, port );
        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.connect( address );
        String localAddressText = udpSocket.getLocalAddress().getHostAddress();
        udpSocket.close();

        return localAddressText;
    }

    private int getHttpStatusCode( String httpResponse )
    {
        int httpIndex = httpResponse.indexOf( "HTTP" );
        int httpEndIndex = httpResponse.indexOf( "\r\n", httpIndex );
        String[] httpLine = httpResponse.split( "[ \t\n]" );

        int statusCode = Integer.parseInt( httpLine[1] );
        String description = httpLine[2];

        logger.debug( "Parsed HTTP status: " + statusCode + " " + description );

        return statusCode;
    }

    // UPnP Discovery.
    // Currently just populates the description URL and TCP port.
    // This information is needed for later requests from the client.
    // TODO: Possible add parsing for control URLs. Hard coded for now.
    /**
     *  sends a request to the getUpnpDeviceAddress():UPNP_UDP_BROADCAST_PORT(1900)
     *  Uses temporary socket. Waits for answer. Extracts tcpPort.
     */
    public boolean discover() {
        int port = UPNP_UDP_BROADCAST_PORT; // use the standard UPnP broadcast port.
        String addressText = getUpnpDeviceAddress();
		return discover(addressText, port);
    }
    /**
     *  sends a request to the getUpnpDeviceAddress():UPNP_UDP_BROADCAST_PORT(1900)
     *  Uses temporary socket. Waits for answer. Extracts tcpPort.
     *  Address 239.255.255.250
     * @throws SocketException
     * @throws IOException
     */
    public boolean discover(String addressText, int port) {
    	try {
            logger.debug("UPNP Discover adr: "+addressText);
			_discover(addressText, port);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return false;
    }
    public void _discover(String addressText, int port)
    		throws SocketException, IOException
    {
        logger.debug("UPNP _Discover adr: "+addressText);
        DatagramSocket socket = new DatagramSocket();

        String request =
        "M-SEARCH * HTTP/1.1\r\nHost:%s:%d\r\n"
      + "ST:urn:schemas-upnp-org:device:InternetGatewayDevice:1\r\n"
      + "Man:\"ssdp:discover\"\r\n"
      + "MX:3\r\n\r\n";

        request = String.format( request, addressText, port );
        byte[] data = request.getBytes();
        logger.debug(addressText+":"+port+"UPNP Discover req\n"+request);

        // Send request.
        InetAddress address = InetAddress.getByName( addressText );
        DatagramPacket packet = new DatagramPacket( data, data.length, address, port );
        socket.send( packet );

        logger.info( "UDP Sent " + data.length + " bytes of data to "
            + address.getHostAddress() + ":" + port );

        // Wait and Parse response.
        data = new byte[ MAX_MESSAGE_SIZE ];
        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
        socket.setSoTimeout(2000);
        socket.receive( receivedPacket );
        String response = new String( data );
        
        logger.debug("UPNP Discover resp\n"+response);

        // Get the port for TCP requests.
        // Extract url, first
        int locationIndex = response.indexOf( "LOCATION:" );
        int urlIndex = response.indexOf( "http://",  locationIndex );
        int urlEndIndex = response.indexOf( "\r\n", urlIndex );
        String url = response.substring( urlIndex, urlEndIndex );
        
        // sets the url obtained
        setUpnpDescriptionURL( url );
        logger.debug("UPNP Discovery desc URL\n" + url);

        // parse and set the port
        URL upnpDeviceURL = new URL( url );
        int tcpPort = upnpDeviceURL.getPort();
        setTcpPort( tcpPort );
        this.setUpnpDeviceAddress(upnpDeviceURL.getHost());
        this.setUpnpDescriptionURL(upnpDeviceURL.getFile());

        logger.debug( "UPNP Discovery Addr: " + this.getUpnpDeviceAddress() );
        logger.debug( "UPNP Discovery Desc URL: " + getUpnpDescriptionURL() );
        logger.debug( "TCP Port: " + getTcpPort() );
        socket.close();
    }

    public String getExternalIPAddress(AddressPortMapping mapping) {
    	try {
			getExternalIP(mapping);
			return mapping.getExternalIPAddress();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
 	/**
     *  Request POST from URL upnpControlURL() Host: getUpnpDeviceAddress():getTcpPort()
     * @param mapping
     * @return
     * @throws IOException
     */
    public long getExternalIP( AddressPortMapping mapping )
        throws IOException
    {
        String externalIPAddress = null;
        int port = getTcpPort();
        String upnpDeviceAddressText = getUpnpDeviceAddress();
        String upnpControlURL = getUpnpIpControlURL();

        String request =
        "POST %s HTTP/1.1\r\n"
      + "Host: %s:%d\r\n"
      + "SOAPAction: \"urn:schemas-upnp-org:service:WANIPConnection:1#GetExternalIPAddress\"\r\n"
      + "Content-Type: text/xml; charset=\"utf-8\"\r\n"
      + "Content-Length: 305\r\n"
      + "\r\n"
      + "<?xml version=\"1.0\"?>\n"
      + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\""
         + "http://schemas.xmlsoap.org/soap/encoding/\">\n"
      + "   <s:Body>\n"
      + "      <u:GetExternalIPAddress xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">"
               +"\n"
      + "      </u:GetExternalIPAddress>\n"
      + "   </s:Body>\n"
      + "</s:Envelope>\n";

        // Verify that the TCP port has been set.
        if( port == 0 )
        {
            logger.error( "TCP port for UPnP requests has not been set." );
            return -1;
        }

        request = String.format( request, upnpControlURL, upnpDeviceAddressText, port );

        InetSocketAddress address = new InetSocketAddress( upnpDeviceAddressText, port );
        Socket socket = new Socket();
        socket.connect( address );

        // Send request.
        OutputStream outStream = socket.getOutputStream();
        byte[] data = request.getBytes();

        long timestampStart = System.currentTimeMillis();

        outStream.write( data );

        logger.info( "TCP Sent " + data.length + " bytes of data to " + address.getHostName()
                + ":" + port );
        logger.debug("Get ExternalIP req\n"+request+"\n");

        // Get response.
        data = new byte[ MAX_MESSAGE_SIZE ];
        InputStream inStream = socket.getInputStream();
        int read = 0, tran = inStream.read( data  );
        
        while (tran > 0) {
        	read += tran;
        	tran = inStream.read(data, read, data.length - read);
        }

        long timestampEnd = System.currentTimeMillis();

        String response = new String( data );
        logger.debug("Get ExternalIP res\n"+response);

        // For now only reads once.
        if( read > 0 )
        {
            logger.info("TCP Received data from "
                + socket.getRemoteSocketAddress().toString() );
        }

        socket.close();

        // Skip the HTTP content.
        int xmlStartIndex = response.indexOf( "<?xml version=\"1.0\"?>" );
        // trim() is needed to get rid of trailing newlines
        String responseXmlContent = response.substring( xmlStartIndex ).trim();

        // Parse response.
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            InputSource is = new InputSource( new StringReader( responseXmlContent ) );
            Document responseDocument = db.parse( is );
            NodeList nl = responseDocument.getElementsByTagName( "NewExternalIPAddress" );
            if( nl != null && nl.getLength() > 0 )
            {
                Element addressElement = (Element)nl.item( 0 ); // there is only one.
                externalIPAddress = addressElement.getFirstChild().getNodeValue();
            }
            else
            {
                logger.error( "NewExternalIPAddress not found in response XML." );
                return -1;
            }
        }
        catch( ParserConfigurationException | SAXException e )
        {
            logger.error(  "Unable to parse response XML: " + e );
        }

        logger.debug( "ExternalIPAddress: " + externalIPAddress );

        // Sets the external IP address only.
        mapping.setExternalIPAddress( externalIPAddress );

        return timestampEnd - timestampStart;
    }

    public long getExternalIPMapping( AddressPortMapping mapping, int port_req, String protocol) {
    	try {
			return _getExternalIPMapping(mapping, port_req, protocol);
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return -1;
    }
    public long _getExternalIPMapping( AddressPortMapping mapping, int port_req, String protocol)
            throws IOException
        {
            String externalIPAddress = null;
            int port = getTcpPort();
            String upnpDeviceAddressText = getUpnpDeviceAddress();
            String upnpControlURL = getUpnpIpControlURL();

            String request =
            "POST %s HTTP/1.1\r\n"
          + "Host: %s:%d\r\n"
          + "SOAPAction: \"urn:schemas-upnp-org:service:WANIPConnection:1#GetSpecificPortMappingEntry\"\r\n"
          + "Content-Type: text/xml; charset=\"utf-8\"\r\n"
          + "Connection: Close\r\n"
          + "Cache-Control: no-cache\r\n"
          + "Pragma: no-cache\r\n"
          //+ "Content-Length: 305\r\n"
          + "\r\n"
          + "<?xml version=\"1.0\"?>\n"
          + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\""
             + "http://schemas.xmlsoap.org/soap/encoding/\">\n"
          + "   <s:Body>\n"
          + "      <u:GetSpecificPortMappingEntry xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">"
                   +"\n"
                   + "        <NewRemoteHost></NewRemoteHost>\n"
                   + "        <NewExternalPort>%d</NewExternalPort>\n"
                   + "        <NewProtocol>%s</NewProtocol>\n"
          + "      </u:GetSpecificPortMappingEntry>\n"
          + "   </s:Body>\n"
          + "</s:Envelope>\n";

            // Verify that the TCP port has been set.
            if( port == 0 )
            {
                logger.error( "TCP port for UPnP requests has not been set." );
                return -1;
            }

            request = String.format( request, upnpControlURL, upnpDeviceAddressText, port, port_req, protocol);

            InetSocketAddress address = new InetSocketAddress( upnpDeviceAddressText, port );
            Socket socket = new Socket();
            socket.connect( address );

            // Send request.
            OutputStream outStream = socket.getOutputStream();
            byte[] data = request.getBytes();

            long timestampStart = System.currentTimeMillis();

            outStream.write( data );

            logger.info( "TCP Sent " + data.length + " bytes of data to " + address.getHostName()
                    + ":" + port );
            logger.debug("Get ExternalIP req\n"+request+"\n");

            // Get response.
            data = new byte[ MAX_MESSAGE_SIZE ];
            InputStream inStream = socket.getInputStream();
            int read = 0, tran = inStream.read( data  );
            
            while (tran > 0) {
            	read += tran;
            	tran = inStream.read(data, read, data.length - read);
            }

            long timestampEnd = System.currentTimeMillis();

            String response = new String( data );
            logger.debug("Get ExternalIPMapping res\n"+response);

            // For now only reads once.
            if( read > 0 )
            {
                logger.info("TCP Received data from "
                    + socket.getRemoteSocketAddress().toString() +"\n"+response);
            }

            socket.close();
            // Parse response (verify status code).
            int resultStatusCode = getHttpStatusCode( response );
            if( resultStatusCode == HTTP_STATUS_OK ) {
                logger.info( "AddPortMapping(): Received OK response from UPnP device." );
            } else {
            	return timestampEnd - timestampStart;
            }
            
            // Skip the HTTP content.
            int xmlStartIndex = response.indexOf( "<?xml version=\"1.0\"?>" );
            // trim() is needed to get rid of trailing newlines
            String responseXmlContent = response.substring( xmlStartIndex ).trim();

            // Parse response.
            try
            {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();

                InputSource is = new InputSource( new StringReader( responseXmlContent ) );
                Document responseDocument = db.parse( is );
                
                String duration=null, port_int=null, IP_int=null, desc=null, enabled=null;
                NodeList nl;
                nl = responseDocument.getElementsByTagName( "NewInternalPort" );
                if( nl != null && nl.getLength() > 0 ) {
                    Element addressElement = (Element)nl.item( 0 ); // there is only one.
                    port_int = addressElement.getFirstChild().getNodeValue();
                }
                nl = responseDocument.getElementsByTagName( "NewInternalClient" );
                if( nl != null && nl.getLength() > 0 ) {
                    Element addressElement = (Element)nl.item( 0 ); // there is only one.
                    IP_int = addressElement.getFirstChild().getNodeValue();
                }
                nl = responseDocument.getElementsByTagName( "NewEnabled" );
                if( nl != null && nl.getLength() > 0 ) {
                    Element addressElement = (Element)nl.item( 0 ); // there is only one.
                    enabled = addressElement.getFirstChild().getNodeValue();
                }
                nl = responseDocument.getElementsByTagName( "NewPortMappingDescription" );
                if( nl != null && nl.getLength() > 0 ) {
                    Element addressElement = (Element)nl.item( 0 ); // there is only one.
                    desc = addressElement.getFirstChild().getNodeValue();
                }
                nl = responseDocument.getElementsByTagName( "NewLeaseDuration" );
                if( nl != null && nl.getLength() > 0 ) {
                    Element addressElement = (Element)nl.item( 0 ); // there is only one.
                    duration = addressElement.getFirstChild().getNodeValue();
                }
                logger.debug("Summary: "+duration+port_int+IP_int+ desc+ enabled);
            }
            catch( ParserConfigurationException | SAXException e )
            {
                logger.error(  "Unable to parse response XML: " + e );
            }

            logger.debug( "ExternalIPAddress: " + externalIPAddress );

            // Sets the external IP address only.
            //mapping.setExternalIPAddress( externalIPAddress );

            return timestampEnd - timestampStart;
        }

    /**
     * Gets the controlURL
     * @return
     */
	public boolean getUpnpDescription() {
		try {
			return _getUpnpDescription();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * Gets the controlURL
	 * @return
	 * @throws IOException
	 */
	public boolean _getUpnpDescription() throws IOException {
        int port = getTcpPort();
        if (port <= 0) port = 5000;
        String upnpDeviceAddressText = getUpnpDeviceAddress();
        
        String requestHeader =
        "GET %s HTTP/1.1\r\n"
      + "Host: %s:%d\r\n"
      + "Connection: Close\r\n"
      + "\r\n";

        //String localAddressText = getLocalIPAddress();

        // Set up the content and header.
        requestHeader = String.format( requestHeader, this.getUpnpDescriptionURL(), upnpDeviceAddressText, port);
        String request = requestHeader;
        
        InetSocketAddress address = new InetSocketAddress( upnpDeviceAddressText, port );
        Socket socket = new Socket();
        logger.debug("Get addMapping conn addr:\n"+address+"\nfor\n"+request);
        
        socket.setSoTimeout(2000);
        socket.connect( address );

        // Send request.
        OutputStream outStream = socket.getOutputStream();
        byte[] data = request.getBytes();

        outStream.write( data );

        logger.info( "TCP Sent " + data.length + " bytes of data to " + address.getHostName()
                + ":" + port );
        logger.debug("Get Description req:\n"+request);

        // Get response.
        data = new byte[ MAX_MESSAGE_SIZE ];
        InputStream inStream = socket.getInputStream();
        int read = 0, tran = inStream.read( data  );
        
        while (tran > 0) {
        	read += tran;
        	tran = inStream.read(data, read, data.length - read);
        }
        socket.close();
       
       // long timestampEnd = System.currentTimeMillis();

        String response = new String( data );
        logger.debug("Got Description resp\n"+response);

        // For now only reads once.
        if( read > 0 )
        {
            logger.info("TCP Received data from "
                + socket.getRemoteSocketAddress() );
        }

        // Parse response (verify status code).
        int resultStatusCode = getHttpStatusCode( response );
        if( resultStatusCode == HTTP_STATUS_OK )
        {
            logger.info( "GetUPNPDescription(): Received OK response from UPnP device." );
            
            
        } else return false;
        
        String url = getControlURL (response, "WANIPConnection");
        if (url == null) url = getControlURL (response, "WANPPPConnection");
        if (url == null) return false;
        
        this.setUpnpIpControlURL(url);

        return true;
	}
	public String getControlURL (String response, String service) {
		String result = null;
        int locationIndex1 = response.indexOf( "urn:schemas-upnp-org:device:WANConnectionDevice:" );
        if (locationIndex1 < 0) return result;
        int locationIndex2 = response.indexOf( "urn:schemas-upnp-org:service:"+service+":", locationIndex1 );
        if (locationIndex2 < 0) return result;
        int urlIndex = response.indexOf( "<controlURL>",  locationIndex2 );
        if (urlIndex < 0) return result;
        int urlEndIndex = response.indexOf( "</controlURL>", urlIndex );
        if (urlEndIndex < 0) return result;
        String url = response.substring( urlIndex+"<controlURL>".length(), urlEndIndex );
        if (url.length() <= 0) return result;
        return url;
	}
    /**
     * 
     * @param internalPortToMap
     * @param externalPortToMap
     * @param protocol ("TCP" or "UDP")
     * @param description
     * @param leaseDuration
     * @param mapping
     * @return
     * @throws UnknownHostException
     * @throws IOException
     */
    public boolean addPortMapping( int internalPortToMap, int externalPortToMap, String protocol,
            String description, long leaseDuration,
            AddressPortMapping mapping ){
        long portMappingTime = -1;
		try {
			portMappingTime = _addPortMapping( internalPortToMap, externalPortToMap, protocol,
			        description, leaseDuration, mapping );
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
        return false;
    }
    public long _addPortMapping( int internalPortToMap, int externalPortToMap, String protocol,
                                String description, long leaseDuration,
                                AddressPortMapping mapping )
        throws UnknownHostException, IOException
    {
        int port = getTcpPort();
        if (port <= 0) port = 5000;
        String upnpDeviceAddressText = getUpnpDeviceAddress();
        String upnpControlURL = getUpnpIpControlURL();

        String requestHeader =
        "POST %s HTTP/1.1\r\n"
      + "Host: %s:%d\r\n"
      + "SOAPAction: \"urn:schemas-upnp-org:service:WANIPConnection:1#AddPortMapping\"\r\n"
      + "Content-Type: text/xml; charset=\"utf-8\"\r\n"
      + "Content-Length: %d\r\n"
      + "\r\n";

        String requestContent =
        "<?xml version=\"1.0\"?>\n"
      + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        +"s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
      + "  <s:Body>\n"
      + "    <u:AddPortMapping xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">\n"
      + "      <NewRemoteHost></NewRemoteHost>\n"
      + "      <NewExternalPort>%d</NewExternalPort>\n"
      + "      <NewProtocol>%s</NewProtocol>\n"
      + "      <NewInternalPort>%d</NewInternalPort>\n"
      + "      <NewInternalClient>%s</NewInternalClient>\n"
      + "      <NewEnabled>1</NewEnabled>\n"
      + "      <NewPortMappingDescription>%s</NewPortMappingDescription>\n"
      + "      <NewLeaseDuration>%d</NewLeaseDuration>\n"
      + "    </u:AddPortMapping>\n"
      + "  </s:Body>\n"
      + "</s:Envelope>\"\n";

        String localAddressText = getLocalIPAddress();

        // Set up the content and header.
        requestContent = String.format( requestContent, externalPortToMap, protocol,
                                        internalPortToMap, localAddressText, description,
                                        leaseDuration );
        int contentLength = requestContent.getBytes().length;
        requestHeader = String.format( requestHeader, upnpControlURL, upnpDeviceAddressText, port,
                                       contentLength );
        String request = requestHeader + requestContent;

        InetSocketAddress address = new InetSocketAddress( upnpDeviceAddressText, port );
        Socket socket = new Socket();
        logger.debug("Get addMapping conn addr:\n"+address+"\nfor\n"+request);
        socket.connect( address );

        // Send request.
        OutputStream outStream = socket.getOutputStream();
        byte[] data = request.getBytes();

        long timestampStart = System.currentTimeMillis();

        outStream.write( data );

        logger.info( "TCP Sent " + data.length + " bytes of data to " + address.getHostName()
                + ":" + port );
        logger.debug("Get addMapping req:\n"+request);

        // Get response.
        data = new byte[ MAX_MESSAGE_SIZE ];
        InputStream inStream = socket.getInputStream();
        int read = 0, tran = inStream.read( data  );
        
        while (tran > 0) {
        	read += tran;
        	tran = inStream.read(data, read, data.length - read);
        }

        long timestampEnd = System.currentTimeMillis();

        String response = new String( data );
        logger.debug("Get addMapping resp\n"+response);

        // For now only reads once.
        if( read > 0 )
        {
            logger.info("TCP Received data from "
                + socket.getRemoteSocketAddress().toString() );
        }

        // Parse response (verify status code).
        int resultStatusCode = getHttpStatusCode( response );
        if( resultStatusCode == HTTP_STATUS_OK )
        {
            logger.info( "AddPortMapping(): Received OK response from UPnP device." );

            // Sets the port information only. Assumes a successful response means the port is
            // mapped.
            mapping.setInternalPort( internalPortToMap );
            mapping.setExternalPort( externalPortToMap );
        }
        else
        {
            logger.info( "AddPortMapping(): Received error response from UPnP device: "
                    + resultStatusCode );
        }

        socket.close();

        return timestampEnd - timestampStart;
    }

    public long deletePortMapping( int externalPortToDelete, String protocol )
        throws IOException
    {
        int port = getTcpPort();
        String upnpDeviceAddressText = getUpnpDeviceAddress();
        String upnpControlURL = getUpnpIpControlURL();

        String requestHeader =
        "POST %s HTTP/1.1\r\n"
      + "Host: %s:%d\r\n"
      + "SOAPAction: \"urn:schemas-upnp-org:service:WANIPConnection:1#DeletePortMapping\"\r\n"
      + "Content-Type: text/xml; charset=\"utf-8\"\r\n"
      + "Content-Length: %d\r\n"
      + "\r\n";

        String requestContent =
        "<?xml version=\"1.0\"?>\n"
      + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" "
        +"s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n"
      + "  <s:Body>\n"
      + "     <u:DeletePortMapping xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">\n"
      + "        <NewRemoteHost></NewRemoteHost>\n"
      + "        <NewExternalPort>%d</NewExternalPort>\n"
      + "        <NewProtocol>%s</NewProtocol>\n"
      + "     </u:DeletePortMapping>\n"
      + "  </s:Body>\n"
      + "</s:Envelope>\n";

        String localAddressText = getLocalIPAddress();

        // Set up the content and header.
        requestContent = String.format( requestContent, externalPortToDelete, protocol );
        int contentLength = requestContent.getBytes().length;
        requestHeader = String.format( requestHeader, upnpControlURL, upnpDeviceAddressText, port,
                                       contentLength );

        InetSocketAddress address = new InetSocketAddress( upnpDeviceAddressText, port );
        Socket socket = new Socket();
        socket.connect( address );

        // Send request.
        OutputStream outStream = socket.getOutputStream();
        String request = requestHeader + requestContent;
        byte[] data = request.getBytes();
        outStream.write( data );

        logger.info( "TCP Sent " + data.length + " bytes of data to " + address.getHostName()
                + ":" + port );

        // Get response.
        data = new byte[ MAX_MESSAGE_SIZE ];
        InputStream inStream = socket.getInputStream();
        int read = 0, tran = inStream.read( data  );
        
        while (tran > 0) {
        	read += tran;
        	tran = inStream.read(data, read, data.length - read);
        }
        String response = new String( data );

        // For now only reads once.
        if( read > 0 )
        {
            logger.info("TCP Received data from "
                + socket.getRemoteSocketAddress().toString() );
        }

        // Parse response (verify status code).
        int resultStatusCode = getHttpStatusCode( response );
        if( resultStatusCode == HTTP_STATUS_OK )
        {
            logger.info( "DeletePortMapping(): Recevied OK response from UPnP device." );
        }
        else
        {
            logger.info( "DeletePortMapping(): Received error response from UPnP device: "
                    + resultStatusCode );
        }

        socket.close();

        return 0; // TODO: Return time.
    }

    /**
     * Returns time taken to accomplish
     * @param internalPortToMap
     * @param externalPortToMap
     * @param protocol
     * @param description
     * @param leaseDuration
     * @param mapping
     * @return
     * @throws IOException
     */
    public long portMappingWrapper( int internalPortToMap, int externalPortToMap, String protocol,
                                    String description, int leaseDuration,
                                    AddressPortMapping mapping )
        throws IOException
    {
        long portMappingTime = _addPortMapping( internalPortToMap, externalPortToMap, protocol,
                                               description, leaseDuration, mapping );
        long addressFetchTime = getExternalIP( mapping );

        return portMappingTime + addressFetchTime;
    }
    /**
     * Returns the external port
     * 
     * @param internalPortToMap
     * @param externalPortToMap
     * @param protocol
     * @param description
     * @param leaseDuration
     * @param mapping
     * @return
     * @throws IOException
     */
    public int getPortMapping( int internalPortToMap, int externalPortToMap, String protocol,
            String description, int leaseDuration,
            AddressPortMapping mapping )
    {
    	try {
			long portMappingTime = _addPortMapping( internalPortToMap, externalPortToMap, protocol,
			               description, leaseDuration, mapping );
	    	long addressFetchTime = getExternalIP( mapping );
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}

    	return mapping.getExternalPort();
    }

    // NOTE: For UPnP this may work without the mapping since the internal and external ports
    // have to be the same. Verify that.
    public long pingPeer( String peerAddressText, int peerPort, AddressPortMapping mapping )
            throws SocketException, IOException, PeerCommunicationException
    {
        InetAddress address = InetAddress.getByName( peerAddressText );

        byte[] data = mapping.getExternalBytes();

        DatagramSocket socket = new DatagramSocket( mapping.getInternalPort() );
        DatagramPacket packet = new DatagramPacket( data, data.length, address, peerPort );
        long startTime = System.currentTimeMillis();
        socket.send( packet );

        logger.info( "UDP Sent " + data.length + " bytes of data to "
            + address.getHostAddress() + ":" + peerPort );

        socket.setSoTimeout( SERVER_TIMEOUT_MS );

        long endTime = 0;
        try
        {
            // Parse response.
            data = new byte[ MAX_MESSAGE_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            socket.receive( receivedPacket );

            endTime = System.currentTimeMillis();

            // Verify sender address.
            String sourceAddress = receivedPacket.getSocketAddress().toString();
            int colonIndex = sourceAddress.indexOf( ':' ); // remove extraneous information.
            sourceAddress = sourceAddress.substring( 1, colonIndex );
            if( ! sourceAddress.equals( peerAddressText ))
            {
                String errorMessage = "Source address of peer response doesn't match peer! "
                        + sourceAddress;
                logger.error( errorMessage );
                throw new PeerCommunicationException( errorMessage );
            }

            logger.info( "Received response from peer!\n" );
        }
        catch( SocketTimeoutException e )
        {
            String errorMessage = "Timed out while waiting for peer response.";
            logger.error( errorMessage);
            throw new PeerCommunicationException( errorMessage );
        }
        finally
        {
            socket.close();
        }

        return endTime - startTime;
    }


    public static void main(String[] args)
        throws UnknownHostException, SocketException, IOException, InterruptedException
    {
        String upnpDeviceAddress = "192.168.1.1";
        String upnpIPControlUrl = "/ctl/IPConn";

        int numRuns = 100;
        String outputFilePath = "output.txt";
        int internalPortToMap = 2500;
        int externalPortToMap = 45000;
        String protocol = "UDP";
        String peerIP = "0";
        int peerPort = 1230;

        // If there are no command line arguments, run with the defaults.
        if( args.length == 0 )
        {
            // nothing to do.
        }
        // Otherwise check for the correct number of arguments.
        else if( args.length < 5 )
        {
            System.out.println( "usage: Client <Device IP> <Peer IP> <Internal Port to Map> "
                    + "<External Port to Map> <Protocol> <Output File> <Number of Runs>" );
        }
        // Otherwise parse the arguments.
        else
        {
            upnpDeviceAddress = args[0];
            peerIP = args[1];
            internalPortToMap = Integer.parseInt( args[2] );
            externalPortToMap = Integer.parseInt( args[3] );
            protocol = args[4];
            outputFilePath = args[5];
            numRuns = Integer.parseInt( args[6] );
        }

        // General setup.
        Client_UPNP client = new Client_UPNP();
        client.setUpnpDeviceAddress(  upnpDeviceAddress  );
        client.setUpnpIpControlURL( upnpIPControlUrl );
        client.discover();

        AddressPortMapping mapping = new AddressPortMapping();

        // Run the tests.
        System.out.println( "Running with the following parameters:");
        System.out.println( "Peer IP Address: " + peerIP );
        System.out.println( "UPnP Device IP:\t" + upnpDeviceAddress );
        System.out.println( "InternalPortToMap:\t" + internalPortToMap );
        System.out.println( "ExternalPortToMap:\t" + externalPortToMap );
        System.out.println( "Protocol:\t" + protocol );
        System.out.println( "Output File:\t" + outputFilePath );
        System.out.println( "Number of Runs:\t" + numRuns );

        for( int i = 0; i < numRuns; i++ )
        {
            long mappingTime = client.portMappingWrapper( internalPortToMap, externalPortToMap, protocol,
                                                          "test mapping", 0, mapping );
            if (mapping == null) continue;

            long peerPingTime = 0;
            try
            {
                peerPingTime = client.pingPeer( peerIP, peerPort, mapping );

                // Uses the default file encoding.
                FileWriter writer = new FileWriter( outputFilePath, true );
                writer.write( i + " " + mappingTime + " " + peerPingTime + " "
                        +  (mappingTime + peerPingTime) + "\n" );
                writer.close();
            }
            catch( PeerCommunicationException e )
            {
                // The error was logged in pingPeer().
            }

            client.deletePortMapping( externalPortToMap, protocol );

            if( i < numRuns - 1 )
            {
                Thread.sleep( 30000 ); // 30 seconds seems to be the minimum. Need to try 60 just to make sure.
            }
        }
    }
}

