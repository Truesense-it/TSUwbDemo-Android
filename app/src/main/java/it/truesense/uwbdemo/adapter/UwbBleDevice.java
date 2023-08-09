package it.truesense.uwbdemo.adapter;

import androidx.core.uwb.*;//RangingResult;
import com.clj.fastble.data.BleDevice;
import io.reactivex.rxjava3.disposables.Disposable;
import it.truesense.uwbdemo.model.UwbDevCfg;

public class UwbBleDevice {
    public BleDevice bleDev;
    public UwbAddress uwbDevAddr;
    public RangingPosition uwbPosition;
    public Disposable disposable;
}
