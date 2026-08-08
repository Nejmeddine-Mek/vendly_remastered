package org.example.vendly_remastered.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ErrorLogsService {
    private static ErrorLogsService instance;

    private final DateTimeFormatter logTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");

    private ErrorLogsService() {}

    public static synchronized ErrorLogsService getInstance() {
        if (instance == null) {
            instance = new ErrorLogsService();
        }
        return instance;
    }

    public synchronized void log(String contextMessage, Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }

        writeToErrorLedger(out -> {
            out.println("================================================================================");
            out.printf("TIMESTAMP : %s | CONTEXT: %s\n",
                    LocalDateTime.now().format(logTimestampFormatter),
                    contextMessage);
            out.printf("EXCEPTION : %s\n", throwable.getClass().getName());
            out.printf("MESSAGE   : %s\n", throwable.getMessage());
            out.println("STACK TRACE:");
            out.print(sw.toString());
            out.println("================================================================================");
        });
    }

    public synchronized void log(Throwable throwable) {
        log("UNHANDLED_EXCEPTION", throwable);
    }

    private void writeToErrorLedger(ErrorLogOperation operation) {
        String currentDateStr = LocalDate.now().format(fileDateFormatter);
        String dailyFileName = "error_" + currentDateStr + ".txt";

        try (FileWriter fw = new FileWriter(dailyFileName, true);
             PrintWriter out = new PrintWriter(fw)) {

            operation.execute(out);
            out.println();

        } catch (IOException e) {
            System.err.println("[ErrorLogsService] CRITICAL: Disk write violation on error ledger: " + dailyFileName);
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[ErrorLogsService] Error executing internal error logging operation.");
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface ErrorLogOperation {
        void execute(PrintWriter out) throws Exception;
    }
}
