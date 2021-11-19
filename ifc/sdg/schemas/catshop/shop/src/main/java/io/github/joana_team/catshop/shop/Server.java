package io.github.joana_team.catshop.shop;

import edu.kit.joana.ui.annotations.EntryPoint;
import io.github.joana_team.catshop.model.CatWithPersonalities;
import io.github.joana_team.catshop.model.Personality;
import io.github.joana_team.catshop.shop.server.impl.ShopApiServiceImpl;

import static edu.kit.joana.microservices.Server.run;

public class Server {
  @EntryPoint
  public static void main(String[] args) {
    run("http://localhost:9010/", new ShopApiServiceImpl(), Personality.class, CatWithPersonalities.class);
  }
}
