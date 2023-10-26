package it.truesense.uwbdemo.adapter;


import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;

import it.truesense.uwbdemo.R;
import it.truesense.uwbdemo.model.UwbDevCfg;

import com.clj.fastble.BleManager;
import com.clj.fastble.data.BleDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import it.truesense.uwbdemo.adapter.UwbBleDevice;

public class DeviceAdapter extends BaseAdapter {

    private final Context context;
    private final List<UwbBleDevice> bleDeviceList = new ArrayList<>();
    private final java.util.Map<UwbDevCfg, BleDevice> uwbDeviceList = new HashMap<UwbDevCfg,BleDevice>();
    public DeviceAdapter(Context context) {
        this.context = context;
    }

    public void addDevice(UwbBleDevice bleDevice) {
        removeDevice(bleDevice);
        bleDeviceList.add(bleDevice);
    }

    public void removeDevice(UwbBleDevice bleDevice) {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i).bleDev;
            if (bleDevice.bleDev.getKey().equals(device.getKey())) {
                bleDeviceList.remove(i);
            }
        }
    }
    public void removeDevice(BleDevice bleDevice) {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i).bleDev;
            if (bleDevice.getKey().equals(device.getKey())) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clearConnectedDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i).bleDev;
            if (BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    public void clearScanDevice() {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice device = bleDeviceList.get(i).bleDev;
            if (!BleManager.getInstance().isConnected(device)) {
                bleDeviceList.remove(i);
            }
        }
    }

    public UwbBleDevice findByUwbAddr(UwbAddress addr)
    {
        for (int i = 0; i < bleDeviceList.size(); i++) {
            UwbAddress myaddr=bleDeviceList.get(i).uwbDevAddr;
            //BleDevice device = bleDeviceList.get(i).bleDev;
            if (myaddr!=null && myaddr.equals(addr)) {
                return bleDeviceList.get(i);
            }
        }
        return null;
    }

    public UwbBleDevice findByBleDevice(BleDevice dev)
    {
        Log.d("Adapter","devicelist size: "+ bleDeviceList.size());
        for (int i = 0; i < bleDeviceList.size(); i++) {
            BleDevice mydev=bleDeviceList.get(i).bleDev;
            //BleDevice device = bleDeviceList.get(i).bleDev;
            Log.d("DeviceAdapter","dev mac: " + dev.getMac() + " list mac: " + mydev.getMac());
            if (mydev.getMac().equals(dev.getMac())) {
                return bleDeviceList.get(i);
            }
        }
        return null;
    }

    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
    }


    @Override
    public int getCount() {
        return bleDeviceList.size();
    }

    @Override
    public UwbBleDevice getItem(int position) {
        if (position > bleDeviceList.size())
            return null;
        return bleDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = View.inflate(context, R.layout.adapter_device, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.img_blue = (ImageView) convertView.findViewById(R.id.img_blue);
            holder.txt_name = (TextView) convertView.findViewById(R.id.txt_name);
            holder.txt_mac = (TextView) convertView.findViewById(R.id.txt_mac);
            holder.txt_rssi = (TextView) convertView.findViewById(R.id.txt_rssi);
            holder.layout_idle = (LinearLayout) convertView.findViewById(R.id.layout_idle);
            holder.layout_connected = (LinearLayout) convertView.findViewById(R.id.layout_connected);
            holder.btn_disconnect = (Button) convertView.findViewById(R.id.btn_disconnect);
            holder.btn_connect = (Button) convertView.findViewById(R.id.btn_connect);
            holder.layout_measure=(LinearLayout) convertView.findViewById(R.id.layout_measure);
            holder.txt_distance = (TextView) convertView.findViewById(R.id.distance_measure);
            holder.txt_angle = (TextView) convertView.findViewById(R.id.angle_measure);
            holder.txt_elevation = (TextView) convertView.findViewById(R.id.elvation_measure);
        }
        final UwbBleDevice uwbBleDevice = getItem(position);
        final BleDevice bleDevice = getItem(position).bleDev;
        if (bleDevice != null) {
            boolean isConnected = BleManager.getInstance().isConnected(bleDevice);
            String name = bleDevice.getName();
            String mac = bleDevice.getMac();
            int rssi = bleDevice.getRssi();
            holder.txt_name.setText(name);
            holder.txt_mac.setText(mac);
            holder.txt_rssi.setText(String.valueOf(rssi));
            if (isConnected) {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
                holder.txt_name.setTextColor(0xFF1DE9B6);
                holder.txt_mac.setTextColor(0xFF1DE9B6);
                holder.layout_idle.setVisibility(View.GONE);
                holder.layout_connected.setVisibility(View.VISIBLE);
                holder.layout_measure.setVisibility(View.VISIBLE);

                if(uwbBleDevice.uwbPosition!=null) {
                    if(uwbBleDevice.uwbPosition.getDistance()!=null)
                        holder.txt_distance.setText(Float.toString(uwbBleDevice.uwbPosition.getDistance().getValue()));
                    if(uwbBleDevice.uwbPosition.getAzimuth()!=null)
                        holder.txt_angle.setText(Float.toString(uwbBleDevice.uwbPosition.getAzimuth().getValue()));
                    if(uwbBleDevice.uwbPosition.getElevation()!=null)
                        holder.txt_elevation.setText(Float.toString(uwbBleDevice.uwbPosition.getElevation().getValue()));
                }

            } else {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
                holder.txt_name.setTextColor(0xFF000000);
                holder.txt_mac.setTextColor(0xFF000000);
                holder.layout_idle.setVisibility(View.VISIBLE);
                holder.layout_connected.setVisibility(View.GONE);
                holder.layout_measure.setVisibility(View.GONE);
            }
        }

        holder.btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onConnect(uwbBleDevice);
                }
            }
        });

        holder.btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDisConnect(uwbBleDevice);
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        TextView txt_rssi;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        LinearLayout layout_measure;
        Button btn_disconnect;
        Button btn_connect;

        TextView txt_distance;
        TextView txt_angle;

        TextView txt_elevation;
    }

    public interface OnDeviceClickListener {
        void onConnect(UwbBleDevice bleDevice);

        void onDisConnect(UwbBleDevice bleDevice);

        //void onDetail(UwbBleDevice bleDevice);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}
