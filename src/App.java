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
                    case "JOBN":
                        Job currentJob = new Job(received);
                        scheduleOTT(fromServer, toServer, currentJob);
                        break;
                    case "JCPL":
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
                                }
                            });
                        } else {
                            receiveMessage(fromServer);
                        }
                        break;
                    case "CHKQ":
                        sendMessage(toServer, "LSTQ GQ *");
                        receiveMessage(fromServer);
                        return;
                    default:
                        System.out.println("Command received: " + received.substring(0,4));
                        break;
                }
                sendMessage(toServer, "REDY");
                received = receiveMessage(fromServer); 
            }

            // while(!received.substring(0,4).equals("NONE")) {
            //     if(received.substring(0,4).equals("JOBN")) {
            //         Job currentJob = new Job(received);
            //         switch(mode) {
            //             case "lrr":
            //                 // Get server details, find largest type
            //                 if(largestServers == null) {
            //                     largestServers = findLargestServer(toServer, fromServer);
            //                 }
            //                 roundRobinIndex = scheduleLRR(largestServers, fromServer, toServer, roundRobinIndex, currentJob);
            //                 // Request next job command
            //                 sendMessage(toServer, "REDY");
            //                 received = receiveMessage(fromServer);
            //                 break;
            //             case "fc":
            //                 scheduleFC(fromServer, toServer, currentJob);
            //                 sendMessage(toServer, "REDY");
            //                 received = receiveMessage(fromServer);
            //                 break;
            //             case "ott":
            //                 jobsInProgress.add(currentJob);
            //                 scheduleOTT(fromServer, toServer, currentJob);

            //                 break;
            //             default:
            //                 System.out.println("Error with mode selection - mode " + mode + " not found");
            //                 return;
            //         }
            //     } else if(received.substring(0,4).equals("JCPL")) {
            //         sendMessage(toServer, "REDY");
            //         received = receiveMessage(fromServer); 
            //     }
            // }

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

    // static Server[] findLargestServer(DataOutputStream toServer, BufferedReader fromServer) {
    //     sendMessage(toServer, "GETS All");
    //     String received = receiveMessage(fromServer);
    //     int numServers = Integer.parseInt(received.split(" ")[1]);
    //     Server[] servers = new Server[numServers];
    //     sendMessage(toServer, "OK");
    //     // System.out.println("Finding largest server.");
    //     int maxCores = 0;
    //     String largestServer = "";
    //     int numLargest = 0;
        
    //     // Identify largest type
    //     for(int i = 0; i < numServers; i++) {
    //         received = receiveMessage(fromServer);
    //         servers[i] = new Server(received);
    //         if(servers[i].cores > maxCores && servers[i].state.equals("inactive")) {
    //             maxCores = servers[i].cores; 
    //             largestServer = servers[i].type; 
    //             numLargest = 0;
    //         }
    //         if(servers[i].type.equals(largestServer)) {
    //             numLargest++;
    //         }
    //     }
    //     // Create list of largest servers
    //     Server[] largestServers = new Server[numLargest];
    //     int largestServersArrayIndex = 0;                   // added to avoid looping through the new array to find the next empty element each time an element is being added
    //     for(int i = 0; i < numServers; i++) {
    //         if(servers[i].type.equals(largestServer) && servers[i].state.equals("inactive")) {
    //             largestServers[largestServersArrayIndex++] = servers[i];
    //         }
    //     }
    //     sendMessage(toServer, "OK");
    //     receiveMessage(fromServer);
    //     return largestServers;
    // }


    // static int scheduleLRR(Server[] largestServers, BufferedReader fromServer, DataOutputStream toServer, int roundRobinIndex, Job currentJob) {
    //     // Schedule jobs using LRR
    //     sendMessage(toServer, "SCHD " + currentJob.id + " " + largestServers[roundRobinIndex].type + " " + largestServers[roundRobinIndex].id);
    //     if(++roundRobinIndex >= largestServers.length) {
    //         roundRobinIndex = 0;
    //     }
    //     receiveMessage(fromServer);
    //     return roundRobinIndex;
    // }

    // static void scheduleFC(BufferedReader fromServer, DataOutputStream toServer, Job currentJob) {
    //         sendMessage(toServer, "GETS Capable " + currentJob.cores + " " + currentJob.mem + " " + currentJob.disk);
    //         int numRecords = Integer.parseInt(receiveMessage(fromServer).split(" ")[1]);
    //         sendMessage(toServer, "OK");
    //         String received = receiveMessage(fromServer);
    //         System.out.println(received);
    //         for(int i = 0; i < numRecords - 1; i++) {
    //             receiveMessage(fromServer);
    //         }
    //         sendMessage(toServer, "OK");
    //         receiveMessage(fromServer);
    //         Server firstCapable = new Server(received);
    //         sendMessage(toServer, "SCHD " + currentJob.id + " " + firstCapable.type + " " + firstCapable.id);
    //         receiveMessage(fromServer);
    // }
    
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
