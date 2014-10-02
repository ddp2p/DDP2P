//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketOptions;
 
/**
 * SocksSocket class implements client sockets through proxy server
 */
public class SocksSocket extends Socket
{
	/*
	 * The factory for all client SocksSockets.
	 */
	private static SocksSocketImplFactory factory=null;
	/*
	 * The implementation of this Socket.
	 */
	private SocksSocketImpl impl;

	/**
	 * Sets the client socket implementation factory for the
	 * application. The factory can be specified only once.
	 * <p>
	 * When an application creates a new client socket, the socket
	 * implementation factory's <code>createSocketImpl</code> method is
	 * called to create the actual socket implementation.
	 * 
	 * <p>If there is a security manager, this method first calls
	 * the security manager's <code>checkSetFactory</code> method 
	 * to ensure the operation is allowed. 
	 * This could result in a SecurityException.
	 *
	 * @param	fac   the desired factory.
	 * @exception  IOException  if an I/O error occurs when setting the
	 *			   socket factory.
	 */
	public static synchronized void setSocketImplFactory(SocksSocketImplFactory fac)throws IOException
	{
		factory = fac;
	}

	/**
	 * Creates an unconnected socket, with the
	 * system-default type of SocketImpl.
	 */
	protected SocksSocket()
	{
		impl = (factory != null) ? (SocksSocketImpl)factory.createSocketImpl() : new SocksSocketImpl();
	}

	/**
	 * Creates an unconnected Socket with a user-specified
	 * SocketImpl.
	 * <P>
	 * The <i>impl</i> parameter is an instance of a <B>SocketImpl</B>
	 * the subclass wishes to use on the Socket.
	 */
	protected SocksSocket(SocksSocketImpl impl)
	{
		this.impl = impl;
	}

	/**
	 * Creates a stream socket and connects it to the specified port
	 * number on the named host.
	 * <p>
	 * If the application has specified a server socket factory, that
	 * factory's <code>createSocketImpl</code> method is called to create
	 * the actual socket implementation. Otherwise a "plain" socket is created.
	 * <p>
	 * If there is a security manager, its
	 * <code>checkConnect</code> method is called
	 * with the host address and <code>port</code> 
	 * as its arguments. This could result in a SecurityException.
	 *
	 * @param	host   the host name.
	 * @param	port   the port number.
	 * @exception  IOException  if an I/O error occurs when creating the socket.
	 */
	public SocksSocket(String host, int port) throws IOException
	{
		this(host, port,null,0);
	}

	/**
	 * Creates a stream socket and connects it to the specified port
	 * number at the specified IP address.
	 * <p>
	 * If the application has specified a socket factory, that factory's
	 * <code>createSocketImpl</code> method is called to create the
	 * actual socket implementation. Otherwise a "plain" socket is created.
	 * <p>
	 * If there is a security manager, its
	 * <code>checkConnect</code> method is called
	 * with the host address and <code>port</code> 
	 * as its arguments. This could result in a SecurityException.
	 * 
	 * @param	address   the IP address.
	 * @param	port	 the port number.
	 * @exception  IOException  if an I/O error occurs when creating the socket.
	 */
	public SocksSocket(InetAddress address, int port) throws IOException
	{
		this(address, port,null,0);
	}
	/**
	 * Creates a socket and connects it to the specified remote host on
	 * the specified remote port. The Socket will also bind() to the local
	 * address and port supplied.
	 * <p>
	 * If there is a security manager, its
	 * <code>checkConnect</code> method is called
	 * with the host address and <code>port</code> 
	 * as its arguments. This could result in a SecurityException.
	 * 
	 * @param host the name of the remote host
	 * @param port the remote port
	 * @param localAddr the local address the socket is bound to
	 * @param localPort the local port the socket is bound to
	 * @exception  IOException  if an I/O error occurs when creating the socket.
	 */
	public SocksSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException
	{
		this();
		try
		{
			impl.create(true);
			impl.bind(localAddr, localPort);
			impl.connect(host, port);
		}
		catch (SocketException e)
		{
			impl.close();
			throw e;
		}
	}

	/**
	 * Creates a socket and connects it to the specified remote address on
	 * the specified remote port. The Socket will also bind() to the local
	 * address and port supplied.
	 * <p>
	 * If there is a security manager, its
	 * <code>checkConnect</code> method is called
	 * with the host address and <code>port</code> 
	 * as its arguments. This could result in a SecurityException.
	 * 
	 * @param address the remote address
	 * @param port the remote port
	 * @param localAddr the local address the socket is bound to
	 * @param localPort the local port the socket is bound to
	 * @exception  IOException  if an I/O error occurs when creating the socket.
	 */
	public SocksSocket(InetAddress address, int port, InetAddress localAddr,int localPort) throws IOException
	{
		this();
		try
		{
			impl.create(true);
			impl.bind(localAddr, localPort);
			impl.connect(address, port);
		}
		catch (SocketException e)
		{
			impl.close();
			throw e;
		}
	}

	/**
	 * Closes this socket.
	 *
	 * @exception  IOException  if an I/O error occurs when closing this socket.
	 */
	public synchronized void close() throws IOException
	{
		impl.close();
	}

	/**
	 * Returns an input stream for this socket.
	 *
	 * @return	 an input stream for reading bytes from this socket.
	 * @exception  IOException  if an I/O error occurs when creating the
	 *			   input stream.
	 */
	public InputStream getInputStream() throws IOException
	{
		return impl.getInputStream();
	}

	/**
	 * Returns an output stream for this socket.
	 *
	 * @return	 an output stream for writing bytes to this socket.
	 * @exception  IOException  if an I/O error occurs when creating the
	 *			   output stream.
	 */
	public OutputStream getOutputStream() throws IOException
	{
		return impl.getOutputStream();
	}

	/**
	 * Gets the local address to which the socket is bound.
	 */
	public InetAddress getLocalAddress()
	{
		return impl.getLocalAddress();
/*		try
		{
			return (InetAddress) impl.getOption(SocketOptions.SO_BINDADDR);
		}
		catch (Exception e)
		{
			try
			{
				return InetAddress.getByName("0.0.0.0");
			}
			catch(java.net.UnknownHostException uhe)
			{
				return null;
			}
		}*/
	}
	/**
	 * Returns the local port to which this socket is bound.
	 *
	 * @return  the local port number to which this socket is connected.
	 */
	public int getLocalPort()
	{
		return impl.getLocalPort();
	}

	/**
	 * Returns setting for SO_TIMEOUT.  0 returns implies that the
	 * option is disabled (i.e., timeout of infinity).
	 */
	public synchronized int getSoTimeout() throws SocketException
	{
		Object o = impl.getOption(SocketOptions.SO_TIMEOUT);
		if (o instanceof Integer)
			return ((Integer) o).intValue();
		else
			return 0;
	}

	/**
	 * Tests if TCP_NODELAY is enabled.
	 *
	 * @since   JDK1.1
	 */
	public boolean getTcpNoDelay() throws SocketException
	{
		return ((Boolean) impl.getOption(SocketOptions.TCP_NODELAY)).booleanValue();
	}

	/**
	 * Returns setting for SO_LINGER. -1 returns implies that the
	 * option is disabled.
	 */
	public int getSoLinger() throws SocketException
	{
		Object o = impl.getOption(SocketOptions.SO_LINGER);
		if (o instanceof Integer)
			return ((Integer) o).intValue();
		else
			return -1;
	}

	/**
	 * Get value of the SO_SNDBUF option for this socket, that is the
	 * buffer size used by the platform for output on the this Socket.
	 *
	 * @see #setSendBufferSize
	 */
	public synchronized int getSendBufferSize() throws SocketException
	{
		Object o = impl.getOption(SocketOptions.SO_SNDBUF);
		if (o instanceof Integer)
			return ((Integer)o).intValue();
		else
			return 0;
	}

	/**
	 * Returns the address to which the socket is connected.
	 *
	 * @return  the remote IP address to which this socket is connected.
	 */
	public InetAddress getInetAddress()
	{
		return impl.getInetAddress();
	}

	/**
	 * Returns the remote port to which this socket is connected.
	 *
	 * @return  the remote port number to which this socket is connected.
	 */
	public int getPort()
	{
		return impl.getPort();
	}

	/**
	 * Get value of the SO_RCVBUF option for this socket, that is the
	 * buffer size used by the platform for input on the this Socket.
	 *
	 * @see #setReceiveBufferSize
	 */
	public synchronized int getReceiveBufferSize()throws SocketException
	{
		int result = 0;
		Object o = impl.getOption(SocketOptions.SO_RCVBUF);
		if (o instanceof Integer)
			result = ((Integer)o).intValue();
		return result;
	}

	/**
	 * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
	 */
	public void setTcpNoDelay(boolean on) throws SocketException
	{
		impl.setOption(SocketOptions.TCP_NODELAY, new Boolean(on));
	}

	/**
	 * Enable/disable SO_LINGER with the specified linger time in seconds. 
	 * If the specified timeout value exceeds 65,535 it will be reduced to
	 * 65,535.
	 * 
	 * @param on	 whether or not to linger on.
	 * @param linger how to linger for, if on is true.
	 * @exception IllegalArgumentException if the linger value is negative.
	 */
	public void setSoLinger(boolean on, int linger) throws SocketException
	{
		if (on)
		{
			if (linger < 0)
			{
				throw new IllegalArgumentException("invalid value for SO_LINGER");
			}
			if (linger > 65535)
				linger = 65535;
			impl.setOption(SocketOptions.SO_LINGER, new Integer(linger));
		}
		else
		{
			impl.setOption(SocketOptions.SO_LINGER, new Boolean(on));
		}
	}

	/**
	 *  Enable/disable SO_TIMEOUT with the specified timeout, in
	 *  milliseconds.  With this option set to a non-zero timeout,
	 *  a read() call on the InputStream associated with this Socket
	 *  will block for only this amount of time.  If the timeout expires,
	 *  a <B>java.io.InterruptedIOException</B> is raised, though the
	 *  Socket is still valid. The option <B>must</B> be enabled
	 *  prior to entering the blocking operation to have effect. The
	 *  timeout must be > 0.
	 *  A timeout of zero is interpreted as an infinite timeout.
	 */
	public synchronized void setSoTimeout(int timeout) throws SocketException
	{
		impl.setOption(SocketOptions.SO_TIMEOUT, new Integer(timeout));
	}

	/**
	 * Sets the SO_SNDBUF option to the specified value for this
	 * DatagramSocket. The SO_SNDBUF option is used by the platform's
	 * networking code as a hint for the size to use to allocate set
	 * the underlying network I/O buffers.
	 *
	 * <p>Increasing buffer size can increase the performance of
	 * network I/O for high-volume connection, while decreasing it can
	 * help reduce the backlog of incoming data. For UDP, this sets
	 * the maximum size of a packet that may be sent on this socket.
	 *
	 * <p>Because SO_SNDBUF is a hint, applications that want to
	 * verify what size the buffers were set to should call
	 * <href="#getSendBufferSize>getSendBufferSize</a>.
	 *
	 * @param size the size to which to set the send buffer
	 * size. This value must be greater than 0.
	 *
	 * @exception IllegalArgumentException if the value is 0 or is
	 * negative.
	 */
	public synchronized void setSendBufferSize(int size)throws SocketException
	{
		if (!(size > 0))
			throw new IllegalArgumentException("negative send size");
		impl.setOption(SocketOptions.SO_SNDBUF, new Integer(size));
	}

	/**
	 * Sets the SO_RCVBUF option to the specified value for this
	 * DatagramSocket. The SO_RCVBUF option is used by the platform's
	 * networking code as a hint for the size to use to allocate set
	 * the underlying network I/O buffers.
	 *
	 * <p>Increasing buffer size can increase the performance of
	 * network I/O for high-volume connection, while decreasing it can
	 * help reduce the backlog of incoming data. For UDP, this sets
	 * the maximum size of a packet that may be sent on this socket.
	 *
	 * <p>Because SO_RCVBUF is a hint, applications that want to
	 * verify what size the buffers were set to should call
	 * <href="#getReceiveBufferSize>getReceiveBufferSize</a>.
	 *
	 * @param size the size to which to set the receive buffer
	 * size. This value must be greater than 0.
	 *
	 * @exception IllegalArgumentException if the value is 0 or is
	 * negative.
	 */
	public synchronized void setReceiveBufferSize(int size)throws SocketException
	{
		if (size < 0)
			throw new IllegalArgumentException("invalid receive size");
		impl.setOption(SocketOptions.SO_RCVBUF, new Integer(size));
	}

	/**
	 * Converts this socket to a <code>String</code>.
	 *
	 * @return  a string representation of this socket.
	 */
	public String toString()
	{
		return "SocksSocket[addr=" + impl.getInetAddress() +
			",port=" + impl.getPort() +
			",localaddr=" 
			+(impl.getLocalAddress()!=null?impl.getLocalAddress().toString():"127.0.0.1")
			+",localport=" + impl.getLocalPort()+
			(impl.getProxyAddress()!=null?
			",proxyddr=" + impl.getProxyAddress() +
			",proxyport=" + impl.getProxyPort():"")
			+"]";
	}

	/**
	 * Some importmant optional parameters for SOCKS4 and SOCKS5
	 */
	/**
	 * Use USER or USERNAME to set username for the specific SOCKS proxy serer.
	 * For SOCKS4A, USERID mean a password
	 */
	public static final String USER ="USER";
	public static final String USERNAME =USER;

	/**
	 * Use USERID,PASSWD or PASSWORD to set password for the specific SOCKS proxy serer.
	 * For SOCKS4A, USERID mean a password
	 */
	public static final String PASSWD ="PASSWD";
	public static final String PASSWORD =PASSWD;
	public static final String USERID =PASSWD;

	/**
	 * Use VERSION to set the appropriate SOCKS version for the specific SOCKS proxy serer.
	 * Value:4 or 5
	 */
	public static final String VERSION ="VERSION";
}
