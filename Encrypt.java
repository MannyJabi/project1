
import java.io.*;

public class Encrypt {

    static String key = null;

    static boolean lettersOnly(String s) {
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

    static String vigenere(String text, String key, int sign) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            int shift = key.charAt(i % key.length()) - 'A';
            int v = ((text.charAt(i) - 'A' + sign * shift) % 26 + 26) % 26;
            sb.append((char) ('A' + v));
        }
        return sb.toString();
    }

    static void reply(String s) {
        System.out.println(s);
        System.out.flush();
    }

    public static void main(String[] args) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            String[] parts = line.split(" ", 2);
            String cmd = parts[0].toUpperCase();
            String arg = parts.length > 1 ? parts[1].trim().toUpperCase() : "";
            if (cmd.equals("QUIT")) {
                reply("RESULT Goodbye");
                break;
            } else if (cmd.equals("PASS")) {
                if (!lettersOnly(arg)) {
                    reply("ERROR Passkey must contain only letters");
                } else {
                    key = arg;
                    reply("RESULT");
                }
            } else if (cmd.equals("ENCRYPT") || cmd.equals("DECRYPT")) {
                if (key == null) {
                    reply("ERROR Password not set");
                } else if (!lettersOnly(arg)) {
                    reply("ERROR Input must contain only letters");
                } else {
                    reply("RESULT " + vigenere(arg, key, cmd.equals("ENCRYPT") ? 1 : -1));
                }
            } else {
                reply("ERROR Unknown command");
            }
        }
    }
}
