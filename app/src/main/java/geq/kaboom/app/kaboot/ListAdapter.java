package geq.kaboom.app.kaboot;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import geq.kaboom.app.kaboot.terminal.TerminalActivity;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private final ArrayList<HashMap<String, Object>> data;
    private Context context;
    private KabUtil util;

    public ListAdapter(ArrayList<HashMap<String, Object>> initialData) {
        this.data = new ArrayList<>(initialData);
    }

    public void updateData(ArrayList<HashMap<String, Object>> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        util = new KabUtil(context);
        View view = LayoutInflater.from(context).inflate(R.layout.list_pkg, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        HashMap<String, Object> item = data.get(position);
        String packagePath = item.get("path").toString();
        String packageName = Uri.parse(packagePath).getLastPathSegment();

        holder.name.setText(packageName);
        if(item.containsKey("size")){
         holder.size.setVisibility(View.VISIBLE);
         holder.size.setText(item.get("size").toString());
        }else holder.size.setVisibility(View.GONE);

        Intent intent = new Intent(context, TerminalActivity.class);
        intent.putExtra("pkgPath", packagePath);
        intent.putExtra("name", packageName);

        try {
            String configContent = util.readFile(packagePath + "/config.json");
            intent.putExtra("config", configContent);
            holder.icon.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            holder.icon.setVisibility(View.GONE);
            holder.name.setText("Invalid Package!");
        }
        holder.icon.setOnClickListener(v -> context.startActivity(intent));
        holder.base.setOnLongClickListener(v -> {
            showDeleteDialog(packagePath, position);
            return true;
        });
    }

    private void showDeleteDialog(String packagePath, int pos) {
        String packageName = Uri.parse(packagePath).getLastPathSegment();

        new MaterialAlertDialogBuilder(context)
                .setTitle("Delete " + packageName + " package?")
                .setPositiveButton("Delete", (dialog, which) -> deletePackage(packagePath, pos))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePackage(String packagePath, int pos) {
        try {
            util.deleteFile(packagePath);
            data.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, data.size());
            util.toast("Package deleted!");
        } catch (Exception e) {
            util.toast("Couldn't delete this package!");
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, size;
        ImageView icon;
        LinearLayout base;

        public ViewHolder(View itemView) {
            super(itemView);
            base = itemView.findViewById(R.id.base);
            name = itemView.findViewById(R.id.name);
            icon = itemView.findViewById(R.id.icon);
            size = itemView.findViewById(R.id.size);
        }
    }
}