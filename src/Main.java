import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.Lock;
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
                //TODO: Access List
            }
            case ("-S3") -> {
                //TODO: Capability List
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