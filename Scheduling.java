// This file contains the main() function for the Scheduling
// simulation.  Init() initializes most of the variables by
// reading from a provided file.  SchedulingAlgorithm.Run() is
// called from main() to run the simulation.  Summary-Results
// is where the summary results are written, and Summary-Processes
// is where the process scheduling summary is written.

// Created by Alexander Reeder, 2001 January 06

import java.io.*;
import java.util.*;

public class Scheduling {

  private static int processnum = 5;
  private static int meanDev = 1000;
  private static int standardDev = 100;
  private static int runtime = 1000;
  private static Vector processVector = new Vector();
  private static Results result = new Results("null", "null", 0);
  private static String resultsFile = "Summary-Results";

  private static void Init(String file) {
    File f = new File(file);
    String line;
    String tmp;
    int cputime = 0;
    int ioblocking = 0;
    double X = 0.0;

    try {
      // BufferedReader in = new BufferedReader(new FileReader(f));
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      while ((line = in.readLine()) != null) {
        if (line.startsWith("numprocess")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          processnum = Common.s2i(st.nextToken());
        }
        if (line.startsWith("meandev")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          meanDev = Common.s2i(st.nextToken());
        }
        if (line.startsWith("standdev")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          standardDev = Common.s2i(st.nextToken());
        }
        if (line.startsWith("process")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          ioblocking = Common.s2i(st.nextToken());
          X = Common.R1();
          while (X == -1.0) {
            X = Common.R1();
          }
          X = X * standardDev;
          cputime = (int) X + meanDev;
          processVector.addElement(new sProcess(cputime, ioblocking, 0, 0, 0));
        }
        if (line.startsWith("runtime")) {
          StringTokenizer st = new StringTokenizer(line);
          st.nextToken();
          runtime = Common.s2i(st.nextToken());
        }
      }
      in.close();
    } catch (IOException e) {
      /* Handle exceptions */ }
  }

  private static void debug() {
    int i = 0;

    System.out.println("processnum " + processnum);
    System.out.println("meandevm " + meanDev);
    System.out.println("standdev " + standardDev);
    int size = processVector.size();
    for (i = 0; i < size; i++) {
      sProcess process = (sProcess) processVector.elementAt(i);
      System.out.println("process " + i + " " + process.cputime + " " + process.ioblocking + " " + process.cpudone + " "
          + process.numblocked);
    }
    System.out.println("runtime " + runtime);
  }

  public static void main(String[] args) {
    int i = 0;

    if (args.length != 1) {
      System.out.println("Usage: 'java Scheduling <INIT FILE>'");
      System.exit(-1);
    }
    File f = new File(args[0]);
    if (!(f.exists())) {
      System.out.println("Scheduling: error, file '" + f.getName() + "' does not exist.");
      System.exit(-1);
    }
    if (!(f.canRead())) {
      System.out.println("Scheduling: error, read of " + f.getName() + " failed.");
      System.exit(-1);
    }
    System.out.println("Working...");
    Init(args[0]);
    if (processVector.size() < processnum) {
      i = 0;
      while (processVector.size() < processnum) {
        double X = Common.R1();
        while (X == -1.0) {
          X = Common.R1();
        }
        X = X * standardDev;
        int cputime = (int) X + meanDev;
        processVector.addElement(new sProcess(cputime, i * 100, 0, 0, 0));
        i++;
      }
    }
    result = SchedulingAlgorithm.Run(runtime, processVector, result);
    try {
      // BufferedWriter out = new BufferedWriter(new FileWriter(resultsFile));
      PrintStream out = new PrintStream(new FileOutputStream(resultsFile));
      out.println("Scheduling Type: " + result.schedulingType);
      out.println("Scheduling Name: " + result.schedulingName);
      out.println("Simulation Run Time: " + result.compuTime);
      out.println("Mean: " + meanDev);
      out.println("Standard Deviation: " + standardDev);
      out.println("Process #\tCPU Time\tIO Blocking\tCPU Completed\tCPU Blocked");
      for (i = 0; i < processVector.size(); i++) {
        sProcess process = (sProcess) processVector.elementAt(i);
        out.print(Integer.toString(i));
        if (i < 100) {
          out.print("\t\t");
        } else {
          out.print("\t");
        }
        out.print(Integer.toString(process.cputime));
        if (process.cputime < 100) {
          out.print(" (ms)\t\t");
        } else {
          out.print(" (ms)\t");
        }
        out.print(Integer.toString(process.ioblocking));
        if (process.ioblocking < 100) {
          out.print(" (ms)\t\t");
        } else {
          out.print(" (ms)\t");
        }
        out.print(Integer.toString(process.cpudone));
        if (process.cpudone < 100) {
          out.print(" (ms)\t\t");
        } else {
          out.print(" (ms)\t");
        }
        out.println(process.numblocked + " times");
      }
      out.close();
    } catch (IOException e) {
      /* Handle exceptions */ }
    System.out.println("Completed.");
  }
}

class Process {
  public int cputime;
  public int ioblocking;
  public int cpudone;
  public int ionext;
  public int numblocked;

  public Process(int cputime, int ioblocking, int cpudone, int ionext, int numblocked) {
    this.cputime = cputime;
    this.ioblocking = ioblocking;
    this.cpudone = cpudone;
    this.ionext = ionext;
    this.numblocked = numblocked;
  }
}

class SchedulingAlgorithm {

  public static Results Run(int runtime, Vector processVector, Results result) {
    int i = 0;
    int comptime = 0;
    int currentProcess = 0;
    int size = processVector.size();
    int completed = 0;
    int timeQuantum = 500; // Adjust this value
    String resultsFile = "Summary-Processes";

    result.schedulingType = "Preemptive";
    result.schedulingName = "Round Robin";

    try {
      PrintStream out = new PrintStream(new FileOutputStream(resultsFile));

      while (completed < size && comptime < runtime) {
        sProcess process = (sProcess) processVector.elementAt(currentProcess);

        // Check if process is completed
        if (process.cpudone == process.cputime) {
          completed++;
          out.println(
              "Process: " + currentProcess + " completed... (" + process.cputime + " " + process.ioblocking + " "
                  + process.cpudone + " " + process.cpudone + ")");
        } else {

          // Allocate CPU time based on time quantum
          int timeToRun = Math.min(timeQuantum, process.cputime - process.cpudone);
          process.cpudone += timeToRun;
          comptime += timeToRun;

          out.println("Process: " + currentProcess + " allocated " + timeToRun + " CPU time... (" + process.cputime
              + " " + process.ioblocking + " "
              + process.cpudone + " " + process.cpudone + ")");

          // Check for I/O blocking
          if (process.ioblocking > 0) {
            process.ionext++;
            out.println(
                "Process: " + currentProcess + " I/O blocked... (" + process.cputime + " " + process.ioblocking + " "
                    + process.cpudone + " " + process.cpudone + ")");
          }
        }

        // Move to next process (circular fashion)
        currentProcess = (currentProcess + 1) % size;
      }

      out.close();
    } catch (IOException e) {
      /* Handle exceptions */
    }

    result.compuTime = comptime;
    return result;
  }
}

// class SchedulingAlgorithm {
//
// public static Results Run(int runtime, Vector processVector, Results result)
// {
// int i = 0;
// int comptime = 0;
// int currentProcess = 0;
// int previousProcess = 0;
// int size = processVector.size();
// int completed = 0;
// String resultsFile = "Summary-Processes";
//
// result.schedulingType = "Batch (Nonpreemptive)";
// result.schedulingName = "First-Come First-Served";
// try {
// // BufferedWriter out = new BufferedWriter(new FileWriter(resultsFile));
// // OutputStream out = new FileOutputStream(resultsFile);
// PrintStream out = new PrintStream(new FileOutputStream(resultsFile));
// sProcess process = (sProcess) processVector.elementAt(currentProcess);
// out.println("Process: " + currentProcess + " registered... (" +
// process.cputime + " " + process.ioblocking + " "
// + process.cpudone + " " + process.cpudone + ")");
// while (comptime < runtime) {
// if (process.cpudone == process.cputime) {
// completed++;
// out.println("Process: " + currentProcess + " completed... (" +
// process.cputime + " " + process.ioblocking
// + " " + process.cpudone + " " + process.cpudone + ")");
// if (completed == size) {
// result.compuTime = comptime;
// out.close();
// return result;
// }
// for (i = size - 1; i >= 0; i--) {
// process = (sProcess) processVector.elementAt(i);
// if (process.cpudone < process.cputime) {
// currentProcess = i;
// }
// }
// process = (sProcess) processVector.elementAt(currentProcess);
// out.println("Process: " + currentProcess + " registered... (" +
// process.cputime + " " + process.ioblocking
// + " " + process.cpudone + " " + process.cpudone + ")");
// }
// if (process.ioblocking == process.ionext) {
// out.println("Process: " + currentProcess + " I/O blocked... (" +
// process.cputime + " " + process.ioblocking
// + " " + process.cpudone + " " + process.cpudone + ")");
// process.numblocked++;
// process.ionext = 0;
// previousProcess = currentProcess;
// for (i = size - 1; i >= 0; i--) {
// process = (sProcess) processVector.elementAt(i);
// if (process.cpudone < process.cputime && previousProcess != i) {
// currentProcess = i;
// }
// }
// process = (sProcess) processVector.elementAt(currentProcess);
// out.println("Process: " + currentProcess + " registered... (" +
// process.cputime + " " + process.ioblocking
// + " " + process.cpudone + " " + process.cpudone + ")");
// }
// process.cpudone++;
// if (process.ioblocking > 0) {
// process.ionext++;
// }
// comptime++;
// }
// out.close();
// } catch (IOException e) {
// /* Handle exceptions */ }
// result.compuTime = comptime;
// return result;
// }
// }

class Common {

  static public int s2i(String s) {
    int i = 0;

    try {
      i = Integer.parseInt(s.trim());
    } catch (NumberFormatException nfe) {
      System.out.println("NumberFormatException: " + nfe.getMessage());
    }
    return i;
  }

  static public double R1() {
    java.util.Random generator = new java.util.Random(System.currentTimeMillis());
    double U = generator.nextDouble();
    while (U < 0 || U >= 1) {
      U = generator.nextDouble();
    }
    double V = generator.nextDouble();
    while (V < 0 || V >= 1) {
      V = generator.nextDouble();
    }
    double X = Math.sqrt((8 / Math.E)) * (V - 0.5) / U;
    if (!(R2(X, U))) {
      return -1;
    }
    if (!(R3(X, U))) {
      return -1;
    }
    if (!(R4(X, U))) {
      return -1;
    }
    return X;
  }

  static public boolean R2(double X, double U) {
    if ((X * X) <= (5 - 4 * Math.exp(.25) * U)) {
      return true;
    } else {
      return false;
    }
  }

  static public boolean R3(double X, double U) {
    if ((X * X) >= (4 * Math.exp(-1.35) / U + 1.4)) {
      return false;
    } else {
      return true;
    }
  }

  static public boolean R4(double X, double U) {
    if ((X * X) < (-4 * Math.log(U))) {
      return true;
    } else {
      return false;
    }
  }

}

class Results {
  public String schedulingType;
  public String schedulingName;
  public int compuTime;

  public Results(String schedulingType, String schedulingName, int compuTime) {
    this.schedulingType = schedulingType;
    this.schedulingName = schedulingName;
    this.compuTime = compuTime;
  }
}

class sProcess {
  public int cputime;
  public int ioblocking;
  public int cpudone;
  public int ionext;
  public int numblocked;

  public sProcess(int cputime, int ioblocking, int cpudone, int ionext, int numblocked) {
    this.cputime = cputime;
    this.ioblocking = ioblocking;
    this.cpudone = cpudone;
    this.ionext = ionext;
    this.numblocked = numblocked;
  }
}
