package ru.gb.storage.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class ConfigProperties {

    private static final String FILE_COMMON_PROP = "common.properties";
    private static final String FILE_HIKARI_PROP = "hikari.properties";
    private static Properties prop;
    private static Properties hikariProp;


    static {
        try (InputStream inCommon = ConfigProperties.class.getClassLoader().getResourceAsStream(FILE_COMMON_PROP);
             InputStream inHikari = ConfigProperties.class.getClassLoader().getResourceAsStream(FILE_HIKARI_PROP)) {

            if (inCommon == null) {
                throw new FileNotFoundException(String.format("Unable to find \"%s\"", FILE_COMMON_PROP));
            }
            if (inHikari == null) {
                throw new FileNotFoundException(String.format("Unable to find \"%s\"", FILE_HIKARI_PROP));
            }

            prop = new Properties();
            prop.load(inCommon);

            hikariProp = new Properties();
            hikariProp.load(inHikari);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Optional<String> getPropertyValue(String propertyKey) {
        return Optional.ofNullable(prop.getProperty(propertyKey));
    }

    protected static Properties getHikariProperties() {
        return hikariProp;
    }

}