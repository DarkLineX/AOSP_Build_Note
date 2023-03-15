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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static net.llllc.LowDuckCore.getClassField;
import static net.llllc.LowDuckCore.getClassFieldObject;
import static net.llllc.LowDuckCore.getClassloader;
import static net.llllc.LowDuckCore.getFieldObject;


public class LowDuck {

    private static String LowDuckClassName = "net.llllc.";

    //根据classLoader->pathList->dexElements拿到dexFile
    //然后拿到mCookie后，使用getClassNameList获取到所有类名。
    //loadClassAndInvoke处理所有类名导出所有函数
    //dumpMethodCode这个函数是自己加在DexFile中的
    public static void lowDuckWithClassLoader(ClassLoader appClassloader) {
        Log.e("low_duck", "lowDuckWithClassLoader "+appClassloader.toString());
        List<Object> dexFilesArray = new ArrayList<Object>();
        //反射 ClassLoader获取
        Field pathList_Field = (Field) getClassField(appClassloader, "dalvik.system.BaseDexClassLoader", "pathList");
        // 获取 pathList 加载的类
        Object pathList_object = getFieldObject("dalvik.system.BaseDexClassLoader", appClassloader, "pathList");
        Object[] ElementsArray = (Object[]) getFieldObject("dalvik.system.DexPathList", pathList_object, "dexElements");
        // 获取class dalvik.system.DexPathList$Element 的 dexFile变量
        Field dexFile_fileField = null;
        try {
            dexFile_fileField = (Field) getClassField(appClassloader, "dalvik.system.DexPathList$Element", "dexFile");
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }

        // 对应 class DexFle.class 的载入
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
        //Method dumpDexFile_method = null;
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
            if (field.getName().equals("lowDuckMethodCode")) {
                dumpMethodCode_method = field;
                dumpMethodCode_method.setAccessible(true);
            }
            if (field.getName().equals("dumpRepair")) {
                dumpRepair_method = field;
                dumpRepair_method.setAccessible(true);
            }
        }


        if(dumpMethodCode_method==null){
            Log.e("low_duck", "->dumpMethodCode_method is null");
        }

        if(dumpRepair_method==null){
            Log.e("low_duck", "->dumpRepair_method is null");
        }


        // 获取Dex Cookie
        Field mCookieField = getClassField(appClassloader, "dalvik.system.DexFile", "mCookie");

        Log.e("low_duck", "->methods dalvik.system.DexPathList.ElementsArray.length:" + ElementsArray.length);

        // ElementsArray 是 dex文件 list
        // 提取dex文件
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
                // 获取 dex文件里面的所有类数组名称 classnames
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
                    // 进入主动调用阶段
                    for (String eachclassname : classnames) {
                        loadClassAndInvoke(appClassloader, eachclassname, dumpMethodCode_method);
                    }

                    // 最终调用修复dex文件方法
                    if(dumpRepair_method!=null){
                        Log.e("low_duck", "lowDuckWithClassLoader dumpRepair");
                        try {
                            dumpRepair_method.invoke(null);
                        }catch(Exception ex){
                            Log.e("low_duck", "lowDuckWithClassLoader dumpRepair invoke err:"+ex.getMessage());
                        }
                    }else{
                        Log.e("low_duck", "lowDuckWithClassLoader dumpRepair is null");
                    }
                }

            }
        }
        return;
    }

    public static void lowDuck(){
        Log.e("low_duck", "lowDuck start");

        // 整体逻辑和ClassLoader的双亲委托有关

        // 提取ClassLoader
        ClassLoader appClassloader = getClassloader();
        if(appClassloader==null){
            //为空直接返回
            Log.e("low_duck", "appClassloader is null");
            return;
        }

        ClassLoader tmpClassloader=appClassloader;

        //取出父类ClassLoader
        ClassLoader parentClassloader=appClassloader.getParent();

        // 不包含 java.lang.BootClassLoader 调用 lowDuckWithClassLoader
        if(appClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
        {
            lowDuckWithClassLoader(appClassloader);
        }

        while(parentClassloader!=null){
            /// 不包含 java.lang.BootClassLoader 调用 lowDuckWithClassLoader
            if(parentClassloader.toString().indexOf("java.lang.BootClassLoader")==-1)
            {
                lowDuckWithClassLoader(parentClassloader);
            }
            tmpClassloader=parentClassloader;
            parentClassloader=parentClassloader.getParent();
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
                    Thread.sleep(1 * 20 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Log.e("low_duck", "sleep over and start lowDuck");
                lowDuck();
                Log.e("low_duck", "lowDuck run over");
            }
        }).start();
    }

    //取指定类的所有构造函数，和所有函数，使用dumpMethodCode函数来把这些函数给保存出来
    public static int loadClassAndInvoke(ClassLoader appClassloader, String eachclassname, Method dumpMethodCode_method) {
        Class resultclass = null;
        Log.e("low_duck", "go into loadClassAndInvoke->" + "classname:" + eachclassname);

        // 加载class
        try {
            resultclass = appClassloader.loadClass(eachclassname);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("low_duck", "load class err1:"+e.getMessage());
            return -2;
        } catch (Error e) {
            Log.e("low_duck", "load class err2:"+e.getMessage());
            e.printStackTrace();
            return -2;
        }

        // 主动调用 构造方法
        if (resultclass != null) {
            try {
                Constructor<?> cons[] = resultclass.getDeclaredConstructors();
                for (Constructor<?> constructor : cons) {
                    if (dumpMethodCode_method != null) {
                        try
                        {
                            // 跳过自己写的类名
                            if(constructor.getName().contains(LowDuckClassName)){
                                continue;
                            }
                            Log.e("low_duck", "classname:" + eachclassname+ " constructor->invoke "+constructor.getName());
                            dumpMethodCode_method.invoke(null, constructor);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        } catch (Error e) {
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        Log.e("low_duck", "dumpMethodCode_method is null ");
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("low_duck", "Constructor invoke err1:"+e.getMessage());
                return -3;
            } catch (Error e) {
                e.printStackTrace();
                Log.e("low_duck", "Constructor invoke err2:"+e.getMessage());
                return -3;
            }

            // 主动调用其他方法
            try {
                Method[] methods = resultclass.getDeclaredMethods();
                if (methods != null) {
                    Log.e("low_duck", "classname:" + eachclassname+ " start invoke");
                    for (Method m : methods) {
                        if (dumpMethodCode_method != null) {
                            try {
                                if(m.getName().contains(LowDuckClassName)){
                                    continue;
                                }
                                Log.e("low_duck", "classname:" + eachclassname+ " method->invoke:" + m.getName());
                                dumpMethodCode_method.invoke(null, m);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("low_duck", "Method invoke err1:"+e.getMessage());
                                continue;
                            } catch (Error e) {
                                e.printStackTrace();
                                Log.e("low_duck", "Method invoke err2:"+e.getMessage());
                                continue;
                            }
                        } else {
                            Log.e("low_duck", "dumpMethodCode_method is null ");
                        }
                    }
                    Log.e("low_duck", "go into loadClassAndInvoke->"   + "classname:" + eachclassname+ " end invoke");
                    return 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("low_duck", "Method invoke err3:"+e.getMessage());
                return -4;
            } catch (Error e) {
                e.printStackTrace();
                Log.e("low_duck", "Method invoke err4:"+e.getMessage());
                return -4;
            }
        }
        return 0;
    }

}

