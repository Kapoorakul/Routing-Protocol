import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class DNS {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		int dport = 0;
		String IP = null;
		PrintWriter writer = null;
		dnsDatabase NS = new dnsDatabase();
		
		//ConcurrentHashMap<String,ArrayList<String>> NameResolver = new ConcurrentHashMap<String,ArrayList<String>>();
		ArrayList<String> init = new ArrayList<String>();
		for (int i = 0; i < init.size(); i++) {
			ArrayList<String> l = NS.NameResolver.get(init.get(i));
			if (l == null) {
				NS.NameResolver.put(init.get(i), new ArrayList<String>());
			}
		}

        try {
			IP = Inet4Address.getLocalHost().getHostAddress();
			System.out.println("DNS IP = "+IP);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
			ServerSocket DNS_socket = new ServerSocket(0);
			dport = DNS_socket.getLocalPort();
			System.out.println("DNS port = "+ dport); 
       
			DNSrec dns_port = new DNSrec(DNS_socket, dport, NS);
			new Thread(dns_port).start();
	
		} catch (IOException ex) {
			System.err.println("no available ports");
		}  	
		

		try {
			writer = new PrintWriter("/afs/cs.pitt.edu/usr0/ank161/public/DNS.txt");
			writer.println(dport);
			writer.println(IP);
			writer.close();
	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while(true){
		System.out.println("\nPress 1 to see the DNS database.");
		Scanner in = new Scanner(System.in);
		String choice = in.next();
		if (choice.equals("1")) {
			System.out.println(NS.NameResolver);
		}
	  }
	}
}

class dnsDatabase{
	volatile ConcurrentHashMap<String, ArrayList<String>> NameResolver = new ConcurrentHashMap<String,ArrayList<String>>();
}

class DNSrec implements Runnable {
	
	ServerSocket sock;
	int port;
	//ConcurrentHashMap<String,ArrayList<String>> NameResolver;
	dnsDatabase NS;
	
	DNSrec(ServerSocket sock, int port, dnsDatabase NS) {
		this.sock = sock;
		this.port = port;
		this.NS = NS;
	}

	
	@Override
	public void run(){
		
		
		try
		{
			System.out.println("\nListening for DNS register and DNS query connections on Port: " + port);
			while (true) {
				//System.out.println("Waiting for connection from client...");
				try{
					Socket socket = sock.accept();
					System.out.println("\nGot a connection from a client..");
					
					DataInputStream in = new DataInputStream(socket.getInputStream());
					String action = in.readUTF();
					System.out.println("\nPacket type is "+action);
					
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					//out.writeUTF("Ack");
					
					if(action.equals("Register")){
					ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
					Packet packet = (Packet) ois.readObject();
					
					ArrayList<String> putinfo = new ArrayList<String>();
					putinfo.add(packet.IP);
					putinfo.add(packet.cport_s);
					putinfo.add(packet.fport_s);
					NS.NameResolver.put(packet.selfID, putinfo);
					System.out.println("Info of "+packet.selfID+ " stored in Name Resolver DB.");
					System.out.println("Sending acknowledgement to "+packet.selfID+".");
					
					out.writeUTF("Ack");
					out.close();
					
					}
					else{
						String selfID = in.readUTF();
						String NeighborID = in.readUTF();
						System.out.println("Information of "+NeighborID+" queried.");
						ArrayList<String> getinfo = new ArrayList<String>();
						getinfo = NS.NameResolver.get(NeighborID);
						if (selfID.equals(NeighborID)){
							if (getinfo != null) {
								out.writeUTF(getinfo.get(0));
								out.writeUTF(getinfo.get(1));
								out.writeUTF(getinfo.get(2));
								System.out.println("Information of "+NeighborID+" sent.");
							}
							else
							{
								out.writeUTF("null");
								out.writeUTF("null");
								out.writeUTF("null");
								System.out.println("Entry of "+NeighborID+" does not exist.");
							}

						}else{
							if (getinfo != null) {
								out.writeUTF(getinfo.get(0));
								out.writeUTF(getinfo.get(1));
								out.writeUTF(getinfo.get(2));
								System.out.println("Information of "+NeighborID+" sent.");
							}
							else
							{
								out.writeUTF("null");
								out.writeUTF("null");
								out.writeUTF("null");
								System.out.println("Entry of "+NeighborID+" does not exist.");
							}

						}
					}
					
				}catch (IOException e) { 
						e.printStackTrace();
			            // Read/write failed --> connection is broken 
			    }catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				catch (NullPointerException e) {
		         	System.out.print("Looks like the router whose information is requested is not registered yet. Try again after sometime.");
		        }
				}
		}finally {
				}

		}
	}


class Packet implements Serializable {
	String selfID;
	String IP;
	String cport_s;
	String fport_s;
	String neighborID;
	String ack;
	
	
	public Packet(String selfID, String IP, String cport_s, String fport_s){
		this.selfID = selfID;
		this.IP = IP;
		this.cport_s = cport_s;
		this.fport_s = fport_s;
	}
	
	public Packet(String selfID, String neighborID){
		this.selfID = selfID;
		this.neighborID = neighborID;
	}
	
	public Packet(String ack){
		this.ack = ack;
	}
}

