package net.ddp2p.common.network.stun.keepalive;

public class SessionKey
{

    int id;         // The generated session id.
    String address; // The address used by the client.
    int port;       // The port used by the client.

    SessionKey( int id, String address, int port )
    {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    SessionKey() { }

    int getId() { return id; }
    void setId( int id ) { this.id = id; }

    String getAddress() { return address; }
    void setAddress( String address ) { this.address = address; }

    int getPort() { return port; }
    void setPort( int port ) { this.port = port; }

    @Override
    public boolean equals( Object obj )
    {
        if( obj == null )
        {
            return false;
        }
        if( getClass() != obj.getClass() )
        {
            return false;
        }

        final SessionKey other = (SessionKey)obj;
        if( this.id != other.getId() )
        {
            System.out.println("DIFF ID: " + this.id + " : " + other.getId() );
            return false;
        }
        if( ! this.address.equals( other.getAddress() ) )
        {
            System.out.println("DIFF ADDRESS: " + this.address + " : " + other.getAddress() );
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 53 * hash + id;
        hash = 53 * hash + ( this.address != null ? this.address.hashCode() : 0 );
        hash = 53 * hash + port;

        return hash;
    }

}
