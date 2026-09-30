package mg.bank.backend.utils;

import java.time.LocalDate;

public class DateUtils {
    
    public static String findTrimestre(LocalDate date) {
        int month = date.getMonthValue();
        if (month >= 1 && month <= 3) {
            return "T1";
        } else if (month >= 4 && month <= 6) {
            return "T2";
        } else if (month >= 7 && month <= 9) {
            return "T3";
        } else {
            return "T4";
        }
    }
}
