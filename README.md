USBtinSubProg
=============

USBtinSubProg is a Java application to dump from and flash Subaru ECUs
using USBtinLib, the Java Library for USBtin. USBtin is an open-source 
USB to CAN interace (http://www.fischl.de/usbtin/).

It requires a kernel for the specific Subaru ECU. Kernels can be obtained
from https://github.com/rimwall/npkern/tree/ssm_can_test/precompiled
The kernels are based on npkern by fenurgrec (https://github.com/fenugrec)

Status
------

Currently experimental status. Flashing has been successfully performed
on the bench of a 7058 ECU.

CAN bootloader
---------------------

Details of its operation are in "Denso CAN bootloader.txt" 


Build and run
-------------

To create a JAR file use
```
ant jar
```

To run use
```
java -jar USBtinSubProg.jar [kernel file name]
```

Hardware Setup
--------------

The key should be in the ignition, and the key turned to 'on' (engine NOT running). Maintaining battery voltage is important, particularly during flashing - you may wish to have a battery maintainer connected.

Connect any green 'test connectors' (does not exist on most CAN vehicles).

A USBtin is required. See http://www.fischl.de/usbtin/

You can either build your own USBtin for purchase one from the website. Connect the CAN-H and CAN-L lines from the USBtin to the correct pins on the OBD connector. Connect the USBtin to the USB port on your computer. Make note of the port number that is allocated to the USBtin by your operating system. Edit the java application (line 287) so that the COM port details are correct.

Ensure you have a copy of the kernel file in the same directory that USBtinSubProg is launched. The kernel file is available here: https://github.com/rimwall/npkern/tree/ssm_can_test/precompiled

So far, only a SH7058 kernel has been compiled. Others can be compiled if required.

Running USBtinSubProg
---------------------

Run as described above. The application will attempt to load and activate the kernel. After that, 4 simple commands are possible via a CLI:
- dump - will dump the ROM contents as per the prompts
- pflash - practice flash - will follow the same process as flashing, but flashing will not actually be activated
- rflash - real flash - will attempt to flash the ECU
- exit - will terminate the kernel, reset the ECU and exit USBtinSubProg

Error handling is rudimentary. Some errors are not handled properly, if this occurs the application may need to be closed and the USBtin may need to be disconnected and reconnected from the USB port.

Risks
-----

This is experimental software, and you use it at your own risk. You may brick your ECU.

License
-------

Copyright (C) 2022  rimwall

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

