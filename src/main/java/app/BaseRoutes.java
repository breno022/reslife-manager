package app;

import spark.Request;
import spark.Response;

import java.sql.Connection;

public abstract class BaseRoutes {
  protected final Connection conn;

  protected BaseRoutes(Connection conn) {
    this.conn = conn;
  }

  protected Integer currentUserId(Request req) {
    var s = req.session(false);
    return (s == null) ? null : s.attribute("userId");
  }
  protected String currentRole(Request req) {
    var s = req.session(false);
    return (s == null) ? null : s.attribute("role");
  }
  protected void requireRole(Request req, Response res, String role) {
    String r = currentRole(req);
    if (r == null || !r.equalsIgnoreCase(role)) {
      res.status(403);
      throw new RuntimeException("Forbidden: " + role + " only");
    }
  }
}