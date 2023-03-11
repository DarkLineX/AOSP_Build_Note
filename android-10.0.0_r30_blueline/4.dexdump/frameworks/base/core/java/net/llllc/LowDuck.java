package net.llllc;


// change low_duck

import android.app.ActivityThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.llllc.LowDuckCore.getClassField;
import static net.llllc.LowDuckCore.getClassFieldObject;
import static net.llllc.LowDuckCore.getClassloader;
import static net.llllc.LowDuckCore.getFieldObject;
import static net.llllc.LowDuckCore.loadClassAndInvoke;


public class LowDuck {
    
    //根据classLoader->pathList->dexElements拿到dexFile
    //然后拿到mCookie后，使用getClassNameList获取到所有类名。
    //loadClassAndInvoke处理所有类名导出所有函数
    //dumpMethodCode这个函数是fart自己加在DexFile中的
    public static void fartWithClassLoader(ClassLoader appClassloader) {
        Log.e("low_duck", "fartWithClassLoader "+appClassloader.toString());
        List<Object> dexFilesArray = new ArrayList<Object>();
        Field paist_Field = (Field) getClassField(appClassloader, "dalvik.system.BaseDexClassLoader", "pathList");
        Object pathList_object = getFieldObject("dalvik.system.BaseDexClassLoader", appClassloader, "pathList");
        Object[] ElementsArray = (Object[]) getFieldObject("dalvik.system.DexPathList", pathList_object, "dexElements");
        Field dexFile_fileField = null;
        try {
            dexFile_fileField = (Field) getClassField(appClassloader, "dalvik.system.DexPathList$Element", "dexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method getClassNameList_method = null;
        Method defineClass_method = null;
        Method dumpDexFile_method = null;
        Method dumpMethodCode_method = null;
        Method dumpRepair_method = null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("getClassNameList")) {
                getClassNameList_method = field;
                getClassNameList_method.setAccessible(true);
            }
            if (field.getName().equals("defineClassNative")) {
                defineClass_method = field;
                defineClass_method.setAccessible(true);
            }
            if (field.getName().equals("dumpDexFile")) {
                dumpDexFile_method = field;
                dumpDexFile_method.setAccessible(true);
            }
            if (field.getName().equals("fartextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
            if (field.getName().equals("dumpRepair")) {
                dumpRepair_method = field;
                dumpRepair_method.setAccessible(true);
            }
        }
        Field mCookiefield = getClassField(appClassloader, "dalvik.system.DexFile", "mCookie");
        Log.e("low_duck", "->methods dalvik.system.DexPathList.ElementsArray.length:" + ElementsArray.length);
        for (int j = 0; j < ElementsArray.length; j++) {
            Object element = ElementsArray[j];
            Object dexfile = null;
            try {
                dexfile = (Object) dexFile_fileField.get(element);
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            if (dexfile == null) {
                Log.e("low_duck", "dexfile is null");
                continue;
            }
            if (dexfile != null) {
                dexFilesArray.add(dexfile);
                Object mcookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mCookie");
                if (mcookie == null) {
                    Object mInternalCookie = getClassFieldObject(appClassloader, "dalvik.system.DexFile", dexfile, "mInternalCookie");
                    if(mInternalCookie!=null)
                    {
                        mcookie=mInternalCookie;
                    }else{
                        Log.e("low_duck", "->err get mInternalCookie is null");
                        continue;
                    }

                }
                String[] classnames = null;
                try {
                    classnames = (String[]) getClassNameList_method.invoke(dexfile, mcookie);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                } catch (Error e) {
                    e.printStackTrace();
                    continue;
                }
                if (classnames != null) {
                    Log.e("low_duck", "all classes "+String.join(",",classnames));
                    for (String eachclassname : classnames) {
                        loadClassAndInvoke(appClassloader, eachclassname, dumpMethodCode_method);
                    }
                    if(dumpRepair_method!=null){
                        Log.e("low_duck", "fartWithClassLoader dumpRepair");
                        try {
                            dumpRepair_method.invoke(null);
                        }catch(Exception ex){
                            Log.e("low_duck", "fartWithClassList dumpRepair invoke err:"+ex.getMessage());
                        }
                    }else{
                        Log.e("low_duck", "fartWithClassLoader dumpRepair is null");
                    }
                }

            }
        }
        return;
    }

    public static void fart(){
        Log.e("low_duck", "fart start");
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            Log.e("low_duck", "appClassloader is null");
            return;
        }
        ClassLoader tmpClassloader=appClassloader;
        ClassLoader parentClassloader=appClassloader.getParent();
        if(appClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
        {
            fartWithClassLoader(appClassloader);
        }
        while(parentClassloader!=null){
            if(parentClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
            {
                fartWithClassLoader(parentClassloader);
            }
            tmpClassloader=parentClassloader;
            parentClassloader=parentClassloader.getParent();
        }
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

    public static void fartWithClassList(String classlist){
        Log.e("low_duck", "fartWithClassList");
        ClassLoader appClassloader = getClassloader();
        if (appClassloader == null) {
            Log.e("low_duck", "appClassloader is null");
            return;
        }
        Class DexFileClazz = null;
        try {
            DexFileClazz = appClassloader.loadClass("dalvik.system.DexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
        Method dumpMethodCode_method = null;
        Method dumpRepair_method=null;
        for (Method field : DexFileClazz.getDeclaredMethods()) {
            if (field.getName().equals("fartextMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
            if (field.getName().equals("dumpRepair")) {
                dumpRepair_method = field;
                dumpRepair_method.setAccessible(true);
            }
        }
        String[] classes = classlist.split("\n");
        String tmp= classes[0];
        if (tmp.startsWith("L") && tmp.endsWith(";")) {
            tmp = tmp.substring(1, tmp.length() - 1);
            tmp = tmp.replace("/", ".");
        }
        ClassLoader classLoader=getClassLoaderByClassName(tmp);
        if(classLoader!=null){
            for (String clsname : classes) {
                String line = clsname;
                if (line.startsWith("L") && line.endsWith(";")) {
                    line = line.substring(1, line.length() - 1);
                    line = line.replace("/", ".");
                }
                loadClassAndInvoke(classLoader, line, dumpMethodCode_method);
            }
        }else{
            Log.e("low_duck", "not found classLoader by class:"+tmp);
        }

        if(dumpRepair_method!=null){
            Log.e("low_duck", "fartWithClassList dumpRepair");
            try {
                dumpRepair_method.invoke(null);
            }catch(Exception ex){
                Log.e("low_duck", "fartWithClassList dumpRepair invoke err:"+ex.getMessage());
            }

        }else{
            Log.e("low_duck", "fartWithClassList dumpRepair is null");
        }

    }

    public static void lowDuckThread() {
        //判断
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Log.e("low_duck", "start sleep......");
                    Thread.sleep(1 * 30 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.e("low_duck", "sleep over and start fart");
                fart();
                Log.e("low_duck", "fart run over");
            }
        }).start();
    }

}

