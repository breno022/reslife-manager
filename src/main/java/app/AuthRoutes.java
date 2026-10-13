package app;

import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;
import spark.Request;
import spark.Response;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

public class AuthRoutes extends BaseRoutes {
  private final Gson gson = new Gson();

  public AuthRoutes(java.sql.Connection conn) {
    super(conn);
  }

  // login(): MISSING / INVALID / OK + set session userId/role
  public Object login(Request req, Response res) throws Exception {
    res.type("application/json");

    Map<?,?> m = gson.fromJson(req.body(), Map.class);
    String username = m.get("username") == null ? "" : m.get("username").toString().trim();
    String password = m.get("password") == null ? "" : m.get("password").toString();

    if (username.isEmpty() || password.isEmpty()) {
      return "{\"ok\":false,\"code\":\"MISSING\"}";
    }

    Integer id = null;
    String role = null;
    String passHash = null;

    try (PreparedStatement ps = conn.prepareStatement(
        "SELECT id, role, passwordHash FROM Users WHERE username=?")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          id = rs.getInt("id");
          role = rs.getString("role");
          passHash = rs.getString("passwordHash");
        }
      }
    }

    if (id == null || role == null || passHash == null) {
      return "{\"ok\":false,\"code\":\"INVALID\"}";
    }

    if (!BCrypt.checkpw(password, passHash)) {
      return "{\"ok\":false,\"code\":\"INVALID\"}";
    }

    req.session(true).attribute("userId", id);
    req.session().attribute("role", role);
    req.session().attribute("username", username);

    return "{\"ok\":true,\"code\":\"OK\",\"role\":\"" + role + "\"}";
  }

  public Object logout(Request req, Response res) {
    res.type("application/json");
    var s = req.session(false);
    if (s != null) s.invalidate();
    return "{\"ok\":true}";
  }

  public Object me(Request req, Response res) {
    res.type("application/json");
    var s = req.session(false);
    if (s == null) return "{\"ok\":false}";
    String role = s.attribute("role");
    String username = s.attribute("username");
    return "{\"ok\":true,\"role\":\"" + role + "\",\"username\":\"" + username + "\"}";
  }
}
