import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) {
        // basic usage checking
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

            // Main job scheduling functionality - optimising turnaround time without sacrificing other metrics too much
            while(!received.substring(0,4).equals("NONE")) {
                switch(received.substring(0,4)) {
                    case "JOBN", "JOBP":
                        Job currentJob = new Job(received);
                        scheduleOTT(fromServer, toServer, currentJob);
                        break;
                    case "JCPL", "CHKQ":
                        sendMessage(toServer, "LSTQ GQ *");
                        received = receiveMessage(fromServer);
                        int numJobs = Integer.parseInt(received.split(" ")[1]);
                        sendMessage(toServer, "OK");
                        if(numJobs > 0) {
                            ArrayList<Job> queuedJobs = new ArrayList<Job>();
                            for(int i = 0; i < numJobs; i++) {
                                String[] job = receiveMessage(fromServer).split(" ");
                                Job queuedJob = new Job();
                                queuedJob.cores = Integer.parseInt(job[5]);
                                queuedJob.disk = Integer.parseInt(job[7]);
                                queuedJob.id = Integer.parseInt(job[0]);
                                queuedJob.mem = Integer.parseInt(job[6]);
                                queuedJob.queuePosition = i;
                                queuedJobs.add(queuedJob);
                            }
                            sendMessage(toServer, "OK");
                            receiveMessage(fromServer);
                            // Request list of servers available to complete job
                            queuedJobs.forEach((job) -> {
                                sendMessage(toServer, "GETS Available " + job.cores + " " + job.mem + " " + job.disk);
                                String message = receiveMessage(fromServer);
                                int numRecords = Integer.parseInt(message.split(" ")[1]);
                                sendMessage(toServer, "OK");

                                if(numRecords > 0) {    // if a capable server is available
                                    // Receive server list
                                    message = receiveMessage(fromServer);

                                    // Evaluate best suited server, testing the first one separately to avoid issues with integer initialisation (i.e. initialising fitness and bestfitness to arbitrary nonzero numbers)
                                    int fitness = Integer.parseInt(message.split(" ")[4]) - job.cores;
                                    int bestFitness = fitness;
                                    Server bestServer = new Server(message);

                                    for(int j = 0; j < numRecords - 1; j++) {
                                        message = receiveMessage(fromServer);  // get details of each server
                                        fitness = Integer.parseInt(message.split(" ")[4]) - job.cores;
                                        if(fitness < bestFitness) {
                                            bestFitness = fitness;
                                            bestServer = new Server(message);
                                        }
                                    }
                                    sendMessage(toServer, "OK");
                                    receiveMessage(fromServer);
                                    
                                    // Dequeue job
                                    sendMessage(toServer, "DEQJ GQ " + job.queuePosition);
                                    receiveMessage(fromServer);

                                    // Schedule job
                                    sendMessage(toServer, "SCHD " + job.id + " " + bestServer.type + " " + bestServer.id);
                                    receiveMessage(fromServer);
                                } else {
                                    receiveMessage(fromServer);
                                }
                            });
                        } else {
                            receiveMessage(fromServer);
                        }
                        break;
                    default:
                        System.out.println("Command received: " + received.substring(0,4));
                        break;
                }
                sendMessage(toServer, "REDY");
                received = receiveMessage(fromServer); 
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
            // System.out.println("SENT: " + message);
        } catch (IOException e) {
            System.out.println("Exception when sending message " + message + ": " + e);
        }
    }

    static String receiveMessage(BufferedReader fromServer) {
        try {
            String message = fromServer.readLine();
            // System.out.println("RCVD: " + message);
            return message;
        } catch (IOException e) {
            System.out.println("Exception when reading message: " + e);
            return null;
        }
    }
    
    static void scheduleOTT(BufferedReader fromServer, DataOutputStream toServer, Job currentJob) {
        // Request list of servers available to complete job
        sendMessage(toServer, "GETS Available " + currentJob.cores + " " + currentJob.mem + " " + currentJob.disk);
        int numRecords = Integer.parseInt(receiveMessage(fromServer).split(" ")[1]);
        sendMessage(toServer, "OK");

        if(numRecords > 0) {    // if a capable server is available
            // Receive server list
            String received = receiveMessage(fromServer);

            // Evaluate best suited server, testing the first one separately to avoid issues with integer initialisation (i.e. initialising fitness and bestfitness to arbitrary nonzero numbers)
            int fitness = Integer.parseInt(received.split(" ")[4]) - currentJob.cores;
            int bestFitness = fitness;
            Server bestServer = new Server(received);

            for(int i = 0; i < numRecords - 1; i++) {
                received = receiveMessage(fromServer);  // get details of each server
                fitness = Integer.parseInt(received.split(" ")[4]) - currentJob.cores;
                if(fitness < bestFitness) {
                    bestFitness = fitness;
                    bestServer = new Server(received);
                }
            }
            sendMessage(toServer, "OK");
            receiveMessage(fromServer);

            // Schedule job
            sendMessage(toServer, "SCHD " + currentJob.id + " " + bestServer.type + " " + bestServer.id);
            receiveMessage(fromServer);
        } else {
            receiveMessage(fromServer);
            sendMessage(toServer, "ENQJ GQ");
            receiveMessage(fromServer);
        }
    }
}
