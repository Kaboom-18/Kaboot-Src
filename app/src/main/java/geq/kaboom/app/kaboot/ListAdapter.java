package geq.kaboom.app.kaboot;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
    private RecyclerView view;
    private TextView warn;

    public ListAdapter(ArrayList<HashMap<String, Object>> initialData, RecyclerView view, TextView warn) {
        this.data = new ArrayList<>(initialData);
        this.view = view;
        this.warn = warn;
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
        String packageName = Config.getPkgName(context, packagePath);
        
        holder.name.setText(packageName);
        if(item.containsKey("size")){
         holder.size.setVisibility(View.VISIBLE);
         holder.size.setText(item.get("size").toString());
        }else holder.size.setVisibility(View.GONE);

        Intent intent = new Intent(context, TerminalActivity.class);
        intent.putExtra("pkgPath", packagePath);

        String configContent = util.readFile(packagePath + "/config.json");
        if(configContent != null){
            intent.putExtra("config", configContent);
            holder.icon.setVisibility(View.VISIBLE);
        } else{
            holder.icon.setVisibility(View.GONE);
            holder.name.setText("Invalid Package!");
        }
        holder.base.setOnClickListener((v) -> context.startActivity(intent));
        holder.base.setOnLongClickListener((v) -> {
            if(configContent == null) return true;
            showConfigDialog(packagePath, packageName, position);
            return true;
        });
    }

    private void showConfigDialog(String packagePath, String packageName, int pos) {
        final EditText input = new EditText(context);
        input.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        input.setText(packageName);
        input.setHint("Enter package name...");
        LinearLayout container = new LinearLayout(context);
        container.setPadding(36,8,36,8);
        container.addView(input);
        new MaterialAlertDialogBuilder(context)
                .setView(container)
                .setTitle("Configure "+packageName)
                .setOnDismissListener((dialog) -> {
                    String inp = input.getText().toString().trim();
                    if(inp.equals(packageName)) return;
                    if(!inp.matches("[a-zA-Z0-9]+")){
                        util.toast("Invalid format!");
                        return;
                    }
                    boolean tmp = true;
                    if(util.isExistFile(Config.getPkgTmpDir(context, packageName))){
                    tmp = util.renameFile(Config.getPkgTmpDir(context, packageName), inp);
                    }
                    if(tmp && util.renameFile(packagePath, inp)){
                        data.get(pos).put("path", Config.getPkgDir(context, inp));
                        notifyItemChanged(pos);
                        util.toast("Package renamed!");
                    }else{
                        util.toast("Package rename failed!");
                    }
                })
                .setPositiveButton("Delete", (dialog, which) -> {
                    new MaterialAlertDialogBuilder(context)
                    .setTitle("Are you sure?")
                    .setMessage("Delete "+packageName)
                    .setPositiveButton("delete", (d, w)-> deletePackage(packagePath, packageName, pos))
                    .setNegativeButton("Cancel", null)
                    .show();
                })
                .show();
    }

    private void deletePackage(String packagePath, String packageName, int pos) {
            if(util.deleteFile(packagePath) && util.deleteFile(Config.getPkgTmpDir(context, packageName))){
            data.remove(pos);
           if(data.isEmpty()){
                    view.setVisibility(View.GONE);
                    warn.setVisibility(View.VISIBLE);
                }else{
                    view.setVisibility(View.VISIBLE);
                    warn.setVisibility(View.GONE);
                }
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, data.size());
            util.toast("Package deleted!");
           }else{
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