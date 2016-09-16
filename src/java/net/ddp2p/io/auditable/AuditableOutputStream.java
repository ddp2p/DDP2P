package net.ddp2p.io.auditable;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

class AuditableOutputStream extends FilterOutputStream {
	AuditableSocket socket;
	
	public AuditableOutputStream(OutputStream out, AuditableSocket socket) {
        super(out);
        this.socket = socket;
	}

	@Override
    public void write(int b) throws IOException {
    	out.write(b);
        Audit.write(socket.connectionAuditId(), socket.getLocalAddress().toString(), b);
    }

	@Override
    public void write(byte[] b) throws IOException {
    	out.write(b);
    	Audit.write(socket.connectionAuditId(), socket.getLocalAddress().toString(), b);
    }

	@Override
    public void write(byte[] b, int off, int len) throws IOException {
    	out.write(b, off, len);
    	Audit.write(socket.connectionAuditId(), socket.getLocalAddress().toString(), b, off, len);
    }
	
	
}