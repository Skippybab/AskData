package com.mt.agent.utils;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    public static final String DATE_PATTERN = "yyyyMMdd";
    public static final String DATE_TIME_PATTERN = "yyyyMMddHHmmss";
    public static final String TIME_PATTERN = "HHmmss";

    public DateUtil() {
    }

    public static String format(long millis, String pattern) {
        return format(new Date(millis), pattern);
    }

    public static String format(Date date, String pattern) {
        DateFormat formatter = new SimpleDateFormat(pattern);
        return formatter.format(date);
    }

    public static String formatDate(Date date) {
        return format(date, "yyyyMMdd");
    }

    /** @deprecated */
    @Deprecated
    public static String formartDate(Date date) {
        return format(date, "yyyyMMdd");
    }

    public static String formatDate(String date) {
        String sp = null;
        String tp = null;
        if (date.length() == 8) {
            sp = "yyyyMMdd";
            tp = "yyyy-MM-dd";
        } else {
            if (date.length() != 10) {
                throw new IllegalArgumentException("不支持的日期字符串:" + date);
            }

            sp = "yyyy-MM-dd";
            tp = "yyyyMMdd";
        }

        return format(parse(date, sp), tp);
    }

    public static String formatTime(Date date) {
        return format(date, "HHmmss");
    }

    /** @deprecated */
    @Deprecated
    public static String formartTime(Date date) {
        return format(date, "HHmmss");
    }

    public static String formatTime(String date) {
        String sp = null;
        String tp = null;
        if (date.length() == 6) {
            sp = "HHmmss";
            tp = "HH:mm:ss";
        } else {
            if (date.length() != 8) {
                throw new IllegalArgumentException("不支持的时间字符串:" + date);
            }

            sp = "HH:mm:ss";
            tp = "HHmmss";
        }

        return format(parse(date, sp), tp);
    }

    /** @deprecated */
    @Deprecated
    public static String formartTime(String date) {
        return formatTime(date);
    }

    public static String formatDateTime(Date date) {
        return format(date, "yyyyMMddHHmmss");
    }

    /** @deprecated */
    @Deprecated
    public static String formartDateTime(Date date) {
        return format(date, "yyyyMMddHHmmss");
    }

    public static String formatDateTime(String date) {
        String sp = null;
        String tp = null;
        if (date.length() == 14) {
            sp = "yyyyMMddHHmmss";
            tp = "yyyy-MM-dd HH:mm:ss";
        } else {
            if (date.length() != 19) {
                throw new IllegalArgumentException("不支持的日期时间字符串:" + date);
            }

            sp = "yyyy-MM-dd HH:mm:ss";
            tp = "yyyyMMddHHmmss";
        }

        return format(parse(date, sp), tp);
    }

    /** @deprecated */
    @Deprecated
    public static String formartDateTime(String date) {
        return formatDateTime(date);
    }

    public static String formatCurrent(String pattern) {
        return format(new Date(), pattern);
    }

    public static String formatCurrentDate() {
        return format(new Date(), "yyyyMMdd");
    }

    /** @deprecated */
    @Deprecated
    public static String formartCurrentDate() {
        return format(new Date(), "yyyyMMdd");
    }

    public static String formatCurrentTime() {
        return format(new Date(), "HHmmss");
    }

    /** @deprecated */
    @Deprecated
    public static String formartCurrentTime() {
        return format(new Date(), "HHmmss");
    }

    /** @deprecated */
    @Deprecated
    public static String formartCurrentDateTime() {
        return format(new Date(), "yyyyMMddHHmmss");
    }

    public static String formatCurrentDateTime() {
        return format(new Date(), "yyyyMMddHHmmss");
    }

    public static Date getCurrentDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(11, 0);
        cal.set(12, 0);
        cal.set(13, 0);
        cal.set(14, 0);
        return cal.getTime();
    }

    public static Date getTheDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(11, 0);
        cal.set(12, 0);
        cal.set(13, 0);
        cal.set(14, 0);
        return cal.getTime();
    }

    public static int compareDate(Date start, Date end) {
        if (start == null && end == null) {
            return 0;
        } else if (end == null) {
            return 1;
        } else {
            if (start == null) {
                start = new Date();
            }

            start = getTheDate(start);
            end = getTheDate(end);
            return start.compareTo(end);
        }
    }

    public static Date parse(String dateString, String pattern) {
        DateFormat formatter = new SimpleDateFormat(pattern);

        try {
            return formatter.parse(dateString);
        } catch (ParseException var4) {
            throw new RuntimeException(var4);
        }
    }

    public static Date addYears(Date date, int amount) {
        return add(date, 1, amount);
    }

    public static Date addMonths(Date date, int amount) {
        return add(date, 2, amount);
    }

    public static Date addWeeks(Date date, int amount) {
        return add(date, 3, amount);
    }

    public static Date addDays(Date date, int amount) {
        return add(date, 5, amount);
    }

    public static Date addHours(Date date, int amount) {
        return add(date, 11, amount);
    }

    public static Date addMinutes(Date date, int amount) {
        return add(date, 12, amount);
    }

    public static Date addSeconds(Date date, int amount) {
        return add(date, 13, amount);
    }

    public static Date addMilliseconds(Date date, int amount) {
        return add(date, 14, amount);
    }

    private static Date add(Date date, int calendarField, int amount) {
        if (date == null) {
            throw new IllegalArgumentException("日期对象不允许为null!");
        } else {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(calendarField, amount);
            return c.getTime();
        }
    }

    public static String friendlyTime(Date time) {
        if (time == null) {
            return "时间不明";
        } else {
            int ct = (int)((System.currentTimeMillis() - time.getTime()) / 1000L);
            if (ct < 3600) {
                return Math.max(ct / 60, 1) + "分钟前";
            } else if (ct >= 3600 && ct < 86400) {
                return ct / 3600 + "小时前";
            } else if (ct >= 86400 && ct < 2592000) {
                int day = ct / 86400;
                return day > 1 ? day + "天前" : "昨天";
            } else {
                return ct >= 2592000 && ct < 31104000 ? ct / 2592000 + "月前" : ct / 31104000 + "年前";
            }
        }
    }

    public static Date getMonthFirstDay(int months) {
        Calendar cal = Calendar.getInstance();
        cal.set(5, 1);
        cal.add(2, months);
        return cal.getTime();
    }

    public static Date getMonthLastDay(int months) {
        Calendar cal = Calendar.getInstance();
        cal.set(5, 1);
        cal.add(2, months + 1);
        cal.add(5, -1);
        return cal.getTime();
    }
}
