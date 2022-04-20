package org.netlykos.fortune;

import static org.netlykos.fortune.FortuneManager.NEW_LINE;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FortuneController {

  @Autowired
  FortuneManager fortuneManager;

  @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public String fortune() {
    return fortuneManager.getRandomFortune().stream().collect(Collectors.joining(NEW_LINE));
  }

}
