package com.hongjia.hjbledemo.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;
import com.hongjia.hjbledemo.R;
import com.hongjia.hjbledemo.bean.HJBleScanDevice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder> {

    public interface OnDeviceListItemClickListener {
        void onConnectClick(int position);
        void onDetailClick(int position);
    }

    private List<HJBleScanDevice> mScanDevices;
    private LayoutInflater mInflator;
    OnDeviceListItemClickListener listener;

    private Context mContext;

    public LeDeviceListAdapter(Context context)
    {
        super();
        mContext = context;
        mScanDevices = new ArrayList<>();
        mInflator = LayoutInflater.from(context);
    }

    public void setListener(OnDeviceListItemClickListener listener) {
        this.listener = listener;
    }

    public void addDevice(HJBleScanDevice scanDevice)
    {
        int pos = -1;
        for (int i=0; i<mScanDevices.size(); i++) {
            if (mScanDevices.get(i).device.getMac().equals(scanDevice.device.getMac())) {
                scanDevice.timeStamp = new Date().getTime();
                mScanDevices.set(i, scanDevice);
                pos = i;
                break;
            }
        }

        // 新增
        if (pos < 0) {
            scanDevice.timeStamp = new Date().getTime();
            mScanDevices.add(scanDevice);
            notifyDataSetChanged();
        } else {
//            notifyItemChanged(pos);
        }

    }

    public void sort() {
        Collections.sort(mScanDevices);
    }

    public BleDevice getDevice(int position)
    {
        return mScanDevices.get(position).device;
    }

    public HJBleScanDevice getScanDeviceInfo(int position)
    {
        return mScanDevices.get(position);
    }


    public void clear() {

        List<HJBleScanDevice> connectedDevices = new ArrayList<>();
        BleManager bleManager = BleManager.getInstance();
        for (HJBleScanDevice item : mScanDevices) {
            if (bleManager.isConnected(item.device.getMac())) {
                connectedDevices.add(item);
            }
        }

        mScanDevices = connectedDevices;

        // 保留以连接的
//            mScanDevices = mScanDevices.stream().filter(item -> BleManager.getInstance().isConnected(item.device.getMac())).collect(Collectors.toList());
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.scan_list_item, parent, false);
        return new ViewHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        HJBleScanDevice scanDevice = mScanDevices.get(position);
        final String deviceName = scanDevice.device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(mContext.getResources().getString(R.string.unknow_device));

        if (BleManager.getInstance().isConnected(scanDevice.device.getMac())) {
            viewHolder.connectBtn.setText(mContext.getResources().getString(R.string.device_disconnect));
            viewHolder.detailBtn.setVisibility(View.VISIBLE);
        } else {
            viewHolder.connectBtn.setText(mContext.getResources().getString(R.string.device_connect));
            viewHolder.detailBtn.setVisibility(View.GONE);
        }

        viewHolder.deviceAddress.setText("address:"+scanDevice.device.getMac() + "     RSSI:"+scanDevice.rssi+"dB");
        viewHolder.deviceRecord.setText("broadcast:"+scanDevice.record);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        viewHolder.deviceTime.setText(sdf.format(scanDevice.timeStamp));

        if (!scanDevice.isEasy) {
//                viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.itemBgLayout.setBackgroundColor(Color.parseColor("#ffffff"));
            viewHolder.deviceName.setTextColor(Color.parseColor("#333333"));
            viewHolder.deviceAddress.setTextColor(Color.parseColor("#333333"));
            viewHolder.deviceRecord.setTextColor(Color.parseColor("#333333"));
            viewHolder.deviceTime.setTextColor(Color.parseColor("#333333"));
        }
        else {
//                viewHolder.imageView.setVisibility(View.VISIBLE);
            viewHolder.itemBgLayout.setBackgroundColor(Color.parseColor("#1FA7D3"));
            viewHolder.deviceName.setTextColor(Color.WHITE);
            viewHolder.deviceAddress.setTextColor(Color.WHITE);
            viewHolder.deviceRecord.setTextColor(Color.WHITE);
            viewHolder.deviceTime.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return mScanDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        OnDeviceListItemClickListener listener;

        LinearLayout itemBgLayout;
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRecord;
        TextView deviceTime;
        ImageView imageView;

        Button connectBtn;
        Button detailBtn;

        public ViewHolder(View itemView, OnDeviceListItemClickListener listener) {
            super(itemView);
            this.listener = listener;

            itemBgLayout = itemView.findViewById(R.id.item_bg);
            deviceAddress = (TextView) itemView.findViewById(R.id.device_address);
            deviceName = (TextView) itemView.findViewById(R.id.device_name);
            deviceRecord = (TextView)itemView.findViewById(R.id.device_record);
            deviceTime = (TextView)itemView.findViewById(R.id.device_time);
            imageView = itemView.findViewById(R.id.icon);
            connectBtn = itemView.findViewById(R.id.connect_btn);
            detailBtn = itemView.findViewById(R.id.detail_btn);

            connectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onConnectClick(position);
                        }
                    }
                }
            });

            detailBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onDetailClick(position);
                        }
                    }
                }
            });
        }
    }
}
