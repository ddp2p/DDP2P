package net.ddp2p.io.auditable;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;

class AuditableInputStream extends FilterInputStream {
	AuditableSocket socket;
	
	public AuditableInputStream(InputStream in, AuditableSocket socket) {
		super(in);
		this.socket = socket;
	}

	@Override
    public int read() throws IOException {
    	int b = in.read();
    	Audit.read(socket.connectionAuditId(), socket.getLocalAddress().toString(), b);
    	return b;
    }
 
	@Override
    public int read(byte b[]) throws IOException {
    	int c = in.read(b);
    	Audit.read(socket.connectionAuditId(), socket.getLocalAddress().toString(), b, c);
        return c;
    }
    
	@Override
    public int read(byte b[], int off, int len) throws IOException {
    	int c = in.read(b, off, len);
    	Audit.read(socket.connectionAuditId(), socket.getLocalAddress().toString(), b, 0, c);
        return c;
    }
}

