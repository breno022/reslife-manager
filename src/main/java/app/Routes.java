package app;

import spark.Request; 
import spark.Response;
import java.sql.*; 
import java.time.LocalDate;
import java.util.*;

public class Routes extends BaseRoutes {
	
	public Routes(Connection conn) {
	    super(conn);
	}

	public Object submitRequest(Request req, Response res) throws Exception {
		  res.type("application/json");
		requireRole(req, res, "Teacher");
		Integer teacherId = currentUserId(req);
		if (teacherId == null) {
		  res.status(401);
		  return "{\"ok\":false,\"error\":\"Not logged in\"}";
		}
		  res.type("application/json");
		  var m = new com.google.gson.Gson().fromJson(req.body(), java.util.Map.class);
		  String studentSid = (String) m.get("studentSid");
		  
		  if (studentSid == null || studentSid.trim().isEmpty())
		    return "{\"ok\":false,\"error\":\"studentSid required\"}";
		  String date = (String) m.getOrDefault("date", java.time.LocalDate.now().toString());
		  String reason = (String) m.getOrDefault("reason", "");
		  if (reason == null || reason.trim().isEmpty())
		    return "{\"ok\":false,\"error\":\"reason required\"}";
		  if (reason.length() > 300)
		    return "{\"ok\":false,\"error\":\"reason too long (max 300)\"}";
		  // look up Students.id from Students.sid
		  Integer studentId = null;
		  try (PreparedStatement ps = conn.prepareStatement(
		      "SELECT id FROM Students WHERE sid = ?")) {
		    ps.setString(1, studentSid.trim());
		    try (ResultSet rs = ps.executeQuery()) {
		      if (rs.next()) studentId = rs.getInt("id");
		    }
		  }
		  if (studentId == null) {
		    return "{\"ok\":false,\"error\":\"No student found with SID: " + studentSid.trim() + "\"}";
		  }
		  // insert request using studentId (i was using id before)
		  try (PreparedStatement ps = conn.prepareStatement(
				    "INSERT INTO Requests(studentId,teacherId,date,reason) VALUES (?,?,?,?)")) {
				  ps.setInt(1, studentId);
				  ps.setInt(2, teacherId);
				  ps.setString(3, date);
				  ps.setString(4, reason.trim());
				  ps.executeUpdate();
				  return "{\"ok\":true}";
				} catch (java.sql.SQLException e) {
				  if (e.getMessage() != null && e.getMessage().contains("UNIQUE"))
				    return "{\"ok\":false,\"error\":\"Duplicate for same student & date\"}";
				  e.printStackTrace();
				  System.out.println("SQLState=" + e.getSQLState() + " ErrorCode=" + e.getErrorCode());
				  System.out.println("Message=" + e.getMessage());
				  throw e;
				}
		}

	// notifications
		public Object myNotifications(Request req, Response res) throws Exception {
			  res.type("application/json");
			  requireRole(req, res, "ResLife");

			  Integer userId = currentUserId(req);
			  if (userId == null) {
			    res.status(401);
			    return "{\"ok\":false,\"error\":\"Not logged in\"}";
			  }

			  var rows = new ArrayList<Map<String,Object>>();
			  try (PreparedStatement ps = conn.prepareStatement(
			      "SELECT n.id, n.message, n.isRead, n.createdAt, s.sid, s.name " +
			      "FROM Notifications n JOIN Students s ON s.id=n.studentId " +
			      "WHERE n.userId=? ORDER BY n.createdAt DESC LIMIT 50")) {
			    ps.setInt(1, userId);
			    try (ResultSet rs = ps.executeQuery()) {
			      while (rs.next()) {
			        var row = new LinkedHashMap<String,Object>();
			        row.put("id", rs.getInt("id"));
			        row.put("message", rs.getString("message"));
			        row.put("isRead", rs.getInt("isRead"));
			        row.put("createdAt", rs.getString("createdAt"));
			        row.put("sid", rs.getString("sid"));
			        row.put("name", rs.getString("name"));
			        rows.add(row);
			      }
			    }
			  }
			  return new com.google.gson.Gson().toJson(rows);
			}
		
		public Object markNotificationRead(Request req, Response res) throws Exception {
			  res.type("application/json");
			  requireRole(req, res, "ResLife");

			  Integer userId = currentUserId(req);
			  if (userId == null) {
			    res.status(401);
			    return "{\"ok\":false,\"error\":\"Not logged in\"}";
			  }

			  int notifId = Integer.parseInt(req.params(":id"));

			  try (PreparedStatement ps = conn.prepareStatement(
			      "UPDATE Notifications SET isRead=1 WHERE id=? AND userId=?")) {
			    ps.setInt(1, notifId);
			    ps.setInt(2, userId);
			    int changed = ps.executeUpdate();
			    if (changed == 0) return "{\"ok\":false,\"error\":\"not found\"}";
			    return "{\"ok\":true}";
			  }
			}
		
		public Object listReleases(Request req, Response res) throws Exception {
			  res.type("application/json");
			  requireRole(req, res, "ResLife");

			  var rows = new ArrayList<Map<String,Object>>();
			  try (PreparedStatement ps = conn.prepareStatement(
			      "SELECT rl.id, rl.ts, rl.actor, s.sid, s.name " +
			      "FROM ReleaseLog rl JOIN Students s ON s.id=rl.studentId " +
			      "ORDER BY rl.ts DESC LIMIT 50");
			       ResultSet rs = ps.executeQuery()) {

			    while (rs.next()) {
			      var row = new LinkedHashMap<String,Object>();
			      row.put("id", rs.getInt("id"));
			      row.put("ts", rs.getString("ts"));
			      row.put("actor", rs.getString("actor"));
			      row.put("sid", rs.getString("sid"));
			      row.put("name", rs.getString("name"));
			      rows.add(row);
			    }
			  }
			  return new com.google.gson.Gson().toJson(rows);
			}

  public Object queue(Request req, Response res) throws Exception {
	  res.type("application/json");
	  requireRole(req, res, "ResLife");
	  
    var rows = new ArrayList<Map<String,Object>>();
    try (PreparedStatement ps = conn.prepareStatement(
      "SELECT r.id, s.name AS student, r.date, r.status, r.createdAt, r.reason " +
      "FROM Requests r JOIN Students s ON s.id=r.studentId " +
      "ORDER BY r.createdAt DESC LIMIT 20");
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        var row = new LinkedHashMap<String,Object>();
        row.put("id", rs.getInt("id"));
        row.put("student", rs.getString("student"));
        row.put("date", rs.getString("date"));
        row.put("status", rs.getString("status"));
        row.put("createdAt", rs.getString("createdAt"));
        row.put("reason", rs.getString("reason"));
        rows.add(row);
      }
    }
    return new com.google.gson.Gson().toJson(rows);
  }
  
  public Object listStudents(Request req, Response res) throws Exception {
	  
	  res.type("application/json");
	  var rows = new ArrayList<Map<String,Object>>();

	  try (PreparedStatement ps = conn.prepareStatement(
	      "SELECT id, sid, name, grade, dorm, room, backlog, updatedAt FROM Students ORDER BY name ASC");
	       ResultSet rs = ps.executeQuery()) {

	    while (rs.next()) {
	      var row = new LinkedHashMap<String,Object>();
	      row.put("id", rs.getInt("id"));
	      row.put("sid", rs.getString("sid"));
	      row.put("name", rs.getString("name"));
	      row.put("grade", rs.getInt("grade"));
	      row.put("dorm", rs.getString("dorm"));
	      row.put("room", rs.getString("room"));
	      row.put("backlog", rs.getInt("backlog"));
	      row.put("updatedAt", rs.getString("updatedAt"));
	      rows.add(row);
	    }
	  }
	  return new com.google.gson.Gson().toJson(rows);
	}
  // method to create student
	public Object createStudent(Request req, Response res) throws Exception {
		res.type("application/json");
		requireRole(req, res, "ResLife");
		
	  var m = new com.google.gson.Gson().fromJson(req.body(), Map.class);

	  String sid = (String) m.get("sid");
	  String name = (String) m.get("name");
	  Double gradeD = (Double) m.get("grade");
	  String dorm = (String) m.get("dorm");
	  String room = (String) m.getOrDefault("room", "");

	  // validation (server-side)
	  if (sid == null || sid.trim().isEmpty()) return "{\"ok\":false,\"error\":\"sid required\"}";
	  if (name == null || name.trim().isEmpty()) return "{\"ok\":false,\"error\":\"name required\"}";
	  if (dorm == null || dorm.trim().isEmpty()) return "{\"ok\":false,\"error\":\"dorm required\"}";
	  if (gradeD == null) return "{\"ok\":false,\"error\":\"grade required\"}";
	  int grade = gradeD.intValue();
	  if (grade < 6 || grade > 12) return "{\"ok\":false,\"error\":\"grade must be 6-12\"}";

	  try (PreparedStatement ps = conn.prepareStatement(
	      "INSERT INTO Students(sid,name,grade,dorm,room) VALUES (?,?,?,?,?)")) {
	    ps.setString(1, sid.trim());
	    ps.setString(2, name.trim());
	    ps.setInt(3, grade);
	    ps.setString(4, dorm.trim());
	    ps.setString(5, room == null ? null : room.trim());
	    ps.executeUpdate();
	    return "{\"ok\":true}";
	  } catch (SQLException e) {
	    if (e.getMessage() != null && e.getMessage().contains("UNIQUE"))
	      return "{\"ok\":false,\"error\":\"sid already exists\"}";
	    return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
	  }
	}
// method to update student
	public Object updateStudent(Request req, Response res) throws Exception {
		res.type("application/json");
		requireRole(req, res, "ResLife");
	  int id = Integer.parseInt(req.params(":id"));
	  var m = new com.google.gson.Gson().fromJson(req.body(), Map.class);

	  String sid = (String) m.get("sid");
	  String name = (String) m.get("name");
	  Double gradeD = (Double) m.get("grade");
	  String dorm = (String) m.get("dorm");
	  String room = (String) m.getOrDefault("room", "");

	  if (sid == null || sid.trim().isEmpty()) return "{\"ok\":false,\"error\":\"sid required\"}";
	  if (name == null || name.trim().isEmpty()) return "{\"ok\":false,\"error\":\"name required\"}";
	  if (dorm == null || dorm.trim().isEmpty()) return "{\"ok\":false,\"error\":\"dorm required\"}";
	  if (gradeD == null) return "{\"ok\":false,\"error\":\"grade required\"}";
	  int grade = gradeD.intValue();
	  if (grade < 6 || grade > 12) return "{\"ok\":false,\"error\":\"grade must be 6-12\"}";

	  try (PreparedStatement ps = conn.prepareStatement(
	      "UPDATE Students SET sid=?, name=?, grade=?, dorm=?, room=?, updatedAt=CURRENT_TIMESTAMP WHERE id=?")) {
	    ps.setString(1, sid.trim());
	    ps.setString(2, name.trim());
	    ps.setInt(3, grade);
	    ps.setString(4, dorm.trim());
	    ps.setString(5, room == null ? null : room.trim());
	    ps.setInt(6, id);
	    int changed = ps.executeUpdate();
	    if (changed == 0) return "{\"ok\":false,\"error\":\"student not found\"}";
	    return "{\"ok\":true}";
	  } catch (SQLException e) {
	    if (e.getMessage() != null && e.getMessage().contains("UNIQUE"))
	      return "{\"ok\":false,\"error\":\"sid already exists\"}";
	    return "{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}";
	  }
	}
// method to remove student from system
	public Object deleteStudent(Request req, Response res) throws Exception {
		res.type("application/json");
		requireRole(req, res, "ResLife");
	  int id = Integer.parseInt(req.params(":id"));

	  try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Students WHERE id=?")) {
	    ps.setInt(1, id);
	    int changed = ps.executeUpdate();
	    if (changed == 0) return "{\"ok\":false,\"error\":\"student not found\"}";
	    return "{\"ok\":true}";
	  }
	}

	public Object updateBacklog(Request req, Response res) throws Exception {
		  res.type("application/json");
		  requireRole(req, res, "ResLife");

		  int studentId = Integer.parseInt(req.params(":id"));
		  var m = new com.google.gson.Gson().fromJson(req.body(), java.util.Map.class);

		  Object d = m.get("delta");
		  if (d == null) return "{\"ok\":false,\"error\":\"delta required\"}";
		  int delta = (d instanceof Double) ? ((Double)d).intValue() : Integer.parseInt(d.toString());

		  String actor = (String) m.getOrDefault("actor", "reslife");

		  boolean released = false;
		  int notified = 0;
		  int oldValue = -1;
		  int newValue = -1;

		  conn.setAutoCommit(false);
		  try {
		    // read current backlog
		    try (PreparedStatement ps = conn.prepareStatement(
		        "SELECT backlog FROM Students WHERE id=?")) {
		      ps.setInt(1, studentId);
		      try (ResultSet rs = ps.executeQuery()) {
		        if (!rs.next()) {
		          conn.rollback();
		          res.status(404);
		          return "{\"ok\":false,\"error\":\"student not found\"}";
		        }
		        oldValue = rs.getInt("backlog");
		      }
		    }
		    // clamp new backlog >= 0
		    newValue = oldValue + delta;
		    if (newValue < 0) newValue = 0;
		    // update student backlog
		    try (PreparedStatement ps = conn.prepareStatement(
		        "UPDATE Students SET backlog=?, updatedAt=CURRENT_TIMESTAMP WHERE id=?")) {
		      ps.setInt(1, newValue);
		      ps.setInt(2, studentId);
		      ps.executeUpdate();
		    }
		    // insert audit log (applied delta)
		    int appliedDelta = newValue - oldValue;
		    try (PreparedStatement ps = conn.prepareStatement(
		        "INSERT INTO AuditLog(studentId, delta, oldValue, newValue, actor) VALUES (?,?,?,?,?)")) {
		      ps.setInt(1, studentId);
		      ps.setInt(2, appliedDelta);
		      ps.setInt(3, oldValue);
		      ps.setInt(4, newValue);
		      ps.setString(5, actor);
		      ps.executeUpdate();
		    }

		    // release + notifications when backlog hits 0
		    if (oldValue > 0 && newValue == 0) {
		      // Release log
		      try (PreparedStatement ps = conn.prepareStatement(
		          "INSERT INTO ReleaseLog(studentId, actor) VALUES (?,?)")) {
		        ps.setInt(1, studentId);
		        ps.setString(2, actor);
		        ps.executeUpdate();
		      }

		      // Student details for message
		      String stuSid = "";
		      String stuName = "";
		      try (PreparedStatement ps = conn.prepareStatement(
		          "SELECT sid, name FROM Students WHERE id=?")) {
		        ps.setInt(1, studentId);
		        try (ResultSet rs = ps.executeQuery()) {
		          if (rs.next()) {
		            stuSid = rs.getString("sid");
		            stuName = rs.getString("name");
		          }
		        }
		      }
		      String msg = "Backlog reached 0 for " + stuName + " (" + stuSid + "). Student released.";
		      // notify ResLife users with notify=1
		      try (PreparedStatement sel = conn.prepareStatement(
		             "SELECT id FROM Users WHERE role='ResLife' AND notify=1");
		           ResultSet rs = sel.executeQuery();
		           PreparedStatement ins = conn.prepareStatement(
		             "INSERT INTO Notifications(userId, studentId, message) VALUES (?,?,?)")) {

		        while (rs.next()) {
		          int userId = rs.getInt("id");
		          ins.setInt(1, userId);
		          ins.setInt(2, studentId);
		          ins.setString(3, msg);
		          ins.addBatch();
		          notified++;
		        }
		        ins.executeBatch();
		      }
		      released = true;
		      System.out.println("RELEASE triggered for studentId=" + studentId + " notified=" + notified);
		    }
		    conn.commit();
		    return "{\"ok\":true,\"old\":" + oldValue + ",\"new\":" + newValue +
		           ",\"released\":" + released + ",\"notified\":" + notified + "}";

		  } catch (Exception e) {
		    try { conn.rollback(); } catch (Exception ignored) {}
		    res.status(500);
		    String msg = (e.getMessage() == null) ? "error" : e.getMessage().replace("\"", "'");
		    return "{\"ok\":false,\"error\":\"" + msg + "\"}";
		  } finally {
		    conn.setAutoCommit(true);
		  }
		}
	
	//audit history endpoint (so I can prove logging)
	public Object getAudit(Request req, Response res) throws Exception {
		res.type("application/json");
		requireRole(req, res, "ResLife");
		  int studentId = Integer.parseInt(req.params(":id"));

		  var rows = new java.util.ArrayList<java.util.Map<String,Object>>();
		  try (PreparedStatement ps = conn.prepareStatement(
		      "SELECT delta, oldValue, newValue, actor, ts FROM AuditLog WHERE studentId=? ORDER BY ts DESC LIMIT 50")) {
		    ps.setInt(1, studentId);
		    try (ResultSet rs = ps.executeQuery()) {
		      while (rs.next()) {
		        var row = new java.util.LinkedHashMap<String,Object>();
		        row.put("delta", rs.getInt("delta"));
		        row.put("oldValue", rs.getInt("oldValue"));
		        row.put("newValue", rs.getInt("newValue"));
		        row.put("actor", rs.getString("actor"));
		        row.put("ts", rs.getString("ts"));
		        rows.add(row);
		      }
		    }
		  }
		  return new com.google.gson.Gson().toJson(rows);
		}
	
	public Object listOpenAttendance(Request req, Response res) throws Exception {
		  res.type("application/json");
		  requireRole(req, res, "ResLife");

		  var rows = new ArrayList<Map<String,Object>>();
		  try (PreparedStatement ps = conn.prepareStatement(
		      "SELECT a.id, s.sid, s.name, a.inStaff, a.isLate, a.minutesLate " +
		      "FROM Attendance a JOIN Students s ON s.id=a.studentId " +
		      "WHERE a.outTs IS NULL ORDER BY a.inTs DESC");
		       ResultSet rs = ps.executeQuery()) {

		    while (rs.next()) {
		      var row = new LinkedHashMap<String,Object>();
		      row.put("attendanceId", rs.getInt("id"));
		      row.put("sid", rs.getString("sid"));
		      row.put("name", rs.getString("name"));
		      row.put("inStaff", rs.getString("inStaff"));

		      int isLate = rs.getInt("isLate");
		      row.put("onTime", isLate == 0 ? "Yes" : "No");
		      row.put("minutesLate", rs.getInt("minutesLate"));

		      rows.add(row);
		    }
		  }
		  return new com.google.gson.Gson().toJson(rows);
		}
	
	public Object toggleAttendance(Request req, Response res) throws Exception {
		  res.type("application/json");
		  requireRole(req, res, "ResLife");

		  var m = new com.google.gson.Gson().fromJson(req.body(), java.util.Map.class);

		  // onTime = "YES" or "NO"
		  String onTime = (String) m.getOrDefault("onTime", "");

		  int minutesLate = 0;
		  Object ml = m.get("minutesLate");
		  if (ml instanceof Double) minutesLate = ((Double) ml).intValue();

		  String studentSid = (String) m.get("studentSid");
		  String staff = (String) m.getOrDefault("staff", "reslife");

		  if (studentSid == null || studentSid.trim().isEmpty())
		    return "{\"ok\":false,\"error\":\"studentSid required\"}";
		  // map SID --> studentId
		  Integer studentId = null;
		  try (var ps = conn.prepareStatement("SELECT id FROM Students WHERE sid=?")) {
		    ps.setString(1, studentSid.trim());
		    try (var rs = ps.executeQuery()) {
		      if (rs.next()) studentId = rs.getInt("id");
		    }
		  }
		  if (studentId == null)
		    return "{\"ok\":false,\"error\":\"No student found with SID: " + studentSid.trim() + "\"}";
		  conn.setAutoCommit(false);
		  try {
		    // check if there is an open session
		    Integer openAttendanceId = null;
		    String inTs = null;
		    try (var ps = conn.prepareStatement(
		        "SELECT id, inTs FROM Attendance WHERE studentId=? AND outTs IS NULL ORDER BY inTs DESC LIMIT 1")) {
		      ps.setInt(1, studentId);
		      try (var rs = ps.executeQuery()) {
		        if (rs.next()) {
		          openAttendanceId = rs.getInt("id");
		          inTs = rs.getString("inTs");
		        }
		      }
		    }
		    if (openAttendanceId == null) {
		      // CHECK-IN (must specify onTime)
		      if (onTime == null || onTime.trim().isEmpty()) {
		        return "{\"ok\":false,\"error\":\"Must choose On time: Yes or No\"}";
		      }
		      int isLate = 0;
		      if ("YES".equalsIgnoreCase(onTime)) {
		        isLate = 0;
		        minutesLate = 0;
		      } else if ("NO".equalsIgnoreCase(onTime)) {
		        isLate = 1;
		        if (minutesLate <= 0) {
		          return "{\"ok\":false,\"error\":\"If On time = No, minutesLate must be > 0\"}";
		        }
		      } else {
		        return "{\"ok\":false,\"error\":\"Invalid onTime value\"}";
		      }
		      try (var ps = conn.prepareStatement(
		    		    "INSERT INTO Attendance(studentId,inStaff,isLate,minutesLate) VALUES (?,?,?,?)")) {
		    		  ps.setInt(1, studentId);
		    		  ps.setString(2, staff);
		    		  ps.setInt(3, isLate);
		    		  ps.setInt(4, minutesLate);
		    		  ps.executeUpdate();
		    		}
		      conn.commit();
		      return "{\"ok\":true,\"action\":\"CHECK_IN\",\"onTime\":\"" +
		             ("YES".equalsIgnoreCase(onTime) ? "Yes" : "No") +
		             "\",\"minutesLate\":" + minutesLate + "}";
		    } else {
		      // CHECK-OUT
		      java.time.LocalDateTime inTime = java.time.LocalDateTime.parse(inTs.replace(" ", "T"));
		      long minutes = java.time.Duration.between(inTime, java.time.LocalDateTime.now()).toMinutes();
		      if (minutes < 0) minutes = 0;

		      try (var ps = conn.prepareStatement(
		          "UPDATE Attendance SET outTs=CURRENT_TIMESTAMP, outStaff=?, durationM=? WHERE id=?")) {
		        ps.setString(1, staff);
		        ps.setLong(2, minutes);
		        ps.setInt(3, openAttendanceId);
		        ps.executeUpdate();
		      }
		      conn.commit();
		      return "{\"ok\":true,\"action\":\"CHECK_OUT\",\"durationM\":" + minutes + "}";
		    }
		  } catch (Exception e) {
		    conn.rollback();
		    throw e;
		  } finally {
		    conn.setAutoCommit(true);
		  }
		}
	public Object myRequests(Request req, Response res) throws Exception {
		  res.type("application/json");
		  requireRole(req, res, "Teacher");

		  Integer teacherId = currentUserId(req);
		  if (teacherId == null) {
		    res.status(401);
		    return "{\"ok\":false,\"error\":\"Not logged in\"}";
		  }

		  var rows = new ArrayList<Map<String,Object>>();
		  try (PreparedStatement ps = conn.prepareStatement(
		      "SELECT r.id, r.date, r.reason, r.status, r.createdAt, s.sid, s.name " +
		      "FROM Requests r JOIN Students s ON s.id=r.studentId " +
		      "WHERE r.teacherId=? ORDER BY r.createdAt DESC LIMIT 100")) {
		    ps.setInt(1, teacherId);
		    try (ResultSet rs = ps.executeQuery()) {
		      while (rs.next()) {
		        var row = new LinkedHashMap<String,Object>();
		        row.put("id", rs.getInt("id"));
		        row.put("date", rs.getString("date"));
		        row.put("reason", rs.getString("reason"));
		        row.put("status", rs.getString("status"));
		        row.put("createdAt", rs.getString("createdAt"));
		        row.put("studentSid", rs.getString("sid"));
		        row.put("studentName", rs.getString("name"));
		        rows.add(row);
		      }
		    }
		  }
		  return new com.google.gson.Gson().toJson(rows);
		}
}