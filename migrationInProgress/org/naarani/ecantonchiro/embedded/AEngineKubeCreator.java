package org.naarani.ecantonchiro.embedded;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import org.apache.log4j.Logger;
import org.naarani.core.exceptions.StopAction;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.IFileLib;
import org.naarani.ecantonchiro.ssh.IServiceLib;
import org.naarani.ecantonchiro.ssh.IUserLib;
import org.naarani.ecantonchiro.ssh.SshServerManager;

public class AEngineKubeCreator {

	static protected Logger logger = Logger.getLogger( AEngineKubeCreator.class );
	
	protected boolean _EXPORT_FILE = true;
	protected boolean _only_SSHKEY = true;

	protected String[] privateIpList;
	protected String[] publicIpList;
	
	protected String mainServer;

	protected String userName = "ecatonchiro";
	protected String userPwd = "a_a223Fsad3__";
	protected String sshKeyUser;

	protected String rootUser;
	protected String rootPwd;
	protected String sshKeyRoot;
	
	protected File workspace;
	protected String _BRANCH_KARGO = "v2.1.1";
	protected String _DASHBOARD_VERSION = "v1.5.1";
	protected SshServerManager mainS3;

	protected String _KUBECFG_FILE;
	protected String kubecfg_text = null;
	protected IServiceLib aservice;
	protected IFileLib afl;
	protected IUserLib userLib;

	public AEngineKubeCreator(){}
	
	public void prepare( File workspace, String[] list, String[] listPublic, String mainServer, String sshKeyUser, String sshKeyRoot, String rootUser, String rootPwd ){
		this.privateIpList = list;
		this.publicIpList = listPublic;
		
		this.mainServer = mainServer;
		
		this.sshKeyUser = sshKeyUser;

		this.rootUser = rootUser;
		this.rootPwd = rootPwd;
		this.sshKeyRoot = sshKeyRoot;

		this.workspace = workspace;
		workspace.mkdir();
		_KUBECFG_FILE = new File( workspace, "kubeconfig.txt" ).getAbsolutePath();
	}

	public void setUserLib( IUserLib userLib ){
		this.userLib = userLib;
	}

	public IUserLib getUserLib() {
		return userLib;
	}

	public boolean operatorUser(){
		String nHost = "";
		try {
			for( int i = 0; i < publicIpList.length; i++ ){
				nHost = publicIpList[i];
				logger.debug( "SETUP USER: ..." + nHost );
				try {
					SshServerManager s3 = null;
					if( new File( sshKeyRoot ).exists() )
						s3 = new SshServerManager( rootUser + "@" + nHost, sshKeyRoot, null, rootPwd ); 
					else
						s3 = new SshServerManager( rootUser + "@" + nHost, null, null, rootPwd );
					ExecutionStatus status = s3.connect();
					for( int j = 0; j < 5; j++ ){
						if( status == ExecutionStatus.Done ){
							break;
						} else if( status != ExecutionStatus.Done && j == 5 ){
							logger.error( "problem... login preflight" + nHost );
							return false;
						} else {
							logger.error( "problem... login preflight (RETRY) " + nHost );
							try {
								Thread.sleep( 15 * 1000 );
							} catch ( InterruptedException e ){
								logger.error( "general failure", e );
							}
							if( new File( sshKeyRoot ).exists() ){
								s3 = new SshServerManager( rootUser + "@" + nHost, sshKeyRoot, null, rootPwd ); 
							} else {
								s3 = new SshServerManager( rootUser + "@" + nHost, null, null, rootPwd );
							}
						}
					}
					status = userLib.createUser( s3, true, userName, userPwd );
					if( status != ExecutionStatus.Done ){
						logger.error( "problem... create user " + nHost );
						return false;
					}
					status = userLib.makeUserSudoer( s3, true, userName );
					if( status != ExecutionStatus.Done ){
						logger.error( "problem... sudo preflight " + nHost );
						return false;
					}
					status = userLib.setUserSshKey( s3, true, userName, new File( sshKeyUser + ".pub" ) ); 
					if( status != ExecutionStatus.Done ){
						logger.error( "problem... preflight pubblica " + nHost );
						return false;
					}
					status = userLib.setUserSshPrivateKey( s3, true, userName, new File( sshKeyUser ), true ); 
					if( status != ExecutionStatus.Done ){
						logger.error( "problem... preflight privata " + nHost );
						return false;
					}
					s3.close();
					// VeRIFICA UTENTE pwd
					if( !_only_SSHKEY ){
						final SshServerManager s4 = new SshServerManager( userName + "@" + nHost, null, null, userPwd ); 
						status = s4.connect();
						if( status != ExecutionStatus.Done ){
							logger.error( "problem... conn k2" + nHost );
							return false;
						}
						status = s4.sudoCommand( "ls -l" );
						if( status != ExecutionStatus.Done ){
							logger.error( "problem... valida k2" + nHost );
							return false;
						}
						s4.close();
					}
					// VeRIFICA UTENTE chiavessh
					final SshServerManager s5 = new SshServerManager( userName + "@" + nHost, sshKeyUser, null, userPwd ); 
					status = s5.connect();
					if( status != ExecutionStatus.Done ){
						logger.error( "problem... ansbile con" + nHost );
						return false;
					}
					status = s5.sudoCommand( "ls -l" );
					if( status != ExecutionStatus.Done ){
						logger.error( "problem... ansibleLS" + nHost );
						return false;
					}
					s5.close();
				} catch ( Exception e ){
					logger.error( "FALLITO... check user ansi" + nHost, e );
					return false;
				}
			}
			return true;
		} catch ( Exception e ){
			logger.error( "general failure", e );
			return false;
		}
	}

	public boolean createKubeConfig(){
		try {
			SshServerManager s3 = mainS3;
		    String certificate_authority = "/etc/kubernetes/ssl/ca.pem";
			String client_key = "/etc/kubernetes/ssl/admin-node1-key.pem";
		    String client_certificate= "/etc/kubernetes/ssl/admin-node1.pem";      
		    certificate_authority = s3.sudoScpFrom( certificate_authority );
		    client_key = s3.sudoScpFrom( client_key );
		    client_certificate = s3.sudoScpFrom( client_certificate );
		    /*
			if( _EXPORT_FILE ){
			    try {
				    FileOutputStream ou = new FileOutputStream( new File( workspace, "certificate_authority" );
				    ou.write( certificate_authority.getBytes() );
				    ou.close();
				} catch (Exception e) {
					logger.error( "general failure", e );
				}
			    try {
				    FileOutputStream ou = new FileOutputStream( new File( workspace, "client_key" ) );
				    ou.write( client_key.getBytes() );
				    ou.close();
				} catch (Exception e) {
					logger.error( "general failure", e );
				}
			    try {
				    FileOutputStream ou = new FileOutputStream( new File( workspace, "client_certificate" ) );
				    ou.write( client_certificate.getBytes() );
				    ou.close();
				} catch (Exception e) {
					logger.error( "general failure", e );
				}
			}
			*/
		    InputStream in = AEngineKubeCreator.class.getResource( "config.txt" ).openStream();
		    int size = 0;
		    byte[] buffer = new byte[2048];
		    String msg = "";
		    while( ( size = in.read( buffer,  0, 2048 ) ) != -1 ){
		    	msg += new String( buffer, 0, size );
		    }
		    in.close();
		    msg = msg.replace( "%CA", b64(certificate_authority) );
		    msg = msg.replace( "%PRIVATE", b64(client_key) );
		    msg = msg.replace( "%PUBLIC", b64(client_certificate) );
		    msg = msg.replace( "%CXNAME", "cosmic" );
		    msg = msg.replace( "%CLUSTERNAME", "kubocosmico" );
		    msg = msg.replace( "%USER", "admin" );
		    msg = msg.replace( "%IP", mainServer );
		    /*
			kubectl config set-cluster javakube-cluster --server=https://<ip>:6443 --certificate-authority=certificate_authority --embed-certs=true
			kubectl config set-credentials default-admin --certificate-authority=certificate_authority --client-key=client_key --client-certificate=client_certificate --embed-certs=true
			kubectl config set-context default-system --cluster=javakube-cluster --user=default-admin
			kubectl config use-context default-system
		    */
		    kubecfg_text = msg;
			if( _EXPORT_FILE ){
			    FileOutputStream ou = new FileOutputStream( _KUBECFG_FILE  );
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

	public boolean connectMainServer(){
		mainS3 = new SshServerManager( userName + "@" + mainServer, sshKeyUser, null, userPwd ); 
		ExecutionStatus status = mainS3.connect();
		if( status != ExecutionStatus.Done ){
			logger.error( "!! cannot connect ANSIBLE SERVER..." );
		    return false;
		}
		return true;
	}

	public void closeConnectionMainServer() throws StopAction {
		mainS3.close();
	}

	public boolean preflight(){
		if( this.operatorUser() ){
			try {
				if( this.presetOs() ){
					logger.debug( "PREFLIGHT COMPLETE : OK!");
					return true;
				}
			} catch (StopAction e) {
				logger.error( "general failure", e );
			}
		}
		logger.error( "Ops preflight FAILURE..." );
		return false;
	}

	public boolean run(){
		try {
			connectMainServer();
			if( this.kargoRun( false ) ){
				logger.debug( "KARGO RUN WORKED FINE!");
				try {
					setupDashboard();
					checkApiServer();
					psDocker();
				} catch ( StopAction e ){
					logger.error( "SHOULD HAVE CALLED API SERVER TO TEST IT..." );
				}
				this.createKubeConfig();
				return true;
			}
			logger.error( "Ops... RUN failure" );
		} catch ( Exception e ){
			logger.error( "Ops... RUN failure: GENERAL FAILURE!", e );
		} finally {
			try {
				closeConnectionMainServer();
			} catch ( StopAction e ){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}

	protected ExecutionStatus psDocker() throws StopAction {
		SshServerManager s3 = mainS3;
		ExecutionStatus status;
		s3.getCmdErrLog().reset();
		s3.getCmdLog().reset();
	    status = s3.sudoCommand( "docker ps" );
	    String t1 = s3.getCmdLog().getText();
		if( _EXPORT_FILE ){
		    try {
			    FileOutputStream ou = new FileOutputStream( new File( workspace, "docker_ps.txt" ) );
			    ou.write( ( t1 ).getBytes() );
			    ou.close();
			} catch (Exception e) {
				logger.error( "general failure", e );
			}
		}
		logger.debug( "DOCKER PS:\r\n" + t1 );
		return status;
	}
	
	protected void checkApiServer() throws StopAction {
		SshServerManager s3 = mainS3;
		ExecutionStatus status;
	    s3.getCmdErrLog().reset();
		s3.getCmdLog().reset();
	    status = s3.command( "kubectl version" );
	    String t1 = s3.getCmdLog().getText();
	    s3.getCmdErrLog().reset();
		s3.getCmdLog().reset();
	    status = s3.command( "curl http://localhost:8080/" );
	    String t2a = s3.getCmdLog().getText();
	    s3.getCmdErrLog().reset();
		s3.getCmdLog().reset();
	    status = s3.command( "curl http://" + privateIpList[0] + ":8080/" );
	    String t2b = s3.getCmdLog().getText();
		if( _EXPORT_FILE ){
		    try {
			    FileOutputStream ou = new FileOutputStream( new File( workspace, "kube_ver_and_api_log.txt" ) );
			    ou.write( ( "\r\n-------------------KUBE VERSION CLIENT/SERVER-----------------------\r\n" + t1 
			    		+ "\r\n-------------------API IP LOCALHOST-----------------------\r\n" + t2a 
			    		+ "\r\n-------------------API IP PRIVATO-----------------------\r\n" + t2b ).getBytes() );
			    ou.close();
			} catch (Exception e) {
				logger.error( "general failure", e );
			}
		}
		logger.debug( "API VERSION:\r\n" + t1 );
		logger.debug( "API LOCALHOST CALL:\r\n" + t2a );
		logger.debug( "API IP CALL:\r\n" + t2b );
	}
	
	static public CharSequence b64( String ref ){
		return Base64.getEncoder().encodeToString( ref.getBytes() );
	}

	public boolean setupDashboard(){
		try {
			SshServerManager s3 = mainS3;
			ExecutionStatus status;
		    status = s3.command( "kubectl create -f https://raw.githubusercontent.com/kubernetes/dashboard/" + _DASHBOARD_VERSION 
		    		+ "/src/deploy/kubernetes-dashboard.yaml" );
		    if( status == ExecutionStatus.Done ){
			    // test UI
			    s3.getCmdErrLog().reset();
				s3.getCmdLog().reset();
				status = s3.command( "curl -L http://localhost:8080/ui" );
				String log = s3.getCmdLog().getText();
				logger.debug( "DASHBOARD ver " + _DASHBOARD_VERSION + " localhost CALL:\r\n" + log );
				if( _EXPORT_FILE ){
				    try {
					    FileOutputStream ou = new FileOutputStream( new File( workspace, "dashboard_" + _DASHBOARD_VERSION  + "_log.txt" ) );
					    ou.write( s3.getCmdLog().getText().getBytes() );
					    ou.close();
					} catch (Exception e) {
						logger.error( "general failure", e );
					}
				}
			} else {
				logger.error( "DASHBOARD INSTALL FAILED" );
				return false;
			}
			return true;
		} catch ( Exception e ){
			logger.error( "general failure", e );
			return false;
		}
	}

	// https://storage.googleapis.com/kubernetes-release/release/stable-1.4.txt
    // https://storage.googleapis.com/kubernetes-release/release/stable-1.5.txt
    // https://storage.googleapis.com/kubernetes-release/release/stable-1.6.txt
	// https://storage.googleapis.com/kubernetes-release/release/<VER/bin/windows/amd64/kubectl.exe
	// https://storage.googleapis.com/kubernetes-release/release/v1.5.7/bin/windows/amd64/kubectl.exe

	public boolean installKubecfg(){
		if( kubecfg_text == null ){
			logger.error( "cannot instal KUBECFG: missing text" );
			return false;
		}
		try {
			logger.debug( "GOING TO OVERRIDE CONFIG OF KUBELET" );
			File home = new File( System.getProperty("user.home") );
			FileOutputStream ou = new FileOutputStream( new File( home, ".kube/config" ) );
		    ou.write( kubecfg_text.getBytes() );
		    ou.close();
		    return true;
		} catch (Exception e) {
			logger.error( "cannot instal KUBECFG", e );
		    return false;
		}
	}

	protected String execution( String cmd, File dir ){
		String command = cmd; // "cmd /c " + cmd
		try {
			Runtime rt = Runtime.getRuntime();
            Process process = rt.exec( command, new String[0], dir );

            // Get input streams
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // Read command standard output
            String s;
            System.out.println("(CMD)Standard output: ");
			String msg = "";
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
				msg += s;
            }
            //
            System.out.println("(CMD)error output: ");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
			//	msg += s;
            }
			return msg;
		} catch ( IOException e ){
			logger.error( "execution failure: " + command, e );
			return null;
		}
	}

	protected String cfgSpec() throws IOException {
		String cfgSPEC = "# ## Configure 'ip' variable to bind kubernetes services on a\n"
				+ "# ## different ip than the default iface\n";
		for (int i = 0; i < privateIpList.length; i++) {
			int id = i+1;
			cfgSPEC += "node" + id + " ansible_ssh_host=" + publicIpList[i] + " ip=" + privateIpList[i] + "\n"
					;
		}
		cfgSPEC += "\n# ## configure a bastion host if your nodes are not directly reachable\n"
				+ "# bastion ansible_ssh_host=x.x.x.x\n"
				+ "\n[kube-master]\n"
				+ "" + "node1" + "\n"
				;
		cfgSPEC += "\n[etcd]\n";
		for( int i = 0; i < privateIpList.length; i++ ){
			int id = i+1;
			cfgSPEC += "" + "node" + id + "\n";
			if( i == 3 )
				break;
		}
		cfgSPEC += "\n[kube-node]\n";
		for( int i = 1; i < privateIpList.length; i++ ){
			int id = i+1;
			cfgSPEC += "node" + id + "\n";
		}
		cfgSPEC += "\n"
			+ "[k8s-cluster:children]\n"
			+ "kube-master\n"
			+ "kube-node"
			;
		if( _EXPORT_FILE ){
			FileOutputStream ou = new FileOutputStream( new File( workspace, "inventory.cfg" ) );
			ou.write( cfgSPEC.getBytes() );
			ou.close();
		}
		return cfgSPEC;
	}

	protected String k8cfg( String dir ) throws StopAction, IOException {
		SshServerManager s3 = mainS3;
		s3.getCmdErrLog().reset();
		s3.getCmdLog().reset();
		String cName = "javakube";
		String k8 = s3.sudoScpFrom( dir + "/my_inventory/group_vars/k8s-cluster.yml" );
		k8 = k8.replace( "kube_network_plugin: calico", "kube_network_plugin: flannel" );
		k8 = k8.replace( "cluster_name: cluster.local", "cluster_name: " + cName );
		k8 = k8.replace( "dns_mode: dnsmasq_kubedns", "dns_mode: kubedns" );
		// k8 = k8.replace( "", "" );
		// k8 = k8.replace( "", "" );
		// k8 = k8.replace( "", "" );
		// k8 = k8.replace( "", "" );
		return k8;
	}

	public boolean kargoRun( boolean retry ){
		try {
			SshServerManager s3 = mainS3;
			ExecutionStatus status;
			String nHost;
			String hostgroup = "[group_name]\n" ;
			for( int i = 0; i < privateIpList.length; i++ ){
				nHost = privateIpList[i];
				hostgroup += "node" + (i+1) + " ansible_ssh_host=" + nHost + "\n";
			}
			s3.getCmdErrLog().reset();
			s3.getCmdLog().reset();
			status = aservice.installAndsetupAnsible( s3, rootPwd, hostgroup, userName, userPwd );
			logger.debug( "ANSIBLE install ended: " + status );
			if( status != ExecutionStatus.Done )
				return false;
			status = aservice.installGit( s3 );
			logger.debug( "GIT install ended: " + status );
			if( status != ExecutionStatus.Done )
				return false;
			String dir = "kargo";
			if( !retry ){
				if( afl.fileOrDirExists( s3, dir ) == ExecutionStatus.Done_CHECK_FALSE ){
					s3.getCmdErrLog().reset();
					s3.getCmdLog().reset();
					status = s3.command( "git clone --branch " + _BRANCH_KARGO + " https://github.com/kubernetes-incubator/kargo.git" );
					if( status != ExecutionStatus.Done ){
						logger.error( "GIT FAILURE" );
						return false;
					}
					logger.debug( "GIT KARGO " + _BRANCH_KARGO );
					status = s3.command( "cd " + dir + " && cp -r inventory my_inventory" );
					if( status != ExecutionStatus.Done ){
						logger.error( "Inventory FAILURE" );
						return false;
					}
				}
				s3.getCmdErrLog().reset();
				s3.getCmdLog().reset();
			}
			// IMPORTANT: Edit my_inventory/groups_vars/*.yaml to override data vars
			String cfgSPEC = cfgSpec();
			status = afl.createCatFile( s3, dir + "/my_inventory/inventory.cfg", cfgSPEC );
			String k8 = k8cfg( dir );
			status = afl.createCatFile( s3, dir + "/my_inventory/group_vars/k8s-cluster.yml", k8 );
			//
			s3.getCmdErrLog().reset();
			s3.getCmdLog().reset();
			logger.debug( "KARGO RUN RUN RUN..." );
			status = s3.command( "cd " + dir 
					+ " && ansible-playbook -i my_inventory/inventory.cfg cluster.yml -b -v --private-key=~/.ssh/id_rsa" 
					+ " --extra-vars \"ansible_sudo_pass=" + userPwd + "\"" 
					+ (retry ? " --limit @/home/ansiloc/kargo/cluster.retry" : "")
					);
			logger.debug( "KARGO ended: " + status );
			return status == ExecutionStatus.Done;
		} catch ( Exception e ){
			logger.error( "general failure", e );
			return false;
		}
	}

	public boolean presetOs() throws StopAction {
		return false;
	}

	/**
	 * TODO: TO BE DEFINED
	 * 
	 */
	public void testStickySessionServiceLoadBalancerFromWindows(){
		if( true )
			return;
		File dir = new File( "." ).getAbsoluteFile();
		try {
			String ip = publicIpList[1];
			int a = 0;
			a++;
			// DNS set IP to lbtest.iperione.eu
			execution( "kubectl_ver\\kubectl15.exe create -f google_pod_host\\servicelb.yaml", dir );
			execution( "kubectl_ver\\kubectl15.exe expose -f google_pod_host\\servicelb.yaml --external-ip=" + ip, dir );
			
			
			
			// REM leggere il contenuto dei PODS... uno deve essere FAILED (!)
			// REM bisognfa poi fare il DESCRIBE POD per leggere il FAILED
			String error = execution( "kubectl_ver\\kubectl15.exe get pods -l google_pod_host\\app=service-loadbalancer", dir );
	//		NAME                         READY     STATUS    RESTARTS   AGE
	//		service-loadbalancer-32mh8   0/1       Pending   0          52s
			
			
			
			String tags = execution( "kubectl_ver\\kubectl15.exe get nodes", dir );
		//	NAME        STATUS                     AGE
		//	ansi-svr1   Ready,SchedulingDisabled   14m
		//	ansi-svr2   Ready                      14m			int ba = 0;
			int ba = 0;
			ba++;
			
			
			
			// REM marcare un nodo come role=loadbalancer
			// REM es: kubectl label node e2e-test-beeps-minion-c9up role=loadbalancer
			// REM per associare un NODO prima vedere quali ci sono: kubectl get nodes
			String mark = "abc";
			execution( "kubectl_ver\\kubectl15.exe label node " + mark + " role=loadbalancer", dir );
			
			execution( "kubectl_ver\\kubectl15.exe create -f google_pod_host\\haexample_hostnames.yaml", dir );
		} catch ( Exception e ){
			logger.error( "test kubectl failure", e );
		}
	}

}