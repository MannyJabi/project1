# 10-18-2026 7:26 PM
Starting Project 1. The project is going to be made of three programs. The Logger writes timestamped lines to a log file, the
Encrypt program takes commands like PASS, ENCRYPT, and DECRYPT and runs a Vigenere cipher, and the Driver is the program the user runs. The Driver starts
the other two as child processes and talks to them through pipes, so this project is really about processes and pipes.
Plan: three programs: Logger, Encrypt, Driver. The Driver launches the other
two as child processes using ProcessBuilder and talks to them through pipes.
I plan to build them in this order: Logger, Encrypt, Driver.
Concerns: The Driver will write a command to the child's stdin and then wait to read a reply from its stdout. If the child doesn't send a reply, or the text sits in a buffer and never gets flushed, the Driver will hang forever waiting. I think I need to flush after every write, but I'll find out when I test it. I'm also not sure yet how the key repeats across the text in the Vigenere cipher, so I'll check that against the example in the assignment (HELLO with passkey HELLO should give OIWWC).

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
2026-10-18 19:42 [START] Logging Started.
That matches the format in the assignment. 

First the cat log didn't appear and i had to re run it because i believe since i was typing interactively in the git back that didnt rach java

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

# 10-18-2026 8:42pm

Compiled Encrypt.java with no errors. My first test gave ERROR Input must
contain only letters followed by ERROR Unknown command where I expected the
DECRYPT result. At first I thought my decrypt code was broken, but looking at
the terminal, the command had been split onto two lines when I pasted it, so the
program received DECRYPT with no argument and then OIWWC as its own command. It
was handling both correctly. Reruning the test on a single line gave RESULT
HELLO, so decrypting works.

Other results: ENCRYPT before PASS gave ERROR Password not set, PASS HELLO then
ENCRYPT HELLO gave OIWWC (matching the assignment), and "Hi there" gave the
letters-only error. A lowercase passkey and lowercase text also gave OIWWC, so
case is handled.

What I learned: the program only communicates through lines on stdin and stdout,
so every command has to arrive as exactly one line ending in a newline. That
matters for the Driver, which must send complete lines and flush them, or the
child process won't see the command and the Driver will wait forever for a reply.

Next: write the Driver, which starts both programs as child processes and
connects them with pipes.

# 10-18-2026 8:47pm

Logger and Encrypt both work on their own, so now I'm writing the Driver. This
is the program the user actually runs, and it ties the other two together. When
it starts it creates two new processes, one running Logger (with the log file
name as an argument) and one running Encrypt. In Java I do that with
ProcessBuilder, whose start() method returns a Process object for each child.
A process is a separate running program with its own memory, so the Driver,
the Logger, and Encrypt are three separate processes that can't share
variables. They have to communicate some other way.

The way they communicate is with pipes. For each child, the Driver gets a
stream connected to the child's standard input (process.getOutputStream(),
which is the Driver's side of the pipe, since the Driver writes to it) and,
for Encrypt, a stream connected to its standard output (process.getInputStream(),
which the Driver reads). So when the Driver writes "ENCRYPT HELLO" and a
newline into Encrypt's pipe, Encrypt's readLine() receives it as if someone had
typed it. Encrypt's reply goes back through the other pipe. The Logger only
needs one pipe, since the Driver only sends it log lines and never reads
anything back. I have to flush after every write, because otherwise the text
sits in the Driver's buffer, the child never sees the command, and the Driver
blocks forever in readLine() waiting for a reply.

Design decisions: the Driver keeps a history list for the run. Strings the user
types for encrypt and decrypt are added to it, and so are the results, but
passwords are never added and never logged. Every command and its result is
logged, along with the start and exit. Input is checked so that only letters are
accepted. On quit, the Driver sends QUIT to Encrypt and the Logger and waits
for both processes to finish.

How I'll test: I'll use piped input from a file because interactive typing
didn't work in Git Bash with the Logger.

# 10-18-2026 9:00PM

Compiled all three files with javac *.java and ran the Driver with a prepared
input file (made with a here doc) because interactive typing doesn't work
well in Git Bash. The full run worked: password Hello gave RESULT, ENCRYPT of
"hello" gave RESULT OIWWC, DECRYPT using history item 2 (OIWWC) gave RESULT
HELLO, "Hello World!" was rejected with the letters-only error, history listed
HELLO, OIWWC, HELLO, and quit exited cleanly. This also confirmed decrypt
works end to end through the pipes.

The log file had the start, a CMD and RESULT line for every command, the ERROR
for the invalid input, and the exit. The password command only logged that it
succeeded, with no password value, which is what the spec requires.

Problems: my first git commit for the Driver failed because I typed "- m"
with a space instead of "-m", so git treated "-" and "m" as filenames and
nothing was committed. Retyping it as -m fixed it. I also removed in.txt and
test.log before committing so the working tree stays clean.

What I learned about processes and pipes: the Driver, Logger, and Encrypt are
separate processes and only exchange data over the pipes. Each command is one
line, written and flushed, and the Driver blocks in readLine() until Encrypt
replies. That's why I flush after every write. On quit the Driver sends QUIT to
both children, closes the pipes, and waits for both processes to finish.

Next: final checks, then zip the repo and submit.