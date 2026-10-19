
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger: writes log messages to a file.
 *
 * Usage: java Logger <logfile>
 *
 * Reads lines from standard input until it receives "QUIT". The first word of
 * each line is the ACTION and the rest of the line is the MESSAGE. Each line is
 * written to the log file in the format: YYYY-MM-DD HH:MM [ACTION] MESSAGE This
 * program is started as a child process by the Driver, which sends it lines
 * through a pipe connected to this program's standard input.
 */
public class Logger {

    public static void main(String[] args) throws IOException {
        // The assignment requires exactly one command line argument: the log file name.
        if (args.length != 1) {
            System.err.println("Usage: java Logger <logfile>");
            System.exit(1);
        }

        // Timestamp format required by the assignment (24 hour notation).
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Standard input is the pipe from the Driver (or the keyboard/test file).
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        // "true" opens the file in append mode so earlier log lines are kept.
        // try-with-resources closes the file automatically when we are done.
        try (PrintWriter log = new PrintWriter(new FileWriter(args[0], true))) {
            String line;
            // readLine() returns null if the pipe is closed, which also ends the loop.
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.equals("QUIT")) {
                    break;   // QUIT ends the program

                                }if (line.isEmpty()) {
                    continue;     // ignore blank lines
                }
                // Split on whitespace into at most 2 pieces:
                // parts[0] = the action (first word), parts[1] = the rest of the line.
                String[] parts = line.split("\\s+", 2);
                String action = parts[0];
                String message = parts.length > 1 ? parts[1] : "";

                // Build the log line, e.g. 2026-09-14 11:32 [START] Logging Started.
                String entry = LocalDateTime.now().format(fmt) + " [" + action + "] " + message;
                log.println(entry.stripTrailing());

                // Flush so each entry reaches the file immediately instead of
                // waiting in a buffer.
                log.flush();
            }
        }
    }
}
