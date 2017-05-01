package jtest.myserver.centos;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.naarani.selenev.ssh.SshServerManager;
import org.naarani.selenev.ssh.centos.UserLibCentos;
import org.naarani.selenev.ssh.ExecutionStatus;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CreateUserAndSudoCentos {

	protected static final String rootUser = System.getProperty("test.jk.rootName", "root");
	protected static final String rootPwd = System.getProperty("test.jk.rootPwd", "rootExamplePWd");

	protected static final String testhost = System.getProperty("test.jk.host", "192.168.1.203");

	protected static final String userToCreate = System.getProperty("test.jk.name", "myPreferredUser");
	protected static final String userPwd = System.getProperty("test.jk.pwd", "abc__99_abbbb");
	protected static final byte[] sshkeylock = System.getProperty("test.jk.lock", "keyNameLock").getBytes();
	
	UserLibCentos userLib = new UserLibCentos();
	
	@Before
	public void setup(){
	}

	@Test
	public void a0_checkUserExists(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, rootPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );
			assertTrue( "should be able to not find this", ExecutionStatus.Done_CHECK_FALSE == userLib.checkUserExists( s, "NON_existing_Test_13432389" ) );
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

	@Test
	public void a1_createUser(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, rootPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );
			assertTrue( "should be able to create user", ExecutionStatus.Done == userLib.createUser( s, false, userToCreate, userPwd ) );
			s.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

	@Test
	public void a2_addWheel(){
		try {
			SshServerManager s = new SshServerManager( rootUser + "@" + testhost, rootPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );
			assertTrue( "should be able to wheel user", ExecutionStatus.Done == userLib.makeUserSudoer( s, false, userToCreate ) );
			s.close();
			//
			SshServerManager s2 = new SshServerManager( userToCreate + "@" + testhost, userPwd );
			assertTrue( "should call for valid password", s2.connect() == ExecutionStatus.Done );
			assertTrue( "should test new user with SUDO", ExecutionStatus.Done == s2.sudoCommand( "ls -l" ) );
			s2.close();
		} catch ( Exception e ){
			assertTrue( "errore " + e.getMessage(), false );
			e.printStackTrace();
		}
	}

	@Test
	public void a3_sshkey(){
		try {
			SshServerManager s = new SshServerManager( userToCreate + "@" + testhost, userPwd );
			assertTrue( "should call for valid password", s.connect() == ExecutionStatus.Done );

			String file = "private/test/key" + userToCreate;
			File fileRef = new File( file );
			if( !fileRef.exists() ){
				assertTrue( "should create key", ExecutionStatus.Done == userLib.generateSshKey( file, s, sshkeylock ) );
			}
			assertTrue( "should push sshkey", ExecutionStatus.Done == userLib.setUserSshKey( s, false, null, new File( file + ".pub" ) ) ); 
//			assertTrue( "should push private sshkey", Status.Done == SelectedUserLib.getUserLib().setUserSshPrivateKey( s, new File( file ) ) );
			
			
			
			// check all
			SshServerManager s2 = new SshServerManager( userToCreate + "@" + testhost, fileRef.getAbsolutePath(), sshkeylock, userPwd ); 
			assertTrue( "should call for valid sshkey", s2.connect() == ExecutionStatus.Done );
			assertTrue( "should test new user with sshkey", ExecutionStatus.Done == s2.command( "ls -l" ) );
			assertTrue( "should test new user with sshkey+sudo", ExecutionStatus.Done == s2.sudoCommand( "ls -l" ) );
			s2.close();
		} catch ( Exception e ){
			e.printStackTrace();
			assertTrue( "errore " + e.getMessage(), false );
		}
	}

}