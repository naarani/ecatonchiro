package org.naarani.ecantonchiro.ssh.centos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.naarani.core.exceptions.StopAction;
import org.naarani.ecantonchiro.ssh.CliInteract;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.IFileLib;
import org.naarani.ecantonchiro.ssh.SshServerManager;

public class FileLibCentos implements IFileLib {

	@Override
	public ExecutionStatus fileOrDirExists( SshServerManager svr, String path ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "ls " + path + " -l" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		if( status == ExecutionStatus.Done ){
			return ExecutionStatus.Done_CHECK_TRUE;
		} else {
			if( svr.getCmdErrLog().getText().toUpperCase().indexOf( "NO SUCH" ) != -1 
					|| svr.getCmdLog().getText().toUpperCase().indexOf( "NO SUCH" ) != -1 ){
				return ExecutionStatus.Done_CHECK_FALSE;
			} else {
				return ExecutionStatus.unknownError;
			}
		}
	}

	@Override
	public ExecutionStatus sudoFileOrDirExists( SshServerManager svr, String path ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.sudoCommand( "ls " + path + " -l" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		if( status == ExecutionStatus.Done ){
			return ExecutionStatus.Done_CHECK_TRUE;
		} else {
			if( svr.getCmdErrLog().getText().toUpperCase().indexOf( "NO SUCH" ) != -1 
					|| svr.getCmdLog().getText().toUpperCase().indexOf( "NO SUCH" ) != -1 ){
				return ExecutionStatus.Done_CHECK_FALSE;
			} else {
				return ExecutionStatus.unknownError;
			}
		}
	}

	@Override
	public ExecutionStatus chmod( SshServerManager svr, String val, String path, String opt ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "chmod " + val + " " + path + " " + ( opt != null ? opt : "" ) );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus sudoChmod( SshServerManager svr, String val, String path, String opt ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.sudoCommand( "chmod " + val + " " + path + " " + ( opt != null ? opt : "" ) );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus chown( SshServerManager svr, String val, String path, String opt ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "chown " + val + " " + path + " " + ( opt != null ? opt : "" ) );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus sudoChown( SshServerManager svr, String val, String path, String opt ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.sudoCommand( "chown " + val + " " + path + " " + ( opt != null ? opt : "" ) );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus suCreateCatFile( SshServerManager svr, String rootPwd, String remotePath, File file ) throws IOException {
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		Path pathNio = Paths.get( file.toURI() );
		String txt = new String( java.nio.file.Files.readAllBytes( pathNio ) );
		ExecutionStatus status = suCreateCatFile( svr, rootPwd, remotePath, txt );
		return status;
	}

	@Override
	public ExecutionStatus sudoCreateCatFile( SshServerManager svr, String remotePath, File file ) throws IOException {
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		Path pathNio = Paths.get( file.toURI() );
		String txt = new String( java.nio.file.Files.readAllBytes( pathNio ) );
		ExecutionStatus status = sudoCreateCatFile( svr, remotePath, txt );
		return status;
	}

	@Override
	public ExecutionStatus mkdir( SshServerManager svr, String remotePath ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "mkdir " + remotePath + " \n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus sudoMkdir( SshServerManager svr, String remotePath ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.sudoCommand( "mkdir " + remotePath + " \n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus sudoCp( SshServerManager svr, String fromFile, String toDestDir, String options ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.sudoCommand( "cp " + fromFile + " " + toDestDir + " " + ( options != null ? options : "" ) + "\n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus cp( SshServerManager svr, String fromFile, String toDestDir, String options ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "cp " + fromFile + " " + toDestDir + " " + ( options != null ? options : "" ) + "\n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus sudoMv( SshServerManager svr, String fromFile, String toFile, String options ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.sudoCommand( "mv " + fromFile + " " + toFile + " " + ( options != null ? options : "" ) + "\n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus mv( SshServerManager svr, String fromFile, String toFile, String options ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "mv " + fromFile + " " + toFile + " " + ( options != null ? options : "" ) + "\n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	@Override
	public ExecutionStatus createCatFile( SshServerManager svr, String remotePath, String txt ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			status = svr.command( "cat <<\"EOF\" > " + remotePath + " \n" + txt + "\nEOF\nexit\n" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}
	
	@Override
	public ExecutionStatus sudoCreateCatFile( SshServerManager svr, String remotePath, String txt ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		String tmp = "~/temp-create";
		ExecutionStatus status = null;
		if( this.fileOrDirExists( svr, tmp ) == ExecutionStatus.Done_CHECK_FALSE ){
			status = this.mkdir( svr, tmp );
			if( status != ExecutionStatus.Done )
				return status;
		}
		String tmpRef = tmp + "/file-" + System.currentTimeMillis() + ".tmp";
		status = createCatFile( svr, tmpRef, txt );
		if( status != ExecutionStatus.Done )
			return status;
		status = sudoChown( svr, "root:root", tmpRef, null );
		if( status != ExecutionStatus.Done )
			return status;
		status = sudoMv( svr, tmpRef, remotePath, null );
		return status;
	}
	
	/**
	 * cant work if login root is blocked...
	 * 
	 * @param svr
	 * @param rootPwd
	 * @param remotePath
	 * @param txt
	 * @return
	 */
	@Override
	public ExecutionStatus suCreateCatFile( SshServerManager svr, String rootPwd, String remotePath, String txt ){
		svr.getCmdErrLog().reset();
		svr.getCmdLog().reset();
		CatInteraction action = new CatInteraction( svr, rootPwd, remotePath, txt ); 
		ExecutionStatus status;
		try {
			status = svr.command( "su", action );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}

	static public class CatInteraction implements CliInteract {

		boolean done = false;
		boolean exit = false;
		SshServerManager s3;
		String cfg;
		String rootPWD;
		String remotePath;

		public CatInteraction( SshServerManager s3, String rootPWD, String remotePath, String cfg ){
			this.s3 = s3;
			this.remotePath = remotePath;
			this.rootPWD = rootPWD;
			this.cfg = cfg + "\nEOF\n";
		}

		@Override
		public String action(){
			if( !done && s3.getCmdLog().getText().endsWith( ": " ) ){ // as NON ROOT
				done = true;
				return rootPWD + "\n";
			} else if( !done && s3.getCmdLog().getText().endsWith( "# " ) ){ // as ROOT
				done = true;
				return null;
			} else if ( done && !exit ){
				exit = true;
				return "cat <<\"EOF\" > " + remotePath + " \n" + cfg + "\nexit\n";
			} else {
				return null;
			}	
		}

	}

}