package ru.gb.storage.client.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;

public class ConfigProperties {
    private static final String FILE_COMMON_PROP = "app.properties";
    private static Properties appProp;

    static {
        try (InputStream inCommon = ConfigProperties.class.getClassLoader().getResourceAsStream(FILE_COMMON_PROP)) {

            if (inCommon == null) {
                throw new FileNotFoundException(String.format("Unable to find \"%s\"", FILE_COMMON_PROP));
            }

            appProp = new Properties();
            appProp.load(inCommon);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Optional<String> getPropertyValue(String propertyKey) {
        return Optional.ofNullable(appProp.getProperty(propertyKey));
    }
}
