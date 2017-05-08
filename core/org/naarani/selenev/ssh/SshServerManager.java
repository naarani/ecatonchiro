package org.naarani.selenev.ssh;

import java.io.ByteArrayOutputStream; 
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.naarani.core.exceptions.StopAction;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/* TODO:
 * 
 * 1) doesn't manage KNOWN HOSTS
 * 2) scpfrom doesnt manage multiple files
 * 
 * near all the features come from examples of JCraft: Copyright (c) 2002-2015 Atsuhiko Yamanaka, JCraft,Inc. All rights reserved.
 * 
 * 
 * 
 */
public class SshServerManager {

	static protected Logger logger = Logger.getLogger( SshServerManager.class );

	static public boolean doubleEcho = true;

	protected int _SSH_PORT = 22;
	
	protected JSch jsch;
    protected Session session;
    protected String sudoPwd;
    protected String user, host;
    protected String prvkey;
    protected byte[] unlockIdentity;
	protected boolean connected = false;
    
	public SshServerManager( String host, String sudoPwd ){
		this( host, null, null, sudoPwd );
    }

	public SshServerManager( String host, String prvkey, byte[] unlock ){
		this( host, prvkey, unlock, null );
	}

	public SshServerManager( String host, String prvkey, byte[] unlock, String sudoPwd ){
      jsch = new JSch();
      //jsch.setKnownHosts("<define>/.ssh/known_hosts");
      this.prvkey = prvkey;
      this.unlockIdentity = unlock;
      this.sudoPwd = sudoPwd;
      this.user=host.substring(0, host.indexOf('@'));
      this.host=host.substring(host.indexOf('@')+1);
    }

	public JSch getJsch(){
		return jsch;
	}

	public String getHost(){
		return host;
	}

	public String getUser(){
		return user;
	}

	public String getPwd(){
		return sudoPwd;
	}

	public boolean isConnected(){
		return connected;
	}

	protected int commandTimeoutConnect = 6 * 1000;
	
	public void setCommandTimeoutConnect( int value ){
		commandTimeoutConnect = value;
	}
	
	public int getCommandTimoutConnect(){
		return commandTimeoutConnect;
	}
	
	protected int connTimeout = 60000;
	
	public void setConnectionTimeoutConnect(int value ) {
		connTimeout = value ;
	}

	public int getConnectionTimeoutConnect(){
		return connTimeout;
	}

	protected DefaultStreamLog commandLogger = new MaskerStreamLog( this, false, true );

	public DefaultStreamLog getCmdLog() {
		return commandLogger;
	}

	public void setCmdLog( DefaultStreamLog cmdLog ) {
		commandLogger = cmdLog;
	}

	protected DefaultStreamLog commandErrorLogger = new MaskerStreamLog( this, true, true );

	public DefaultStreamLog getCmdErrLog() {
		return commandErrorLogger;
	}

	public void setCmdErrLog( DefaultStreamLog cmdErrLog ) {
		commandErrorLogger = cmdErrLog;
	}

	public ExecutionStatus connect(){
		try {
			if( prvkey != null ){
				if( unlockIdentity != null ){
					jsch.addIdentity( prvkey, unlockIdentity );
				} else {
					jsch.addIdentity( prvkey );
				}
			}
			session = jsch.getSession( user, host, _SSH_PORT );
			if( prvkey == null ){
				session.setPassword( sudoPwd );
			}
		    // It must not be recommended, but if you want to skip host-key check,
		    // invoke following,
	        //
		    session.setConfig("StrictHostKeyChecking", "no");
			session.connect(connTimeout);   // making a connection with timeout.
			connected = true;
			return ExecutionStatus.Done;
	    } catch( Exception e ){
			connected = false;
	    	if( e.getMessage().toUpperCase().indexOf( "AUTH FAIL" ) != -1 ){
	    		return ExecutionStatus.passwordWrong;
			}
			if( e.getMessage().toUpperCase().indexOf( "TIMED OUT" ) != -1 || e.getMessage().toUpperCase().indexOf( "TIMEOUT" ) != -1 ){
				return ExecutionStatus.connectionTimeout;
			}
	    	e.printStackTrace();
	    	return ExecutionStatus.unknownError;
	    }
    }

	public void close() throws StopAction {
		if( !connected ){
			throw new StopAction( "Connect before run command" );
		}
		try {
			session.disconnect();
	      	connected = true;
	    } catch( Exception e ){
	    	System.out.println(e);
	    }
	}

	public ExecutionStatus sudoCommand( String command ) throws StopAction {
		return sudoCommand( command, null );
	}
	
	public ExecutionStatus sudoCommand( String command, CliInteract interact ) throws StopAction {
		getCmdErrLog().reset();
		getCmdLog().reset();
		if( !connected ){
			throw new StopAction( "Connect before run command" );
		}
		try {
			Channel channel = session.openChannel("exec");
			// man sudo
			// -S The -S (stdin) option causes sudo to read the password from
			// the
			// standard input instead of the terminal device.
			// -p The -p (prompt) option allows you to override the default
			// password prompt and use a custom one.
			((ChannelExec) channel).setPty( true ); // only for SUDO
			((ChannelExec) channel).setCommand("sudo -S -p '' " + command );

			InputStream in = channel.getInputStream();
			OutputStream out = channel.getOutputStream();
			((ChannelExec) channel).setErrStream( commandErrorLogger );
			channel.connect(commandTimeoutConnect);

			out.write((sudoPwd + "\n").getBytes());
			out.flush();
			byte[] tmp = new byte[1024];
			ExecutionStatus status = ExecutionStatus.Done;
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					commandLogger.write( tmp, 0, i );
					if( doubleEcho ){
						String masked = getCmdErrLog().getText();
						if( masked.length() != 0 ) {
							masked = masked.replaceAll( sudoPwd, "##########" );
							System.out.print( "ECHO: " + masked );
						}
					}
				}
				if( channel.isClosed() ){
					if( channel.getExitStatus() != 0 ){
						status = ExecutionStatus.wrongExecution;
						System.out.println( "CMD:exit-status: " + channel.getExitStatus() );
					}
					break;
				}
				try {
					if( interact != null ){
						String msg = interact.action();
						if( msg != null ){
							out.write( ( msg ).getBytes() );
							out.flush();
						}
					}
					Thread.sleep(1000);
				} catch (Exception ee) {
					status = ExecutionStatus.wrongExecution;
				}
			}
			channel.disconnect();
			if( doubleEcho && getCmdErrLog().getText().trim().length() > 0 ){
				String masked = getCmdErrLog().getText();
				if( masked.length() != 0 ) {
					masked = masked.replaceAll( sudoPwd, "##########" );
					System.out.print( "ECHO: " + masked );
				}
			}
			return status; // ref != -1;
		} catch ( JSchException | IOException e ){
			if( e.getMessage().toUpperCase().indexOf( "SESSION IS DOWN" ) != -1 ){
				return ExecutionStatus.sessionDown;
			}
			logger.error( "SSHMANAGER error on (sudo)CMD", e );
			return ExecutionStatus.unknownError;
		}
	}

	public ExecutionStatus command( String command ) throws StopAction {
		return command( command, null );
	}

	public ExecutionStatus command( String command, CliInteract interact ) throws StopAction {
		getCmdErrLog().reset();
		getCmdLog().reset();
		if( !connected ){
			throw new StopAction( "Connect before run command" );
		}
		try {
			Channel channel = session.openChannel("exec");
			// man sudo
			// -S The -S (stdin) option causes sudo to read the password from
			// the
			// standard input instead of the terminal device.
			// -p The -p (prompt) option allows you to override the default
			// password prompt and use a custom one.
			if( interact != null )
				((ChannelExec) channel).setPty( true ); // only for SUDO
			((ChannelExec) channel).setCommand( command );

			InputStream in = channel.getInputStream();
			OutputStream out = channel.getOutputStream();
			((ChannelExec) channel).setErrStream( commandErrorLogger );
			channel.connect(commandTimeoutConnect);

			byte[] tmp = new byte[1024];
			ExecutionStatus status = ExecutionStatus.Done;
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					commandLogger.write( tmp, 0, i );
				}
				if( channel.isClosed() ){
					if( channel.getExitStatus() != 0 ){
						status = ExecutionStatus.wrongExecution;
						System.out.println( "CMD:exit-status: " + channel.getExitStatus() );
					}
					break;
				}
				try {
					if( interact != null ){
						String msg = interact.action();
						if( msg != null ){
							out.write( ( msg ).getBytes() );
							out.flush();
						}
					}
					Thread.sleep(1000);
				} catch (Exception ee) {
					status = ExecutionStatus.wrongExecution;
				}
			}
			channel.disconnect();
			if( doubleEcho && getCmdErrLog().getText().trim().length() > 0 ){
				String masked = getCmdErrLog().getText();
				masked = masked.replaceAll( sudoPwd, "##########" );
				System.out.print( "ECHO: " + masked );
			}
			return status;
		} catch ( JSchException | IOException e ){
			if( e.getMessage().toUpperCase().indexOf( "SESSION IS DOWN" ) != -1 ){
				return ExecutionStatus.sessionDown;
			}
			logger.error( "SSHMANAGER error on CMD", e );
			return ExecutionStatus.unknownError;
		}
	}

	public ExecutionStatus scpTO( String lfile, InputStream fis, long lastmodified, long size, String rfile ) throws StopAction {
		getCmdErrLog().reset();
		getCmdLog().reset();
		if( !connected ){
			throw new StopAction( "Connect before run command" );
		}
		try {
			boolean ptimestamp = true;
			// exec 'scp -t rfile' remotely
			String command="scp -v " + (ptimestamp ? "-p" :"") +" -t " + rfile;
			Channel channel = session.openChannel("exec");
//			((ChannelExec) channel).setPty( true );
			((ChannelExec)channel).setCommand(command);
			// get I/O streams for remote scp
			OutputStream out=channel.getOutputStream();
			InputStream in= channel.getInputStream();
			channel.connect();
			if(checkAck(in)!=0){
				return ExecutionStatus.stopped;
			}
		//	File _lfile = new File(lfile);
			if( ptimestamp ){
				command = "T"+(lastmodified/1000)+" 0";
				// The access time should be sent here,
				// but it is not accessible with JavaAPI ;-<
				command += (" "+(lastmodified/1000)+" 0\n");
				out.write(command.getBytes()); 
				out.flush();
				if(checkAck(in)!=0){
					return ExecutionStatus.stopped;
				}
			}
			// send "C0644 filesize filename", where filename should not include '/'
			long filesize=size;
			command="C0644 "+filesize+" ";
			if(lfile.lastIndexOf('/')>0){
				command+=lfile.substring(lfile.lastIndexOf('/')+1);
			} else{
				command+=lfile;
			}
			command+="\n";
			out.write(command.getBytes()); out.flush();
			if(checkAck(in)!=0){
				return ExecutionStatus.stopped;
			}
			// send a content of lfile
			// fis=new FileInputStream(lfile);
			byte[] buf=new byte[1024];
			while(true){
				int len=fis.read(buf, 0, buf.length);
				if(len<=0) break;
				out.write(buf, 0, len); //out.flush();
			}
			fis.close();
			fis=null;
			// send '\0'
			buf[0]=0; out.write(buf, 0, 1); out.flush();
			if(checkAck(in)!=0){
				return ExecutionStatus.stopped;
			}
			out.close();
			channel.disconnect();
			return ExecutionStatus.Done; // ref != -1;
		} catch ( JSchException | IOException e ){
			if( e.getMessage().toUpperCase().indexOf( "SESSION IS DOWN" ) != -1 ){
				return ExecutionStatus.sessionDown;
			}
			logger.error( "SSHMANAGER error on ScpTo", e );
			return ExecutionStatus.unknownError;
		}
	}

	public String sudoScpFrom( String rfile ) throws StopAction, IOException {
		ByteArrayOutputStream ou = new ByteArrayOutputStream();
		ExecutionStatus status = sudoScpFrom( rfile, ou );
		ou.close();
		if( status != ExecutionStatus.Done ){
			throw new StopAction( "FAILED " + status );
		}
		return new String( ou.toByteArray() );
	}

	public String scpFrom( String rfile ) throws StopAction, IOException {
		ByteArrayOutputStream ou = new ByteArrayOutputStream();
		ExecutionStatus status = scpFrom( rfile, ou );
		ou.close();
		if( ExecutionStatus.Done == status ){
			throw new StopAction( "FAILED " + status );
		}
		return new String( ou.toByteArray() );
	}
	
	public ExecutionStatus scpFrom( String rfile, OutputStream fos ) throws StopAction {
		getCmdErrLog().reset();
		getCmdLog().reset();
		if( !connected ){
			throw new StopAction( "Connect before run command" );
		}
		try {
			/*
			String prefix=null;
		    if(new File(lfile).isDirectory()){
		    	prefix=lfile+File.separator;
		    }
		    */
		    //
			String command = "scp -f " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out=channel.getOutputStream();
			InputStream in= channel.getInputStream();
			channel.connect();

			byte[] buf = new byte[1024];
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}
				// read '0644 '
				in.read(buf, 0, 5);
				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}
				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
				// read a content of lfile
	//			fos = new FileOutputStream( prefix == null ? lfile : prefix + file );
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;
				if (checkAck(in) != 0) {
					return ExecutionStatus.stopped;
				}
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}
			channel.disconnect();
			return ExecutionStatus.Done; // ref != -1;
		} catch ( JSchException | IOException e ){
			if( e.getMessage().toUpperCase().indexOf( "SESSION IS DOWN" ) != -1 ){
				return ExecutionStatus.sessionDown;
			}
			logger.error( "SSHMANAGER error on ScpFrom", e );
			return ExecutionStatus.unknownError;
		}
	}

	public ExecutionStatus sudoScpFrom( String rfile, OutputStream fos ) throws StopAction {
		getCmdErrLog().reset();
		getCmdLog().reset();
		if( !connected ){
			throw new StopAction( "Connect before run command" );
		}
		try {
			String temp = "~/temp_exchange" + System.currentTimeMillis();
			ExecutionStatus status = sudoCommand( "cp " + rfile + " " + temp );
			if( status != ExecutionStatus.Done )
				return ExecutionStatus.stopped;
			status = sudoCommand( "chown " + user + ":" + user + " " + temp );
			if( status != ExecutionStatus.Done )
				return ExecutionStatus.stopped;
			rfile = temp;
/*
			String prefix=null;
		    if( new File(lfile).isDirectory() ){
		    	prefix=lfile+File.separator;
		    }
		    */
		    //
			String command = "scp -f " + rfile;
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(command);

			// get I/O streams for remote scp
			OutputStream out=channel.getOutputStream();
			InputStream in= channel.getInputStream();
			channel.connect();

			byte[] buf = new byte[1024];
			// send '\0'
			buf[0] = 0;
			out.write(buf, 0, 1);
			out.flush();
			while (true) {
				int c = checkAck(in);
				if (c != 'C') {
					break;
				}
				// read '0644 '
				in.read(buf, 0, 5);
				long filesize = 0L;
				while (true) {
					if (in.read(buf, 0, 1) < 0) {
						// error
						break;
					}
					if (buf[0] == ' ')
						break;
					filesize = filesize * 10L + (long) (buf[0] - '0');
				}
				String file = null;
				for (int i = 0;; i++) {
					in.read(buf, i, 1);
					if (buf[i] == (byte) 0x0a) {
						file = new String(buf, 0, i);
						break;
					}
				}
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
				// read a content of lfile
				//fos = new FileOutputStream( prefix == null ? lfile : prefix + file );
				int foo;
				while (true) {
					if (buf.length < filesize)
						foo = buf.length;
					else
						foo = (int) filesize;
					foo = in.read(buf, 0, foo);
					if (foo < 0) {
						// error
						break;
					}
					fos.write(buf, 0, foo);
					filesize -= foo;
					if (filesize == 0L)
						break;
				}
				fos.close();
				fos = null;
				if (checkAck(in) != 0) {
					return ExecutionStatus.stopped;
				}
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();
			}
			channel.disconnect();
			//
			status = sudoCommand( "rm " + temp );
			return status; // ref != -1;
		} catch ( JSchException | IOException e ){
			if( e.getMessage().toUpperCase().indexOf( "SESSION IS DOWN" ) != -1 ){
				return ExecutionStatus.sessionDown;
			}
			logger.error( "SSHMANAGER error on (sudo)ScpFrom", e );
			return ExecutionStatus.unknownError;
		}
	}
	
	protected int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;
		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			} else if (b == 2) { // fatal error
				System.out.print(sb.toString());
			} else if( b > 0 ){ // boh...
	//			System.out.print(sb.toString());
			}
		}
		return b;
	}

}