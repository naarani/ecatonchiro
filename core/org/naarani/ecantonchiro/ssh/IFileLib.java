package org.naarani.ecantonchiro.ssh;

import java.io.File;
import java.io.IOException;

public interface IFileLib { // implements IUserLib {

	public ExecutionStatus fileOrDirExists( SshServerManager svr, String path );

	public ExecutionStatus sudoFileOrDirExists( SshServerManager svr, String path );

	public ExecutionStatus chmod( SshServerManager svr, String val, String path, String opt );

	public ExecutionStatus sudoChmod( SshServerManager svr, String val, String path, String opt );

	public ExecutionStatus chown( SshServerManager svr, String val, String path, String opt );

	public ExecutionStatus sudoChown( SshServerManager svr, String val, String path, String opt );

	public ExecutionStatus suCreateCatFile( SshServerManager svr, String rootPwd, String remotePath, File file ) throws IOException;

	public ExecutionStatus sudoCreateCatFile( SshServerManager svr, String remotePath, File file ) throws IOException;

	public ExecutionStatus mkdir( SshServerManager svr, String remotePath );

	public ExecutionStatus sudoMkdir( SshServerManager svr, String remotePath );

	public ExecutionStatus sudoCp( SshServerManager svr, String fromFile, String toDestDir, String options );

	public ExecutionStatus cp( SshServerManager svr, String fromFile, String toDestDir, String options );

	public ExecutionStatus sudoMv( SshServerManager svr, String fromFile, String toFile, String options );

	public ExecutionStatus mv( SshServerManager svr, String fromFile, String toFile, String options );

	public ExecutionStatus createCatFile( SshServerManager svr, String remotePath, String txt );
	
	public ExecutionStatus sudoCreateCatFile( SshServerManager svr, String remotePath, String txt );
	
	/**
	 * cant work if login root is blocked...
	 * 
	 * @param svr
	 * @param rootPwd
	 * @param remotePath
	 * @param txt
	 * @return
	 */
	public ExecutionStatus suCreateCatFile( SshServerManager svr, String rootPwd, String remotePath, String txt );

}