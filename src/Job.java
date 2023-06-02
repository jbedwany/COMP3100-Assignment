public class Job {
    public int time;
    public int id;
    public int runtimeEst;
    public int cores;
    public int mem;
    public int disk;
    public int queuePosition;

    public Job(String job) {
        this.time = Integer.parseInt(job.split(" ")[1]);
        this.id = Integer.parseInt(job.split(" ")[2]);
        this.runtimeEst = Integer.parseInt(job.split(" ")[3]);
        this.cores = Integer.parseInt(job.split(" ")[4]);
        this.mem = Integer.parseInt(job.split(" ")[5]);
        this.disk = Integer.parseInt(job.split(" ")[6]);
        this.queuePosition = 0;
    }

    public Job() {
        this.time = 0;
        this.id = 0;
        this.runtimeEst = 0;
        this.cores = 0;
        this.mem = 0;
        this.disk = 0;
        this.queuePosition = 0;
    }

    public String toString() {
        return "Time: " + time + ", ID: " + id + ", Runtime: " + runtimeEst + ", Cores: " + cores + ", Memory: " + mem + ", Disk: " + disk;
    }
}
