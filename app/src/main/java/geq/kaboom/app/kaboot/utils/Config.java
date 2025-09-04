package geq.kaboom.app.kaboot.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.File;

public class Config {

  public static boolean refreshList = false;
  
  public final static float FSCALE = 15f;
    
  public final static int MAX_FONT_SIZE = 160;
    
  public final static int MIN_FONT_SIZE = 8;
  
  public final static String TERM_BG = "#000000";
  
  public final static Handler UI = new Handler(Looper.getMainLooper());
  
  public final static int REFRESH_CODE = 0;
  
  public final static int PKG_REFRESH_CODE = 1;
    
  public final static String MAINURL = "https://raw.githubusercontent.com/Kaboom-18/Kaboot/refs/heads/main";
  
  public final static String REPOURL = MAINURL+"/Packages.json";
    
  public final static String VERSIONURL = MAINURL+"/LatestAppVersion";
    
  public final static String WEBSITEURL = MAINURL+"/WebsiteUrl";
    
  public final static String DOWNLOADURL = MAINURL+"/DownloadLink";  
    
  public final static String PACKAGE_NAME = "geq.kaboom.app.kaboot";
  
  public final static String EMAIL = "kaboomofficial18@gmail.com";
  
  public static String getTmpDir(Context context){
      return (context.getCacheDir().getAbsolutePath());
  }
    
  public static String getPkgTmpDir(Context context, String pkgName){
      return (getTmpDir(context)+File.separator+pkgName);
  }
    
  public static String getPkgDir(Context context, String pkgName){
      return (getFilesDir(context)+File.separator+"Packages"+File.separator+pkgName);
  }
    
  public static String getPkgName(String pkgPath){
      return (Uri.parse(pkgPath).getLastPathSegment());
  }
  
  public static String getFilesDir(Context context){
      return (context.getFilesDir().getAbsolutePath());
  }
  
  public static String getLibsPath(Context context){
      return (context.getApplicationInfo().nativeLibraryDir);
  }
    
  public static String getKabmem(Context context){
      return (getLibsPath(context)+"/libkabmem.so");
    }  
  
  public static String getKaboot(Context context){
      return (getLibsPath(context)+"/libkaboot.so");
  }
  
  public static String[] getKabootVars(Context context){
      final String libdir = getLibsPath(context);
      return (new String[]{"LD_LIBRARY_PATH=" + libdir,"PROOT_LOADER=" + libdir + "/libkabooter.so","PROOT_LOADER_32=" + libdir + "/libkabooter32.so","PROOT_TMP_DIR=" + getTmpDir(context)});
  }
}