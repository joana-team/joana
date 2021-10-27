package io.github.joana_team.catshop.wiki;

import io.github.joana_team.catshop.model.CatWithPersonalities;
import io.github.joana_team.catshop.model.Personality;
import io.github.joana_team.catshop.wiki.server.impl.WikiApiServiceImpl;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

public class Server {
  public static void main(String[] args) throws Exception {
    JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
    factory.setResourceClasses(Personality.class);
    factory.setResourceClasses(CatWithPersonalities.class);
    factory.setResourceClasses(WikiApiServiceImpl.class);
    factory.setResourceProvider(WikiApiServiceImpl.class,
        new SingletonResourceProvider(new WikiApiServiceImpl()));
    factory.setProvider(new org.codehaus.jackson.jaxrs.JacksonJsonProvider());
    factory.setAddress("http://localhost:9000/");
    factory.create();

    System.out.println("Server ready...");
    Thread.sleep(5 * 60 * 1000);

    System.out.println("Server exiting ...");
    System.exit(0);
  }
}