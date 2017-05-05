package org.naarani.selenev.jclouds.digitalocean;

import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.digitalocean2.compute.options.DigitalOcean2TemplateOptions;
//import org.jclouds.digitalocean2.domain.Image;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;

import com.esotericsoftware.yamlbeans.YamlWriter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set; 
import org.jclouds.compute.ComputeService; 
import org.jclouds.compute.RunNodesException; 
import org.jclouds.sshj.config.SshjSshClientModule;
import org.naarani.core.exceptions.StopAction;
import org.naarani.selenev.cmd.ASelenevCmd;
import org.naarani.selenev.ssh.ExecutionStatus;
import org.naarani.selenev.ssh.SshServerManager;
import org.naarani.selenev.ssh.centos.UserLibCentos;
import org.naarani.selenev.yaml.IncludeVars;
import com.google.inject.Module;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class ProvisionDigitalOcean extends ASelenevCmd {
	
	static private Logger logger = Logger.getLogger( ProvisionDigitalOcean.class );

	static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex( Apis.viewableAs(ComputeServiceContext.class), Apis.idFunction() );
    static final Map<String, ProviderMetadata> appProviders = Maps.uniqueIndex( Providers.viewableAs(ComputeServiceContext.class), Providers.idFunction() );

	static final String POLL_PERIOD_TWENTY_SECONDS = String.valueOf(SECONDS.toMillis(20));
	static final Set<String> allKeys = ImmutableSet.copyOf(Iterables.concat( appProviders.keySet(), allApis.keySet()));
	
	protected File dropletPath;
	protected Provisioning provisioning;
	protected UserModel usermodel;
	
	@Deprecated
	public Set<? extends NodeMetadata> buildDroplet( DoToken token, String serverDistro, int size, String ROOT_PASSWORD
			, String serverTagName, File workdir, File prv ) throws RunNodesException, IOException {
		//
		Provisioning prov = new Provisioning();
		prov.servetTag = new String[] { serverTagName };
		prov.distro = serverDistro;
		prov.region = "fra1";
		prov.hardware = "2gb";
		//
		UserModel usermodel = new UserModel();
		usermodel.rootLikeUser = "root";
		if( prov.distro.toUpperCase().indexOf( "COREOS" ) != -1 ){
			usermodel.rootLikeUser = "core";
		}
		usermodel.rootLikePassword = ROOT_PASSWORD;
		usermodel.sshPrivateKey = "";
		//
		return buildDroplet( token, prov, usermodel, size, workdir, prv );
	}

	@Override
	public Set<? extends NodeMetadata> exec( String[] args, String prv, String wk ) throws Exception {
		OptionParser parser = super.basicParser();
        //
        parser.accepts( "size" ).withRequiredArg().describedAs( "Number of server to instance" ).defaultsTo( "1" ).ofType( Integer.class );
        parser.accepts( "distro" ).withRequiredArg().describedAs( "Distro as from cloud list" ).defaultsTo( "fra1/centos-7-x64" );
        parser.accepts( "region" ).withRequiredArg().describedAs( "Region as from cloud list" ).defaultsTo( "fra1" );
        parser.accepts( "hardware" ).withRequiredArg().describedAs( "Hardware as from cloud list" ).defaultsTo( "2g" );
        //
        parser.accepts( "serverName" ).withRequiredArg().describedAs( "Server name; we'll add a number to it, for each server" ).defaultsTo( "<random>" );
        parser.accepts( "servetTag" ).withRequiredArg().describedAs( "Label/tag to add to server" );
        //
        parser.accepts( "privateIp" ).withRequiredArg().describedAs( "Instance private IP" ).defaultsTo( "true" ).ofType( Boolean.class );
        //
		if( args.length == 0 ) {
			// zeroHelp( parser );
			throw new IOException( "wrong args [NONE]..." );
		} else {
	        OptionSet options = parser.parse( args );
	        // PRIVATE & WORKSPACE setting
	        if( options.has( "prv" ) ){
	        	prv = (String) options.valueOf( "prv" );
	        }
	        if( options.has( "wk" ) ) {
	        	wk = (String) options.valueOf( "wk" );
	        }
			// PROVISIONING
	        provisioning = new Provisioning();
	        provisioning.distro = (String) options.valueOf( "distro" );
	        provisioning.hardware = (String) options.valueOf( "hardware" );
	        provisioning.region = (String) options.valueOf( "region" );
	        //
	        provisioning.privatNetworking = Boolean.parseBoolean( (String) options.valueOf( "privateIp" ) );
	        if( options.has( "servetTag" ) ) {
	        	provisioning.servetTag = new String[1];;
	        	provisioning.servetTag[0] = (String)options.valueOf( "servetTag" );
			} else {
				provisioning.servetTag = new String[0];
			}
	        int size = (Integer) options.valueOf( "size" );
	        provisioning.serverName = new String[size];
	        String ref;
	        if( options.has( "serverName" ) ) {
	        	ref = (String)options.valueOf( "serverName" );
			} else {
	        	ref = "SELV-"+ getSaltString( 10 ).toUpperCase() + "-";
			}
	        for( int i = 0; i < size; i++ ){
	        	provisioning.serverName[i] = ref + i;
			}
			// DO API TOKEN MANAGEMENT
			DoToken token = new DoToken();
			IncludeVars v1 = new IncludeVars();
			v1.addFile( new File( prv, "digitalocean.yaml" ) );
			HashMap h1 = v1.getVars();
			token.name = (String) h1.get( "digitaloceanName" );
			token.token = (String) h1.get( "digitaloceanToken" );
			if( token.name == null || token.token == null ){
				throw new StopAction( "Missing VAR: digitalocean*" );
			}
			// USER MODEL
			UserModel user = new UserModel();
			IncludeVars v2 = new IncludeVars();
			File fileYaml = new File( prv, "usermodel.yaml" );
			if( !fileYaml.getParentFile().exists() ){
				fileYaml.getParentFile().mkdirs();
			}
			HashMap h2 = null;
			try {
				v2.addFile( fileYaml );
				h2 = v2.getVars();
			} catch (Exception e) {
				h2 = new HashMap();
			}
			user.rootLikeUser = (String) h2.get( "rootLikeUser" );
			if( user.rootLikeUser == null ) {
				user.rootLikeUser = "root";
				// SAVE YAML
				updateYaml( fileYaml, h2 );
			}
			if( user.rootLikeUser.trim().length() == 0 ) {
				user.rootLikeUser = "root";
				// SAVE YAML
				updateYaml( fileYaml, h2 );
			}
			user.rootLikePassword = (String) h2.get( "rootLikePassword" );
			if( user.rootLikePassword == null ){
				user.rootLikePassword = "";
			}
			if( user.rootLikePassword.trim().length() == 0 ) {
				h2.put( "rootLikePassword", getSaltString( 12 ) );
				user.rootLikePassword = (String) h2.get( "rootLikePassword" );
				// SAVE YAML
				updateYaml( fileYaml, h2 );
			}
			String tmpText = (String) h2.get( "sshPrivateKey" );
			if( tmpText == null ){
				tmpText = "";
			}
			if( tmpText.trim().length() != 0 ){
				user.sshPrivateKey = new String( Base64.getDecoder().decode( tmpText ) );
			}
			if( tmpText.trim().length() == 0 ){
				File tmpFile = new File( prv, "users" );
				tmpFile.mkdirs();
				//
				if( new File( tmpFile, "key_" + user.rootLikeUser + ".pub" ).exists() 
						&& new File( tmpFile, "key_" + user.rootLikeUser ).exists() ) {
					//
				} else {
					SshServerManager tmp = new SshServerManager( "a@b", "b" );
					UserLibCentos ulc = new UserLibCentos();
					ExecutionStatus status = ulc.generateSshKey( tmpFile.getAbsolutePath() + "/key_" + user.rootLikeUser, tmp, null );
					if( status != ExecutionStatus.Done ) {
						throw new StopAction( "problems during SSH KEY creation..." );
					}
				}
			    InputStream in = new FileInputStream( new File( tmpFile, "key_" + user.rootLikeUser ) );
			    int sizeBB = 0;
			    byte[] buffer = new byte[2048];
			    String key = "";
			    while( ( sizeBB = in.read( buffer,  0, 2048 ) ) != -1 ){
			    	key += new String( buffer, 0, sizeBB );
			    }
			    in.close();
			    h2.put( "sshPrivateKey", Base64.getEncoder().encodeToString( key.getBytes() ) );
			    user.sshPrivateKey = new String( Base64.getDecoder().decode( (String) h2.get( "sshPrivateKey" ) ) );
				// SAVE YAML
				updateYaml( fileYaml, h2 );
			}
			if( user.rootLikeUser == null || user.rootLikePassword == null ){
				throw new StopAction( "NULL VAR: UserModel" );
			}
			if( user.rootLikeUser.trim().length() == 0 || user.rootLikePassword.trim().length() == 0 ){
				throw new StopAction( "EMPTY VAR: UserModel" );
			}
			// RUN RUN RUN...
			return buildDroplet( token, provisioning, user, size, new File( wk ), new File( prv ) );
		}
	}

	private void updateYaml( File fileYaml, HashMap h2 ) throws IOException {
		YamlWriter writer = new YamlWriter( new FileWriter( fileYaml ) );
		writer.write( h2 );
		writer.close();
	}

	public Set<? extends NodeMetadata> buildDroplet( DoToken token, Provisioning provisioning, UserModel user, int size
			, File workdir, File prv ) throws RunNodesException, IOException {
		this.provisioning = provisioning;
		this.usermodel = user;
		//
		Properties overrides = new Properties();
		overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS );
		overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS );
		//
		dropletPath = new File( workdir, "hostdata" );
		dropletPath.mkdirs();
		//
		logger.debug( "requested nodes: " + size );
		//
		ImmutableSet<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule(), new Log4JLoggingModule());
		ComputeServiceContext context = ContextBuilder.newBuilder("digitalocean2").credentials( token.name, token.token )
               .modules(modules).overrides(overrides).buildView(ComputeServiceContext.class);
        ComputeService computeService = context.getComputeService();
        //
		ImmutableSet<String> nodeNames = ImmutableSet.copyOf( provisioning.serverName );
		ImmutableSet<String> tags = ImmutableSet.copyOf( provisioning.servetTag );
		//
 	   	TemplateOptions opts = computeService.templateOptions();
        Template template = computeService.templateBuilder()
        		.locationId( provisioning.region ).imageId( provisioning.distro ).hardwareId( provisioning.hardware )
                .options( opts.as( DigitalOcean2TemplateOptions.class ).privateNetworking( provisioning.privatNetworking )
                		.nodeNames( nodeNames ).tags( tags ).overrideLoginUser( user.rootLikeUser )
                		.overrideLoginPassword( user.rootLikePassword )
                		.overrideLoginPrivateKey( user.sshPrivateKey )
                ).build();
		//
        String NAME = "cluster";
        Set<? extends NodeMetadata> nodi = computeService.createNodesInGroup( NAME, size, template );
        Iterator<? extends NodeMetadata> noit = nodi.iterator();
		logger.debug( "nodes available: " + size );
        //
        if( workdir != null ){
	        dropletPath.mkdirs();	
	        while( noit.hasNext() ){
	        	NodeMetadata ref = noit.next();
	        	// CREATE YAML File...
	        	HashMap table = new HashMap();
	        	table.put( "id", ref.getId() );
	        	table.put( "hostname", ref.getHostname() );
	        	table.put( "ip.public", ref.getPublicAddresses().toArray()[0].toString() );
	        	if( ref.getPrivateAddresses() != null )
		        	if( ref.getPrivateAddresses().size() > 0 )
		        		table.put( "ip.private", ref.getPrivateAddresses().toArray()[0].toString() );
	        	table.put( "hardware", ref.getHardware().toString() );
	        	table.put( "type", ref.getType().toString() );
	        	table.put( "os", ref.getOperatingSystem().getName() );
	        	table.put( "osfamily", ref.getOperatingSystem().getFamily() );
	        	table.put( "location", ref.getLocation().getDescription() );
	        	table.put( "tags", ref.getTags().toString() );
	        	table.put( "imageId", ref.getImageId() );
	        	File file = new File( dropletPath, "svr_" + ref.getHostname() + "_" + ref.getPublicAddresses().toArray()[0].toString() + ".yaml" );
	        	updateYaml( file, table );
	        	/*
	        	ou.write( ( ref.getHostname() + "   " + ref.getPublicAddresses().toArray()[0].toString() + "\r\n" ).getBytes() );
	        	ou.write( ( ref.getHostname() + "   " + ref.getPrivateAddresses().toArray()[0].toString() + "\r\n" ).getBytes() );
	        	String line = ref.getCredentials().getUser() + "@" + ref.getPublicAddresses().toArray()[0] + "\r\n" 
	        			+ ref.getPrivateAddresses().toArray()[0] + "\r\n" 
	        			+ ref.getCredentials().getOptionalPassword() + "\r\n" + ref.getCredentials().getOptionalPrivateKey().get();
	            FileOutputStream ou2 = new FileOutputStream( new File( dropletPath, ref.getPublicAddresses().toArray()[0] + ".txt" ) );
	        	ou2.write( line.getBytes() );
	            ou2.close();
	            */
			}
		}
        setupSshkey( nodi, user, prv );
        try {
	    	Closeables.close( computeService.getContext(), true );
		} catch ( IOException e ){
			logger.error( "error closing COMPUTESERVICE", e );
		}
	    return nodi;
	}

	private void setupSshkey(Set<? extends NodeMetadata> nodi, UserModel user, File prv ){
		try {
			while( true ){
				logger.info( "waiting for server..." );
				boolean done = true;
				Iterator it = nodi.iterator();
				while( it.hasNext() ){
					NodeMetadata nodo = (NodeMetadata) it.next();
					if( nodo.getStatus() != NodeMetadata.Status.RUNNING ){
						done = false;
					} else {
						/*
						try {
							IUserLib ulc = new UserLibCentos();
							if( nodo.getOperatingSystem().getFamily() == OsFamily.COREOS )
								ulc = new UserLibCoreos();
							SshServerManager svr = new SshServerManager( user.rootLikeUser + "@" + nodo.getPublicAddresses().toArray()[0].toString()
									, user.rootLikePassword );
							ExecutionStatus status = svr.connect();
							if( status != ExecutionStatus.Done )
								done = false;
							File tmpFile = new File( prv, "users" );
							File fileRef2 = new File( tmpFile, "key_" + user.rootLikeUser + ".pub" ) ;
							status = ulc.setUserSshKey( svr, false, "root", fileRef2 );
							if( status != ExecutionStatus.Done )
								done = false;
							svr.close();
						} catch ( Exception e ){
							logger.error( "loop error setting up KEY - COMPUTESERVICE", e );
							done = false;
						}
						*/
					}
				}
				if( done )
					break;
			}
		} catch ( Exception e ){
			logger.error( "error SETUP KEY - COMPUTESERVICE", e );
		}
	}

/*	
	public String getDefaultUser(){
		return defaultUser;
	}
*/
	protected String getSaltString( int len ){
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < len) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }
}