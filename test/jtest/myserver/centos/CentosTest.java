package jtest.myserver.centos;

import static org.junit.Assert.*;
import org.junit.Test;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.SshServerManager;
import org.naarani.ecantonchiro.ssh.centos.UserLibCentos;

public class CentosTest {

	protected static final String rootUser = System.getProperty("test.jk.rootName", "root");
	protected static final String rootPwd = System.getProperty("test.jk.rootPwd", "rootExamplePWd");

	protected static final String testhost = System.getProperty("test.jk.host", "192.168.1.203");

	UserLibCentos userLib = new UserLibCentos();
	
	@Test
	public void centosRelease(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, rootPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );
			// assertTrue( "should run command with SUDO", Status.Done == s.command( "cat /etc/redhat-release" ) );
			String ref = userLib.checkLinuxDistro( s ).toUpperCase();
			assertTrue( "should find name", ref.startsWith( "CENTOS" ) || ref.startsWith( "REDHAT" ) );

			ref = ref.substring( ref.indexOf( "RELEASE" ) + "RELEASE".length() );
			assertTrue( "should find name", ref.startsWith( "7" ) );
			
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

	@Test
	public void kernelRelease(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, rootPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );
			// assertTrue( "should run command with SUDO", Status.Done == s.command( "uname -r" ) );
			String ref = userLib.checkKernel( s ).toUpperCase();
			// ..........
			//FIXME
			
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

}