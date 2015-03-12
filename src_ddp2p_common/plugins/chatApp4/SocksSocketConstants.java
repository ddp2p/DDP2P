//package com.jeeva.chatclient;
package dd_p2p.plugin;

interface SocksSocketConstants
{
	/**
	 * The SOCKS service's conventional TCP port
	 */
	public static final int SOCKS_PORT = 1080;

	/**
	 * SOCKS Protocol Version
	 *
	 * SOCKS 4A is a simple extension to SOCKS 4 protocol.
	 * SOCKS 4A allow the use of SOCKS 4A on hosts
	 * which are not capable of resolving all domain names. 
	 *
	 */
	public static final int SOCKS_VERSION_4A = 4;
	public static final int SOCKS_VERSION_5 = 5;

	/**
	 * SOCKS request commands
	 */
	public static final int COMMAND_CONNECT =1;
	public static final int COMMAND_BIND =2;
	public static final int COMMAND_UDP_ASSOCIATE =3;

	/**
	 * SOCKS5 Identifier/method selection message values
	 */	
	public static final int NO_AUTHENTICATION_REQUIRED =0;
	public static final int GSSAPI =1;
	public static final int USERNAME_AND_PASSWORD =2;
	public static final int CHAP =3;
	public static final int NO_ACCEPTABLE_METHODS =0xFF;
	
	/**
	 * IP address type of following address
	 */
	public static final int IP_V4 =1;
	public static final int DOMAINNAME =3;
	public static final int IP_V6 =4;

	/**
	 * NULL is a byte of all zero bits.
	 */
	public static final int NULL=0;

	/**
	 * SOCKS4 reply code
	 */
	public static final int REQUEST_GRANTED =90;
	public static final int REQUEST_REJECTED =91;
	public static final int REQUEST_REJECTED_NO_IDENTD =92;
	public static final int REQUEST_REJECTED_DIFF_IDENTS =93;

	/**
	 * Socks5 reply code
	 */
	public static final int SUCCEEDED =0;
	public static final int FAILURE =1;
	public static final int NOT_ALLOWED =2;
	public static final int NETWORK_UNREACHABLE =3;
	public static final int HOST_UNREACHABLE =4;
	public static final int REFUSED =5;
	public static final int TTL_EXPIRED =6;
	public static final int COMMAND_NOT_SUPPORTED =7;
	public static final int ADDRESS_TYPE_NOT_SUPPORTED =8;
	public static final int INVALID_ADDRESS =9;

	/**
	 *
	 */
	public static final int INTERFACE_REQUEST =1;
	public static final int USECLIENTSPORT =4;
}
