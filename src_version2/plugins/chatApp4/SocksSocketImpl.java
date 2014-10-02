//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.Socket;
import java.net.SocketImpl;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.Properties;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.FileDescriptor;
import java.net.SocketAddress;
/**
 *
 *  SocksSocketImpl: A Socket implementation class support SOCKS4A & SOCKS5
 *
 *  @version 1.1a,
 *
 *  @author  Jeeva S (vavjeeva@yahoo.com)
 *
 *
 */

/**
 *  SocksSocketImpl: A Socket Implementation class support SOCKS4A & SOCKS5
 *
 *  <A HREF="http://www.socks.nec.com/socksprot.html">SOCKS References:</A><BR>
 *	 <DD>[1] <A HREF="http://www.socks.nec.com/protocol/socks4.protocol">SOCKS 4 Protocol</A><BR>
 *	 <DD>[2] <A HREF="http://www.socks.nec.com/protocol/socks4a.protocol">SOCKS 4A: A Simple Extension to SOCKS 4 Protocol</A><BR>
 *	 <DD>[3] <A HREF="//http://www.socks.nec.com/rfc/rfc1928.txt">RFC1928 SOCKS Protocol Version 5</A><BR>
 *	 <DD>[4] <A HREF="//http://www.socks.nec.com/rfc/rfc1929.txt">RFC1929 Username/Password Authentication for SOCKS V5</A></DL></DD>
 *	 <DD>[5] <A HREF="//http://www.socks.nec.com/draft/draft-ietf-aft-socks-pro-v5-04.txt">SOCKS Protocol Version 5 (22 Feb 1999)</A></DL></DD>
 */

class SocksSocketImpl extends SocketImpl implements SocksSocketConstants
{
	/*
	 *  a boolean indicating whether this is a stream socket or a datagram socket.
	 *  If the stream argument is true, this creates a stream socket. If the stream argument is
	 *  false, it creates a datagram socket. 
	 */
	private boolean stream=true;
	/*
	 * The Socket between local host and SOCKS proxy server when there is a proxy server.
	 * The Socket between local host and remote host when there isn't a proxy server.
	 */
	private Socket clientSocket=null;
	/*
	 * the direct DatagramSocket communication between local host and remote host.
	 */
	private DatagramSocket clientDatagramSocket=null;

	/*
	 * The IP address of the SOCKS proxy server.
	 */
	private InetAddress proxyAddress=null;
	/**
	 * Returns the value of this socket's <code>proxy address</code> field.
	 *
	 * @return  the value of this socket's <code>proxy address</code> field.
	 * @see	 java.net.SocketImpl#address
	 */
	protected InetAddress getProxyAddress()
	{
		return proxyAddress;
	}
	/*
	 * The port number on the SOCKS proxy serer.
	 */
	private int proxyPort=SOCKS_PORT;
	/**
	 * Returns the value of this socket's <code>proxy port</code> field.
	 *
	 * @return  the value of this socket's <code>proxy port</code> field.
	 */
	protected int getProxyPort()
	{
		return proxyPort;
	}

	/*
	 * The IP address of the local end of this socket.
	 * instance variable for SO_BINDADDR
	 */
	/*
	 */
	private InetAddress localAddress=null;
	/**
	 * Returns the value of this socket's <code>proxy address</code> field.
	 *
	 * @return  the value of this socket's <code>proxy address</code> field.
	 * @see	 java.net.SocketImpl#address
	 */
	protected InetAddress getLocalAddress()
	{
		return localAddress;
	}
	/**
	 * Returns the value of this socket's <code>localport</code> field.
	 *
	 * @return  the value of this socket's <code>localport</code> field.
	 * @see	 java.net.SocketImpl#localport
	 */
	protected int getLocalPort()
	{
		return localport;
	}
	/**
	 * Returns the value of this socket's <code>address</code> field.
	 *
	 * @return  the value of this socket's <code>address</code> field.
	 * @see	 java.net.SocketImpl#address
	 */
	protected InetAddress getInetAddress()
	{
		return address;
	}

	/**
	 * Returns the value of this socket's <code>port</code> field.
	 *
	 * @return  the value of this socket's <code>port</code> field.
	 * @see	 java.net.SocketImpl#port
	 */
	protected int getPort()
	{
		return port;
	}


	/*
	 * An ojbect for output buffer
	 */
	private ByteArrayOutputStream outputBuffer;

	protected SocksSocketImpl(){}
	protected SocksSocketImpl(boolean stream)
	{
		this.stream=stream;
	}
	
	protected void sendUrgentData(int data)
	{
		/*********Do Nothing ********/	
	}
	
	protected SocksSocketImpl(String proxyHost,int proxyPort)throws java.net.UnknownHostException
	{
		this(InetAddress.getByName(proxyHost),proxyPort,null,true);
	}
	protected SocksSocketImpl(InetAddress proxyAddress,int proxyPort,Properties properties)
	{
		this(proxyAddress,proxyPort,properties,true);
	}
	protected SocksSocketImpl(InetAddress proxyAddress,int proxyPort,Properties properties,boolean stream)
	{
		this.proxyAddress=proxyAddress;
		this.proxyPort=proxyPort;
		this.stream=stream;
		if(proxyAddress!=null)
			outputBuffer= new ByteArrayOutputStream(1024);
		if(properties!=null)
			initProperties(properties);
		if(!stream)//Set version to SOCKS_VERSION_5 since SOCKS_VERSION_4A cann't support datagram socket
			version=SOCKS_VERSION_5;
	}
	
	private void initProperties(Properties properties)
	{
		String value=properties.getProperty(SocksSocket.USERNAME);
		if(value!=null)username=value.getBytes();
		value=properties.getProperty(SocksSocket.PASSWORD);
		if(value!=null)password=value.getBytes();
		value=properties.getProperty(SocksSocket.VERSION);
		if(value!=null)
		{
			try
			{
				version=Integer.parseInt(value)==SOCKS_VERSION_5?SOCKS_VERSION_5:SOCKS_VERSION_4A;
			}
			catch(NumberFormatException nfe){}
		}
	}



	/**
	 * Creates either a stream or a datagram socket. 
	 * Creates a socket with a boolean that specifies whether this
	 * is a stream socket (true) or an unconnected UDP socket (false).
	 *
	 * @param	stream   if <code>true</code>, create a stream socket;
	 *					  otherwise, create a datagram socket.
	 * @exception  IOException  if an I/O error occurs while creating the
	 *			   socket.
	 */
	protected void create(boolean stream) throws IOException
	{
		//Nothing since it's always a socket connection between local host and proxy server.
	}

	/**
	 * Binds this socket to the specified port number flag the specified host. 
	 *
	 * @param	address   the IP address of the specified host.
	 * @param	port   the port number.
	 * @exception  IOException  if an I/O error occurs when binding this socket.
	 */
	protected void bind(InetAddress address, int port)throws IOException
	{
		this.localAddress=address;
		this.localport=port;
	}

	/**
	 * Connects this stream socket to the specified port flag the named host. 
	 *
	 * @param	host   the name of the remote host.
	 * @param	port   the port number.
	 * @exception  IOException  if an I/O error occurs when connecting to the
	 *			   remote host.
	 */
	protected void connect(String host, int port)throws UnknownHostException, IOException
	{
		try
		{
			if(proxyAddress==null)//Direct Socket Connection
			{
				clientSocket=new Socket(InetAddress.getByName(host),port,localAddress,localport);
				localAddress=clientSocket.getLocalAddress();
			}
			else//SOCKS Connection
			{
				initRemoteHost(host,port);
				doSOCKSConnect();
			}
		}
		catch (IOException e)
		{
			close();
			throw e;
		}
	}

	/**
	 * Connects this stream or datagram socket to the specified port number flag the specified host.
	 *
	 * @param	address   the IP address of the remote host.
	 * @param	port	 the port number.
	 * @exception  IOException  if an I/O error occurs when attempting a
	 *			   connection.
	 */
	protected void connect(InetAddress address, int port) throws IOException
	{
		try
		{
			if(stream)// Stream socket connection
			{
				boolean isConnected=clientSocket!=null;
				if(isConnected)return;
				if(proxyAddress==null)// Direct Stream Socket Connection
				{
					clientSocket=new Socket(address,port,localAddress,localport);
					localAddress=clientSocket.getLocalAddress();
					localport=clientSocket.getLocalPort();
				}
				else//SOCKS Connection
				{
					initRemoteHost(address,port);
					doSOCKSConnect();
				}
			}
			else//datagram socket connection
			{
				this.address=address;
				this.port=port;
				boolean isConnected=clientDatagramSocket!=null;
				if(!isConnected)
				{
					clientDatagramSocket=new DatagramSocket(localport,localAddress);
					localAddress=clientDatagramSocket.getLocalAddress();
					localport=clientDatagramSocket.getLocalPort();
				}
				if(proxyAddress==null)// Direct datagram socket connection
				{
					clientDatagramSocket.connect(address,port);
				}
				else//SOCKS Connection
				{
					initRemoteHost(address,port);
					if(!isConnected)
						doSOCKSConnect();
				}
			}
		}
		catch (IOException e)
		{
			close();
			throw e;
		}
	}
	
	protected void connect(SocketAddress address, int port) throws IOException
	{
		/**********Do Nothing **********/	
	}
	/**
	 * Returns an input stream for this socket.
	 *
	 * @return	 a stream for reading from this socket.
	 * @exception  IOException  if an I/O error occurs when creating the
	 *			   input stream.
	 */
	protected synchronized InputStream getInputStream() throws IOException
	{
		if(clientSocket!=null)
			return clientSocket.getInputStream();
		else
			throw new IOException("Error: Try to access an unconnected or closed Socks connection.");
	}

	/**
	 * Returns an output stream for this socket.
	 *
	 * @return	 an output stream for writing to this socket.
	 * @exception  IOException  if an I/O error occurs when creating the
	 *			   output stream.
	 */
	protected synchronized OutputStream getOutputStream() throws IOException
	{
		if(clientSocket!=null)
			return clientSocket.getOutputStream();
		else
			throw new IOException("Error: Try to access an unconnected or closed Socks connection.");
	}

	/**
	 * Returns the number of bytes that can be read from this socket
	 * without blocking.
	 *
	 * @return	 the number of bytes that can be read from this socket
	 *			 without blocking.
	 * @exception  IOException  if an I/O error occurs when determining the
	 *			   number of bytes available.
	 */
	protected synchronized int available() throws IOException
	{
		return getInputStream().available();
	}


	/**
	 * Set the appropriate SOCKS version for the specific SOCKS proxy serer.
	 */
	private int version=SOCKS_VERSION_5;

	/**
	 * Set username for the specific SOCKS proxy serer.
	 */
	private byte[] username=null;

	/**
	 * Set password for the specific SOCKS proxy serer.
	 */
	private byte[] password=null;

	/**
	 * Set the IP address and port number of the remote end of this socket.
	 */private byte[] remoteHost=null;
	private byte[] remotePort=null;
	private byte[] remoteAddress=null;
	private int remoteAddressType;
	private void initRemoteHost(String host,int port)throws UnknownHostException
	{
		try
		{
			this.port = port;
			remotePort=getPort(port);
			remoteAddressType=IP_V4;
			remoteHost=host.getBytes();
			this.address= InetAddress.getByName(host);
			remoteAddress=getAddress(address);
		}
		catch(Exception uhe)//Use it to catch security exception
//		catch(UnknownHostException uhe)
		{
			switch(version)
			{
			case SOCKS_VERSION_4A:
				remoteAddress=new byte[4];
				//Such a destination IP address is inadmissible
//				remoteAddress[0]=remoteAddress[1]=remoteAddress[2]=0;
				remoteAddress[3]=(byte)0xFF;
				break;
			case SOCKS_VERSION_5:
				remoteAddress=remoteHost;
				remoteAddressType=DOMAINNAME;
				break;
			}
		}
	}
	private void initRemoteHost(InetAddress address,int port)
	{
		this.address= address;
		this.port = port;
		remoteAddress=getAddress(address);
		remotePort=getPort(port);
		remoteAddressType=IP_V4;
	}
	private static final byte[] getAddress(InetAddress ia)
	{
		if(ia==null)
			return new byte[4];
		else
			return ia.getAddress();

/*		int addressIntegerValue=ia.hashCode();
		byte[] address=new byte[4];
		for(int i=3;i>=0;i--)
		{
			address[i]=(byte)(addressIntegerValue&0xFF);
			addressIntegerValue>>>=8;
		}
		return address;*/
	}
	private static final byte[] getAddress(int addressType, byte[] b, int offset)
	{
		int length;
		switch(addressType)
		{
		case DOMAINNAME:
			length=b[offset];
			offset++;
			break;
		case IP_V4:
			length=4;
			break;
//		case IP_V6:
		default://
			length=16;
			break;
		}
		byte[] address=new byte[length];
		for(int i=0;i<length;i++,offset++)
			address[i]=b[offset];
		return address;
	}
	private static final byte[] getPort(int p)
	{
		byte[] port=new byte[2];
		port[0]=(byte)((p>>>8)&0xFF);
		port[1]=(byte)(p&0xFF);
		return port;
	}
	private static final byte[] getPort(byte[] b, int offset)
	{
		byte[] port=new byte[2];
		port[0]=b[offset];
		port[1]=b[offset+1];
		return port;
	}
	private static final int getPortValue(
		byte[] b,int offset)
	{
		int port=b[offset]<0?(short)b[offset]+256:b[offset];
		port<<=8;
		offset++;
		port+=b[offset]<0?(short)b[offset]+256:b[offset];
		return port;
	}
	private static final InetAddress getInetAddress(
		int addressType,byte[] b,int offset)throws IOException
	{
		String host;
		switch(addressType)
		{
		case IP_V4:
			StringBuffer destinationHost=new StringBuffer(16);
			for(int i=offset;i<offset+4;i++)
			{
				destinationHost.append(b[i]<0?(short)(b[i]+256):(short)b[i]);
				if(i<offset+3)
					destinationHost.append('.');
			}
			host=destinationHost.toString();
			break;
		case DOMAINNAME:
			int length=b[offset++];
			if(length<0)length+=256;
			host=new String(b,offset,length);
			break;
		case IP_V6:
			throw new IOException("Error: IPV6 is not supported.");
		default:
			throw new IOException("Error: Unknown IP address type.");
		}
		return InetAddress.getByName(host);
	}




	/**
	 * Setup a SOCKS connection to the remote host through the SOCKS server
	 */
	private synchronized void doSOCKSConnect()throws IOException
	{
		if(remoteAddress==null || remotePort==null)
			throw new IOException("Error: You Cann't connect without defining the IP address and port of remote host.");

		for(int i=0;i<5;i++)//Try fivet time since the proxy server is possible very busy.
		{
			try
			{
				if(stream)
				{
					clientSocket=new Socket(proxyAddress,proxyPort,localAddress,localport);
					localAddress=clientSocket.getLocalAddress();
					localport=clientSocket.getLocalPort();
				}
				else//DatagramSocket
				{
					clientSocket=new Socket(proxyAddress,proxyPort,localAddress,0);
				}
				clientSocket.setSoTimeout(timeout);
				break;
			}
			catch(IOException e)
			{
				if(i<4)
				{
					try
					{
						Thread.sleep(200);
					}
					catch (InterruptedException ie){} 
				}
				else
				{
					close();
					throw e;
				}
			}
		}

		switch(version)
		{
		case SOCKS_VERSION_4A:
			request_of_V4A(COMMAND_CONNECT,remoteAddress,remotePort,remoteHost);
			reply_of_V4A(false);
			break;
		case SOCKS_VERSION_5:
			methodSelection_of_V5();
			if(stream)
			{
				request_of_V5(COMMAND_CONNECT,NULL,remoteAddressType,remoteAddress,remotePort);
				reply_of_V5(COMMAND_CONNECT,false);
			}
			else
			{
//				request_of_V5(COMMAND_UDP_ASSOCIATE,INTERFACE_REQUEST,remoteAddressType,remoteAddress,remotePort);
				request_of_V5(COMMAND_UDP_ASSOCIATE,NULL,remoteAddressType,remoteAddress,remotePort);
				reply_of_V5(COMMAND_UDP_ASSOCIATE,false);
			}
			break;
		}
	}
	private void request_of_V4A(int command,
		byte[] destinationAddress,byte[] destinationPort,
		byte[] destinationHost)throws IOException
	{
		outputBuffer.reset();
		outputBuffer.write(SOCKS_VERSION_4A);
		outputBuffer.write(command);
		outputBuffer.write(destinationPort);
		outputBuffer.write(destinationAddress);
		if(password!=null)
			outputBuffer.write(password);//It's the USERID in SOCKS4A.
		outputBuffer.write(NULL);//terminate in NULL byte
		if(destinationAddress[0]==0)//Such a destination IP address is inadmissible
		{
			outputBuffer.write(destinationHost);
			outputBuffer.write(NULL);//terminate in NULL byte
		}

		OutputStream out=clientSocket.getOutputStream();
		outputBuffer.writeTo(out);
		out.flush();
	}

	private int serverBoundAddressType;
	private InetAddress serverBoundAddress=null;
	private int serverBoundPort=0;
	private int remoteBoundAddressType;
	private byte[] remoteBoundAddress=null;
	private byte[] remoteBoundPort=null;
	/**
	 * Set command for the specific SOCKS proxy serer.
	 */
	private int command=COMMAND_CONNECT;

	private void reply_of_V4A(boolean isGetRemoteBoundParamaters)throws IOException
	{
		int count;
		String errorMessage=null;
		byte[] buffer=new byte[8];
		InputStream in=clientSocket.getInputStream();
		while((count= in.read(buffer)) >= 0)
		{
			if(count==0)
				continue;
			else if(count<8 || buffer[0]!=NULL)
				errorMessage="failed to parse the malformed reply from the socks server.";
			else
			{
				switch(buffer[1])
				{
				case REQUEST_GRANTED:
					if(isGetRemoteBoundParamaters)
					{
						remoteBoundPort=getPort(buffer,2);
						remoteBoundAddress=getAddress(IP_V4,buffer,4);
					}
					else
					{
						serverBoundPort=getPortValue(buffer,2);
						serverBoundAddress=getInetAddress(IP_V4,buffer,4);
					}
					break;
				case REQUEST_REJECTED:
					errorMessage="request rejected or failed";
					break;
				case REQUEST_REJECTED_NO_IDENTD:
					errorMessage="request rejected becasue SOCKS server cannot connect to identd on the client";
					break;
				case REQUEST_REJECTED_DIFF_IDENTS:
					errorMessage="request rejected because the client program and identd report different user-ids";
					break;
				}
			}
			if(errorMessage!=null)
			{
				close();
				throw new IOException("("+proxyAddress.getHostAddress()+":"+proxyPort+") "+errorMessage);
			}
			break;
		}

		if(command!=COMMAND_BIND)return;
		{
		}
	}private String Username_AND_Password_Authentication_of_V5()throws IOException
	{
		outputBuffer.reset();
		outputBuffer.write(0x01);
		if(username==null)
			outputBuffer.write(0x0);
		else
		{
			outputBuffer.write(username.length);
			outputBuffer.write(username);
		}
		if(password==null)
			outputBuffer.write(0x0);
		else
		{
			outputBuffer.write(password.length);
			outputBuffer.write(password);
		}

		OutputStream out=clientSocket.getOutputStream();
		outputBuffer.writeTo(out);
		out.flush();

		int count;
		String errorMessage=null;
		byte[] buffer=new byte[2];
		InputStream in=clientSocket.getInputStream();
		while((count= in.read(buffer)) >= 0)
		{
			if(count==0)
				continue;
			else if(count<2 || buffer[0]!=0x01)
			{
				errorMessage="failed to parse the authentication reply from the socks server.";
			}
			else if(buffer[1]!=0x0)
			{
				errorMessage="failed to through the authentication reply from the socks server.";
			}
			if(errorMessage!=null)
			{
				close();
				throw new IOException("("+proxyAddress.getHostAddress()+":"+proxyPort+") "+errorMessage);
			}
			break;
		}
		return errorMessage;
	}
	private void methodSelection_of_V5()throws IOException
	{
		outputBuffer.reset();
		outputBuffer.write(SOCKS_VERSION_5);

		//Now only complement the negotiation methods of
		// NO_AUTHENTICATION_REQUIRED and USERNAME_AND_PASSWORD
		outputBuffer.write(2);//the number of method identifier octets that appear in the METHODS field
		outputBuffer.write(NO_AUTHENTICATION_REQUIRED);
//		outputBuffer.write(GSSAPI);
		outputBuffer.write(USERNAME_AND_PASSWORD);
//		outputBuffer.write(CHAP);

		OutputStream out=clientSocket.getOutputStream();
		outputBuffer.writeTo(out);
		out.flush();

		int method;
		if(username!=null || password!=null)
			method=USERNAME_AND_PASSWORD;
		else
			method=NO_AUTHENTICATION_REQUIRED;

		int count;
		String errorMessage=null;
		byte[] buffer=new byte[2];
		InputStream in=clientSocket.getInputStream();
		while((count= in.read(buffer)) >= 0)
		{
			if(count==0)
				continue;
			else if(count<2 || buffer[0]!=SOCKS_VERSION_5)
				errorMessage="failed to parse the reply from the socks server.";
			else
			{
				switch(buffer[1])
				{
				case NO_AUTHENTICATION_REQUIRED:
					break;
				case GSSAPI:
					errorMessage="GSSAPI negotiation hasn't been still complemented.";
					break;
				case USERNAME_AND_PASSWORD:
					errorMessage=Username_AND_Password_Authentication_of_V5();
					break;
				case CHAP:
					errorMessage="CHAP negotiation hasn't been still complemented.";
					break;
				case (byte)NO_ACCEPTABLE_METHODS:
					errorMessage="No acceptable negotiation method.";
					break;
				default:
					errorMessage="The negotiation method with a METHOD number of "+(int)buffer[1]+" hasn't been still complemented.";
					break;
				}
			}
			if(errorMessage!=null)
			{
				close();
				throw new IOException("("+proxyAddress.getHostAddress()+":"+proxyPort+") "+errorMessage);
			}
			break;
		}
	}
	private void request_of_V5(int command,int flag,
		int addressType,byte[] destinationAddress,byte[] destinationPort)throws IOException
	{
		outputBuffer.reset();

		outputBuffer.write(SOCKS_VERSION_5);
		outputBuffer.write(command);
		outputBuffer.write(flag);//command dependent flag (defaults to X'00')
		if(command!=COMMAND_UDP_ASSOCIATE)
		{
			outputBuffer.write(addressType);
			if(addressType==DOMAINNAME)
				outputBuffer.write(destinationAddress.length);
			outputBuffer.write(destinationAddress);
			outputBuffer.write(destinationPort);
		}
		else
		{
			outputBuffer.write(IP_V4);
			outputBuffer.write(getAddress(localAddress));
			outputBuffer.write(getPort(localport));
		}

		OutputStream out=clientSocket.getOutputStream();
		outputBuffer.writeTo(out);
		out.flush();
	}
	private void reply_of_V5(int command,boolean isGetRemoteBoundParamaters)throws IOException
	{
		int count;
		String errorMessage=null;
		byte[] buffer=new byte[64];
		InputStream in=clientSocket.getInputStream();
		while((count= in.read(buffer)) >= 0)
		{
			if(count==0)
				continue;
			else if(count<8 || buffer[0]!=SOCKS_VERSION_5)
				errorMessage="failed to parse the reply from the socks server.";
			else
			{
				switch(buffer[1])
				{
				case SUCCEEDED:
					int tempBoundAddressType=buffer[3];
					int offset=4;
					InetAddress tempBoundAddress=getInetAddress(tempBoundAddressType,buffer,offset);
					switch(tempBoundAddressType)
					{
					case DOMAINNAME:
						offset+=(buffer[offset+1]<0?buffer[offset+1]+256:buffer[offset+1])+1;
						break;
					case IP_V4:
						offset+=4;
						break;
//					case IP_V6:
					default:
						offset+=16;
					}

					int tempBoundPort=getPortValue(buffer,offset);
					if(isGetRemoteBoundParamaters)
					{
//						remoteBoundAddressType=tempBoundAddressType;
//						remoteBoundAddress=tempBoundAddress;
//						remoteBoundPort=tempBoundPort;
					}
					else
					{
						serverBoundAddressType=tempBoundAddressType;
						serverBoundAddress=tempBoundAddress;
						serverBoundPort=tempBoundPort;
					}
					break;
				case FAILURE:
					errorMessage="general SOCKS server failure";
					break;
				case NOT_ALLOWED:
					errorMessage="connection not allowed by ruleset";
					break;
				case NETWORK_UNREACHABLE:
					errorMessage="Network unreachable";
					break;
				case HOST_UNREACHABLE:
					errorMessage="Host unreachable";
					break;
				case REFUSED:
					errorMessage="Connection refused";
					break;
				case TTL_EXPIRED:
					errorMessage="TTL expired";
					break;
				case COMMAND_NOT_SUPPORTED:
					errorMessage="Command not supported";
					break;
				case ADDRESS_TYPE_NOT_SUPPORTED:
					errorMessage="Address type not supported";
					break;
				case INVALID_ADDRESS:
					errorMessage="Invalid address";
					break;
				default:
					errorMessage="unknown reply code ("+(int)buffer[1]+")";
					break;
				}
			}
			if(errorMessage!=null)
			{
				close();
				throw new IOException("("+proxyAddress.getHostAddress()+":"+proxyPort+") "+errorMessage);
			}
			break;
		}
	}

	/**
	 * Close the SocksSocket() connection to the SOCKS server.
	 *
	 * @exception  IOException  if an I/O error occurs when closing this socket.
	 */
	protected void close() throws IOException
	{
		if (clientSocket != null)
		{
			try
			{
				clientSocket.close();
			}
			catch(java.io.IOException e)
			{
			}
			clientSocket=null;
		}
		else if(clientDatagramSocket!=null)
		{
			clientDatagramSocket.close();
			clientDatagramSocket=null;
		}

	}

	/*
	 * instance variable for SO_TIMEOUT
	 */
	private int timeout=15000;//A timeout of zero is interpreted as an infinite timeout.
	/*
	 * instance variable for TCP_NODELAY
	 */
	private boolean tcpNoDelay=false;
	/*
	 * instance variable for SO_LINGER
	 */
	private int soLinger=-1;
	/*
	 * instance variable for SO_SNDBUF
	 */
	private int soSndBuf=0;
	/*
	 * instance variable for SO_SNDBUF
	 */
	private int soRCVndBuf=0;


	/**
	 * Sets the maximum queue length for incoming connection indications 
	 * (a request to connect) to the <code>count</code> argument. If a 
	 * connection indication arrives when the queue is full, the 
	 * connection is refused. 
	 *
	 * @param	backlog   the maximum length of the queue.
	 * @exception  IOException  if an I/O error occurs when creating the queue.
	 */
	protected synchronized void listen(int backlog) throws IOException
	{
	}

	/**
	 * Accepts a connection. 
	 *
	 * @param	s   the accepted connection.
	 * @exception  IOException  if an I/O error occurs when accepting the
	 *			   connection.
	 */
	protected synchronized void accept(SocketImpl s) throws IOException
	{
	}



	/**
	 * Returns the address and port of this socket as a <code>String</code>.
	 *
	 * @return  a string representation of this socket.
	 */
	public String toString()
	{
		return "SocksSocket[addr=" + getInetAddress() +
			",port=" + getPort() +
			",localaddr=" 
			+(getLocalAddress()!=null?getLocalAddress().toString():"127.0.0.1")
			+",localport=" + getLocalPort()
			+(getProxyAddress()!=null?
			",proxyddr=" + getProxyAddress() +
			",proxyport=" + getProxyPort():"")
			+"]";
	}

	/**
	 * Cleans up if the user forgets to close it.
	 */
	protected void finalize() throws IOException
	{
		close();
	}


	/**
	 * Fetch the value of an option.
	 * Binary options will return java.lang.Boolean(true)
	 * if enabled, java.lang.Boolean(false) if disabled, e.g.:
	 * <BR><PRE>
	 * SocketImpl s;
	 * ...
	 * Boolean noDelay = (Boolean)(s.getOption(TCP_NODELAY));
	 * if (noDelay.booleanValue()) {
	 *	 // true if TCP_NODELAY is enabled...
	 * ...
	 * }
	 * </PRE>
	 * <P>
	 * For options that take a particular type as a parameter,
	 * getOption(int) will return the paramter's value, else
	 * it will return java.lang.Boolean(false):
	 * <PRE>
	 * Object o = s.getOption(SO_LINGER);
	 * if (o instanceof Integer) {
	 *	 System.out.print("Linger time is " + ((Integer)o).intValue());
	 * } else {
	 *   // the true type of o is java.lang.Boolean(false);
	 * }
	 * </PRE>
	 *
	 * @throws SocketException if the socket is closed
	 * @throws SocketException if <I>optID</I> is unknown along the
	 *		 protocol stack (including the SocketImpl)
	 */
	public Object getOption(int opt) throws SocketException
	{
		switch (opt)
		{
		case SO_TIMEOUT:
			if(clientSocket!=null)
				timeout=clientSocket.getSoTimeout();
			return new Integer(timeout);
		case TCP_NODELAY:
			if(clientSocket!=null)
				tcpNoDelay=clientSocket.getTcpNoDelay();
			return new Boolean(tcpNoDelay);
		case SO_LINGER:
			if(clientSocket!=null)
				soLinger=clientSocket.getSoLinger();
			return new Integer(soLinger);
		case SO_BINDADDR:
			return localAddress;
		case SO_SNDBUF:
			if(clientSocket!=null)
				soSndBuf=clientSocket.getSendBufferSize();
			return new Integer(soSndBuf);
		case SO_RCVBUF:
			if(clientSocket!=null)
				soRCVndBuf=clientSocket.getReceiveBufferSize();
			return new Integer(soRCVndBuf);

		case IP_MULTICAST_IF://For datagram socket

		default:
			throw new SocketException("unrecognized TCP option: " + opt);
		}
	}

	/**
	 * Enable/disable the option specified by <I>optID</I>.  If the option
	 * is to be enabled, and it takes an option-specific "value",  this is
	 * passed in <I>value</I>.  The actual type of value is option-specific,
	 * and it is an error to pass something that isn't of the expected type:
	 * <BR><PRE>
	 * SocketImpl s;
	 * ...
	 * s.setOption(SO_LINGER, new Integer(10));
	 *	// OK - set SO_LINGER w/ timeout of 10 sec.
	 * s.setOption(SO_LINGER, new Double(10));
	 *	// ERROR - expects java.lang.Integer
	 *</PRE>
	 * If the requested option is binary, it can be set using this method by
	 * a java.lang.Boolean:
	 * <BR><PRE>
	 * s.setOption(TCP_NODELAY, new Boolean(true));
	 *	// OK - enables TCP_NODELAY, a binary option
	 * </PRE>
	 * <BR>
	 * Any option can be disabled using this method with a Boolean(false):
	 * <BR><PRE>
	 * s.setOption(TCP_NODELAY, new Boolean(false));
	 *	// OK - disables TCP_NODELAY
	 * s.setOption(SO_LINGER, new Boolean(false));
	 *	// OK - disables SO_LINGER
	 * </PRE>
	 * <BR>
	 * For an option that requires a particular parameter,
	 * setting its value to anything other than
	 * <I>Boolean(false)</I> implicitly enables it.
	 * <BR>
	 * Throws SocketException if the option is unrecognized,
	 * the socket is closed, or some low-level error occurred
	 * <BR>
	 * @param optID identifies the option
	 * @param value the parameter of the socket option
	 * @throws SocketException if the option is unrecognized,
	 * the socket is closed, or some low-level error occurred
	 */public void setOption(int opt, Object val) throws SocketException
	{
		boolean flag = true;
		switch (opt)
		{
		case SO_LINGER:
			if (val == null || (!(val instanceof Integer) && !(val instanceof Boolean)))
				throw new SocketException("Bad parameter for option");
			if (val instanceof Boolean)
			{
				/* true only if disabling - enabling should be Integer */
				flag = false;
			}
			if(clientSocket!=null)
			{
				if(flag)				
					soLinger=((Integer)val).intValue();
				else
					soLinger=clientSocket.getSoLinger();
				clientSocket.setSoLinger(flag,soLinger);
			}
			break;
		case SO_TIMEOUT:
			if (val == null || (!(val instanceof Integer)))
				throw new SocketException("Bad parameter for SO_TIMEOUT");
			int t = ((Integer) val).intValue();
			timeout=(t<0)?0:t;
			if(clientSocket!=null)
				clientSocket.setSoTimeout(timeout);
			return;
		case SO_BINDADDR:
			throw new SocketException("Cannot re-bind socket");
		case TCP_NODELAY:
			if (val == null || !(val instanceof Boolean))
				throw new SocketException("bad parameter for TCP_NODELAY");
			flag = ((Boolean)val).booleanValue();
			tcpNoDelay=flag;
			if(clientSocket!=null)
				clientSocket.setTcpNoDelay(tcpNoDelay);
			break;
		case SO_SNDBUF:
		case SO_RCVBUF:
			if (val == null || !(val instanceof Integer) ||
				!(((Integer)val).intValue() > 0))
			{
				throw new SocketException("bad parameter for SO_SNDBUF " +
					"or SO_RCVBUF");
			}
			if(clientSocket!=null)
			{
				int size=((Integer)val).intValue();
				if(opt==SO_SNDBUF)
				{
					soSndBuf=size;
					clientSocket.setSendBufferSize(size);
				}
				else
				{
					soRCVndBuf=size;
					clientSocket.setReceiveBufferSize(size);
				}
			}
			break;
		
		case SO_REUSEADDR://For datagram socket
			if (val== null || !(val instanceof Integer))
				throw new SocketException("bad argument for SO_REUSEADDR");
//			break;
		case IP_MULTICAST_IF://For datagram socket
			if(val == null || !(val instanceof InetAddress))
				throw new SocketException("bad argument for IP_MULTICAST_IF");
//			break;
		
		default:
			throw new SocketException("unrecognized TCP option: " + opt);
		}
	}

	//The methods of DatagramSocketImpl
	/**
	 * Creates a datagram socket
	 */
	protected void create() throws SocketException
	{
		//Nothing
	}

	/**
	 * Binds a datagram socket to a local port and address.
	 */
	protected void bind(int localport, InetAddress localAddress) throws SocketException
	{
		this.localAddress=localAddress;
		this.localport=localport;
	}

	/**
	 * Sends a datagram packet from this socket. The
	 * <code>DatagramPacket</code> includes information indicating the
	 * data to be sent, its length, the IP address of the remote host,
	 * and the port number on the remote host.
	 *
	 * @param	 p   the <code>DatagramPacket</code> to be sent.
	 * 
	 * @exception  IOException  if an I/O error occurs.
	 */
	protected void send(DatagramPacket p) throws IOException
	{
		InetAddress packetAddress = p.getAddress();
		if(address== null && packetAddress == null)
			throw new IllegalArgumentException("Both of remote address and packet address are null.");
		else if(packetAddress != null &&
			!(packetAddress.equals(address) &&
			p.getPort() == port))
		{
			connect(packetAddress,p.getPort());
		}
		else if(clientDatagramSocket==null)
			connect(address,port);

		if(proxyAddress==null)
		{
			clientDatagramSocket.send(p);
			return;
		}

		//encapsulate datagram packet
		outputBuffer.reset();

		outputBuffer.write(NULL);
		outputBuffer.write(NULL);

		outputBuffer.write(NULL);

		outputBuffer.write(remoteAddressType);

		outputBuffer.write(remoteAddress);

		outputBuffer.write(remotePort);

		outputBuffer.write(p.getData());

		byte[] data=outputBuffer.toByteArray();

		p=new DatagramPacket(data,data.length,serverBoundAddress,serverBoundPort);

		clientDatagramSocket.send(p);
	}
 
	/**
	 * An extended method for DatagramSocket
	 *
	 * Disconnects the socket. This does nothing if the socket is not
	 * connected.
	 */
	protected void disconnect()
	{
		if(stream)// Direct stream socket connection
			throw new IllegalAccessError("Illegal called a specific method for DatagramSocket whne using Socket");
		if(clientDatagramSocket!=null)
		{
			this.address=null;
			this.port=-1;
			clientDatagramSocket.disconnect();
		}
		else
			initRemoteHost((InetAddress)null,-1);
	}

	/**
	 * Peek at the packet to see who it is from.
	 * @param return the address which the packet came from.
	 */
	protected int peek(InetAddress i) throws IOException
	{
		throw new IOException("Not implemented.");
	}

	/**
	 * Receive the datagram packet.
	 * @param Packet Received.
	 */
	protected void receive(DatagramPacket p) throws IOException
	{
		InetAddress packetAddress = p.getAddress();
		if(address== null && packetAddress == null)
			throw new IllegalArgumentException("Both of remote address and packet address are null.");
		else if(packetAddress != null &&
			!(packetAddress.equals(address) &&
			p.getPort() == port))
		{
			connect(packetAddress,p.getPort());
		}
		else if(clientDatagramSocket==null)
			connect(address,port);
			
		if(proxyAddress==null)
		{
			clientDatagramSocket.receive(p);
			return;
		}

		p.setAddress(serverBoundAddress);
		p.setPort(serverBoundPort);

		clientDatagramSocket.receive(p);

		byte[] data=p.getData();
		if(data.length<10
			|| data[0]!=NULL || data[1]!=NULL || data[2]!=NULL
			|| data[3]!=IP_V4)
			throw new IOException("Unkown socks datagram packet: "+new String(data));
		InetAddress sourceInetAddress=getInetAddress(data[3],data,4);
		if(!sourceInetAddress.equals(address)
			|| getPortValue(data,8)!=port)///Discard invalid datagram packet
		{
			receive(p);
			return;
		}
		byte[] ba=new byte[data.length-10];
		System.arraycopy(data,10,ba,0,ba.length);
		p.setData(ba);
		p.setAddress(address);
		p.setPort(port);
	}

	/**
	 * Set the TTL (time-to-live) option.
	 * @param TTL to be set.
	 *
	 * @deprecated use setTimeToLive instead.
	 */
	protected void setTTL(byte ttl) throws IOException
	{
		throw new IOException("Not implemented deprecated function.");
	}

	/**
	 * Retrieve the TTL (time-to-live) option.
	 *
	 * @deprecated use getTimeToLive instead.
	 */
	protected byte getTTL() throws IOException
	{
		throw new IOException("Not implemented deprecated function.");
	}

	/**
	 * Set the TTL (time-to-live) option.
	 * @param TTL to be set.
	 */
	protected void setTimeToLive(int ttl) throws IOException
	{
		throw new IOException("Not implemented.");
	}

	/**
	 * Retrieve the TTL (time-to-live) option.
	 */
	protected int getTimeToLive() throws IOException
	{
		throw new IOException("Not implemented.");
	}

	/**
	 * Join the multicast group.
	 * @param multicast address to join.
	 */
	protected void join(InetAddress inetaddr) throws IOException
	{
		throw new IOException("Not implemented.");
	}

	/**
	 * Leave the multicast group.
	 * @param multicast address to leave.
	 */
	protected void leave(InetAddress inetaddr) throws IOException
	{
		throw new IOException("Not implemented.");
	}

	/**
	 * Get the datagram socket file descriptor
	 */
	protected FileDescriptor getFileDescriptor()
	{
		return null;//Not implemented.
	}
}