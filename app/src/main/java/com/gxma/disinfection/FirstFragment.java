package com.gxma.disinfection;

import android.app.Activity;
import android.content.Context;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;
import java.util.concurrent.Executor;
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
    Context context ;

    ImageView imageView;
    private TextView textCounter, progressText;
    Animation fadeIn = new AlphaAnimation(1, 0);
    Animation fadeOut = new AlphaAnimation(1, 0);

    MediaPlayer mediaPlayer, mediaPlayer2, mediaPlayer3;
    ProgressBar progressBar;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        context = this.getContext();
    }

    @Override
    public void onStart() {
        mPhysicaloid = new Physicaloid(context);
        mPhysicaloid.setBaudrate(9600);
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
            class SerialTask extends AsyncTask<Void, Void, Void> {
                @Override
                protected Void doInBackground(Void... voids) {
                    while (true) {
                        Log.w("Thread", "enter the loop");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!mPhysicaloid.isOpened() && mPhysicaloid.open()) {
                                    mPhysicaloid.open();
                                    startSerialReading();
                                }
                                else if (!mPhysicaloid.isOpened()) {
                                    Toast.makeText(context, "Probleme de connexion", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            new SerialTask().execute();
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
    }

    private void runTimer() {
        if (position<positions.length)
        {
            Toast.makeText(context, positions[position], Toast.LENGTH_LONG).show();
            timer.start();
            progressText.setText(String.format("Position %d/%d", position+1, positions.length));

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
            Toast.makeText(context, "Fin", Toast.LENGTH_LONG).show();
            hideEverything();
        }

    }

}
