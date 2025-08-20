package com.mt.agent.utils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

public class DateExtendUtil {

    /**
     * 获取日期的周一 yyyyMMddHHmmss to yyyyMMdd
     *
     * @param dateStr
     * @return
     */
    public static String getWeekStartDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int mon = calendar.get(Calendar.DAY_OF_WEEK);//获取日期在本周属于第几日
        //Calendar类的一周是从周日到周六，中国习惯周一到周日
        //所以如果是本周第1天，则为周日，在中国应该为本周第7天，其他日期-1即可对应
        mon = mon == 1 ? 7 : mon - 1;
        //获取本周第1天(周一)
        calendar.add(Calendar.DATE, 1 - mon);
        Date monday = calendar.getTime();

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(monday);

        return str;
    }

    /**
     * 获取上周周一  yyyyMMdd
     *
     * @return
     */
    public static String getLastWeekStartDate() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        int mon = calendar.get(Calendar.DAY_OF_WEEK);//获取日期在本周属于第几日
        //Calendar类的一周是从周日到周六，中国习惯周一到周日
        //所以如果是本周第1天，则为周日，在中国应该为本周第7天，其他日期-1即可对应
        mon = mon == 1 ? 7 : mon - 1;
        //获取本周第1天(周一)
        calendar.add(Calendar.DATE, 1 - mon);
        Date monday = calendar.getTime();

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(monday);

        return str;
    }

    /**
     * 获取一周的周日 yyyyMMddHHmmss to yyyyMMdd
     *
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static String getWeekEndDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int mon = calendar.get(Calendar.DAY_OF_WEEK);//获取日期在本周属于第几日
        //Calendar类的一周是从周日到周六，中国习惯周一到周日
        //所以如果是本周第1天，则为周日，在中国应该为本周第7天，其他日期-1即可对应
        mon = mon == 1 ? 7 : mon - 1;
        //获取本周第1天(周一)
        calendar.add(Calendar.DATE, 1 - mon);
        calendar.add(Calendar.DATE, 6);
        Date monday = calendar.getTime();

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(monday);

        return str;
    }

    /**
     * 获取上周周的周日 yyyyMMddHHmmss to yyyyMMdd
     *
     * @return
     */
    public static String getLastWeekEndDate() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        int mon = calendar.get(Calendar.DAY_OF_WEEK);//获取日期在本周属于第几日
        //Calendar类的一周是从周日到周六，中国习惯周一到周日
        //所以如果是本周第1天，则为周日，在中国应该为本周第7天，其他日期-1即可对应
        mon = mon == 1 ? 7 : mon - 1;
        //获取本周第1天(周一)
        calendar.add(Calendar.DATE, 1 - mon);
        calendar.add(Calendar.DATE, 6);
        Date monday = calendar.getTime();

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(monday);

        return str;
    }


    /**
     * 获取月初 yyyyMMddHHmmss to yyyyMMdd
     *
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static String getMonthStartDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(calendar.getTime());
        return str;
    }

    /**
     * 获取月底 yyyyMMddHHmmss to yyyyMMdd
     *
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static String getMonthEndDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(calendar.getTime());
        return str;
    }

    /**
     * 获取月份第一天 yyyyMM to yyyyMMdd
     *
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static String getMonthFirstDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(calendar.getTime());
        return str;
    }

    /**
     * 获取月份最后一天 yyyyMM to yyyyMMdd
     *
     * @param dateStr
     * @return
     * @throws ParseException
     */
    public static String getMonthLastDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        String str = sdf2.format(calendar.getTime());
        return str;
    }

    /**
     * 查询日期相差天数，第一个日期大
     *
     * @param day1
     * @param day2
     * @return
     */
    public static long getDaySubNum(String day1, String day2) {
        LocalDate startDate = LocalDate.parse(day1, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate endDate = LocalDate.parse(day2, DateTimeFormatter.ofPattern("yyyyMMdd"));

        long daysDiff = ChronoUnit.DAYS.between(endDate, startDate);
        return daysDiff;
    }

    /**
     * 获取当前日期格式
     *
     * @return
     */
    public static String getDateFormat() {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sf.format(new Date());
    }

    /**
     * 根据格式获取当前日期
     *
     * @param format 日期格式
     * @return 当前日期字符串
     * @author lfz
     * @date 2023/11/23 10:51
     */
    public static String getDateByFormat(String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        return sf.format(new Date());
    }

    /**
     * 根据timeType检查时间格式
     */
    public static Boolean checkTimeFormat(String time, int timeType) {
        SimpleDateFormat sdf = null;
        if (timeType == 1) {
            // 月
            sdf = new SimpleDateFormat("yyyyMM");

        } else if (timeType == 2) {
            // 季
            sdf = new SimpleDateFormat("yyyyMM");
        } else if (timeType == 3) {
            // 年
            sdf = new SimpleDateFormat("yyyy");
        }
        assert sdf != null;
        sdf.setLenient(false); // 设置严格模式，不允许不严格的日期解析
        try {
            sdf.parse(time); // 尝试解析字符串
            return true; // 如果没有抛出异常，说明格式正确
        } catch (ParseException e) {
            return false; // 解析失败，说明格式不正确
        }
    }

    /**
     * 根据格式获取n小时后的日期
     *
     * @param num    小时差值
     * @param format 日期格式
     * @return 时间字符串
     * @author lfz
     * @date 2023/11/10 17:33
     */
    public static String getAddHourByFormat(int num, String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, num);
        return sf.format(calendar.getTime());
    }

    /**
     * 根据格式获取该日期n小时后的日期
     *
     * @param date   日期
     * @param num    小时差值
     * @param format 日期格式
     * @return 时间字符串
     * @author lfz
     * @date 2023/12/5 18:09
     */
    public static String getAddHourByFormat(Date date, int num, String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, num);
        return sf.format(calendar.getTime());
    }

    /**
     * 获取n小时后的日期
     *
     * @param num 小时差值
     * @return 日期
     * @author cpx
     * @date 2023/12/5 17:46
     */
    public static Date getAddHour(int num) {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, num);
        return calendar.getTime();
    }

    /**
     * 获取n天后的日期
     *
     * @param num    天数差值
     * @param format 日期格式
     * @return 结果日期
     * @author lfz
     * @date 2023/11/21 15:30
     */
    public static String getAddDayByFormat(int num, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, num);

        return sdf.format(calendar.getTime());
    }

    /**
     * 获取n秒后的时间
     *
     * @param num    秒差值
     * @param time   时间
     * @param format 格式化字符串
     * @return 结果时间
     * @author lfz
     * @date 2024/1/4 14:32
     */
    public static String getAddSecondByFormat(String time, int num, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sdf.parse(time));
        calendar.add(Calendar.SECOND, num);

        return sdf.format(calendar.getTime());
    }

    /**
     * 获取指定日期n天后的日期
     *
     * @param dateStr 指定日期
     * @param num     天数差值
     * @param format  日期格式
     * @return 结果日期
     * @author lfz
     * @date 2023/11/23 13:44
     */
    public static String getAddDayByFormat(String dateStr, int num, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.add(Calendar.DATE, num);
        Date monday = calendar.getTime();

        return sdf.format(monday);
    }

    /**
     * 获取指定日期n个月后的日期
     *
     * @param dateStr 指定日期
     * @param num     月份差值
     * @param format  日期格式
     * @return 结果日期
     * @author lfz
     * @date 2023/11/23 13:49
     */
    public static String getAddMonthByFormat(String dateStr, int num, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.add(Calendar.MONTH, num);
        Date monday = calendar.getTime();

        return sdf.format(monday);
    }

    /**
     * 获取n个月后的日期
     *
     * @param num    天数差值
     * @param format 日期格式
     * @return 结果日期
     * @author lfz
     * @date 2023/11/23 13:49
     */
    public static String getAddMonthByFormat(int num, String format) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        calendar.add(Calendar.MONTH, num);
        Date date = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        return sdf.format(date);
    }

    /**
     * 获取n分钟后的时间
     *
     * @param time   时间
     * @param minute 添加分钟数
     * @param format 日期格式
     * @return 结果时间
     * @author lfz
     * @date 2023/11/15 10:59
     */
    public static String getAddMinutByFormat(String time, int minute, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = sdf.parse(time);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);

        return sdf.format(calendar.getTime());
    }

    /**
     * 获得指定日期n年后的日期
     *
     * @param dateStr 指定日期
     * @param num     年份差值
     * @param format  日期格式
     * @return 结果日期
     * @author lfz
     * @date 2023/11/23 14:03
     */
    public static String getAddYearByFormat(String dateStr, int num, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = sdf.parse(dateStr);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.add(Calendar.YEAR, num);
        Date monday = calendar.getTime();

        return sdf.format(monday);
    }

    /**
     * 获取当前年份n年后的日期
     *
     * @author lfz
     * @date 2024/12/3 9:39
     * @param num 年份差值
     * @param format 日期格式
     * @return 结果日期
     */
    public static String getAddYearByFormat(int num, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.add(Calendar.YEAR, num);
        Date monday = calendar.getTime();

        return sdf.format(monday);
    }

    /**
     * 根据当前月份获取对应季度首月 yyyyMMdd to yyyyMMdd
     *
     * @param monthStr
     * @return
     */
    public static String getQuarterDate(String monthStr) {
        String year = monthStr.substring(0, 4);
        if ("0".equals(monthStr.substring(4, 5))) {
            int month = Integer.parseInt(monthStr.substring(5, 6));
            if (month >= 1 && month <= 3) {
                return year + "01";
            } else if (month >= 4 && month <= 6) {
                return year + "04";
            } else if (month >= 7 && month <= 9) {
                return year + "07";
            }
        } else {
            int month = Integer.parseInt(monthStr.substring(4, 6));
            if (month >= 10 && month <= 12) {
                return year + "10";
            }
        }
        return null;
    }

    /**
     * 获取上个季度首月 yyyyMMdd to yyyyMMdd
     *
     * @param monthStr
     * @return
     */
    public static String getLastQuarterDate(String monthStr) {
        String year = monthStr.substring(0, 4);
        if ("0".equals(monthStr.substring(4, 5))) {
            int month = Integer.parseInt(monthStr.substring(5, 6));
            if (month == 1) {
                int years = Integer.parseInt(year);
                String annual = String.valueOf(years - 1);
                return annual + "10";
            } else if (month == 4) {
                return year + "01";
            } else if (month == 7) {
                return year + "04";
            }
        } else {
            int month = Integer.parseInt(monthStr.substring(4, 6));
            if (month == 10) {
                return year + "07";
            }
        }
        return null;
    }

    /**
     * 获取下个季度日期 yyyyMMdd to yyyyMMdd
     *
     * @param monthStr
     * @return
     */
    public static String getNextQuarterDate(String monthStr) {
        String year = monthStr.substring(0, 4);
        String day = monthStr.substring(6);
        if ("0".equals(monthStr.substring(4, 5))) {
            int month = Integer.parseInt(monthStr.substring(5, 6));
            if (month == 1 || month == 2 || month == 3) {
                return year + "04" + day;
            } else if (month == 4 || month == 5 || month == 6) {
                return year + "07" + day;
            } else if (month == 7 || month == 8 || month == 9) {
                return year + "10" + day;
            }
        } else {
            int month = Integer.parseInt(monthStr.substring(4, 6));
            if (month == 10 || month == 11 || month == 12) {
                int years = Integer.parseInt(year);
                String annual = String.valueOf(years + 1);
                return annual + "01" + day;
            }
        }
        return null;
    }

    /**
     * 将时间戳添加分钟返回
     *
     * @param timestamp 时间戳
     * @param minute    分钟
     * @return 添加后的时间戳字符串
     * @author lfz
     * @date 2023/11/14 18:27
     */
    public static String timestampAddMinute(String timestamp, int minute) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        calendar.add(Calendar.MINUTE, minute);

        return String.valueOf(calendar.getTimeInMillis());
    }

    /**
     * 获取该时间的秒时间戳
     *
     * @param time 时间
     * @return 时间戳字符串
     * @author lfz
     * @date 2023/11/14 18:27
     */
    public static String getSecondTimestamp(String time) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = sdf.parse(time);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return String.valueOf(calendar.getTimeInMillis() / 1000);
    }

    /**
     * 获取该时间的秒时间戳
     *
     * @param time 时间
     * @return 时间戳字符串
     * @author lfz
     * @date 2023/11/14 18:27
     */
    public static Integer getSecondTimestampInt(String time) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = sdf.parse(time);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return Math.toIntExact(calendar.getTimeInMillis() / 1000);
    }

    /**
     * 获取当前时间的秒时间戳
     *
     * @return 时间戳字符串
     * @author lfz
     * @date 2023/11/14 18:27
     */
    public static String getSecondTimestamp() throws ParseException {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return String.valueOf(calendar.getTimeInMillis() / 1000);
    }

    /**
     * 对时间戳进行格式转化，精确到分钟
     *
     * @param timestamp 时间戳
     * @return 转化后的时间字符串
     * @author lfz
     * @date 2023/11/14 22:37
     */
    public static String getTimestampToDateFormatByminute(String timestamp) {
        Date date = new Date(Long.parseLong(timestamp));
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmm00");
        return sf.format(date);
    }


    /**
     * 比较两个时间戳字符串，前者大于后者返回true
     *
     * @param timestamp1 时间戳1
     * @param timestamp2 时间戳2
     * @return 比较结果
     * @author lfz
     * @date 2023/11/14 22:53
     */
    public static boolean compareTimestampByStr(String timestamp1, String timestamp2) {
        Date date1 = new Date(Long.parseLong(timestamp1));
        Date date2 = new Date(Long.parseLong(timestamp2));
        return date1.after(date2);
    }


    /**
     * 比较两个时间字符串
     *
     * @param time1 时间1
     * @param time2 时间2
     * @return boolean 大于返回true
     * @author cpx
     */
    public static boolean compareTime(String time1, String time2) {
        int i = time1.compareTo(time2);
        return i > 0;
    }

    /**
     * 获取两个月份之间的差值
     *
     * @author lfz
     * @date 2024/7/11 10:19
     * @param month1 月份1
     * @param month2 月份2
     * @return 两个月份之间的差值
     */
    public static int getTowMonthDifference(String month1,String month2){

        YearMonth date1 = YearMonth.parse(month1, DateTimeFormatter.ofPattern("yyyyMM"));
        YearMonth date2 = YearMonth.parse(month2, DateTimeFormatter.ofPattern("yyyyMM"));

        long monthsBetween = ChronoUnit.MONTHS.between(date1, date2);
        return (int) Math.abs(monthsBetween);
    }


    // LocalDateTime转字符串 格式示例：2025年05月29日 10:22:29
    public static String localDateTimeToStringDate(LocalDateTime localDateTime) {
        Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        return sdf.format(date);
    }
}
