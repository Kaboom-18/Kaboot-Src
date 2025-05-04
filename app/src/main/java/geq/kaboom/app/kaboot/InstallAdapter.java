package geq.kaboom.app.kaboot;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    private ArrayList<HashMap<String, Object>> data;
    private Dialog dialog;
    private Context context;
    private boolean installing=false;

    public InstallAdapter(Dialog dial, ArrayList<HashMap<String, Object>> _arr) {
        dialog = dial;
        data = _arr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View _v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.install_pkg, parent, false);
        context = parent.getContext();
        return new ViewHolder(_v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int POS) {
        holder.name.setText(data.get(POS).get("name").toString());
        holder.name.setOnClickListener(
                v -> {
                    if(!installing){
                        installing = true;
                    if (data.get(POS).get("url") != null) {
                        installer(
                                holder,
                                data.get(POS).get("url").toString(),
                                context.getCacheDir().getAbsolutePath(),
                                context.getFilesDir().getAbsolutePath()
                                        + "/Packages/"
                                        + data.get(POS).get("name").toString());
                    } else {
                        installing = false;
                        KabUtil.toast(context, "Unsupported arch!");
                    }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private LinearLayout download;
        private LinearProgressIndicator prog;
        private TextView log;

        public ViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.name);
            download = v.findViewById(R.id.download);
            prog = v.findViewById(R.id.prog);
            log = v.findViewById(R.id.log);
        }
    }

    public void installer(
            final ViewHolder holder,
            final String _link,
            final String _downloadpath,
            final String _pkgpath) {

        holder.download.setVisibility(View.VISIBLE);
        dialog.setCancelable(false);
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(
                        () -> {
                            InputStream input = null;
                            FileOutputStream output = null;
                            HttpURLConnection connection = null;

                            try {
                                URL url = new URL(_link);
                                connection = (HttpURLConnection) url.openConnection();
                                connection.connect();

                                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                                    throw new RuntimeException("Download failed!");
                                }

                                int fileLength = connection.getContentLength();
                                input = new BufferedInputStream(connection.getInputStream());
                                File archivefile =
                                        new File(
                                                _downloadpath
                                                        + File.separator
                                                        + Uri.parse(_link).getLastPathSegment());
                                output = new FileOutputStream(archivefile);

                                byte[] data = new byte[4096];
                                long total = 0;
                                int count;

                                while ((count = input.read(data)) != -1) {
                                    total += count;
                                    if (fileLength > 0) {
                                        int pro = (int) (total * 100 / fileLength);
                                        handler.post(() -> holder.prog.setProgress(pro));
                                    }
                                    output.write(data, 0, count);
                                }

                                handler.post(
                                        () -> {
                                            KabUtil.toast(context, "Download completed!");
                                            holder.prog.setVisibility(View.GONE);
                                            holder.log.setVisibility(View.VISIBLE);
                                        });

                                input.close();
                                output.close();

                                KabUtil.deleteFile(_pkgpath);
                                KabUtil.makeDir(_pkgpath);

                                ArrayList<String> command = new ArrayList<>();
                                command.add(
                                        context.getApplicationInfo().nativeLibraryDir
                                                + "/libkaboot.so");
                                command.add("-l");
                                command.add("tar");
                                command.add("-xvJf");
                                command.add(archivefile.getAbsolutePath());
                                command.add("-C");
                                command.add(_pkgpath);

                                ProcessBuilder pb = new ProcessBuilder(command);
                                pb.environment()
                                        .put(
                                                "LD_LIBRARY_PATH",
                                                context.getApplicationInfo().nativeLibraryDir);
                                pb.environment()
                                        .put(
                                                "PROOT_LOADER",
                                                context.getApplicationInfo().nativeLibraryDir
                                                        + "/libkabooter.so");
                                pb.environment()
                                        .put(
                                                "PROOT_LOADER_32",
                                                context.getApplicationInfo().nativeLibraryDir
                                                        + "/libkabooter32.so");

                                java.lang.Process proc = pb.start();
                                proc.getErrorStream().close();

                                BufferedReader reader =
                                        new BufferedReader(
                                                new InputStreamReader(proc.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    final String fline = line;
                                    handler.post(() -> holder.log.setText(fline));
                                }

                                proc.waitFor();
                                reader.close();

                                KabUtil.deleteFile(archivefile.getAbsolutePath());

                                handler.post(
                                        () -> {
                                            installing = false;
                                            KabUtil.toast(context, "Extraction completed!");
                                            dialog.dismiss();
                                        });

                            } catch (final Exception e) {
                                handler.post(
                                        () -> {
                                            installing = false;
                                            dialog.dismiss();
                                            KabUtil.toast(context, "Installation failed!");
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
                        })
                .start();
    }
}
