package geq.kaboom.app.kaboot;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class InstallAdapter extends RecyclerView.Adapter<InstallAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> data;
    private final AlertDialog dialog;
    private Context context;
    private boolean installing = false;
    private KabUtil util;

    public InstallAdapter(AlertDialog dialog, ArrayList<HashMap<String, Object>> dataList) {
        this.dialog = dialog;
        this.data = dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        util = new KabUtil(context);
        View view = LayoutInflater.from(context).inflate(R.layout.install_pkg, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        HashMap<String, Object> item = data.get(pos);
        String name = item.get("name").toString();
        holder.name.setText(name);

        holder.name.setOnClickListener(v -> {
            if (!installing){
            String url = item.get("url") != null ? item.get("url").toString() : null;
            if (url != null) {
                installing = true;
                installer(holder, url, Config.getTmpDir(context), Config.getFilesDir(context) + "/Packages/" + name);
            } else {
                util.toast("Unsupported arch!");
            }
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, log;
        LinearLayout download;
        LinearProgressIndicator prog;

        public ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.name);
            download = v.findViewById(R.id.download);
            prog = v.findViewById(R.id.prog);
            log = v.findViewById(R.id.log);
        }
    }

    private void installer(ViewHolder holder, String url, String downloadPath, String pkgPath) {
        holder.download.setVisibility(View.VISIBLE);
        dialog.setCancelable(false);

        new Thread(() -> {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;

            try {
                URL link = new URL(url);
                connection = (HttpURLConnection) link.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new RuntimeException("Download failed!");
                }

                int fileLength = connection.getContentLength();
                File archiveFile = new File(downloadPath, Uri.parse(url).getLastPathSegment());
                input = new BufferedInputStream(connection.getInputStream());
                output = new FileOutputStream(archiveFile);

                byte[] buffer = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    int progress = (int) (total * 100 / fileLength);
                    Config.UI.post(() -> holder.prog.setProgress(progress));
                    output.write(buffer, 0, count);
                }

                input.close();
                output.close();

                Config.UI.post(() -> {
                    util.toast("Download completed!");
                    Config.installed = true;
                    holder.prog.setVisibility(View.GONE);
                    holder.log.setVisibility(View.VISIBLE);
                });

                util.deleteFile(pkgPath);
                util.makeDir(pkgPath);

                ArrayList<String> command = new ArrayList<>();
                command.add(Config.getKaboot(context));
                command.add("-l");
                command.add("tar");
                command.add("-xvzf");
                command.add(archiveFile.getAbsolutePath());
                command.add("-C");
                command.add(pkgPath);

                ProcessBuilder pb = new ProcessBuilder(command);
                
                for(String va : Config.getKabootVars(context)){
                    String[] ar = va.split("=", 2);
                    pb.environment().put(ar[0], ar[1]);
                }

                Process proc = pb.start();
                proc.getErrorStream().close();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Config.UI.post(() -> holder.log.setText(finalLine));
                }
                proc.waitFor();
                reader.close();

                util.deleteFile(archiveFile.getAbsolutePath());

                Config.UI.post(() -> {
                    installing = false;
                    util.toast("Installation completed!");
                    dialog.dismiss();
                });

            } catch (Exception e) {
                Config.UI.post(() -> {
                    installing = false;
                    util.toast("Installation failed!");
                    dialog.dismiss();
                });
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
                if (connection != null) connection.disconnect();
            }
        }).start();
    }
}