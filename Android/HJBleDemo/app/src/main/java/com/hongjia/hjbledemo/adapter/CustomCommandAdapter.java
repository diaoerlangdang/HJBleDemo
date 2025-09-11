package com.hongjia.hjbledemo.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hongjia.hjbledemo.R;
import com.hongjia.hjbledemo.bean.CustomCommand;

import java.util.List;

/**
 * 自定义指令列表适配器
 */
public class CustomCommandAdapter extends RecyclerView.Adapter<CustomCommandAdapter.ViewHolder> {
    
    private Context context;
    private List<CustomCommand> commandList;
    private OnCommandClickListener clickListener;
    private OnItemClickListener itemClickListener;
    private OnEditClickListener editClickListener;
    private OnDeleteClickListener deleteClickListener;
    
    public interface OnCommandClickListener {
        void onCommandClick(CustomCommand command);
        void onCommandEdit(CustomCommand command);
        void onCommandDelete(CustomCommand command);
    }
    
    public interface OnItemClickListener {
        void onItemClick(CustomCommand command);
    }
    
    public interface OnEditClickListener {
        void onEditClick(CustomCommand command);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(CustomCommand command);
    }
    
    public CustomCommandAdapter(Context context, List<CustomCommand> commandList) {
        this.context = context;
        this.commandList = commandList;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }
    
    public void setOnCommandClickListener(OnCommandClickListener listener) {
        this.clickListener = listener;
    }
    
    public void updateCommands(List<CustomCommand> commandList) {
        this.commandList = commandList;
        notifyDataSetChanged();
    }
    
    public void updateData(List<CustomCommand> commandList) {
        this.commandList = commandList;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_custom_command, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomCommand command = commandList.get(position);
        
        holder.tvCommandName.setText(command.getName());
        holder.tvCommandContent.setText(command.getCommand());
        holder.tvCommandType.setText(command.getTypeString());
        
        // 根据指令类型设置不同颜色
        if (command.getType() == CustomCommand.TYPE_HEX) {
            holder.tvCommandType.setTextColor(Color.BLUE);
        } else {
            holder.tvCommandType.setTextColor(Color.parseColor("#4CAF50"));
        }
        
        // 点击整个item执行指令
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCommandClick(command);
            }
            if (itemClickListener != null) {
                itemClickListener.onItemClick(command);
            }
        });
        
        // 长按编辑
        holder.itemView.setOnLongClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCommandEdit(command);
            }
            if (editClickListener != null) {
                editClickListener.onEditClick(command);
            }
            return true;
        });
        
        // 编辑按钮
        holder.tvEdit.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCommandEdit(command);
            }
            if (editClickListener != null) {
                editClickListener.onEditClick(command);
            }
        });
        
        // 删除按钮
        holder.tvDelete.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCommandDelete(command);
            }
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(command);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return commandList != null ? commandList.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommandName;
        TextView tvCommandContent;
        TextView tvCommandType;
        TextView tvEdit;
        TextView tvDelete;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommandName = itemView.findViewById(R.id.tv_command_name);
            tvCommandContent = itemView.findViewById(R.id.tv_command_content);
            tvCommandType = itemView.findViewById(R.id.tv_command_type);
            tvEdit = itemView.findViewById(R.id.tv_edit);
            tvDelete = itemView.findViewById(R.id.tv_delete);
        }
    }
}