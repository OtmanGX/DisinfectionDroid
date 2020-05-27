package com.gxma.disinfection;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

public class SecondFragment extends Fragment {

    private Physicaloid mPhysicaloid;
    int connexion = 0;
    int marche=0, arrete=1;
    private int position = 0;
    private String dataStock="";
    private TextView textCounter;
    private CountDownTimer timer;
    private byte[] buf ;
    private static final String[] positions = new String[]{
            "HOMME POSITION 1 FORM 1",
            "HOMME POSITION 1 FORM 2",
            "HOMME POSITION 2 FORM 1" ,
            "HOMME POSITION 2 FORM 2"};

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void startSerialReading() {
//                        if(connexion==2){
//                            connect.setText("Fermer la connexion");
//                            connect.setTextColor(getApplication().getResources().getColor(R.color.GreenAccent));
//                        }
                        final Activity activity = this.getActivity();

                        mPhysicaloid.addReadListener(new ReadLisener() {
                            @Override
                            public void onRead(int size) {

                                buf = new byte[size];
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
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    startJob();
                                                }
                                            });
                                        }
                                        dataStock="";
                                    }
                                }
                            }
                        });
                    }



    public void startJob(){
                if (marche==1 && arrete==0) {
                    textCounter.setVisibility(View.VISIBLE);
                    runTimer();
                }
            }

    private void runTimer() {
        Toast.makeText(this.getContext(), positions[position], Toast.LENGTH_LONG).show();
        if (position<positions.length)
            timer.start();
            position++;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        textCounter = view.findViewById(R.id.textView);
        timer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long l) {
                textCounter.setText(String.valueOf(l/1000));
            }

            @Override
            public void onFinish() {
                runTimer();
            }
        };
//        if(mPhysicaloid.open()) {
//            byte[] buf = "moemoe".getBytes();
//            mPhysicaloid.write(buf, buf.length);
//            mPhysicaloid.close();
//        }
        startSerialReading();

        view.findViewById(R.id.button_second).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPhysicaloid.close();
                mPhysicaloid.clearReadListener();
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
    }
}
