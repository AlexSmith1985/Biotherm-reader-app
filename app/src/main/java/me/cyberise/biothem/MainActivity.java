package me.cyberise.biothem;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    RelativeLayout mainPanel;
    boolean inSettings = false;
    static float offset = new Float(0.5);
    Random random = new Random();
    HashMap<SVG.Path, Float> paths = new HashMap<SVG.Path, Float>();
    BluetoothConnection connection = null;
    private void walk(SVG.SvgObject root){
        if(root instanceof SVG.SvgConditionalContainer){
            for(SVG.SvgObject child: ((SVG.SvgConditionalContainer)root).children){
                walk(child);
            }
        }
        if(root instanceof SVG.Path){
            SVG.Path path = (SVG.Path)root;
            if(!paths.containsKey(path)){
                float speed = random.nextFloat();
                if (random.nextBoolean()){
                    speed = speed*-1;
                }
                paths.put(path, speed);
            }


        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothConnection.ui = this;

        mainPanel = (RelativeLayout)findViewById(R.id.mainpanel);


        final LinearLayout background = (LinearLayout)findViewById(R.id.background);
        final SVGImageView svgImageView = new SVGImageView(this);

        try {
            final SVG svg = SVG.getFromAsset(svgImageView.getContext().getAssets(), "logo.svg");
            final SVG.Svg rootElement = svg.getRootElement();


            walk(rootElement.getChildren().get(0));
            int period = 100;
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask()
            {
                public void run()
                {

                    if(connection == null || !connection.connected){
                        return;
                    }
                    for(SVG.Path path : paths.keySet()){
                        path.transform.postRotate(paths.get(path));
                    }
                    //path.transform.postRotate(offset);

                    //offset = (offset+1)%360;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            svgImageView.setSVG(svg);
                            background.removeView(svgImageView);
                            background.addView(svgImageView, new LinearLayout.LayoutParams(AbsoluteLayout.LayoutParams.MATCH_PARENT, AbsoluteLayout.LayoutParams.MATCH_PARENT));
                        }
                    });
                }
            }, 1, period);





            svgImageView.setSVG(svg);
        } catch (SVGParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        background.addView(svgImageView, new LinearLayout.LayoutParams(AbsoluteLayout.LayoutParams.MATCH_PARENT, AbsoluteLayout.LayoutParams.MATCH_PARENT));



        connection = BluetoothConnection.getConnection();


    }



    public void Exit(String message){
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        try {
            Thread.sleep(4000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public void onBackPressed() {
        if (inSettings) {
            backFromSettingsFragment();
            return;
        }
        super.onBackPressed();
    }
    private void backFromSettingsFragment() {
        inSettings = false;
        getFragmentManager().popBackStack();
        mainPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            mainPanel.setVisibility(View.GONE);
            inSettings = true;
            FragmentManager mFragmentManager = getFragmentManager();
            FragmentTransaction mFragmentTransaction = mFragmentManager.beginTransaction();
            PrefsFragment mPrefsFragment = new PrefsFragment();
            mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
            mFragmentTransaction.addToBackStack("settings");
            mFragmentTransaction.commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

