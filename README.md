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

