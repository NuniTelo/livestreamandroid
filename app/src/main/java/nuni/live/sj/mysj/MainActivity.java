package nuni.live.sj.mysj;

import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements IVLCVout.Callback {
    public final static String TAG = "MainActivity";
    private String mFilePath;
    private SurfaceView mSurface;
    private SurfaceHolder holder;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;

    public int mHeight;
    public int mWidth;


    private ImageView takePic, takeVideo, stopVideo;
    private WebView webView;
    private TextView rec;
    private Switch picOrVid;
    private boolean success = false;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mHeight = displayMetrics.heightPixels;
        mWidth = displayMetrics.widthPixels;


        //landscape
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //do te jete path i video qe do te nise
        mFilePath = FilterArgs.VIDEOURL;
        Log.d(TAG, "Playing: " + mFilePath);
        rec = (TextView) findViewById(R.id.recording);
        webView = (WebView) findViewById(R.id.web);
        webView.getSettings().setJavaScriptEnabled(true);
        //butonat
        takePic = (ImageView) findViewById(R.id.takePic);
        takeVideo = (ImageView) findViewById(R.id.takeVideo);
        stopVideo = (ImageView) findViewById(R.id.stopVideo);
        picOrVid = (Switch)findViewById(R.id.mode);
        picOrVid.setChecked(false);
        Toast.makeText(MainActivity.this,"Video mode",Toast.LENGTH_SHORT).show();

        picOrVid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                   webView.loadUrl(FilterArgs.PICMODE);
                    takeVideo.setEnabled(false);
                    stopVideo.setEnabled(false);
                    takePic.setEnabled(true);
                    Toast.makeText(MainActivity.this,"Picture mode",Toast.LENGTH_SHORT).show();
                }else{
                    webView.loadUrl(FilterArgs.VIDEOMODE);
                    takePic.setEnabled(false);
                    takeVideo.setEnabled(true);
                    stopVideo.setEnabled(true);
                    Toast.makeText(MainActivity.this,"Video Mode",Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopVideo.setEnabled(false);

        //surface
        mSurface = (SurfaceView) findViewById(R.id.surface);
        holder = mSurface.getHolder();
        holder.setFormat(PixelFormat.TRANSLUCENT);

        //onclick

        takeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec.setVisibility(View.VISIBLE);
                stopVideo.setEnabled(true);
                takeVideo.setEnabled(false);
                Toast.makeText(MainActivity.this, "Video Recording Started!" , Toast.LENGTH_LONG).show();
                webView.loadUrl(FilterArgs.STARTVIDEO);
                releasePlayer();
                createPlayer(mFilePath);

            }
        });

        stopVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rec.setVisibility(View.GONE);
                takeVideo.setEnabled(true);
                stopVideo.setEnabled(false);
                Toast.makeText(MainActivity.this, "Video Recording Stopped!" , Toast.LENGTH_LONG).show();
                webView.loadUrl(FilterArgs.STOPVIDEO);
                releasePlayer();
                createPlayer(mFilePath);
            }
        });

        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Picture Captured!" , Toast.LENGTH_LONG).show();
                webView.loadUrl(FilterArgs.PICCAPTURE);
                createPlayer(mFilePath);
            }
        });


    }


    @Override
    protected void onResume() {
        super.onResume();
        createPlayer(mFilePath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void createPlayer(String media) {
        releasePlayer();
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
//                toast.show();
            }

            // Create LibVLC
            ArrayList<String> options = new ArrayList<String>();
            //options.add("--subsdec-encoding <encoding>");
            options.add("--aout=opensles");
            options.add("--audio-time-stretch"); // time stretching
            options.add("-vvv"); // verbosity
            libvlc = new LibVLC(this, options);
            holder.setKeepScreenOn(true);
            holder.setFixedSize(mWidth, mHeight);
            // Creating media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);

            // Seting up video output
            final IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setVideoView(mSurface);
            vout.setWindowSize(mWidth,mHeight);
            vout.addCallback(this);
            vout.attachViews();
            Media m = new Media(libvlc, Uri.parse(media));
            mMediaPlayer.setMedia(m);
            mMediaPlayer.play();

        } catch (Exception e) {
            Toast.makeText(this, "Error in creating player!", Toast
                    .LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();
        holder = null;
        libvlc.release();
        libvlc = null;

      //  mVideoWidth = 0;
       // mVideoHeight = 0;
    }

    private MediaPlayer.EventListener mPlayerListener = new MyPlayerListener(this);

    @Override
    public void onSurfacesCreated(IVLCVout vout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vout) {

    }

    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.e(TAG, "Error with hardware acceleration");
        this.releasePlayer();
        Toast.makeText(this, "Error with hardware acceleration", Toast.LENGTH_LONG).show();
    }

    private static class MyPlayerListener implements MediaPlayer.EventListener {
        private WeakReference<MainActivity> mOwner;

        public MyPlayerListener(MainActivity owner) {
            mOwner = new WeakReference<MainActivity>(owner);
        }

        @Override
        public void onEvent(MediaPlayer.Event event) {
            MainActivity player = mOwner.get();

            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    Log.d(TAG, "MediaPlayerEndReached");
                    player.releasePlayer();
                    break;
                case MediaPlayer.Event.Playing:
                case MediaPlayer.Event.Paused:
                case MediaPlayer.Event.Stopped:
                default:
                    break;
            }
        }
    }
}

