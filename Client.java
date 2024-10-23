package Lab03;
import java.rmi.Naming;

public class Client {
    public static void main(String[] args) {
        try {
            // MutexManager manager = (MutexManager) Naming.lookup("rmi://localhost/MutexManager");

            ProcessInterface process0 = (ProcessInterface) Naming.lookup("rmi://localhost/Process0");
            ProcessInterface process1 = (ProcessInterface) Naming.lookup("rmi://localhost/Process1");
            ProcessInterface process2 = (ProcessInterface) Naming.lookup("rmi://localhost/Process2");
            // ProcessInterface[] processList = {process0, process1, process2};

            // manager.addProcesses(processList);
            
            process0.requestCriticalSection();
            process1.requestCriticalSection();
            process2.requestCriticalSection();
            process0.releaseCriticalSection();
            process2.releaseCriticalSection();
            process1.releaseCriticalSection();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}