package co.com.aws.lambda.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonReader {
    public static String readJsonFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
}
