package com.zzg.robotium.lib;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Debug;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * 全局共用的常用方法类
 *
 * @author Abelzzg
 */
public class Tools {

    private static String TAG = "ROBOT";

    /**
     * 获取location area code 位置区编码
     *
     * @return
     */
    public static int getLac() {
        GsmCellLocation location = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) CrashHandler.globalContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            location = (GsmCellLocation) telephonyManager
                    .getCellLocation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (location == null) {
            return 0;
        } else {
            int lac = location.getLac();
            return lac;
        }
    }

    /**
     * 手机信号覆盖区域的的编号ID 获取cell_id
     *
     * @return
     */
    public static int getCellId() {
        // 中国移动和中国联通获取LAC、CID的方式
        GsmCellLocation location = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) CrashHandler.globalContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            location = (GsmCellLocation) telephonyManager
                    .getCellLocation();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getCellId失败");
        }
        if (location == null) {
            return 0;
        } else {
            int cellId = location.getCid();
            return cellId;
        }
    }

    /**
     * 获得当前使用语言
     *
     * @return
     */
    public static String getLan() {
        String lan = Locale.getDefault().getLanguage();
        return lan;
    }

    /**
     * 获取软件包名
     *
     * @return
     */
    public static String getPackageName() {
        return CrashHandler.globalContext.getPackageName();
    }

    /**
     * 获取versionCode（ANDROID版本号）
     */
    public static String getDeviceVersion() {
        String sdkVersion = Build.VERSION.RELEASE;
        return sdkVersion;
    }

    /**
     * 是否为调试模式
     *
     * @return
     */
    public static boolean isDebuggable() {
        try {
            PackageInfo pinfo = CrashHandler.globalContext.getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            if (pinfo != null) {
                int flags = pinfo.applicationInfo.flags;
                return (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取平台号+版本号+渠道号
     *
     * @return
     */
    public static String getVersionName() {
        try {
            PackageInfo pinfo = CrashHandler.globalContext.getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            String versionName = pinfo.versionName;
            if (null != versionName) {
                return versionName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取使用内存大小
     */
    public static String getMemory() {
        int pss = 0;
        ActivityManager myAM = (ActivityManager) CrashHandler.globalContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        String packageName = CrashHandler.globalContext.getPackageName();
        List<ActivityManager.RunningAppProcessInfo> appProcesses = myAM
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(packageName)) {
                int pids[] = {appProcess.pid};
                Debug.MemoryInfo self_mi[] = myAM.getProcessMemoryInfo(pids);
                pss = self_mi[0].getTotalPss();
            }
        }
        return Formatter.formatFileSize(CrashHandler.globalContext, pss * 1024);
    }

    public static String getTotalMemory() {
        String str1 = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
            arrayOfString = str2.split("\\s+");
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
            localBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Formatter.formatFileSize(CrashHandler.globalContext, initial_memory);// Byte转换为KB或者MB，内存大小规格化
    }



    private String getAvailMemory() {// 获取android当前可用内存大小
        ActivityManager am = (ActivityManager) CrashHandler.globalContext.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        //mi.availMem; 当前系统的可用内存
        return Formatter.formatFileSize(CrashHandler.globalContext, mi.availMem);// 将获取的内存大小规格化
    }


    /**
     * 获取网络信息
     * @return
     */
    public static String getNetworkType() throws Exception {
        StringBuffer sInfo = new StringBuffer();
        ConnectivityManager connectivity = (ConnectivityManager) CrashHandler.globalContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo activeNetInfo = connectivity.getActiveNetworkInfo();// 如果不是wifi则为空
            NetworkInfo mobNetInfo = connectivity
                    .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);// 判断是否是手机网络
            // 非手机网络
            if (activeNetInfo != null) {
                sInfo.append(activeNetInfo.getTypeName());
            }
            // 手机网络
            else if (mobNetInfo != null) {
                sInfo.append(mobNetInfo.getSubtypeName());
            }
        }
        return sInfo.toString();
    }

    /**
     * 获得CPU使用率
     */
    public static String getCpuInfo() {
        int cpu = 0;
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();
            String[] toks = load.split(" ");
            long idle1 = Long.parseLong(toks[5]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
                    + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
                    + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
            reader.seek(0);
            load = reader.readLine();
            reader.close();
            toks = load.split(" ");
            long idle2 = Long.parseLong(toks[5]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
                    + Long.parseLong(toks[4]) + Long.parseLong(toks[6])
                    + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            cpu = (int) (100 * (cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1)));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return cpu + "%";
    }

    public static String getDpi() {
        DisplayMetrics metric = new DisplayMetrics();
        Activity activity = (Activity) (CrashHandler.globalContext);
        activity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int densityDpi = metric.densityDpi;  // 屏幕密度DPI（120 / 160 / 240）
        return densityDpi + "dpi";
    }

    public static String getResolution() {
        WindowManager wm = (WindowManager) CrashHandler.globalContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        return width + "*" + height;
    }

    /**
     * 获取IP
     * @return
     */
    public static String getLocalIPV6IpAddress() {

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("IpAddress", ex.toString());
        }
        return null;
    }

    public static double getLatitude() {
        Location loc = null;
        try {
            loc = getLoc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loc != null) {
            return loc.getLatitude();
        } else
            return 0;
    }

    public static double getLongitude() {
        Location loc = null;
        try {
            loc = getLoc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (loc != null) {
            return loc.getLongitude();
        } else
            return 0;
    }


    /**
     * 获取wifi的mac地址
     *
     * @return
     */
    public static String getMacAddress() throws Exception {
        PackageManager pm = CrashHandler.globalContext.getPackageManager();
        boolean permission1 = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.ACCESS_NETWORK_STATE", "packageName"));
        boolean permission2 = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.ACCESS_WIFI_STATE", "packageName"));
        if (permission1 && permission2) {
            WifiManager wifi = (WifiManager) CrashHandler.globalContext
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            String mac = info.getMacAddress();
            if (null != mac) {
                return mac;
            }
        }
        return "";
    }


    public static Location getLoc() {
        Location location = null;
        try {
            LocationManager locationManager = (LocationManager) CrashHandler.globalContext
                    .getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);// 高精度
            criteria.setAltitudeRequired(false);// 不要求海拔
            criteria.setBearingRequired(false);// 不要求方位
            criteria.setCostAllowed(true);// 允许有花费
            criteria.setPowerRequirement(Criteria.POWER_LOW);// 低功耗
            // 从可用的位置提供器中，匹配以上标准的最佳提供器
            String provider = locationManager.getBestProvider(criteria, true);
            // 获得最后一次变化的位置
            location = locationManager.getLastKnownLocation(provider);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "获取loc失败");
        }
        return location;
    }

    /**
     * json统一时间格式
     *
     * @return
     */
    public static String getCurrentTime4Json() {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String current_time = sdf.format(date);
        return current_time;
    }


    /**
     * 获得手机型号
     *
     * @return
     */
    public static String getPhoneModel() {
        try {
            String phoneVersion = Build.MODEL;
            if (null != phoneVersion) {
                return phoneVersion;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获得手机IMEI
     *
     * @return
     */
    public static String getIMEI() throws Exception {
        PackageManager pm = CrashHandler.globalContext.getPackageManager();
        boolean permission = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.READ_PHONE_STATE", "packageName"));
        if (permission) {
            TelephonyManager tm = (TelephonyManager) CrashHandler.globalContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            if (null != imei) {
                return imei;
            }
        }
        return "";
    }

    /**
     * 获得手机IMSI
     *
     * @return
     */
    public static String getIMSI() throws Exception {
        try {
            TelephonyManager tm = (TelephonyManager) CrashHandler.globalContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String imsi = tm.getNetworkOperator();
            if (null != imsi) {
                return imsi;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 判断当前是否符合桌面显示的对话框
     *
     * @param context
     * @return
     */
    public static boolean pushDeskFlag(Context context) {
        boolean deskFlag = false;
        String taskNameTop = "";
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(100);
        if (tasksInfo.size() > 0) {
            taskNameTop = tasksInfo.get(0).topActivity.getPackageName();
        } else {
            return true;
        }
        for (int i = 0; i < tasksInfo.size(); i++) {
            if (context.getPackageName().equals(
                    tasksInfo.get(i).topActivity.getPackageName())) {
                return false;
            }
        }
        List<String> names = getAllTheLauncher(context);
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equals(taskNameTop)) {
                deskFlag = true;
            }
        }
        return deskFlag;
    }

    /**
     * 获取所有的launcher信息
     *
     * @param context
     * @return
     */
    private static List<String> getAllTheLauncher(Context context) {
        List<String> names = null;
        PackageManager pkgMgt = context.getPackageManager();
        Intent it = new Intent(Intent.ACTION_MAIN);
        it.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> ra = pkgMgt.queryIntentActivities(it, 0);
        if (ra.size() != 0) {
            names = new ArrayList<String>();
        }
        for (int i = 0; i < ra.size(); i++) {
            String packageName = ra.get(i).activityInfo.packageName;
            names.add(packageName);
        }
        return names;
    }

    /**
     * 获取目标APK的编译版本
     * @return
     */
    public static String getTargetAndroidVersion() {
        int targetSdkVersion = CrashHandler.globalContext.getApplicationInfo().targetSdkVersion;
        return "android-"+targetSdkVersion + "";
    }
}
