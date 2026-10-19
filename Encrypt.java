
import java.io.*;

/**
 * Encrypt: the encryption backend.
 *
 * Reads commands, one per line, from standard input and prints one response
 * line to standard output. The first word of a line is the command and the rest
 * of the line (after the first space) is the argument.
 *
 * Commands: PASS <key> sets the passkey used for encrypting and decrypting
 * ENCRYPT <text> encrypts the text with a Vigenere cipher using the passkey
 * DECRYPT <text> decrypts the text with a Vigenere cipher using the passkey
 * QUIT exits the program
 *
 * Responses start with RESULT (success) or ERROR (failure). This program is
 * started as a child process by the Driver, which talks to it through pipes
 * connected to its standard input and standard output.
 */
public class Encrypt {

    // The current passkey, remembered between commands. null means "not set yet".
    static String key = null;

    // Returns true if the string is non-empty and contains only letters A-Z / a-z.
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

    /**
     * Vigenere cipher. Both text and key must already be uppercase letters.
     * Each letter of the text is shifted by the matching letter of the key (A =
     * 0, B = 1, ... Z = 25). The key repeats if it is shorter than the text.
     *
     * sign = +1 encrypts (shift forward), sign = -1 decrypts (shift backward).
     */
    static String vigenere(String text, String key, int sign) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            // i % key.length() makes the key repeat over the text.
            int shift = key.charAt(i % key.length()) - 'A';
            // Convert the letter to 0-25, shift it, then wrap around with mod 26.
            // The "+ 26) % 26" makes sure the result is never negative when decrypting,
            // because Java's % operator can return a negative number.
            int v = ((text.charAt(i) - 'A' + sign * shift) % 26 + 26) % 26;
            sb.append((char) ('A' + v));   // convert back to a letter
        }
        return sb.toString();
    }

    // Print one response line and flush it right away. The Driver is blocked
    // waiting for this line, so it must not sit in a buffer.
    static void reply(String s) {
        System.out.println(s);
        System.out.flush();
    }

    public static void main(String[] args) throws IOException {
        // Standard input is the pipe from the Driver.
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = in.readLine()) != null) {
            // Split into the command (first word) and the argument (everything after
            // the first space).
            String[] parts = line.split(" ", 2);
            String cmd = parts[0].toUpperCase();
            // The input is made uppercase so "hello" and "HELLO" give the same result.
            String arg = parts.length > 1 ? parts[1].trim().toUpperCase() : "";

            if (cmd.equals("QUIT")) {
                reply("RESULT Goodbye");
                break;
            } else if (cmd.equals("PASS")) {
                // The passkey must be letters only.
                if (!lettersOnly(arg)) {
                    reply("ERROR Passkey must contain only letters");
                } else {
                    key = arg;
                    reply("RESULT");
                }
            } else if (cmd.equals("ENCRYPT") || cmd.equals("DECRYPT")) {
                if (key == null) {
                    // Required by the assignment: error if no passkey has been set.
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
