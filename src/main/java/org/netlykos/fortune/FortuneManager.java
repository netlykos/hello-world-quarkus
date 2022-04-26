package org.netlykos.fortune;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
  private static final Charset UTF_8 = StandardCharsets.UTF_8;
  private static final String DAT_FILE_SUFFIX = ".dat";
  private static final String NEW_LINE = System.getProperty("line.separator");
  private static final String UNIX_NEW_LINE = "\\n";
  private static final int MAX_BUFFER_SIZE = 4096;
  private static final int FORTUNE_PADDING = 3; // every fortune is padded by '\n%\n'

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
    String category = this.fortunes.get(RANDOM.nextInt(fortuneResources.size()));
    return getRandomCookieFromCategory(category);
  }

  public List<String> getFortune(String category) {
    LOGGER.debug("Looking for cookie in category {}", category);
    if (!this.fortunes.contains(category)) {
      throw new IllegalArgumentException(format("No fortunes for category [%s] available.", category));
    }
    return getRandomCookieFromCategory(category);
  }

  public List<String> getFortune(String category, int cookie) {
    int cookieOffset = cookie - 1;
    LOGGER.debug("Looking for cookie # {}, offset {} from category {}", cookie, cookieOffset, category);
    if (!this.fortunes.contains(category)) {
      throw new IllegalArgumentException(format("Category %s is not setup.", category));
    }
    FortuneFileRecord structFile = fortuneResources.get(category);
    Integer totalRecords = structFile.totalRecords();
    LOGGER.debug("For category {}, total records {}", category, totalRecords);
    if (cookieOffset < 0) {
      throw new IllegalArgumentException("Cookie number should be positive.");
    }
    if (cookie > totalRecords) {
      throw new IllegalArgumentException(format("Category %s only contains %d cookie(s).", category, totalRecords));
    }
    return getCookieNumberFromRecord(structFile, cookieOffset);
  }

  private List<String> getRandomCookieFromCategory(String category) {
    FortuneFileRecord structFile = fortuneResources.get(category);
    int totalRecords = structFile.totalRecords();
    int luckyCookie = RANDOM.nextInt(totalRecords);
    LOGGER.debug("Selected cookie {} from {} for category {}.", luckyCookie, totalRecords, category);
    return getCookieNumberFromRecord(structFile, luckyCookie);
  }

  private List<String> getCookieNumberFromRecord(FortuneFileRecord structFile, int cookie) {
    List<Integer> records = structFile.records();
    int byteOffsetStart = records.get(cookie);
    // if we are reading the last record from the data file then read till the end of the file, else read till the next cookie
    int byteOffsetEnd = structFile.fileContent().capacity();
    if (cookie < records.size()) {
      byteOffsetEnd = records.get(cookie + 1);
    }
    // remove the FORTUNE_PADDING length from the bytes to read
    int totalLength = byteOffsetEnd - byteOffsetStart - FORTUNE_PADDING;
    LOGGER.debug("Cookie {} of {}, reading {} byte(s) from byte offset {} to {}",
        cookie, records.size(), totalLength, byteOffsetStart, byteOffsetEnd);
    byte[] byteCookie = structFile.getFileContent(byteOffsetStart, totalLength);
    return Arrays.asList(new String(byteCookie, UTF_8).split(UNIX_NEW_LINE));
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
