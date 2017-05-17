package org.naarani.ecantonchiro.ssh;

import java.io.IOException;
import java.io.OutputStream;

public class DefaultStreamLog extends OutputStream {

	protected boolean echo = false;
	protected boolean error = false;
	protected OutputStream ou;
	protected StringBuffer sequence = new StringBuffer();

	public DefaultStreamLog(){}

	public DefaultStreamLog( boolean error, boolean echo ){
		this.ou = error ? System.err : System.out;
		this.error = error;
		this.echo = echo;
	}
	
	public String getText(){
		return sequence.toString();
	}
	
	public void reset(){
		sequence.setLength( 0 );
	}

	@Override
	public void write( byte[] buffer, int start, int len ) throws IOException {
		String msg = new String( buffer, start, len, "UTF8" );
		writeMsg( msg );
	}
	
	@Override
	public void write( int b ) throws IOException {
		if( echo )
			ou.write( b );
		sequence.append( (char)b );
	}

	public void writeMsg( String msg ){
		if( echo )
			try {
				ou.write( msg.getBytes() );
			} catch (IOException e) {
				e.printStackTrace();
			}
		sequence.append( msg );
	}
	
}