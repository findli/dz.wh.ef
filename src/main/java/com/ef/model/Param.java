package com.ef.model;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Param {
    private final HashMap<Variable, String> params = new HashMap<>();

    public Param(String... args) {
        for (String arg : args) {
            final String[] split = arg.split("--")[1].split("=");
            try {
                final Variable variable2 = Variable.valueOf(Variable.class, split[0].toUpperCase());
                params.put(variable2, split[1]);
            } catch (Exception e) {
                log.error("Invalid argument( " + arg + ")!", e);
                throw new IllegalArgumentException("Invalid argument( " + arg + ")!");
            }
        }
    }

    public HashMap<Variable, String> getParams() {
        return params;
    }

    public void validate() {
        for (Map.Entry<Variable, String> entry : params.entrySet()) {
            Variable variable = entry.getKey();
            String val = entry.getValue();
            if (variable == Variable.ACCESSLOG) {
                final File path;
                try {
                    path = new File(val);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid argument( " + variable.getName() + ")!");
                }
                if (!path.isFile() || !path.canRead()) {
                    throw new IllegalArgumentException("Invalid argument( " + variable.getName() + ")!");
                }
            } else if (variable == Variable.DURATION) {
                try {
                    Duration.valueOf((val).toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.error("Duration validate error!", e);
                    throw new IllegalArgumentException("Invalid argument( " + variable.getName() + ")!");
                }
            } else if (variable == Variable.STARTDATE) {
                try {
                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("yyyy-MM-dd.HH:mm:ss");
                    LocalDate.parse(val, formatter);
                } catch (Exception e) {
                    log.error("StartDate validate error!", e);
                    throw new IllegalArgumentException("Invalid argument( " + variable.getName() + ")!");
                }
            } else if (variable == Variable.THRESHOLD) {
                try {
                    if (Integer.valueOf(val) < 0)
                        throw new IllegalArgumentException("Invalid argument( " + variable.getName() + ")!");
                } catch (Exception e) {
                    log.error("Threshold validate error!", e);
                }
            }
        }

    }
}
