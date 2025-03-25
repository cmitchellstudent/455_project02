import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
//begin code changes by Martin Cook C00102798
public class TaskThree implements Runnable{
    
    private int num; //thread number
    private int domain; //domain number

    private static List<LinkedList<String>> container; //array list of access linked lists
    private static int m; //files
    private static int n; //domains
    private static Random rand = new Random(); //random number generator
    private static Semaphore[] sems; //semaphore array for file semaphores
    
    public static void main(Integer[] args) //just learned that you can use Integer[] args instead of String[] args
    {

        // N domains, M files
        m = args[0]; //files
        n = args[1]; //domains
        
        container = new ArrayList<>(); //create list of linked lists




        //randomly assign access values
        for (int j = 0; j<n;j++)
        {
            LinkedList<String> row = new LinkedList<>(); //create linked list

            for (int i = 0; i < m + n; i++) //populate linked list, h is inserted as a index reference of access. the linked list not containing the index means access is denied
            {
                rand = new Random();
                boolean rBool = rand.nextBoolean();
                boolean wBool = rand.nextBoolean();
                if (i < m) {
                    if (rBool && wBool) {
                        row.add("R/W");
                    } else if (rBool && !wBool) {
                        row.add("R");
                    } else if (wBool && !rBool) {
                        row.add("W");
                    } else {
                        row.add(" ");
                    }
                } else {
                    if (rBool) {
                        row.add("allow ");
                    } else {
                        row.add(" ");
                    }
                }
                if (i == j + m) {
                    row.set(i, "N/A");
                }
            }
            container.add(row); //add linked list to list
        }

        System.out.println();


        printList(); //print linked list of access


        sems = new Semaphore[m]; //create semaphore array for files

        //populates sem array for each file
        for (int i = 0; i < m; i++)
        {
            sems[i] = new Semaphore(1);
        }


        //creates and starts threads
        for (int i = 0; i < n; i++)
        {
            TaskThree t = new TaskThree(i);
            Thread t1 = new Thread(t);
            t1.start();
        }


        //end of main
    }

    public boolean arbitrateLinkedList(LinkedList<String> list, int index, String lookingFor)
    {
        String access = list.get(index);
        if(access.startsWith(lookingFor))
        {
            return true;
        }
        return false;
    }


    public TaskThree(int num)
    {
        this.num = num; //assign thread number
        this.domain = num; //assign domain
    }

    private String getThreadInfo()
    {
        return "Thread " + num + " (D" + domain + ")";
    }

    @Override
    public void run()
    {

        for (int i = 0; i < 5; i++) //5 requests
        {
            
            int target = rand.nextInt(0, m+n); //random target

            while(target-m == domain) //if target is the same as current domain, get new target
            {
                target = rand.nextInt(0, m+n);
            }

            //if target is a file
            if (target < m)
            {
                boolean rBool = rand.nextBoolean();
                if(rBool){
                    System.out.println(getThreadInfo() + " is trying to read file " + target);
                    boolean readAccess = arbitrateLinkedList(container.get(domain), target, "R");
                    if (readAccess){
                        System.out.println(getThreadInfo() + " has read access to file " + target);
                        sems[target].acquireUninterruptibly();                                            //acquire semaphore
                        System.out.println(getThreadInfo() + " is reading file " + target);

                        //yields
                        int cycles = rand.nextInt(3,8);
                        System.out.println(getThreadInfo() + " is yielding " + cycles + " times");
                        for(int u = 0; u < cycles; u++)
                        {
                            Thread.yield();                                                               //yield during semaphore access
                        }

                        System.out.println(getThreadInfo() + " is releasing file " + target);
                        sems[target].release();
                    }
                } else {
                    System.out.println(getThreadInfo() + " is trying to write to file " + target);
                    boolean writeAccess = arbitrateLinkedList(container.get(domain), target, "W");
                    if (writeAccess){
                        System.out.println(getThreadInfo() + " has write access to file " + target);
                        sems[target].acquireUninterruptibly();                                            //acquire semaphore
                        System.out.println(getThreadInfo() + " is writing to file " + target);

                        //yields
                        int cycles = rand.nextInt(3,8);
                        System.out.println(getThreadInfo() + " is yielding " + cycles + " times");
                        for(int u = 0; u < cycles; u++)
                        {
                            Thread.yield();                                                               //yield during semaphore access
                        }

                        System.out.println(getThreadInfo() + " is releasing file " + target);
                        sems[target].release();
                    }
                }
            }
            else //if target is a domain
            {
                System.out.println(getThreadInfo() + " is trying to switch to domain " + (target-m));
                boolean switchAccess = arbitrateLinkedList(container.get(domain), target, "allow");
                if (switchAccess)
                {
                    System.out.println(getThreadInfo() + " has access to domain " + (target-m));
                    this.domain = target - m;
                    System.out.println(getThreadInfo() +  " has switched to domain " + (target-m));
                }
                else
                {
                    System.out.println(getThreadInfo() + " is denied access and cannot switch to domain " + (target-m));
                }
            }

            
        }
    }


    //prints linked list of access
    private static void printList()
    {

        for (int i = 0; i < container.size(); i++)
        {
            System.out.print("Domain " + i + " -> ");
            LinkedList<String> k = container.get(i);
            for (int j = 0; j < m+n; j++)
            {
                if (j < m){
                    if (!k.get(j).startsWith(" ")){
                        System.out.print("F" + (j) + ": ");
                        System.out.print(k.get(j) + ", ");
                    }
                } else {
                    if (!k.get(j).startsWith(" ") && !k.get(j).startsWith("N/A")){
                        System.out.print("D" + (j-m) + ": ");
                        System.out.print(k.get(j) + ", ");
                    }
                }
                

                if (j == n+m-1)
                {
                    System.out.println();
                }
            }

            //System.out.println();
            //System.out.println();

        }

        System.out.println();
        System.out.println();
    }
}
//end code changes by Martin Cook C00102798