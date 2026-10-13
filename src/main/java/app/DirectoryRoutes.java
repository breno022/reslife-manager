package app;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;

import java.util.*;

public class DirectoryRoutes extends BaseRoutes {
  private final Gson gson = new Gson();

  public DirectoryRoutes(java.sql.Connection conn) {
    super(conn);
  }

  // SC8 allowedPages(role)
  public Object pages(Request req, Response res) {
    res.type("application/json");
    String role = currentRole(req);

    List<Map<String,String>> out = new ArrayList<>();
    if ("Teacher".equalsIgnoreCase(role)) {
      out.add(link("Submit Request", "/request.html"));
      out.add(link("My Requests", "/my_requests.html")); // you can add this page next
    } else if ("ResLife".equalsIgnoreCase(role)) {
      out.add(link("Students", "/students.html"));
      out.add(link("Requests Queue", "/queue.html"));
      out.add(link("Attendance", "/attendance.html"));
      out.add(link("Notifications", "/notifications.html"));
    }
    return gson.toJson(out);
  }

  private Map<String,String> link(String title, String url) {
    Map<String,String> m = new LinkedHashMap<>();
    m.put("title", title);
    m.put("url", url);
    return m;
  }
}