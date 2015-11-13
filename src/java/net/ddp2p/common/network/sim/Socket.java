package net.ddp2p.common.network.sim;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import net.ddp2p.simulator.DDP2P_Network;

class DDP2P_OutputStream extends OutputStream {
	public DDP2P_OutputStream(Socket socket) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void write(int b) throws IOException {
		// TODO Auto-generated method stub	
	}
}
class DDP2P_InputStream extends InputStream {

	public DDP2P_InputStream(Socket socket) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
}

public class Socket extends java.net.Socket {
	public static boolean simulated = false;
	public int port;
	public Socket () {
		super();
		if (! simulated) return;
		// TODO
	}
	
	@Override
	public void connect(SocketAddress endpoint) throws IOException {
		if (! simulated) super.connect(endpoint);
		// TODO
	}
	
	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		if (! simulated) super.setSoTimeout(timeout);
		
	}
	
	@Override
	public OutputStream getOutputStream() throws IOException {
		if (! simulated) return super.getOutputStream();
		return new DDP2P_OutputStream(this);
	}
	
	@Override
	public InputStream getInputStream() throws IOException {
		if (! simulated) return super.getInputStream();
		return new DDP2P_InputStream(this);
	}
	
}