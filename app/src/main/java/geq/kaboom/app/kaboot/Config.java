package geq.kaboom.app.kaboot;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.File;

public class Config {

  public static boolean refreshList = false;
            
  public final static int MAX_FONTSIZE = 256;
  
  public final static float FSCALE = 12f;
    
  public final static String TERM_BG = "#000000";
  
  public final static Handler UI = new Handler(Looper.getMainLooper());
  
  public final static int REFRESH_CODE = 0;
  
  public final static int PKG_REFRESH_CODE = 1;
  
  public final static String REPOURL = "https://raw.githubusercontent.com/Kaboom-18/Kaboot/refs/heads/main/Packages.json";
  
  public final static String ABOUTUS = "Kaboot is a robust Linux emulation application developed by Kaboom, enabling a full Linux environment on Android devices—no root access required. It supports the latest Android versions and is specifically engineered to bypass W^X (Write XOR Execute) restrictions, ensuring maximum compatibility and stability. Users can also use their custom repositories to run their own Linux or shell-based applications.\n\n•Facebook\n-KA Boom \n\n•Email\n-Kaboomofficial18@gmail.com";
  
  public final static String EMAIL= "kaboomofficial18@gmail.com";
  
  public static String getTmpDir(Context context){
      return (context.getCacheDir().getAbsolutePath());
  }
    
  public static String getPkgTmpDir(Context context, String pkgName){
      return (getTmpDir(context)+File.separator+pkgName);
  }
    
  public static String getPkgDir(Context context, String pkgName){
      return (getFilesDir(context)+File.separator+"Packages"+File.separator+pkgName);
  }
    
  public static String getPkgName(Context context, String pkgPath){
      return (Uri.parse(pkgPath).getLastPathSegment());
  }
  
  public static String getFilesDir(Context context){
      return (context.getFilesDir().getAbsolutePath());
  }
  
  public static String getLibsPath(Context context){
      return (context.getApplicationInfo().nativeLibraryDir);
  }
  
  public static String getKaboot(Context context){
      return (getLibsPath(context)+"/libkaboot.so");
  }
  
  public static String[] getKabootVars(Context context){
      final String libdir = getLibsPath(context);
      return (new String[]{"LD_LIBRARY_PATH=" + libdir,"PROOT_LOADER=" + libdir + "/libkabooter.so","PROOT_LOADER_32=" + libdir + "/libkabooter32.so","PROOT_TMP_DIR=" + getTmpDir(context)});
  }

}