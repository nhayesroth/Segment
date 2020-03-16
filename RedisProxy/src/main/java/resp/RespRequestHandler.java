package resp;

import cache.CallableGet;
import cache.LruCache;
import com.google.common.collect.ImmutableList;
import configuration.Configuration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resp.RespExceptions.RespException;
import server.Server;

/**
 * Handles one or more RESP commands sent in a single request.
 * <p>
 * If more than one command is sent in a single request, the commands are split
 * and processed in parallel, before their results are returned serially in the
 * same order as they were received.
 */
public class RespRequestHandler implements Runnable {

  private static final Logger logger =
      LoggerFactory.getLogger(Server.class.getName());
  private final ExecutorService threadPool;
  private Socket socket;
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final LruCache cache;
  private final int maxPipelinedRespCommands;

  public RespRequestHandler(
      Socket socket,
      LruCache cache,
      Configuration configuration)
      throws IOException {
    threadPool =
        Executors.newFixedThreadPool(configuration.maxPipelinedRespCommands());
    this.socket = socket;
    this.inputStream = socket.getInputStream();
    this.outputStream = socket.getOutputStream();
    this.cache = cache;
    this.maxPipelinedRespCommands = configuration.maxPipelinedRespCommands();
  }

  @Override
  public void run() {
    try {
      logger.info("RESP request handler running...");
      ImmutableList<RespRequest> requests = parseRequests();
      List<Future<Optional<String>>> futures =
          processAllRequestsInParallel(requests);
      writeToOutput(futures);
    } catch (RespException e) {
      writeToOutput(e.getMessage() + "\r\n");
    } catch (Exception e) {
      logger.info("RESP request handler encountered an exception!");
      writeToOutput(
          String.format("-ERR internal error - %s - %s", e.getClass(), e.getMessage()));
    } finally {
      closeSocket();
      threadPool.shutdown();
    }
  }

  private ImmutableList<RespRequest> parseRequests()
      throws IOException, RespException {
    ImmutableList<RespRequest> requests = RespProtocol.parse(inputStream);
    logger.info("Parsed {} requests", requests.size());
    if (requests.size() > maxPipelinedRespCommands) {
      throw new RuntimeException(
          String.format(
              "Number of pipelined requests (%d) exceeds the limit (%d): %s",
              requests.size(),
              maxPipelinedRespCommands,
              requests));
    }
    return requests;
  }

  private ImmutableList<Future<Optional<String>>> processAllRequestsInParallel(
      List<RespRequest> requests) throws InterruptedException {
    return ImmutableList.copyOf(
        threadPool.invokeAll(
            requests.stream()
                .map(request -> new CallableGet(cache, request.key()))
                .collect(ImmutableList.toImmutableList())));
  }

  private void writeToOutput(List<Future<Optional<String>>> futures)
      throws InterruptedException, ExecutionException {
    for (Future<Optional<String>> future : futures) {
      Optional<String> result = future.get();
      String response = RespProtocol.toBulkString(result);
      writeToOutput(response);
    }
  }

  private void writeToOutput(String response) {
    try {
      outputStream.write(response.getBytes("UTF-8"));
      logger.info(
          "Wrote to outputStream: {}",
          RespProtocol.formatNewLineChars(response));
    } catch (IOException e) {
      logger.warn(
          "Encountered exception writing to output: ",
          RespProtocol.formatNewLineChars(response));
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void closeSocket() {
    try {
      socket.close();
      logger.debug("Closed socket {}", socket);
    } catch (IOException e) {
      logger.warn("Encountered exception while closing socket.");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
