package org.naarani.selenev.embedded;

import org.naarani.selenev.embedded.centos.AnsibleKubeCentos;
import org.naarani.selenev.ssh.centos.UserLibCentos;

public class MainAnsibleCentos2Node extends AMainKube {

	static public void main( String[] args ) throws Exception {
		org.apache.log4j.BasicConfigurator.configure();
		
		if( args.length == 1 )
			rootPwd = args[0];
		else
			throw new Exception( "missing root password" );
		
		workdir = "workspace/centos/ansible";
		SERVER_LABEL = "ANSI-CENTOS7-SVR";
		SERVER_DISTRO = "fra1/centos-7-x64";
		engine = new AnsibleKubeCentos();
		engine.setUserLib( new UserLibCentos() );
		AMainKube.main( 2 );
	}

}