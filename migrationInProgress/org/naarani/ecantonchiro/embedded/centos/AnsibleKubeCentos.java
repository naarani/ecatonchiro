package org.naarani.ecantonchiro.embedded.centos;

import java.io.File;
import org.naarani.core.exceptions.StopAction;
import org.naarani.ecantonchiro.embedded.AEngineKubeCreator;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.SshServerManager;
import org.naarani.ecantonchiro.ssh.centos.FileLibCentos;
import org.naarani.ecantonchiro.ssh.centos.ServiceLibCentos;

public class AnsibleKubeCentos extends AEngineKubeCreator {

	public AnsibleKubeCentos(){
		aservice = new ServiceLibCentos(); 
		afl = new FileLibCentos();
	}

	@Override
	public boolean presetOs() throws StopAction {
		ServiceLibCentos service = new ServiceLibCentos(); 
		String nHost;
		boolean done = false;
		ExecutionStatus status = ExecutionStatus.stopped;
		for( int i = 0; i < publicIpList.length; i++ ){
			nHost = publicIpList[i];
			logger.debug( "CALLING(presets)..." + nHost );
			SshServerManager svr = null;
			if( new File( sshKeyUser ).exists() ){
				svr = new SshServerManager( userName + "@" + nHost, sshKeyUser, null, userPwd ); 
			} else {
				svr = new SshServerManager( userName + "@" + nHost, null, null, userPwd );
			}
			status = svr.connect();
			if( status != ExecutionStatus.Done ){
				return false;
			}
			done = ExecutionStatus.Done == svr.sudoCommand( "hostname" );
			if( !done  )
				break;
			String tag = svr.getCmdLog().getText().trim();
			int pos = tag.lastIndexOf( "\n" );
			if( pos != -1 ){
				tag = tag.substring( pos +1 );
				tag = tag.replace( "\r", "" ).trim();
			}
			tag = tag.replace( "svr", "node" );
			done = false;
			if( status == ExecutionStatus.Done ){
				logger.debug( "set NTPD " + tag );
				status = service.installAndsetupNtpd( svr );
				if( status == ExecutionStatus.Done ){
					logger.debug( "set SELINUX " + tag );
					status = service.disableSelinux( svr );
					if( status == ExecutionStatus.Done ){
						logger.debug( "set FIREWALL " + tag );
						status = service.disableFirewall( svr );
						if( status == ExecutionStatus.Done ){
							done = true;
						}
					}
				}
			}
			svr.close();
			if( !done )
				break;
		}
		return status == ExecutionStatus.Done;
	}
	
}