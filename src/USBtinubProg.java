/*
 * Application based on USBtinLibDemo and using USBtinLib, the Java Library for USBtin
 * http://www.fischl.de/usbtin
 *
 * Copyright (C) 2022  rimwall 
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package USBtinSubProg;

import de.fischl.usbtin.*;
import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

/**
 * Application based on using USBtinLibDemo authored by Thomas Fischl.
 * 
 * 
 */
public class USBtinSubProg implements CANMessageListener {

	enum AccessStatus { NO_COMMS, COMMS_OK, READY_FOR_LOAD, READY_FOR_JUMP, READY_FOR_ERASE, READY_TO_LOAD_128BYTES, FLASHED_128BYTES, KERNEL_DIED };

    /** CAN message identifier for CAN messages from ECU */
    static final int ECUID = 0x21;
	static int checkSum;
	static AccessStatus currentStatus = AccessStatus.NO_COMMS;
	static byte dumpBytes[] = new byte[256];
	static int dumpByteCounter, dumpTimeOut;
	static boolean dumpingBlocks = false;
										 
    /**
     * This method is called every time a CAN message is received.
     * 
     * @param canmsg Received CAN message
     */
    @Override
    public void receiveCANMessage(CANMessage canmsg) {

		byte rspCode, rspDataLength, i;
		byte rspAll[] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

		if (canmsg.getId() == ECUID) {
            
            //System.out.println("Watched message: " + canmsg);
			
			rspAll = canmsg.getData();
			
			//System.out.println("rspAll: " + (rspAll[0] & 0xFF) + " " + (rspAll[1] & 0xFF) + " " + (rspAll[2] & 0xFF) + " " + (rspAll[3] & 0xFF) + " " + (rspAll[4] & 0xFF) + " " + (rspAll[5] & 0xFF) + " " + (rspAll[6] & 0xFF) + " " + (rspAll[7] & 0xFF));
			
			if (dumpingBlocks) {
				
				for (i = 0; i < 8; i++) {
					
					dumpBytes[dumpByteCounter] = rspAll[i];
					dumpByteCounter++;
					dumpTimeOut = 0;
					
				}
				
			}
			
			else if (rspAll[0] == (byte) 0x7A) {   // first byte should be 0x7A

				rspCode = (byte) (rspAll[1] & 0xF8);   // top 5 bits is the code
				rspDataLength = (byte) (rspAll[1] & 0x07);   // bottom 3 bits is the data length//System.out.println("0x7A response received, rspCode: " + (rspCode & 0xFF) + " rspDataLength: " + rspDataLength);

				switch(rspCode) {
					case (byte) 0x88 :
						// Bootloader: response to set unk command
						System.out.println("0x88 response received, length " + rspDataLength);
						break;
					case (byte) 0x90 : 
						// Bootloader: handle response to unk command
						System.out.println("0x90 response received, length " + rspDataLength);
						currentStatus = AccessStatus.COMMS_OK;
						break;
					case (byte) 0x98 :
						// Bootloader: response to set copy address command
						System.out.println("0x98 response received, length " + rspDataLength);
						currentStatus = AccessStatus.READY_FOR_LOAD;
						break;
					case (byte) 0xA8 : 
						// Bootloader: handle response to copy kernel command
						System.out.println("0xA8 response received, length " + rspDataLength);
						break;
					case (byte) 0xB0 :
						// Bootloader: handle response to report checksum command
						System.out.println("0xB0 response received, length " + rspDataLength + " checksum: " + (rspAll[2] & 0xFF));
						if(checkSum == (rspAll[2] & 0xFF)) {
							System.out.println("Checksum ok");
							currentStatus = AccessStatus.READY_FOR_JUMP;
						}
						else System.out.println("Checksum error");
						break;							
					case (byte) 0xC0 :
						// Bootloader: handle response to unk command
						System.out.println("0xC0 response received, length " + rspDataLength);
						break;
					case (byte) 0xD0 :
						// Kernel: handle response to request checksum command
						System.out.println("0xD0 response received, length " + rspDataLength);
						checkSum = (rspAll[2] & 0xFF);
						break;
					case (byte) 0xD8 :
						// Kernel: handle response to ROM dump command
						System.out.println("0xD8 response received, length " + rspDataLength);
						//for (i = 2; i < (rspDataLength + 2); i++) {
						//	dumpBytes[dumpByteCounter] = rspAll[i];
						//	dumpByteCounter++;
						//	dumpTimeOut = 0;
						//}
						dumpingBlocks = true;
						break;
					case (byte) 0xE0 :
						// Kernel: response to initialise erasing / flashing microcodes command
						System.out.println("0xE0 response received, length " + rspDataLength);
						currentStatus = AccessStatus.READY_FOR_ERASE;
						break;
					case (byte) 0xF0 :
						// Kernel: response to erase block command
						System.out.println("0xF0 response received, length " + rspDataLength);
						currentStatus = AccessStatus.READY_TO_LOAD_128BYTES;
						break;
					case (byte) 0xF8 :
						// Kernel: response to command to flash 128 bytes
						System.out.println("0xF8 response received, length " + rspDataLength);
						currentStatus = AccessStatus.FLASHED_128BYTES;
						break;
					default :
						// unrecognised response
						System.out.println("Unrecognised 0x7A response received, length " + rspDataLength);
						break;
				}

								
			}

			else if (rspAll[0] == (byte) 0x7F) {
				System.out.println("0x7F response received, rspCode: " + ((rspAll[1] & 0xF8) & 0xFF) + " error code: " + (rspAll[2] & 0xFF));

				switch(rspAll[2]) {
					case (byte) 0x30 :
						System.out.println("Kernel response indicates error: incorrect message format");
						break;
					case (byte) 0x31 :
						System.out.println("Kernel response indicates error: checksum error");
						break;
					case (byte) 0x32 :
						System.out.println("Kernel response indicates error: block number error");
						break;
					case (byte) 0x34 :
						System.out.println("Kernel response indicates error: unrecognised command in 0x7A message");
						break;
					case (byte) 0x35 :
						System.out.println("Kernel response indicates error: unrecognised non-0x7A message");
						break;
					case (byte) 0x36 :
						System.out.println("Kernel response indicates error: received message was overwritten before being read");
						break;
					case (byte) 0xA8 :
					case (byte) 0xA9 :
					case (byte) 0xAA :
					case (byte) 0xAB :
					case (byte) 0xAC :
					case (byte) 0xAD :
					case (byte) 0xAE :
						System.out.println("Kernel response indicates error: erase/flash microcode initialisation error");
						break;
					case (byte) 0x88 :
					case (byte) 0x89 :
					case (byte) 0x8A :
					case (byte) 0x8B :
					case (byte) 0x8C :
						System.out.println("Kernel response indicates error: erase/flash error");
						break;
					default:
						System.out.println("Kernel response indicates error, review code for more information");
						break;
				}
			}	
			
			else if ((rspAll[0] == (byte) 0xFF) && (rspAll[1] == (byte) 0xC8)) {

				System.out.println("0xFF 0xC8 response received, kernel will terminate");
				currentStatus = AccessStatus.KERNEL_DIED;

			}
			
			else System.out.println("Unrecognised response received");
            
        }
    }

	
	/**
     * simple function to pause execution to avoid flooding comms
     * 
     * 
     */
    public static void wait(int ms) {
	
		try {
			Thread.sleep(ms);
		}
		catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
	

	/**
     * Entry method for our demo programm
     * 
     * @param args Arguments
     */
    public static void main(String[] args) {
	
		try {

            // create the instances
            USBtin usbtin = new USBtin();
            USBtinSubProg subProg = new USBtinSubProg();

			int i, j, k;
			int loadAddress = 0xFFFF3000;
			int endAddress, flashCheckSum;
			byte msgAll[] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			CANMessage cantxmsg = new CANMessage(0xFFFFE, msgAll);
			if (args[0] == null) throw new USBtinException("no kernel file specified");
			String fileName = args[0];
			
			int fBlockStart7058[] = {
				0x00000000,	
				0x00001000,	
				0x00002000,	
				0x00003000,	
				0x00004000,	
				0x00005000,	
				0x00006000,	
				0x00007000,	
				0x00008000,	
				0x00020000,	
				0x00040000,	
				0x00060000,	
				0x00080000,	
				0x000A0000,	
				0x000C0000,	
				0x000E0000	
			};
			
			int fBlockLength7058[] = {
				0x00001000,
				0x00001000,
				0x00001000,
				0x00001000,
				0x00001000,
				0x00001000,
				0x00001000,
				0x00001000,
				0x00018000,
				0x00020000,
				0x00020000,
				0x00020000,
				0x00020000,
				0x00020000,
				0x00020000,
				0x00020000
			};
			
            // connect to USBtin and open CAN channel with 10kBaud in Active-Mode
            // for unix use usbtin.connect("/dev/ttyACM1"), for Windows use "COM3"
            usbtin.connect("COM3");
			usbtin.addMessageListener(subProg);

            // ECU operates on high speed CAN 500,000bps
			usbtin.openCANChannel(500000, USBtin.OpenMode.ACTIVE);

            // CAN message should enter bootloader if ECU is already on
			System.out.println("Ensure ECU is on, but car is not running, and press ENTER");
            System.in.read(new byte[2]);
            
            // send message to enter Denso CAN bootloader
			msgAll[0] = (byte) 0xFF;
			msgAll[1] = (byte) 0x86;
			cantxmsg.setData(msgAll);
			usbtin.send(cantxmsg);
			System.out.println("Enter bootloader command sent, giving time for ECU to enter bootloader...");

			wait(200);
			
			// send 0x90 command to check for successful comms with CAN bootloader
			msgAll[0] = (byte) 0x7A;
			msgAll[1] = (byte) 0x90;
			cantxmsg.setData(msgAll);
			usbtin.send(cantxmsg);
			System.out.println("0x90 message sent to bootloader to test communciation, waiting for response...");
			
			wait(200);
			if (currentStatus != AccessStatus.COMMS_OK) throw new USBtinException("Timed-out - no communication with ECU");

			// send 0x98 command to set copy address for kernel
			msgAll[0] = (byte) 0x7A;
			msgAll[1] = (byte) (0x98 + 0x04);
			msgAll[2] = (byte) ((loadAddress >> 24) & 0xFF);
			msgAll[3] = (byte) ((loadAddress >> 16) & 0xFF);
			msgAll[4] = (byte) ((loadAddress >> 8) & 0xFF);
			msgAll[5] = (byte) (loadAddress & 0xFF);
			cantxmsg.setData(msgAll);
			usbtin.send(cantxmsg);
			System.out.println("0x98 message sent to bootloader to set kernel load address, waiting for response...");

			wait(200);
			if (currentStatus != AccessStatus.READY_FOR_LOAD) throw new USBtinException("Timed-out or bad response");

			// read kernel file into a buffer and arrange into 6 byte blocks ready for upload
			if(!Files.exists(Path.of(fileName))) throw new USBtinException("kernel file not found");
			int kernelSize = (int) Files.size(Path.of(fileName));
			System.out.println("File length: " + kernelSize);
			byte[] kernelBytes = new byte[kernelSize + 6];
			byte[] bufferBytes = Files.readAllBytes(Paths.get(fileName));
			for(i = 0; i < kernelSize; i++) kernelBytes[i] = bufferBytes[i];
			int kernelBlocks = (int) (kernelSize / 6); // number of full 6 byte blocks
			if((kernelSize % 6) != 0) kernelBlocks++; // increase number of blocks by 1 for partial block at the end, note: this will result in 0x00 at the end
			endAddress = (loadAddress + (kernelBlocks * 6)) & 0xFFFFFFFF;
			int byteCounter = 0;
			checkSum = 0;
			msgAll[0] = 0x7A;
			msgAll[1] = (byte) (0xA8 + 0x06);
						
			System.out.println("kernel blocks: " + kernelBlocks + " end address: " + (endAddress & 0xFFFFFFFF));
							
			// send multiple 0xA8 commands to load the kernel
			for(i = 0; i < kernelBlocks; i++){
			
				for(j = 0; j < 6; j++){

					msgAll[2 + j] = kernelBytes[byteCounter + j];
					checkSum += (kernelBytes[byteCounter + j] & 0xFF);               
					checkSum = ((checkSum >> 8) & 0xFF) + (checkSum & 0xFF);

				}	

				byteCounter += 6;
				cantxmsg.setData(msgAll);
				usbtin.send(cantxmsg);
				System.out.print("\r0xA8 message sent to bootloader to load kernel for block:  " + i);
							
				wait(20);

			}
							
			System.out.println("All 0xA8 messages sent, checksum: " + checkSum);

			// send 0xB0 command to check checksum
			msgAll[0] = (byte) 0x7A;
			msgAll[1] = (byte) (0xB0 + 0x04);
			msgAll[2] = (byte) (((endAddress + 1) >> 24) & 0xFF);
			msgAll[3] = (byte) (((endAddress + 1) >> 16) & 0xFF);
			msgAll[4] = (byte) (((endAddress + 1) >> 8) & 0xFF);
			msgAll[5] = (byte) ((endAddress + 1) & 0xFF);
			cantxmsg.setData(msgAll);
			usbtin.send(cantxmsg);
			System.out.println("0xB0 message sent to bootloader to check checksum, waiting for response...");

			wait(200);
			if (currentStatus != AccessStatus.READY_FOR_JUMP) throw new USBtinException("Timed-out or bad response");

			// send 0xA0 command to jump into kernel
			msgAll[0] = (byte) 0x7A;
			msgAll[1] = (byte) (0xA0 + 0x04);
			msgAll[2] = (byte) ((endAddress >> 24) & 0xFF);
			msgAll[3] = (byte) ((endAddress >> 16) & 0xFF);
			msgAll[4] = (byte) ((endAddress >> 8) & 0xFF);
			msgAll[5] = (byte) (endAddress & 0xFF);
			cantxmsg.setData(msgAll);
			usbtin.send(cantxmsg);
			System.out.println("0xA0 message sent to bootloader to jump into kernel");

			System.out.println("ECU should now be running from kernel, entering simple CLI loop");

			boolean exitFlag = false;
			boolean realFlashFlag = false;
			boolean timeOutFlag = false;

			Scanner cli = new Scanner(System.in);

			// enter simple command line interface loop
			do {
				
				System.out.print("Enter command (dump/pflash/rflash/exit): ");	
				String userEntry = cli.nextLine();
				
				switch(userEntry) {
					
					case "dump" :
						System.out.print("Enter filename for dumpfile: ");
						String dumpFile = cli.nextLine();
						
						System.out.print("Enter dump start address in hex (eg: 0x0), will be clipped to 256 byte multiple: ");
						String dumpStartString = cli.nextLine();
						int dumpStart = Integer.decode(dumpStartString);
						dumpStart &= 0xFFFFFF00;
						
						System.out.print("Enter dump length in hex (eg: 0x100), will be clipped to 256 byte multiple: ");
						String dumpLengthString = cli.nextLine();
						int dumpLength = Integer.decode(dumpLengthString);
						dumpLength &= 0xFFFFFF00;
						System.out.println("dumpLength: " + dumpLength);
						if ((dumpLength <= 0) || (dumpLength > 1048576)) throw new USBtinException("Dump length invalid"); 
						byte writeBytes[] = new byte[dumpLength];
						
						int dumpChunks = dumpLength / 256;
						System.out.println("dumpChunks: " + dumpChunks);

						dumpLength = 256;
						for (i = 0; i < dumpChunks; i++) {
						
							// send 0xD0 command to kernel to get checksum of dump chunk
							msgAll[0] = (byte) 0x7A;
							msgAll[1] = (byte) (0xD0 + 0x06);
							msgAll[2] = (byte) ((dumpLength >> 24) & 0xFF);
							msgAll[3] = (byte) ((dumpLength >> 16) & 0xFF);
							msgAll[4] = (byte) ((dumpLength >> 8) & 0xFF);
							msgAll[5] = (byte) ((dumpStart >> 24) & 0xFF);
							msgAll[6] = (byte) ((dumpStart >> 16) & 0xFF);
							msgAll[7] = (byte) ((dumpStart >> 8) & 0xFF);			
							cantxmsg.setData(msgAll);
							usbtin.send(cantxmsg);
							//System.out.println("msgAll: " + msgAll[0] + " " + msgAll[1] + " " + msgAll[2] + " " + msgAll[3] + " " + msgAll[4] + " " + msgAll[5] + " " + msgAll[6] + " " + msgAll[7]);
							System.out.println("0xD0 message sent to kernel to get checksum over dump region");						

							dumpByteCounter = 0;

							// send 0xD8 command to kernel to dump the chunk from ROM
							msgAll[0] = (byte) 0x7A;
							msgAll[1] = (byte) (0xD8 + 0x06);
							msgAll[2] = (byte) ((dumpLength >> 24) & 0xFF);
							msgAll[3] = (byte) ((dumpLength >> 16) & 0xFF);
							msgAll[4] = (byte) ((dumpLength >> 8) & 0xFF);
							msgAll[5] = (byte) ((dumpStart >> 24) & 0xFF);
							msgAll[6] = (byte) ((dumpStart >> 16) & 0xFF);
							msgAll[7] = (byte) ((dumpStart >> 8) & 0xFF);			
							cantxmsg.setData(msgAll);
							usbtin.send(cantxmsg);
							//System.out.println("msgAll: " + msgAll[0] + " " + msgAll[1] + " " + msgAll[2] + " " + msgAll[3] + " " + msgAll[4] + " " + msgAll[5] + " " + msgAll[6] + " " + msgAll[7]);
							System.out.println("0xD8 message sent to kernel initiate dump");						

							// wait for all data in the chunk to be received
							dumpTimeOut = 0;
							timeOutFlag = false;
							while((dumpByteCounter < dumpLength) && !timeOutFlag) {
								System.out.print("\rdumpByteCounter: " + ((i * 256) + dumpByteCounter));
								dumpTimeOut++;
								if (dumpTimeOut > 50000) timeOutFlag = true; 
							};  
							
							//System.out.println("dumpByteCounter: " + dumpByteCounter);
					
							int writeCheckSum = 0;
							for (j = 0; j < dumpLength; j++) {
								writeBytes[(i * 256) + j] = dumpBytes[j];
								writeCheckSum += (dumpBytes[j] & 0xFF);               
								writeCheckSum = ((writeCheckSum >> 8) & 0xFF) + (writeCheckSum & 0xFF);
							}

							if ((writeCheckSum != checkSum) || timeOutFlag) {
								System.out.println("dump timed out or had a checksum error, repeating 256 byte chunk");
								i--;
							} 
							else {
								System.out.println("dump checksum ok for chunk: " + i + " moving to next chunk");
								dumpStart += 256;
							}
						
							dumpingBlocks = false;
							
						}
						
						System.out.println("all chunks dumped, writing to dump file");
						File file = new File(dumpFile);
						file.createNewFile();
						Files.write(Path.of(dumpFile), writeBytes);
						
						break;
					
					case "rflash" :
						realFlashFlag = true;
					
					case "pflash" :
						System.out.print("Enter filename with old rom: ");	
						String oldRomFile = cli.nextLine();
						
						System.out.print("Enter filename with new rom: ");
						String newRomFile = cli.nextLine();
					
						if(!Files.exists(Path.of(oldRomFile))) throw new USBtinException("old ROM file not found");
						if(!Files.exists(Path.of(newRomFile))) throw new USBtinException("new ROM file not found");
						
						int oldRomFileSize = (int) Files.size(Path.of(oldRomFile));
						int newRomFileSize = (int) Files.size(Path.of(newRomFile));
						
						if (oldRomFileSize != newRomFileSize) throw new USBtinException("ROM file sizes don't match");
						if (newRomFileSize != 1048576) throw new USBtinException("ROM file size not exactly 1MB");
						
						byte[] oldRomBytes = Files.readAllBytes(Paths.get(oldRomFile));
						byte[] newRomBytes = Files.readAllBytes(Paths.get(newRomFile));
						
						boolean blockChangedFlag[] = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
						int blockNumber;
						byteCounter = 0;
						
						for (blockNumber = 0; blockNumber < 16; blockNumber++) {
							for (i = 0; i < fBlockLength7058[blockNumber]; i++) {
								if(oldRomBytes[byteCounter] != newRomBytes[byteCounter]) blockChangedFlag[blockNumber] = true;
								byteCounter++;
							}
							if (blockChangedFlag[blockNumber]) System.out.println("Change detected in block: " + blockNumber);
						}
						
						System.out.println("Proceeding to flash only changed blocks, press ENTER to continue");
						System.in.read(new byte[2]);
						
						// send 0xE0 command to initialise erasing / flashing microcodes
						msgAll[0] = (byte) 0x7A;
						msgAll[1] = (byte) (0xE0 + 0x01);
						if (realFlashFlag) msgAll[2] = (byte) 0xA5;
						else msgAll[2] = (byte) 0x00;
						cantxmsg.setData(msgAll);
						usbtin.send(cantxmsg);
						System.out.println("msgAll: " + msgAll[0] + " " + msgAll[1] + " " + msgAll[2] + " " + msgAll[3] + " " + msgAll[4] + " " + msgAll[5] + " " + msgAll[6] + " " + msgAll[7]);
						
						System.out.println("0xE0 message sent to kernel to initialise erasing / flashing microcodes");		

						wait(200);
						//if (currentStatus != AccessStatus.READY_FOR_ERASE) throw new USBtinException("Timed out or bad response"); 
			
						for (blockNumber = 0; blockNumber < 16; blockNumber++) {
							
							if(currentStatus != AccessStatus.READY_FOR_ERASE) throw new USBtinException("Not ready for block erasing");
							
							// if block has not changed, then go to the next block
							if (!blockChangedFlag[blockNumber]) continue;
							
							System.out.println("Proceeding to attempt erase and flash of block number: " + blockNumber);
							int num128ByteBlocks = (fBlockLength7058[blockNumber] >> 7) & 0xFFFFFFFF;

							// send 0xF0 command to erase block
							msgAll[0] = (byte) 0x7A;
							msgAll[1] = (byte) (0xF0 + 0x06);
							msgAll[2] = (byte) (blockNumber & 0xFF);	
							msgAll[3] = (byte) ((fBlockStart7058[blockNumber] >> 24) & 0xFF);
							msgAll[4] = (byte) ((fBlockStart7058[blockNumber] >> 16) & 0xFF);
							msgAll[5] = (byte) ((fBlockStart7058[blockNumber] >> 8) & 0xFF);
							msgAll[6] = (byte) ((num128ByteBlocks >> 8) & 0xFF);
							msgAll[7] = (byte) (num128ByteBlocks & 0xFF);
							cantxmsg.setData(msgAll);
							usbtin.send(cantxmsg);
							System.out.println("0xF0 message sent to kernel to erase block number: " + blockNumber);											

							wait(500);
							//if (currentStatus != AccessStatus.READY_TO_LOAD_128BYTES) throw new USBtinException("Timed out or bad response");
			
							int byteIndex = fBlockStart7058[blockNumber] & 0xFFFFFFFF;

							for (i = 0; i < num128ByteBlocks; i++) {

								flashCheckSum = 0;
								if(currentStatus != AccessStatus.READY_TO_LOAD_128BYTES) throw new USBtinException("Not ready for 128byte block writing");
								
								for (j = 0; j < 16; j++) {

									// send 16 lots of 8 byte pure data messages to load and flash the new block (16 x 8 bytes = 128 bytes)
									for (k = 0; k < 8; k++) {
									
										msgAll[k] = (byte) (newRomBytes[byteIndex + k] & 0xFF);
										flashCheckSum += (msgAll[k] & 0xFF);               
										flashCheckSum = ((flashCheckSum >> 8) & 0xFF) + (flashCheckSum & 0xFF);
										
									}
									
									byteIndex += 8;
									cantxmsg.setData(msgAll);
									usbtin.send(cantxmsg);
									System.out.print("\rFlash data message sent to kernel for block (128 byte block, 8 byte block):  " + i + ", " + j);

									wait(5);

								}
								
								// send 0xF8 command to check and flash 128 bytes
								msgAll[0] = (byte) 0x7A;
								msgAll[1] = (byte) (0xF8 + 0x03);
								msgAll[2] = (byte) ((i >> 8) & 0xFF);
								msgAll[3] = (byte) (i & 0xFF);
								msgAll[4] = (byte) (flashCheckSum & 0xFF);	
								
								cantxmsg.setData(msgAll);
								usbtin.send(cantxmsg);
								System.out.println();
								System.out.println("0xF8 command sent to kernel to check and flash 128 byte block");

								wait(200);
								
								// check for flash success or not
								if (currentStatus == AccessStatus.FLASHED_128BYTES) {
									System.out.println("Flashing of 128 byte block successful, proceeding to next 128 byte block");
								} else {
									System.out.println("Flashing of 128 byte block unsuccessful, proceeding to next 128 byte block");
								}

								currentStatus = AccessStatus.READY_TO_LOAD_128BYTES;
							
							}
							
							currentStatus = AccessStatus.READY_FOR_ERASE;
							
						}
						
						realFlashFlag = false;
						System.out.println("Flashing complete");
				
						break;
					
					case "exit" :
						exitFlag = true;
						msgAll[0] = (byte) 0xFF;
						msgAll[1] = (byte) 0xC8;
						cantxmsg.setData(msgAll);
						usbtin.send(cantxmsg);
						System.out.println("0xFF 0xC8 command sent to terminate kernel and reset ECU");
						
						wait(200);
						
						if (currentStatus == AccessStatus.KERNEL_DIED) System.out.println("Kernel has now terminated, ECU will have reset, cli exiting");
						else throw new USBtinException("Timed-out or bad response");
						
						break;

					default :
						System.out.println("Unrecognised command");
						break;
				}
				
			} while (!exitFlag);	
			
			// close the CAN channel and close the connection
            usbtin.closeCANChannel();
            usbtin.disconnect();

        } catch (USBtinException ex) {
            
            // Ohh.. something goes wrong while accessing/talking to USBtin           
            System.err.println(ex);            
            //usbtin.closeCANChannel();
            //usbtin.disconnect();
			System.exit(0);
            
        } catch (java.io.IOException ex) {
            
            // this we need because of the System.in.read()
            System.err.println(ex);
            //usbtin.closeCANChannel();
            //usbtin.disconnect();
			System.exit(0);

        }
    }
}
