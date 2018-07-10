package com.lqy;

import android.content.Context;

/**
 * Created by lqy on 2018/7/6.
 */

public class Utils {
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        if (result == 0) {
            result = (int) getStatusBarHeightPrivate(context);
        }
        return result;
    }


    private static double getStatusBarHeightPrivate(Context context) {
        double statusBarHeight = Math.ceil(25 * context.getResources().getDisplayMetrics().density);
        return statusBarHeight;
    }


}
