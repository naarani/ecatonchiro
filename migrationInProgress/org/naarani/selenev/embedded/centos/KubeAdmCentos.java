package org.naarani.selenev.embedded.centos;

import java.io.FileOutputStream;
import org.naarani.core.exceptions.StopAction;
import org.naarani.selenev.ssh.CliInteract;
import org.naarani.selenev.ssh.SshServerManager;
import org.naarani.selenev.ssh.centos.FileLibCentos;
import org.naarani.selenev.ssh.centos.ServiceLibCentos;
import org.naarani.selenev.ssh.ExecutionStatus;

public class KubeAdmCentos extends AnsibleKubeCentos {

	public KubeAdmCentos(){
		aservice = new ServiceLibCentos(); 
		afl = new FileLibCentos();
		_DASHBOARD_VERSION = "master";
	}

	protected String kubeVer = "v1.6.2";
	protected String join;
	
	protected boolean initAllKubeAdm( SshServerManager s3 ){
		ExecutionStatus status;
		boolean _KILL_PREVIOUS = false;
		try {
			boolean DOCKER_ENG = false;
			if( DOCKER_ENG ){
				String dockRep = "/etc/yum.repos.d/docker.repo";
				String text = "[dockerrepo]\n"
					+ "name=Docker Repository\n"
					+ "baseurl=https://yum.dockerproject.org/repo/experimental/centos/7/\n"
					+ "enabled=1\n"
					+ "gpgcheck=1\n"
					+ "gpgkey=https://yum.dockerproject.org/gpg\n"
					;
				status = afl.sudoCreateCatFile( s3, dockRep, text );
			}
			final String body = "[kubernetes]\n"
					+ "name=Kubernetes\n"
					+ "baseurl=http://yum.kubernetes.io/repos/kubernetes-el7-x86_64\n"
					+ "enabled=1\n"
					+ "gpgcheck=1\n"
					+ "repo_gpgcheck=1\n"
					+ "gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg\n"
					;
	//				+ "EOF";
			status = afl.sudoCreateCatFile( s3, "/etc/yum.repos.d/kubernetes.repo", body );
			logger.debug( "setup standard yum repository for kubernetes, dokcer e kubeadm" );
			if( _KILL_PREVIOUS ){
				CliInteract m1 = new ServiceUserSelection( s3, userName, userPwd );
				status = s3.sudoCommand( "systemctl disable docker && systemctl stop docker", m1 );
				CliInteract m2 = new ServiceUserSelection( s3, userName, userPwd );
				status = s3.sudoCommand( "systemctl disable kubelet && systemctl stop kubelet", m2 );
				status = s3.sudoCommand( "yum remove -y docker docker-engine kubelet kubeadm kubectl kubernetes-cni" );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "yum remove -y docker kubelet kubeadm kubectl kubernetes-cni" ) );
		
		//		status = s3.sudoCommand( "yum list available kubelet --showduplicates -y" );
		//		assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "yum list available kubeadm --showduplicates" ) );
		//		assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "yum list available kubectl --showduplicates" ) );
		//		assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "yum list available kubernetes-cni --showduplicates" ) );
				
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "yum install iptables-services.x86_64 -y" ) );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "systemctl mask firewalld.service" ) );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "systemctl start iptables" ) );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "systemctl enable iptables" ) );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "systemctl unmask iptables" ) );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "iptables -F" ) );
				//assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "service iptables save" ) );
				logger.debug( "removed previous kubernetes, dokcer repo and service" );
			}
	//		assertTrue( "should install kube/dock", Status.Done == s3.sudoCommand( "yum install -y docker kubelet-1.5.4-0.x86_64 kubeadm kubectl kubernetes-cni" ) );
			if( DOCKER_ENG )
				status = s3.sudoCommand( "yum install -y docker-engine kubelet kubeadm kubectl kubernetes-cni" ); // Complete!
			else
				status = s3.sudoCommand( "yum install -y docker kubelet kubeadm kubectl kubernetes-cni" ); // Complete!
			CliInteract multiple = new ServiceUserSelection( s3, userName, userPwd );
			
			status = s3.sudoCommand( "yum update -y" );
			//		/etc/systemd/system/kubelet.service.d/10-kubeadm.conf [root con non so quali flag...
			logger.debug( "updated release" );
			
			status = s3.sudoCommand( "systemctl enable docker && systemctl start docker", multiple );
			multiple = new ServiceUserSelection( s3, userName, userPwd );
			status = s3.sudoCommand( "systemctl enable kubelet && systemctl start kubelet", multiple );
			status = s3.sudoCommand( "systemctl -l status docker", multiple );
			status = s3.sudoCommand( "systemctl -l status kubelet", multiple );
			logger.debug( "preflight docker and kubelet" );
			s3.getCmdErrLog().reset();
			s3.getCmdLog().reset();
			if( _KILL_PREVIOUS ){
				status = s3.sudoCommand( "kubeadm reset" );
				status = s3.sudoCommand( "sysctl -w net.bridge.bridge-nf-call-iptables=\"1\"" );
				status = s3.sudoCommand( "sysctl -w net.bridge.bridge-nf-call-ip6tables=1" );
				logger.debug( "reset kubeadm if present" );
			}
			return true;
		} catch ( Exception e ){
			logger.error( "Ops... RUN failure: GENERAL FAILURE!", e );
			return false;
		}
	}

	@Override
	public boolean run(){
		try {
			connectMainServer();
			SshServerManager s3 = mainS3;
			try {
				ExecutionStatus status;
				initAllKubeAdm( s3 );
				
				s3.getCmdErrLog().reset();
				s3.getCmdLog().reset();
				status = s3.sudoCommand( "kubeadm init --kubernetes-version " + kubeVer ); // + " --token-ttl 0" ) );
				String msg = s3.getCmdLog().getText();
				int pos = msg.indexOf( "kubeadm join -" ) ;
				if( pos != -1 ){
					join = msg.substring( pos ).trim().replace( "\r", "" ).replace( "\n", "" );
				}
				logger.debug( "Master online!");
				mountAllWorkes();

				logger.debug( "Master AND workes online!");
				try {
					setupDashboard();
					checkApiServer();
					psDocker();
				} catch ( StopAction e ){
					logger.error( "SHOULD HAVE CALLED API SERVER TO TEST IT..." );
				}
				this.createKubeConfig();
				return true;
			} catch ( Exception e ){
				logger.error( "Ops... RUN failure: GENERAL FAILURE(29)!", e );
			}
		} catch ( Exception e ){
			logger.error( "Ops... RUN failure: GENERAL FAILURE(1)!", e );
		} finally {
			try {
				closeConnectionMainServer();
			} catch ( StopAction e ){
				logger.error( "Ops... RUN failure: GENERAL FAILURE(0)!", e );
			}
		}
		return false;
	}

	private boolean mountAllWorkes() throws StopAction {
		for( int i = 1; i < publicIpList.length; i++ ){
			try {
				SshServerManager s4 = new SshServerManager( userName + "@" + publicIpList[i], sshKeyUser, null, userPwd ); 
				ExecutionStatus status = s4.connect();
				if( status != ExecutionStatus.Done ){
					logger.error( "!! cannot connect ANSIBLE SERVER..." );
				    return false;
				}
				initAllKubeAdm( s4 );
				s4.sudoCommand( join );
				s4.close();
				logger.debug( "worker " + publicIpList[i] + " online!");
			} catch ( StopAction e ){
				throw e;
			}
		}
		return true;
	}

	public boolean createKubeConfig(){
		try {
			SshServerManager s3 = mainS3;
			String adm = "/etc/kubernetes/admin.conf";
		    adm = s3.sudoScpFrom( adm );
		    /*
		    sudo cp /etc/kubernetes/admin.conf $HOME/
		    sudo chown $(id -u):$(id -g) $HOME/admin.conf
		    export KUBECONFIG=$HOME/admin.conf
		     */
		    kubecfg_text = adm;
			if( _EXPORT_FILE ){
			    FileOutputStream ou = new FileOutputStream( _KUBECFG_FILE );
			    ou.write( kubecfg_text.getBytes() );
			    ou.close();
				logger.debug( "KUBECONFIG EXPORTED" );
			}
			return true;
		} catch ( Exception e ){
			logger.error( "general failure", e );
			return false;
		}
	}

	static public class ServiceUserSelection implements CliInteract {
		
		boolean done = false;
		boolean exit = false;
		SshServerManager s3;
		
		String user, pwd;
		
		public ServiceUserSelection( SshServerManager s3, String user, String pwd ){
			this.pwd = pwd;
			this.user = user;
			this.s3 = s3;
			s3.getCmdLog().reset();
		}
		
		@Override
		public String action(){
			if( !done && s3.getCmdLog().getText().toLowerCase().indexOf( "multiple " ) != -1 ){
				done = true;
				return null;
			} else if ( done && !exit ){
				if( s3.getCmdLog().getText().toLowerCase().indexOf( "choose " ) != -1 ){
					int pos = s3.getCmdLog().getText().lastIndexOf( " " + user );
					if( pos != -1 ){
						int pos2 = s3.getCmdLog().getText().substring( 0, pos ).lastIndexOf( '.' );
						int pos3 = s3.getCmdLog().getText().substring( 0, pos2 ).lastIndexOf( '\n' );
						String n = s3.getCmdLog().getText().substring( pos3, pos2 ).replace( "\n", "" ).trim();
						exit = true;
						return n + "\n";
					}
				}
			} else if ( done && exit ){
				if( s3.getCmdLog().getText().toLowerCase().indexOf( "password: " ) != -1 ){
					return pwd + "\n";
				}
			} else {
				if( s3.getCmdLog().getText().toLowerCase().indexOf( "password: " ) != -1 ){
					return pwd + "\n";
				}
			}	
			return null;
		}

	};
	
}