package org.naarani.selenev.jclouds.digitalocean;

import java.util.Properties;

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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set; 
import org.jclouds.compute.ComputeService; 
import org.jclouds.compute.RunNodesException; 
import org.jclouds.sshj.config.SshjSshClientModule; 
import com.google.inject.Module;

/* TODO
 * should use YAML input and YAML output! 
 * 
 * 
 */
public class CreateDroplet {
	
	static private Logger logger = Logger.getLogger( CreateDroplet.class );

	static final Map<String, ApiMetadata> allApis = Maps.uniqueIndex( Apis.viewableAs(ComputeServiceContext.class), Apis.idFunction());

    static final Map<String, ProviderMetadata> appProviders = Maps.uniqueIndex(
            Providers.viewableAs(ComputeServiceContext.class), Providers.idFunction());

	static final String POLL_PERIOD_TWENTY_SECONDS = String.valueOf(SECONDS.toMillis(20));
	static final Set<String> allKeys = ImmutableSet.copyOf(Iterables.concat( appProviders.keySet(), allApis.keySet()));

	protected String defaultUser;
	
	protected File dropletPath;
	
	public Set<? extends NodeMetadata> buildDroplet( String serverDistro, int size, String ROOT_PASSWORD
			, String serverTagName, File workdir ) throws RunNodesException, IOException {
		Properties overrides = new Properties();
		overrides.setProperty(ComputeServiceProperties.POLL_INITIAL_PERIOD, POLL_PERIOD_TWENTY_SECONDS );
		overrides.setProperty(ComputeServiceProperties.POLL_MAX_PERIOD, POLL_PERIOD_TWENTY_SECONDS );
		
		dropletPath = new File( workdir, "droplet" );
		
		defaultUser = "root";
		if( serverDistro.toUpperCase().indexOf( "COREOS" ) != -1 ){
			defaultUser = "core";
		}
		
		Properties p = new Properties();
		try {
			FileInputStream in = new FileInputStream(  new File( "PRIVATE_properties.txt" ) );
			p.load( in );
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug( "requested nodes: " + size );

		ImmutableSet<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule(), new Log4JLoggingModule());
		ComputeServiceContext context = ContextBuilder.newBuilder("digitalocean2")
               .credentials( p.getProperty( "name" ), p.getProperty( "key" ) )
               .modules(modules)
               .overrides(overrides)
               .buildView(ComputeServiceContext.class);
        ComputeService computeService = context.getComputeService();
        
		ImmutableSet<String> nodeNames = ImmutableSet.<String> of( serverTagName + "1", serverTagName + "2", serverTagName + "3"
				, serverTagName + "4", serverTagName + "5" );
		ImmutableSet<String> tags = ImmutableSet.<String> of("javakube");
		
 	   	TemplateOptions opts = computeService.templateOptions();
        Template template = computeService.templateBuilder()
                .locationId("fra1")
                .imageId( serverDistro )
                .hardwareId( "2gb" )
                .options( opts.as( DigitalOcean2TemplateOptions.class).privateNetworking(true)
                		.nodeNames(nodeNames)
                		.tags(tags)
                		.overrideLoginUser( defaultUser )
                		.overrideLoginPassword( ROOT_PASSWORD )
                ).build();
		logger.debug( "nodes available: " + size );

        String NAME = "cluster";
        Set<? extends NodeMetadata> nodi = computeService.createNodesInGroup( NAME, size, template );
        Iterator<? extends NodeMetadata> noit = nodi.iterator();
        
        if( workdir != null ){
	        dropletPath.mkdirs();	
	        FileOutputStream ou = new FileOutputStream( new File( dropletPath, "ip.txt" ) );
	        while( noit.hasNext() ){
	        	NodeMetadata ref = noit.next();
	        	ou.write( ( ref.getHostname() + "   " + ref.getPublicAddresses().toArray()[0].toString() + "\r\n" ).getBytes() );
	        	ou.write( ( ref.getHostname() + "   " + ref.getPrivateAddresses().toArray()[0].toString() + "\r\n" ).getBytes() );
	        	String line = ref.getCredentials().getUser() + "@" + ref.getPublicAddresses().toArray()[0] + "\r\n" 
	        			+ ref.getPrivateAddresses().toArray()[0] + "\r\n" 
	        			+ ref.getCredentials().getOptionalPassword() + "\r\n" + ref.getCredentials().getOptionalPrivateKey().get();
	            FileOutputStream ou2 = new FileOutputStream( new File( dropletPath, ref.getPublicAddresses().toArray()[0] + ".txt" ) );
	        	ou2.write( line.getBytes() );
	            ou2.close();
			}
	        ou.close();
		}
        try {
	    	Closeables.close(computeService.getContext(), true);
		} catch (IOException e) {
			logger.error( "error closing COMPUTESERVICE", e );
		}
	    return nodi;
	}
	
	public String getDefaultUser(){
		return defaultUser;
	}

}