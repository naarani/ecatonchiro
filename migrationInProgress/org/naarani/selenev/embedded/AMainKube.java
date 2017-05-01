package org.naarani.selenev.embedded;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.naarani.selenev.ssh.SshServerManager;
import org.naarani.selenev.jclouds.digitalocean.CreateDroplet;
import org.naarani.selenev.ssh.ExecutionStatus;

public class AMainKube {

	static protected Logger logger = Logger.getLogger( AMainKube.class );

	static protected String SERVER_LABEL = null;
	static protected String SERVER_DISTRO = null;
	static protected AEngineKubeCreator engine = null;
	
	static public String workdir = "workspace/basic";
	
	static protected String ansibleName = "workeruser";
	static protected String ansibleKey = "private/key" + ansibleName;

	static protected String rootPwd;

	static public void main( int size ) throws RunNodesException, IOException {
		CreateDroplet drop = new CreateDroplet();
		Set<? extends NodeMetadata> nodi = drop.buildDroplet( SERVER_DISTRO, size, rootPwd, SERVER_LABEL, new File( workdir ) );

		String stringkeyRoot = nodi.iterator().next().getCredentials().getOptionalPrivateKey().get();

		FileOutputStream ou = new FileOutputStream( new File( workdir, "key" ) );
		ou.write( stringkeyRoot.getBytes() );
		stringkeyRoot = new File( workdir, "key" ).getAbsolutePath();
		ou.close();
		//
		List<NodeMetadata> elenco = new ArrayList<NodeMetadata>();
		for( Iterator iterator = nodi.iterator(); iterator.hasNext();){
			NodeMetadata nodeMetadata = (NodeMetadata) iterator.next();
			elenco.add( nodeMetadata );
		}
		Comparator<NodeMetadata> c = new Comparator<NodeMetadata>(){

			@Override
			public int compare(NodeMetadata o1, NodeMetadata o2) {
				return o1.getHostname().compareToIgnoreCase( o2.getHostname() );
			}

		};
		Collections.sort( elenco, c );
		//
		String[] list = new String[size]; 
		String[] listPublic = new String[size];
		for( int j = 0; j < elenco.size(); j++ ){
			NodeMetadata nodeMetadata = elenco.get( j );
			list[j] = nodeMetadata.getPrivateAddresses().toArray()[0].toString();
			listPublic[j] = nodeMetadata.getPublicAddresses().toArray()[0].toString();
		}
		String primo = listPublic[0];
		//
		File fileAnsi = new File( ansibleKey );
		String rootUser = drop.getDefaultUser();
		if( !fileAnsi.exists() ){
			SshServerManager s0 = new SshServerManager( rootUser + "@" + primo, stringkeyRoot, null, rootPwd ); 
			ExecutionStatus status = engine.getUserLib().generateSshKey( ansibleKey, s0, null );
			if( status != ExecutionStatus.Done ){
				logger.error( "can't log in...");
				return;
			}
		}
		//
		// pausa
		try {
			Thread.sleep( 10 * 1000 );
		} catch ( InterruptedException e ){
			e.printStackTrace();
		}
		AMainKube kubeActions = new AMainKube();
		kubeActions.run( new File( workdir ), list, listPublic, primo, ansibleKey, stringkeyRoot, rootUser, rootPwd );
	}

	protected void run( File workdir, String[] list, String[] listPublic, String primo, String ansibleKey, String stringkeyRoot, String rootUser, String rootPwd ){
		engine.prepare( workdir, list, listPublic, primo, ansibleKey, stringkeyRoot, rootUser, rootPwd );
		if( engine.preflight() ){
			if( engine.run() )
				testDISTRUTTIVOconfig( engine );
		} else if( engine.preflight() ){
			try {
				Thread.sleep( 10 * 1000 );
			} catch ( InterruptedException e ){
				e.printStackTrace();
			}
			if( engine.run() )
				testDISTRUTTIVOconfig( engine );
		}
	}

	protected void testDISTRUTTIVOconfig( AEngineKubeCreator kube ){
		if( kube.installKubecfg() ) 
			kube.testStickySessionServiceLoadBalancerFromWindows();
	}

}