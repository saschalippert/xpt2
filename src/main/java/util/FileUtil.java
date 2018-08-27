package util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class FileUtil {

    private static final String userHome = System.getProperty("user.home");

    private static final String prefix = "xpt2";

    private static final String pathSeparator = File.separator;

    public static final String saveDirectoryString = userHome + pathSeparator + prefix + pathSeparator + "%s"
	    + pathSeparator + "%s" + pathSeparator
	    + "%s" + pathSeparator + "%s" + pathSeparator + "%s.json";

    public static Path getFile(LocalDateTime date, String instrument) {
        String year = String.format("%02d", date.getYear());
        String month = String.format("%02d", date.getMonthValue());
        String day = String.format("%02d", date.getDayOfMonth());
        String hour = String.format("%02d", date.getHour());

	return Paths.get(String.format(saveDirectoryString, instrument, year, month, day, hour));
    }
}
