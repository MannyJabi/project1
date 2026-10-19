# 10-18-2026 <7:26 PM>
Starting Project 1. The project is going to be made of three programs. The Logger writes timestamped lines to a log file, the
Encrypt program takes commands like PASS, ENCRYPT, and DECRYPT and runs a Vigenere cipher, and the Driver is the program the user runs. The Driver starts
the other two as child processes and talks to them through pipes, so this project is really about processes and pipes.
Plan: three programs: Logger, Encrypt, Driver. The Driver launches the other
two as child processes using ProcessBuilder and talks to them through pipes.
I plan to build them in this order: Logger, Encrypt, Driver, because [your reason].
Concerns: The Driver will write a command to the child's
stdin and then wait to read a reply from its stdout. If the child doesn't send a reply, or the text sits in a buffer and never gets flushed, the Driver will hang forever waiting. I think I need to flush after every write, but I'll find out when I test it. I'm also not sure yet how the key repeats across the text in the Vigenere cipher, so I'll check that against the example in the assignment (HELLO with passkey HELLO should give OIWWC).

# 10-18-2026 7:37pm

Starting on the Logger first, since it is the simplest of the three programs.
It takes one command line argument, the name of the log file, and then reads
lines from standard input until it receives QUIT. For each line, the first
word (the first group of non-whitespace characters) is the action and the rest
of the line is the message. Each one gets written to the file as a single line
in the format YYYY-MM-DD HH:MM [ACTION] MESSAGE, using the current time in
24 hour notation.

I plan to read the input with a BufferedReader on System.in and split each line
on whitespace with a limit of 2, so the action and the message come out
separately. I'll open the file in append mode so a second run doesn't erase
earlier log lines, and I'll flush after every line. Flushing matters here
because the Driver will eventually write to this program through a pipe, and
I want each log entry written right away instead of sitting in a buffer.

To test it, I'll run it by hand, type START Logging Started. and then QUIT,
and check that the file contains one correctly formatted line. One thing I'm
not sure about is what happens if a line has only an action and no message, so
I'll try that as well.



# 10-18-2026 7:50pm

Compiled Logger.java and ran it with test.log. I typed START Logging Started.
and then QUIT, and test.log contained one line:
2026-10-18 23:34 [START] Logging Started.
That matches the format in the assignment. I also tried a line with no message
(just the word DONE) and it logged as [DONE] with nothing after it, which is
what I expected.

First the cat log didn't appear and i had to re run it because i believe since pressiing enter might have caused and issue. 

What I learned: the logger is already a program that only communicates through
standard input, which means that when the Driver starts it as a child process,
the Driver can write to the logger's stdin through a pipe and the code does not
need to change. I also need to remember that the Driver has to send QUIT, or the
logger will keep waiting for input.

Next: write the Encrypt program and test it by hand before building the Driver.



# 10-18-2026 7:57pm

Logger is done and committed, so next is the Encrypt program. It reads commands
from standard input, one per line. The first word is the command and the rest of
the line is the argument. It has to handle PASS (set the passkey), ENCRYPT,
DECRYPT, and QUIT. It prints one line of output for each command, starting with
RESULT or ERROR. ENCRYPT and DECRYPT should give an error if no passkey has been
set yet.

For the cipher, I'm using a Vigenere cipher. Each letter of the text is shifted
by the matching letter of the key (A = 0, B = 1, and so on), and the key repeats
when the text is longer than the key. To decrypt I shift backwards by the same
amount. Wrapping around the alphabet needs the mod 26 operation, and I need to
be careful with negative numbers when decrypting, since Java's % can return a
negative result. I'll check my work against the assignment's example: HELLO with
passkey HELLO should give OIWWC. Working it out by hand, H+H = 7+7 = 14 = O,
E+E = 4+4 = 8 = I, L+L = 11+11 = 22 = W, and the last letter O+O = 14+14 = 28,
which wraps to 2 = C.

The spec also says the input should only be letters, so I'll return an error for
anything else. I'll flush the output after every reply, because the Driver will
be waiting on this program's output through a pipe and a buffered reply would
make it hang. Since interactive typing didn't work in Git Bash last time, I'll
test with piped input.