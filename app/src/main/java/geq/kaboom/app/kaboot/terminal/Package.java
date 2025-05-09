package geq.kaboom.app.kaboot.terminal;

import android.content.Context;
import geq.kaboom.app.kaboot.KabUtil;
import geq.kaboom.app.kaboot.Config;
import geq.kaboom.app.kaboot.terminal.termlib.TerminalSession;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class Package {
    private String pkgPath;
    private TerminalSession.SessionChangedCallback callback;
    private TerminalSession session;
    private KabUtil util;
    private String config;
    private String name;
    private Context context;
    
   public Package(Context context, String name, String pkgPath, String config, TerminalSession.SessionChangedCallback callback){
       this.pkgPath = pkgPath;
       this.callback = callback;
       this.context = context;
       this.config = config;
       this.name = name;
       util = new KabUtil(context);
   }

   public TerminalSession getTerminalSession() throws Exception{
            return new TerminalSession(getCmd(context), Config.getKabootVars(context), Config.getTmpDir(context), callback);
   }

  private String[] getCmd(Context context) throws Exception{
      final ArrayList<String> cmd = new ArrayList<>();
      final JSONObject obj = new JSONObject(config);
      final String tmpDir = Config.getTmpDir(context);
        
      cmd.add(Config.getKaboot(context));
      cmd.add("--kill-on-exit");
      cmd.add("-w");
      cmd.add("/");
      cmd.add("-b");
      cmd.add("/dev");
      cmd.add("-b");
      cmd.add("/proc");
      cmd.add("-b");
      cmd.add("/sys");
      cmd.add("-b");
      util.makeDir(tmpDir+"/shm");
      cmd.add(tmpDir+"/shm:/dev/shm");
      cmd.add("-b");
      util.makeDir(tmpDir+"/"+name+"/tmp");
      cmd.add(tmpDir+"/"+name+"/tmp:/tmp");
      cmd.add("-r");
      cmd.add(pkgPath+"/rootfs");
      
                final JSONArray args = obj.getJSONArray("args");
                for (int i = 0; i < args.length(); i++) {
                    cmd.add(args.getString(i));
                }
            
                final JSONArray variables = obj.getJSONArray("envVars");
                for (int i = 0; i < variables.length(); i++) {
                    if (variables.getString(i).startsWith("HOME")) {
                        cmd.add("-w");
                        cmd.add(variables.getString(i).split("=")[1]);
                    }
                }
                cmd.add(obj.getString("env"));
                cmd.add("-i");
                for (int i = 0; i < variables.length(); i++) {
                    cmd.add(variables.getString(i));
                }
            
                final JSONArray commands = obj.getJSONArray("cmd");
                for (int i = 0; i < commands.length(); i++) {
                    cmd.add(commands.getString(i));
                }
            
    return cmd.toArray(new String[0]);
  }
}
