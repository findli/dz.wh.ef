package com.ef;

import com.ef.Config.Config;
import com.ef.Config.DatabaseConnection;
import com.ef.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Probable optimization: after start time + period stop read lines from log file.
 * <p>
 * Test case by Ian Burtovoy
 * skype yanchik366
 * email burtovoy.ian@gmail.com
 * <p>
 * mvn clean compile assembly:single; java -cp "target/parser.jar"  com.ef.Parser --accesslog=/path/to/access.log \
 * --startDate=2017-01-01.00:00:00 \
 * --duration=daily \
 * --threshold=500
 */
@Slf4j
public class Parser {
    public static void main(String[] args) {
        try (final DatabaseConnection connection = new DatabaseConnection()) {
            final Param param = new Param(args);
            param.validate();
            final HashMap<Variable, String> params = param.getParams();
            save(params.get(Variable.ACCESSLOG));
            final Map<String, Integer> countLines = countLines(params.get(Variable.STARTDATE),
                    params.get(Variable.DURATION), params.get(Variable.THRESHOLD));
            final Integer integer = Integer.valueOf(params.get(Variable.THRESHOLD));
            final Set<Map.Entry<String, Integer>> entries = countLines.entrySet();
            final List<Map.Entry<String, Integer>> blockedIp = entries
                    .stream()
                    .filter(stringIntegerMap -> stringIntegerMap.getValue() > integer)
                    .collect(Collectors.toList());
            saveBlockedIp(blockedIp, params.get(Variable.STARTDATE), params.get(Variable.DURATION), params.get(Variable.THRESHOLD));
            printResult(blockedIp);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void printResult(List<Map.Entry<String, Integer>> blockedIp) {
        blockedIp.stream().peek(blocked -> {
            System.out.println(blocked.getKey() + "\t : \t" + blocked.getValue());
        }).collect(Collectors.toList());
    }

    private static void saveBlockedIp(List<Map.Entry<String, Integer>> blockedIp, String startDate, String duration, String threshold) {
        final Properties properties = Config.getProperties();

        try {
            Connection connection = DatabaseConnection.getConnection();
            String sql = "INSERT INTO filtered (ip, comment) VALUES (?,?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            try {
                for (Map.Entry<String, Integer> blockedEntry : blockedIp) {
                    statement.setString(1, blockedEntry.getKey());
                    final String comment = new StringBuilder()
                            .append("Exceeded request limit( ")
                            .append(threshold)
                            .append(") from ip: ")
                            .append(blockedEntry.getKey())
                            .append(" starting from ")
                            .append(startDate)
                            .append(" during one ")
                            .append(duration.toUpperCase().equals(Duration.DAILY.name().toUpperCase()) ? "day." : "hour.")
                            .toString();
                    blockedEntry.getValue().toString();
                    statement.setString(2, comment);
                    statement.executeUpdate();
                }
            } catch (SQLIntegrityConstraintViolationException e) {
                try {
                    if (!Boolean.valueOf(properties.getProperty("omitDuplicationMessage"))) {
                        log.debug("If you want to omit duplicate entry log message truncate log table or set property " +
                                "omitDuplicationMessage=true in configuration file!");
                        log.debug(e.getMessage(), e);
                    }
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                }
            } finally {
                statement.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Problem with database!");
        }
    }

    private static Map<String, Integer> countLines(String startDate, String duration, String threshold) {
        Map<String, Integer> counter = new HashMap<>();
        final Duration durationLocal = Duration.valueOf(duration.toUpperCase());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss");
        final LocalDateTime startTime = LocalDateTime.parse(startDate, formatter);
        final LocalDateTime plus = startTime.plus(durationLocal.seconds, ChronoUnit.SECONDS);

        Connection connection = DatabaseConnection.getConnection();
        String sql = "SELECT DISTINCT l.ip, COUNT(l.id) as threshold FROM log l WHERE l.ts > '"
                + startTime + "' AND l.ts < '" + plus + "' GROUP  BY l.ip HAVING threshold >= " + threshold;
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            final ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                final String ip = rs.getString("ip");
                final String thresholdRow = rs.getString("threshold");
                counter.put(ip, Integer.valueOf(thresholdRow));
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return counter;
    }

    private static void save(String accesslog) {
        final Properties properties = Config.getProperties();

        try {
            Connection connection = DatabaseConnection.getConnection();
            String sql = "INSERT INTO log (ip, ts) VALUES (?,?)";
            PreparedStatement statement = connection.prepareStatement(sql);
            try (Stream<String> stream = Files.lines(Paths.get(accesslog))) {
                final Iterator<String> iterator = stream.iterator();
                for (; iterator.hasNext(); ) {
                    final String line = iterator.next();
                    final ParsedLine parsedLine;
                    try {
                        parsedLine = parseLine(line);
                        statement.setString(1, parsedLine.ip);
                        statement.setString(2, parsedLine.time.toString());
                        statement.executeUpdate();
                    } catch (SQLIntegrityConstraintViolationException e) {
                        try {
                            if (!Boolean.valueOf(properties.getProperty("omitDuplicationMessage"))) {
                                log.debug("If you want to omit duplicate entry log message truncate log table" +
                                        " or set property omitDuplicationMessage=true in configuration file!");
                                log.debug(e.getMessage(), e);
                            }
                        } catch (Exception ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    } catch (RuntimeException e) {
                        log.error("Wrong line format!( " + line + ")", e);
                    }
                }
            } catch (IOException e) {
                log.error("Check input file!", e);
                throw new RuntimeException("Check input file!");
            } finally {
                statement.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("Problem with database!");
        }
    }

    private static ParsedLine parseLine(String line) {
        final ParsedLine result;
        try {
            final String[] split = line.split("\\|");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime dateTime = LocalDateTime.parse(split[0], formatter);
            result = new ParsedLine(split[1], dateTime);
        } catch (Exception e) {
            log.error("Check input file!", e);
            throw new RuntimeException("Check input file!");
        }
        return result;
    }

    static class ParsedLine {
        private final String ip;
        private final LocalDateTime time;

        ParsedLine(String ip, LocalDateTime time) {
            this.ip = ip;
            this.time = time;
        }
    }


}
