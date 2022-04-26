package org.netlykos.fortune;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FortuneManagerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(FortuneManagerTest.class);

  @Inject
  FortuneManager fortuneManager;

  @BeforeEach
  public void init() {
    assertNotNull(fortuneManager);
  }

  @Test
  public void testGetRandomFortune() {
    List<String> fortune = fortuneManager.getRandomFortune();
    LOGGER.debug("{}", fortune);
    assertNotNull(fortune);
  }

  @Test
  public void testGetFortuneSuccess() {
    String category = "art";
    int cookie = 3;
    List<String> expect = Arrays.asList("A celebrity is a person who is known for his well-knownness.");
    List<String> actual = fortuneManager.getFortune(category, cookie);
    LOGGER.debug("{}", actual);
    assertEquals(expect.size(), actual.size());
    assertEquals(expect.get(0), actual.get(0));
  }

  @Test
  public void testGetFortuneFailureBadCategory() {
    String category = "not_a_valid_category";
    String expected = String.format("Category %s is not setup.", category);
    IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> {
      fortuneManager.getFortune(category, 1);
    });
    assertEquals(expected, actual.getMessage());
  }

  @Test
  public void testGetFortuneFailureNegativeCookie() {
    String category = "art";
    int cookie = -1;
    String expected = "Cookie number should be positive.";
    IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> {
      fortuneManager.getFortune(category, cookie);
    });
    assertEquals(expected, actual.getMessage());
  }

  @Test
  public void testGetFortuneFailureOverflowCookie() {
    String category = "art";
    int range = 465, cookie = range + 1;
    String expected = String.format("Category %s only contains %d cookie(s).", category, range);
    IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> {
      fortuneManager.getFortune(category, cookie);
    });
    assertEquals(expected, actual.getMessage());
  }

  @Test
  public void testGetFortuneSuccessEdge() {
    String category = "art";
    int cookie = 465;
    List<String> expect = Arrays.asList(
        "\"Hiro has two loves, baseball and porn, but due to an elbow injury he",
        "gives up baseball....\"",
        "  -- AniDB description of _H2_, with selective quoting applied.",
        "     http://anidb.info/perl-bin/animedb.pl?show=anime&aid=352");
    List<String> actual = fortuneManager.getFortune(category, cookie);
    LOGGER.debug("{}", actual);
    assertEquals(expect.size(), actual.size());
    for (int i = 0; i < expect.size(); i++) {
      assertEquals(expect.get(i), actual.get(i));
    }
  }

}