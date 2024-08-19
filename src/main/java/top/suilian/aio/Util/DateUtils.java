package top.suilian.aio.Util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DateUtils {

public static final String DATE_FORMAT = "yyyy-MM-dd";

	public static final String DATE_FORMAT1 = "yyyyMMdd";

	public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final String DATE_FORMAT2 = "yyMMdd";




	public static String convertTimestampToString(long timestampInMillis) {
		// 将时间戳转换为Instant对象
		Instant instant = Instant.ofEpochMilli(timestampInMillis);

		// 转换为LocalDateTime并指定时区
		LocalDateTime dateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

		// 创建DateTimeFormatter并按指定格式格式化日期时间
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
		return dateTime.format(formatter);
	}

	/**
	 *4位太阳码
	 */
	public static Date convertTimeNo4ToDate(String timeNo4){
		int yearNo = Integer.parseInt(timeNo4.substring(0, 1));
		int dayNum = Integer.parseInt(timeNo4.substring(1, 4));

		Calendar cal=Calendar.getInstance();
		int year= cal.get(Calendar.YEAR);
		Map<Integer,Integer> yearNo2Year=new HashMap<Integer,Integer>();
		for(int index=-4; index<= 5; index++){
			int newYear = year +index;
			yearNo2Year.put(newYear%10, newYear);
		}
		int timeYear = yearNo2Year.get(yearNo);
		cal.set(Calendar.YEAR, timeYear);
		cal.set(Calendar.DAY_OF_YEAR, dayNum);
		return convertStrToTime(convertDateToStr(cal.getTime())+" 23:59:59");
	}



	/**
     * 获取当前时间
     */
    public static String getCurrentTime(String dateFormat) {
        java.text.Format formatter = new SimpleDateFormat(dateFormat);
        return formatter.format(new Date());
    }

	public static String getCurrentTime() {
		java.text.Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return formatter.format(new Date());
	}

    /**
	 * 日期转换成字符串 yyyy-MM-dd HH:mm:ss
	 *
	 * @param date
	 * @return
	 */
	public static String convertTimeToStr(Date date) {
		if(date==null){
			return "0000-00-00 00:00:00";
		}
		java.text.Format formatter = new SimpleDateFormat(DATETIME_FORMAT);
		return formatter.format(date);
	}


	/**
	 * 日期转换成字符串
	 * @return
	 */
	public static String convertTimeToStr(Date date,String format) {
		if(date==null){
			return "";
		}
		java.text.Format formatter = new SimpleDateFormat(format);
		return formatter.format(date);
	}


	/**
	 * 日期转换成字符串 并转成的是当天的最后时间  2022-11-12 11:01:01 -->2022-11-12 23:59:59
	 * @param date
	 * @return
	 */
	public static String convertLastTimeToStr(Date date) {
		return convertTimeToStr(date,"yyyy-MM-dd 23:59:59");
	}

	/**
	 * 日期转换成字符串
	 * yyyy-MM-dd
	 * @param date
	 * @return
	 */
	public static String convertDateToStr(Date date) {
		if(date==null){
			return "0000-00-00";
		}
		java.text.Format formatter = new SimpleDateFormat(DATE_FORMAT);
		return formatter.format(date);

	}

	/**
	 * 日期转换成字符串
	 * yyyyMMdd
	 * @param date
	 * @return
	 */
	public static String convertDateToStr1(Date date) {
		if(date==null){
			return "";
		}
		java.text.Format formatter = new SimpleDateFormat(DATE_FORMAT1);
		return formatter.format(date);

	}

	public static Date convertStrToTime( String str) {
		return convertStrToDate( str, DATETIME_FORMAT);
	}

	public static Date convertStrToDate( String str) {
		return convertStrToDate( str, DATE_FORMAT);
	}

	public static Date convertStrToDate1( String str) {
		return convertStrToDate( str, DATETIME_FORMAT);
	}

	public static Date convertStrToDate( String str,String dateFormat) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
			return sdf.parse(str);
		} catch (ParseException e) {
			throw new RuntimeException("您输入的数据格式不对");
		}
	}

	/**
	 *时间间距
	 *		时 分 秒
	 */
	public static long[]  betweenDistanceOfSec(long dis){
		dis = Math.abs(dis);
		long house = dis/(60*60);
		dis = dis%(60*60);
		long minute = dis/(60);
		dis = dis%(60);
		long second = dis;
		return new long[]{house,minute,second};
	}

	/**
	 *时间间距
	 *		时 分 秒
	 */
	public static long[]  betweenDistanceOfMin(long dis){
		dis = Math.abs(dis);
		long house = dis/(60*60);
		dis = dis%(60*60);
		long minute = dis/(60);
		return new long[]{house,minute};
	}

	/**
	 * 获取今天开始时间
	 *
	 * @return
	 */
	public static Date getTodayStart() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	public static Date getDateStart(Date data) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

	/**
	 * 获取年月日六位日期（180420）
	 *
	 * @param date
	 * @return
	 */
	public static String getYymmdd(Date date) {
		if (date == null) {
			return "";
		}
		try {
			java.text.Format formatter = new SimpleDateFormat(DATE_FORMAT2);
			return formatter.format(date);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * 获取本月倒数第n天
	 *
	 * @param n
	 * @return
	 */
	public static Date getMonthEndBackwardDate(int n) {
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - (n - 1));
			String dateTimeStr = convertDateToStr(calendar.getTime()) + " 00:00:00";
			Date dateTime = convertStrToTime(dateTimeStr);
			return dateTime;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 获取上个月盘月份
	 *
	 * @param checkMonth
	 * @return
	 */
	public static String getPreviousCheckMonth(String checkMonth) {
		try {
			Calendar cal = Calendar.getInstance();
			Date checkMonthTime = convertStrToDate(checkMonth, "yyyyMM");
			cal.setTime(checkMonthTime);
			cal.add(Calendar.MONTH, -1);
			return convertTimeToStr(cal.getTime(), "yyyyMM");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 获取下个月盘月份
	 *
	 * @param checkMonth
	 * @return
	 */
	public static String getNextCheckMonth(String checkMonth) {
		try {
			Calendar cal = Calendar.getInstance();
			Date checkMonthTime = convertStrToDate(checkMonth, "yyyyMM");
			cal.setTime(checkMonthTime);
			cal.add(Calendar.MONTH, 1);
			return convertTimeToStr(cal.getTime(), "yyyyMM");
		} catch (Exception e) {
			return null;
		}
	}

	public static String getCheckMonthStart(String checkMonth) {
		try {
			Date checkMonthTime = convertStrToDate(checkMonth, "yyyyMM");
			String checkMonthStart = convertTimeToStr(checkMonthTime, "yyyy-MM") + "-01" + " 00:00:00";
			return checkMonthStart;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 将String类型转换为 Date 类型
	 * @param workDate 需要转换的时间
	 * @return 转换后的时间
	 * @author 高志利
	 */
	public static  Date getWorkDate(String workDate){
		if (workDate == null){
			return null;
		}
		Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(workDate);
        } catch (ParseException e) {
            return null;
        }
        return date;
	}

	public static  Date getCurrentDtime(){
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT);
		Date date1 = convertStrToDate(sdf.format(date));
		return date1;
	}

	/**
	 * 计算两个日期间隔的天数
	 * @param before
	 * @param after
	 * @return
	 */
	public static long getDistanceOfTwoDate(Date before, Date after) {
		return (before.getTime() - after.getTime()) / (1000 * 60 * 60 * 24);
	}

	public static String convertTimestampToString1(long timestamp) {
		Date date = new Date(timestamp * 1000); // 转换为毫秒级
		SimpleDateFormat sdf = new SimpleDateFormat(DATETIME_FORMAT);
		return sdf.format(date);
	}

	/**
	 * 获取传入时间的前后多少天的时间
	 * @param dateFormat
	 * @param dateNum
	 * @return
	 */
	public static String getBetweenTime(Date date,String dateFormat, Integer dateNum) {
		DateFormat df = new SimpleDateFormat(dateFormat);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, dateNum);
		date = calendar.getTime();
		return df.format(date);
	}
	public static String getBetweenLastTime(Date date, Integer dateNum) {
		return getBetweenTime( date,"yyyy-MM-dd 23:59:59",dateNum);
	}

	public static String getBetweenStartTime(Date date, Integer dateNum) {
		return getBetweenTime( date,"yyyy-MM-dd 00:00:00",dateNum);
	}

	public static String getBetweenTime(Date date, Integer dateNum) {
		return getBetweenTime( date,DATETIME_FORMAT,dateNum);
	}

	// 获得某天最大时间 2020-02-19 23:59:59
	public static Date getEndOfDay(Date date) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());;
		LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
		return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
	}

	// 获得某天最小时间 2020-02-17 00:00:00
	public static Date getStartOfDay(Date date) {
		LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
		LocalDateTime startOfDay = localDateTime.with(LocalTime.MIN);
		return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
	}



	public static void main(String[] args) {

		System.out.println( getBetweenLastTime(convertStrToTime("2023-09-18 00:00:00"), 5));
	}


}
