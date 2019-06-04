package com.ef;

import com.ef.Config.Config;
import com.ef.Config.DatabaseConnection;
import com.ef.model.Duration;
import com.ef.model.Param;
import com.ef.model.Variable;
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
 * Optimization: after start time + period stop read lines from log file.
 */
@Slf4j
public class ParserWithoutDataBase {
    public static void run(String[] args) {
        try {
            final Param param = new Param(args);
            param.validate();
            final HashMap<Variable, String> params = param.getParams();
            final Map<String, Integer> countLines = countLines(params.get(Variable.ACCESSLOG), params.get(Variable.STARTDATE),
                    params.get(Variable.DURATION), params.get(Variable.THRESHOLD));
            final Integer integer = Integer.valueOf(params.get(Variable.THRESHOLD));
            final Set<Map.Entry<String, Integer>> entries = countLines.entrySet();
            final List<Map.Entry<String, Integer>> blockedIp = getBlockedIp(integer, entries);
            printResult(blockedIp);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static List<Map.Entry<String, Integer>> getBlockedIp(Integer integer, Set<Map.Entry<String, Integer>> entries) {
        return entries
                .stream()
                .filter(stringIntegerMap -> stringIntegerMap.getValue() > integer)
                .collect(Collectors.toList());
    }

    private static void printResult(List<Map.Entry<String, Integer>> blockedIp) {
        blockedIp.stream().peek(blocked -> {
            System.out.println(blocked.getKey() + "\t : \t" + blocked.getValue());
        }).collect(Collectors.toList());
    }

    private static Map<String, Integer> countLines(String accesslog, String startDate, String duration, String threshold) {
        Map<String, Integer> counter = new HashMap<>();
        final Duration durationLocal = Duration.valueOf(duration.toUpperCase());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss");
        final LocalDateTime startTime = LocalDateTime.parse(startDate, formatter);
        final LocalDateTime plus = startTime.plus(durationLocal.seconds, ChronoUnit.SECONDS);
        try (Stream<String> stream = Files.lines(Paths.get(accesslog))) {
            final Iterator<String> iterator = stream.iterator();
            for (; iterator.hasNext(); ) {
                final String line = iterator.next();
                ParsedLine parsedLine = null;
                try {
                    parsedLine = parseLine(line);
                } catch (Exception e) {
                    System.out.println("Wrong line format!( " + line + ")");
                    continue;
                }
                if (parsedLine.time.isAfter(plus)) break;
                if (parsedLine.time.isAfter(startTime) && parsedLine.time.isBefore(plus)) {
                    if (counter.containsKey(parsedLine.ip)) {
                        counter.put(parsedLine.ip, counter.get(parsedLine.ip) + 1);
                    } else {
                        counter.put(parsedLine.ip, 1);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Check input file!", e);
            throw new RuntimeException("Check input file!");
        }

        return counter;
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
