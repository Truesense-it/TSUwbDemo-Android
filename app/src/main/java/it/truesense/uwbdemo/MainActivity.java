package it.truesense.uwbdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingPosition;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;
import androidx.core.uwb.UwbComplexChannel;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.UwbClientSessionScopeRx;
import androidx.core.uwb.rxjava3.UwbManagerRx;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;
import it.truesense.uwbdemo.adapter.DeviceAdapter;
import it.truesense.uwbdemo.adapter.UwbBleDevice;
import it.truesense.uwbdemo.comm.ObserverManager;
import it.truesense.uwbdemo.model.UwbDevCfg;
import it.truesense.uwbdemo.model.UwbPhoneCfg;


enum appStatus {
    IDLE,
    CONFIGURING,
    RANGING
}

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    public static final int PREFERRED_UWB_PROFILE_ID = RangingParameters.CONFIG_UNICAST_DS_TWR;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;
    private final int mUwbChannel = 9;
    private final int mUwbPreambleIndex = 10;
    ArrayList<UwbDevice> peerDevices;
    private Button btn_scan;
    private ImageView img_loading;
    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;
    private appStatus appState;
    private UwbManager mUwbManager = null;
    private Single<UwbControleeSessionScope> controleeSessionScopeSingle = null;
    private UwbControleeSessionScope controleeSessionScope = null;
    private Observable<RangingResult> rangingResultObservable = null;
    private Disposable disposable = null;
    private int mPreferredUwbPhoneRole = 1;
    private int mPreferredUwbProfileId = PREFERRED_UWB_PROFILE_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create the Uwb Manager if supported by this device

        initView();
        peerDevices = new ArrayList<UwbDevice>();
        appState = appStatus.IDLE;

        PackageManager packageManager = getApplicationContext().getPackageManager();
        if (packageManager.hasSystemFeature("android.hardware.uwb")) {
            Log.d(TAG, "UWB hardware IS available!");
            createUwbManagerLocalAdapter();


        } else {
            Toast.makeText(getApplicationContext(), "UWB hardware is not available", Toast.LENGTH_LONG).show();
        }
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setSplitWriteNum(50)
                .setOperateTimeout(5000);
        if (btn_scan.getText().equals(getString(R.string.start_scan))) {
            Log.d(TAG, "ScanStart");
            checkPermissions();
        } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
            Log.d(TAG, "onClick: stop");
            BleManager.getInstance().cancelScan();

        }
    }

    private byte selectUwbDeviceRangingRole(int supportedRoles) {
        int preferedRole = this.mPreferredUwbPhoneRole;
        if (preferedRole != 0 || ((supportedRoles >> 1) & 1) == 0) {
            return ((preferedRole != 1 || ((supportedRoles >> 0) & 1) == 0) && ((supportedRoles >> 0) & 1) != 0) ? (byte) 1 : 0;
        }
        return 1;
    }

    private byte selectUwbProfileId(int supportedProfileIds) {
        int retValue;
        long j = (long) supportedProfileIds;
        if (BigInteger.valueOf(j).testBit(this.mPreferredUwbProfileId)) {
            retValue = this.mPreferredUwbProfileId;
        } else if (BigInteger.valueOf(j).testBit(RangingParameters.CONFIG_UNICAST_DS_TWR)) {
            retValue = RangingParameters.CONFIG_UNICAST_DS_TWR;
        } else if (BigInteger.valueOf(j).testBit(RangingParameters.CONFIG_MULTICAST_DS_TWR)) {
            retValue = RangingParameters.CONFIG_MULTICAST_DS_TWR;
        } else if (!BigInteger.valueOf(j).testBit(RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR)) {
            return 0;
        } else {
            retValue = RangingParameters.CONFIG_UNICAST_DS_TWR;
        }
        return (byte) retValue;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    Log.d(TAG, "ScanStart");
                    checkPermissions();
                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    Log.d(TAG, "onClick: stop");
                    BleManager.getInstance().cancelScan();

                }
                break;

        }
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setText(getString(R.string.start_scan));
        btn_scan.setOnClickListener(this);
        img_loading = (ImageView) findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);
        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(UwbBleDevice bleDevice) {
                if (!BleManager.getInstance().isConnected(bleDevice.bleDev)) {
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
                btn_scan.setVisibility(View.GONE);
            }

            @Override
            public void onDisConnect(final UwbBleDevice bleDevice) {
                Log.d(TAG, "Disconnecting UWB1");
                Log.d(TAG, "onDisConnected called");
                UwbBleDevice uwbdev = mDeviceAdapter.findByBleDevice(bleDevice.bleDev);
                if (uwbdev != null) {
                    if (uwbdev.disposable != null) {
                        Log.d(TAG, "Removing Disposable");
                        (uwbdev.disposable).dispose();
                    }
                    Log.d(TAG, "Disconnecting UWB");

                    for (int j = 0; j < peerDevices.size(); j++) {
                        UwbDevice dev = peerDevices.get(j);
                        Log.d(TAG, "alldev : " + dev.getAddress() + " local: " + uwbdev.uwbDevAddr);
                        if (dev.getAddress().equals(uwbdev.uwbDevAddr)) {
                            Log.d(TAG, "Removing UWB");
                            peerDevices.remove(dev);
                            return;
                        }

                    }
                    ;

                }
                if (BleManager.getInstance().isConnected(bleDevice.bleDev)) {
                    BleManager.getInstance().disconnect(bleDevice.bleDev);

                    stopRanging();
                }
                btn_scan.setVisibility(View.VISIBLE);
                if (btn_scan.getText().equals(getString(R.string.start_scan))) {
                    Log.d(TAG, "ScanStart");
                    checkPermissions();
                } else if (btn_scan.getText().equals(getString(R.string.stop_scan))) {
                    Log.d(TAG, "onClick: stop");
                    BleManager.getInstance().cancelScan();
                }
            }
        });
        ListView listView_device = (ListView) findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
    }

    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            UwbBleDevice dev = new UwbBleDevice();
            dev.bleDev = bleDevice;
            mDeviceAdapter.addDevice(dev);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    private void setScanRule() {

        UUID[] serviceUuids = null;

        String[] names = new String[]{"TS_DCU150", "TS_DCU040", "NXP_SR040", "NXP_SR150", "NXP_SR160"};
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)
                .setDeviceName(true, names)
                .setDeviceMac("")
                .setAutoConnect(false)
                .setScanTimeOut(10000)
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.stop_scan));
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                UwbBleDevice dev = new UwbBleDevice();
                dev.bleDev = bleDevice;
                mDeviceAdapter.addDevice(dev);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                Log.d(TAG, "onScanFinished: ");
            }
        });
    }

    private void connect(final UwbBleDevice uwbBleDevice) {
        BleDevice bleDevice = uwbBleDevice.bleDev;
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                //connection successfull, stop the progressDialog
                progressDialog.dismiss();

                //create the object to put in our list of connected devices
                UwbBleDevice dev = new UwbBleDevice();
                dev.bleDev = bleDevice;
                mDeviceAdapter.addDevice(dev);
                mDeviceAdapter.notifyDataSetChanged();


                BleManager.getInstance().notify(bleDevice,
                        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
                        "6E400003-B5A3-F393-E0A9-E50E24DCCA9E",
                        new BleNotifyCallback() {
                            @Override
                            public void onNotifySuccess() {
                                Log.d(TAG, "onNotifySuccess: ");
                                appState = appStatus.CONFIGURING;
                                BleManager.getInstance().write(
                                        bleDevice,
                                        "6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
                                        "6E400002-B5A3-F393-E0A9-E50E24DCCA9E",
                                        HexUtil.hexStringToBytes("A5"),
                                        new BleWriteCallback() {
                                            @Override
                                            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                                Log.d(TAG, "onWriteSuccess: ");
                                            }

                                            @Override
                                            public void onWriteFailure(BleException exception) {
                                                Log.d(TAG, "onWriteFailure: ");
                                            }
                                        });

                            }

                            @Override
                            public void onNotifyFailure(BleException exception) {

                            }

                            @Override
                            public void onCharacteristicChanged(byte[] data) {
                                switch (appState) {
                                    case CONFIGURING:
                                        //parse the data sent from device
                                        byte[] data2 = new byte[data.length - 1];
                                        System.arraycopy(data, 1, data2, 0, data.length - 1);
                                        UwbDevCfg devConfig = UwbDevCfg.fromByteArray(data2);
                                        Log.d(TAG, "VMAJ: " + devConfig.specVerMajor);
                                        Log.d(TAG, "VMIN: " + devConfig.specVerMinor);
                                        Log.d(TAG, "CHIPD: " + devConfig.chipId);
                                        Log.d(TAG, "FWVER: " + devConfig.chipFwVersion);
                                        Log.d(TAG, "MWVER: " + devConfig.mwVersion);
                                        Log.d(TAG, "UWBPROFS: " + devConfig.supportedUwbProfileIds);
                                        Log.d(TAG, "RNGROLES: " + devConfig.supportedDeviceRangingRoles);
                                        Log.d(TAG, "MAC: " + HexUtil.formatHexString(devConfig.deviceMacAddress));

                                        //define the channel and preamble
                                        UwbComplexChannel uwbComplexChannel = new UwbComplexChannel(mUwbChannel, mUwbPreambleIndex);
                                        ;
                                        UwbControllerSessionScope uwbControllerSessionScope;
                                        UwbControleeSessionScope uwbControleeSessionScope;
                                        UwbAddress uwbAddress;
                                        Flowable<RangingResult> flowable;

                                        byte selectUwbDeviceRangingRole = 0;//Device is controller
                                        Log.d(TAG, "Uwb device supported ranging roles: " + devConfig.getSupportedDeviceRangingRoles() + ", selected role for UWB device: " + selectUwbDeviceRangingRole);

                                        byte selectUwbProfileId = PREFERRED_UWB_PROFILE_ID; //UNICAST DS TWR
                                        Log.d(TAG, "Uwb device supported UWB profile IDs: " + devConfig.getSupportedUwbProfileIds() + ", selected UWB profile ID: " + selectUwbProfileId);

                                        if (selectUwbDeviceRangingRole == 0) {
                                            try {
                                                Log.d(TAG, "Android device will act as Controlee!");
                                                UwbControleeSessionScope blockingGet = UwbManagerRx.controleeSessionScopeSingle(mUwbManager).blockingGet();
                                                uwbAddress = blockingGet.getLocalAddress();
                                                uwbControllerSessionScope = null;
                                                uwbControleeSessionScope = blockingGet;

                                            } catch (Exception e) {
                                                Log.e(TAG, "UWB Ranging configuration exception: " + e.getMessage());

                                                return;
                                            }
                                        } else {
                                            Log.d(TAG, "Android device will act as Controller!");
                                            UwbControllerSessionScope blockingGet2 = UwbManagerRx.controllerSessionScopeSingle(mUwbManager).blockingGet();
                                            uwbAddress = blockingGet2.getLocalAddress();
                                            uwbControllerSessionScope = blockingGet2;
                                            Log.d(TAG, blockingGet2.getUwbComplexChannel().toString());
                                            uwbComplexChannel = blockingGet2.getUwbComplexChannel();
                                            uwbControleeSessionScope = null;
                                        }

                                        //generate a random sessionID
                                        int sessionId = new Random().nextInt();

                                        //add the device to the list of peer devices
                                        UwbDevice uwbDevice = new UwbDevice(new UwbAddress((devConfig.getDeviceMacAddress())));
                                        peerDevices.add(uwbDevice);

                                        //update our internal list of connected devices
                                        dev.uwbDevAddr = uwbDevice.getAddress();

                                        Log.d(TAG, "UWB SessionId: " + String.format("0x%04X", sessionId));
                                        Log.d(TAG, "UWB Local Address: " + uwbAddress);
                                        Log.d(TAG, "UWB Remote Address: " + uwbDevice.getAddress().toString());
                                        Log.d(TAG, "UWB Channel: " + uwbComplexChannel.getChannel());
                                        Log.d(TAG, "UWB Preamble Index: " + uwbComplexChannel.getPreambleIndex());
                                        Log.d(TAG, "Configure ranging parameters for Profile ID: " + selectUwbProfileId);

                                        //setup the ranging parameters
                                        byte[] sessionKey = {0x8, 0x7, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6}; //that's what the firmware wants for VendorID and STS_IV
                                        byte[] subSessionKey = {};
                                        //RangingParameters rangingParameters = new RangingParameters(selectUwbProfileId, sessionId,0,sessionKey, subSessionKey,uwbComplexChannel, peerDevices, RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC);
                                        RangingParameters rangingParameters = new RangingParameters(selectUwbProfileId, sessionId, 0, sessionKey, null, uwbComplexChannel, peerDevices, RangingParameters.RANGING_UPDATE_RATE_FREQUENT);

                                        //determine the flowable to use
                                        if (selectUwbDeviceRangingRole == 0) {
                                            Log.d(TAG, "Configure controlee flowable");
                                            flowable = UwbClientSessionScopeRx.rangingResultsFlowable(uwbControleeSessionScope, rangingParameters);
                                        } else {
                                            Log.d(TAG, "Configure controller flowable");
                                            flowable = UwbClientSessionScopeRx.rangingResultsFlowable(uwbControllerSessionScope, rangingParameters);
                                        }

                                        //generate the config to send to firmware
                                        UwbPhoneCfg uwbPhoneCfg = new UwbPhoneCfg();
                                        uwbPhoneCfg.setSpecVerMajor((short) 256); //this is 1 sent in Little Endian
                                        uwbPhoneCfg.setSpecVerMinor((short) 0);
                                        uwbPhoneCfg.setSessionId(sessionId);
                                        uwbPhoneCfg.setPreambleIndex((byte) uwbComplexChannel.getPreambleIndex());
                                        uwbPhoneCfg.setChannel((byte) uwbComplexChannel.getChannel());
                                        uwbPhoneCfg.setProfileId(selectUwbProfileId);
                                        uwbPhoneCfg.setDeviceRangingRole((byte) (1 << selectUwbDeviceRangingRole));
                                        uwbPhoneCfg.setPhoneMacAddress((uwbAddress.getAddress()));
                                        byte[] cmd = new byte[uwbPhoneCfg.toByteArray().length + 1];
                                        cmd[0] = 0x0B;
                                        System.arraycopy(uwbPhoneCfg.toByteArray(), 0, cmd, 1, uwbPhoneCfg.toByteArray().length);
                                        Log.d(TAG, "command: " + HexUtil.formatHexString(cmd));

                                        //application will go into ranging mode
                                        appState = appStatus.RANGING;

                                        //send the config data to device
                                        BleManager.getInstance().write(
                                                bleDevice,
                                                "6E400001-B5A3-F393-E0A9-E50E24DCCA9E",
                                                "6E400002-B5A3-F393-E0A9-E50E24DCCA9E",
                                                cmd,

                                                //HexUtil.hexStringToBytes("0B"). 010001001945559ED400000B090600100EE6010349646D67B36BDEE4C800"),
                                                new BleWriteCallback() {
                                                    @Override
                                                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                                                        Log.d(TAG, "onWriteSuccess: Start");
                                                        appState = appStatus.RANGING;
                                                        startRanging(mDeviceAdapter, dev, uwbPhoneCfg, rangingParameters, flowable);
                                                    }

                                                    @Override
                                                    public void onWriteFailure(BleException exception) {
                                                        Log.d(TAG, "onWriteFailure: ");
                                                    }
                                                });
                                        break;
                                    case RANGING:
                                        Log.d(TAG, "onCharacteristicChanged in Ranging: " + HexUtil.formatHexString(data, true));
                                        break;
                                }
                            }
                        }
                );
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

                progressDialog.dismiss();
                Log.d(TAG, "onDisConnected called");
                /*UwbBleDevice uwbdev = mDeviceAdapter.findByBleDevice(bleDevice);
                if (uwbdev != null) {
                    Log.d(TAG, "Disconnecting UWB");
                    peerDevices.forEach(dev -> {
                        Log.d(TAG, "alldev : " + dev.getAddress() + " local: " + uwbdev.uwbDevAddr.getAddress());
                        if (dev.getAddress().equals(uwbdev.uwbDevAddr.getAddress())) {
                            Log.d(TAG, "Removing UWB");
                            peerDevices.remove(dev);
                            return;
                        }
                    });
                    if (uwbdev.disposable != null) {
                        Log.d(TAG, "Removing Disposable");
                        ((Disposable) uwbdev.disposable).dispose();
                    }
                }*/
                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                stopRanging();

                if (isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }

            }
        });
    }


    public void startRanging(DeviceAdapter DA, UwbBleDevice dev, UwbPhoneCfg phoneConfig, RangingParameters rangingParameters, Flowable<RangingResult> rangingResultFlowable) {
        Thread t = new Thread(() -> {

            Disposable disposable = (Disposable) rangingResultFlowable
                    .delay(100, TimeUnit.MILLISECONDS)
                    .subscribeWith(new DisposableSubscriber<RangingResult>() {
                        @Override
                        public void onStart() {
                            Log.d(TAG, "UWB Disposable started");
                            request(1);
                        }

                        @Override
                        public void onNext(RangingResult rangingResult) {
                            Log.d(TAG, "UWB Ranging notification received " + rangingResult.getDevice().getAddress().toString());

                            if (rangingResult instanceof RangingResult.RangingResultPosition) {

                                RangingResult.RangingResultPosition rangingResultPosition = (RangingResult.RangingResultPosition) rangingResult;
                                RangingPosition pos = rangingResultPosition.getPosition();
                                if (pos != null) {//rangingResultPosition.getPosition().getDistance() != null) {
                                    float distance, azimuth;
                                    distance = 0;
                                    azimuth = 0;
                                    if (pos.getDistance() != null)
                                        distance = pos.getDistance().getValue();
                                    if (pos.getAzimuth() != null)
                                        azimuth = pos.getAzimuth().getValue();
                                    UwbBleDevice d = mDeviceAdapter.findByUwbAddr(rangingResult.getDevice().getAddress());
                                    if (d != null) {
                                        d.uwbPosition = pos;//rangingResultPosition;
                                        mDeviceAdapter.addDevice(d);
                                        runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                mDeviceAdapter.notifyDataSetChanged();
                                            }
                                        });

                                    }
                                    Log.d(TAG, "Position distance: " + distance);
                                    Log.d(TAG, "Position azimuth: " + azimuth);
                                } else {
                                    Log.e(TAG, "Unexpected rangingResult value, distance is null!");
                                }
                            } else {
                                Log.e(TAG, "not a rangingResult " + rangingResult.getClass().getName());
                                if (rangingResult instanceof RangingResult.RangingResultPeerDisconnected) {
                                    RangingResult.RangingResultPeerDisconnected r = (RangingResult.RangingResultPeerDisconnected) rangingResult;
                                    Log.e(TAG, "device: " + r.getDevice().getAddress().toString());
                                    stopRanging();
                                    dispose();
                                }
                            }
                            request(1);
                        }


                        @Override
                        public void onError(Throwable t) {
                            Log.d(TAG, "UWB Ranging error received");
                            //doSomethingWithError(t);
                            t.printStackTrace();
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "UWB Ranging session completed");
                            //doSomethingOnEventsCompleted();
                        }
                    });
        });
        dev.disposable = disposable;
        DA.addDevice(dev);
        t.start();
    }

    public void stopRanging() {


        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void createUwbManagerLocalAdapter() {
        Thread t = new Thread(() -> {
            mUwbManager = UwbManager.createInstance(getApplicationContext());
            controleeSessionScopeSingle = UwbManagerRx.controleeSessionScopeSingle(mUwbManager);
            controleeSessionScope = controleeSessionScopeSingle.blockingGet();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Log.d(TAG, "run: " + localAddress.toString());
                }
            });
        });
        t.start();
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.UWB_RANGING,/* Manifest.permission.NEARBY_WIFI_DEVICES,*/ Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
//            case Manifest.permission.BLUETOOTH_SCAN:
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    String[] perms={ Manifest.permission.BLUETOOTH_SCAN,
//                            Manifest.permission.BLUETOOTH_CONNECT};
//                    ActivityCompat.requestPermissions(this,perms,REQUEST_CODE_PERMISSION_LOCATION);
//
//                }
//                else{
//                    //Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    //startActivityForResult(intent, BluetoothAdapter.);
//                    //val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                    //requestBluetooth.launch(enableBtIntent)
//                }
//                break;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }

}
