package org.netlykos.fortune;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FortuneController {

  private static final String NEW_LINE = System.getProperty("line.separator");

  @Autowired
  FortuneManager fortuneManager;

  @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public String fortune() {
    return fortuneManager.getRandomFortune().stream().collect(Collectors.joining(NEW_LINE));
  }

}
