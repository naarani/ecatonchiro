package jtest.myserver.centos;

import static org.junit.Assert.*;
import org.junit.Test;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.SshServerManager;

public class LoginErrorTests {

	protected static final String rootUser = System.getProperty("test.jk.rootName", "root");
	protected static final String rootPwd = System.getProperty("test.jk.rootPwd", "rootExamplePWd");

	protected static final String testhost = System.getProperty("test.jk.host", "192.168.1.203");

	@Test
	public void login(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, rootPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

	@Test
	public void wrongPassword(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, "abc123" );
			assertTrue( "should call for wrong password", s.connect() == ExecutionStatus.passwordWrong );
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

	@Test
	public void timeout(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + "1.1.1.1", "abc123" );
			s.setConnectionTimeoutConnect( 1000 );
			s.setCommandTimeoutConnect( 1000 );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.connectionTimeout );
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

}