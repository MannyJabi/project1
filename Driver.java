
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Driver: the program the user runs.
 *
 * Usage: java Driver <logfile>
 *
 * It starts two child processes, the Logger and the Encrypt program, using
 * ProcessBuilder. It communicates with them through pipes: -
 * Process.getOutputStream() is the Driver's end of a pipe that feeds the
 * child's standard input (the Driver writes to it). - Process.getInputStream()
 * is the Driver's end of a pipe that carries the child's standard output (the
 * Driver reads from it).
 *
 * The Driver shows a menu (password, encrypt, decrypt, history, quit), keeps a
 * history of strings for this run, and logs every command and its result.
 * Passwords are never logged and never stored in the history.
 */
public class Driver {

    // Strings entered for encrypt/decrypt and their results. Lasts only for this run.
    static List<String> history = new ArrayList<>();

    static BufferedWriter loggerIn;   // writes to the Logger's standard input
    static BufferedWriter encIn;      // writes to Encrypt's standard input
    static BufferedReader encOut;     // reads Encrypt's standard output
    static BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

    // Send one line to the Logger through its pipe. Flushing makes sure the
    // line actually leaves our buffer and reaches the child process.
    static void log(String line) throws IOException {
        loggerIn.write(line);
        loggerIn.newLine();
        loggerIn.flush();
    }

    // Send one command line to the Encrypt program and return its one-line reply.
    // readLine() blocks (waits) until Encrypt answers, which is why both the
    // newline and the flush are required.
    static String send(String line) throws IOException {
        encIn.write(line);
        encIn.newLine();
        encIn.flush();
        String resp = encOut.readLine();
        return resp == null ? "ERROR No response" : resp.trim();
    }

    // Print a prompt and read one line from the user.
    // If the input ends (end of file), throw so main() can shut down cleanly.
    static String prompt(String msg) throws IOException {
        System.out.print(msg);
        System.out.flush();
        String s = console.readLine();
        if (s == null) {
            throw new EOFException();
        }
        return s.trim();
    }

    // Input checking: non-empty and letters only (as the assignment requires).
    static boolean valid(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (char c : s.toCharArray()) {
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                return false;
            }
        }
        return true;
    }

    // Print the numbered history list.
    static void showHistory() {
        if (history.isEmpty()) {
            System.out.println("History is empty.");
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + history.get(i));
        }
    }

    /**
     * Lets the user pick a string from the history or type a new one. Used by
     * the password, encrypt, and decrypt commands.
     *
     * msg the prompt to show when asking for a new string storeNew whether a
     * newly typed string is saved in the history (true for encrypt/decrypt,
     * false for passwords)
     *
     * Returns the chosen string in uppercase, or null if the input was invalid.
     */
    static String chooseString(String msg, boolean storeNew) throws IOException {
        // Only offer the history menu if there is something in the history.
        if (!history.isEmpty()) {
            String c;
            while (true) {
                c = prompt("1) Use a string from history\n2) Enter a new string\n> ");
                if (c.equals("1") || c.equals("2")) {
                    break;
                }
                System.out.println("Please enter 1 or 2.");
            }
            if (c.equals("1")) {
                while (true) {
                    showHistory();
                    String n = prompt("Select a number (0 to enter a new string instead): ");
                    // 0 is the way out of the history so a new string can be entered.
                    if (n.equals("0")) {
                        break;
                    }
                    try {
                        int idx = Integer.parseInt(n);
                        if (idx >= 1 && idx <= history.size()) {
                            return history.get(idx - 1);
                        }
                    } catch (NumberFormatException e) {
                        /* not a number: fall through */ }
                    System.out.println("Invalid selection.");
                }
            }
        }
        // Enter a new string.
        String s = prompt(msg);
        if (!valid(s)) {
            System.out.println("Error: input must contain only letters.");
            return null;
        }
        s = s.toUpperCase();   // case insensitive: "Hello" and "hello" are the same
        if (storeNew) {
            history.add(s);
        }
        return s;
    }

    // The "password" command: choose a string, then send PASS to Encrypt.
    static void doPassword() throws IOException {
        log("CMD password");
        // storeNew is false: the password is not saved in the history.
        String s = chooseString("Enter password: ", false);
        if (s == null) {
            log("ERROR password input invalid");
            return;
        }
        String resp = send("PASS " + s);
        System.out.println(resp);
        // Never log the password itself, only whether the command worked.
        log("RESULT password command " + (resp.startsWith("ERROR") ? resp : "succeeded"));
    }

    // The "encrypt" and "decrypt" commands. name is "ENCRYPT" or "DECRYPT".
    static void doCrypt(String name) throws IOException {
        log("CMD " + name.toLowerCase());
        // storeNew is true: a newly typed string is saved in the history.
        String s = chooseString("Enter string to " + name.toLowerCase() + ": ", true);
        if (s == null) {
            log("ERROR " + name.toLowerCase() + " input invalid");
            return;
        }
        log(name + " input " + s);
        String resp = send(name + " " + s);
        System.out.println(resp);
        // On success the reply looks like "RESULT <text>". Save <text> in the history.
        String[] parts = resp.split(" ", 2);
        if (parts[0].equals("RESULT") && parts.length > 1) {
            history.add(parts[1]);
        }
        log("RESULT " + name.toLowerCase() + " " + resp);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // The assignment requires exactly one command line argument: the log file name.
        if (args.length != 1) {
            System.out.println("Usage: java Driver <logfile>");
            System.exit(1);
        }

        // Use the same Java and class path that launched the Driver to start the children.
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String cp = System.getProperty("java.class.path");

        // Create the two child processes. Error output is passed through to the
        // terminal so problems in a child are visible.
        Process logger = new ProcessBuilder(javaBin, "-cp", cp, "Logger", args[0])
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        Process enc = new ProcessBuilder(javaBin, "-cp", cp, "Encrypt")
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();

        // Wrap the pipe streams so we can write and read whole lines of text.
        loggerIn = new BufferedWriter(new OutputStreamWriter(logger.getOutputStream()));
        encIn = new BufferedWriter(new OutputStreamWriter(enc.getOutputStream()));
        encOut = new BufferedReader(new InputStreamReader(enc.getInputStream()));

        log("START Driver started");
        try {
            // Main menu loop: runs until the user types quit.
            while (true) {
                System.out.println("\nCommands: password, encrypt, decrypt, history, quit");
                String cmd = prompt("> ").toLowerCase();   // commands are case insensitive
                if (cmd.equals("password")) {
                    doPassword();
                } else if (cmd.equals("encrypt")) {
                    doCrypt("ENCRYPT");
                } else if (cmd.equals("decrypt")) {
                    doCrypt("DECRYPT");
                } else if (cmd.equals("history")) {
                    log("CMD history");
                    showHistory();
                    log("RESULT history shown");
                } else if (cmd.equals("quit")) {
                    log("CMD quit");
                    break;
                } else {
                    System.out.println("Unknown command.");
                    log("ERROR unknown command");
                }
            }
        } catch (EOFException e) {
            // The input ended without a quit command; fall through to a clean shutdown.
        }

        // Shut down: tell Encrypt and the Logger to quit, close the pipes, and
        // wait for both child processes to finish so none are left running.
        send("QUIT");
        log("EXIT Driver exiting");
        log("QUIT");
        loggerIn.close();
        encIn.close();
        logger.waitFor();
        enc.waitFor();
    }
}
