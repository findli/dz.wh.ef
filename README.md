
  Test case by Ian Burtovoy
  
  skype yanchik366
  
  email burtovoy.ian@gmail.com
 
set database connection parameters:
/src/main/resources/config.properties

  mvn clean compile assembly:single; java -cp "target/parser.jar"  com.ef.Parser --accesslog=/path/to/access.log \
  --startDate=2017-01-01.00:00:00 \
  --duration=daily \
  --threshold=500
  
  
  SELECT DISTINCT l.ip, COUNT(l.id) as threshold
  FROM log l
  WHERE l.ts > '2017-01-01T15:00'
    AND l.ts < '2017-01-02T15:00'
  GROUP BY l.ip
  HAVING threshold >= 500;
