package com.bydauto.car.i_key.cardv_table.util;

/**
 * author：hm2359767 on 17/5/16 09:31
 * mail：huang.min12@byd.com
 * tele: 18666287409
 */

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogcatDate {
    public static String getFileName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String date = format.format(new Date(System.currentTimeMillis()));
        return date;
    }

    public static String getDateEN() {
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date1 = format1.format(new Date(System.currentTimeMillis()));
        return date1;
    }
}
