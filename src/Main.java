import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static void main(String[] args) {
        String CLI = "";
        for (String arg: args) {
            CLI += arg;
        }
        Random rand = new Random();
        // N domains, M files
        int m = rand.nextInt(3,8);
        int n = rand.nextInt(3,8);
        System.out.println(m + " files (M) and " + n + " domains (N)");

        switch (CLI) {
            case ("-S1") -> {
                String[][] accessMatrix = new String[n][m + n];
                //populate
                for (int j = 0; j < accessMatrix.length; j++) {
                    for (int i = 0; i < accessMatrix[j].length; i++) {
                        String[] row = accessMatrix[j];
                        boolean rBool = rand.nextBoolean();
                        boolean wBool = rand.nextBoolean();
                        if (i < m) {
                            if (rBool && wBool) {
                                row[i] = "R/W";
                            } else if (rBool && !wBool) {
                                row[i] = "R";
                            } else if (wBool && !rBool) {
                                row[i] = "W";
                            }
                        } else {
                            if (rBool) {
                                row[i] = "allow";
                            }
                        }
                        if (i == j + m) {
                            row[i] = "N/A";
                        }
                    }
                }
                ReentrantLock[][] lockMatrix = new ReentrantLock[n][m];
                for (ReentrantLock[] row : lockMatrix) {
                    Arrays.fill(row, new ReentrantLock());
                }
                //print
                System.out.print("   ");
                for (int i = 0; i < n + m; i++) {
                    if (i < m) {
                        System.out.print("   F" + i + "   ");
                    } else {
                        System.out.print("   D" + (i - m) + "   ");
                    }
                }
                System.out.println();
                for (int i = 0; i < accessMatrix.length; i++) {
                    System.out.print("D" + (i) + "|");
                    String[] row = accessMatrix[i];
                    for (String value : row) {
                        if (value == null) {
                            System.out.print("       |");
                        } else {
                            System.out.printf("%7s|", value);
                        }
                    }
                    System.out.println();
                }
                //create threads
                for (int i = 0; i < n; i++) {
                    maxtrixThread t = new maxtrixThread(i, accessMatrix, lockMatrix);
                    t.start();
                }
            }
            case ("-S2") -> {

                String[] operation = {"R", "W", "R/W", null};
                String[] perm = {"N/A", "allow", null};

                LinkedList<String>[] accessLists = new LinkedList[m + n];
                for (int i = 0; i < accessLists.length; i++) {
                    accessLists[i] = new LinkedList<String>();
                }

                // Create list for each object with random operations/permissions
                for (int i = 0; i < m+n; i++) {
                    if (i < m) { // lists for files created first
                        for (int j = 0; j < n; j++) {
                            int randOp = rand.nextInt(operation.length);
                            accessLists[i].add(operation[randOp]); // add operation to each file list
                        }

                    } else {
                        // add permissions to each domain list
                        for (int j = 0; j < n; j++) {
                            int randPerm = rand.nextInt(perm.length);
                            accessLists[i].add(perm[randPerm]);
                        }
                    }
                }

                // Print out the lists for each object/domain
                for (int i = 0; i < accessLists.length; i++) {
                    if (i < m) {
                        System.out.print("F" + (i+1) + " --> ");
                        for (int j = 0; j < n; j++) {
                            if(accessLists[i].get(j) == null) {
                                System.out.print("");
                            } else {
                                System.out.print("D" + (j+1) + ":" + accessLists[i].get(j) + "  ");
                            }
                        }
                        System.out.println();

                    } else {
                        System.out.print("D" + (i-m+1) + " --> ");
                        for (int j = 0; j < n; j++) {
                            if(accessLists[i].get(j) == null) {
                                System.out.print("");
                            } else {
                                System.out.print("D" + (j+1) + ":" + accessLists[i].get(j) + "  ");
                            }
                        }
                        System.out.println();
                    }
                }
                // Create locks for file access (one lock per file)
                ReentrantLock[] fileLocks = new ReentrantLock[m];
                for (int i = 0; i < m; i++) {
                    fileLocks[i] = new ReentrantLock();
                }

                // Create an instance of S2Arbitrator and pass m, n along with accessLists
                S2Arbitrator s2arb = new S2Arbitrator(accessLists, m, n);

                // Create and start a ListThread for each domain, passing the S2Arbitrator
                for (int i = 0; i < n; i++) {
                    Thread t = new Thread(new ListThread(i, accessLists, fileLocks, s2arb));
                    t.start();
                }



                // Run threads; use accessLists to traverse through object lists in thread class

            }
            case ("-S3") -> {
                
                Integer[] parameters = {m,n};
                TaskThree.main(parameters);
            }
            default -> System.out.println("Invalid input. Possible arguments are '-S 1', '-S 2', and '-S 3'");
        }

    }
}

class maxtrixThread extends Thread{
    int tID;
    int requestCount = 0;

    //for ease of referencing arrays, currentDomain is 0-indexed
    int currentDomain;
    static ReentrantLock[][] lockMatrix;
    static String[][] accessMatrix;
    //stringMatrix represents files to read/write to
    static String[][] stringMatrix;
    static Random rand = new Random();
    static int m;
    static int n;

    public maxtrixThread(int ID, String[][]matrix, ReentrantLock[][] lockMatrix) {
        this.tID = ID;
        currentDomain = tID;
        maxtrixThread.n = matrix.length;
        maxtrixThread.m = matrix[0].length - n;
        maxtrixThread.lockMatrix = lockMatrix;
        maxtrixThread.stringMatrix = new String[n][m];
        //initialize file contents
        for (String[] strings : stringMatrix) {
            Arrays.fill(strings, "Hi!");
        }
        accessMatrix = matrix;
    }

    //the param lookingFor has intended values "R", "W", and "allow"
    public boolean arbitrate(int currentDomain, int column, String lookingFor){
        int row = currentDomain;
        String access = accessMatrix[row][column];
        if (column < m){
            if (access == null || !access.contains(lookingFor)){
                System.out.println("Thread " + tID + "(D" + (currentDomain) + "): " + lookingFor + " request failed on F"  + column);
                return false;
            } else {
                System.out.println("Thread " + tID + "(D" + (currentDomain) + "): " + lookingFor + " request successful on F"  + column);
                return true;
            }
        } else {
            if (access == null || !access.contains(lookingFor)){
                System.out.println("Thread " + tID + "(D" + (currentDomain) + "): switch request failed on D"  + (column-m));
                return false;
            } else {
                System.out.println("Thread " + tID + "(D" + (currentDomain) + "): switch request successful on D"  + (column-m));
                return true;
            }
            //return false;
        }
    }

    public void run(){
        while(requestCount < 5){
            int column = rand.nextInt(0, m+n);
            //for files
            if (column < m){
                boolean rBool = rand.nextBoolean();
                //request read
                if (rBool){
                    System.out.println("----Thread " + tID + "(D" + (currentDomain) + ")" + " requests to read F"  + column);
                    boolean readAccess = arbitrate(tID, column, "R");
                    if (readAccess) {
                        lockMatrix[currentDomain][column].lock();
                        System.out.println("Thread " + tID + "(D" + (currentDomain) + ")" +
                                " reads F" + column +": " + stringMatrix[currentDomain][column]);
                        int yields = rand.nextInt(3,8);
                        System.out.println("Thread " + tID + "(D" + (currentDomain) + ")" +
                                " yielding " +  yields + " times");
                        for (int j = 0; j < yields; j++) {
                            Thread.yield();
                        }
                        lockMatrix[currentDomain][column].unlock();
                    }
                //request write
                } else {
                    System.out.println("----Thread " + tID + "(D" + (currentDomain) + ")" + " requests to write to F"  + column);
                    boolean writeAccess = arbitrate(tID, column, "W");
                    if (writeAccess){
                        lockMatrix[currentDomain][column].lock();
                        String[] randomStrings = {"Red", "Purple", "Blue", "Yellow", "Orange"};
                        int index = rand.nextInt(0, randomStrings.length);
                        stringMatrix[currentDomain][column] = randomStrings[index];
                        System.out.println("Thread " + tID + "(D" + (currentDomain) + ")" +
                                " writes to F" + column +": " + randomStrings[index]);

                        int yields = rand.nextInt(3,8);
                        System.out.println("Thread " + tID + "(D" + (currentDomain) + ")" +
                                " yielding " +  yields + " times");
                        for (int j = 0; j < yields; j++) {
                            Thread.yield();
                        }
                        lockMatrix[currentDomain][column].unlock();
                    }
                }
            //domain switching
            } else if (column >= m){
                while (column-m == currentDomain){
                    column = rand.nextInt(m, m+n);
                }
                System.out.println("----Thread " + tID + "(D" + (currentDomain) + ")" + " requests to switch to D"  + (column-m));
                boolean switchAccess = arbitrate(currentDomain, column, "allow");
                if(switchAccess){
                    int oldDomain = currentDomain;
                    currentDomain = column-m;
                    System.out.println("Thread " + tID + "(D" + (currentDomain) + ")" + " has switched from D"  + oldDomain + " to D" + currentDomain);
                    int yields = rand.nextInt(3,8);
                    System.out.println("Thread " + tID + "(D" + (currentDomain) + ")" +
                            " yielding " +  yields + " times");
                    for (int j = 0; j < yields; j++) {
                        Thread.yield();
                    }
                }

            }
            requestCount++;
            System.out.println("++++Thread " + tID + "(D" + (currentDomain) + ")" +" request count:" + requestCount);
        }
    }
}

class ListThread implements Runnable {
    int ID;
    LinkedList<String>[] accessLists;
    ReentrantLock[] fileLocks;
    S2Arbitrator s2arb;  // S2Arbitrator instance for S2 access control
    Random rand = new Random();
    public ListThread(int ID, LinkedList<String>[] accessLists, ReentrantLock[] fileLocks, S2Arbitrator s2arb) {
        this.ID = ID;
        this.accessLists = accessLists;
        this.fileLocks = fileLocks;
        this.s2arb = s2arb;

    }



    @Override
    public void run() {
        int currentDomain = ID;

        for (int req = 0; req < 5; req++) { // Each thread makes at least 5 requests
            int action = rand.nextInt(2); // 0 = file access, 1 = domain switch

            if (action == 0) {
                // Try accessing a file
                int fileId = rand.nextInt(fileLocks.length);
                if (s2arb.arbitrate(currentDomain, fileId, "R")) {
                    fileLocks[fileId].lock();
                    try {
                        System.out.println("D" + (ID + 1) + " accessed F" + (fileId + 1)
                                + " with: " + accessLists[fileId].get(currentDomain));
                    } finally {
                        fileLocks[fileId].unlock();
                    }
                } else {
                    System.out.println("D" + (ID + 1) + " denied access to F" + (fileId + 1));
                }
            } else {
                // Try switching domains
                int targetDomain = rand.nextInt(s2arb.n);

                while(targetDomain == currentDomain) {
                    targetDomain = rand.nextInt(s2arb.n); //get a new target domain if domains match
                }

                int column = s2arb.m + targetDomain; // Calculate the column index for domain switching
                if (s2arb.arbitrate(currentDomain, column, "allow")) {
                    System.out.println("D" + (ID + 1) + " switched from D" + (currentDomain + 1)
                            + " to D" + (targetDomain + 1));
                    currentDomain = targetDomain;
                } else {
                    System.out.println("D" + (ID + 1) + " denied switching to D" + (targetDomain + 1));
                }
            }
        }
    }
}

class S2Arbitrator {
    LinkedList<String>[] accessLists;
    int m; // Number of files
    int n; // Number of domains

    public S2Arbitrator(LinkedList<String>[] accessLists, int m, int n) {
        this.accessLists = accessLists;
        this.m = m;
        this.n = n;
    }

    public boolean arbitrate(int currentDomain, int column, String lookingFor) {
        if (column < m) { // File access case
            String permission = accessLists[column].get(currentDomain);
            if (permission == null || !permission.contains(lookingFor)) {
                System.out.println("D" + (currentDomain + 1) + " denied " + lookingFor + " on F" + (column + 1));
                return false;
            } else {
                System.out.println("D" + (currentDomain + 1) + " granted " + lookingFor + " on F" + (column + 1));
                return true;
            }
        } else { // Domain switching case
            int newDomain = column - m;
            String permission = accessLists[column].get(currentDomain);
            if (permission == null || !permission.equals("allow")) {
                System.out.println("D" + (currentDomain + 1) + " denied switching to D" + (newDomain + 1));
                return false;
            } else {
                System.out.println("D" + (currentDomain + 1) + " granted switching to D" + (newDomain + 1));
                return true;
            }
        }
    }
}