SELECT DISTINCT l.ip, COUNT(l.id) as threshold
FROM log l
WHERE l.ts > '2017-01-01T15:00'
  AND l.ts < '2017-01-02T15:00'
GROUP BY l.ip
HAVING threshold >= 500;
