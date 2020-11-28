package com.gxma.disinfection;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.physicaloid.lib.Physicaloid;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;


public class FirstFragment extends Fragment {
    private Physicaloid mPhysicaloid;

    static int marche=0;
    static int arrete=1;
    private int position = 0;
    boolean waiting = false ;
    private CountDownTimer timer;
//    private byte[] buf ;
    private static final String[] positions = new String[]{
            "POSITION 1 FORM 1",
            "POSITION 1 FORM 2",
            "POSITION 2 FORM 1" ,
            "POSITION 2 FORM 2",
            "POSITION 3 FORM 1",
            "POSITION 3 FORM 2",
            "POSITION 4 FORM 2",
    };
    private static final int[] image_positions = new int[] {
            R.drawable.pos1,
            R.drawable.pos2,
            R.drawable.pos3,
            R.drawable.pos4,
            R.drawable.pos5,
            R.drawable.pos6,
            R.drawable.pos7
    };
    private static final int[] image_widgets = new int[] {
            R.id.imageView1,
            R.id.imageView2,
            R.id.imageView3,
            R.id.imageView4,
            R.id.imageView5,
            R.id.imageView6,
            R.id.imageView7,
    };
    Context context ;
    View view;
    String data = "";

    MenuItem diskMenuItem;
    ConstraintLayout layout ;
    ConstraintSet set;
    ImageView imageView, imageView2, imageView3;
    ImageView focusedImage;
    private TextView textCounter, progressText;
    Animation fadeIn = new AlphaAnimation(1, 0);
    Animation fadeOut = new AlphaAnimation(1, 0);
    Animation rotation;

    MediaPlayer mediaPlayer, mediaPlayer2, mediaPlayer3;
    ProgressBar progressBar;

    // Serial
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    public final String ACTION_USB_PERMISSION = "confoosball.lmu.mff.confoosball.USB_PERMISSION";

    // new
    private UsbService usbService;
    private MyBluetoothService bluetoothService;
    private MyHandler mHandler;

    BluetoothAdapter btAdapter;
    BluetoothSocket btSocket = null;

    private static final int REQUEST_ENABLE_BT = 448;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        set = new ConstraintSet();
        context = this.getContext();
        mHandler = new MyHandler((MainActivity) getActivity());
//        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

    }

    @Override
    public void onStop() {
//        getActivity().unregisterReceiver(broadcastReceiver);
        getActivity().unbindService(usbConnection);
        super.onStop();
    }

    @Override
    public void onStart() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(500);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(500);
        fadeOut.setDuration(500);
        super.onStart();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        diskMenuItem = menu.findItem(R.id.disk);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        final Activity activity = this.getActivity();
        if (id == R.id.start) {
//            openConnection();
            marche=1; arrete=0;
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            System.out.println(btAdapter.getBondedDevices());
//            startJob();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        layout = view.findViewById(R.id.layout);
        textCounter = view.findViewById(R.id.textView);
        progressText = view.findViewById(R.id.textView2);
        imageView = view.findViewById(R.id.imageView);
        imageView2 = view.findViewById(R.id.vent);
        imageView3 = view.findViewById(R.id.vent2);
        rotation = AnimationUtils.loadAnimation(context, R.anim.rotate);
        rotation.setFillAfter(true);
        progressBar = view.findViewById(R.id.progressBar);
        mediaPlayer = MediaPlayer.create(context, R.raw.tick);
        mediaPlayer2 = MediaPlayer.create(context, R.raw.stop1);
        mediaPlayer3 = MediaPlayer.create(context, R.raw.stop2);

        timer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long l) {
                long sec = TimeUnit.MILLISECONDS.toSeconds(l);
                if (sec>=1) mediaPlayer.start();
                textCounter.setText(String.format("%02d", sec));
            }

            @Override
            public void onFinish() {
                Toast.makeText(context, "Fin du position", Toast.LENGTH_LONG).show();
                mediaPlayer2.start();
                textCounter.setText("0");
                runTimer();
            }
        };
    }

    public void startJob() {
        if (marche==1 && arrete==0 && waiting==false) {
            diskMenuItem.setIcon(R.drawable.ic_red_24dp);
            waiting = true;
            textCounter.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            for (int id:image_widgets)
                view.findViewById(id).setVisibility(View.VISIBLE);
            runTimer();
        } else if (marche==0 && arrete==1) {
            stopJob();
        }
    }

    public void stopJob() {
        waiting = false;
        mediaPlayer3.start();
        position = 0;
        Toast.makeText(context, "Fin", Toast.LENGTH_LONG).show();
        diskMenuItem.setIcon(R.drawable.ic_green_24dp);
        hideEverything();
    }

    private void hideEverything() {
        timer.cancel();
        textCounter.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        progressText.setVisibility(View.INVISIBLE);
        textCounter.setText("15");
        imageView.setAnimation(fadeIn);
        imageView3.clearAnimation();
        imageView2.clearAnimation();
        imageView2.setVisibility(View.INVISIBLE);
        imageView3.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
        imageView.setFocusableInTouchMode(true);
        imageView.requestFocus();
        ImageView img;
        for (int id:image_widgets){
            img = view.findViewById(id);
            img.setVisibility(View.GONE);
            img.clearColorFilter();
        }
    }

    private void runTimer() {
        if (position<positions.length) {
            set.clone(layout);
            if (position != 0) {
                ColorMatrix matrix = new ColorMatrix();
                matrix.setSaturation(0);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                ((ImageView)view.findViewById(image_widgets[position-1]))
                        .setColorFilter(filter);
            }
            set.connect(textCounter.getId(),
                    ConstraintSet.TOP,
                    image_widgets[position],
                    ConstraintSet.TOP, 0);
            set.connect(progressText.getId(),
                    ConstraintSet.TOP,
                    textCounter.getId(),
                    ConstraintSet.BOTTOM, 5);
            set.connect(progressBar.getId(),
                    ConstraintSet.BOTTOM,
                    image_widgets[position],
                    ConstraintSet.BOTTOM, 2);
            if (position+1<positions.length) {
                focusedImage = view.findViewById(image_widgets[position+1]);
                focusedImage.setFocusableInTouchMode(true);
                focusedImage.requestFocus();
            }
            if (position==6) {
                imageView2.setVisibility(View.VISIBLE);
                imageView3.setVisibility(View.VISIBLE);
                set.connect(imageView2.getId(),
                        ConstraintSet.TOP,
                        image_widgets[position],
                        ConstraintSet.TOP, 3);
                set.connect(imageView3.getId(),
                        ConstraintSet.TOP,
                        image_widgets[position],
                        ConstraintSet.TOP, 3);
                imageView2.startAnimation(rotation);
                imageView3.startAnimation(rotation);
            }
            timer.start();
//            progressText.setText(String.format("Position %d/%d", position+1, positions.length));
            progressText.setText(positions[position]);

            set.applyTo(layout);
//            imageView.startAnimation(fadeOut);
//            imageView.setVisibility(View.INVISIBLE);
//            imageView.setVisibility(View.VISIBLE);
//            imageView.startAnimation(fadeIn);
//            imageView.setImageResource(image_positions[position]);
            progressBar.setProgress(position);
            position++;
        } else
            stopJob();

    }

    public void openConnection() {
        HashMap usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Object entry : usbDevices.entrySet()) {
                device = (UsbDevice) ((Entry)entry).getValue();
                int deviceVID = device.getVendorId();
                Toast.makeText(context, "deviceId: "+String.valueOf(deviceVID),Toast.LENGTH_SHORT).show();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    Toast.makeText(context, "Arduino Found",Toast.LENGTH_SHORT).show();
                    PendingIntent pi = PendingIntent.getBroadcast(context, 0,
                            new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            switch (intent.getAction()) {
//                case MyBluetoothService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
//                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
//                    break;
//                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
//                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
//                    break;
//                case MyBluetoothService.MessageConstants.MESSAGE_TOAST: // NO USB CONNECTED
//                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
//                    break;
//                case MyBluetoothService.MessageConstants.ERROR: // USB DISCONNECTED
//                    Toast.makeText(context, "Bluetooth disconnected", Toast.LENGTH_SHORT).show();
//                    marche = 0; arrete = 1;
//                    stopJob();
//                    break;
//            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
//        setFilters();  // Start listening notifications from UsbService
        startService(MyBluetoothService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
//        getActivity().unregisterReceiver(broadcastReceiver);
        getActivity().unbindService(usbConnection);
    }

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            bluetoothService = ((MyBluetoothService.BBinder) arg1).getService();
            bluetoothService.setHandler(mHandler);
            BluetoothDevice hc05 = btAdapter.getRemoteDevice("98:D3:32:20:D5:B3");
            System.out.println("hc--05");
            System.out.println(hc05.getName());
            bluetoothService.connect(hc05);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bluetoothService = null;
        }
    };

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!MyBluetoothService.SERVICE_CONNECTED) {
            Intent startService = new Intent(getContext(), service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            getActivity().startService(startService);
            Toast.makeText(getContext(), "service connected", Toast.LENGTH_SHORT).show();
        }
        Intent bindingIntent = new Intent(getActivity(), service);
        getActivity().bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    startService(MyBluetoothService.class, usbConnection, null);
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }


    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
//        getActivity().registerReceiver(broadcastReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        String dataStock = "";
        String data_split[] ;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyBluetoothService.MessageConstants.MESSAGE_READ:
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            data += (String) msg.obj;
                            byte[] readBuf = (byte[]) msg.obj;
                            // construct a string from the valid bytes in the buffer
                            data += new String(readBuf, 0, msg.arg1);

                            System.out.println(data);
                            int index = data.indexOf('S');
                            if (index>=0 && data.indexOf('F')>index) {
                                dataStock = data.substring(index);
                                data_split = dataStock.split(";");
                                marche = Integer.parseInt(data_split[1]);
                                arrete = Integer.parseInt(data_split[2]);
                                bluetoothService.write((""+marche).getBytes());
                                startJob();
                                data = "";
                            }
                        }
                    });
//                    mActivity.get().display.append(data);
                    break;
                case MyBluetoothService.MessageConstants.MESSAGE_TOAST:
                    Toast.makeText(mActivity.get(), msg.getData().getString("toast"),Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}
