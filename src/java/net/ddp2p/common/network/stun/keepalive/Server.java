package net.ddp2p.common.network.stun.keepalive;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ddp2p.common.network.stun.stun.Attribute;
import net.ddp2p.common.network.stun.stun.KeepAliveTimer;
import net.ddp2p.common.network.stun.stun.MappedAddress;
import net.ddp2p.common.network.stun.stun.Message;
import net.ddp2p.common.network.stun.stun.ResponsePort;
import net.ddp2p.common.network.stun.stun.XorMappedAddress;
import net.ddp2p.common.util.Logger;

public class Server
{
    public enum Protocol
    {
         TCP
        ,UDP
    }

    Map<SessionKey, KeepAliveData> sessions = null;
    MessageWheel wheel = null;
    Thread wheelRunner = null;
    Logger logger = null;

    Thread patternedRunner = null;


    public static void main( String args[] )
        throws SocketException, IOException, InterruptedException
    {
        int listenerPort = Message.STUN_PORT;
        if( args.length > 0 && args[0] != null )
        {
            listenerPort = Integer.parseInt( args[0] );
        }

        DatagramSocket serverSocket = new DatagramSocket( listenerPort );

        Server server = new Server( serverSocket );
        server.wheelRunner = new Thread( server.wheel.getRunner() );
        server.wheelRunner.start();

//        PatternedTrafficClientRunner tr =
//                new PatternedTrafficClientRunner( serverSocket,
//                                                  "127.0.0.1",
//                                                  4000,
//                                                  "network_traffic//skype1.txt" );
//        tr.run();

        server.runServerUDP( serverSocket );
//        server.runServerTCP();
    }

    public Server( DatagramSocket serverSocket )
        throws SocketException
    {
        sessions = Collections.synchronizedMap( new HashMap<SessionKey, KeepAliveData>() );
        wheel = new MessageWheel( sessions, serverSocket );
        logger = new Logger( true, true, true, true );
    }

    public void runServerUDP( DatagramSocket serverSocket )
        throws IOException, InterruptedException
    {
        //DatagramSocket serverSocket = new DatagramSocket( Message.STUN_PORT );
        logger.info( "Starting UDP server listening on port " + serverSocket.getLocalPort() );
        while( true )
        {
            // UDP must be received all at once (as far as I can tell).
            byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
            DatagramPacket receivedPacket = new DatagramPacket( data, data.length );
            serverSocket.receive( receivedPacket );

            InetSocketAddress originAddress =
                (InetSocketAddress)receivedPacket.getSocketAddress();
            logger.info( "Received data from "
                + originAddress.getAddress().getHostAddress() + " : "
                + originAddress.getPort() );

            // Process the request message.
            Message requestMessage = new Message( data );
            processRequest( requestMessage, originAddress, null, serverSocket, Protocol.UDP );
        }
    }

    public void runServerTCP( ServerSocket tcpServerSocket )
        throws IOException, InterruptedException
    {
        // Listen for a TCP Message.
        //ServerSocket tcpServerSocket = new ServerSocket( Message.STUN_PORT );
        while( true )
        {
            // Process the TCP request message.
            byte[] data = new byte[ Message.STUN_MAX_IPV4_SIZE ];
            Socket clientSocket = tcpServerSocket.accept();
            InputStream inStream = clientSocket.getInputStream();
            int readSize =  inStream.read( data );

            // For now only reads once.
            if( readSize > 0 )
            {
                logger.info( "Received data from "
                    + clientSocket.getRemoteSocketAddress().toString() );
            }

            InetSocketAddress originAddress =
                (InetSocketAddress)clientSocket.getRemoteSocketAddress();
            logger.info( "Received data from "
                + originAddress.getAddress().getHostAddress() + " : " + originAddress.getPort() );

            // Process the request message.
            Message requestMessage = new Message( data );
            processRequest( requestMessage, originAddress, clientSocket, null, Protocol.TCP );
        }
        // TODO: Exit loop and close tcpServerSocket.
    }

    public void sendUDP( byte[] data, String addressText, int port, DatagramSocket socket )
        throws UnknownHostException, SocketException, IOException
    {
    	boolean noExistingSocket = !(socket == null);
        InetAddress address = InetAddress.getByName( addressText );

        DatagramPacket packet = new DatagramPacket( data, data.length, address, port );

        if( socket == null )
        {
        	socket = new DatagramSocket();
        }
        socket.send( packet );

        if( !noExistingSocket )
        {
        	socket.close();
        }

        logger.info( "UDP: Sent " + data.length + " bytes of data to "
            + addressText + " : " + port);
    }

    public void sendTCP( byte[] data, Socket socket )
        throws UnknownHostException, SocketException, IOException
    {
        OutputStream outStream = socket.getOutputStream();

        outStream.write( data );

        logger.info( "TCP: Sent " + data.length + " bytes of data to "
            + socket.getRemoteSocketAddress().toString() );

        socket.close();
    }

    public void processRequest( Message requestMessage, InetSocketAddress originAddress,
                                Socket tcpClientSocket, DatagramSocket udpSocket, Protocol protocol )
        throws SocketException, UnknownHostException, IOException, InterruptedException
    {
        short messageType = ByteBuffer.allocate( 2 ).put( requestMessage.getMessageType())
                                                    .getShort( 0 );
        switch ( messageType )
        {
            case Message.STUN_BINDING_REQUEST:
                processBindingRequest( requestMessage, originAddress, tcpClientSocket, udpSocket, protocol );
                break;
            case Message.STUN_CALC_KEEPALIVE_REQUEST:
                processCalcKeepAliveRequest( requestMessage, originAddress );
                break;
            case Message.STUN_CALC_KEEPALIVE_STOP_REQUEST:
                processCalcKeepAliveStopRequest( requestMessage, originAddress );
                break;
            case Message.STUN_CALC_KEEPALIVE_RESET_REQUEST:
                processCalcKeepAliveResetRequest( requestMessage, originAddress );
                break;
            case Message.STUN_GENTRAF_START_REQUEST:
                processGenTrafStartRequest( requestMessage, originAddress, udpSocket );
                break;
            case Message.STUN_GENTRAF_STOP_REQUEST:
                processGenTrafStopRequest( requestMessage, originAddress );
                break;
            default:
                break;
        }
    }

    // clientSocket is used only for TCP. Should be null for UDP.
    public void processBindingRequest( Message requestMessage, InetSocketAddress originAddress,
                                       Socket tcpClientSocket, DatagramSocket udpSocket, Protocol protocol )
        throws UnknownHostException, SocketException, IOException
    {
        // Build the response message.
    	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
        message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_RESPONSE );
        message.setTransactionID( requestMessage.getTransactionID() );

        // Get origin address and port.
        int port = originAddress.getPort();
        byte[] address = originAddress.getAddress().getAddress();

        MappedAddress ma = new MappedAddress();
        ma.setFamily( (byte)Attribute.TYPE_MAPPED_ADDRESS );
        ma.setPort( port );
        ma.setAddress( ByteBuffer.allocate( 4 ).put( address ).getInt( 0 ) );
        message.addAttribute(  "MappedAddress", ma );

        XorMappedAddress xma = new XorMappedAddress( ma );
        message.addAttribute(  "XorMappedAddress", xma );

        // Check if the response should be sent to a different port.
        Attribute responsePort = requestMessage.getAttribute( "ResponsePort" );
        if( responsePort != null && responsePort.getType() == Attribute.TYPE_RESPONSE_PORT )
        {
            port = ((ResponsePort)responsePort).getPort();
        }

        byte[] data = message.getBytes();
        switch( protocol )
        {
            case UDP:
                sendUDP( data, originAddress.getAddress().getHostAddress(), port, udpSocket );
                break;
            case TCP:
                if( tcpClientSocket == null )
                {
                    logger.error( "TCP client socket is null!" );
                }
                else
                {
                    sendTCP( data, tcpClientSocket );
                }
                break;
            default:
                logger.error( "Unknown protocol value." );
                break;
        }
    }

    // Same method for both INCREMENT and FIXED.
    public void processCalcKeepAliveRequest( Message requestMessage,
                                             InetSocketAddress originAddress )
    {
        // Check if there is a session with ID already in progress.
        KeepAliveTimer timer = new KeepAliveTimer( requestMessage.getAttribute( "KeepAliveTimer" )
                                                                 .getBytes() );
        // Build the session key from the id, address, and port.
        SessionKey key = new SessionKey( timer.getId(),
                                         originAddress.getAddress().getHostAddress(),
                                         originAddress.getPort() );

        KeepAliveData data = null;
        if( ! sessions.containsKey( key ) )
        {
            // Add the session to the table.
            data = new KeepAliveData();
            data.setSessionKey( key );
//            if(    timer.getIncrementType() == KeepAliveTimer.GEOMETRIC_INCREMENT
//                || timer.getUsesStarterMessage() == KeepAliveTimer.USES_STARTER_MESSAGE )
//            {
//                data.setK( 0 );
//            }
//            else
//            {
//                data.setK( 1 );
//            }
        	
        	data.setK( timer.getK() );
        	data.setInitialK( timer.getK() );
        	logger.debug( "NEW SESSION k: " + data.getK() );
            // These items will be constant for the session
            // (other than the time, which will be modified by the wheel only) so they
            // only need to be initialized when the session is created.
            // If "usesStarterMessage" is used, then the first send time of the KeepAlive message
            // will be immediate; after that the delay is used.

            data.setDeltaT( timer.getDeltaT() );
            data.setCurrentTime( timer.getTime() );
            data.setUsesStarterMessage( timer.getUsesStarterMessage() );
            data.setDirection( timer.getDirection() );
            data.setMaxRepeats( timer.getRepeats() );
            data.setMaxTime( timer.getMaxTime() );
            data.setActionAfterMax( timer.getActionAfterMax() );
            data.setServerAdjustsZ( timer.getServerAdjustsZ() );
            data.setZ( timer.getZ() );
            data.setzMultiplier( timer.getzMultiplier() );
            data.setInitialDeltaT( timer.getDeltaT() );
            data.setDeltaTAfterMax( timer.getDeltaTAfterMax() );

            sessions.put( key, data );

            // Add to the wheel.
            wheel.add( data );

            // Interrupt the wheel thread to allow the new item to be sent immediately.
            // If the thread is not waiting this will have no effect.
            wheelRunner.interrupt();
        }
        else
        {
            data = sessions.get( key );
        }

        // Set or update the incrementType.
        data.setIncrementType( timer.getIncrementType() ); // May possible change throughout the
                                                           // session.
    }

    // Some of the arguments may not be needed, but this is still in development.
    public void processCalcKeepAliveStopRequest( Message requestMessage,
                                                 InetSocketAddress originAddress )
    {
        // For now this uses the session id included in the KeepAliveTImer STUN object.
        KeepAliveTimer timer = new KeepAliveTimer( requestMessage.getAttribute( "KeepAliveTimer" )
                                                                 .getBytes() );
        // Build the session key from the id, address, and port.
        // DELETE THIS REMINDER: Issue is that new session with same id has different port!
        SessionKey key = new SessionKey( timer.getId(),
                                         originAddress.getAddress().getHostAddress(),
                                         originAddress.getPort() );

        // If this is a valid session, remove it from the main list and from the wheel.
        if( sessions.containsKey( key ) )
        {
            logger.debug( "[STOP] - " + System.currentTimeMillis()
                + " - Received STOP request for " + timer.getId()
                + " ... stopping session." );
            sessions.remove( key );
            wheel.remove( key );
        }
        else
        {
            logger.debug( "Cannot remove requested session because it does "
                + "not exist: " + timer.getId() );
        }
    }

    // Some of the arguments may not be needed, but this is still in development.
    public void processCalcKeepAliveResetRequest( Message requestMessage,
                                                 InetSocketAddress originAddress )
    {
        // For now this uses the session id included in the KeepAliveTImer STUN object.
        KeepAliveTimer timer = new KeepAliveTimer( requestMessage.getAttribute( "KeepAliveTimer" )
                                                                 .getBytes() );
        // Build the session key from the id, address, and port.
        SessionKey key = new SessionKey( timer.getId(),
                                         originAddress.getAddress().getHostAddress(),
                                         originAddress.getPort() );

        // If this is a valid session, reset the next send time.
        if( sessions.containsKey( key ) )
        {
            logger.debug( "[RESET] - " + System.currentTimeMillis()
                + " - Received RESET request for " + timer.getId()
                + " ... resetting session next send time." );

            KeepAliveData session = sessions.get( key );
            session.setNextSendTime( System.currentTimeMillis() + session.getCurrentTime() );

            wheelRunner.interrupt();
        }
        else
        {
            logger.debug( "Cannot reset requested session because it does "
                + "not exist: " + timer.getId() );
        }
    }

    public void processGenTrafStartRequest( Message requestMessage,
                                            InetSocketAddress originAddress
                                            ,DatagramSocket socket )
        throws InterruptedException
    {
        if( patternedRunner != null && patternedRunner.isAlive() )
        {
            // Depending on timing, it's possible that the last runner thread missed the
            // STOP signal before the new runner thread was started and caught it
            // instead.
            logger.debug( "patterned traffic runner still running. stopping." );
            patternedRunner.interrupt();
            patternedRunner.join();
            logger.debug( "patterned traffic runner stopping successfully." );
            patternedRunner = null;
        }

        // Start the pattern runner thread.
        String addressText = originAddress.getAddress().getHostAddress();
        int port = originAddress.getPort();
        logger.debug( "Starting traffic generation to " + addressText + ":" + port );
        patternedRunner =
                new Thread(
                        new PatternedTrafficClientRunner(socket,
                                                         addressText,
                                                         port,
                                                         "network_traffic//ddp2p_60s.txt") );
        patternedRunner.start();
    }

    public void processGenTrafStopRequest( Message requestMessage,
                                           InetSocketAddress originAddress )
        throws InterruptedException
    {
        if( patternedRunner != null && patternedRunner.isAlive() )
        {
            // Depending on timing, it's possible that the last runner thread missed the
            // STOP signal before the new runner thread was started and caught it
            // instead.
            logger.debug( "patterned traffic runner still running. stopping." );
            patternedRunner.interrupt();
            patternedRunner.join();
            logger.debug( "patterned traffic runner stopping successfully." );
            patternedRunner = null;
        }
        else
        {
            logger.debug( "Cannot STOP. Patterned traffic runner is not active." );
        }
    }

    public void sendPatternedNetworkTraffic( DatagramSocket socket, String addressText, int port,
                                             String patternFilePath, boolean repeatsForever )
        throws IOException, InterruptedException
    {
        List<Integer> delays = new ArrayList<Integer>();
        BufferedReader reader =  null;
        int sentCount = 0;
        try
        {
            // Load all of the values first for a more accurate recreation.
            reader = new BufferedReader( new FileReader( patternFilePath ) );
            String currentLine;
            logger.debug( "Loading patterned traffic data from \"" + patternFilePath );
            while( ( currentLine = reader.readLine() ) != null )
            {
                delays.add( Integer.parseInt( currentLine) );
            }
            logger.debug( "Traffic data size is " + delays.size() + " messages." );

            do
            {
                // Build a binding request message to use for the tests.
            	net.ddp2p.common.network.stun.stun.Message message = new net.ddp2p.common.network.stun.stun.Message();
                message.setMessageType( net.ddp2p.common.network.stun.stun.Message.STUN_BINDING_RESPONSE );
                message.generateTransactionID();
                byte[] udpData = message.getBytes();

                // Send the traffic at the appropriate intervals.
                for( Integer delay : delays )
                {
                    if( Thread.currentThread().isInterrupted() )
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if ( delay > 0 )
                    {
                        Thread.sleep( delay );
                    }

                    sendUDP( udpData, addressText, port, socket );
//                    logger.debug( "PATTERN - sending message after " + delay + " ms." );
                    sentCount++;
                }

                logger.debug( "End of traffic data.  Repeat: " + repeatsForever );
            } while( repeatsForever );
        }
        catch( IOException e )
        {
            logger.error( "Unexpected IO Exception: " + e );
            throw e;
        }
        finally
        {
            logger.debug( "TRAFFIC SENT COUNT: " + sentCount );
            if( reader != null )
            {
                reader.close();
            }
        }
    }

    private static class PatternedTrafficClientRunner implements Runnable
    {
        private DatagramSocket socket = null;
        private String address = null;
        private int port = 0;
        private SynchronizedTime sharedTime = null;
        private String patternFilePath = null;

        public PatternedTrafficClientRunner( DatagramSocket socket, String address, int port,
                                             String patternFilePath )
        {
            this.socket = socket;
            this.address = address;
            this.port = port;
            this.patternFilePath = patternFilePath;

            this.sharedTime = new SynchronizedTime(); // NOT USED.
        }

        @Override
        public void run()
        {
            try
            {
                Server server = new Server( socket );
                server.sendPatternedNetworkTraffic( socket, address, port, patternFilePath, true );
            }
            catch( IOException e )
            {
                System.out.println( "Unexpected Exception: " + e );
                return;
            }
            catch( InterruptedException e )
            {
                System.out.println( "Patterned Traffic Generation Interrupted: " + e );
                return;
            }
        }
    }

}

