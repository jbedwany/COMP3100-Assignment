package src;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        // basic usage checking
        if(args.length < 3) {
            System.out.println("Usage: client.java <ipaddr> <port> <username>");
            return;
        }
        String ipaddr = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];
        String mode = "lrr";
        if(args.length == 5) {
            if("-a".equals(args[3])) {
                mode = args[4];
            }
        }
       
        System.out.println("Connecting to server at " + ipaddr + ":" + port + ".");
        try {
            Socket socket = new Socket(ipaddr, port);
            System.out.println("Connected to server.");

            BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            DataOutputStream toServer = new DataOutputStream(socket.getOutputStream());

            // Initial connection & preamble
            sendMessage(toServer, "HELO");
            String received = receiveMessage(fromServer);
    
            sendMessage(toServer, "AUTH " + user);
            received = receiveMessage(fromServer);

            sendMessage(toServer, "REDY");
            received = receiveMessage(fromServer); 

            // Main job scheduling functionality
            int roundRobinIndex = 0; 
            Server[] largestServers = null;
            while(!received.substring(0,4).equals("NONE")) {
                if(received.substring(0,4).equals("JOBN")) {
                    Job currentJob = new Job(received);
                    if("lrr".equals(mode)) {
                        scheduleLRR();
                    } else if("fc".equals(mode)) {
                        System.out.println("fc mode");
                        // scheduleFC();
                    }


                } else if(received.substring(0,4).equals("JCPL")) {
                    sendMessage(toServer, "REDY");
                    received = receiveMessage(fromServer); 
                }
            }

            // terminate connection
            sendMessage(toServer, "QUIT");
            received = receiveMessage(fromServer);
            toServer.close();
            fromServer.close();
            socket.close();
        } catch(IOException e) {
            System.out.println("Error creating socket: " + e); 
        }
    }

    // Helper functions
    static void sendMessage(DataOutputStream toServer, String message) {
        try {
            toServer.write((message + "\n").getBytes());
            toServer.flush();
            System.out.println("SENT: " + message);
        } catch (IOException e) {
            System.out.println("Exception when sending message " + message + ": " + e);
        }
    }

    static String receiveMessage(BufferedReader fromServer) {
        try {
            String message = fromServer.readLine();
            System.out.println("RCVD: " + message);
            return message;
        } catch (IOException e) {
            System.out.println("Exception when reading message: " + e);
            return null;
        }
    }

    static Server[] findLargestServer(DataOutputStream toServer, BufferedReader fromServer) {
        sendMessage(toServer, "GETS All");
        String received = receiveMessage(fromServer);
        int numServers = Integer.parseInt(received.split(" ")[1]);
        Server[] servers = new Server[numServers];
        sendMessage(toServer, "OK");
        System.out.println("Finding largest server.");
        int maxCores = 0;
        String largestServer = "";
        int numLargest = 0;
        
        // Identify largest type
        for(int i = 0; i < numServers; i++) {
            received = receiveMessage(fromServer);
            servers[i] = new Server(received);
            if(servers[i].cores > maxCores && servers[i].state.equals("inactive")) {
                maxCores = servers[i].cores; 
                largestServer = servers[i].type; 
                numLargest = 0;
            }
            if(servers[i].type.equals(largestServer)) {
                numLargest++;
            }
        }
        // Create list of largest servers
        Server[] largestServers = new Server[numLargest];
        int largestServersArrayIndex = 0;                   // added to avoid looping through the new array to find the next empty element each time an element is being added
        for(int i = 0; i < numServers; i++) {
            if(servers[i].type.equals(largestServer) && servers[i].state.equals("inactive")) {
                largestServers[largestServersArrayIndex++] = servers[i];
            }
        }
        sendMessage(toServer, "OK");
        receiveMessage(fromServer);
        return largestServers;
    }


    static void scheduleLRR() {
        // Get server details, find largest type
        if(largestServers == null) {
            largestServers = findLargestServer(toServer, fromServer);
        }
        // Schedule jobs using LRR
        try {
            sendMessage(toServer, "SCHD " + currentJob.id + " " + largestServers[roundRobinIndex].type + " " + largestServers[roundRobinIndex].id);
            if(++roundRobinIndex >= largestServers.length) {
                roundRobinIndex = 0;
            }
            received = receiveMessage(fromServer);
        } catch(NullPointerException e) {
            System.out.println("Something went wrong with identifying the largest server type: " + e);
            break;
        }
        // Request next job command
        sendMessage(toServer, "REDY");
        received = receiveMessage(fromServer); 
    }

    static void scheduleFC() {
            sendMessage(toServer, "GETS Capable " + currentJob.cores + " " + currentJob.mem + " " + currentJob.disk);
            receiveMessage(fromServer);
            sendMessage(toServer, "OK");
            received = receiveMessage(fromServer);
            fromServer.flush();
            sendMessage(toServer, "OK");
            Server firstCapable = new Server(received);
            sendMessage(toServer, "SCHD " + currentJob.id + " " + firstCapable.type + " " + firstCapable.id);
            receiveMessage(fromServer);
    }

}
