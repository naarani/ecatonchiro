package org.naarani.selenev;

public class Host {

	public String name;
	
	public String ip;
	
	public int sshport = 22;
	
	public String sshUser;

	public String toString(){
		return "\"" + name + "\" " + sshUser + "@" + ip + ":" + sshport;
	}
	
}