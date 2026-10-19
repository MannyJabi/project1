# 10-18-2026 <7:26 PM>
Starting Project 1. The project is going to be made of three programs. The Logger writes timestamped lines to a log file, the
Encrypt program takes commands like PASS, ENCRYPT, and DECRYPT and runs a Vigenere cipher, and the Driver is the program the user runs. The Driver starts
the other two as child processes and talks to them through pipes, so this project is really about processes and pipes.
Plan: three programs: Logger, Encrypt, Driver. The Driver launches the other
two as child processes using ProcessBuilder and talks to them through pipes.
I plan to build them in this order: Logger, Encrypt, Driver, because [your reason].
Concerns: The Driver will write a command to the child's
stdin and then wait to read a reply from its stdout. If the child doesn't send a reply, or the text sits in a buffer and never gets flushed, the Driver will hang forever waiting. I think I need to flush after every write, but I'll find out when I test it. I'm also not sure yet how the key repeats across the text in the Vigenere cipher, so I'll check that against the example in the assignment (HELLO with passkey HELLO should give OIWWC).