package geq.kaboom.app.kaboot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import geq.kaboom.app.kaboot.terminal.TerminalActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONObject;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> data;
    private Context context;

    public ListAdapter(ArrayList<HashMap<String, Object>> _arr) {
        this.data = _arr;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pkg, parent, false);
        context = parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int POS) {
        String packagePath = data.get(POS).get("path").toString();
        holder.name.setText(Uri.parse(packagePath).getLastPathSegment());

        Intent intent = new Intent();
        holder.size.setText(data.get(POS).get("size").toString());
        intent.putExtra("pkgPath", packagePath);
        intent.putExtra("name", holder.name.getText().toString());
        try {
            String configcnt = KabUtil.readFile(packagePath + "/config.json");
            intent.putExtra("config", configcnt);
        } catch (Exception e) {
            holder.icon.setVisibility(View.GONE);
            holder.name.setText("Invalid Package!");
        }
        intent.setClass(context, TerminalActivity.class);
        holder.icon.setOnClickListener(v -> context.startActivity(intent));
        holder.base.setOnLongClickListener(
                v -> {
                    showDeleteDialog(packagePath, POS);
                    return true;
                });
    }

    private void showDeleteDialog(String packagePath, int pos) {
        new MaterialAlertDialogBuilder(context)
                .setTitle("Delete " + new File(packagePath).getName() + " package?")
                .setPositiveButton("Delete", (dialog, which) -> deletePackage(packagePath, pos))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePackage(String packagePath, int pos) {
        try {
            KabUtil.deleteFile(packagePath);
            data.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, data.size());
            KabUtil.toast(context, "Package deleted!");
        } catch (Exception e) {
            KabUtil.toast(context, "Couldn't delete this package!");
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView size;
        ImageView icon;
        LinearLayout base;

        public ViewHolder(View v) {
            super(v);
            base = v.findViewById(R.id.base);
            name = v.findViewById(R.id.name);
            icon = v.findViewById(R.id.icon);
            size = v.findViewById(R.id.size);
        }
    }
}
