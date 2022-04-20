package org.netlykos.fortune;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FortuneManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(FortuneManager.class);
  private static final SecureRandom RANDOM = getSecureRandomInstance();

  public static final String DAT_FILE_SUFFIX = ".dat";
  public static final int MAX_BUFFER_SIZE = 4096;
  public static final String NEW_LINE = System.getProperty("line.separator");
  public static final int FORTUNE_PADDING = 3; // every fortune is padded by '\n%\n'

  @Value("${org.netlykos.fortune.directory:/fortune}")
  String fortuneDirectory;

  private Map<String, FortuneFileRecord> fortuneResources = new HashMap<>();
  private List<String> fortunes = new ArrayList<>();

  @PostConstruct
  public void init() {
    LOGGER.debug("Looking for data files in {}", fortuneDirectory);
    try {
      byte[] content = getResourceContent(fortuneDirectory);
      try (BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(new ByteArrayInputStream(content)))) {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
          LOGGER.debug("Read {} in directory {}", line, fortuneDirectory);
          if (line.endsWith(DAT_FILE_SUFFIX)) {
            String cookieName = line.replace(DAT_FILE_SUFFIX, "");
            byte[] structFileContent = getResourceContent(format("%s%s%s", fortuneDirectory, File.separator, line));
            byte[] dataFileContent = getResourceContent(format("%s%s%s", fortuneDirectory, File.separator, cookieName));
            if (dataFileContent == null || structFileContent == null) {
              continue;
            }
            FortuneFileRecord structFile = FortuneFileRecord.build(cookieName, structFileContent, dataFileContent);
            fortuneResources.put(cookieName, structFile);
            fortunes.add(cookieName);
          }
        }
      }
    } catch (IOException ioe) {
      throw new IllegalStateException("Failed to read fortune files from directory.", ioe);
    }
  }

  public List<String> getRandomFortune() {
    String cookieFile = this.fortunes.get(RANDOM.nextInt(fortuneResources.size()));
    LOGGER.debug("Returning cookie from {}", cookieFile);
    FortuneFileRecord structFile = fortuneResources.get(cookieFile);
    List<Integer> totalRecords = structFile.records();
    int luckyCookie = RANDOM.nextInt(totalRecords.size());
    int byteOffsetStart = totalRecords.get(luckyCookie);
    // if we are reading the last record from the data file then read till the end of the file, else read till the next cookie
    int byteOffsetEnd = structFile.fileContent().capacity();
    if (luckyCookie < totalRecords.size()) {
      byteOffsetEnd = totalRecords.get(luckyCookie + 1);
    }
    int totalLength = byteOffsetEnd - byteOffsetStart - FORTUNE_PADDING;
    LOGGER.debug("Lucky cookie {} from {}, reading {} byte(s) from byte offset {} to {}",
        luckyCookie, totalRecords.size(), totalLength, byteOffsetStart, byteOffsetEnd);
    byte[] byteCookie = structFile.getFileContent(byteOffsetStart, totalLength);
    return Arrays.asList(new String(byteCookie).split(NEW_LINE));
  }

  static byte[] getResourceContent(String resourcePath) throws IOException {
    File file = new File(resourcePath);
    if (file.exists()) {
      return getResourceContent(file);
    }
    InputStream inputStream = FortuneManager.class.getResourceAsStream(resourcePath);
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[MAX_BUFFER_SIZE];
      int bytesRead = 0;
      // Note: Don't be complacent and use available() - that just tells if the data cannot be read while blocking. You need to check for the eof marker -1
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        bos.write(buffer, 0, bytesRead);
      }
      return bos.toByteArray();
    }
  }

  static byte[] getResourceContent(File file) throws IOException {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      StringBuilder sb = new StringBuilder();
      Stream.of(files).forEach(f -> sb.append(f.getName()).append(NEW_LINE));
      return sb.toString().getBytes();
    }
    return Files.readAllBytes(file.toPath());
  }

  static SecureRandom getSecureRandomInstance() {
    try {
      return SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to create an instance of SecureRandom.", e);
    }
  }

}
