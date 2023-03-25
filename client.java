package src;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class client {

    public static void main(String[] args) {
        // basic usage checking; make this more robust in future
        if(args.length < 3) {
            System.out.println("Usage: client.java <ipaddr> <port> <username>");
            return;
        }

        String ipaddr = args[0];
        int port = Integer.parseInt(args[1]);
        String user = args[2];
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
            Job currentJob = new Job(received);

            // Get details about existing servers
            sendMessage(toServer, "GETS All");
            received = receiveMessage(fromServer);
            int numServers = Integer.parseInt(received.split(" ")[1]);
            Server[] servers = new Server[numServers];
            System.out.println("Number of servers: " + numServers);
            sendMessage(toServer, "OK");
            String largestServer = "";
            for(int i = 0; i < numServers; i++) {
                int maxCores = 0;
                received = receiveMessage(fromServer);
                servers[i] = new Server(received);
                if(servers[i].cores > maxCores && servers[i].state.equals("inactive")) {
                    maxCores = servers[i].cores; 
                    largestServer = servers[i].type; 
                }
            }
            sendMessage(toServer, "OK");
            System.out.println("Largest server type: " + largestServer);


            for(int i = 0; i < servers.length; i++) {
                if(currentJob.cores > servers[i].cores || currentJob.mem > servers[i].mem || currentJob.disk > servers[i].disk) {
                    continue;
                }
                else {
                    sendMessage(toServer, "SCHD " + currentJob.id + " " + servers[i].type);
                    received = receiveMessage(fromServer);
                    break;
                }
            }

            if(!received.equals("OK")) {
                sendMessage(toServer, "QUIT");
                received = receiveMessage(fromServer);
            }


            toServer.close();
            fromServer.close();
            socket.close();
        } catch(IOException e) {
            System.out.println("Error creating socket: " + e);
        }
    }

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
}
