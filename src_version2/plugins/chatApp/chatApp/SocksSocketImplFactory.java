//package com.jeeva.chatclient;
package chatApp;

import java.io.IOException;

import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Properties;

/**
 * SocksSocketImplFactory class defines a factory for SocksSocket implementations.
 * SocksSocket class implements client sockets through proxy server
 */
public class SocksSocketImplFactory implements SocketImplFactory
{
	/*
	 * The IP address of the SOCKS proxy server.
	 */
	private InetAddress proxyAddress=null;
	/*
	 * The port number on the SOCKS proxy serer.
	 */
	private int proxyPort=SocksSocketConstants.SOCKS_PORT;
	/*
	 *  a boolean indicating whether this is a stream socket or a datagram socket.
	 *  If the stream argument is true, this creates a stream socket. If the stream argument is
     *  false, it creates a datagram socket. 
	 */
	private boolean stream=true;
	/*
	 * Some proxy settings for the SOCKS proxy serer.
	 */
	private Properties properties=null;

	public SocksSocketImplFactory(){}
	public SocksSocketImplFactory(boolean stream)
	{
		this.stream=stream;
	}
	public SocksSocketImplFactory(String proxyHost,int proxyPort)throws UnknownHostException
	{
		this(proxyHost,proxyPort,null,true);
	}
	public SocksSocketImplFactory(String proxyHost,int proxyPort,boolean stream)throws UnknownHostException
	{
		this(proxyHost,proxyPort,null,stream);
	}
	public SocksSocketImplFactory(String proxyHost,int proxyPort,Properties properties)throws UnknownHostException
	{
		this(proxyHost,proxyPort,properties,true);
	}
	public SocksSocketImplFactory(String proxyHost,int proxyPort,Properties properties,boolean stream)throws UnknownHostException
	{
		this(proxyHost==null?null:InetAddress.getByName(proxyHost),proxyPort,properties,stream);
	}
	public SocksSocketImplFactory(InetAddress proxyAddress,int proxyPort)
	{
		this(proxyAddress,proxyPort,null,true);
	}
	public SocksSocketImplFactory(InetAddress proxyAddress,int proxyPort,boolean stream)
	{
		this(proxyAddress,proxyPort,null,stream);
	}
	public SocksSocketImplFactory(InetAddress proxyAddress,int proxyPort,Properties properties)
	{
		this(proxyAddress,proxyPort,properties,true);
	}
	public SocksSocketImplFactory(InetAddress proxyAddress,int proxyPort,Properties properties, boolean stream)
	{
		this.proxyAddress=proxyAddress;
		this.proxyPort=proxyPort;
		this.properties=properties;
		this.stream=stream;
	}


    /**
     * Creates a new <code>SocketImpl</code> instance.
     *
     * @return  a new instance of <code>SocketImpl</code>.
     * @see     java.net.SocketImpl
     */
	public SocketImpl createSocketImpl()
	{
		return new SocksSocketImpl(proxyAddress,proxyPort,properties,stream);
	}
}