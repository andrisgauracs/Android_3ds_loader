package com.example.andrisgauracs.loader3ds;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.app.Dialog;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {

    private MyGLSurfaceView mGLView;
    private MyGLRenderer mRenderer;
    private SeekBar scaleBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mGLView = (MyGLSurfaceView) findViewById(R.id.glView);


        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLView.setEGLContextClientVersion(2);

            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            // Set the renderer for the GLSurfaceView
            mRenderer = new MyGLRenderer(this);
            mGLView.setRenderer(mRenderer, displayMetrics.density);

        } else {
            // Show error message, if the device is not OpenGL ES 2.0 compatible
            Toast.makeText(this, "OpenGL ES 2.0 is not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

        //Create a seek bar for scaling
        scaleBar = (SeekBar) findViewById(R.id.seekbar1);

        if (scaleBar != null) {
            //Set the bar's event listener, which will update the scale factor everytime, the value is changed
            scaleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int progressChanged = 0;

                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                    progressChanged = progress;
                    //Turn the integer value to a percentage value
                    mRenderer.changeScale(progressChanged/100.0f);
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                    // TODO Auto-generated method stub
                }
            });
        }
    }

    //Inflate the top menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /**
             * There are 5 models loaded in the MyGLRenderer class.
             * @param names[] holds the list of the objects loaded. They are written in the sequence, they're loaded
             */
            case R.id.change_object:
                // custom dialog
                final Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.object_list);
                dialog.setTitle("Choose object");
                String names[] ={"Crate","Car","Fighter Jet","Character","Mobile phone"};

                ListView lv = (ListView) dialog.findViewById(R.id.listView1);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,names);
                lv.setAdapter(adapter);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //By changing the "position" value, we can load a new model
                        mRenderer.setCurrentObject(position);
                        scaleBar.setProgress(0);
                        dialog.dismiss();
                    }
                });
                dialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
    }
}
