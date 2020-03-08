package resp;

import java.util.concurrent.Executor;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;

public class RespServerConnection extends AbstractConnection implements Connection {

  protected RespServerConnection(EndPoint endp, Executor executor) {
    super(endp, executor);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void onFillable() {
    // TODO Auto-generated method stub

  }

}
