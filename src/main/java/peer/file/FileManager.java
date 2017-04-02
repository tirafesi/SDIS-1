package peer.file;

import peer.channel.*;
import peer.message.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

/**
* A file manager to handle file transfers
*/
public class FileManager {

  /**
  * Constructor
  */
  public FileManager() {

  }


  /**
  * Returns the ID of a given file
  *
  * @param filepath Path to the file
  *
  * @return File ID
  */
  public String getFileId(String filepath) {

    Path path = Paths.get(filepath);

    try {

      // get file attrs
      BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

      // generate a string containing attrs
      String rawId = filepath + attrs.size() + attrs.creationTime() + attrs.lastModifiedTime();

      // convert string to bitstring
      String bitId = stringToBitstring(rawId);

      // hash bitstring
      String hashedId = hashString(bitId);

      return hashedId;
    }
    catch (Exception e) {
      System.out.println("FileManager: Error creating file id: " + e);
      return null;
    }

  }


  /**
  * Converts a given string into a bitstring
  *
  * @param str String to convert
  *
  * @return Bitstring
  */
  public String stringToBitstring(String str) {

    // get bytes from string
    byte[] bytes = str.getBytes();

    // init output
    String bitstring = "";

    // loop each byte and turn them into bits
    for (byte b: bytes) {
      bitstring += Integer.toBinaryString(b);
    }

    return bitstring;
  }


  /**
  * Hashes a string by applying SHA256
  *
  * @param str String to hash
  *
  * @return Hashed string
  */
  public String hashString(String str) {

    try {

      // create message digest sha256
      MessageDigest md = MessageDigest.getInstance("SHA-256");

      // get hashed byte array
      byte[] hashed = md.digest(str.getBytes());

      // return hashed string
      return DatatypeConverter.printHexBinary(hashed);
    }
    catch (Exception e) {

      System.out.println("FileManager: Error hashing string: " + e);
      return null;
    }
  }


  /**
  * Backs up the given file
  * by splitting it into 64Kb chunks
  * and asking other peers to store them
  *
  * @param filepath Path to the file to backup
  */
  public void backup(String filepath) {

    File file = new File(filepath);

    long filesize = file.length();

    // init array
    byte[] chunk = new byte[Message.CHUNK_SIZE];

    try {

      FileInputStream fis = new FileInputStream(file);

      String fileId = getFileId(filepath);

      int chunkNo = 0;

      // read file into chunks
      while (fis.read(chunk) > 0) {

        PutChunkMessage msg = new PutChunkMessage("1.0", "1", fileId, Integer.toString(chunkNo), "1", chunk);

        // add this message to waiting "queue"
        /*synchronized (ControlChannelListener.waitingConfirmation) {

          ControlChannelListener.waitingConfirmation.add(msg);

          System.out.println(ControlChannelListener.waitingConfirmation.size());
        }*/

        // send message to MDB channel
        msg.send();

        // prepare next ite
        chunkNo++;
      }

      // If the file size is a multiple of the chunk size,
      // the last chunk has size 0
      if (filesize % Message.CHUNK_SIZE == 0) {

        // get message to send to multicast channel
        PutChunkMessage lastmsg = new PutChunkMessage("1.0", "1", fileId, Integer.toString(chunkNo), "1", new byte[0]);

        // send message
        lastmsg.send();
      }

    }
    catch (Exception e) {
      System.out.println("FileManager: Error opening/reading file " + filepath + ": " + e);
    }

  }
}
