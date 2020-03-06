package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import server.Canvas.Mode;
import server.Request.Type;

/** Thread that handles all requests for one client session. */
public class RequestHandler implements Runnable {

  final int sessionId;

  private final Logger logger;
  private final Socket socket;
  private final BufferedReader inputReader;
  private final OutputStream outputStream;

  private Canvas canvas;

  public RequestHandler(int sessionId, Socket socket) throws IOException {
    this.logger = new Logger(sessionId);
    this.socket = socket;
    this.sessionId = sessionId;
    this.inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));;
    this.outputStream = socket.getOutputStream();
    this.canvas = new Canvas(sessionId);
    logger.log("Spawned new RequestHandler.");
    clearCanvas();
  }

  @Override
  public void run() {
    try {
      handshake();
      handleRequests();
    } catch (Exception e) {
      closeSocket();
    }
  }

  private void clearCanvas() {
    canvas.clear();
    logger.log("Canvas cleared.");
  }

  private void closeSocket() {
    try {
      socket.close();
      logger.log("Closed socket %s", socket);
    } catch (IOException e) {
      logger.log("Encountered exception while closing socket.");
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void handleCoordRequest() {
    Coordinates c = canvas.getCurrentCoordinates();
    writeToOutput(c.toString());
  }

  private void handleInvalidRequest(Request request) {
    logger.log("Received invalid command: %s", request.line);
  }

  private void handleRenderRequest() {
    logger.log("Rendering canvas...");
    canvas.getListOfStringsForRender()
      .forEach(this::writeToOutput);
  }

  /** Main loop that continuously handles requests until the socket closes. */
  private void handleRequests() {
    while (!socket.isClosed()) {
      Request request = parseRequest();
      logger.logWithLeadingNewline("Received request: %s", request.toString());
      logger.log(canvas.toString());

      switch (request.type) {
        case COORD:
          handleCoordRequest();
          break;
        case RENDER:
          handleRenderRequest();
          break;
        case HOVER:
        case DRAW:
        case ERASER:
          handleUpdateModeRequest(request);
          break;
        case LEFT:
        case RIGHT:
          handleUpdateDirectionRequest(request);
          break;
        case CLEAR:
          clearCanvas();
          break;
        case STEPS:
          handleStepsRequest(request.steps.get());
          break;
        case QUIT:
          // Client session is done. Thread finishes.
          closeSocket();
          return;
        case INVALID:
          handleInvalidRequest(request);
          break;
        default:
          throw new IllegalStateException(
              String.format(
                  "Unnexpected request: " + request.toString()));
      }
    }
  }

  private void handleStepsRequest(int steps) {
    canvas.step(steps);
  }

  private void handshake() throws IOException {
    String str = "hello\n";
    socket.getOutputStream().write(str.getBytes());
    logger.log("Handshake complete.");
  }

  private void handleUpdateDirectionRequest(Request request) {
    canvas.updateDirection(request);
  }

  private void handleUpdateModeRequest(Request request) {
    Mode mode;
    try {
      mode = Mode.valueOf(request.type.name());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("Unable to parse mode from: " + request.type.name());
    }
    canvas.updateMode(mode);
  }

  private Request parseRequest() {
    try {
      return Request.parse(inputReader.readLine());
    } catch (IOException e) {
      return Request.newBuilder().setLine("").setType(Type.INVALID).setException(e).build();
    }
  }

  private void writeToOutput(String str) {
    try {
      outputStream.write((str + "\r\n").getBytes());
      logger.log("Wrote to outputStream: %s", str);
    } catch (IOException e) {
      logger.log("Encountered exception writing to output: ", str);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
