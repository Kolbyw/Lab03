package Lab03;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.Arrays;


public class Server {
    public static void main(String[] args) {
        try{
            LocateRegistry.createRegistry(1099); // RMI on port 1099 (default)
            int numProcesses = 3;

            MutexManager manager = new MutexManager();
            ProcessInterface[] processes = new ProcessInterface[3];

            for(int x = 0; x < numProcesses; x++) {
                ProcessInterface remoteProcess = new Process(x, numProcesses, manager);
                processes[x] = remoteProcess;
                Naming.rebind("rmi://localhost/Process" + x, remoteProcess);
                System.out.println("Process " + x + " is ready");
            }
            manager.addProcesses(processes);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}

interface MutexManagerInterface extends Remote {
    public void requestEntry(int processID, VectorClock clock) throws RemoteException;
    public void releaseEntry(int processID) throws RemoteException;
}

class MutexManager implements MutexManagerInterface {
        private PriorityQueue<Request> requestQueue;
        private ProcessInterface[] processList;
        private int currProcess;

        public MutexManager(){
                this.requestQueue = new PriorityQueue<>(Comparator.comparing((Request r) -> sum(r.getClock())).thenComparing(Request::getProcessId));
                this.currProcess = -1;
        }

        public int sum(int[] array){
                int total = 0;
                for(int x = 0; x < array.length; x++){
                        total += array[x];
                }
                return(total);
        }

        @Override
        public void requestEntry(int processId, VectorClock clock) throws RemoteException{
                requestQueue.add(new Request(processId, clock));
                System.out.println("Process " + processId + " has requested critical section");
                checkQueue();
        }

        @Override
        public void releaseEntry(int processId) throws RemoteException {
                if(currProcess == processId){
                        currProcess = -1;
                        System.out.println("Process " + processId + " has been released from critical section");
                        checkQueue();
                }
        }

        public void addProcesses(ProcessInterface[] processes){
                processList = processes;
        }

        public void checkQueue() throws RemoteException {
                if(currProcess == -1 && !requestQueue.isEmpty()){
                        Request next = requestQueue.peek();
                        currProcess = next.getProcessId();
                        requestQueue.poll();
                        System.out.println("Process " + currProcess + " has been granted critical section");

                        // send reply to process using RMI
                        processList[currProcess].reply();
                }
        }
}

interface ProcessInterface extends Remote {
    public void requestCriticalSection() throws RemoteException;
    public void releaseCriticalSection() throws RemoteException;
    public VectorClockInterface getVectorClock() throws RemoteException;
    public void reply() throws RemoteException;
}

class Process extends UnicastRemoteObject implements ProcessInterface {
    private int processId;
    private VectorClock vectorClock;
    public boolean criticalSectionStatus;
    private MutexManager manager;

    public Process(int processId, int totalProcesses, MutexManager manager) throws RemoteException {
        this.processId = processId;
        this.vectorClock = new VectorClock(totalProcesses);
        this.criticalSectionStatus = false;
        this.manager = manager;
    }
    
    @Override
    public void requestCriticalSection() throws RemoteException {
        this.vectorClock.increment(processId);    
        this.manager.requestEntry(processId, vectorClock);  
    }
    @Override
    public void releaseCriticalSection() throws RemoteException {
        this.criticalSectionStatus = false;
        this.manager.releaseEntry(processId);
    }
    @Override
    public VectorClockInterface getVectorClock() throws RemoteException {
        return this.vectorClock;
    }
    @Override
    public void reply(){
        // in critical section
        System.out.println("Process " + processId + " is in critical section.");
    }
    public int getId(){
        return this.processId;
    }
}

class Request {
    private int processId;       
    private VectorClock clock;   

    public Request(int processId, VectorClock clock) {
        this.processId = processId;
        this.clock = clock;
    }

    public int getProcessId() {
        return processId;
    }

    public int[] getClock() {
        return clock.getClock();
    }
}

interface VectorClockInterface extends Serializable {
    public int[] getClock(); // Returns internal clock array
    public void increment(int processId); // increment value of clock at processId
    public void update(VectorClock clock); // Compares local clock with remote clock and update with max
    public String toString(); // Returns clock value as string
}

class VectorClock implements VectorClockInterface {
    private int[] clock;

    public VectorClock(int numProcesses) {
        clock = new int[numProcesses];
    }

    @Override 
    public synchronized int[] getClock() {
        return clock.clone();
    }
    @Override
    public synchronized void increment(int processId) {
        clock[processId]++;
    }
    @Override
    public synchronized void update(VectorClock remoteClock) {
        int[] newClock = remoteClock.getClock();
        for(int x = 0; x < clock.length; x++) {
            clock[x] = Math.max(clock[x], newClock[x]);
        }
    }
    @Override
    public String toString() {
        return Arrays.toString(clock);
    }
}