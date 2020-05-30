package com.gxma.disinfection;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {
    private Physicaloid mPhysicaloid;

    int marche=0, arrete=1;
    private int position = 0;
    private String dataStock="";
    boolean waiting = false ;
    private CountDownTimer timer;
    private byte[] buf ;
    private static final String[] positions = new String[]{
            "POSITION 1 FORM 1",
            "POSITION 1 FORM 2",
            "POSITION 2 FORM 1" ,
//            "POSITION 2 FORM 2",
//            "POSITION 3 FORM 1",
//            "POSITION 3 FORM 2",
//            "POSITION 4 FORM 2",
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
    Context context ;

    ImageView imageView, imageView2, imageView3;
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
    UsbSerialInterface.UsbReadCallback mCallback;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        context = this.getContext();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mCallback = new UsbSerialInterface.UsbReadCallback() {
            //Defining a Callback which triggers whenever data is read.
            @Override
            public void onReceivedData(byte[] arg0) {
                String data = null;
                try {
                    data = new String(arg0, "UTF-8");
                    {
                        final String data_split[] = data.split(";");
                        if(data_split[0].indexOf("S")>1){
                            marche = Integer.parseInt(data_split[1]);
                            arrete = Integer.parseInt(data_split[2]);
                            startJob();
                        }
                        dataStock="";
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            ;
        };
    }

    @Override
    public void onStop() {
//        mPhysicaloid.clearReadListener();
//        mPhysicaloid.close();
//        mPhysicaloid=null;
        super.onStop();
    }

    @Override
    public void onStart() {
//        mPhysicaloid = new Physicaloid(context);
//        mPhysicaloid.setBaudrate(9600);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        final Activity activity = this.getActivity();
        if (id == R.id.start) {

//            class SerialTask extends AsyncTask<Void, Void, Void> {
//                @Override
//                protected Void doInBackground(Void... voids) {
//                    while (mPhysicaloid!=null) {
//                        Log.w("Thread", "enter the loop");
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (!mPhysicaloid.isOpened() && mPhysicaloid.open()) {
//                                    mPhysicaloid.open();
//                                    startSerialReading();
//                                }
//                                else if (!mPhysicaloid.isOpened()) {
//                                    Toast.makeText(context, "Probleme de connexion", Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    return null;
//                }
//            }
//            new SerialTask().execute();
            openConnection();
//            marche=1; arrete=0;
//            startJob();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
                mediaPlayer2.start();
                textCounter.setText("0");
                runTimer();
            }
        };
    }

    public void startSerialReading() {
        final Activity activity = this.getActivity();
        mPhysicaloid.addReadListener(new ReadLisener() {
            @Override
            public void onRead(final int size) {
                buf = new byte[size];
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPhysicaloid.read(buf, size);
                        for(int i=0;i<size;i++)
                        {
                            char a=(char)buf[i];
                            dataStock += a;
                            if(a=='F' )
                            {
                                final String data[] = dataStock.split(";");
                                if(data[0].indexOf("S")>1){
                                    marche = Integer.parseInt(data[1]);
                                    arrete = Integer.parseInt(data[2]);
                                    startJob();
                                }
                                dataStock="";
                            }
                        }
                    }});
            }
        });
    }

    public void startJob(){
        if (marche==1 && arrete==0 && waiting==false) {
            waiting = true;
            textCounter.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            runTimer();
        } else if (marche==0 && arrete==1) {
            hideEverything();
            position = 0;
        }
    }

    private void hideEverything() {
        timer.cancel();
        textCounter.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        progressText.setVisibility(View.INVISIBLE);
        textCounter.setText("15");
        imageView.setAnimation(fadeIn);
        imageView.setImageResource(R.drawable.desi);
        imageView3.clearAnimation();
        imageView2.clearAnimation();
        imageView2.setVisibility(View.INVISIBLE);
        imageView3.setVisibility(View.INVISIBLE);

    }

    private void runTimer() {
        if (position<positions.length)
        {
            Toast.makeText(context, positions[position], Toast.LENGTH_LONG).show();
            timer.start();
            progressText.setText(String.format("Position %d/%d", position+1, positions.length));
            if (position==6) {
                imageView2.setVisibility(View.VISIBLE);
                imageView3.setVisibility(View.VISIBLE);
                imageView2.startAnimation(rotation);
                imageView3.startAnimation(rotation);
            }
            imageView.startAnimation(fadeOut);
            imageView.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            imageView.startAnimation(fadeIn);
            imageView.setImageResource(image_positions[position]);
            progressBar.setProgress(position);
            position++;
        } else {
            waiting = false;
            mediaPlayer3.start();
            position = 0;
            Toast.makeText(context, "Fin", Toast.LENGTH_LONG).show();
            hideEverything();
        }

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
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted =
                        intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { // Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Log.d("SERIAL", "SERIAL CONNECTION OPENED!");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERMISSION NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                openConnection();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serialPort.close(); // The connection is automatically closed, when the device is detached
            }
        };
    };

}
