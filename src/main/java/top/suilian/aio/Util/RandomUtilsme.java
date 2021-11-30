
package top.suilian.aio.Util;

import java.util.Random;



public class RandomUtilsme {

    /**
     * 生成一个比num小的指定小数位位数的随机数小数
     *
     * @param precision
     * @return
     */
    public static Double getRandom(double num, Integer precision) {
        int maxNum = new Double((Math.pow(10, precision) * num)).intValue();
        double v = 0;
        if (num > 0) {
            int i = new Random().nextInt(maxNum);
            v = i / Math.pow(10, precision);
        }else {
           return getRandom(num,precision) ;
        }
        return v;
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
        System.out.println(  org.apache.commons.lang.math.RandomUtils.nextInt(10));
    }
}
