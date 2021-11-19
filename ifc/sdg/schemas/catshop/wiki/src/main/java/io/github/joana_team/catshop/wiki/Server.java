package io.github.joana_team.catshop.wiki;

import edu.kit.joana.ui.annotations.EntryPoint;
import io.github.joana_team.catshop.model.CatWithPersonalities;
import io.github.joana_team.catshop.model.Personality;
import io.github.joana_team.catshop.wiki.server.impl.WikiApiServiceImpl;

import static edu.kit.joana.microservices.Server.run;

public class Server {
  @EntryPoint
  public static void main(String[] args) {
    run("http://localhost:90ß0/", new WikiApiServiceImpl(), Personality.class, CatWithPersonalities.class);
  }
}
