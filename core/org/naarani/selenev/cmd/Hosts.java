package org.naarani.selenev.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.naarani.selenev.Engine;
import org.naarani.selenev.Host;
import org.naarani.selenev.HostDataFilter;
import org.naarani.selenev.ssh.SshServerManager;

import it.andreascarpino.ansible.inventory.type.AnsibleGroup;
import it.andreascarpino.ansible.inventory.type.AnsibleHost;
import it.andreascarpino.ansible.inventory.type.AnsibleInventory;
import it.andreascarpino.ansible.inventory.type.AnsibleVariable;
import it.andreascarpino.ansible.inventory.util.AnsibleInventoryReader;
import it.andreascarpino.ansible.inventory.util.AnsibleInventoryWriter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Hosts extends ASelenevCmd {
	
	static private Logger logger = Logger.getLogger( Hosts.class );

	@Override
	public Object exec( String[] args, String prv, String wk ) throws Exception {
		OptionParser parser = super.basicParser();
        parser.accepts( "list", "show server list" );
        parser.accepts( "build", "build/update file with hostdata information" );
        parser.acceptsAll( java.util.Arrays.asList( "p", "password" ) ).withRequiredArg().describedAs( "password to login/sudo" );
        parser.acceptsAll( java.util.Arrays.asList( "s", "sshUnlockPassword" ) ).withRequiredArg()
        		.describedAs( "password of ssh private key" );
        parser.nonOptions("filter").ofType(String.class).describedAs( "server filter: all / os_filter / group filter" );
        //
		if( args.length == 0 ){
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
	        //
	        File hosts = new File( new File( prv ), "hosts" );
	        if( options.has( "build" ) ){
	        	File[] f = new File( wk, "hostdata" ).listFiles( new HostDataFilter() );
	        	if( f == null ){
	        		// no fix needed
	        	} else {
			        AnsibleInventory inv = new AnsibleInventory();
	        		if( hosts.exists() ){
				        inv = getHostsInventory( hosts );
	        		} else {
	        			if( f.length == 0 ){
	        				return null;
	        			}
	        		}
	        		for( int i = 0; i < f.length; i++ ){
	        			File f1 = f[i];
	        			Map map = (Map)Engine.loadYaml( f1 );
	        			String hostname = (String)map.get( "hostname" );
	        			AnsibleHost h3 = new AnsibleHost( hostname );
	        			h3.addVariable( new AnsibleVariable( "ansible_host", (String)map.get( "ip.public" ) ) );
	        			h3.addVariable( new AnsibleVariable( "ansible_user", "root" ) );
	        			// h3.addVariable( new AnsibleVariable( "ansible_port", "22" ) );
		        		inv.addHost( h3 );
	        		}
	        		saveHostsInventory( hosts, inv );
			        return null;
	        	}
	        } else {
		        AnsibleInventory inv = getHostsInventory( hosts );
		        //
		        boolean list = options.has( "list" );
		        if( list ){
		        	System.out.println( "list all hosts: " );
		        	Collection<AnsibleGroup> l1 = inv.getGroups();
		        	Collection<AnsibleHost> l3 = inv.getHosts();
			        List<Host> sum = hostList( l1, l3 );
		        	for (int i = 0; i < sum.size(); i++) {
		        		System.out.println( sum.get( i ) );
					}
		        } else {
			        String filter = "";
			        if( options.nonOptionArguments().size() == 1 ){
				        filter = (String) options.nonOptionArguments().get( 0 );
				        //
				        List<Host> sum;
				        if( filter.compareTo( "all" ) == 0 ){
				        	Collection<AnsibleGroup> l1 = inv.getGroups();
				        	Collection<AnsibleHost> l3 = inv.getHosts();
				        	sum = hostList( l1, l3 );
				        } else {
				        	AnsibleGroup l1 = inv.getGroup( filter );
				        	AnsibleHost l2 = inv.getHost( filter );
				        	sum = hostList( l1, l2 );
				        }
				        // ---------------------------------------------------------------
			        	String pwd = (String) options.valueOf( "password" );
			        	String unlock = (String) options.valueOf( "sshUnlockPassword" );
				        // ---------------------------------------------------------------
				        List<SshServerManager> ssh = new ArrayList<SshServerManager>(); 
				        for( int i = 0; i < sum.size(); i++ ){
				        	Host h1 = sum.get( i );
				        	SshServerManager ssh1;
							String privateKey = getKey( prv, h1.sshUser );
							if( privateKey == null ){
				        		ssh1 = new SshServerManager( h1.sshUser + "@" + h1.ip, pwd );
							} else {
				        		ssh1 = new SshServerManager( h1.sshUser + "@" + h1.ip, privateKey, unlock == null ? null : unlock.getBytes(), pwd );
							}
							ssh.add( ssh1 );
						}
				        return ssh;
			        } else {
						parser.printHelpOn( System.err );
						throw new IOException( "wrong args [no filter]..." );
			        }
		        }
	        }
	        return null;
		}
	}

	private AnsibleInventory getHostsInventory( File hosts ) throws IOException {
    	String body = "";
        FileInputStream in = new FileInputStream( hosts );
        int size = 0;
        byte[] buffer = new byte[2048];
        while( ( size = in.read( buffer, 0, buffer.length ) ) != -1 ) {
        	body += new String( buffer, 0, size );
        }
        in.close();
        body = body.replace( "selenev", "ansible" );
        AnsibleInventory inv = AnsibleInventoryReader.read( body );
        return inv;
	}

	private void saveHostsInventory( File hosts, AnsibleInventory inv ) throws IOException {
        String body = AnsibleInventoryWriter.write( inv );
    	body = body.replace( "ansible", "selenev" );
        //
    	FileOutputStream ou = new FileOutputStream( hosts );
    	ou.write( body.getBytes() );
        ou.close();
	}

	private String getKey( String prv, String user ) throws IOException {
		File f = new File( new File( prv ), "users/key_" + user );
		if( f.exists() ) {
			return f.getCanonicalPath();
		} else {
			return null;
		}
	}

	private List<Host> hostList( AnsibleGroup group, AnsibleHost singleHost1 ){
		List<Host> list = new ArrayList<Host>();
		if( singleHost1 != null ){
        	Host host = hostfromansible( singleHost1 );
        	list.add( host );
		}
        for( Iterator<AnsibleHost> it2 = group.getHosts().iterator(); it2.hasNext(); ){
        	AnsibleHost singleHost = it2.next();
        	if( singleHost.getName().compareTo( "---" ) == 0 )
        		break;
        	Host host = hostfromansible( singleHost );
        	list.add( host );
		}
		return list;
	}

	private List<Host> hostList( Collection<AnsibleGroup> group3, Collection<AnsibleHost> group2 ){
		List<Host> list = new ArrayList<Host>();
        for( Iterator<AnsibleGroup> it3 = group3.iterator(); it3.hasNext(); ){
        	AnsibleGroup group = it3.next();
	        for( Iterator<AnsibleHost> it2 = group.getHosts().iterator(); it2.hasNext(); ){
	        	AnsibleHost singleHost = it2.next();
	        	if( singleHost.getName().compareTo( "---" ) == 0 )
	        		break;
	        	Host host = hostfromansible( singleHost );
	        	list.add( host );
			}
		}
        for( Iterator<AnsibleHost>iterator = group2.iterator(); iterator.hasNext(); ){
        	AnsibleHost singleHost = iterator.next();
        	if( singleHost.getName().compareTo( "---" ) == 0 )
        		break;
        	Host host = hostfromansible( singleHost );
        	list.add( host );
		}
		return list;
	}

	private Host hostfromansible( AnsibleHost singleHost ){
		Host host = new Host();
    	host.name = singleHost.getName();
    	host.ip = (String)singleHost.getVariable( "ansible_host" ).getValue();
    	AnsibleVariable ref = singleHost.getVariable( "ansible_port" );
    	if( ref != null ){
        	if( ref.getValue() != null ){
        		host.sshport = Integer.parseInt( (String)ref.getValue() );
			}
		}
    	//
    	ref = singleHost.getVariable( "ansible_user" );    	
    	if( ref != null ){
        	if( ref.getValue() != null ){
            	host.sshUser = (String)ref.getValue();
        	}
    	}
    	if( host.sshUser == null ){
	    	ref = singleHost.getVariable( "ansible_ssh_user" );    	
	    	if( ref != null ){
	        	if( ref.getValue() != null ){
	            	host.sshUser = (String)ref.getValue();
	        	}
	    	}
    	}
    	//
    	ref = singleHost.getVariable( "ansible_ssh_pass" );    	
    	if( ref != null ){
        	if( ref.getValue() != null ){
            	host.pwd = (String)ref.getValue();
			}
		}
		return host;
	}

}