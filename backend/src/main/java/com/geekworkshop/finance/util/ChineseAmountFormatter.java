package com.geekworkshop.finance.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ChineseAmountFormatter {
    private static final String[] DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] UNITS = {"", "拾", "佰", "仟"};
    private static final String[] GROUPS = {"", "万", "亿"};

    private ChineseAmountFormatter() {}

    public static String format(BigDecimal amount) {
        if (amount == null) return "-";
        long cents = amount.abs().setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        long integer = cents / 100;
        int jiao = (int) (cents / 10 % 10);
        int fen = (int) (cents % 10);
        if (integer == 0) {
            return "人民币" + (jiao > 0 ? DIGITS[jiao] + "角" : "零")
                    + (fen > 0 ? DIGITS[fen] + "分" : jiao > 0 ? "" : "元整");
        }
        StringBuilder result = new StringBuilder();
        int groupIndex = 0;
        boolean pendingZero = false;
        while (integer > 0) {
            int part = (int) (integer % 10000);
            if (part == 0) {
                pendingZero = result.length() > 0;
            } else {
                StringBuilder partText = new StringBuilder();
                boolean zero = false;
                for (int i = 0; i < 4; i++) {
                    int digit = part % 10;
                    if (digit == 0) {
                        zero = partText.length() > 0;
                    } else {
                        if (zero) partText.insert(0, DIGITS[0]);
                        partText.insert(0, DIGITS[digit] + UNITS[i]);
                        zero = false;
                    }
                    part /= 10;
                }
                if (pendingZero) result.insert(0, DIGITS[0]);
                result.insert(0, partText + GROUPS[groupIndex]);
                pendingZero = false;
            }
            integer /= 10000;
            groupIndex++;
        }
        result.insert(0, "人民币").append("元");
        if (jiao == 0 && fen == 0) return result.append("整").toString();
        if (jiao > 0) result.append(DIGITS[jiao]).append("角");
        else if (fen > 0) result.append("零");
        if (fen > 0) result.append(DIGITS[fen]).append("分");
        return result.toString();
    }
}
