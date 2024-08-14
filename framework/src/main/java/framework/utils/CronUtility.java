package framework.utils;

import framework.exceptions.InvalidCronStringFormat;

public class CronUtility {
    public static int parseTotalSecondsFromCron(String cronString) throws InvalidCronStringFormat {
        String[] secondsAndMinutes = cronString.trim().split(" ");
        if (secondsAndMinutes.length != 2) {
            throwCronFormatException(cronString);
        }

        int totalSeconds = 0;

        try {

            totalSeconds += Integer.parseInt(secondsAndMinutes[0]);
            totalSeconds += (Integer.parseInt(secondsAndMinutes[1]) * 60);

        } catch (NumberFormatException e) {
            throwCronFormatException(cronString);
        }

        return totalSeconds;
    }

    private static void throwCronFormatException(String cronString) throws InvalidCronStringFormat {
        String errorMessage = String.format("""
                Error Message:
                    Expected a cron string with two integer values e.g. (cron="5 0") but received => %s
                """, cronString);
        throw new InvalidCronStringFormat(errorMessage);
    }
}
