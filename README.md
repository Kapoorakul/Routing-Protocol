# Routing-Protocol
Link State Routing Protocol using DNS Servers and Dijkstra's Algorithm

1. Problem Statement

The purpose of the project is to design a simple Link-State Routing Protocol (sLSRP). We will work on typical network topologies and determine the shortest path from one node to all the other nodes present in the network. The shortest path from a node to all the other nodes will be stored in a link state database on each router and this information is reliably distributed to all the other routers in the form of Link State Advertisements. Going further we shall discuss as to how each functionality requested in the problem description will be implemented and an approach to achieve the desired goal.

2. Router configuration

Routers will be configured using the configuration file and the policy management file given by the System Administrator. The router will parse the file for various parameters like:

	Router ID

	List of Neighbors

	Protocol Version

Every router in the given topology will have its own IP address. To simplify the program implementation we will be using a different port for each thread that is running in the code. For example, the thread to establish neighborship will establish sockets on port 2000 and the thread to send periodic LSA’s will establish sockets on port 2001 and so on. Therefore each router will be listening on multiple ports.

The major issue that could arise when using sockets is that, the port which we have decided to use in our code could be used by someone else as well. To alleviate this problem, we will ask the machine itself to provide us its unused ports and we will use those to establish connections.

	We will be running a small program on each of the routers which will give us the IP address and the available port on that machine.
 

	The same program will be run on a Domain Name Server and the IP address and the generated port number will be stored in a file named “DNS.txt” directory (AFS) which is accessible by all the routers.

	Each router accesses the DNS.txt file stored in the AFS and connects to the DNS server by establishing a socket to the IP address and the port number present in the file. Once a router connects to the DNS server it will register its own IP address and the multiple port numbers for each type of thread.

	The DNS server will maintain a database of the IP address and port number of each router and classify it based on the thread type. Example is shown below.
 

Thread – Neighborship

Router ID	IP address	Port Number
R1	192.168.1.1	1000
R2	192.168.1.2	2000
R3	192.168.1.3	3000
 

Thread - Keepalive

Router ID	IP address	Port Number
R1	192.168.1.1	1001
R2	192.168.1.2	2001
R3	192.168.1.3	3001
 

	When a particular router wants to connect to its neighbor router, it will obtain the IP address of the neighbor router and the respective port (thread) it wants to connect to from the DNS server and create a socket to carry out the operations such as sending neighborship message or a keepalive message.
 
3. User Interface


Following are the options given on the screen of the router. Once the administrator selects the option, the corresponding code runs to give the desired output. A help menu will be provided at each step which tells the user as to what information needs to be entered. Example shown below.

#Enter 1 to view the routing table

#Enter 2 to view the shortest path from this node to all the nodes #Enter 3 to send a file to a remote node

#Enter 4 to view the neighborship table #Enter 5 to change the configuration

If user choice is 1 then a method is called which displays the current routing table of the router.

Node	Next Hop Node
R2	R1
R3	R2
R4	R2
R5	R1

If the user choice is 3, then we will ask further information such as the destination node, file name etc. and initiate the FTP file transfer.

#What is the destination node?

#Enter the file name and the location

Details on the file transfer protocol is given later in the report.

If the user choice is 5 and he wishes to change the configuration, then we will further obtain information as to what information the user wants to change i.e. the neighborship, update interval, Keepalive interval, protocol version etc.

#Press 1 if you would like to change the protocol version

#Press 2 if you would like to change the neighborship

Depending on the choice a particular method will be called which carries out the appropriate actions.

4. Policy Management File

Every router will maintain a policy management file. This file will consist the router ID's which are whitelisted and blacklisted. Each router will have its own whitelist and blacklist router ID's and the neighborship messages are acted upon by parsing this file. The file will look similar to as shown below.

Whitelist

R2

R3
 
Blacklist

R5

R6

This information contained in this file will be ensured that it is synchronous when compared to the policy management file of another router. There shouldn’t be a mismatch in the file such as Router_2 is listed as whitelisted on Router_1 but Router_1 is blacklisted in Router_2's policy management file. If such a case exists neighborship cannot be formed.

5. Data Structures

Hash-Map:

HashMap can be synchronized by using synchronizedMap (HashMap) method. So every modification is performed on Map is locked on Map object thus achieving atomic operations on the database. By using the synchronization we are avoiding multiple threads to write to the same database and a thread reading the database while another thread is writing to it.

Neighborship Database: We are proposing to use Hash-Map to store the neighboring Router-ID’s as the keys and array-list to store multiple values representing Acknowledgement and Counter.

Neighbor_Alive Database: We are proposing to use Hash-Map to store neighboring Router-ID’s as the keys and array-list to store Neighbor Router-ID, Acknowledgement and Counter.

LSA Database: We are proposing to use Hash-Map to store the Link ID as the key and the array-list to store cost, Acknowledgement, counter and Sequence Number.

Queue: The router listening on multiple ports will enqueue the packets in the queue and subsequently dequeued till the queue is empty. Each packet is handled based on the port it is received on.

6. Threads

Main() Thread: The main program will start by parsing the router configuration file. After that the main() will obtain the router’s self IP address and generates port number for the various connections. After the connection is made to the DNS server, the router will registers its Router-ID, IP address, port number and the connection type i.e. neighborship, neighbor_alive, delay, LSA and FTP.
 

Neighbor Acquisition: Router will fetch neighboring Router ID’s from the configuration file and compare them against the policy management file to check whether it is a whitelisted neighbor. If not it is not inserted in the database. After that we will create the socket (the socket is created as described above by querying DNS with the Neighbor Router-ID and the packet type to obtain the IP address of the neighbor router and the port number) to send out Be_Neighbor_Request packet. After sending the request packet the main thread will sleep, while waiting for the acknowledgement from the neighbor. The neighbor on the receipt of Be_Neighbor_Request packet will check its policy management file to check whether the request is coming from the white-listed router or not, then reply to the request accordingly with either accept or reject. The neighbor router will create the socket by querying the DNS to obtain the IP address and the port number of the neighborship requesting router. If the neighborship request is accepted, the neighbor router id is written to Neighbor_Alive database and LSA database is also updated with the corresponding Link ID. We will track the acknowledgement of the request by using the Acknowledgement Flag and the Counter (declaring neighbor unreachable after 3 request packet). We will keep running this porting of the code for case where the policy management file changes and the previously blacklisted router is made white listed. In that case the router will try to acquire that neighbor.

Neighbor_Alive Thread: After the neighborship is formed the Neighbor_Alive thread is started. The sockets are created for each neighbor in the Neighbor_alive database and the hello packets are sent. After sending the hello packets the thread sleeps and then wakes up again to check whether it has receive acknowledgement or not. The value of the hello timers are given in the configuration file. The thread waits for 3 unacknowledged hello packets before declaring the neighbor unreachable. If the neighbor becomes unreachable, the neighbor router id is removed from the neighbor_alive database and the link is removed from the LSA database. Thus, as soon as a neighbor is acquired the Keep_Alive database will get the Neighbor Router-ID and the keep alive thread will start sending out the keep alive messages periodically over the sockets.

Delay Thread: The thread will be used to calculate the cost of the link connecting the router to its neighbor. It will create a socket to send a small packet over to the neighbor with a timestamp and the neighbor will acknowledge with the receipt timestamp. Using the difference of the sent and receipt timestamp the delay can be calculated. There could be a possibility that the delay calculated from Router 1 to Router 2 be different from the delay between Router 2 and Router 1. Hence to maintain consistency across the router databases we will estimate the link depending on a range on a delay. For example, if the delay is between 0 and 20ms then the link cost is 20. If the delay is between 20 and 50ms then the cost of the link is 50. The range is just an example and could change depending on our research on the typical delay values between the machines. We are not taking care of tracking the acknowledgement of the packets. In order to calculate the delay as we are sending out these packets every 15 seconds and averaging out over the period of 180 seconds to calculate the Cost of the link. We update the cost in the LSA database for the neighbor pair.

LSA Thread: The cost to a router is informed to all the other routers in the network by using reliable flooding. The LSA’s are sent after every update interval (180 seconds) and the sent flag is set in the database. Router upon receiving the LSA will compare the sequence number stored in the LSA database to check whether it is a new packet or an old one. If the sequence number is a higher one then it will update the cost, send an acknowledgement to the source router and forwards the LSA packet on all its
 

interfaces except the interface on which it received the LSA. The forwarded LSA packet will have the same data but updated source router id and CRC. If the sequence number is old then the router will not update the cost and it will not forward the packet again as it is an old LSA but it will acknowledge the source router which sent the LSA to avoid receiveing similar LSA packets. Once we have the cost between all the neighbors in the network, we can use it to create an NxN matrix. This matrix will be given as the input to the Dijkstra’s Algorithm.

Dijkstra’s Algorithm: The Dijkstra’s algorithm will be used to calculate the shortest paths from a node to all the other nodes in the topology. The input to the algorithm will be a matrix containing integer values of link cost from every router to its neighbor. A sample link cost table is shown below.


	R1	R2	R3	R4	R5
R1	0	10	0	15	20
R2	10	0	25	0	15
R3	0	25	0	15	30
R4	15	0	15	0	10
R5	20	15	30	10	0

The output of the Dijkstra’s algorithm will be the least cost to reach each of the remaining nodes through a shortest path present in the topology. The Dijkstra’s function will also provide us the next hop node to reach a distant router which effectively is our routing table.

FTP Thread: After the user enters the destination node and the file name, the router will check whether the file given by the user is valid or not (may be not present). If the file is valid then, it will use the Routing table to check the next-hop Router ID. After the next hop router id is known, then the router will create the socket to the destination machine using DNS and send the file encapsulated in the packet (object) with the source router-id and destination router-id over to the next router. The next hop router will check the packet and make a decision whether the file sent is for self or it has to again forward the file to the next hop using the routing table and so on to finally reach its destination.

7. CRC and Congestion Simulation

Before sending any packet, a CRC value is calculated and appended to the packet. To simulate the packet error mechanism, we will generate a random number between 1 and 100. If the number happens to be between 95 and 100 then a bit is changed in the packet and then sent. The router which receives the packet, calculates its own CRC and once it observes that the CRC does not match it discards the packet. If the number is between 1 and 94 then the packet is sent with its original unmodified content.

Similarly to simulate the congestion, we will generate a random number between 1 and 100. If the number happens to be between 95 and 100 it is assumed that there high congestion in the link and the packet is dropped. Else the packet is forwarded to the intended destination.

We have chosen a small range i.e. between 95 and 100 to introduce packet errors or packet drops. This is done to avoid frequent packet errors and packet drops. The range might be further tweaked if it is
 

observed that packets are frequently dropped or a lot of errors are caused. The goal is to have the shortest path determined with occasional packet drops and errors.


8. Use Case Scenarios

There are various scenarios that can arise when the program is running on all the routers which can cause instabilities in the network. We have come up with few use case scenarios which will describe how our code will handle each situation and act accordingly.

Router goes down: Every router will be sending keepalive packets to each of its neighbor. When a router goes down, the keepalives sent by its neighbor will timeout. The router which is sending the keepalives will retry for some time and if no response is received then it declares that its neighbor is dead. The link cost to that neighbor is changed to a high value (infinity) and the neighborship is ceased. The corresponding entries for that link in LSA table are updated. The entries for the router which went down are changed to false in the neighborship table and deleted from the neighbor_alive table. When the router comes back up, it will send neighborship messages and the neighborship is established again.

Link goes down: When a link goes down, the keepalives will fail to a neighbor router. The routers connected to that link will notice that the keepalives are failing and hence change the link cost to a high value (after 3 keepalives fail). The neighborship to that router will be changed to false and the LSA table is updated with the new cost and the entry for that router is removed from the neighbor_alive table. The LSA thread will also disseminate this information in the network to make all the routers aware of the link failure. The neighborship thread which runs periodically will check if the neighborship to any particular whitelisted router is false. Since the neighborship to this router is false, then the neighborship messages are sent periodically. If the link comes back up again after some time, the neighborship will be formed again and the entry for this router is added in the neighbor_alive table. The link cost is calculated and the LSA table is updated and the normal process continues.

Router goes down and comes back up with a different IP address: There is a possibility that when a router goes down and comes back up, it can have a different IP address. Irrespective of the IP address, the router will go and register its new IP address and the new port numbers with the DNS server. Any other router who wishes to connect with this router now will contact the DNS server and obtain the new IP address and the port number.

Administrator changes the policy management file: In the command line interface, we provide the option to the administrator to change the policy management file i.e. to change the whitelisted and the blacklisted routers. If the administrator removes a particular router from the whitelist and adds it to the blacklist, then the periodically running neighborship thread will check the policy management file and notice that a router to which neighborship was established, is not a neighbor anymore. Hence it changes the neighborship to false. Any neighborship messages received from the router which was blacklisted, will be rejected. The entry for the router is deleted from the neighbor_alive table and the LSA table is also updated accordingly.
 
