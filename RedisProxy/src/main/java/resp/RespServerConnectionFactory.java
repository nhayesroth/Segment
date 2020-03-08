package resp;

import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;

public class RespServerConnectionFactory implements ConnectionFactory {

  @Override
  public String getProtocol() {
    return "RESP";
  }

  @Override
  public Connection newConnection(Connector connector, EndPoint endPoint) {
    return null;
  }

}
