package http;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpStatus;

public class HttpServlet extends javax.servlet.http.HttpServlet {
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // TODO check the lruCache before calling redis
    
    resp.setStatus(HttpStatus.OK_200);
    resp.getWriter().println("EmbeddedJetty");
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException();
  }

}
