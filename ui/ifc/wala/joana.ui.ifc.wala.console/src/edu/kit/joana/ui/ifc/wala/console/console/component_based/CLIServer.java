package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.kit.joana.component.connector.JoanaCall;
import edu.kit.joana.component.connector.JoanaCallReturn;
import edu.kit.joana.component.connector.Util;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static edu.kit.joana.component.connector.JoanaCall.SERVER_PORT;

/**
 * Starts a server that receives {@link JoanaCall} zips and print the JSON version of the resulting {@link JoanaCallReturn}
 */
public class CLIServer {

  private static Logger LOGGER = Logger.getLogger("CLIServer");
  private static class ServerHttpHandler implements HttpHandler {

    @Override public void handle(HttpExchange exchange) throws IOException {
      LOGGER.info(String.format("Handle %s", exchange.getRemoteAddress()));
      LOGGER.info("Create tmp file");
      Path tmpFile = Files.createTempFile("", ".zip");
      try {
        LOGGER.info("Copy file");
        IOUtils.copy(exchange.getRequestBody(), Files.newOutputStream(tmpFile));
        LOGGER.info("Load zip file");
        JoanaCall.loadZipFile(tmpFile, c -> {
          Logger.getGlobal().setLevel(c.logLevel);
          LOGGER.info("Process JoanaCall");
          JoanaCallReturn joanaCallReturn = new BasicFlowAnalyzer().processJoanaCall(c);
          String response = Util.toJson(joanaCallReturn);
          try {
            exchange.getResponseHeaders()
                .set("Content-Type", String.format("application/json; charset=%s", StandardCharsets.UTF_8));
            final byte[] rawResponseBody = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, rawResponseBody.length);
            exchange.getResponseBody().write(rawResponseBody);
          } catch (IOException e) {
            e.printStackTrace();
          } finally {
            exchange.close();
          }
          exchange.close();
        });
      } catch (RuntimeException ex) {
        ex.printStackTrace();
        throw ex; // seems to be caught silently be the server
      }
      Files.delete(tmpFile);
    }
  }

  static void run(){
    run(SERVER_PORT, 1);
  }

  static void run(int port, int threads){
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), port), 0);
      System.out.println(String.format("Running on %s on port %s", InetAddress.getLocalHost(), port));
      server.createContext("/", new ServerHttpHandler());
      server.setExecutor(threads == 1 ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(threads));
      server.start();
      Thread.currentThread().join();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println(String.format("Usage: program [PORT|default is %s] [THREADS|default is 1]", SERVER_PORT));
    run(args.length == 0 ? SERVER_PORT : Integer.parseInt(args[0]), args.length == 2 ? Integer.parseInt(args[1]) : 1);
  }

}
