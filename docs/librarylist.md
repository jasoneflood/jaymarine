1 Removed ALL libraries from Arduino IDE
The following did not report any issues, so may be installed by default
SPI.h
Wire.h
WiFI.h

The following reported errors (No such file or directory) and were addressed by installing the indicted libraries
#include <AsyncTCP.h>         // dvarell, https://github.com/dvarrel/AsyncTCP

