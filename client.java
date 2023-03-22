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

            // initialise connection
            try {
                toServer.write(("HELO\n").getBytes());
                System.out.println("SENT: HELO");
                toServer.flush();
                String received = fromServer.readLine();
                System.out.println("RCVD: " + received);
                if(!received.equals("OK")) {
                    System.out.println("Unexpected response from server. Quitting.");
                    socket.close();
                    return;
                }

                toServer.write(("AUTH " + user + "\n").getBytes());
                System.out.println("SENT: AUTH " + user);
                toServer.flush();
                received = fromServer.readLine();
                System.out.println("RCVD: " + received);
                if(!received.equals("OK")) {
                    System.out.println("Unexpected response from server. Quitting.");
                    socket.close();
                    return;
                }

            } catch(IOException e) {

            }

            socket.close();
        } catch(IOException e) {
            System.out.println("Error creating socket: " + e);
        }
    }
}
