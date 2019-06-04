package com.ef.Config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public class Config {
    public static Properties getProperties() {
        Properties prop = new Properties();
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {


            if (Objects.isNull(prop)) throw new RuntimeException("Unable to find config.properties in resources!");

            prop.load(input);

/*
            if (!prop.containsKey("db.url") ||
            !prop.containsKey("db.user") ||
            !prop.containsKey("db.password")) {
                throw new Exception("")
            }
*/
        } catch (IOException ex) {
            throw new RuntimeException("Unable to parse config.properties");
        }
        return prop;
    }
}
