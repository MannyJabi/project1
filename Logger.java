import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger 
{
    public static void main(String[] args) throws IOException 
    {
        if (args.length != 1) 
        {
            System.err.println("Usage: java Logger <logfile>");
            System.exit(1);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try (PrintWriter log = new PrintWriter(new FileWriter(args[0], true))) 
        {
            String line;
            while ((line = in.readLine()) != null) 
            {
                line = line.trim();
                if (line.equals("QUIT")) break;
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+", 2);
                String action = parts[0];
                String message = parts.length > 1 ? parts[1] : "";
                String entry = LocalDateTime.now().format(fmt) + " [" + action + "] " + message;
                log.println(entry.stripTrailing());
                log.flush();
            }
        }
    }
}