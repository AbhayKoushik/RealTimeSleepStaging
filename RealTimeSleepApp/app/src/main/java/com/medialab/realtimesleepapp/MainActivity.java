package com.medialab.realtimesleepapp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Gyro;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;
import com.choosemuse.libmuse.Battery;

/* Unused libmuse imports */
//import com.choosemuse.libmuse.AnnotationData;
//import com.choosemuse.libmuse.MuseConfiguration;
//import com.choosemuse.libmuse.MessageType;
//import com.choosemuse.libmuse.MuseFileFactory;
//import com.choosemuse.libmuse.MuseFileReader;
//import com.choosemuse.libmuse.Result;
//import com.choosemuse.libmuse.ResultLevel;
//import com.choosemuse.libmuse.MuseArtifactPacket;

/* Unused dropbox imports */
//import com.dropbox.client2
//import com.dropbox.client2.DropboxAPI;
//import com.dropbox.client2.android.AndroidAuthSession;
//import com.dropbox.client2.session.AppKeyPair;
//import com.dropbox.core.DbxException;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

/* Unused Graphview imports */
//import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
//import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

//import android.app.ProgressDialog;
//import android.widget.ToggleButton;

/* Unused android imports */
//import android.widget.ToggleButton;
//import android.os.Parcelable;
//import android.os.PowerManager;
//import android.provider.MediaStore;
//import android.support.annotation.NonNull;
//import android.graphics.Color;


import uk.me.berndporr.iirj.Butterworth;

import static java.lang.Math.sqrt;

public class MainActivity extends Activity implements OnClickListener {

    /* Number 1, 2 and 3 correspond to the following leads in the muse */
    /* EEG Lead 1 : Frontal Af7 */
    /* EEG Lead 2 : Frontal Af8 */
    /* EEG Lead 3 : Frontal tp10 */
    /*Enables sleeping on the left without disturbing the power buttons on the right */

    ////////////////////////////////////////////////////////////////////////////////
    private final String mTAG = "TestLibMuseAndroid";
//    private final String TAG = "SleepBCI";

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */

    private MuseManagerAndroid manager;
    private Handler activityhand = null;


//    /* BioEssence */
//    private MainActivityEssence.BluetoothDeviceData mSelectedDeviceData;
//    private BleManager mBleManager;
//    private AlertDialog mConnectingDialog;
//    private BleDevicesScanner mScanner;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */

    private MuseArtifactPacket artifactPacket;
    private Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     * <p>
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     * <p>
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     * <p>
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     * <p>
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */

    private Handler uiHandler = new Handler();

    /**
     * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */

    private ArrayAdapter<String> spinnerAdapter;
    private ArrayAdapter<String> eSpinnerAdapter;

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    private boolean dataTransmission = true;

    /* Connection Status */
    private String status=" ";

    //--------------------------------------

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
/////////////////////////////////* Olfaction */////////////////////////////////////

    private double sumFAI=0.0;
    TextView scoreFAI;

/////////////////////////////////* TF Lite *////////////////////////////////////////



    /////////////////////// Classification Activity //////////////////////////
    private static final int INPUT_SIZE = 3000;

    private static final String MODEL_FILE = "SleepZwake.tflite"; // input .tflite model here
    private static final String LABEL_FILE = "Labels_sleep.txt"; // Sleep Labels: 0 = Wake, 1 = N1, 2 = N2, 3 = N3, 4 = REM

    private String valueResult1="Stage: 0";
    private String valueResult2="Stage: 0";
    private String valueResult3="Stage: 0";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();


/////////////////////////////////* SigProc ^///////////////////////////////////////

    Butterworth butterworthH,butterworthL,butterworthBP,butterworthBS;

////////////////////////////// EEG Adaptation ///////////////////////////////////////////////////

    // Sampling Rate of Raw EEG
    private int sampling30=3000;

    /**
     * Variables and Functions to calculate standard deviation
     **/

    private int m_n; // number of data values pushed
    private double m_oldM, m_newM, m_oldS, m_newS;

    // Calculating Mean, Variance and Standard Deviation

    private void clearNormEEG()
    {
        m_n = 0;
    }

    private void pushEEG(double x)
    {
        m_n++;
        // See Knuth TAOCP vol 2, 3rd edition, page 232
        if (m_n == 1)
        {
            m_oldM = m_newM = x;
            m_oldS = 0.0;
        }
        else
        {
            m_newM = m_oldM + (x - m_oldM)/m_n;
            m_newS = m_oldS + (x - m_oldM)*(x - m_newM);

            // set up for next iteration
            m_oldM = m_newM;
            m_oldS = m_newS;
        }
    }
    private int NumDataValues() {  return m_n; }

    private double meanEEG()
    {
        return (m_n > 0) ? m_newM : 0.0;
    }

    private double varEEG()
    {
        return ((m_n > 1) ? m_newS/(m_n - 1) : 0.0 );
    }

    ////// unbiased standard deviation //////

    private double unbiasedStdEEG()
    {
        return sqrt( varEEG() );
    }

    ////// population standard deviation //////

    private double mean1EEG(double[] eeg)
    {
        int i;
        double sum=0.0;
        for(i=0;i<eeg.length;i++)
        {
            sum+=eeg[i];
        }
        return (sum/(double)eeg.length);
    }

    private float stdEEG(double[] eeg){
        float std,var;
        double mean=mean1EEG(eeg);
        int i;
        float sum=0.0f;
        for(i=0;i<eeg.length;i++)
        {
            sum+=Math.pow(Math.abs(eeg[i]-mean),2);
        }
        var=sum/(float)eeg.length;
        std=(float)Math.sqrt(var);
        return std;
    }


    private double[] normalizedEEG(double[] eeg)
    {
        int l,k;
        double mean,std;
        double[] norm_eeg=new double[sampling30];
        clearNormEEG();
        for(l=0;l<sampling30;l++)
        {
            pushEEG(eeg[l]);
        }
        std=stdEEG(eeg);
        eegStd="Std: "+String.valueOf(std);
        if(countStd<3) // Goes from 1 to 2 in 1 minutes, Finds the corresponding wake stage mean
        {
            stdWake=stdWake+(std-stdWake)/countStd;
            countStd=countStd+1;
        }

        stdWake=8; // Mean Hardcoding threshold
        for(k=0;k<sampling30;k++) {
            norm_eeg[k]=(eeg[k])/stdWake;
        }
        return  norm_eeg;
    }

    /**
     * Spectral Conditioning Variables and Functions for Thresholds
     */
    /////////////////////////////////////////////*Spectral Conditioning */////////////////////////////////////////////
    boolean spectralConditioning=false;

//    int deepSleep1=0,  unsure1=0,
//        deepSleep0=0,  unsure2=0,
//        deepSleep2=0,  unsure0=0;
//    double deepSleepD_G1=0.0, deepSleepD_G0=0.0, deepSleepD_G2=0.0;
//    double n1SleepABG_D=0.0;
//    double deepSleepThreshold1=100;
//    double n1SleepThreshold=100; // Non zero large value, so that it will adjust itself
//    double threshD1=0,threshD0=0,threshD2=0;

//    ArrayList<Double> alphaMuse1 = new ArrayList<Double>();
//    ArrayList<Double> thetaMuse = new ArrayList<Double>();
//    ArrayList<Double> betaMuse1 = new ArrayList<Double>();
//    ArrayList<Double> deltaMuse1 = new ArrayList<Double>();
//    ArrayList<Double> gammaMuse1 = new ArrayList<Double>();
//    ArrayList<Double> deltaMuse2 = new ArrayList<Double>();
//    ArrayList<Double> gammaMuse2 = new ArrayList<Double>();
//    ArrayList<Double> deltaMuse0 = new ArrayList<Double>();
//    ArrayList<Double> gammaMuse0 = new ArrayList<Double>();
//    private double meanValue(ArrayList<Double> array)
//    {
//        int i;
//        double sum=0.0;
//        for (i=0;i<array.size();i++)
//        {
//            sum=sum+array.get(i);
//        }
//        return (sum/array.size());
//    }

    /*Spectral Conditioning*/

////////////////////// Calibrating thresholds for Spectral Conditioning *////////////////////////

//    private double deepThreshold(double deepSleepD_G)
//    {
//        double threshD=0.0;
//        Log.i(mTAG, "D/G: "+String.valueOf(deepSleepD_G)+" threshDeep: "+String.valueOf(deepSleepThreshold1));
//        if(countStd<3) // Calibration for 1 minute
//        {
//            deepSleepThreshold1=deepSleepD_G;
//            if(deepSleepThreshold1!=100)
//            {
//                deepSleepThreshold1=deepSleepThreshold1+(deepSleepD_G-deepSleepThreshold1)/countStd;
//            }
//        }
//        return deepSleepThreshold1;
//    }

//    private double n1Threshold()
//    {
//        if(countStd<3)
//        {
////            n1SleepThreshold=n1SleepABG_D;
//            if(n1SleepThreshold!=100)
//            {
//                n1SleepThreshold=n1SleepThreshold+(n1SleepABG_D-n1SleepThreshold)/countStd;
//            }
//        }
//        Log.i(mTAG,"threshN1 "+ String.valueOf(n1SleepThreshold));
//        return n1SleepThreshold;
//    }


    /**
     * Audio-Neural Feedback and Media Adaptation for Sound during Sleep Stages
     *
     **/

    private boolean playable = false;
    private EditText numberPicker;
    public ArrayList<java.io.File> findTracks(java.io.File root){

        ArrayList<java.io.File> allTracks=new ArrayList<java.io.File>();
        File[] audiofiles=root.listFiles();
        Log.i(mTAG,"Path name: "+root.getName().toString());
        Log.i(mTAG,"Number of Files: "+String.valueOf(root.list().length));

        for(File singleFile: audiofiles)
        {
            Log.i(mTAG,"File Names: "+singleFile.getName().toString());
            if(singleFile.isDirectory() && !singleFile.isHidden())
            {
                allTracks.addAll(findTracks(singleFile));
            }
            else
            {
                if(singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".wav"))
                {
                    allTracks.add(singleFile);
                }
            }
        }
        return allTracks;
    }

    private MediaPlayer mPlayer;
    Uri trackUri;
    ListView playList;
    private int sleepAlarmT=3000;
    private String[] playables;
    ArrayAdapter<String> playAdapter;

    private double twopi = 8. * Math.atan(1.);
    private int sleepT=100;
    private final int SAMPLE_RATE = 44100;
    private AudioTrack audioA;
    int buffsize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
    private int sampleCount=10000; //(int) ((float) SAMPLE_RATE / frequency);


//    private Runnable audioRunA= new Runnable() {
//        @Override
//        public void run() {
//            while(recording) {
//                try {
//                    double waveCreative=(alphaBuffer[2]/thetaBuffer[2]+0.000000001)*200;
//                    double waveCalm=50/(alphaBuffer[2]+0.000000001);
//                    Log.i(mTAG,"Creative: "+String.valueOf(waveCreative)+" Calm: "+String.valueOf(waveCalm));
//                    if(waveToggle)
//                    {
//                        setWave1(waveCalm); //Log.i(TAG,"AlphaThetaValues "+ waveCreative);
//                    }
//                    else
//                    {
//                        setWave1(waveCreative);
//                    }
//                    audioA.play();
//                    audioTA.sleep(sleepT);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                if(audioA!=null) {
//                    audioA.release();
//                }
//            }
//        }
//    };

    private AudioTrack PlayWave1() {
        audioA = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buffsize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        return audioA;
    }

    private void setWave1(double frequencyA) {
        short samples1[] = new short[sampleCount];
        int amplitude1 = 32767;
        double phase1 = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            samples1[i] = (short) (amplitude1 * Math.sin(phase1));
            phase1 += twopi * frequencyA / SAMPLE_RATE;
        }
        audioA=PlayWave1();
        audioA.write(samples1, 0, sampleCount);
    }

    private Runnable audioREM= new Runnable() {
        @Override
        public void run() {
            while (recording) {
                if (remBell) {
                    try {
                        if (playable) {
                            mPlayer.start();
                            remBell=false;
                            remThread.sleep(sleepAlarmT);
                            mPlayer.seekTo(0);
                        }

                        ///// Audio Adaptation /////
//                        setREMWave(50);
//                        remAudio.play();
//                        remThread.sleep(1);
//                        remAudio.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };
    /* Audio-Neural Feedback - AK */
//    private Thread audioTA= new Thread(audioRunA);
    private Thread remThread= new Thread(audioREM);




    ///////////////////////////////////////////// DropBox Variables //////////////////////////////////////////////////
    // Before accessing the button upload, fill in the corresponding dropbox access variables
    final static private String APP_KEY = " "; // Insert the corresponding dropbox app key
    final static private String APP_SECRET = " "; // Insert the corresponding dropbox app secret
    private DbxClientV2 client;
    private static final String ACCESS_TOKEN = " "; // Insert the corresponding dropbox access token


    /**
     * Designing Variables and Buffers for Muse EEG
     */
    //////////////////////////////// Brain Wave Storage variables ///////////////////////////////////////////////

    private double faIndex;

    /* EEG Lead 1 : Frontal Af7 */
    ArrayList<Double> epoch1 = new ArrayList<>(); // EEG values collected at Muse sampling rate
    ArrayList<Double> eeg301 = new ArrayList<>(); // EEG values selected at training dataset's sampling rate
    double[] normalizedBrain1=new double[sampling30]; // Normalized EEG sent in for classification
    float[] normalizedBrainF1=new float[sampling30]; // Normalized EEG sent in for classification
    ArrayList<Double> outputEEG1 = new ArrayList<>();


    /* EEG Lead 2 : Frontal Af8 */
//    ArrayList<Double> epoch2 = new ArrayList<Double>(); // EEG values collected at Muse sampling rate
//    ArrayList<Double> eeg302 = new ArrayList<Double>(); // EEG values selected at training dataset's sampling rate
//    double[] normalizedBrain2 =new double[sampling30]; // Normalized EEG sent in for classification
//    ArrayList<Double> outputEEG2 = new ArrayList<Double>();
//

    /* EEG Lead 3 : Frontal tp10 */
    ArrayList<Double> epoch3 = new ArrayList<>(); // EEG values collected at Muse sampling rate
    ArrayList<Double> eeg303 = new ArrayList<>(); // EEG values selected at training dataset's sampling rate
    double[] normalizedBrain3 =new double[sampling30]; // Normalized EEG sent in for classification
    ArrayList<Double> outputEEG3 = new ArrayList<>();

    double stdWake=0;


    // double[] eegSleep=new double[sampling30]; // EEG storage alternative

    /*   variables selected for visualization and filecapture */

    /* GraphView */
    private GraphView graphWaves, hypnoGram, hypnoGram3, graphFrontal; //, graphAcc;
    private boolean vizFAI=false, vizEEG=true;
    private int counter=0,counterStage=0,counterStage3=0;
    // Visualization variables
    LineGraphSeries<DataPoint> seriesEEG,seriesStages, seriesStages3, seriesFrontal;
    ////////////////////////////////////////////////////////////////

    private double[] eegBuffer = new double[6];
    private boolean eegStale;

    private double[] isgoodBuffer=new double[4];
    private boolean isgoodStale;

    /* Relative power spectral density values of brain waves */

    private double[] alphaBuffer = new double[6];
    private boolean alphaStale;
    private double[] betaBuffer = new double[6];
    private boolean betaStale;
    private double[] gammaBuffer = new double[6];
    private boolean gammaStale;
    private double[] deltaBuffer = new double[6];
    private boolean deltaStale;
    private double[] thetaBuffer = new double[6];
    private boolean thetaStale;

    private double[] batteryBuffer=new double[3];
    private boolean batteryStale;

    /* Absolute power spectral density values of brain waves */
    private double[] alphaABuffer = new double[6];
    private boolean alphaAStale;
    private double[] betaABuffer = new double[6];
    private boolean betaAStale;
    private double[] gammaABuffer = new double[6];
    private boolean gammaAStale;
    private double[] deltaABuffer = new double[6];
    private boolean deltaAStale;
    private double[] thetaABuffer = new double[6];
    private boolean thetaAStale;

    /* Accelerometer values */
    private double[] accelBuffer = new double[3];
    private boolean accelStale;

    /* gyrometer values */
    private double[] gyroBuffer = new double[3];
    private boolean gyroStale;
    private int max_brain_thresh=0,max_calm_thresh=0;
    private boolean jawBuffer,blinkBuffer;
    private int blinkCount = 0, clenchCount = 0;
    private boolean artifactStale;
    private TextView calInst;
    private TextView brainT;
    private TextView calmT;
    private TextView stdEEG;
    private String eegStd;
    private int countStd=1;
    private boolean waveToggle=false;

    /* For moving average calculation */

    private int movingCount;
    // Averaging window size

    private int aveWindow=20;
    private double[] avgAccel=new double[aveWindow];
    private double[] avgABuffer= new double[aveWindow];
    private double[] avgBBuffer= new double[aveWindow];
    private double[] avgGBuffer= new double[aveWindow];
    private double[] avgDBuffer= new double[aveWindow];
    private double[] avgTBuffer= new double[aveWindow];
    private double aveAccel,aveA,aveB,aveG,aveD,aveT;

    String entryStage1="0";
    String entryConfidence1="0";
    String surity1="";

    String entryStage3="0";
    String entryConfidence3="0";
    String surity3="";

    String entryStage2="0";
    String entryConfidence2="0";
    String surity2="";


    String isGood="0";

    private boolean filledBuffer;
    //   private short sW=0, sR=0, sN1=0, sN2=0, sN3=0, sN4=0,sM=0; /* FOr custom sleep staging algorithm */


    /**
     * ///////////////////////////////////// TimeFlow and Design variables and UI for 30 second EEG processing //////////////////////////////////////////////////////
     */

    private boolean sphincter1=true;
    private boolean sphincter2=true;
    private String minutePrevious, secondPrevious, currentTime;
    private boolean minuteChange=false;
    private boolean rerun=false;
    private String secondStart;
    private boolean checkEpochOnce=true;
    private int calibrationTime=0,calBegin=0,secondCount=0;
    private boolean calibrating=false,calAble=false,calibrationDone=false, calSwitch=true, epochAble=false,
            toggleRelWaves, toggleAccGyro,toggleFAI, toggleEEG;

    LinearLayout spinnerLayout,connectionLayout, relwavesLayout, accgyroLayout, faiLayout, eegLayout;
    Spinner musesSpinner;
    Button helpButton, refreshButton, connectButton, disconnectButton, pauseButton, uploadButton,
            playButton, calibrateButton, togglewaveButton, viz2Button, viz1Button, listButton,
            accgyroButton, relwaveButton, faiButton, eegButton;


    SeekBar seekbar;
    private View clearButton; //,detectButton;
    private TextView sleepStage1,stage1,sleepStage2,stage2 ,sleepStage3,stage3,fp1, fp2, tp9, tp10, elem1, elem2, elem3, elem4,
    //                                             elemA1, elemA2, elemA3, elemA4,
    acc_x, acc_y, acc_z,
            gyro_x, gyro_y, gyro_z,
            jaw, blink, isgood, battery; // For getting data from textViews

    private String sleepView1="",sleepView3="",sleepView2="";

    private boolean badBLE=false;

    /**
     /////////////////////////////////////////////// File Output Stream Variables /////////////////////////////////////////////
     **/

    java.io.File datafiles;
    int nf=0;
    private String[] writables;
    private FileOutputStream fos,fosLog1, fosLog1N, fosLog3, fosLog3N;
    private java.io.File file,fileLog1,fileLog1N,fileLog3,fileLog3N;
    private Uri filePath;
    private EditText editText;
    private String nameOfFile;
    private boolean recordingBegin=true;
    private boolean recording = false;  // Boolean indicating the recording state while connected

    /* File Search for Automatic File Naming */

    public ArrayList<java.io.File> findDataFiles(java.io.File root){

        ArrayList<java.io.File> allcsv=new ArrayList<java.io.File>();
        File[] csvfiles=root.listFiles();
        Log.i(mTAG,"Path name: "+root.getName().toString());
        Log.i(mTAG,"Number of Files: "+String.valueOf(root.list().length));

        for(File singleFile: csvfiles)
        {
            Log.i(mTAG,"File Names: "+singleFile.getName().toString());
            if(singleFile.isDirectory() && !singleFile.isHidden())
            {
                allcsv.addAll(findDataFiles(singleFile));
            }
            else
            {
                if(singleFile.getName().endsWith(".csv"))
                {
                    allcsv.add(singleFile);
                }
            }
        }
        return allcsv;
    }

    /**
     /////////////////////////////// TF Lite Classifier ///////////////////////////////
     **/
    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TFLiteSleepClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE);
                    Log.d(mTAG, "Load Success");
                }
                catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    ////////////////////////////////////////// Sleep Experiments: Update in both 30 second epochs ///////////////////////////////////////////////////////////////

    private boolean remBell;
    private int durationREMBell; // duration*0.1 seconds
    private int second30=0;

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////
    //--------------------------------------
    // File I/O

    /**
     * We don't want to block the UI thread while we write to a file, so the file
     * writing is moved to a separate thread.
     */
    private Runnable fileRun = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            while(recording) {

                double relA = (alphaBuffer[1]+alphaBuffer[2])/2;
                double relB = (betaBuffer[1]+betaBuffer[2])/2;
                double relG = (gammaBuffer[1]+gammaBuffer[2]/2);
                double relD = (deltaBuffer[1]+deltaBuffer[2])/2;
                double relT = (thetaBuffer[1]+thetaBuffer[2])/2;

//                Log.i(mTAG,"FileRunning");
                currentTime = getCurrentTimeStamp();
                String minuteCurrent = currentTime.split(":")[1];

                if(recordingBegin)
                {
                    recordingBegin=false;
                    try {
                        fileThread.sleep(1000);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                try {
                    fileThread.sleep(1000/220);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                // Time Stamping onto File

                String currentTimePrint=getCurrentTimeStamp();
                String[] times=currentTimePrint.split(":");
//                Log.i(mTAG,times[0]+" "+times[1]+" "+times[2]+" "+" ");
                String[] secmilli=times[2].split("\\.");
                String entryTHour = times[0] + ",";
                String entryTMin = times[1] + ",";
                String entryTSec = secmilli[0]+ ",";
                String entryTMilli = secmilli[1] + ",";

                String blinkSCount = "" + ",";
                String clenchSCount = "" + ",";
                if(!minutePrevious.equals(minuteCurrent))
                {
                    minuteChange=true;
                    blinkSCount = String.valueOf(blinkCount) + ",";
                    clenchSCount = String.valueOf(clenchCount) + ",";
                    blinkCount = 0;
                    clenchCount = 0;
                    minutePrevious = minuteCurrent;
                }
                else
                {
                    minuteChange=false;
                }

/////////////////////* Audio-Neural Feedback - AK *////////////////////

//                if(calibrating)
//                {
//                    calibrationTime=secondCount;
//                    if(calibrationTime<=calBegin+60)
//                    {
//                        if(max_calm_thresh<1000*alphaBuffer[2])
//                        {
//                            max_calm_thresh=(int)(1000*alphaBuffer[2]);
//                        }
//                        if(max_brain_thresh<1000*(alphaBuffer[2]/(thetaBuffer[2]+0.0000001)))
//                        {
//                            max_brain_thresh=(int)(1000*(alphaBuffer[2]/(thetaBuffer[2]+0.0000001)));
//                        }
////                        Log.i(mTAG,"Calibrating############");
//                    }
//                    else
//                    {
//                        calibrationDone=true;
//                        calibrating=false;
//                    }
//                }


                /* Averaging out TP9 and TP10 electrodes */

                Float acceleration = (float) sqrt(Math.pow(accelBuffer[0], 2)
                        + Math.pow(accelBuffer[1], 2)
                        + Math.pow(accelBuffer[2], 2));

                Float gyrometer = (float) sqrt(Math.pow(gyroBuffer[0], 2)
                        + Math.pow(gyroBuffer[1], 2)
                        + Math.pow(gyroBuffer[2], 2));

                /**
                 *  Moving average
                 **/


//                if(movingCount==aveWindow)
//                {
//                    filledBuffer=true;
//                    movingCount=0;
//                }
//                else {
//                    try {
//                        avgAccel[movingCount] = acceleration;
//                        avgAccel[movingCount] = acceleration;
//                        avgABuffer[movingCount] = relA;
//                        avgBBuffer[movingCount] = relB;
//                        avgGBuffer[movingCount] = relG;
//                        avgDBuffer[movingCount] = relD;
//                        avgTBuffer[movingCount] = relT;
//                        movingCount = movingCount + 1;
//                    }
//                    catch (Exception e)
//                    {
//                        e.printStackTrace();
//                    }
//                } // Variables for smoothening
//
//
//                if(filledBuffer=true)
//                {
//                    int iter;
//                    aveAccel=avgAccel[0];
//                    aveA=avgABuffer[0];
//                    aveB=avgBBuffer[0];
//                    aveG=avgGBuffer[0];
//                    aveD=avgDBuffer[0];
//                    aveT=avgTBuffer[0];
//
//                    for(iter=0; iter<aveWindow; iter++)
//                    {
//
//                        aveAccel=aveAccel+(avgAccel[iter]-aveAccel)/(iter+1);
//                        aveA=aveA+(avgABuffer[iter]-aveA)/(iter+1);
//                        aveB=aveB+(avgBBuffer[iter]-aveB)/(iter+1);
//                        aveG=aveG+(avgGBuffer[iter]-aveG)/(iter+1);
//                        aveD=aveD+(avgDBuffer[iter]-aveD)/(iter+1);
//                        aveT=aveT+(avgTBuffer[iter]-aveT)/(iter+1);
//                    }
//                }

                String entry1 = String.valueOf(eegBuffer[0]) + "," + String.valueOf(eegBuffer[1]) + "," + String.valueOf(eegBuffer[2]) + "," + String.valueOf(eegBuffer[3]) + ",";
                String entry2, entry3, entry4, entry5; //, entryA2, entryA3, entryA4, entryA5;

                entry2 = noNaN(String.valueOf(alphaBuffer[0]) + "," + String.valueOf(betaBuffer[0]) + "," + String.valueOf(gammaBuffer[0]) + "," + String.valueOf(deltaBuffer[0]) + "," + String.valueOf(thetaBuffer[0])) + ",";
                entry3 = noNaN(String.valueOf(alphaBuffer[1]) + "," + String.valueOf(betaBuffer[1]) + "," + String.valueOf(gammaBuffer[1]) + "," + String.valueOf(deltaBuffer[1]) + "," + String.valueOf(thetaBuffer[1])) + ",";
                entry4 = noNaN(String.valueOf(alphaBuffer[2]) + "," + String.valueOf(betaBuffer[2]) + "," + String.valueOf(gammaBuffer[2]) + "," + String.valueOf(deltaBuffer[2]) + "," + String.valueOf(thetaBuffer[2])) + ",";
                entry5 = noNaN(String.valueOf(alphaBuffer[3]) + "," + String.valueOf(betaBuffer[3]) + "," + String.valueOf(gammaBuffer[3]) + "," + String.valueOf(deltaBuffer[3]) + "," + String.valueOf(thetaBuffer[3])) + ",";

//                entryA2 = noNaN(String.valueOf(alphaABuffer[0]) + "," + String.valueOf(betaABuffer[0]) + "," + String.valueOf(gammaABuffer[0]) + "," + String.valueOf(deltaABuffer[0]) + "," + String.valueOf(thetaABuffer[0])) + ",";
//                entryA3 = noNaN(String.valueOf(alphaABuffer[1]) + "," + String.valueOf(betaABuffer[1]) + "," + String.valueOf(gammaABuffer[1]) + "," + String.valueOf(deltaABuffer[1]) + "," + String.valueOf(thetaABuffer[1])) + ",";
//                entryA4 = noNaN(String.valueOf(alphaABuffer[2]) + "," + String.valueOf(betaABuffer[2]) + "," + String.valueOf(gammaABuffer[2]) + "," + String.valueOf(deltaABuffer[2]) + "," + String.valueOf(thetaABuffer[2])) + ",";
//                entryA5 = noNaN(String.valueOf(alphaABuffer[3]) + "," + String.valueOf(betaABuffer[3]) + "," + String.valueOf(gammaABuffer[3]) + "," + String.valueOf(deltaABuffer[3]) + "," + String.valueOf(thetaABuffer[3])) + ",";
//                String entry6 = String.valueOf(accelBuffer[0]) + "," + String.valueOf(accelBuffer[1]) + "," + String.valueOf(accelBuffer[2]) + ",";
                String entry7 = Float.toString(acceleration) + ",";
//                String entry6G = String.valueOf(gyroBuffer[0]) + "," + String.valueOf(gyroBuffer[1]) + "," + String.valueOf(gyroBuffer[2]) + ",";
                String entry7G = Float.toString(gyrometer) + ",";
                String entry8, entry9;
                if (jawBuffer) {
                    entry8 = "1,";
                    clenchCount = clenchCount + 1;
                }
                else {
                    entry8 = "0,";
                }
                if (blinkBuffer) {
                    entry9 = "1,";
                    blinkCount = blinkCount + 1;
                }
                else {
                    entry9 = "0,";
                }

                /* External Conditioning and Confidence Thresholding Variables */
//                if(unsure2==1)
//                {
//                    surity2="5";
//                }
//                if(deepSleep2==1)
//                {
//                    surity2="3";
//                }
//                if(unsure0==1)
//                {
//                    surity0="5";
//                }
//                if(deepSleep0==1)
//                {
//                    surity0="3";
//                }
//                if(unsure1==1)
//                {
//                    surity1="5";
//                }
//                if(deepSleep1==1)
//                {
//                    surity1="3";
//                }
//                if(n1Sleep==1)
//                {
//                    surity1="1";
//                }

                isGood=String.valueOf(isgoodBuffer[0])+","+String.valueOf(isgoodBuffer[1])+","+
                        String.valueOf(isgoodBuffer[2])+","+String.valueOf(isgoodBuffer[3]);

                try {
                    // Time Entries
                    fos.write((currentTimePrint+",").getBytes());
                    fos.write(entryTHour.getBytes());
                    fos.write(entryTMin.getBytes());
                    fos.write(entryTSec.getBytes());
                    fos.write(entryTMilli.getBytes());

                    // EEG Raw Entries
                    fos.write(entry1.getBytes());

                    //Relative EEG Values
                    fos.write(entry2.getBytes());
                    fos.write(entry3.getBytes());
                    fos.write(entry4.getBytes());
                    fos.write(entry5.getBytes());

                    // Optimizing file writing

                    //Absolute Values
//                    fos.write(entryA2.getBytes());
//                    fos.write(entryA3.getBytes());
//                    fos.write(entryA4.getBytes());
//                    fos.write(entryA5.getBytes());
//                    fos.write(entry6.getBytes());

                    // Acceleration
                    fos.write(entry7.getBytes());

//                    fos.write(entry6G.getBytes());

                    // Gyrometer
                    fos.write(entry7G.getBytes());
                    fos.write(entry8.getBytes());
                    fos.write(entry9.getBytes());
                    fos.write(blinkSCount.getBytes());
                    fos.write(clenchSCount.getBytes());
                    fos.write((entryStage1+",").getBytes());
                    fos.write((entryConfidence1+",").getBytes());
                    fos.write((surity1+",").getBytes());

//                    fos.write((entryStage0+",").getBytes());
//                    fos.write((entryConfidence0+",").getBytes());
//                    fos.write((surity0+",").getBytes());
//                    fos.write((entryStage2+",").getBytes());
//                    fos.write((entryConfidence2+",").getBytes());
//                    fos.write((surity2+",").getBytes());

                    fos.write((isGood+"\n").getBytes());

                } catch (Exception i) {
                    i.printStackTrace();
                }
            }
            Looper.loop();
        }
    };

    /**
     * Filtering and Normalization of Downsampled EEG
     * @param epoch
     * @return normalizedBrain: The ArrayBuffer of preprocessed 30 second EEG Values
     */



    private double[] preprocessEEG(ArrayList<Double> epoch, FileOutputStream fosLog,FileOutputStream fosLog1) {
        int m, e, j;
        double[] eeg30 = new double[sampling30]; // EEG values selected at training dataset's sampling rate
        double[] normalizedBrain; // Normalized EEG sent in for classification
        double[] outputEEG = new double[sampling30];

        int lengthEpoch=epoch.size();
        float step = epoch.size() / 3000f; // denominator should be same as float of sampling30

        double sumEpoch=0.0;
        for (e=0;e<lengthEpoch;e++)
        {
            sumEpoch=sumEpoch+epoch.get(e);
        }

        double meanEEGepoch=sumEpoch/(double)lengthEpoch;

        /* Filtering Before Down-sampling */
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
        int vSamplingRate=lengthEpoch/30;

        butterworthBS= new Butterworth();
        butterworthBS.bandStop(1,vSamplingRate,60,10);
        butterworthBP= new Butterworth();
        butterworthBP.bandPass(1, vSamplingRate,23,44); // 1 to 45 Hz

        for (e=0;e<lengthEpoch;e++)
        {
            double rawEEG=(epoch.get(e)-meanEEGepoch);
            double notchfilteredEEG= (butterworthBS.filter(rawEEG));
            epoch.set(e,(butterworthBP.filter(notchfilteredEEG)));
        }
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /* Downsampling to 100 Hz */
        int eegindex=0;

        Log.i(mTAG, "Length: " + String.valueOf(epoch.size()));
        for (e = 0; e < lengthEpoch; e++) {
            int idx = Math.round(step * e); // In order to obtain equally spaced samples
            if (idx < lengthEpoch)
            {
                eeg30[eegindex]=(epoch.get(idx));
                eegindex=eegindex+1;
            }
        }

        // Alternative Storage of EEG //  for (j=0;j<eeg30.size();j++){ eegSleep[j]=eeg30.get(j); }
        boolean noise = false;

        ////////////////////////////////////////////////////////////////////////////////////////////////
        /* Filtering the Downsampled EEG  */
        /* Filtering Low Pass 48 and High Pass 1 Hz */

//////        double meanEEGepoch=mean1EEG(eeg30);
        for (m = 0; m < eeg30.length; m++)
        {
            double rawDownEEG=(eeg30[m]-mean1EEG(eeg30));
            outputEEG[m]=(butterworthBP).filter(rawDownEEG);
//            outputEEG[m]=(butterworthL.filter(outputEEG[m]));
//            outputEEG[m]=(butterworthH.filter(outputEEG[m]));
        }


        /* Normalization of Filtered EEG */

        double std=stdEEG(eeg30);
        eegStd="Std: "+String.valueOf(std);
        stdEEG.setText(eegStd);

        Log.i(mTAG,eegStd);

        normalizedBrain = normalizedEEG(eeg30); // outputEEG if filtering after downsampling else, eeg30
        for (m = 0; m < normalizedBrain.length ; m++) {
            try
            {
                // Writing to file before noise clipping
                fosLog1.write((String.valueOf(normalizedBrain[m]) + ",").getBytes());
                fosLog.write(String.valueOf(eeg30[m]+",").getBytes());

            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }

            /* Clipping Spiky Noise */
            if(normalizedBrain[m]>4)
            {
                normalizedBrain[m]=4;
            }
            if(normalizedBrain[m]<-4)
            {
                normalizedBrain[m]=-4;
            }

            if (normalizedBrain[m] != normalizedBrain[m])
            {
                Log.i(mTAG, "Nan");
                badBLE = true;
                sleepView1 = "Bad BLE";
            }
            else
            {
                badBLE = false;
            }
        }

        Log.i(mTAG,"Length eeg30: "+String.valueOf(eeg30.length));
        Log.i(mTAG,"Length NormalizedBrain: "+String.valueOf(normalizedBrain.length));
        return normalizedBrain;
    }


    private Thread fileThread = new Thread(fileRun);
    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    @Override
    public void onClick(View v)
    {

        // Media Audio File Selection for Playing during a Sleep Stage

        if (v.getId() == R.id.listButton)
        {

            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.setContentView(R.layout.list_view);
            dialog.setTitle("Title...");
            playList = (ListView) dialog.findViewById(R.id.playList);

            final ArrayList<java.io.File> tracks = findTracks(getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath()));
//                    Environment.getDataDirectory().getAbsolutePath())); // /sdcard
            playables = new String[tracks.size()];

            for (int i = 0; i < tracks.size(); i++) {
                playables[i] = tracks.get(i).getName().toString();
            }

            if(playables.length==0)
            {
                Toast.makeText(getApplicationContext(), "Please transfer the sound files to "+datafiles, Toast.LENGTH_LONG).show();
            }

            playAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.track_layout, R.id.item_text, playables);
            playList.setAdapter(playAdapter);
            playList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                    trackUri = Uri.parse(tracks.get(position).toString());
                    if (trackUri != null)
                    {
                        Log.i(mTAG, "Track Set");
                        mPlayer = MediaPlayer.create(getApplicationContext(), trackUri); // Custom track
                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                // Do something. For example: playButton.setEnabled(true);
                                playable = true;
                                Log.i(mTAG, "MediaPlayer Prepared");
                            }
                        });
                        dialog.dismiss();
                    } else {
                        Log.i(mTAG, "Track Default");
                        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silence); // Dog Barking Sound/Silence
                        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                // Do something. For example: playButton.setEnabled(true);
                                playable = true;
                                Log.i(mTAG, "MediaPlayer Prepared");
                            }
                        });
                        dialog.dismiss();
                    }
                }
            });
            dialog.show();
        }

        if(v.getId()==R.id.buttonViz1)
        {
            vizEEG=!vizEEG;
        }

        if(v.getId()==R.id.buttonEEG)
        {
            toggleEEG=!toggleEEG;
            if(toggleEEG)
            {
                eegLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                eegLayout.setVisibility(View.GONE);
            }
        }

        if(v.getId()==R.id.buttonAccelGyro)
        {
            toggleAccGyro=!toggleAccGyro;
            if(toggleAccGyro)
            {
                accgyroLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                accgyroLayout.setVisibility(View.GONE);
            }
        }

        if(v.getId()==R.id.buttonRelWaves)
        {
            toggleRelWaves=!toggleRelWaves;
            if(toggleRelWaves)
            {
                relwavesLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                relwavesLayout.setVisibility(View.GONE);
            }
        }
        if(v.getId()==R.id.buttonFAI)
        {
            toggleFAI=!toggleFAI;
            if(toggleFAI)
            {
                faiLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                faiLayout.setVisibility(View.GONE);
            }
        }

        if (v.getId()== R.id.upload)
        {
            if(isNetworkAvailable())
            {
                Toast.makeText(getApplicationContext(), "Getting ready to upload", Toast.LENGTH_SHORT).show();
                new Upload().execute();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Please turn on the wifi and retry", Toast.LENGTH_SHORT).show();
            }
        }

        if (v.getId() == R.id.refresh) {
            Log.i(mTAG,"Refreshing...");
            // The user has pressed the "Refresh" button.
            // Start listening for nearby or paired Muse headbands. We call stopListening
            // first to make sure startListening will clear the list of headbands and start fresh.
            manager.stopListening();
            manager.startListening();

        } else if (v.getId() == R.id.connect) {
            Log.i(mTAG,"Connecting...");


            // Writing File:
            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.
            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Log.w(mTAG, "There is nothing to connect to");
            } else
            {

                nf=0;
                //////////////////////////File Operations/////////////////////////
                // Start up a thread for asynchronous file operations.
                // This is only needed if you want to do File I/O.

                final ArrayList<java.io.File> datafile = findDataFiles(datafiles);
//                    Environment.getDataDirectory().getAbsolutePath())); // /sdcard
                writables = new String[datafile.size()];
                int[] filenumbers=new int[datafile.size()];
                if(datafile.size() == 0)
                {
                    Log.i(mTAG,"Empty Data");
                }
                else
                {
                    for (int i = 0; i < datafile.size(); i++) {
                        String namefile=datafile.get(i).getName().toString();
                        String prefix=(namefile.split(".csv")[0]);
                        if (prefix.matches("-?\\d+(\\.\\d+)?"))
                        {
                            writables[i] = namefile;
                            filenumbers[i] = Integer.valueOf(prefix);
//                        Log.i(mTAG, "Writables here: " + writables[i]);
                        }
                    }
                    Arrays.sort(filenumbers);

                    for (int nfile : filenumbers) {
                        try {
                            if (nfile == nf) {
                                nf = nf + 1;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                //            nameOfFile = editText.getText().toString();
                try {
                    nameOfFile = String.valueOf(nf);
                    file = new java.io.File(datafiles, nameOfFile + ".csv");
                    fos = new FileOutputStream(file);

                    fileLog1 = new java.io.File(datafiles, "eegRawNorm1.csv");
                    fosLog1 = new FileOutputStream(fileLog1);

                    fileLog1N = new java.io.File(datafiles, "eegFNorm1.csv");
                    fosLog1N = new FileOutputStream(fileLog1N);

                    fileLog3 = new java.io.File(datafiles, "eegRawNorm3.csv");
                    fosLog3 = new FileOutputStream(fileLog3);

                    fileLog3N = new java.io.File(datafiles, "eegFNorm3.csv");
                    fosLog3N = new FileOutputStream(fileLog3N);

                    Toast.makeText(getApplicationContext(), "Created new file: " + nameOfFile, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
//            editText.clearFocus();
                /* ------------------------File Initialization------------------------ */

                // Cache the Muse that the user has selected.
                muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.
                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
                Log.i(mTAG, "Registerd ConnectionListener");
                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.BETA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.THETA_RELATIVE);
                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.BETA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.THETA_ABSOLUTE);
                muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
                muse.registerDataListener(dataListener, MuseDataPacketType.GYRO);
                muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
                muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
                muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);
                muse.registerDataListener(dataListener, MuseDataPacketType.ARTIFACTS);
                muse.registerDataListener(dataListener, MuseDataPacketType.IS_GOOD);

//                muse.enableException(true);
//                Log.i(mTAG,"Exceptions enabled");
                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();
            }
        } else if (v.getId() == R.id.disconnect) {
            // The user has pressed the "Disconnect" button.
            // Disconnect from the selected Muse.
            Log.i(mTAG,"Disconnecting...");
            if (muse != null) {
                Log.i(mTAG, "Muse is NULL");
                muse.disconnect();
            }
        }
        else if (v.getId() == R.id.pause)
        {
            // The user has pressed the "Pause/Resume" button to either pause or
            // resume data transmission.  Toggle the state and pause or resume the
            // transmission on the headband.

            if (muse != null)
            {
                dataTransmission = !dataTransmission;
                muse.enableDataTransmission(dataTransmission);

            }
        }
/**
 *
 *  Extra UI features for Audio Neural Feedback and Visualizations
 */
        //        ////////////////////// Exit //////////////////////////
//        if(v.getId()==R.id.buttonClear)
//        {
//            finish();
//            System.exit(0);
//        }
//        //////////////////////////////////////////////////////


//        ///////////////////// Vizualize Frontal ////////////////
//        if(v.getId()==R.id.buttonViz2)
//        {
//            vizFAI=!vizFAI;
//        }

        //////////////////////////* Audio-Neural Feedback - AK *////////////////////////

//        if (v.getId() == R.id.calib) {
//            calBegin = secondCount;
//            calibrating = true;
//            togglewaveButton.setVisibility(View.VISIBLE);
//            playButton.setVisibility(View.VISIBLE);
//            calibrateButton.setVisibility(View.INVISIBLE);
//        }

//        if (v.getId() == R.id.play)
//        {
//
//            if (!audioTA.isAlive()) {
//                audioTA.start();
//            }
//            if (waveToggle)
//            {
//                calInst.setText("Calm Audioplay");
//            }
//            else
//            {
//                calInst.setText("Creative Audioplay");
//            }
//        }
//        if (v.getId() == R.id.toggleWave)
//        {
//            waveToggle = !waveToggle;
//            if (waveToggle) {
//                calInst.setText("Calm Audioplay");
//            } else {
//                calInst.setText("Creative Audioplay");
//            }
//        }
//        if(v.getId()==R.id.buttonEEG)
//        {
//            toggleEEG=!toggleEEG;
//            if(toggleEEG)
//            {
//                eegLayout.setVisibility(View.VISIBLE);
//            }
//            else
//            {
//                eegLayout.setVisibility(View.GONE);
//            }
//        }
//        if (v.getId() == R.id.help) {
//            Log.i(mTAG,"Help inside");
//            Toast.makeText(getApplicationContext(), "Step 1: Enter File name to be saved before clicking REFRESH", Toast.LENGTH_LONG).show();
//            Toast.makeText(getApplicationContext(), "Step 2: After refreshing, wait till the muse is available, else REFRESH again", Toast.LENGTH_LONG).show();
//            Toast.makeText(getApplicationContext(), "Step 3: Select the muse and press CONNECT, as soon as its connected, data starts recording", Toast.LENGTH_LONG).show();
//            Toast.makeText(getApplicationContext(), "Step 4: After the session is over, press DISCONNECT to stop recording and save the file", Toast.LENGTH_LONG).show();
//        }


    }
    //--------------------------------------
    // Permissions
    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     * <p>
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        }
                    };
            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
            Log.i(mTAG,"Ensured Permissions");
        }
    }

    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     *
     * @param p    A packet containing the current and prior connection states
     * @param muse The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();
        // Format a message to show the change of connection state in the UI.
        status = p.getPreviousConnectionState() + " -> " + current;
//        Log.i(mTAG, status);

        /* SleepBCI - BioEssence Integration */

        activityhand.postDelayed(new Runnable() {
            public void run() {
                SharedPreferences activityprefs = getSharedPreferences("activityPref", MODE_PRIVATE);
                SharedPreferences.Editor editormain = activityprefs.edit();
                editormain.putString("raweeg", String.valueOf(eegBuffer[1]) + "," + String.valueOf(eegBuffer[2]) + "," + String.valueOf(eegBuffer[3]) + "," + String.valueOf(eegBuffer[4]));
                editormain.putString("alpha", String.valueOf(alphaBuffer[1]) + "," + String.valueOf(alphaBuffer[2]) + "," + String.valueOf(alphaBuffer[3]) + "," + String.valueOf(alphaBuffer[4]));
                editormain.putString("beta", String.valueOf(betaBuffer[1]) + "," + String.valueOf(betaBuffer[2]) + "," + String.valueOf(betaBuffer[3]) + "," + String.valueOf(betaBuffer[4]));
                editormain.putString("gamma", String.valueOf(gammaBuffer[1]) + "," + String.valueOf(gammaBuffer[2]) + "," + String.valueOf(thetaBuffer[3]) + "," + String.valueOf(thetaBuffer[4]));
                editormain.putString("delta", String.valueOf(deltaBuffer[1]) + "," + String.valueOf(deltaBuffer[2]) + "," + String.valueOf(deltaBuffer[3]) + "," + String.valueOf(deltaBuffer[4]));
                editormain.putString("theta", String.valueOf(thetaBuffer[1]) + "," + String.valueOf(thetaBuffer[2]) + "," + String.valueOf(thetaBuffer[3]) + "," + String.valueOf(thetaBuffer[4]));
                editormain.putString("blink", String.valueOf(blinkBuffer));
                editormain.putString("clench", String.valueOf(jawBuffer));
                editormain.putString("fai", String.valueOf(faIndex));
                editormain.commit();
//                    Log.i(mTAG, "i");
                sendBroadcast(new Intent("eeg"));
                activityhand.postDelayed(this, 1);
            }
        }, 1);

        /* SleepBCI - BioEssence Integration */
        uiHandler.post(new Runnable()
        {
            @Override
            public void run() {

                final TextView statusText = (TextView) findViewById(R.id.con_status);
                statusText.setText(status);

                final MuseVersion museVersion = muse.getMuseVersion();
                // final TextView museVersionText = (TextView) findViewById(R.id.version);
                // If we haven't yet connected to the headband, the version information
                // will be null.  You have to connect to the headband before either the
                // MuseVersion or MuseConfiguration information is known.
                if (museVersion != null) {
                    final String version = museVersion.getFirmwareType() + " - "
                            + museVersion.getFirmwareVersion() + " - "
                            + museVersion.getProtocolVersion();
                }

                /* Visualizing relative power spectral bands
//                graphAcc.removeAllSeries();
//                graphWaves.addSeries(seriesRA);
//                graphWaves.addSeries(seriesRB);
//                graphWaves.addSeries(seriesRG);
//                graphWaves.addSeries(seriesRD);
//                graphWaves.addSeries(seriesRT);
//                graphAcc.addSeries(seriesAcc);
//                graphAcc.addSeries(seriesAccAverage);
*/
            }
        });

        if (current == ConnectionState.CONNECTED) {
            minutePrevious = getCurrentTimeStamp().split(":")[1];
            secondPrevious = (getCurrentTimeStamp().split(":")[2]).split("\\.")[0];

            refreshButton.setVisibility(View.GONE);
            connectButton.setVisibility(View.GONE);
//            editText.setVisibility(View.GONE);
            musesSpinner.setVisibility(View.GONE);
            spinnerLayout.setVisibility(View.GONE);

            secondStart = (getCurrentTimeStamp().split(":")[2]).split("\\.")[0];
            recording = true;
//            pauseButton.setEnabled(true);
            refreshButton.setEnabled(false);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
//            graphFrontal.addSeries(seriesFrontal);
            graphWaves.addSeries(seriesEEG);
            hypnoGram.addSeries(seriesStages);
            hypnoGram3.addSeries(seriesStages3);
//
//            //////////////////////////////Sleep Experiments: Update in both 30-second epochs//////////////////////////////////////////
            if (!remThread.isAlive()) {
                remThread.start();
                Log.i(mTAG, "REM-Thread Started");
            }
            ////////////////////////////////////////////////////////////////////////

            if (!fileThread.isAlive()) {
                fileThread.start();
            }
            if (rerun) {
                movingCount = 0;
                Log.i(mTAG, "new Thread Started");
                fileThread = new Thread(fileRun);
                fileThread.start();
            }

            Toast.makeText(getApplicationContext(), "Started Recording", Toast.LENGTH_SHORT).show();
            String columnHeadings =
                    "Time,"+
                            "Hours," +
                            "Minutes," +
                            "Seconds," +
                            "Milli," +

                            "EEG1,EEG2,EEG3,EEG4," +
                            "R1A,R1B,R1G,R1D,R1T," +
                            "R2A,R2B,R2G,R2D,R2T," +
                            "R3A,R3B,R3G,R3D,R3T," +
                            "R4A,R4B,R4G,R4D,R4T," +
                            /* Optimizing file-writing */
//                    "A1A,A1B,A1G,A1D,A1T," +
//                    "A2A,A2B,A2G,A2D,A2T," +
//                    "A3A,A3B,A3G,A3D,A3T," +
//                    "A4A,A4B,A4G,A4D,A4T," +
//                    "AccX,AccY,AccZ," +
                            "Acceleration," +
//                    "GyroX,GyroY,GyroZ," +
                            "Gyrometer," +
                            "JawClench," +
                            "Blink," +
                            "BlinkCount," +
                            "ClenchCount," +
                            "stage," +
                            "confidence," +
                            "surity," +
                            "is_good0,is_good1,is_good2,is_good3" +"\n";

            try {
                fos.write(columnHeadings.getBytes());
            }
            catch (Exception i) {
                i.printStackTrace();
            }
        }
        if (current == ConnectionState.DISCONNECTED) {

            //////////////////////// Special Attention to what needs to be refreshed /////////////////////
            refreshButton.setVisibility(View.VISIBLE);
            connectButton.setVisibility(View.VISIBLE);
//            editText.setVisibility(View.VISIBLE);
            musesSpinner.setVisibility(View.VISIBLE);
            spinnerLayout.setVisibility(View.VISIBLE);
            Log.i(mTAG, "Muse disconnected:" + muse.getName());
            // Save the data file once streaming has stopped.
            recording = false;
            filledBuffer=false;
            movingCount=0;
            ////////////////* To restart the inference */////////////////
            secondCount=0;
            checkEpochOnce=true;
            epochAble=false;
            stdWake=0;
            countStd=1;
            filePath=android.net.Uri.parse(file.toURI().toString());
            try {
                fileThread.sleep(1000);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            rerun=true;
            refreshButton.setEnabled(true);
            connectButton.setEnabled(true);
            pauseButton.setEnabled(false);

            /* Audio - Neural Feedback - AK */
//            calibrateButton.setEnabled(false);
//            calibrateButton.setVisibility(View.INVISIBLE);
//            calSwitch=true;
            Toast.makeText(getApplicationContext(), "Saved recording to file "+nameOfFile, Toast.LENGTH_SHORT).show();
//            try {
//                graph.getGridLabelRenderer().invalidate(true, true);
//                graph2.getGridLabelRenderer().invalidate(true, true);
//                graph3.getGridLabelRenderer().invalidate(true, true);
//            } catch (Exception i) {
//                i.printStackTrace();
//            }

            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
        }
    }

    /**
     *
     * Function for spectral Conditioned predictions
     * Using (Delta/100*Gamma) Ratio for Deep Sleep
     * and
     * (100*Alpha*Beta*Gamma/Delta Ratio for N1
     */

    private void predictSpectralConditionedSleepStages()
    {
        /////////////////////////////////////////////*Spectral Conditioning*///////////////////////////////////////////////

//    Log.i(mTAG, "D/G: " + String.valueOf(deepSleepD_G1) + " threshDeep: " + String.valueOf(deepSleepThreshold1) + " FinalThresh: " + String.valueOf(threshD1));
//    Log.i("mTAG","Length"+String.valueOf(gammaMuse1.size()));
//    Log.i("mTAG","Length"+String.valueOf(deltaMuse1.size()));
//    Log.i(mTAG, "D/G: " + String.valueOf(deepSleepD_G1) + " threshDeep: " + String.valueOf(deepSleepThreshold1));
//
//        if (deepSleepThreshold1 > 0.1) {
//            threshD1 = 4 * deepSleepThreshold1;
//        }
//        if (deepSleepThreshold1 >= 0.04 && deepSleepThreshold1 <= 0.1) {
//            threshD1 = 10 * deepSleepThreshold1;
//        }
//        if (deepSleepThreshold1 < 0.04) {
//            threshD1 = 100 * deepSleepThreshold1;
//        }
//          Log.i(mTAG, "D/G: " + String.valueOf(deepSleepD_G1) + " threshDeep: " + String.valueOf(deepSleepThreshold1) + " FinalThresh: " + String.valueOf(threshD1));
////                            n1SleepThreshold=n1Threshold();
//
//        if(deepSleepD_G1>threshD1)
//        {
//            deepSleep1=1; unsure1=0; // n1Sleep=0;
//            valueResult1 = "DeepStage: 3";
//            Log.i(mTAG,"DG "+String.valueOf(deepSleepD_G1)+" "+"Thresh: "+String.valueOf(deepSleepThreshold1)+" FinalThresh: "+String.valueOf(threshD1));
//            Log.i(mTAG, "Result.........");
//            Log.i(mTAG,valueResult1);
//            sleepView1="N-REM";
//            sleepStage1.setText(sleepView1);
//            seriesStages.appendData(new DataPoint(counterStage++, 0), true, 600);
//            seriesStages.appendData(new DataPoint(counterStage++, 0),true, 600);
//        }
//        else if(n1SleepABG_D>n1SleepThreshold)
//        {
//            deepSleep1=0;n1Sleep=1;unsure1=0;
//            Log.i(mTAG, "Result.........");
//            Log.i(mTAG,valueResult1);
//
////                  sleepView="N-REM";
//
//            sleepStage1.setText(sleepView1);
//            seriesStages.appendData(new DataPoint(counterStage++, 2), true, 600);
//            seriesStages.appendData(new DataPoint(counterStage++, 2),true, 600);
//            Log.i(mTAG," DSleep "+String.valueOf(deepSleepD_G1)+" n1Sleep "+String.valueOf(n1SleepABG_D));
//        }
    }

    /**
     **
     Function for Collecting EEG into Buffers of 30 seconds for prediction and conditioning
     **
     **/

    private void fillBuffersForPrediction()
    {
        epoch1.add(eegBuffer[1]); // Storing EEG Values: Af7

        //        epoch0.add(eegBuffer[0]); // Storing Af8 Values

        epoch3.add(eegBuffer[3]); // Storing tp10 Values

        if(spectralConditioning)
        {
            ////////////////////////////////* Filling Buffers for Spectral Conditioning *///////////////////////////
//                    if(alphaBuffer[1]==alphaBuffer[1]) // Will skip if NaN because Nan!=Nan
//                    {
//                        alphaMuse.add(alphaBuffer[1]);
//                    }
//                    if(betaBuffer[1]==betaBuffer[1])
//                    {
//                        betaMuse.add(betaBuffer[1]);
//                    }
//                    if(thetaBuffer[1]==thetaBuffer[1])
//                    {
//                        thetaMuse.add(thetaBuffer[1]);
//                    }
//                    if(deltaBuffer[1]==deltaBuffer[1])
//                    {
//                        deltaMuse1.add(deltaBuffer[1]);
//                    }
//                    if(gammaBuffer[1]==gammaBuffer[1])
//                    {
//                        gammaMuse1.add(gammaBuffer[1]);
//                    }
//                    if(deltaBuffer[0]==deltaBuffer[0])
//                    {
//                        deltaMuse0.add(deltaBuffer[1]);
//                    }
//                    if(gammaBuffer[0]==gammaBuffer[0])
//                    {
//                        gammaMuse0.add(gammaBuffer[0]);
//                    }
//                    if(deltaBuffer[2]==deltaBuffer[2])
//                    {
//                        deltaMuse2.add(deltaBuffer[2]);
//                    }
//                    if(gammaBuffer[2]==gammaBuffer[2])
//                    {
//                        gammaMuse2.add(gammaBuffer[2]);
//                    }
        }
    }

    /**
     * Renew All Buffers Used For Prediction in the last thirty seconds
     */
    private void renewBuffersForPrediction()
    {
        /* Clear all the values, refresh buffers */
        normalizedBrain1=new double[sampling30];
//        normalizedBrain2=new double[sampling30];
        normalizedBrain3=new double[sampling30];

        sumFAI=0;

        /* For Lead Af7 = 1 */
        epoch1.clear();
        eeg301.clear();

        /* For Lead Af8 = 2 */
//                    epoch2.clear();
//                    eeg302.clear();
//
        /* For Lead tp10 = 3 */
        epoch3.clear();
        eeg303.clear();

        /*Clearing Buffers used for Spectral Conditioning */
        if(spectralConditioning) {
//            deltaMuse1.clear();
//            deltaMuse2.clear();
//            deltaMuse0.clear();
//            gammaMuse0.clear();
//            gammaMuse1.clear();
//            gammaMuse2.clear();
//            alphaMuse1.clear();
//            betaMuse1.clear();
        }
    }

    /**
     * Predicting Sleep Stages with the filled 30 second buffers
     */
    private void predictSleepStage()
    {
        try
        {
            normalizedBrain1 = preprocessEEG(epoch1, fosLog1, fosLog1N);
            fosLog1.write("\n".getBytes());
            fosLog1N.write("\n".getBytes());



//          normalizedBrain2=preprocessEEG(epoch2);
//            fosLog2.write("\n".getBytes());
//            fosLog2N.write("\n".getBytes());



            normalizedBrain3 = preprocessEEG(epoch3, fosLog3, fosLog3N);
            fosLog3.write("\n".getBytes());
            fosLog3N.write("\n".getBytes());



////////////////////////////////////*Spectral Conditioning*///////////////////////////////////
//            Log.i(mTAG, "D/G1: "+String.valueOf(deepSleepD_G1)+" threshDeep: "+String.valueOf(deepSleepThreshold1)+" FinalThresh: "+String.valueOf(threshD1));
//            Log.i(mTAG, "D/G0: "+String.valueOf(deepSleepD_G0)+" threshDeep: "+String.valueOf(deepSleepThreshold1)+" FinalThresh: "+String.valueOf(threshD0));
//            Log.i(mTAG, "D/G2: "+String.valueOf(deepSleepD_G2)+" threshDeep: "+String.valueOf(deepSleepThreshold1)+" FinalThresh: "+String.valueOf(threshD2));
//            Log.i("mTAG","Length"+String.valueOf(gammaMuse1.size()));
//            Log.i("mTAG","Length"+String.valueOf(deltaMuse1.size()));
//            float stepDelta = deltaMuse1.size()/3000f;
//            float stepGamma = gammaMuse1.size()/3000f;
//            for (e = 0; e < deltaMuse.size(); e++) {
//                int idx = Math.round(stepDelta * e); // In order to obtain equally spaced samples
//                if (idx < deltaMuse.size()) {
//                    delta30.add(deltaMuse.get(idx));
////                                Log.i("EEG " + String.valueOf(e), epoch.get(idx).toString());
//                }
//            }
//
//            for (e = 0; e < gammaMuse.size(); e++) {
//                int idx = Math.round(stepGamma * e); // In order to obtain equally spaced samples
//                if (idx < gammaMuse.size()) {
//                    gamma30.add(gammaMuse.get(idx));
////                                Log.i("EEG " + String.valueOf(e), epoch.get(idx).toString());
//                }
//            }
//            Log.i("Ready for Prediction",String.valueOf(badBLE));
//            deepSleepD_G1=meanValue(deltaMuse1)/(100*meanValue(gammaMuse1)+0.0000000001);
//            n1SleepABG_D=meanValue(alphaMuse1)*meanValue(betaMuse1)*meanValue(gammaMuse1)/(10*meanValue(deltaMuse1)+0.0000000001);

//////////////////////////////////////TF Lite and Mobile//////////////////////////////////////////////////////////
            if (!badBLE)
            {
                Log.i(mTAG, "In for prediction");
                final List<Classifier.Recognition> results1 = classifier.recognizeSleep(normalizedBrain1); // normalizedBrain => without filter
                if (results1.size() > 0) {
                    entryConfidence1 = String.valueOf(results1.get(0).getConfidence());
                    entryStage1 = results1.get(0).getTitle();
                    valueResult1 = "Stage: " + entryStage1 + "  Confidence: " + String.valueOf(Math.round(Double.valueOf(entryConfidence1) * 1000.0) / 1000.0);
                    Log.i(mTAG, "Result.........");
                    Log.i(mTAG, valueResult1);
                }

//                final List<Classifier.Recognition> results2 = classifier.recognizeSleep(normalizedBrain2); // normalizedBrain => without filter
//                if (results2.size() > 0) {
//                    entryConfidence2=String.valueOf(results2.get(0).getConfidence());
//                    entryStage2=results2.get(0).getTitle();
//                    valueResult2 = "Stage: " + entryStage2+"  Confidence: "+String.valueOf(Math.round(Double.valueOf(entryConfidence2)*1000.0)/1000.0);
//                    Log.i(mTAG, "Result.........");
//                    Log.i(mTAG,valueResult2);
//                }
//
                final List<Classifier.Recognition> results3 = classifier.recognizeSleep(normalizedBrain3); // normalizedBrain => without filter
                if (results3.size() > 0)
                {
                    entryConfidence3=String.valueOf(results3.get(0).getConfidence());
                    entryStage3=results3.get(0).getTitle();
                    valueResult3 = "Stage: " + entryStage3+"  Confidence: "+String.valueOf(Math.round(Double.valueOf(entryConfidence3)*1000.0)/1000.0);
                    Log.i(mTAG, "Result.........");
                    Log.i(mTAG,valueResult3);
                }



///////////////////////////* SleepBCI - BioEssence Integration */////////////////////////
                /**
                 * BroadCasting to ControllerActivity
                 */

//                SharedPreferences eegprefs = getSharedPreferences("eegsleepPref", MODE_PRIVATE);
//                SharedPreferences.Editor editor = eegprefs.edit();
//                editor.putString("entryStage", entryStage);
//                editor.putString("entryConfidence", String.valueOf(Math.round(Double.valueOf(entryConfidence)*1000.0)/1000.0));
//                editor.commit();
//                Log.i(mTAG,"Sending Broadcast");
//                sendBroadcast(new Intent("sleep"));
//////////////////////////////* SleepBCI - BioEssence Integration *////////////////////////////

            }
            else
            {
                valueResult1 = "Noisy";
                badBLE = false;
            }

/////////////////////////////////* GraphView Updates *//////////////////////////////////////////////////

            graphWaves.removeAllSeries();
            seriesEEG = new LineGraphSeries<DataPoint>();
            seriesEEG.setColor(Color.BLUE);
            counter = 0;
            graphWaves.addSeries(seriesEEG);

//          graphFrontal.removeAllSeries();
//          seriesFrontal=new LineGraphSeries<>();
//          seriesFrontal.setColor(Color.BLUE);
//          graphFrontal.addSeries(seriesFrontal);

            Log.i(mTAG, "CountStd" + String.valueOf(countStd));
            if (countStd < 3) // Goes from 1 to 2 in 1 minute
            {
                sleepView1 = "Calibrating";
                sleepView2 = "Calibrating";
                sleepView3 = "Calibrating";

                sleepStage1.setText(sleepView1);
                sleepStage3.setText(sleepView3);

///////////////////* Deep Sleep Threshold returns WakeState mean D/G over 30 seconds for 1 minute calibration */////////////
//              deepSleepThreshold1 = deepThreshold(deepSleepD_G1);
            }
            else
            {
                if(spectralConditioning)
                {
                    predictSpectralConditionedSleepStages();
                }
                else
                {
                    /* Af7 Electrode: Update UI */
                    if (Double.valueOf(entryConfidence1) > 0.5) {
                        //                                unsure1 = 0;
                        valueResult1 = "Stage: " + entryStage1 + "     Confidence: " + String.valueOf(Math.round(Double.valueOf(entryConfidence1) * 1000.0) / 1000.0);

                        if (Integer.valueOf(entryStage1) == 4)
                        {
                            sleepView1 = "REM";
                            sleepStage1.setText(sleepView1);
                            remBell = true;
                            //                                Log.i(TAG, "REMMing");
                            seriesStages.appendData(new DataPoint(counterStage++, 3), true, 600); // Set Max datapoints according to sleep hours
                            seriesStages.appendData(new DataPoint(counterStage++, 3), true, 600); // Set Max datapoints according to sleep hours
                        }
                        else if (Integer.valueOf(entryStage1) == 0)
                        {
                            sleepView1 = "Wake";
                            sleepStage1.setText(sleepView1);

                            seriesStages.appendData(new DataPoint(counterStage++, 4), true, 600);
                            seriesStages.appendData(new DataPoint(counterStage++, 4), true, 600);
                        }
                        else
                        {
                            sleepView1 = "N-REM";
                            sleepStage1.setText(sleepView1);
                            seriesStages.appendData(new DataPoint(counterStage++, 3 - Integer.valueOf(entryStage1)), true, 600);
                            seriesStages.appendData(new DataPoint(counterStage++, 3 - Integer.valueOf(entryStage1)), true, 600);
                        }
                    }
                    else
                    {
                        //                                unsure1 = 1;
                        valueResult1 = "Stage: " + entryStage1 + "   Confidence: " + String.valueOf(Math.round(Double.valueOf(entryConfidence1) * 1000.0) / 1000.0);
                        sleepView1 = "Unsure";
                        Log.i(mTAG, "Stage: " + entryStage1);
                        sleepStage1.setText(sleepView1);
                    }



                    /* Tp10 Electrode: Update UI */
                    if (Double.valueOf(entryConfidence3) > 0.5) {
                        //                                unsure1 = 0;
                        valueResult3 = "Stage: " + entryStage3 + "     Confidence: " + String.valueOf(Math.round(Double.valueOf(entryConfidence3) * 1000.0) / 1000.0);

                        if (Integer.valueOf(entryStage3) == 4)
                        {
                            sleepView3 = "REM";
                            sleepStage3.setText(sleepView3);
                            remBell = true;
                            //                                Log.i(TAG, "REMMing");

                            /* Only one hypnogram and EEG graph */
                            seriesStages3.appendData(new DataPoint(counterStage3++, 3), true, 600); // Set Max datapoints according to sleep hours
                            seriesStages3.appendData(new DataPoint(counterStage3++, 3), true, 600); // Set Max datapoints according to sleep hours
                        }
                        else if (Integer.valueOf(entryStage3) == 0)
                        {
                            sleepView3 = "Wake";
                            sleepStage3.setText(sleepView3);

                            /* Only one hypnogram and EEG graph */
                            seriesStages3.appendData(new DataPoint(counterStage3++, 4), true, 600);
                            seriesStages3.appendData(new DataPoint(counterStage3++, 4), true, 600);
                        }
                        else
                        {
                            sleepView3 = "N-REM";
                            sleepStage3.setText(sleepView3);
                            /* Only one hypnogram and EEG graph */
                            seriesStages3.appendData(new DataPoint(counterStage3++, 3 - Integer.valueOf(entryStage3)), true, 600);
                            seriesStages3.appendData(new DataPoint(counterStage3++, 3 - Integer.valueOf(entryStage3)), true, 600);
                        }
                    }
                    else
                    {
                        //                                unsure1 = 1;
                        valueResult3 = "Stage: " + entryStage3 + "   Confidence: " + String.valueOf(Math.round(Double.valueOf(entryConfidence3) * 1000.0) / 1000.0);
                        sleepView3 = "Unsure";
                        Log.i(mTAG, "Stage: " + entryStage3);
                        sleepStage3.setText(sleepView3);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        renewBuffersForPrediction();
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     *
     * @param p    The data packet containing the data from the headband (eg. EEG data)
     * @param muse The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse)
    {
        /////////////EEG Collection///////////
        String secondCurrent = (getCurrentTimeStamp().split(":")[2]).split("\\.")[0];
        if(!secondPrevious.equals(secondCurrent)){
            secondPrevious=secondCurrent;
            secondCount++;
        }

/////////////////////* Audio-Neural Feedback - AK *////////////////////
//        if(secondCount>10 && calSwitch)
//        {
//            calAble=true;
//            calSwitch=false;
//        }
///////////////////////////////////////////////////////////////////////
        if(secondCount>30)
        {
            if(checkEpochOnce) {
//                Log.i(mTAG, "secondStart :" + secondStart + " secondCurrent" + secondCurrent);
                epochAble = true;
                checkEpochOnce=false;
            }
        }
        if(epochAble)
        {
            if (Integer.valueOf(secondCurrent) >= 0 && Integer.valueOf(secondCurrent) < 30)
            {
                if (sphincter1)
                {
                    String eegF="";String eeg="";
                    Log.i(mTAG, "EEG Values of 0-30 seconds ");

                    predictSleepStage();

                    second30=1;
                    sphincter1 = false;
                    sphincter2 = true;
                }
                else
                {
                    second30=0;
                    fillBuffersForPrediction();
                    /*Rules of interest can also be hardcoded here*/ // if(relT<0.7 && relG>0.2) { sW=(short)(sW+2000);} else{  sN3++; sN4++; }
                }
            }
            else
            {
                if (sphincter2)
                {
                    Log.i(mTAG, "EEG Values of 30-60 seconds ");

                    predictSleepStage();
                    sphincter2 = false;
                    sphincter1 = true;
                }
                else
                {
                    second30=0;
                    fillBuffersForPrediction();
                }
            }
        }
        //////////////////////////////////////
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert (eegBuffer.length >= n);
                getEegChannelValues(eegBuffer, p);
                eegStale = true;
                break;
            case ACCELEROMETER:
                assert (accelBuffer.length >= n);
                getAccelValues(p);
                accelStale = true;
                break;

            case GYRO:
                assert (gyroBuffer.length >= n);
                getGyroValues(p);
                gyroStale = true;
                break;

            case ALPHA_RELATIVE:
                assert (alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer, p);
                alphaStale = true;
                break;

            case BETA_RELATIVE:
                assert (betaBuffer.length >= n);
                getEegChannelValues(betaBuffer, p);
                betaStale = true;
                break;

            case GAMMA_RELATIVE:
                assert (gammaBuffer.length >= n);
                getEegChannelValues(gammaBuffer, p);
                gammaStale = true;
                break;

            case DELTA_RELATIVE:
                assert (deltaBuffer.length >= n);
                getEegChannelValues(deltaBuffer, p);
                deltaStale = true;
                break;

            case THETA_RELATIVE:
                assert (thetaBuffer.length >= n);
                getEegChannelValues(thetaBuffer, p);
                thetaStale = true;
                break;

            case ALPHA_ABSOLUTE:
                assert (alphaABuffer.length >= n);
                getEegChannelValues(alphaABuffer, p);
                alphaAStale = true;
                break;

            case BETA_ABSOLUTE:
                assert (betaABuffer.length >= n);
                getEegChannelValues(betaABuffer, p);
                betaAStale = true;
                break;

            case GAMMA_ABSOLUTE:
                assert (gammaABuffer.length >= n);
                getEegChannelValues(gammaABuffer, p);
                gammaAStale = true;
                break;

            case DELTA_ABSOLUTE:
                assert (deltaABuffer.length >= n);
                getEegChannelValues(deltaABuffer, p);
                deltaAStale = true;
                break;

            case THETA_ABSOLUTE:
                assert (thetaABuffer.length >= n);
                getEegChannelValues(thetaABuffer, p);
                thetaAStale = true;
                break;


            case BATTERY:
                assert (batteryBuffer.length >= n);
                getBatteryValues(batteryBuffer,p);
                batteryStale=true;
                break;

            case IS_GOOD:
                assert(isgoodBuffer.length>=n);
                getIsGoodValues(isgoodBuffer,p);
                isgoodStale=true;
                //isgood.setText("1");
                break;

            case DRL_REF:
            case QUANTIZATION:

            default:
                break;
        }
    }
    /*
     * *
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket artifactPacket, final Muse muse) {
        getArtifactValues(artifactPacket);
        artifactStale = true;
    }

    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     * <p>
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */

    private void getBatteryValues(double[] batteryBuffer,MuseDataPacket p)
    {
        batteryBuffer[0]=p.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
        batteryBuffer[1]=p.getBatteryValue(Battery.MILLIVOLTS);
        batteryBuffer[2]=p.getBatteryValue(Battery.TEMPERATURE_CELSIUS);
    }

    private void getIsGoodValues(double[] isgoodBuffer,MuseDataPacket p)
    {
        isgoodBuffer[0]=p.getEegChannelValue(Eeg.EEG1);
        isgoodBuffer[1]=p.getEegChannelValue(Eeg.EEG2);
        isgoodBuffer[2]=p.getEegChannelValue(Eeg.EEG3);
        isgoodBuffer[3]=p.getEegChannelValue(Eeg.EEG4);
    }

    private void getEegChannelValues(double[] buffer, MuseDataPacket p)
    {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    private void getArtifactValues(MuseArtifactPacket artifactPacket) {
        jawBuffer = false;
        blinkBuffer = false;
        if (artifactPacket.getJawClench()) {
            jawBuffer = true;
        }
        if (artifactPacket.getBlink()) {
            blinkBuffer = true;
        }
    }

    private void getAccelValues(MuseDataPacket p) {
        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    }

    private void getGyroValues(MuseDataPacket p) {
        gyroBuffer[0] = p.getGyroValue(Gyro.X);
        gyroBuffer[1] = p.getGyroValue(Gyro.Y);
        gyroBuffer[2] = p.getGyroValue(Gyro.Z);
    }


    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */

    private void initUI() {

        setContentView(R.layout.activity_main);

//////////////////////////* SleepBCI - AK */////////////////////////////
        /**
         * UI For Sleep Predictions
         */
//        detectButton = findViewById(R.id.buttonDetect);
//        detectButton.setOnClickListener(this);

        /* Audio-Neural Feedback */
//        togglewaveButton = findViewById(R.id.toggleWave);
//        togglewaveButton.setOnClickListener(MainActivityEssence.this);
//        togglewaveButton.setVisibility(View.VISIBLE);

        listButton=findViewById(R.id.listButton);
        scoreFAI = findViewById(R.id.scoreFAI);
        listButton.setOnClickListener(MainActivity.this);
        numberPicker=findViewById(R.id.npAlarm);
        numberPicker.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int numberEdit;
                Log.i(mTAG,"Seq "+charSequence.toString());
                try
                {
                    numberEdit=Integer.parseInt(charSequence.toString().trim());
                    if(numberEdit<=20 && numberEdit>0) {
                        sleepAlarmT = 1000 * numberEdit;
//                        Log.i(mTAG, "sleepAlarm " + String.valueOf(sleepAlarmT));
                        if(charSequence.toString().contains("\n"))
                        {
                            numberPicker.clearFocus();
                        }
                        Toast.makeText(getApplicationContext(), String.valueOf(numberEdit)+" second/s alarm set", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Choose only numbers between 0 to 20", Toast.LENGTH_SHORT).show();
                    }
                }
                catch(Exception e)
                {
                    Toast.makeText(getApplicationContext(), "Choose only numbers between 0 to 20", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {
            }
        });


        spinnerLayout=(LinearLayout)findViewById(R.id.spinnerLayout);
        connectionLayout=(LinearLayout)findViewById(R.id.connectionLayout);
        accgyroLayout=(LinearLayout)findViewById(R.id.layoutAccGyro);
        accgyroLayout.setVisibility(View.GONE);
        faiLayout=(LinearLayout)findViewById(R.id.layoutFAI);
        faiLayout.setVisibility(View.GONE);
        eegLayout=(LinearLayout)findViewById(R.id.layoutEEG);
        eegLayout.setVisibility(View.GONE);
        relwavesLayout=(LinearLayout)findViewById(R.id.layoutRelWaves);
        relwavesLayout.setVisibility(View.GONE);


        viz1Button=findViewById(R.id.buttonViz1);
        viz1Button.setOnClickListener(this);

        ///////////////////////////////////////////
        eegButton=findViewById(R.id.buttonEEG);
        eegButton.setOnClickListener(this);
        accgyroButton=findViewById(R.id.buttonAccelGyro);
        accgyroButton.setOnClickListener(this);
        relwaveButton=findViewById(R.id.buttonRelWaves);
        relwaveButton.setOnClickListener(this);
        faiButton=findViewById(R.id.buttonFAI);
        faiButton.setOnClickListener(this);


//        stage1 = findViewById(R.id.stage1);
//        sleepStage1=findViewById(R.id.sleepStage1);

//        stage2 = findViewById(R.id.stage2);
//        sleepStage2=findViewById(R.id.sleepStage2);

        stage3 = findViewById(R.id.stage3);
        sleepStage3=findViewById(R.id.sleepStage3);

        refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);

        connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);

        disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);

        pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);
        pauseButton.setEnabled(false);

        uploadButton= (Button) findViewById(R.id.upload);
        uploadButton.setOnClickListener(this);


        stdEEG=(TextView)findViewById(R.id.stdEEG);

        battery = (TextView)findViewById(R.id.battery);

        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
        acc_x = (TextView) findViewById(R.id.acc_x);
        acc_y = (TextView) findViewById(R.id.acc_y);
        acc_z = (TextView) findViewById(R.id.acc_z);
        gyro_x = (TextView) findViewById(R.id.gyro_x);
        gyro_y = (TextView) findViewById(R.id.gyro_y);
        gyro_z = (TextView) findViewById(R.id.gyro_z);

        elem1 = (TextView) findViewById(R.id.elem1);
        elem2 = (TextView) findViewById(R.id.elem2);
        elem3 = (TextView) findViewById(R.id.elem3);
        elem4 = (TextView) findViewById(R.id.elem4);

        tp9 = (TextView) findViewById(R.id.eeg_tp9);
        fp1 = (TextView) findViewById(R.id.eeg_af7);
        fp2 = (TextView) findViewById(R.id.eeg_af8);
        tp10 = (TextView) findViewById(R.id.eeg_tp10);
        jaw = findViewById(R.id.clench);
        blink = findViewById(R.id.blink);


        /* Extra UI Features */

//        clearButton = findViewById(R.id.buttonClear);
//        clearButton.setOnClickListener(this);

//        viz2Button=findViewById(R.id.buttonViz2);
//        viz2Button.setOnClickListener(this);


//        eegButton=findViewById(R.id.buttonEEG);
//        eegButton.setOnClickListener(this);
//        editText = findViewById(R.id.editText);
//        stage0 = findViewById(R.id.stage0);
//        sleepStage0=findViewById(R.id.sleepStage0);

        /////////////////////* Audio-Neural Feedback - SleepBCI - AK *////////////////////
//        calInst = (TextView)findViewById(R.id.calInst);
//        brainT = (TextView)findViewById(R.id.brainThresh);
//        calmT = (TextView)findViewById(R.id.calmThresh);

//        playButton=(Button) findViewById(R.id.play);
//        playButton.setOnClickListener(this);
//        playButton.setVisibility(View.INVISIBLE);

//        calibrateButton=(Button) findViewById(R.id.calib);
//        calibrateButton.setOnClickListener(this);
//        calibrateButton.setEnabled(false);
//        calibrateButton.setVisibility(View.INVISIBLE);
//
//        seekbar=(SeekBar)findViewById(R.id.seekBar);
//        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
//        {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                sleepT=(int)(10+(progress*4.9));
//                sampleCount=(int)(100+progress*499);
//            }
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//        helpButton = (Button) findViewById((R.id.help));
//        helpButton.setOnClickListener(this);

    }

    /**
     * The runnable that is used to update the UI at 60Hz.
     * <p>
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */


    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
//          Looper.prepare();
//          Log.i(mTAG,"UI Running");
            try {
                uiThread.sleep(1000/60);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            if(recording)
            {
                /* Af7 Electrode */
                if(isgoodBuffer[1]==0)
                {
                    sleepStage1.setText("BadContact");
                }
                else
                {
                    sleepStage1.setText(sleepView1);
                }

                /* Tp10 Electrode */
                if(isgoodBuffer[3]==0)
                {
                    sleepStage3.setText("BadContact");
                }
                else
                {
                    sleepStage3.setText(sleepView3);
                }



/////////////////////* Audio-Neural Feedback - AK *////////////////////
/*              ////////////////Calibration///////////////
                if (calAble) {
                    calibrateButton.setEnabled(true);
                    calibrateButton.setVisibility(View.VISIBLE);
                    calInst.setText("Play Now?");
                    calAble = false;
                }
                if (calibrationDone) {
                    calibrateButton.setVisibility(View.VISIBLE);
                    calInst.setText("Play Again?");
                    calibrationDone = false;
                    playButton.setVisibility(View.INVISIBLE);
                    togglewaveButton.setVisibility(View.VISIBLE);
                }
                String FAIndex = "Sum: " + String.valueOf(sumFAI);
                scoreFAI.setText(FAIndex);

                String calmScore = "Calm Score: " + String.valueOf(max_calm_thresh);
                calmT.setText(calmScore);
                String creativeScore = "Creative Score: " + String.valueOf(max_brain_thresh);
                brainT.setText(creativeScore);*/
//////////////////////////* Audio-Neural Feedback - AK *////////////////////


                stage1.setText(valueResult1);// TF Lite: Af7
                stage3.setText(valueResult3); // TF Lite: TP10

                /* Graphs for visualization */

                counter = counter + 1;
                /* Raw EEG */
                if (vizEEG) {
                    seriesEEG.appendData(new DataPoint(counter, eegBuffer[3]), true, 1000);
                }



                // Frontal Asymmetry Index values
                double fratio = alphaBuffer[2] / alphaBuffer[1];
                //            Log.i(TAG," fratio: right/left "+String.valueOf(fratio));
                if (fratio > 0) {
                    faIndex = Math.log(fratio);
                } else {
                    //                Log.i(TAG,"LoggingIndex 0");
                    faIndex = 0;
                }
                sumFAI += faIndex;

                /* Frontal Asymmetry Index and Visualizing Relative Spectral Band and Accelerations */
//                if (vizFAI) {
//                    seriesFrontal.appendData(new DataPoint(counter, faIndex), true, 10000);
//                }
//                //            seriesRB.appendData(dpb, true, 20000);
//                //            seriesRG.appendData(dpg, true, 20000);
//                //            seriesRD.appendData(dpd, true, 20000);
//                //            seriesRT.appendData(dpt, true, 20000);
//                //
//                //            //seriesAvgRel.appendData(new DataPoint(counter,aveD),true,20000);
//                //            seriesAcc.appendData(dpacc, true,20000);
//                //            seriesAccAverage.appendData(dpaccave, true, 20000);
//

            }
            if (eegStale) {
                updateEeg();
            }
            if (accelStale) {
                updateAccel();
            }
            if (gyroStale) {
                updateGyro();
            }
            if (alphaStale || betaStale || gammaStale || deltaStale || thetaStale) {
                updateRelative();
            }

            if (artifactStale) {
                updateArtifact();
            }
            if (batteryStale) {
                updateBattery();
            }
            uiHandler.postDelayed(tickUi, 1000 / 100);
            //            Looper.loop();
        }
    };

    private Thread uiThread = new Thread(tickUi);

    /**
     * The following methods update the TextViews in the UI with the data
     * from the buffers.
     */
    private void updateAccel() {
        acc_x.setText(String.format("%6.2f", accelBuffer[0]));
        acc_y.setText(String.format("%6.2f", accelBuffer[1]));
        acc_z.setText(String.format("%6.2f", accelBuffer[2]));
    }

    private void updateGyro() {
        gyro_x.setText(String.format("%6.2f", gyroBuffer[0]));
        gyro_y.setText(String.format("%6.2f", gyroBuffer[1]));
        gyro_z.setText(String.format("%6.2f", gyroBuffer[2]));
    }

    private void updateEeg() {
        tp9.setText(String.format("%6.3f", eegBuffer[0]));
        if(isgoodBuffer[0]==1)
        {
            tp9.setBackgroundColor(Color.parseColor("#cbcbcb"));
        }
        else
        {
            tp9.setBackgroundColor(Color.WHITE);
        }
        fp1.setText(String.format("%6.3f", eegBuffer[1]));
        if(isgoodBuffer[1]==1)
        {
            fp1.setBackgroundColor(Color.parseColor("#cbcbcb"));
        }
        else
        {
            fp1.setBackgroundColor(Color.WHITE);
        }
        fp2.setText(String.format("%6.3f", eegBuffer[2]));
        if(isgoodBuffer[2]==1)
        {
            fp2.setBackgroundColor(Color.parseColor("#cbcbcb"));
        }
        else
        {
            fp2.setBackgroundColor(Color.WHITE);
        }
        tp10.setText(String.format("%6.3f", eegBuffer[3]));
        if(isgoodBuffer[3]==1)
        {
            tp10.setBackgroundColor(Color.parseColor("#cbcbcb"));
        }
        else
        {
            tp10.setBackgroundColor(Color.WHITE);
        }
    }

    private void updateBattery(){
        battery.setText(String.format("Charge percent: %2.2f",batteryBuffer[0]));
    }

    private void updateRelative() {
        elem1.setText(String.format("%6.3f", alphaBuffer[0]));
        elem2.setText(String.format("%6.3f", alphaBuffer[1]));
        elem3.setText(String.format("%6.3f", alphaBuffer[2]));
        elem4.setText(String.format("%6.3f", alphaBuffer[3]));
    }

    private void updateArtifact() {
        if (jawBuffer) {
            jaw.setText("Clenching");
            jawBuffer = false;
        } else {
            jaw.setText("No clench");
        }
        if (blinkBuffer) {
            blink.setText("Blinking");
            blinkBuffer = false;
        } else {
            blink.setText("No blinks");
        }
    }

    /* Timestamping */
    public static String getCurrentTimeStamp() {

        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
    }



    /* Converting NaN to zero */
    public String noNaN(String x){
        if (x.equals("   NaN,   NaN,   NaN,   NaN,   NaN")) {
            x = "0.0,0.0,0.0,0.0,0.0";
        }
        return x;
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged()
        {
            Log.i(mTAG,"Muse Listener working fine");
            activityRef.get().museListChanged();
        }
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef)
        {
            this.activityRef = activityRef;
            Log.i(mTAG,"Muse connectionListener initialized");
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
            Log.i(mTAG,"Muse connectionListener getting packets");
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
            Log.i(mTAG,"Muse dataListener initialized");
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket artifactPacket, final Muse muse){
            activityRef.get().receiveMuseArtifactPacket(artifactPacket, muse);
        }
    }

    /* Async Uploading */

    class Upload extends AsyncTask<String, Void, String>
    {
        boolean uploadFailure=false;
        protected void onPreExecute()
        {

        }

        @Override
        protected String doInBackground(String... arg0) {
            if (filePath != null)
            {
//                final ProgressDialog progressDialog = new ProgressDialog(getApplicationContext());
//                progressDialog.setTitle("Uploading");
//                progressDialog.show();
                try (java.io.InputStream in = new java.io.FileInputStream(file)){
                    FileMetadata metadata = client.files().uploadBuilder("/"+ /*nameOfFile +*/"testWake.csv").uploadAndFinish(in);
                }
                catch(Exception e)
                {
                    uploadFailure=true;
                    e.printStackTrace();
                }
            }
            //if there is not any file
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            if(uploadFailure)
            {
                Toast.makeText(getApplicationContext(), "File Uploading Error", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "File Uploaded", Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //////////////////////////////////* Muse *////////////////////////////////////////
        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Log.i(mTAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity = new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);
        // Register a listener to receive notifications of what Muse headbands
        // we can connect to.
        manager.setMuseListener(new MuseL(weakActivity));

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.

        ensurePermissions();

        Log.i(mTAG,"..............OnCreate............");

        /////////////////////////* File Saving Module */////////////////////////////

        datafiles = getExternalFilesDir(Environment.getDataDirectory().getAbsolutePath());

        /////////////////////////* TensorFlow Lite *////////////////////////////////
        initTensorFlowAndLoadModel();
        //////////////////////////////////////////////////////////////////////////

        /* Init Signal Processing Filter */

        butterworthBP= new Butterworth();
        butterworthBP.bandPass(1, 100,23,44); // 1 to 45 Hz

        butterworthBS= new Butterworth();
        butterworthBS.bandStop(1,100,60,10); // Not practical, against Nquist Theorem: as 60 hz > (SamplingRate)/2

        butterworthH = new Butterworth();
        butterworthH.highPass(1, 100, 1); // HighPass

        butterworthL = new Butterworth();
        butterworthL.lowPass(1, 100, 45); // LowPass


        ///////////////////////////////// Dropbox ////////////////////////////////////////
        DbxRequestConfig config = DbxRequestConfig.newBuilder("/"+nameOfFile+".csv").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);
        //////////////////////////////////////////////////////////////////////////////////

        // Load and initialize our UI.
        initUI();

        ////////////////////////////////////// Visualization Module ////////////////////////////////////////////

        /////////////////////////////////////* GraphView */////////////////////////////////////////
        // For relative values graph
        // private LineGraphSeries<DataPoint> seriesAA, seriesAB, seriesAG, seriesAD, seriesAT;

        graphWaves = findViewById(R.id.graph);
        // hypnoGram = findViewById(R.id.hypnogram);
        hypnoGram3 = findViewById(R.id.hypnogram3);



        /* Initializing EEG Visuals and Hypnogram Af7,Tp10 */
        seriesEEG=createEEGVisuals(graphWaves);
        // seriesStages=createHypnogram(hypnoGram,1);
        seriesStages3=createHypnogram(hypnoGram3,3);

        ///////////////////////* Media Audio Adaptation */////////////////////////////
        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silence); // Dog Barking Sound/silence
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // Do something. For example: playButton.setEnabled(true);
                playable = true;
                Log.i(mTAG, "MediaPlayer Prepared");
            }
        });


        // Start our asynchronous updates of the UI.
        uiHandler.post(tickUi);

        activityhand = new Handler(); //Integration

    }

    private LineGraphSeries<DataPoint> createEEGVisuals(GraphView graphWaves)
    {
        GridLabelRenderer glr; //, glrAcc;
        glr = graphWaves.getGridLabelRenderer();
//        glr.setLabelFormatter(new DefaultLabelFormatter() {
//            @Override
//            public String formatLabel(double v, boolean isValX) {
//                if (isValX)
//                {
//                        return currentTime;
//                } else {
//                    return super.formatLabel(v, isValX);
//                }
//
//            }
//        });
//      glr.setHumanRounding(false);

        //        glr.setVerticalAxisTitle("EEG");

        glr.setNumHorizontalLabels(0);
        glr.setHorizontalLabelsColor(Color.TRANSPARENT);
        glr.setPadding(75);
        Viewport viewport; //   , viewportAcc;
        viewport = graphWaves.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(false);
        viewport.setMinX(0);
        viewport.setMaxX(200); // approximate visual datapoints for 30 seconds
//        viewport.setMinY(700);
//        viewport.setMaxY(1000);
        viewport.setScalable(true); // enables horizontal zooming and scrolling
        viewport.setScalableY(true);
        viewport.setScrollable(false);
        viewport.setScrollableY(false);
        LineGraphSeries<DataPoint>seriesEEG=new LineGraphSeries<DataPoint>();
        seriesEEG.setColor(Color.BLUE);
        return seriesEEG;
    }

    private LineGraphSeries<DataPoint> createHypnogram(GraphView hypnoGram, int electrode)
    {
        Viewport viewport; //   , viewportAcc;
        viewport = hypnoGram.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(100); // approximate visual datapoints for 30 seconds
        viewport.setMinY(0);
        viewport.setMaxY(4);
        viewport.setScrollable(true);
        viewport.setScalable(false);
        viewport.setScalableY(false);
        viewport.setScrollableY(false);
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(hypnoGram);
        String[] sleepLabels=new String[5];
        sleepLabels[0]="N-REM3";
        sleepLabels[1]="N-REM2";
        sleepLabels[2]="N-REM1";
        sleepLabels[3]="REM";
        sleepLabels[4]="Wake";
        staticLabelsFormatter.setVerticalLabels(sleepLabels);
        viewport.setMaxYAxisSize(4);
        GridLabelRenderer glrHyp; //, glrAcc;
        glrHyp = hypnoGram.getGridLabelRenderer();
        glrHyp.setHorizontalLabelsColor(Color.GRAY);
        glrHyp.setVerticalLabelsColor(Color.GRAY);
        glrHyp.setNumVerticalLabels(5);
        staticLabelsFormatter.setViewport(viewport);
        glrHyp.setLabelFormatter(staticLabelsFormatter);

        String eegElectrode="";
        if(electrode==0) {
            eegElectrode="Tp9";
        }
        else if(electrode==1){
            eegElectrode="Af7";
        }
        else if(electrode==2){
            eegElectrode="Af8";
        }
        else if(electrode==3)
        {
            eegElectrode="Tp10";
        }

        glrHyp.setVerticalAxisTitle("Stages of Sleep - "+eegElectrode);
        glrHyp.setVerticalAxisTitleColor(Color.GRAY);
        glrHyp.setNumHorizontalLabels(0);
        glrHyp.setHorizontalLabelsColor(Color.BLACK);

        LineGraphSeries<DataPoint> seriesStages=new LineGraphSeries<DataPoint>();
        seriesStages.setColor(Color.BLUE);
        return seriesStages;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        manager.stopListening();
        Log.d(mTAG, "onPause");
        super.onPause();
    }
}
