package org.netlykos.fortune;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FortuneManagerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(FortuneManagerTest.class);

  @Inject
  FortuneManager fortuneManager;

  @Test
  public void testGetRandomFortune() {
    assertNotNull(fortuneManager);
    List<String> fortune = fortuneManager.getRandomFortune();
    LOGGER.debug("{}", fortune);
    assertNotNull(fortune);
  }

}