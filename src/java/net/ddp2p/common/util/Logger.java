/*   Copyright (C) 2015 Christopher Widmer
		Author: Christopher Widmer: cwidmer@my.fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
package net.ddp2p.common.util;
public class Logger
{
    private boolean errorPrint = true;
    private boolean debugPrint = true;
    private boolean dataPrint = true;
    private boolean infoPrint = true;
    public Logger() { }
    public Logger( boolean errorPrint, boolean debugPrint, boolean dataPrint, boolean infoPrint )
    {
        this.errorPrint = errorPrint;
        this.debugPrint = debugPrint;
        this.dataPrint = dataPrint;
        this.infoPrint = infoPrint;
    }
    public void setErrorPrint( boolean errorPrint ) { this.errorPrint = errorPrint; }
    public boolean getErrorPrint() { return errorPrint; }
    public void setDebugPrint( boolean debugPrint ) { this.debugPrint = debugPrint; }
    public boolean getDebugPrint() { return debugPrint; }
    public void setDataPrint( boolean dataPrint ) { this.dataPrint = dataPrint; }
    public boolean getDataPrint() { return dataPrint; }
    public void setInfoPrint( boolean infoPrint ) { this.infoPrint = infoPrint; }
    public boolean getInfoPrint() { return infoPrint; }
    public void message( String message )
    {
        System.out.println( message );
    }
    public void info( String message )
    {
        if( infoPrint )
        {
            message( "INFO:  " + message );
        }
    }
    public void debug( String message )
    {
        if( debugPrint )
        {
            message( "DEBUG: " + message );
        }
    }
    public void error( String message )
    {
        if( errorPrint )
        {
            message( "ERROR: " + message );
        }
    }
    public void data( Object... data )
    {
        if( dataPrint )
        {
            System.out.print( "DATA:  " );
            for( int i = 0; i < data.length - 1; i++ )
            {
                System.out.print( data[i].toString() + " " + data[i+1].toString() );
                if( i < data.length - 2 )
                {
                    System.out.print(" | " );
                }
                i++;
            }
            System.out.println();
        }
    }
	public void setAgentName(String name) {
	}
	public void setOutputFilePath(String logFileName) {
	}
}
