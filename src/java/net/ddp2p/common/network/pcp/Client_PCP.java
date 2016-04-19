package net.ddp2p.common.network.pcp;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import net.ddp2p.common.network.upnp.AddressPortMapping;
import net.ddp2p.common.util.Logger;
import net.ddp2p.common.util.Util;
public class Client_PCP
{
    private static final boolean DEBUG = false;
	public final int TIMEOUT_BUFFER_MS = 5000;
    public final int SERVER_TIMEOUT_MS = 10000;
    public final int MAX_MESSAGE_SIZE = 10000;
    private class PeerCommunicationException extends Exception
    {
        public PeerCommunicationException( String message )
        {
            super( message );
        }
    }
    private Logger logger = null;
    public Client_PCP()
    {
        logger = new Logger( false, false, false, false );
    }
    public byte[] encodeIPv4toIPv6( byte[] ipv4Address)
    {
        byte[] ipv6Address = new byte[16];
        ipv6Address[10] = -1; 
        ipv6Address[11] = -1;
        Util.copyBytes( ipv4Address, ipv6Address, 12, 4 );
        return ipv6Address;
    }
    private byte[] getLocalIPv4AddressBytes( String serverAddress )
        throws SocketException
    {
        InetSocketAddress address = new InetSocketAddress( serverAddress,
                                                           Request.PCP_SERVER_PORT );
        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.connect( address );
        byte[] localAddressBytes = udpSocket.getLocalAddress().getAddress();
        udpSocket.close();
        return localAddressBytes;
    }
    private String getLocalIPv4AddressText( String serverAddress )
            throws SocketException
    {
        InetSocketAddress address = new InetSocketAddress( serverAddress,
                                                           Request.PCP_SERVER_PORT );
        DatagramSocket udpSocket = new DatagramSocket();
        udpSocket.connect( address );
        String localAddressText = udpSocket.getLocalAddress().getHostAddress();
        udpSocket.close();
        return localAddressText;
    }
    public long pcpMap( String serverAddress, byte protocol, int internalPort,
            int suggestedExternalPort, byte[] suggestedIPAddress,
            long requestedLifetime, AddressPortMapping mapping ) {
    	try {
			return _pcpMap(  serverAddress,  protocol,  internalPort,
			         suggestedExternalPort, suggestedIPAddress,
			         requestedLifetime,  mapping );
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return -1;
    }
    public long _pcpMap( String serverAddress, byte protocol, int internalPort,
                                          int suggestedExternalPort, byte[] suggestedIPAddress,
                                          long requestedLifetime, AddressPortMapping mapping )
        throws IOException
    {
        DatagramSocket udpSocket = new DatagramSocket( Request.PCP_CLIENT_PORT );
        udpSocket.connect( new InetSocketAddress( serverAddress, Request.PCP_SERVER_PORT ) );
        int port = udpSocket.getLocalPort(); 
        Request mapRequest = new Request();
        mapRequest.setVersion( Request.PCP_PROTOCOL_VERSION );
        mapRequest.setOpCode( Request.PCP_OPCODE_MAP );
        mapRequest.setRequestedLifetime( (int)requestedLifetime );
        mapRequest.setClientIPAddress(
                encodeIPv4toIPv6( getLocalIPv4AddressBytes( serverAddress ) ) );
        MapOpCodeInfoRequest r1 = new MapOpCodeInfoRequest();
        r1.setProtocol( protocol );
        r1.setInternalPort( internalPort );
        r1.setSuggestedExternalPort( suggestedExternalPort );
        r1.setSuggestedExternalIPAddress( suggestedIPAddress );
        mapRequest.setOpCodeInfo( r1 );
        byte[] udpData = mapRequest.getBytes();
        long startTime = sendUDP( udpData, serverAddress, udpSocket );
        byte[] data = new byte[ Request.PCP_MAX_MESSAGE_SIZE ];
        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
        udpSocket.receive( receivedPacket );
        long endTime = System.currentTimeMillis();
        InetSocketAddress originAddress = (InetSocketAddress)receivedPacket.getSocketAddress();
        logger.info( "UDP Received data from " + originAddress.getAddress().getHostAddress()
            + " : " + originAddress.getPort() );
        Response receivedMessage = new Response( data );
        logger.debug( "Response:\n" + receivedMessage.toString() );
        udpSocket.close();
        if( receivedMessage.getResultCode() == 0 )
        {
            if( mapRequest.getRequestedLifetime() > 0 )
            {
                mapping.setInternalPort( internalPort );
                MapOpCodeInfoResponse opCodeInfo
                    = (MapOpCodeInfoResponse)receivedMessage.getOpCodeInfo();
                mapping.setExternalPort( opCodeInfo.getAssignedExternalPort() );
                mapping.setInternalIPAddress( getLocalIPv4AddressText( serverAddress ) );
                byte[] mappedExternalAddress = opCodeInfo.getAssignedExternalIPAddress();
                String mappedExternalAddressText = "";
                for( int i = 12; i < mappedExternalAddress.length; i++ )
                {
                    mappedExternalAddressText
                        += String.format( "%d", (mappedExternalAddress[i] & 0xff) );
                    if( i < mappedExternalAddress.length - 1 )
                    {
                        mappedExternalAddressText += ".";
                    }
                }
                mapping.setExternalIPAddress( mappedExternalAddressText );
            }
        }
        else
        {
            logger.error( "PCP mapping unsuccessful. Error code: "
                    + receivedMessage.getResultCode() );
        }
        return endTime - startTime;
    }
    public long natpmpMap( String serverAddress, byte protocol, int internalPort,
                           int suggestedExternalPort, long requestedLifetime,
                           AddressPortMapping mapping )
        throws IOException
    {
        DatagramSocket udpSocket = new DatagramSocket( Request.PCP_CLIENT_PORT );
        net.ddp2p.common.network.natpmp.Request mapRequest
            = new net.ddp2p.common.network.natpmp.Request( net.ddp2p.common.network.natpmp.Request.RequestType.REQUEST_MAPPING );
        mapRequest.setVersion( net.ddp2p.common.network.natpmp.Request.NATPMP_PROTOCOL_VERSION );
        mapRequest.setOpCode( protocol );
        mapRequest.setInternalPort( internalPort );
        mapRequest.setSuggestedExternalPort( suggestedExternalPort );
        mapRequest.setRequestedLifetime( (int)requestedLifetime );
        byte[] udpData = mapRequest.getBytes();
        long startTime = sendUDP( udpData, serverAddress, udpSocket );
        byte[] data = new byte[ net.ddp2p.common.network.natpmp.Request.NATPMP_MAX_SIZE ];
        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
        udpSocket.receive( receivedPacket );
        long endTime = System.currentTimeMillis();
        String sourceAddress = receivedPacket.getSocketAddress().toString();
        int colonIndex = sourceAddress.indexOf( ':' ); 
        sourceAddress = sourceAddress.substring( 1, colonIndex );
        if( ! sourceAddress.equals( serverAddress ))
        {
            logger.error( "PCP response packet source address doesn't match server: "
                    + sourceAddress );
            return -1;
        }
        InetSocketAddress originAddress = (InetSocketAddress)receivedPacket.getSocketAddress();
        logger.info( "UDP Received data from " + originAddress.getAddress().getHostAddress()
            + " : " + originAddress.getPort() );
        net.ddp2p.common.network.natpmp.Response receivedMessage =
                        new net.ddp2p.common.network.natpmp.Response( data, net.ddp2p.common.network.natpmp.Response.ResponseType.MAPPING_RESPONSE );
        logger.debug( "Response:\n" + receivedMessage.toString() );
        udpSocket.close();
        if( receivedMessage.getResultCode() == 0 )
        {
            if( mapRequest.getRequestedLifetime() > 0 )
            {
                mapping.setInternalPort( internalPort );
                mapping.setExternalPort( receivedMessage.getMappedExternalPort() );
            }
        }
        else
        {
            logger.error( "NAT port mapping unsuccessful. Error code: "
                    + receivedMessage.getResultCode() );
        }
        return endTime - startTime;
    }
    public long natpmpRequestAddressInfo( String serverAddress, AddressPortMapping mapping )
        throws IOException
    {
        DatagramSocket udpSocket = new DatagramSocket( Request.PCP_CLIENT_PORT );
        net.ddp2p.common.network.natpmp.Request mapRequest
            = new net.ddp2p.common.network.natpmp.Request( net.ddp2p.common.network.natpmp.Request.RequestType.REQUEST_ADDRESS_INFO);
        byte[] udpData = mapRequest.getBytes();
        long startTime = sendUDP( udpData, serverAddress, udpSocket );
        logger.debug("Sent Client: "+mapRequest);
        byte[] data = new byte[ net.ddp2p.common.network.natpmp.Request.NATPMP_MAX_SIZE ];
        DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
        udpSocket.setSoTimeout(2000);
        udpSocket.receive( receivedPacket );
        long endTime = System.currentTimeMillis();
        InetSocketAddress originAddress = (InetSocketAddress)receivedPacket.getSocketAddress();
        logger.info( "UDP Received data from " + originAddress.getAddress().getHostAddress()
            + " : " + originAddress.getPort() );
        net.ddp2p.common.network.natpmp.Response receivedMessage =
                        new net.ddp2p.common.network.natpmp.Response( data, net.ddp2p.common.network.natpmp.Response.ResponseType.ADDRESS_INFO_RESPONSE );
        logger.debug( "Response:\n" + receivedMessage.toString() );
        udpSocket.close();
        if( receivedMessage.getResultCode() == 0 )
        {
            mapping.setInternalIPAddress( getLocalIPv4AddressText( serverAddress ) );
            byte[] mappedExternalAddress = receivedMessage.getExternalIPv4Address();
            String mappedExternalAddressText = "";
            for( int i = 0; i < mappedExternalAddress.length; i++ )
            {
                mappedExternalAddressText
                    += String.format( "%d", (mappedExternalAddress[i] & 0xff) );
                if( i < mappedExternalAddress.length - 1 )
                {
                    mappedExternalAddressText += ".";
                }
            }
            mapping.setExternalIPAddress( mappedExternalAddressText );
        }
        else
        {
            logger.error( "NAT external address request unsuccessful. Error code: "
                    + receivedMessage.getResultCode() );
        }
        return endTime - startTime;
    }
    /**
     * Sends to PCP server port
     * @param data
     * @param addressText
     * @param socket
     * @return
     * @throws UnknownHostException
     * @throws SocketException
     * @throws IOException
     */
    public long sendUDP( byte[] data, String addressText, DatagramSocket socket )
        throws UnknownHostException, SocketException, IOException
    {
        InetAddress address = InetAddress.getByName( addressText );
        DatagramPacket packet = new DatagramPacket( data, data.length, address,
                                                    Request.PCP_SERVER_PORT );
        long sendTime = System.currentTimeMillis();
        socket.send( packet );
        return sendTime;
    }
    public long sendTCP( byte[] data, String addressText, Socket socket )
        throws UnknownHostException, SocketException, IOException
    {
        InetSocketAddress address = new InetSocketAddress( addressText,
                                                           Request.PCP_SERVER_PORT);
        socket.connect( address );
        OutputStream outStream = socket.getOutputStream();
        long sendTime = System.currentTimeMillis();
        outStream.write( data );
        logger.info( "TCP Sent " + data.length + " bytes of data to "
            + address.getHostName() + ":" + Request.PCP_SERVER_PORT );
        return sendTime;
    }
    public long natpmpMapWrapper( String serverAddress, byte protocol, int internalPort,
            int suggestedExternalPort, long requestedLifetime,
            AddressPortMapping mapping ){
    	try {
			return _natpmpMapWrapper(  serverAddress,  protocol,  internalPort,
			         suggestedExternalPort,  requestedLifetime,
			         mapping);
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
		}
    	return -1;
    }
    public long _natpmpMapWrapper( String serverAddress, byte protocol, int internalPort,
                                 int suggestedExternalPort, long requestedLifetime,
                                 AddressPortMapping mapping )
        throws IOException
    {
        long addressFetchTime = natpmpRequestAddressInfo( serverAddress, mapping );
        long portMapTime = natpmpMap( serverAddress, protocol, internalPort, suggestedExternalPort,
                                      requestedLifetime, mapping );
        return addressFetchTime + portMapTime;
    }
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
            data = new byte[ MAX_MESSAGE_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            socket.receive( receivedPacket );
            endTime = System.currentTimeMillis();
            String sourceAddress = receivedPacket.getSocketAddress().toString();
            int colonIndex = sourceAddress.indexOf( ':' ); 
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
        String natpmpDeviceAddress= "10.0.0.1";//"192.168.1.1";
        int numRuns = 2;
        String outputFilePath = "output.txt";
        int portToMap = 2500;
        String protocol = "UDP";
        String peerIP = "0";
        int peerPort = 5350;
        String mappingProtocol = "NATPMP";
        Client_PCP client = new Client_PCP();
        AddressPortMapping mapping = new AddressPortMapping();
        if( args.length == 0 )
        {
        }
        else if( args.length < 5 )
        {
            System.out.println( "usage: Client <Device IP> <Peer IP> <Mapping Protocol> "
                    + "<Port to Map> <Protocol> <Output File> <Number of Runs>" );
        }
        else
        {
            natpmpDeviceAddress = args[0];
            peerIP = args[1];
            mappingProtocol = args[2];
            portToMap = Integer.parseInt( args[3] );
            protocol = args[4];
            outputFilePath = args[5];
            numRuns = Integer.parseInt( args[6] );
        }
        System.out.println( "Running with the following parameters:");
        System.out.println( "UPnP Device IP:\t" + natpmpDeviceAddress );
        System.out.println( "PeerIP Address: " + peerIP );
        System.out.println( "Mapping Protocol: " + mappingProtocol );
        System.out.println( "PortToMap:\t" + portToMap );
        System.out.println( "Protocol:\t" + protocol );
        System.out.println( "Output File:\t" + outputFilePath );
        System.out.println( "Number of Runs:\t" + numRuns );
        for( int i = 0; i < numRuns; i++ )
        {
            long mappingTime = 0;
            if( mappingProtocol.equalsIgnoreCase( "NATPMP" ) )
            {
                mappingTime = client.natpmpMapWrapper( natpmpDeviceAddress
                                                      ,net.ddp2p.common.network.natpmp.Request.NATPMP_OPCODE_UDP
                                                      ,net.ddp2p.common.network.natpmp.Request.NATPMP_CLIENT_PORT
                                                      ,portToMap
                                                      ,120
                                                      ,mapping);
            }
            else if( mappingProtocol.equalsIgnoreCase( "PCP"  ) )
            {
                mappingTime = client.pcpMap( natpmpDeviceAddress
                                            ,OpCodeInfo.PROTOCOL_UDP
                                            ,Request.PCP_CLIENT_PORT
                                            ,portToMap
                                            ,new byte[4]
                                            ,120
                                            ,mapping );
            }
            else
            {
            }
            long peerPingTime = 0;
            if( ! peerIP.equals( "0" ) )
            {
                try
                {
                    peerPingTime = client.pingPeer( peerIP, peerPort, mapping );
                    FileWriter writer = new FileWriter( outputFilePath, true );
                    writer.write( i + " " + mappingTime + " " + peerPingTime + " "
                            +  (mappingTime + peerPingTime) + "\n" );
                    writer.close();
                }
                catch( PeerCommunicationException e )
                {
                }
            }
            if( mappingProtocol.equalsIgnoreCase( "NATPMP" ) )
            {
                client.natpmpMap( natpmpDeviceAddress
                                 ,net.ddp2p.common.network.natpmp.Request.NATPMP_OPCODE_UDP
                                 ,net.ddp2p.common.network.natpmp.Request.NATPMP_CLIENT_PORT
                                 ,portToMap
                                 ,0
                                 ,mapping);
            }
            else if( mappingProtocol.equalsIgnoreCase( "PCP"  ) )
            {
                client.pcpMap( natpmpDeviceAddress
                        ,OpCodeInfo.PROTOCOL_UDP
                        ,Request.PCP_CLIENT_PORT
                        ,portToMap
                        ,new byte[4]
                        ,0
                        ,mapping );
            }
            else
            {
            }
            if( i < numRuns - 1 )
            {
                Thread.sleep( 32000 ); 
            }
        }
    }
}
