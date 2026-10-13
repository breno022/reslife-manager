package app;

import static spark.Spark.*;
import java.sql.*;

public class Main {
  public static Connection conn;

  public static void main(String[] args) throws Exception {
    port(4567);
    staticFiles.externalLocation(new java.io.File("webroot").getAbsolutePath());

    exception(Exception.class, (e, req, res) -> {
      e.printStackTrace();
      if (res.status() < 400) res.status(500);
      res.type("text/plain");
      res.body(e.toString());
    });

    System.out.println("Working dir = " + System.getProperty("user.dir"));
    System.out.println("DB absolute path = " + new java.io.File("reslife.db").getAbsolutePath());

    // DB first
    conn = DriverManager.getConnection("jdbc:sqlite:reslife.db");
    try (Statement s = conn.createStatement()) {
      s.execute("PRAGMA foreign_keys = ON");
    }
    runSql("/db/schema.sql");
    runSql("/db/seed.sql");

    // create route objects
    AuthRoutes auth = new AuthRoutes(conn);
    DirectoryRoutes dir = new DirectoryRoutes(conn);
    Routes routes = new Routes(conn);

    // auth routes
    post("/api/login", auth::login);
    post("/api/logout", auth::logout);
    get("/api/me", auth::me);

    // directory route
    get("/api/pages", dir::pages);

    // app routes
    post("/request", routes::submitRequest);
    get("/api/queue", routes::queue);
    get("/api/students", routes::listStudents);
    post("/api/students", routes::createStudent);
    put("/api/students/:id", routes::updateStudent);
    delete("/api/students/:id", routes::deleteStudent);
    post("/api/students/:id/backlog", routes::updateBacklog);
    get("/api/students/:id/audit", routes::getAudit);
    get("/api/attendance/open", routes::listOpenAttendance);
    post("/api/attendance/toggle", routes::toggleAttendance);
    get("/api/notifications", routes::myNotifications);
    post("/api/notifications/:id/read", routes::markNotificationRead);
    get("/api/releases", routes::listReleases);
    get("/api/my_requests", routes::myRequests);

    System.out.println("Running at http://localhost:4567/login.html");
  }

  static void runSql(String resourcePath) throws Exception {
    try (var in = Main.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        System.out.println("Could not find resource: " + resourcePath);
        return;
      }

      String sql = new String(in.readAllBytes());
      String[] statements = sql.split(";(\\s*\\r?\\n|\\s*$)");

      try (Statement st = conn.createStatement()) {
        for (String stmt : statements) {
          String s = stmt.trim();
          if (s.isEmpty()) continue;
          st.execute(s);
        }
      }
    }
  }
}