package org.netlykos.fortune;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record FortuneFileRecord(String cookieName, ByteBuffer fileContent, Integer totalRecords, List<Integer> records) {

  private static final Logger LOGGER = LoggerFactory.getLogger(FortuneFileRecord.class);

  public synchronized byte[] getFileContent(int byteOffSet, int length) {
    byte[] content = new byte[length];
    this.fileContent.position(byteOffSet);
    this.fileContent.get(content, 0, length);
    return content;
  }

  public static FortuneFileRecord build(String cookieName, byte[] structFile, byte[] dataFile) {
    ByteBuffer fileContent = ByteBuffer.wrap(dataFile);
    ByteBuffer header = ByteBuffer.wrap(structFile, 0, 24);
    header.order(ByteOrder.BIG_ENDIAN);
    ByteBuffer content = ByteBuffer.wrap(structFile, 24, structFile.length - 24);
    content.order(ByteOrder.BIG_ENDIAN);
    // IIIIIcxxx (24 bytes structure)
    int versionNumber = header.getInt();
    int totalRecords = header.getInt();
    int shortestRecord = header.getInt();
    int longestRecord = header.getInt();
    int flag = header.getInt();
    Character delimiter = header.getChar();
    LOGGER.debug("Version number {}, total records {}, shortest record {}, longest record {}, flag {}, delimiter {}",
        versionNumber, totalRecords, longestRecord, shortestRecord, flag, delimiter);
    List<Integer> records = new ArrayList<>();
    while (content.hasRemaining()) {
      records.add(content.getInt());
    }
    LOGGER.debug("Byte offsets {}", records);
    return new FortuneFileRecord(cookieName, fileContent, totalRecords, records);
  }
}
