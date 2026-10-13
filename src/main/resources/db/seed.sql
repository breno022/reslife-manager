-- logins: teacher1, reslife1
-- passwords: teacherpass, reslifepass
INSERT OR REPLACE INTO Users(username,passwordHash,role,notify) VALUES
 ('teacher1','$2a$10$LH3ryZc6jOdhhpO4zYApzehvs8Q1N/KdIb3k7nE6kXfPvYGP9zVli','Teacher',0),
 ('reslife1','$2a$10$QxYEq4l6Ra4kVJm3RBUH3etiPlpl2cZ5QuGMcT.5oK0voxKZG5Dtm','ResLife',1);

INSERT OR IGNORE INTO Students(id, sid, name, grade, dorm, room, backlog)
VALUES (1, 'S0001', 'Test Student', 10, 'Iselworth', '663-B', 0);
