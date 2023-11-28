
package top.suilian.aio.Util;

import java.math.BigDecimal;

public class RandomUtilsme {

    /**
     * 生成一个比num小的指定小数位位数的随机数小数
     *
     * @param precision
     * @return
     */
    public static Double getRandom(double num, Integer precision) {
        precision=8;
        double randomNum = Math.floor(Math.random() * Math.pow(10, precision)) / Math.pow(10, precision);

        while (randomNum >= num) {
            randomNum = Math.floor(Math.random() * Math.pow(10, precision)) / Math.pow(10, precision);
        }
        return randomNum;
    }

    public static Double getRandomAmount(double min,double max) {

        return min + Math.random() * (max - min);
    }


    /**
     * 获得一个比num小的整数
     * @param num
     * @return
     */
    public static Integer getRandom(Integer num) {
        Double random = getRandom(num, 0);
        return random.intValue();
    }

    /**
     * 获取小数位
     * @param number
     * @return
     */
    public static int getNumberDecimalDigits(double number) {
        String moneyStr = String.valueOf(number);
        String[] num = moneyStr.split("\\.");
        if (num.length == 2) {
            for (;;){
                if (num[1].endsWith("0")) {
                    num[1] = num[1].substring(0, num[1].length() - 1);
                }else {
                    break;
                }
            }
            return num[1].length();
        }else {
            return 0;
        }
    }


    public static int getNumberDecimalDigits(String moneyStr) {
        String[] num = moneyStr.split("\\.");
        if (num.length == 2) {
            for (;;){
                if (num[1].endsWith("0")) {
                    num[1] = num[1].substring(0, num[1].length() - 1);
                }else {
                    break;
                }
            }
            return num[1].length();
        }else {
            return 0;
        }
    }


    public static void main(String[] args) {
        System.out.println(  new BigDecimal(getRandomAmount(800,1200)).toPlainString());
    }
}
