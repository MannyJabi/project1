
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Driver {

    static List<String> history = new ArrayList<>();
    static BufferedWriter loggerIn;
    static BufferedWriter encIn;
    static BufferedReader encOut;
    static BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

    static void log(String line) throws IOException {
        loggerIn.write(line);
        loggerIn.newLine();
        loggerIn.flush();
    }

    // Send one command line to the encryption program and return its response line.
    static String send(String line) throws IOException {
        encIn.write(line);
        encIn.newLine();
        encIn.flush();
        String resp = encOut.readLine();
        return resp == null ? "ERROR No response" : resp.trim();
    }

    static String prompt(String msg) throws IOException {
        System.out.print(msg);
        System.out.flush();
        String s = console.readLine();
        if (s == null) {
            throw new EOFException();
        }
        return s.trim();
    }

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

    static void showHistory() {
        if (history.isEmpty()) {
            System.out.println("History is empty.");
            return;
        }
        for (int i = 0; i < history.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + history.get(i));
        }
    }

    // Returns a string from history or a newly typed one; null if the input was invalid.
    static String chooseString(String msg, boolean storeNew) throws IOException {
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
                    if (n.equals("0")) {
                        break;
                    }
                    try {
                        int idx = Integer.parseInt(n);
                        if (idx >= 1 && idx <= history.size()) {
                            return history.get(idx - 1);
                        }
                    } catch (NumberFormatException e) {
                        /* fall through */ }
                    System.out.println("Invalid selection.");
                }
            }
        }
        String s = prompt(msg);
        if (!valid(s)) {
            System.out.println("Error: input must contain only letters.");
            return null;
        }
        s = s.toUpperCase();
        if (storeNew) {
            history.add(s);
        }
        return s;
    }

    static void doPassword() throws IOException {
        log("CMD password");
        String s = chooseString("Enter password: ", false);
        if (s == null) {
            log("ERROR password input invalid");
            return;
        }
        String resp = send("PASS " + s);
        System.out.println(resp);

        // Never log the password itself.
        log("RESULT password command " + (resp.startsWith("ERROR") ? resp : "succeeded"));
    }

    static void doCrypt(String name) throws IOException {
        log("CMD " + name.toLowerCase());
        String s = chooseString("Enter string to " + name.toLowerCase() + ": ", true);
        if (s == null) {
            log("ERROR " + name.toLowerCase() + " input invalid");
            return;
        }
        log(name + " input " + s);
        String resp = send(name + " " + s);
        System.out.println(resp);
        String[] parts = resp.split(" ", 2);
        if (parts[0].equals("RESULT") && parts.length > 1) {
            history.add(parts[1]);
        }
        log("RESULT " + name.toLowerCase() + " " + resp);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 1) {
            System.out.println("Usage: java Driver <logfile>");
            System.exit(1);
        }
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String cp = System.getProperty("java.class.path");

        Process logger = new ProcessBuilder(javaBin, "-cp", cp, "Logger", args[0])
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        Process enc = new ProcessBuilder(javaBin, "-cp", cp, "Encrypt")
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();

        loggerIn = new BufferedWriter(new OutputStreamWriter(logger.getOutputStream()));
        encIn = new BufferedWriter(new OutputStreamWriter(enc.getOutputStream()));
        encOut = new BufferedReader(new InputStreamReader(enc.getInputStream()));

        log("START Driver started");
        try {
            while (true) {
                System.out.println("\nCommands: password, encrypt, decrypt, history, quit");
                String cmd = prompt("> ").toLowerCase();
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
            // console closed; fall through to clean shutdown
        }
        send("QUIT");
        log("EXIT Driver exiting");
        log("QUIT");
        loggerIn.close();
        encIn.close();
        logger.waitFor();
        enc.waitFor();
    }
}
