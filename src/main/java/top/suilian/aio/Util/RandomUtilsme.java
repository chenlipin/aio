
package top.suilian.aio.Util;

import java.math.BigDecimal;
import java.util.Random;

public class RandomUtilsme {

    /**
     * 生成一个比num小的指定小数位位数的随机数小数
     *
     * @param precision
     * @return
     */
    public static Double getRandom(double num, Integer precision) {
        precision=12;
        double randomNum = Math.floor(Math.random() * Math.pow(10, precision)) / Math.pow(10, precision);

        while (randomNum >= num) {
            randomNum = Math.floor(Math.random() * Math.pow(10, precision)) / Math.pow(10, precision);
        }
        return randomNum;
    }

    private static final Random RANDOM = new Random();

    public static double generateRandomDecimalLessThan(double num, int precision) {
        // 确保 precision 是非负整数
        if (precision < 0) {
            throw new IllegalArgumentException("Precision must be a non-negative integer.");
        }

        // 生成 [0, num) 范围内的随机数，向下取整到小数点后 precision 位
        double lowerBound = 0;
        double upperBoundExclusive = Math.pow(10, precision) * num;
        Random random = new Random();

        // 使用 nextDouble() 生成 [0, 1) 区间内的随机浮点数
        double randomFraction = random.nextDouble();
        // 将其放大到所需的精度范围，并向下取整
        double scaledRandom = Math.floor(randomFraction * upperBoundExclusive);

        // 将结果除以 10^precision，得到所需精度的小数
        double result = scaledRandom / Math.pow(10, precision);
        BigDecimal bigDecimal = new BigDecimal(result).setScale(precision, BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
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
        System.out.println(  new BigDecimal(generateRandomDecimalLessThan(0.000000009,9)).toPlainString());
    }
}
