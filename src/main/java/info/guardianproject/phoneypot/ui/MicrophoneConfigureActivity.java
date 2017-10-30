package info.guardianproject.phoneypot.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.maxproj.simplewaveform.SimpleWaveform;

import java.util.LinkedList;

import info.guardianproject.phoneypot.PreferenceManager;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.EventTrigger;
import info.guardianproject.phoneypot.sensors.media.AudioRecorderTask;
import info.guardianproject.phoneypot.sensors.media.MicSamplerTask;
import info.guardianproject.phoneypot.sensors.media.MicrophoneTaskFactory;
import me.angrybyte.numberpicker.listener.OnValueChangeListener;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

import static info.guardianproject.phoneypot.R.id.microphone;

public class MicrophoneConfigureActivity extends AppCompatActivity implements MicSamplerTask.MicListener {

    private MicSamplerTask microphone;
    private TextView mTextLevel;
    private ActualNumberPicker mNumberTrigger;
    private PreferenceManager mPrefManager;
    private SimpleWaveformExtended mWaveform;
    private LinkedList<Integer> mWaveAmpList;

    private double maxAmp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_configure);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextLevel = (TextView)findViewById(R.id.text_display_level);
        mNumberTrigger = (ActualNumberPicker)findViewById(R.id.number_trigger_level);
        mWaveform = (SimpleWaveformExtended)findViewById(R.id.simplewaveform);

        mNumberTrigger.setMinValue(0);
        mNumberTrigger.setMaxValue(120);
        mNumberTrigger.setListener(new OnValueChangeListener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                mWaveform.setThreshold(newValue);
            }
        });

        mPrefManager = new PreferenceManager(this.getApplicationContext());

        /**
        if (mPrefManager.getMicrophoneSensitivity().equals("High")) {
            mNumberTrigger.setValue(40);
        } else if (mPrefManager.getMicrophoneSensitivity().equals("Medium")) {
            mNumberTrigger.setValue(60);
        }
        else
        {
            try {
                //maybe it is a threshold value?
                mNumberTrigger.setValue(Integer.parseInt(mPrefManager.getMicrophoneSensitivity()));
            }
            catch (Exception e){}
        }**/

        initWave();
        startMic();
    }

    private void initWave ()
    {
        mWaveform.init();

        mWaveAmpList = new LinkedList<>();

        mWaveform.setDataList(mWaveAmpList);

        //define bar gap
        mWaveform.barGap = 30;

        //define x-axis direction
        mWaveform.modeDirection = SimpleWaveform.MODE_DIRECTION_RIGHT_LEFT;

        //define if draw opposite pole when show bars
        mWaveform.modeAmp = SimpleWaveform.MODE_AMP_ABSOLUTE;
        //define if the unit is px or percent of the view's height
        mWaveform.modeHeight = SimpleWaveform.MODE_HEIGHT_PERCENT;
        //define where is the x-axis in y-axis
        mWaveform.modeZero = SimpleWaveform.MODE_ZERO_CENTER;
        //if show bars?
        mWaveform.showBar = true;

        //define how to show peaks outline
        mWaveform.modePeak = SimpleWaveform.MODE_PEAK_ORIGIN;
        //if show peaks outline?
        mWaveform.showPeak = true;

        //show x-axis
        mWaveform.showXAxis = true;
        Paint xAxisPencil = new Paint();
        xAxisPencil.setStrokeWidth(1);
        xAxisPencil.setColor(0x88ffffff);
        mWaveform.xAxisPencil = xAxisPencil;

        //define pencil to draw bar
        Paint barPencilFirst = new Paint();
        Paint barPencilSecond = new Paint();
        Paint peakPencilFirst = new Paint();
        Paint peakPencilSecond = new Paint();

        barPencilFirst.setStrokeWidth(15);
        barPencilFirst.setColor(getResources().getColor(R.color.colorAccent));
        mWaveform.barPencilFirst = barPencilFirst;

        barPencilFirst.setStrokeWidth(15);

        barPencilSecond.setStrokeWidth(15);
        barPencilSecond.setColor(getResources().getColor(R.color.colorPrimaryDark));
        mWaveform.barPencilSecond = barPencilSecond;

        //define pencil to draw peaks outline
        peakPencilFirst.setStrokeWidth(5);
        peakPencilFirst.setColor(getResources().getColor(R.color.colorAccent));
        mWaveform.peakPencilFirst = peakPencilFirst;
        peakPencilSecond.setStrokeWidth(5);
        peakPencilSecond.setColor(getResources().getColor(R.color.colorPrimaryDark));
        mWaveform.peakPencilSecond = peakPencilSecond;
        mWaveform.firstPartNum = 0;


        //define how to clear screen
        mWaveform.clearScreenListener = new SimpleWaveform.ClearScreenListener() {
            @Override
            public void clearScreen(Canvas canvas) {
                canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            }
        };
        /**
        mWaveform.progressTouch = new SimpleWaveform.ProgressTouch() {
            @Override
            public void progressTouch(int progress, MotionEvent event) {
                Log.d("", "you touch at: " + progress);
                mWaveform.firstPartNum = progress;
                mWaveform.refresh();
            }
        };**/
        //show...
        mWaveform.refresh();
    }
    private void startMic ()
    {
        try {
            microphone = MicrophoneTaskFactory.makeSampler(this);
            microphone.setMicListener(this);
            microphone.execute();
        } catch (MicrophoneTaskFactory.RecordLimitExceeded e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (microphone != null)
            microphone.cancel(true);

    }

    private void save ()
    {
        mPrefManager.setMicrophoneSensitivity(mNumberTrigger.getValue()+"");
        finish();
    }

    @Override
    public void onSignalReceived(short[] signal) {
        /*
		 * We do and average of the 512 samples
		 */
        int total = 0;
        int count = 0;
        for (short peak : signal) {
            //Log.i("MicrophoneFragment", "Sampled values are: "+peak);
            if (peak != 0) {
                total += Math.abs(peak);
                count++;
            }
        }
        //  Log.i("MicrophoneFragment", "Total value: " + total);
        int average = 0;
        if (count > 0) average = total / count;
		/*
		 * We compute a value in decibels
		 */
        double averageDB = 0.0;
        if (average != 0) {
            averageDB = 20 * Math.log10(Math.abs(average) / 1);
        }

        if (averageDB > maxAmp) {
            maxAmp = averageDB + 5d; //add 5db buffer
            mNumberTrigger.setValue(new Integer((int)maxAmp));
            mNumberTrigger.invalidate();
        }

        int perc = (int)((averageDB/140d)*100d);
        mWaveAmpList.addFirst(new Integer((int)perc));
        if (mWaveAmpList.size() > mWaveform.width / mWaveform.barGap + 2) {
            mWaveAmpList.removeLast();
        }
        mWaveform.refresh();
        mTextLevel.setText(getString(R.string.current_noise_base) + ' ' + ((int)averageDB)+"db");

    }

    @Override
    public void onMicError() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.monitor_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_save:
                save();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }
}
