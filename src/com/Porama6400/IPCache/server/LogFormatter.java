package com.Porama6400.IPCache.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {
    private final boolean displayDate;

    public LogFormatter(boolean displayDate) {
        this.displayDate = displayDate;
    }


    @Override
    public String format(LogRecord record) {


        StringBuilder sb = new StringBuilder();

        if (displayDate) sb.append("[")
                .append(new Date(record.getMillis()))
                .append("]");

        sb.append("[")
                .append(record.getLevel().getName())
                .append("] ")
                .append(record.getMessage())
                .append("\n");

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
                // ignore
            }
        }

        return sb.toString();
    }
}
