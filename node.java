import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.BindException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class node2 {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		System.out.println("Enter the Configuration File Location");
        System.out.println("Help: Enter the location as /afs/cs.pitt.edu/usr0/ank161/private/WAN/configR1.txt. Please make sure to use the appropriate config.txt file for each router.");
		Scanner in = new Scanner(System.in);
		String choice = in.next();

		String IP = null;
		int cport = 0;
		int fport = 0;
		String cport_s = null;
		String fport_s = null;
		ServerSocket ctrl_socket = new ServerSocket(0);
		ServerSocket ftp_socket = new ServerSocket(0);

		String[] tokenize = null;
		int pVersion = 1;
		int pvFlag = 0;
		String selfID = null;
		int sFlag = 0;
		int updateInterval = 0;
		int uFlag = 0;
		int aliveInterval = 0;
		int aFlag = 0;
		int number_of_nodes = 0;
		int nodesFlag = 0;
		int number_of_Neighbor = 0;
		int neighborFlag = 0;
		ArrayList<String> neighborList = new ArrayList<String>();
		int neighborlistFlag = 0;
		ArrayList<String> linkID = new ArrayList<String>();
		int linkidFlag = 0;
		//Reading the Config File
		try {
			BufferedReader br = new BufferedReader(new FileReader(choice));
			String fileRead = br.readLine();

			while (fileRead != null) {

				tokenize = fileRead.split("\n");
				for (String x : tokenize) {

					String[] y = x.split(":");

					for (String z : y) {
						if (pvFlag == 1) {
							pVersion = Integer.parseInt(z);
							pvFlag = 0;
						} else if (sFlag == 1) {
							selfID = z;
							sFlag = 0;
						} else if (uFlag == 1) {
							updateInterval = Integer.parseInt(z);
							uFlag = 0;
						} else if (aFlag == 1) {
							aliveInterval = Integer.parseInt(z);
							aFlag = 0;
						} else if (nodesFlag == 1) {
							number_of_nodes = Integer.parseInt(z);
							nodesFlag = 0;
						} else if (neighborFlag == 1) {
							number_of_Neighbor = Integer.parseInt(z);
							neighborFlag = 0;
						} else if (neighborlistFlag == 1) {
							neighborList.add(z);
							neighborlistFlag = 0;
						} else if (linkidFlag == 1) {
							linkID.add(z);
							linkidFlag = 0;
						}

						if (z.equals("Protocol version Number")) {
							pvFlag = 1;
						} else if (z.equals("Self RouterID")) {
							sFlag = 1;
						} else if (z.equals("Update Interval")) {
							uFlag = 1;
						} else if (z.equals("Alive Interval")) {
							aFlag = 1;
						} else if (z.equals("Number of Nodes in Topology")) {
							nodesFlag = 1;
						} else if (z.equals("Number of Neighbors")) {
							neighborFlag = 1;
						} else if (z.equals("Neighbor RouterID")) {
							neighborlistFlag = 1;
						} else if (z.equals("LinkID")) {
							linkidFlag = 1;
						}

					}

				}
				fileRead = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Config File not found");
			e.printStackTrace();
		}

		System.out.println("\nProtocol version: " + pVersion);
		System.out.println("Self ID: " + selfID);
		System.out.println("Update Interval: " + updateInterval);
		System.out.println("Alive Interval: " + aliveInterval);
		System.out.println("Number of Nodes in Topology: " + number_of_nodes);
		System.out.println("Number of Neighbors: " + number_of_Neighbor);
		System.out.println("Neighbor Routers: " + neighborList);
		System.out.println("Link ID's: " + linkID);

		Map<String, String> linkNeighbor = new HashMap<String, String>();
		for (int i = 0; i < neighborList.size(); i++) {
			linkNeighbor.put(neighborList.get(i), linkID.get(i));
		}

		System.out.println("\nReading the DNS.txt file and getting the DNS IP and port number..");
		ArrayList<String> sinfo = new ArrayList<String>();
		sinfo = DNSQuery(selfID, selfID);

		if (!sinfo.get(0).equals("null") && !sinfo.get(1).equals("null") && !sinfo.get(2).equals("null")) {
			IP = sinfo.get(0);
			cport_s = sinfo.get(1);
			cport = Integer.parseInt(cport_s);
			fport_s = sinfo.get(2);
			fport = Integer.parseInt(fport_s);
		} else {
			System.out.println("\nGenerating ports and getting the local IP address..");
			try {
				IP = Inet4Address.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			cport = ctrl_socket.getLocalPort();
			System.out.println("Control port = " + cport);
			cport_s = Integer.toString(cport);

			fport = ftp_socket.getLocalPort();
			System.out.println("FTP port = " + fport);
			fport_s = Integer.toString(fport);

			// Creating an Packet for DNS Registration
			Packet packet = new Packet(selfID, IP, cport_s, fport_s);

			// Registering with self IP address
			DNSRegister("Register", packet);
		}
		ArrayList<Integer> portList = new ArrayList<Integer>();
		Map<String, Integer> ftpPort = new HashMap<String, Integer>();
		ArrayList<String> ipaddr = new ArrayList<String>();
		System.out.println("\nPress 1 to start DNS queries.");
		in = new Scanner(System.in);
		choice = in.next();
		if (choice.equals("1")) {
			for (int i = 0; i < neighborList.size(); i++) {
				ArrayList<String> ninfo = DNSQuery(selfID, neighborList.get(i));
				ipaddr.add(ninfo.get(0));
				portList.add(Integer.parseInt(ninfo.get(1)));
				ftpPort.put(neighborList.get(i), Integer.parseInt(ninfo.get(2)));

			}
		}

		int selfint = 0;
		// Integer Equivalent of the Router ID for the Dijktras
		if (selfID.equals("R1")) {
			selfint = 0;
		} else if (selfID.equals("R2")) {
			selfint = 1;
		} else if (selfID.equals("R3")) {
			selfint = 2;
		} else if (selfID.equals("R4")) {
			selfint = 3;
		} else if (selfID.equals("R5")) {
			selfint = 4;
		} else if (selfID.equals("R6")) {
			selfint = 5;
		} else if (selfID.equals("R7")) {
			selfint = 6;
		} else if (selfID.equals("R8")) {
			selfint = 7;
		} else if (selfID.equals("R9")) {
			selfint = 8;
		} else if (selfID.equals("R10")) {
			selfint = 9;
		} else if (selfID.equals("R11")) {
			selfint = 10;
		} else if (selfID.equals("R12")) {
			selfint = 13;
		} else if (selfID.equals("R13")) {
			selfint = 12;
		} else if (selfID.equals("R14")) {
			selfint = 13;
		} else if (selfID.equals("R15")) {
			selfint = 14;
		}
		
		// Initializing Neighbor Database
		neigborDatabase2 nDB = new neigborDatabase2(neighborList, portList, ipaddr);

		nDB.display();
		
		// Initializing Queue to receive packets
		neighborQUEUE2 nQUEUE = new neighborQUEUE2(1000);
		lsaQ2 lsaQueue = new lsaQ2(1000);

		// Start Queue Thread
		queueThread2 qThread = new queueThread2(nQUEUE, nDB, lsaQueue, selfID, aliveInterval, updateInterval, pVersion);
		new Thread(qThread).start();

		// Start Listening on the Receive Ports
		receivePort2000 rP2000 = new receivePort2000(cport, nDB, nQUEUE, ctrl_socket);
		new Thread(rP2000).start();

		System.out.println("\nInitialization Complete.");
		
		// Acquiring Neighbors

		System.out.println("Choose among the following: ");
		System.out.println("Acquire neighbors and run LSRP ? (y/n)");

		choice = in.next();

		if (choice.equals("y") || choice.equals("Y")) {

			acquireNeighborNODE2 acquireN;
			for (int i = 0; i < neighborList.size(); i++) {
				System.out.println("\nAcquiring Neighbor...");

				acquireN = new acquireNeighborNODE2(portList.get(i), nDB, neighborList.get(i), ipaddr.get(i), selfID,
						aliveInterval, updateInterval, pVersion);
				new Thread(acquireN).start();

			}

		} else {
			System.out.println("Exiting, Bye Bye !!!");
			System.exit(0);
		}
		
		// Starting Neighbor Alive Thread
		for (int i = 0; i < neighborList.size(); i++) {
			System.out.println("\nSending Hello Packets..");

			neighbor_Alive2 nAlive = new neighbor_Alive2(portList.get(i), nDB, neighborList.get(i), ipaddr.get(i),
					linkID.get(i), selfID, aliveInterval);
			new Thread(nAlive).start();

		}
		
		// Initializing LSA Database
		lsa_database2 lsa = new lsa_database2(nDB, neighborList, selfID, linkID);

		// Starting FTP Receiving Thread
		recftp2 rP4001 = new recftp2(fport, lsa, selfID, nDB, ftpPort, ftp_socket, selfint, number_of_nodes);
		new Thread(rP4001).start();

		// Start LSA Thread to handle received LSA Packets
		lsaThread2 lsaT = new lsaThread2(lsaQueue, lsa, nDB, selfID, neighborList);
		new Thread(lsaT).start();
		
		// Starting Delay Thread
		for (int i = 0; i < neighborList.size(); i++) {
			delayCalculate2 delC = new delayCalculate2(nDB, neighborList.get(i), lsa, linkID.get(i), neighborList,
					selfID);
			new Thread(delC).start();
		}

		// Starting LSA send Thread

		lsaSend2 lsaS = new lsaSend2(lsa, nDB, neighborList, selfID, linkID);
		new Thread(lsaS).start();
		
		// Menu and Command Prompt
		while (true) {
			System.out.println("\nType help for the list of commands.");
            System.out.println("#");

			in = new Scanner(System.in);
			choice = in.nextLine();

			if (choice.equals("show neighbor database")) {
				System.out.println(nDB.m);
			} else if (choice.equals("show lsa database")) {
				System.out.println("LSA: " + lsa.lsa);
			} else if (choice.equals("show dijkstra")) {

				int row = 0;
				int column = 0;
				int size = number_of_nodes;
				int graph[][] = new int[size][size];

				// initializing the graph with 9999
				for (int i = size; i < size; i++) {
					for (int j = 0; j < size; j++) {
						graph[i][j] = 9999;
					}
				}
				// inserting the cost value of the link in the graph to
				// calculate the Dijktras
				for (Map.Entry<String, ArrayList<String>> entry : lsa.lsa.entrySet()) {
					try {
						ArrayList<String> l = new ArrayList<String>();
						l = entry.getValue();
						// Creating Matrix(Row and Column) as an Input to the Dijktras Algorithm 
						if (l.get(0).equals("R1")) {
							row = 0;
						} else if (l.get(0).equals("R2")) {
							row = 1;
						} else if (l.get(0).equals("R3")) {
							row = 2;
						} else if (l.get(0).equals("R4")) {
							row = 3;
						} else if (l.get(0).equals("R5")) {
							row = 4;
						} else if (l.get(0).equals("R6")) {
							row = 5;
						} else if (l.get(0).equals("R7")) {
							row = 6;
						} else if (l.get(0).equals("R8")) {
							row = 7;
						} else if (l.get(0).equals("R9")) {
							row = 8;
						} else if (l.get(0).equals("R10")) {
							row = 9;
						} else if (l.get(0).equals("R11")) {
							row = 10;
						} else if (l.get(0).equals("R12")) {
							row = 11;
						} else if (l.get(0).equals("R13")) {
							row = 12;
						} else if (l.get(0).equals("R14")) {
							row = 13;
						} else if (l.get(0).equals("R15")) {
							row = 14;
						}
						// C0LOUMN
						if (l.get(1).equals("R1")) {
							column = 0;
						} else if (l.get(1).equals("R2")) {
							column = 1;
						} else if (l.get(1).equals("R3")) {
							column = 2;
						} else if (l.get(1).equals("R4")) {
							column = 3;
						} else if (l.get(1).equals("R5")) {
							column = 4;
						} else if (l.get(1).equals("R6")) {
							column = 5;
						} else if (l.get(1).equals("R7")) {
							column = 6;
						} else if (l.get(1).equals("R8")) {
							column = 7;
						} else if (l.get(1).equals("R9")) {
							column = 8;
						} else if (l.get(1).equals("R10")) {
							column = 9;
						} else if (l.get(1).equals("R11")) {
							column = 10;
						} else if (l.get(1).equals("R12")) {
							column = 11;
						} else if (l.get(1).equals("R13")) {
							column = 12;
						} else if (l.get(1).equals("R14")) {
							column = 13;
						} else if (l.get(1).equals("R15")) {
							column = 14;
						}

						graph[row][column] = Integer.parseInt(l.get(2));
						graph[column][row] = Integer.parseInt(l.get(2));

					} catch (IllegalStateException e) {
						System.out.println("Entry Removed");
					}
				}
				
				// Initializing and Calling the Dijktras Algorithm to compute the shortest path
				ShortestPath2 t = new ShortestPath2(size);

				t.dijkstra(graph, selfint);
				
				// Displaying the Output of Dijktras Algorithm
				System.out.println("Vertex   Distance from Source   Preceeding Hop");

				int j = 0;
				int k = 0;
				for (int i = 0; i < size; i++) {
					j = i + 1;
					k = t.next_hop[i] + 1;
					String phop = "R" + k;
					String nName = "R" + j;
					System.out.println(nName + " \t\t " + t.dist[i] + " \t\t " + phop);
				}

			} else if (choice.equals("ftp")) {

				int row = 0;
				int column = 0;
				int size = number_of_nodes;
				int graph[][] = new int[size][size];

				// initializing the graph with 9999
				for (int i = size; i < size; i++) {
					for (int j = 0; j < size; j++) {
						graph[i][j] = 9999;
					}
				}
				// inserting the cost value of the link in the graph to
				// calculate the Dijktras
				for (Map.Entry<String, ArrayList<String>> entry : lsa.lsa.entrySet()) {
					try {
						ArrayList<String> l = new ArrayList<String>();
						l = entry.getValue();
						// ROW
						if (l.get(0).equals("R1")) {
							row = 0;
						} else if (l.get(0).equals("R2")) {
							row = 1;
						} else if (l.get(0).equals("R3")) {
							row = 2;
						} else if (l.get(0).equals("R4")) {
							row = 3;
						} else if (l.get(0).equals("R5")) {
							row = 4;
						} else if (l.get(0).equals("R6")) {
							row = 5;
						} else if (l.get(0).equals("R7")) {
							row = 6;
						} else if (l.get(0).equals("R8")) {
							row = 7;
						} else if (l.get(0).equals("R9")) {
							row = 8;
						} else if (l.get(0).equals("R10")) {
							row = 9;
						} else if (l.get(0).equals("R11")) {
							row = 10;
						} else if (l.get(0).equals("R12")) {
							row = 11;
						} else if (l.get(0).equals("R13")) {
							row = 12;
						} else if (l.get(0).equals("R14")) {
							row = 13;
						} else if (l.get(0).equals("R15")) {
							row = 14;
						}
						// C0LOUMN
						if (l.get(1).equals("R1")) {
							column = 0;
						} else if (l.get(1).equals("R2")) {
							column = 1;
						} else if (l.get(1).equals("R3")) {
							column = 2;
						} else if (l.get(1).equals("R4")) {
							column = 3;
						} else if (l.get(1).equals("R5")) {
							column = 4;
						} else if (l.get(1).equals("R6")) {
							column = 5;
						} else if (l.get(1).equals("R7")) {
							column = 6;
						} else if (l.get(1).equals("R8")) {
							column = 7;
						} else if (l.get(1).equals("R9")) {
							column = 8;
						} else if (l.get(1).equals("R10")) {
							column = 9;
						} else if (l.get(1).equals("R11")) {
							column = 10;
						} else if (l.get(1).equals("R12")) {
							column = 11;
						} else if (l.get(1).equals("R13")) {
							column = 12;
						} else if (l.get(1).equals("R14")) {
							column = 13;
						} else if (l.get(1).equals("R15")) {
							column = 14;
						}

						graph[row][column] = Integer.parseInt(l.get(2));
						graph[column][row] = Integer.parseInt(l.get(2));

					} catch (IllegalStateException e) {
						System.out.println("Entry Removed");
					}
				}

				ShortestPath2 t = new ShortestPath2(size);

				t.dijkstra(graph, selfint);

				System.out.println("Vertex   Distance from Source   Preceeding Hop");

				int j = 0;
				int k = 0;
				for (int i = 0; i < size; i++) {
					j = i + 1;
					k = t.next_hop[i] + 1;
					String phop = "R" + k;
					String nName = "R" + j;
					System.out.println(nName + " \t\t " + t.dist[i] + " \t\t " + phop);
				}
				System.out.println("\nEnter the complete file name along with the extension.. (Make sure the file you are about to send is present in the location /afs/cs.pitt.edu/usr0/ank161/private/WAN/send_folder)");
				@SuppressWarnings("resource")
				Scanner in2 = new Scanner(System.in);
				String FILE_NAME = in2.next();

				System.out.println("\nEnter the Destination Router ID. (Router ID is of the form R1, R2, R3 etc)");
				@SuppressWarnings("resource")
				Scanner in3 = new Scanner(System.in);
				String Destination = in3.next();

				send2 send = new send2(FILE_NAME, Destination, t.next_hop, nDB, ftpPort, selfint); // sending
																									// function
				send.send_file();
			} else if (choice.equals("cease neighborship")) {
				System.out.println("\nEnter the Neighbor Router ID: ");
				in = new Scanner(System.in);
				choice = in.next();
				nDB.cease_neighborship(choice);

			} else if (choice.equals("start neighborship")) {
				System.out.println("\nEnter the Neighbor Router ID: ");
				in = new Scanner(System.in);
				choice = in.next();
				ArrayList<String> l = new ArrayList<String>();
				l = nDB.m.get(choice);
				l.add(2, "Not_Neighbor");
				l.remove(3);
			} else if (choice.equals("help")) {
				System.out.println("\n**************Command List******************");
				System.out.println(" show neighbor database ------ Command to show to the neighbor database");
				System.out.println(" show lsa database ----------- Command to show the LSA database");
				System.out.println(" show dijktras --------------- Command to show the routing table and djikstra calculation");
				System.out.println(" ftp ------------------------- Command to carry out file transfer");
				System.out.println(" cease neighborship ---------- Command to cease neighborship");
				System.out.println(" start neighborship ---------- Command to start neighborship");
                System.out.println("********************************************");
			} else {
				System.out.println("\n**************Command List******************");
				System.out.println(" show neighbor database ------ Command to show to the neighbor database");
				System.out.println(" show lsa database ----------- Command to show the LSA database");
				System.out.println(" show dijktras --------------- Command to show the routing table and djikstra cost calculation");
				System.out.println(" ftp ------------------------- Command to carry out file transfer");
				System.out.println(" cease neighborship ---------- Command to cease neighborship");
				System.out.println(" start neighborship ---------- Command to start neighborship");
                System.out.println("********************************************");
			}
		}

	}

	public static void DNSRegister(String Register, Packet packet) throws IOException, Exception {
		int timeout_flag = 1;
		String fileName = "/afs/cs.pitt.edu/usr0/ank161/public/DNS.txt";
		String DNSport = null;
		String DNSIP = null;
		long expectedtime = System.currentTimeMillis();
		do {
			try {
				FileReader fileReader = new FileReader(fileName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				StringBuilder sb = new StringBuilder();
				String line = bufferedReader.readLine();
				while (line != null) {
					sb.append(line);
					sb.append("\n");
					line = bufferedReader.readLine();
				}

				Scanner scan = new Scanner(sb.toString());
				DNSport = scan.nextLine();
				DNSIP = scan.nextLine();
				scan.close();

				bufferedReader.close();
				fileReader.close();
			} catch (FileNotFoundException ex) {
				System.out.println("\nUnable to open file '" + fileName + "'");
			} catch (IOException ex) {
				System.out.println("\nError reading file '" + fileName + "'");
			}

			try {
				int dport = Integer.parseInt(DNSport);
				Socket sock = new Socket(DNSIP, dport);
				sock.setSoTimeout(5000);
				DataOutputStream out = new DataOutputStream(sock.getOutputStream());
				DataInputStream in = new DataInputStream(sock.getInputStream());

				out.writeUTF(Register);
				System.out.println("\nRegistering on the DNS server..");
				ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
				oos.writeObject(packet);
				oos.flush();

				String ack = in.readUTF();
				if (ack.equals("Ack")) {
					System.out.println("Registration Complete.");
					timeout_flag = 0;
					// break;
				}

				in.close();
				sock.close();
			} catch (ConnectException e) {
				System.out.println("\nDNS server did not respond. Trying again.. ");
				timeout_flag = 1;
			} catch (SocketTimeoutException ste) {
				System.out.println("\nNo ack received from the DNS server. Trying again..");
				timeout_flag = 1;
			} catch (SocketException e) {
				System.out.println("\nNo ack received from the DNS server. Trying again..");
				timeout_flag = 1;
			}

			while (System.currentTimeMillis() < expectedtime) {
				// Empty Loop
			}
			expectedtime += 3000;

		} while (timeout_flag == 1);
	}

	public static ArrayList<String> DNSQuery(String selfID, String NeighborID)
			throws UnknownHostException, IOException {

		String fileName = "/afs/cs.pitt.edu/usr0/ank161/public/DNS.txt";
		String DNSport = null;
		String DNSIP = null;
		int timeout_flag = 1;
		ArrayList<String> DNSinfo = new ArrayList<String>();
		long expectedtime = System.currentTimeMillis();

		do {
			try {
				FileReader fileReader = new FileReader(fileName);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				StringBuilder sb = new StringBuilder();
				String line = bufferedReader.readLine();
				while (line != null) {
					sb.append(line);
					sb.append("\n");
					line = bufferedReader.readLine();
				}

				Scanner scan = new Scanner(sb.toString());
				DNSport = scan.nextLine();
				System.out.println("\nDNS Server Port = " + DNSport);
				DNSIP = scan.nextLine();
				System.out.println("DNS Server IP = " + DNSIP);
				scan.close();

				bufferedReader.close();
				fileReader.close();
			} catch (FileNotFoundException ex) {
				System.out.println("\nUnable to open file '" + fileName + "'");
				timeout_flag = 1;
			} catch (IOException ex) {
				System.out.println("\nError reading file '" + fileName + "'");
				timeout_flag = 1;
			}

			try {
				int dport = Integer.parseInt(DNSport);
				Socket sock = new Socket(DNSIP, dport);
				sock.setSoTimeout(5000);
				DataOutputStream out = new DataOutputStream(sock.getOutputStream());
				DataInputStream in = new DataInputStream(sock.getInputStream());

				out.writeUTF("Query");
				out.writeUTF(selfID);
				out.writeUTF(NeighborID);

				String IP = in.readUTF();
				String cport_s = in.readUTF();
				String fport_s = in.readUTF();

				if (selfID.equals(NeighborID)) {
					System.out.println("\nQuerying DNS for the information of self..");
					if (IP.equals("null") && cport_s.equals("null") && fport_s.equals("null")) {
						DNSinfo.add(0, "null");
						DNSinfo.add(1, "null");
						DNSinfo.add(2, "null");
						System.out.println("\nNo information received.");
						timeout_flag = 0;
					} else {
						DNSinfo.add(0, IP);
						DNSinfo.add(1, cport_s);
						DNSinfo.add(2, fport_s);
						System.out.println("\nDetails of self received.");
						System.out.println("IP = " + IP);
						System.out.println("Control Port = " + cport_s);
						System.out.println("FTP Port = " + fport_s);
						timeout_flag = 0;
					}
				} else {
					System.out.println("\nQuerying DNS for the information of " + NeighborID + "..");
					if (IP.equals("null") && cport_s.equals("null") && fport_s.equals("null")) {
						DNSinfo.add(0, "null");
						DNSinfo.add(1, "null");
						DNSinfo.add(2, "null");
						System.out.println("\nNo information of " + NeighborID + " received. Trying again..");
						timeout_flag = 1;
					} else {
						DNSinfo.add(0, IP);
						DNSinfo.add(1, cport_s);
						DNSinfo.add(2, fport_s);
						System.out.println("\nDetails of " + NeighborID + " received.");
						System.out.println("IP = " + IP);
						System.out.println("Control Port = " + cport_s);
						System.out.println("FTP Port = " + fport_s);
						timeout_flag = 0;
					}
				}
				sock.close();

			} catch (ConnectException e) {
				System.out.println("\nDNS server did not respond. Trying again.. ");
				timeout_flag = 1;
			} catch (SocketTimeoutException ste) {
				System.out.println("\nNo ack received from the DNS server. Trying again..");
				timeout_flag = 1;
			} catch (NumberFormatException e) {
				timeout_flag = 1;
			} catch (SocketException e) {
				timeout_flag = 1;
			}

			while (System.currentTimeMillis() < expectedtime) {
				// Empty Loop
			}
			expectedtime += 3000;
		} while (timeout_flag == 1);

		// return statement
		return DNSinfo;
	}
}

class send2 {

	final String FILE_Location = "/afs/cs.pitt.edu/usr0/ank161/private/WAN/send_folder/"; // you
																							// may
																							// change
																							// this
	FileInputStream fis = null;
	BufferedInputStream bis = null;
	DataOutputStream os = null;
	Socket sock = null;
	String FILE_NAME;
	String Destination1;
	int[] next1;
	neigborDatabase2 nDB1;
	Map<String, Integer> ftpPort;
	int selfint;
	int MTU = 1500;

	public send2(String FILE_NAME, String Destination, int[] next, neigborDatabase2 nDB, Map<String, Integer> ftpPort,
			int selfint) {
		this.FILE_NAME = FILE_NAME;
		this.Destination1 = Destination;
		this.next1 = next;
		this.nDB1 = nDB;
		this.ftpPort = ftpPort;
		this.selfint = selfint;
	}

	public void send_file() throws IOException {

		String nexthop = nexthop1(Destination1, next1, nDB1);    // Recursively checking the Dijktras Table to find the next hop for the given destination
		System.out.println("\nFor the Destination " + Destination1 + ", the next hop is " + nexthop + ".");
		int nextport = ftpPort.get(nexthop);                     // Getting the port details of the next hop
		System.out.println("Sending file to " + nexthop + " on port " + nextport + ".");
		String FILE_TO_SEND1 = FILE_Location + FILE_NAME;

		try {
			sock = new Socket(nDB1.m.get(nexthop).get(0), nextport);
			File myFile = new File(FILE_TO_SEND1);

			int fileLength = (int) myFile.length();
			fis = new FileInputStream(myFile);
			bis = new BufferedInputStream(fis);

			os = new DataOutputStream(sock.getOutputStream());
			System.out.println("Sending " + FILE_NAME + "(" + fileLength + " bytes)");
			os.writeInt(fileLength + 1); // Sending the length of the
											// file
			os.flush();
			os.writeUTF(Destination1); // Sending the destination router ID
			os.flush();
			os.writeUTF(FILE_NAME);
			os.flush();
			int len = 0;
			byte[] buf = new byte[MTU];
			while ((len = fis.read(buf)) > 0) {
				os.write(buf, 0, len);
			}

			os.flush();
			System.out.println("File uploaded");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bis != null)
				bis.close();
			if (os != null)
				os.close();
			if (sock != null)
				sock.close();
			//System.out.println("Continuing to listen on port 4001..");
		}

	}

	public String nexthop1(String Destination1, int[] next1, neigborDatabase2 nDB1) {
		int actual_destination = 0;

		System.out.println("Destination: " + Destination1);
		while (true) {
			if (Destination1.equals("R1")) {
				actual_destination = 0;
			} else if (Destination1.equals("R2")) {
				actual_destination = 1;
			} else if (Destination1.equals("R3")) {
				actual_destination = 2;
			} else if (Destination1.equals("R4")) {
				actual_destination = 3;
			} else if (Destination1.equals("R5")) {
				actual_destination = 4;
			} else if (Destination1.equals("R6")) {
				actual_destination = 5;
			} else if (Destination1.equals("R7")) {
				actual_destination = 6;
			} else if (Destination1.equals("R8")) {
				actual_destination = 7;
			} else if (Destination1.equals("R9")) {
				actual_destination = 8;
			} else if (Destination1.equals("R10")) {
				actual_destination = 9;
			} else if (Destination1.equals("R11")) {
				actual_destination = 10;
			} else if (Destination1.equals("R12")) {
				actual_destination = 11;
			} else if (Destination1.equals("R13")) {
				actual_destination = 12;
			} else if (Destination1.equals("R14")) {
				actual_destination = 13;
			} else if (Destination1.equals("R15")) {
				actual_destination = 14;
			}
			
			
			int next = next1[actual_destination];
			if (next == selfint) {
				actual_destination = actual_destination + 1;
				String nexthop = "R" + Integer.toString(actual_destination);
				return nexthop;
			}
			next = next + 1;
			String nexthop = "R" + Integer.toString(next);

			if (nDB1.m.containsKey(nexthop)) {
				System.out.println("Returning Next Hop: " + nexthop);
				return nexthop;

			} else {
				Destination1 = nexthop;
			}
		}

	}

}

class neigborDatabase2 {

	volatile Map<String, ArrayList<String>> m = new HashMap<String, ArrayList<String>>();

	ArrayList<Integer> portList = new ArrayList<Integer>(); 
	ArrayList<String> neighborList = new ArrayList<String>();
	ArrayList<String> ipaddr = new ArrayList<String>();

	public neigborDatabase2(ArrayList<String> neighborList, ArrayList<Integer> portList, ArrayList<String> ipaddr) { 
		this.neighborList = neighborList;
		for (int i = 0; i < neighborList.size(); i++) {
			ArrayList<String> l = new ArrayList<String>();
			l = m.get(neighborList.get(i));
			if (l == null) {
				m.put(neighborList.get(i), new ArrayList<String>());
			}
		}
		// Initializing Values
		this.portList = portList;
		this.ipaddr = ipaddr;

		for (int i = 0; i < portList.size(); i++) {
			ArrayList<String> l = new ArrayList<String>();
			l = m.get(neighborList.get(i));
			l.add(0, ipaddr.get(i));
			l.add(1, Integer.toString(portList.get(i)));
			l.add(2, "Not_Neighbor");

			m.put(neighborList.get(i), l);
		}
	}

	public synchronized void display() {
		System.out.println(m);
	}

	public synchronized void neighbor_remove(String nrouter_ID) {
		System.out.println("\nRemoving Neighborship");
		ArrayList<String> l = new ArrayList<String>();
		l = m.get(nrouter_ID);
		l.add(2, "Not_Neighbor");
		l.remove(3);
		m.put(nrouter_ID, l);
		this.display();
	}

	public synchronized void cease_neighborship(String nrouter_ID) {
												
		System.out.println("\nCeasing Neighborship");
		ArrayList<String> l = new ArrayList<String>();
		l = m.get(nrouter_ID);
		l.add(2, "Ceased Neighbor");
		l.remove(3);
		m.put(nrouter_ID, l);
		this.display();
	}

	public synchronized void update_neighborship(nPacket np) {
		String neighbor = np.source_routerID;

		ArrayList<String> l = new ArrayList<String>();
		l = m.get(neighbor);
		if (np.ptype.equals("Be_Neighbor_Confirm")) {

			l.add(2, "Neighbor");
			l.remove(3);
			m.put(neighbor, l);
		}
	}
}

class acquireNeighborNODE2 implements Runnable {

	int port;
	neigborDatabase2 ndatabase;
	String nrouter_ID;
	String ipaddress;
	String selfID;
	int aliveInterval;
	int updateInterval;
	int pVersion;

	public acquireNeighborNODE2(int port, neigborDatabase2 ndatabase, String nrouter_ID, String ipaddress,
			String selfID, int aliveInterval, int updateInterval, int pVersion) {
		this.port = port;
		this.ndatabase = ndatabase;
		this.nrouter_ID = nrouter_ID;
		this.ipaddress = ipaddress;
		this.selfID = selfID;
		this.aliveInterval = aliveInterval;
		this.updateInterval = updateInterval;
		this.pVersion = pVersion;
	}

	public void run() {
		// TODO Auto-generated method stub
		while (true) {

			while (ndatabase.m.get(nrouter_ID).get(2).equals("Not_Neighbor")) {
				int nei_not_alive = 3;
				int nei_time_out = 3;
				int timeout_flag = 1;
				int noalive_flag = 1;
				do {
					try {
					

						Socket s = new Socket(ipaddress, port);
						s.setSoTimeout(5000);
						java.io.OutputStream os = s.getOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(os);
						InputStream is = s.getInputStream();
						ObjectInputStream ois = new ObjectInputStream(is);

						do {
							try {

								nPacket sendNR = new nPacket("Be_Neighbor_Request", selfID, aliveInterval,
										updateInterval, pVersion);
								oos.writeObject(sendNR);
								nPacket recAck = (nPacket) ois.readObject();

								timeout_flag = 0;
								ndatabase.update_neighborship(recAck);

							} catch (SocketTimeoutException e) {

								System.err.println("\nTimed-Out");
								System.out.println("Node did not respond, Sending Request Again " + nrouter_ID);
								timeout_flag = 1;
								nei_time_out--;
							}
						} while (nei_time_out != 0 && timeout_flag == 1);

						oos.close();
						os.close();
						s.close();
						noalive_flag = 0;
					} catch (ConnectException e) {

						System.out.println("\nNeighbor Not Reachable " + nrouter_ID);
						noalive_flag = 1;
						nei_not_alive--;
					} catch (SocketTimeoutException e) {
						System.out.println("\nNeighbor Not Reachable " + nrouter_ID);
						noalive_flag = 1;
						nei_not_alive--;
					} catch (SocketException e) {
						System.out.println("\nNeighbor Not Reachable " + nrouter_ID);
						noalive_flag = 1;
						nei_not_alive--;
					} catch (Exception e) { // handle the StackTraces Carefully
						e.printStackTrace();
					}
				} while (nei_not_alive != 0 && noalive_flag == 1);

				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

}

class receivePort2000 implements Runnable {

	int port;
	neigborDatabase2 neighbor;
	neighborQUEUE2 nQUEUE;
	ServerSocket ctrl_socket;

	receivePort2000(int port, neigborDatabase2 neighbor, neighborQUEUE2 nQUEUE, ServerSocket ctrl_socket) {
		this.port = port;
		this.neighbor = neighbor;
		this.nQUEUE = nQUEUE;
		this.ctrl_socket = ctrl_socket;
	}

	public void run() {
		// TODO Auto-generated method stub
	
		try {
		// collecting each connection as socket and storing in the queue
			while (true) {
				Socket s = ctrl_socket.accept();
				nQUEUE.add(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}

class neighborQUEUE2 {

	int capacity;
	volatile BlockingQueue<Socket> Nqueue;

	// Initializing the Queue to collect the Incoming Neighbor Request packet
	public neighborQUEUE2(int capacity) {
		this.capacity = capacity;
		Nqueue = new ArrayBlockingQueue<Socket>(capacity);
	}

	public synchronized void add(Socket s) throws InterruptedException {
		Nqueue.put(s);

	}

}

class queueThread2 implements Runnable {

	neighborQUEUE2 nQUEUE;
	neigborDatabase2 nDB;
	lsaQ2 lsaQueue;
	String selfID;
	int aliveInterval;
	int updateInterval;
	int pVersion;

	public queueThread2(neighborQUEUE2 nQUEUE, neigborDatabase2 nDB, lsaQ2 lsaQueue, String selfID, int aliveInterval,
			int updateInterval, int pVersion) {
		this.nQUEUE = nQUEUE;
		this.nDB = nDB;
		this.lsaQueue = lsaQueue;
		this.selfID = selfID;
		this.aliveInterval = aliveInterval;
		this.updateInterval = updateInterval;
		this.pVersion = pVersion;
	}

	public void run() {
		// TODO Auto-generated method stub
		// Taking each socket from the queue and then serving it based on the type of packet
		try {
			while (true) {
				Socket s = nQUEUE.Nqueue.take();
				InputStream is = s.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(is);
				java.io.OutputStream os = s.getOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(os);
				nPacket np = (nPacket) ois.readObject();

				nPacket ackN;
				if (np.ptype.equals("Be_Neighbor_Request") && nDB.m.containsKey(np.source_routerID)) {

					if (np.updateInterval == updateInterval && np.aliveInterval == aliveInterval
							&& np.pVersion == pVersion) {
						ackN = new nPacket("Be_Neighbor_Confirm", selfID);
						oos.writeObject(ackN);
						ArrayList<String> l = new ArrayList<String>();
						l = nDB.m.get(np.source_routerID);
						l.add(2, "Neighbor");
						l.remove(3);
						nDB.m.put(np.source_routerID, l);

					} else {
						ackN = new nPacket("Be_Neighbor_Refuse", selfID);
						oos.writeObject(ackN);
					}
				} else if (np.ptype.equals("Hello") && nDB.m.containsKey(np.source_routerID)) {

					if (nDB.m.get(np.source_routerID).get(2).equals("Neighbor")) {
						ackN = new nPacket("Hello_Ack", selfID);
						oos.writeObject(ackN);
					} else if (nDB.m.get(np.source_routerID).get(2).equals("Ceased Neighbor")) {
						ackN = new nPacket("Cease Neighborship", selfID);
						oos.writeObject(ackN);
					}

				} else if (np.ptype.equals("Delay")) {
					ackN = new nPacket("Delay_Ack", selfID);
					oos.writeObject(ackN);
				} else if (np.ptype.equals("lsa")) {

					String bigString = "lsa" + selfID + Integer.toString(np.len) + Integer.toString(np.numLinks)
							+ np.advRouter;
					byte[] bytesofMessage = bigString.getBytes("UTF-8");
					MessageDigest md = MessageDigest.getInstance("MD5");

					byte[] rdigest = md.digest(bytesofMessage);

					ackN = new nPacket("lsa_ack", selfID);
					oos.writeObject(ackN);
					lsaQueue.add(np);

				}
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

class neighbor_Alive2 implements Runnable {

	neigborDatabase2 nDB;
	int port;
	String nrouter_ID;
	String ipaddress;
	String LinkID;
	String selfID;
	int aliveInterval;

	public neighbor_Alive2(int port, neigborDatabase2 ndatabase, String nrouter_ID, String ipaddress, String LinkID,
			String selfID, int aliveInterval) {
		this.port = port;
		this.nDB = ndatabase;
		this.nrouter_ID = nrouter_ID;
		this.ipaddress = ipaddress;
		this.LinkID = LinkID;
		this.selfID = selfID;
		this.aliveInterval = aliveInterval * 1000;
		System.out.println("Hello");
	}

	public void run() {

		while (true) {
			int cease_nei_flag = 0;
			int nei_not_alive = 3;
			int nei_time_out = 3;
			int timeout_flag = 1;
			int noalive_flag = 1;
			do {
				if (nDB.m.get(nrouter_ID).get(2).equals("Neighbor")) {
					try {

					
						Socket s = new Socket(ipaddress, port);
						s.setSoTimeout(5000);
						java.io.OutputStream os = s.getOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(os);
						InputStream is = s.getInputStream();
						ObjectInputStream ois = new ObjectInputStream(is);

						do {
							try {
								nPacket send_hello = new nPacket("Hello", selfID);
								oos.writeObject(send_hello);
								nPacket rec_hello = (nPacket) ois.readObject();

								if (rec_hello.ptype.endsWith("Cease Neighborship")) {
									cease_nei_flag = 1;
									timeout_flag = 0;
								} else if (rec_hello.ptype.endsWith("Hello_Ack")) {
									timeout_flag = 0;
								}
								Thread.sleep(aliveInterval);

							} catch (SocketTimeoutException e) {

								System.out.println("\nTime Out, Node did not respond, Sending HELLO Again " + nrouter_ID);
								timeout_flag = 1;
								nei_time_out--;
								Thread.sleep(aliveInterval);
							} catch (SocketException e) {
								System.out.println("\nSocket Closed by Peer Sending HELLO Again " + nrouter_ID);
								timeout_flag = 1;
								nei_time_out--;
								Thread.sleep(aliveInterval);

							}

						} while (nei_time_out != 0 && timeout_flag == 1);
						oos.close();
						os.close();
						s.close();
						noalive_flag = 0;

					} catch (ConnectException e) {

						System.out.println("\nHello Packet failed, Trying Again !!! " + nrouter_ID);
						noalive_flag = 1;
						nei_not_alive--;

					} catch (SocketTimeoutException e) {
						System.out.println("\nHello Packet failed, Trying Again !!! " + nrouter_ID);
						noalive_flag = 1;
						nei_not_alive--;

					} catch (SocketException e) {
						System.out.println("\nHello Packet failed, Trying Again !!! " + nrouter_ID);
						noalive_flag = 1;
						nei_not_alive--;
					} catch (Exception e) {

						e.printStackTrace();
					}
				}

			} while (nei_not_alive != 0 && noalive_flag == 1);

			if (nei_not_alive == 0) {
				// remove the neighborship
				nDB.neighbor_remove(nrouter_ID);
			}
			if (cease_nei_flag == 1) {  
			// ceasing the neighborship
				nDB.cease_neighborship(nrouter_ID);
			}

		}
	}
}

class delayCalculate2 implements Runnable {
	neigborDatabase2 nDB;
	String neighbor;
	lsa_database2 lsaDB;
	String linkID;
	ArrayList<String> neighborList;
	String selfID;
	int cost_change = 0;

	public delayCalculate2(neigborDatabase2 nDB, String neighbor, lsa_database2 lsaDB, String linkID,
			ArrayList<String> neighborList, String selfID) {
		this.nDB = nDB;
		this.neighbor = neighbor;
		this.lsaDB = lsaDB;
		this.linkID = linkID;
		this.neighborList = neighborList;
		this.selfID = selfID;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while (true) {

			int sum = 0;
			int i = 0;
			int nei_not_alive = 0;
			cost_change = 0;

			if (nDB.m.get(neighbor).get(2).equals("Neighbor")) {
				ArrayList<String> l = nDB.m.get(neighbor);
				String ipAddr = l.get(0);
				int port = Integer.parseInt(l.get(1));
				ArrayList<Integer> avg = new ArrayList<Integer>();
				do {
					if (nDB.m.get(neighbor).get(2).equals("Neighbor")) {
						try {

							Socket s = new Socket(ipAddr, port);
							s.setSoTimeout(5000);
							java.io.OutputStream os = s.getOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(os);
							InputStream is = s.getInputStream();
							ObjectInputStream ois = new ObjectInputStream(is);

							nPacket hello_send = new nPacket("Delay", "R1");

							Timestamp ts1 = new Timestamp(System.currentTimeMillis());
							int ts_send = ts1.getNanos();
							oos.writeObject(hello_send);

							nPacket hello_ack = (nPacket) ois.readObject();
							ts1 = new Timestamp(System.currentTimeMillis());
							int ts_ack = ts1.getNanos();
							avg.add(((ts_ack - ts_send) / 1000000));
							nei_not_alive = 0;
							oos.close();
							os.close();
							s.close();
						} catch (ConnectException e) {
							System.out.println("\nNODE Not Reachable(Delay) " + neighbor);
							nei_not_alive++;

						} catch (SocketTimeoutException e) {
							System.err.println("\nTimed-Out");
							System.out.println("Node did not respond, Sending Delay packet Again " + neighbor);
							nei_not_alive++;

						} catch (SocketException e) {
							System.err.println("\nTimed-Out");
							System.out.println("Node did not respond, Sending Delay packet Again " + neighbor);
							nei_not_alive++;
						} catch (Exception e) {

							e.printStackTrace();
						}
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						i++;
						if (nei_not_alive == 3) {

							break;
						}
					} else if (nDB.m.get(neighbor).get(2).equals("Ceased Neighbor")) {
						break;
					}
				} while (i < 8);

				if (nei_not_alive != 3) {
					for (int j = 0; j < avg.size(); j++) {
						sum = sum + avg.get(j);

					}

					ArrayList<String> val = lsaDB.lsa.get(linkID);

					if (avg.size() > 0 && l.get(2).equals("Neighbor")) {
						int c = Math.abs(sum / avg.size());
						if (c >= 0 && c < 100) {
							if (!lsaDB.lsa.get(linkID).get(2).equals("50")) {
								cost_change = 1;
							}
							val.add(2, Integer.toString(50));
							val.remove(3);
						} else if (c >= 100 && c < 200) {
							if (!lsaDB.lsa.get(linkID).get(2).equals("150")) {
								cost_change = 1;
							}
							val.add(2, Integer.toString(150));
							val.remove(3);
						} else if (c >= 200 && c < 300) {
							if (!lsaDB.lsa.get(linkID).get(2).equals("250")) {
								cost_change = 1;
							}
							val.add(2, Integer.toString(250));
							val.remove(3);
						} else {
							if (!lsaDB.lsa.get(linkID).get(2).equals("400")) {
								cost_change = 1;
							}
							val.add(2, Integer.toString(400));
							val.remove(3);
						}

					} else {
						if (!lsaDB.lsa.get(linkID).get(2).equals("400")) {
							cost_change = 1;
						}
						val.add(2, "9999");
						val.remove(3);
					}
					lsaDB.lsa.put(linkID, val);

				} else { // neighbor did not respond therefore making its cost
							// to 9999
					ArrayList<String> val = lsaDB.lsa.get(linkID);
					if (!lsaDB.lsa.get(linkID).get(2).equals("9999")) {
						cost_change = 1;
					}
					val.add(2, "9999");
					val.remove(3);
					lsaDB.lsa.put(linkID, val);

				}
				if (cost_change == 1) {
					System.out.println("\nCost Changed");
					ArrayList<String> x = lsaDB.lsa.get(linkID);

					int seq = Integer.parseInt(x.get(3)) + 1;
					x.add(3, Integer.toString(seq));
					x.remove(4);
					lsaDB.lsa.put(linkID, x);
					for (String neighbor : neighborList) {

						nei_not_alive = 0;

						if (nDB.m.get(neighbor).get(2).equals("Neighbor")) {
							l = nDB.m.get(neighbor);
							ipAddr = l.get(0);
							port = Integer.parseInt(l.get(1));

							do {
								try {
									Socket s = new Socket(ipAddr, port);
									s.setSoTimeout(5000);
									java.io.OutputStream os = s.getOutputStream();
									ObjectOutputStream oos = new ObjectOutputStream(os);
									InputStream is = s.getInputStream();
									ObjectInputStream ois = new ObjectInputStream(is);

									Map<String, ArrayList<String>> k = new HashMap<String, ArrayList<String>>();
									k.put(linkID, x);

									String bigString = "lsa" + selfID + Integer.toString(20)
											+ Integer.toString(lsaDB.lsa.size()) + selfID;
									byte[] bytesofMessage = bigString.getBytes("UTF-8");
									MessageDigest md = MessageDigest.getInstance("MD5");
									byte[] thedigest = md.digest(bytesofMessage);
									nPacket lsaP = new nPacket(lsaDB.lsa, "lsa", selfID, 20, lsaDB.lsa.size(), selfID,
											thedigest);

									oos.writeObject(lsaP);

									nPacket lsa_ack = (nPacket) ois.readObject();
									nei_not_alive = 0;
									oos.close();
									os.close();
									s.close();
								} catch (ConnectException e) {
									System.out.println("\nNODE Not Reachable(Cost Changed) " + neighbor);
									nei_not_alive++;

								} catch (SocketTimeoutException e) {
									System.err.println("\nTimed-Out");
									System.out.println("Node did not respond, Cost Changed " + neighbor);
									nei_not_alive++;

								} catch (SocketException e) {
									System.err.println("\nTimed-Out");
									System.out.println("Node did not respond, Cost Changed " + neighbor);
									nei_not_alive++;
								} catch (Exception e) {
									e.printStackTrace();
								}
								try {
									Thread.sleep(5000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								if (nei_not_alive == 3) {

									break;
								}
							} while (nei_not_alive != 0);

						}

					}
				}
			}

		}
	}

}

class lsa_database2 {

	volatile ConcurrentHashMap<String, ArrayList<String>> lsa = new ConcurrentHashMap<String, ArrayList<String>>();
	neigborDatabase2 nDB;
	ArrayList<String> neighbor;
	String selfID;

	public lsa_database2(neigborDatabase2 nDB, ArrayList<String> neighbor, String selfID, ArrayList<String> linkID) {

		this.nDB = nDB;
		this.neighbor = neighbor;
		this.selfID = selfID;
		int i = 0;
		for (String neighborID : neighbor) {

			ArrayList<String> l = new ArrayList<String>();
			l.add(selfID); // Source
			l.add(neighborID); // Neighbor
			l.add("9999"); // Cost
			l.add("0"); // sequence number
			lsa.put(linkID.get(i), l);
			i++;
		}

		System.out.println(lsa);

	}

	public synchronized void update_cost(String LinkID) {
		ArrayList<String> l = lsa.get(LinkID);
		l.add(2, "9999");
		l.remove(3);
		lsa.put(LinkID, l);

	}

}

class lsaQ2 {

	int capacity;
	volatile BlockingQueue<nPacket> lsaqueue;
	nPacket np;

	// Initializing the Queue to collect the Incoming Neighbor Request packet
	public lsaQ2(int capacity) {
		this.capacity = capacity;
		lsaqueue = new ArrayBlockingQueue<nPacket>(capacity);
	}

	public synchronized void add(nPacket np) throws InterruptedException {
		lsaqueue.put(np);

	}
}

class lsaSend2 implements Runnable {

	neigborDatabase2 nDB;
	ArrayList<String> neighborList;
	String selfID;
	lsa_database2 lsaDB;
	ArrayList<String> linkID;
	byte[] thedigest;

	public lsaSend2(lsa_database2 lsaDB, neigborDatabase2 nDB, ArrayList<String> neighborList, String selfID,
			ArrayList<String> linkID) {
		this.lsaDB = lsaDB;
		this.nDB = nDB;
		this.neighborList = neighborList;
		this.selfID = selfID;
		this.linkID = linkID;

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while (true) {
			Random r = new Random();

			int Result = (r.nextInt(10000 - 1) + 1);
			for (String link : linkID) {
				ArrayList<String> x = new ArrayList<String>();
				x = lsaDB.lsa.get(link);
				int seq = Integer.parseInt(x.get(3)) + 1;

				x.add(3, Integer.toString(seq));
				x.remove(4);
				lsaDB.lsa.put(link, x);
			}

			for (String neighbor : neighborList) {

				int nei_not_alive = 0;

				if (nDB.m.get(neighbor).get(2).equals("Neighbor") && Result != 1) {
					ArrayList<String> l = nDB.m.get(neighbor);
					String ipAddr = l.get(0);
					int port = Integer.parseInt(l.get(1));

					do {
						try {
							Socket s = new Socket(ipAddr, port);
							s.setSoTimeout(5000);
							java.io.OutputStream os = s.getOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(os);
							InputStream is = s.getInputStream();
							ObjectInputStream ois = new ObjectInputStream(is);
							String bigString = "lsa" + selfID + Integer.toString(20)
									+ Integer.toString(lsaDB.lsa.size()) + selfID;
							byte[] bytesofMessage = bigString.getBytes("UTF-8");
							MessageDigest md = MessageDigest.getInstance("MD5");
							byte[] thedigest = md.digest(bytesofMessage);

							nPacket lsaP = new nPacket(lsaDB.lsa, "lsa", selfID, 20, lsaDB.lsa.size(), selfID,
									thedigest);

							oos.writeObject(lsaP);

							nPacket lsa_ack = (nPacket) ois.readObject();
							nei_not_alive = 0;
							oos.close();
							os.close();
							s.close();
						} catch (ConnectException e) {
							System.out.println("\nNODE Not Reachable(LSA SEND) " + neighbor);
							nei_not_alive++;

						} catch (SocketTimeoutException e) {
							System.err.println("\nTimed-Out");
							System.out.println("Node did not respond, Sending LSA Again " + neighbor);
							nei_not_alive++;

						} catch (SocketException e) {
							System.err.println("\nTimed-Out");
							System.out.println("Node did not respond, Sending LSA Again " + neighbor);
							nei_not_alive++;
						} catch (Exception e) {
							e.printStackTrace();
						}
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} while (nei_not_alive != 0);

				}

			}
			try {
				Thread.sleep(1800000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

class lsaThread2 implements Runnable {

	lsaQ2 lsaQueue;
	lsa_database2 lsaDB;
	neigborDatabase2 nDB;
	String selfID;
	ArrayList<String> neighborList;

	public lsaThread2(lsaQ2 lsaQueue, lsa_database2 lsaDB, neigborDatabase2 nDB, String selfID,
			ArrayList<String> neighborList) {
		this.lsaDB = lsaDB;
		this.lsaQueue = lsaQueue;
		this.nDB = nDB;
		this.selfID = selfID;
		this.neighborList = neighborList;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				nPacket np = lsaQueue.lsaqueue.take();

				if (!np.advRouter.equals(selfID)) {

					for (String link : np.lsa.keySet()) {
						ArrayList<String> l = new ArrayList<String>();
						l = np.lsa.get(link);
						int flag = 0;
						ArrayList<String> y = new ArrayList<String>();
						y = np.lsa.get(link);
						for (String link2 : lsaDB.lsa.keySet()) {
							if (link2.equals(link)) {
								flag = 1;
								ArrayList<String> y2 = lsaDB.lsa.get(link2);
								if (Integer.parseInt(y2.get(3)) < Integer.parseInt(y.get(3))) {
									lsaDB.lsa.put(link2, y);
									System.out.println("\nLinkID updated in LSA Updated");
								}
							}
						}
						if (flag != 1 && !l.get(2).equals("9999")) {
							lsaDB.lsa.put(link, y);
							System.out.println("\nLink Added in the LSA Database.");
						}
					}

					for (String neighbor : neighborList) {
						Random r = new Random();
						int Result = (r.nextInt(10000 - 1) + 1);

						int nei_not_alive = 0;

						if (!neighbor.equals(np.source_routerID) && nDB.m.get(neighbor).get(2).equals("Neighbor")
								&& Result != 1) {
							ArrayList<String> l = nDB.m.get(neighbor);
							String ipAddr = l.get(0);
							int port = Integer.parseInt(l.get(1));

							do {
								if (nDB.m.get(neighbor).get(2).equals("Neighbor")) {
									try {
										Socket s = new Socket(ipAddr, port);
										s.setSoTimeout(5000);
										java.io.OutputStream os = s.getOutputStream();
										ObjectOutputStream oos = new ObjectOutputStream(os);
										InputStream is = s.getInputStream();
										ObjectInputStream ois = new ObjectInputStream(is);
										String bigString = "lsa" + selfID + Integer.toString(np.len)
												+ Integer.toString(np.numLinks) + np.advRouter;
										byte[] bytesofMessage = bigString.getBytes("UTF-8");
										MessageDigest md = MessageDigest.getInstance("MD5");
										byte[] thedigest = md.digest(bytesofMessage);

										nPacket lsaP = new nPacket(np.lsa, "lsa", selfID, np.len, np.numLinks,
												np.advRouter, thedigest);

										oos.writeObject(lsaP);

										nPacket lsa_ack = (nPacket) ois.readObject();
										nei_not_alive = 0;
										oos.close();
										os.close();
										s.close();
									} catch (ConnectException e) {
										System.out.println("\nNODE Not Reachable(LSA ADV) " + neighbor);
										nei_not_alive++;

									} catch (SocketTimeoutException e) {
										System.err.println("\nTimed-Out");
										System.out.println("Node did not respond, Sending LSA ADV Again " + neighbor);
										nei_not_alive++;

									} catch (SocketException e) {
										System.err.println("\nTimed-Out");
										System.out.println("Node did not respond, Sending LSA ADV Again " + neighbor);
										nei_not_alive++;
									} catch (Exception e) {
										e.printStackTrace();
									}
								} else {
									break;
								}
								Thread.sleep(5000);

							} while (nei_not_alive != 0);

						}

					}

				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class ShortestPath2 {

	int V;
	volatile int next_hop[];
	volatile int dist[];

	public ShortestPath2(int V) {
		this.V = V;
		this.next_hop = new int[V];
		this.dist = new int[V];
	}

	int minDistance(int dist[], Boolean sptSet[]) {
		int min = Integer.MAX_VALUE, min_index = -1;

		for (int v = 0; v < V; v++)
			if (sptSet[v] == false && dist[v] <= min) {
				min = dist[v];
				min_index = v;
			}

		return min_index;
	}

	void dijkstra(int graph[][], int src) {

		Boolean sptSet[] = new Boolean[V];

		for (int i = 0; i < V; i++) {
			dist[i] = Integer.MAX_VALUE;
			sptSet[i] = false;
		}

		dist[src] = 0;
		next_hop[src] = src;
		for (int count = 0; count < V - 1; count++) {

			int u = minDistance(dist, sptSet);
			sptSet[u] = true;

			for (int v = 0; v < V; v++) {
				if (!sptSet[v] && graph[u][v] != 0 && dist[u] != Integer.MAX_VALUE && dist[u] + graph[u][v] < dist[v]) {
					dist[v] = dist[u] + graph[u][v];
					next_hop[v] = u;
				}

			}

		}

	}
}

class recftp2 implements Runnable {

	int port;
	public final static String LOCATION_TO_RECEIVE = "/afs/cs.pitt.edu/usr0/ank161/private/WAN/receive_folder/";
	String self;
	lsa_database2 lsaDB;
	int selfint;
	neigborDatabase2 nDB;
	Map<String, Integer> ftpPort;
	ServerSocket ftp_socket;
	int number_of_nodes;
	int MTU = 1500;

	public recftp2(int port, lsa_database2 lsaDB, String selfID, neigborDatabase2 nDB, Map<String, Integer> ftpPort,
			ServerSocket ftp_socket, int selfint, int number_of_nodes) {
		this.port = port;
		this.lsaDB = lsaDB;
		this.self = selfID;
		this.nDB = nDB;
		this.ftpPort = ftpPort;
		this.ftp_socket = ftp_socket;
		this.selfint = selfint;
		this.number_of_nodes = number_of_nodes;

	}

	@Override
	public void run() {
		int bytesRead;
		int current = 0;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		ServerSocket servsock = null;
		Socket sock = null;
		Socket fSocket = null;
		try {

			//System.out.println("Listening on Port: " + port);
			while (true) {
				//System.out.println("Waiting for connection from client...");
				try {
					sock = ftp_socket.accept();
					//System.out.println("Got a connection from a client..");
					DataInputStream is = new DataInputStream(sock.getInputStream());

					int FILE_SIZE = is.readInt();
					String destination = is.readUTF();
					String FILE_NAME = is.readUTF();
					String FILE_LOCATION = LOCATION_TO_RECEIVE + FILE_NAME;
					// }
					System.out.println("\nDestination Server = " + destination);

					if (destination.equals(self)) {
						byte[] mybytearray = new byte[FILE_SIZE];
						fos = new FileOutputStream(FILE_LOCATION);
						bos = new BufferedOutputStream(fos);
						bytesRead = is.read(mybytearray, 0, mybytearray.length);
						current = bytesRead;

						do {
							bytesRead = is.read(mybytearray, current, (mybytearray.length - current));
							if (bytesRead >= 0 && current < FILE_SIZE) {
								current += bytesRead;
							}
						} while (bytesRead > -1);

						bos.write(mybytearray, 0, current);
						bos.flush();
						System.out.println("\nFile received (" + current + " bytes read) at location " + LOCATION_TO_RECEIVE);
					} else {
						//System.out.println("\nForwarding the File.");
						int size = number_of_nodes;
						ShortestPath2 t = new ShortestPath2(size);
						//System.out.println("LSA Size: " + size);
						int row = 0;
						int column = 0;
						ArrayList<String> l1 = new ArrayList<String>();

						int graph[][] = new int[size][size];

						// initializing the graph with 9999
						for (int i = size; i < size; i++) {
							for (int j = 0; j < size; j++) {
								graph[i][j] = 9999;
							}
						}
						// inserting the cost value of the link in the graph to
						// calculate the Dijktras

						for (Map.Entry<String, ArrayList<String>> entry : lsaDB.lsa.entrySet()) {
							try {
								ArrayList<String> l = new ArrayList<String>();
								l = entry.getValue();
								// ROW
								if (l.get(0).equals("R1")) {
									row = 0;
								} else if (l.get(0).equals("R2")) {
									row = 1;
								} else if (l.get(0).equals("R3")) {
									row = 2;
								} else if (l.get(0).equals("R4")) {
									row = 3;
								} else if (l.get(0).equals("R5")) {
									row = 4;
								} else if (l.get(0).equals("R6")) {
									row = 5;
								} else if (l.get(0).equals("R7")) {
									row = 6;
								} else if (l.get(0).equals("R8")) {
									row = 7;
								} else if (l.get(0).equals("R9")) {
									row = 8;
								} else if (l.get(0).equals("R10")) {
									row = 9;
								} else if (l.get(0).equals("R11")) {
									row = 10;
								} else if (l.get(0).equals("R12")) {
									row = 11;
								} else if (l.get(0).equals("R13")) {
									row = 12;
								} else if (l.get(0).equals("R14")) {
									row = 13;
								} else if (l.get(0).equals("R15")) {
									row = 14;
								}
								// C0LOUMN
								if (l.get(1).equals("R1")) {
									column = 0;
								} else if (l.get(1).equals("R2")) {
									column = 1;
								} else if (l.get(1).equals("R3")) {
									column = 2;
								} else if (l.get(1).equals("R4")) {
									column = 3;
								} else if (l.get(1).equals("R5")) {
									column = 4;
								} else if (l.get(1).equals("R6")) {
									column = 5;
								} else if (l.get(1).equals("R7")) {
									column = 6;
								} else if (l.get(1).equals("R8")) {
									column = 7;
								} else if (l.get(1).equals("R9")) {
									column = 8;
								} else if (l.get(1).equals("R10")) {
									column = 9;
								} else if (l.get(1).equals("R11")) {
									column = 10;
								} else if (l.get(1).equals("R12")) {
									column = 11;
								} else if (l.get(1).equals("R13")) {
									column = 12;
								} else if (l.get(1).equals("R14")) {
									column = 13;
								} else if (l.get(1).equals("R15")) {
									column = 14;
								}

								graph[row][column] = Integer.parseInt(l.get(2));
								graph[column][row] = Integer.parseInt(l.get(2));

							} catch (IllegalStateException e) {
								System.out.println("Entry Removed");
							}
						}

						t.dijkstra(graph, selfint);

						System.out.println("Vertex   Distance from Source   Preceeding Hop");

						int j = 0;
						int k = 0;
						for (int i = 0; i < size; i++) {
							j = i + 1;
							k = t.next_hop[i] + 1;
							String phop = "R" + k;
							String nName = "R" + j;
							System.out.println(nName + " \t\t " + t.dist[i] + " \t\t " + phop);
						}

						int actual_destination = 0;
						String nexthop;
						while (true) {
							if (destination.equals("R1")) {
								actual_destination = 0;
							} else if (destination.equals("R2")) {
								actual_destination = 1;
							} else if (destination.equals("R3")) {
								actual_destination = 2;
							} else if (destination.equals("R4")) {
								actual_destination = 3;
							} else if (destination.equals("R5")) {
								actual_destination = 4;
							} else if (destination.equals("R6")) {
								actual_destination = 5;
							} else if (destination.equals("R7")) {
								actual_destination = 6;
							} else if (destination.equals("R8")) {
								actual_destination = 7;
							} else if (destination.equals("R9")) {
								actual_destination = 8;
							} else if (destination.equals("R10")) {
								actual_destination = 9;
							} else if (destination.equals("R11")) {
								actual_destination = 10;
							} else if (destination.equals("R12")) {
								actual_destination = 11;
							} else if (destination.equals("R13")) {
								actual_destination = 12;
							} else if (destination.equals("R14")) {
								actual_destination = 13;
							} else if (destination.equals("R15")) {
								actual_destination = 14;
							}

							int next = t.next_hop[actual_destination];
							if (next == selfint) {
								actual_destination = actual_destination + 1;
								nexthop = "R" + Integer.toString(actual_destination);
								break;
							}
							next = next + 1;
							nexthop = "R" + Integer.toString(next);

							if (nDB.m.containsKey(nexthop)) {

								break;
							} else {
								destination = nexthop;
							}
						}

						int nextport = ftpPort.get(nexthop);
						System.out.println("\nFor the destination " + destination + " the next hop is " + nexthop + ".");
						System.out.println("Establishing a connection to " + nexthop);
						fSocket = new Socket(nDB.m.get(nexthop).get(0), nextport);
						DataOutputStream out = new DataOutputStream(fSocket.getOutputStream());
						byte[] buf = new byte[MTU];
						try {
							out.writeInt(FILE_SIZE + 1);
							out.flush();
							out.writeUTF(destination);
							out.flush();
							out.writeUTF(FILE_NAME);
							out.flush();
							int len = 0;
							while ((len = is.read(buf)) > 0) {
								out.write(buf, 0, len);
							}

							out.flush();
							out.close();
						} catch (IOException e) {

						}
						System.out.println("\nFile is forwarded to the destination " + destination
								+ " through the next hop " + nexthop + ".");
					}
				} finally {
					if (fSocket != null)
						fSocket.close();
					if (bos != null)
						bos.close();
					if (fos != null)
						fos.close();
					if (sock != null)
						sock.close();

				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

class nPacket implements Serializable {

	String ptype; // type of message
	String source_routerID; // Source Router
	int seqNum;
	int len;
	int numLinks;
	Map<String, ArrayList<String>> lsa; // LSA Database
	String advRouter; // originator of the LSA
	int errorFlag;
	int aliveInterval;
	int updateInterval;
	int pVersion;
	byte[] thedigest;

	public nPacket(String ptype, String routerID, int aliveInterval, int updateInterval, int pVersion) {

		this.ptype = ptype;
		this.source_routerID = routerID;
		this.aliveInterval = aliveInterval;
		this.updateInterval = updateInterval;
		this.pVersion = pVersion;

	}

	public nPacket(String ptype, String routerID) {

		this.ptype = ptype;
		this.source_routerID = routerID;

	}

	public nPacket(Map<String, ArrayList<String>> lsa, String ptype, String source_routerID, int len, int numLinks,
			String advRouter, byte[] thedigest) {
		this.ptype = ptype;

		this.lsa = lsa;
		this.numLinks = numLinks;
		this.advRouter = advRouter; // original LSA originating Router
		this.source_routerID = source_routerID; // forwarding router
		this.len = len;
		this.thedigest = thedigest;

	}

	void displayPacket() {
		System.out.println("\nPacket Type: " + this.ptype + " Packet Source: " + this.source_routerID);

	}

}

class Packet implements Serializable {
	String selfID;
	String IP;
	String cport_s;
	String fport_s;
	String neighborID;
	String ack;

	public Packet(String selfID, String IP, String cport_s, String fport_s) {
		this.selfID = selfID;
		this.IP = IP;
		this.cport_s = cport_s;
		this.fport_s = fport_s;
	}

	public Packet(String selfID, String neighborID) {
		this.selfID = selfID;
		this.neighborID = neighborID;
	}

	public Packet(String ack) {
		this.ack = ack;
	}
}
