public class Server {
    public String type;
    public int id;
    public String state;
    public int curStartTime;
    public int cores;
    public int mem;
    public int disk;
    public int wJobs;
    public int rJobs;


    public Server(String server) {
        this.type = server.split(" ")[0];
        this.id = Integer.parseInt(server.split(" ")[1]);
        this.state = server.split(" ")[2];
        this.curStartTime = Integer.parseInt(server.split(" ")[3]);
        this.cores = Integer.parseInt(server.split(" ")[4]);
        this.mem = Integer.parseInt(server.split(" ")[5]);
        this.disk = Integer.parseInt(server.split(" ")[6]);
        this.wJobs = Integer.parseInt(server.split(" ")[7]);
        this.rJobs = Integer.parseInt(server.split(" ")[8]);
    }

    public String toString() {
        return "Type: " + type + ", ID: " + id + ", State: " + state + ", Current Start Time: " + curStartTime + ", Cores: " + cores + ", Memory: " + mem + ", Disk: " + disk + ", wJobs: " + wJobs + ", rJobs: " + rJobs;
    }
}
