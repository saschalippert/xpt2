package util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FileUtil {

    public static Path getFile(LocalDateTime date, String instrument) {
        String year = String.format("%02d", date.getYear());
        String month = String.format("%02d", date.getMonthValue());
        String day = String.format("%02d", date.getDayOfMonth());
        String hour = String.format("%02d", date.getHour());

        return Paths.get(String.format("c:/temp2/%s/%s/%s/%s/%s.json", instrument, year, month, day, hour));
    }
}
