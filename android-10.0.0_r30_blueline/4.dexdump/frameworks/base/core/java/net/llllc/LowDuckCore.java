package net.llllc;

import android.app.ActivityThread;
import android.app.Application;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LowDuckCore {
    //dex ClassLoader 相关处理 

    //为了反射封装，根据类名和字段名，反射获取字段
    public static Field getClassField(ClassLoader classloader, String class_name,
                                      String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object getClassFieldObject(ClassLoader classloader, String class_name, Object obj,
                                             String filedName) {

        try {
            Class obj_class = classloader.loadClass(class_name);//Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            Object result = null;
            result = field.get(obj);
            return result;
            //field.setAccessible(true);
            //return field;
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object invokeStaticMethod(String class_name,
                                            String method_name, Class[] pareTyple, Object[] pareVaules) {

        try {
            Class obj_class = Class.forName(class_name);
            Method method = obj_class.getMethod(method_name, pareTyple);
            return method.invoke(null, pareVaules);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Object getFieldObject(String class_name, Object obj,
                                        String filedName) {
        try {
            Class obj_class = Class.forName(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static Application getCurrentApplication(){
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        return mApplication;
    }

    public static ClassLoader getClassloader() {
        ClassLoader resultClassloader = null;
        Object currentActivityThread = invokeStaticMethod(
                "android.app.ActivityThread", "currentActivityThread",
                new Class[]{}, new Object[]{});
        Object mBoundApplication = getFieldObject(
                "android.app.ActivityThread", currentActivityThread,
                "mBoundApplication");
        Application mInitialApplication = (Application) getFieldObject("android.app.ActivityThread",
                currentActivityThread, "mInitialApplication");
        Object loadedApkInfo = getFieldObject(
                "android.app.ActivityThread$AppBindData",
                mBoundApplication, "info");
        Application mApplication = (Application) getFieldObject("android.app.LoadedApk", loadedApkInfo, "mApplication");
        String processName = ActivityThread.currentProcessName();
        Log.e("low_duck", "go into app->" + "packagename:" + processName);
        resultClassloader = mApplication.getClassLoader();
        return resultClassloader;
    }

    public static ClassLoader getClassLoaderByClassName(String clsname){
        Log.e("low_duck", "getClassLoaderByClassName clsname:"+clsname);
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("low_duck", "appClassloader is null");
            return null;
        }
        ClassLoader parentClassloader=appClassloader.getParent();
        if(appClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
        {
            try {
                appClassloader.loadClass(clsname);
                return appClassloader;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        while(parentClassloader!=null){
            if(parentClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
            {
                try {
                    appClassloader.loadClass(clsname);
                    return appClassloader;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            parentClassloader=parentClassloader.getParent();
        }
        return null;
    }

}
