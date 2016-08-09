package com.example.jtsao.myapplication;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;

import java.util.List;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity {
    static String TAG = "Main Activity";

    // Number of fragment pages
    static final int NUM_ITEMS = 3;
    MyAdapter mAdapter;
    ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_pager);
        Log.i(TAG, "App is Running yay!!");

        // Create action bar
        final ActionBar actionBar = getActionBar();

        //Create Adapter for Fragment
        mAdapter = new MyAdapter(getSupportFragmentManager());

        // Set Adapter to the pager
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

    }

    @Override
    protected void onResume(){
        super.onResume();

        cancelNotification(111);
    }

    @Override
    protected void onPause(){
        super.onPause();

        makeNotification();

    }

    /**
     * Custom class extends FragmentPagerAdapter
     *
     **/
    public static class MyAdapter extends FragmentPagerAdapter{
        public MyAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position){
            switch (position){
                case 0:
                    return "System Overview";
                case 1:
                    return "CPU Usage";
                case 2:
                    return "Memory Usage";
            }

            return null;
        }

        @Override
        public int getCount(){
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position){
            return ArrayListFragment.newInstance(position);
        }
    }

    public static class ArrayListFragment extends ListFragment {
        int mNum;

        static ArrayListFragment newInstance(int num){
            ArrayListFragment f = new ArrayListFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * When creating, retrieve this instance's number from its arguments.
         *
         */
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        /**
         * The Fragment's UI is just a simple text view showing its
         * instance number.
         *
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v;

            if (mNum == 0){
                Log.i(TAG,"Loading System Overview Fragment");

                TextView brandText,modelText,osText,sdkText,manufacturerText,archText,nameText;

                v = inflater.inflate(R.layout.fragment_systemoverview, container, false);

                // initialize text views and assign proper text.
                brandText = (TextView) v.findViewById(R.id.brand_text);
                modelText = (TextView) v.findViewById(R.id.model_text);
                archText = (TextView) v.findViewById(R.id.arch_text);
                nameText = (TextView) v.findViewById(R.id.name_text);
                osText = (TextView) v.findViewById(R.id.os_text);
                sdkText = (TextView) v.findViewById(R.id.SDK_text);
                manufacturerText = (TextView) v.findViewById(R.id.manufacturer_text);

                // Device Name
                Log.i(TAG, "Brand : " + String.valueOf(Build.BRAND));
                brandText.setText("Brand : " + String.valueOf(Build.BRAND));

                Log.i(TAG, "Model : " + String.valueOf(Build.MODEL));
                modelText.setText("Model : " + String.valueOf(Build.MODEL));

                // OS arch
                Log.i(TAG, "OS Arch : " + System.getProperty("os.Arch"));
                archText.setText("OS Arch : " + String.valueOf(Build.DISPLAY));

                // OS name
                Log.i(TAG, "OS Name : " + System.getProperty("os.Name"));
                nameText.setText("OS Name : " + String.valueOf(Build.ID));

                // OS version
                Log.i(TAG, "OS Version : " + System.getProperty("os.version"));
                osText.setText("OS Version : " + System.getProperty("os.version"));

                // API level
                Log.i(TAG, "SDK Version : " + String.valueOf(android.os.Build.VERSION.SDK_INT));
                sdkText.setText("SDK Version : " + String.valueOf(android.os.Build.VERSION.SDK_INT));

                // Device
                Log.i(TAG, "Manufacturer : " + String.valueOf(Build.MANUFACTURER));
                manufacturerText.setText("Manufacturer : " + String.valueOf(Build.MANUFACTURER));

            } else if (mNum == 1){
                Log.i(TAG,"Loading CPU Usage Fragment");

                // Load View.
                v = inflater.inflate(R.layout.fragment_cpuusage, container, false);

                // Initialize Chart
                final LineChart mChart = createChart(v.findViewById(R.id.chart), 100f);

                // Create Thread to pass data to chart.
                new Thread(new Runnable() {

                    boolean isRunning = true;

                    @Override
                    public void run() {
                        List<Float> movingAvg = new ArrayList<>();

                        while (isRunning){

                            // Moving Average...
                            float movingFrameSize = 7;
                            float result = 0;
                            float weight;
                            float cpuUsage;
                            float weightedTotal = ((movingFrameSize * (movingFrameSize + 1)) / 2);

                            // Fill up array first//add on to last spot..
                            while (movingAvg.size() < movingFrameSize) {
                                do {
                                    cpuUsage = readCPUUsage(5, 200);
                                } while (Float.isNaN(cpuUsage) || cpuUsage == 0);
                                movingAvg.add(cpuUsage);
                            }
                            // Sum all values and get result.
                            for (int i = 0; i < movingAvg.size(); i++) {
                                weight = ((float) (i + 1) / weightedTotal);
                                result += movingAvg.get(i) * weight;
                            }

                            // Pop first value off.
                            movingAvg.remove(0);

                            final float cpu = result * 100;

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addEntry(mChart,cpu, "CPU Usage");
                                }
                            });

                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }

                }).start();

            } else if (mNum == 2) {
                Log.i(TAG, "Loading Fragment 2");

                v = inflater.inflate(R.layout.fragment_memoryusage, container, false);

                float totalMem = findTotalMemory();

                final LineChart mChart = createChart(v.findViewById(R.id.chart), totalMem);

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        while (true){

                            final float memoryUsage = (float) readMemUsage();

                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addEntry(mChart,memoryUsage, "Memory Usage");
                                }
                            });

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }).start();

            } else {
                Log.i(TAG, "Somehow got here... shouldnt get here");

                v = inflater.inflate(R.layout.fragment_pager_list, container, false);
            }

            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            Log.i(TAG,"Fragment Activity Created.");
        }

    }

    /**
     * Create LineChart
     *
     * Input: View to draw chart to..
     *        Float Y max of chart
     *
     * Returns: LineChart
     *
     */
    private static LineChart createChart(android.view.View v, float ymax ){
        LineChart chart = (LineChart) v;
            chart.setDescription("");
            chart.setNoDataTextDescription("No Data Right Meow");
            chart.setHighlightEnabled(true);
            chart.setTouchEnabled(false);
            chart.setDragEnabled(true);
            chart.setScaleEnabled(true);
            chart.setDrawGridBackground(true);
            chart.setPinchZoom(true);
            chart.setBackgroundColor(Color.LTGRAY);

        // Create empty data set and assign to mChart
        LineData data = new LineData();

        chart.setData(data);

        Legend l = chart.getLegend();
            l.setForm(Legend.LegendForm.LINE);
            l.setTextColor(Color.WHITE);

        XAxis xl = chart.getXAxis();
            xl.setTextColor(Color.WHITE);
            xl.setDrawGridLines(false);
            xl.setAvoidFirstLastClipping(true);

        YAxis yl = chart.getAxisLeft();
            yl.setTextColor(Color.WHITE);
            yl.setAxisMaxValue(ymax);
            yl.setDrawGridLines(true);

        YAxis yl2 = chart.getAxisRight();
            yl2.setEnabled(false);

        return chart;
    }

    /**
     * Add Entry to Chart
     *
     */
    private static void addEntry(LineChart mChart, float value, String setName){
        LineData data = mChart.getData();

        if (data != null) {
            LineDataSet set = data.getDataSetByIndex(0);

            if (set == null){
                set = createSet(setName);
                data.addDataSet(set);
            }
            data.addXValue("");
            data.addEntry(
                    new Entry(value, set.getEntryCount()),0
            );

            // notify chart data has changed
            mChart.notifyDataSetChanged();

            // limit number of visible entries
            mChart.setVisibleXRange(6);

            // scroll to latest entry
            mChart.moveViewToX(data.getXValCount()-7);

        }
    }

    /**
     * Function to create LineDataSet based on name.
     *
     * Returns: LineDataSet type
     *
     */
    private static LineDataSet createSet(String dataName){

        LineDataSet set = new LineDataSet(null, dataName);
            set.setDrawCubic(true);
            set.setCubicIntensity(0.2f);
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
            set.setColor(ColorTemplate.getHoloBlue());
            set.setCircleColor(ColorTemplate.getHoloBlue());
            set.setLineWidth(2f);
            set.setCircleSize(4f);
            set.setFillAlpha(65);
            set.setFillColor(ColorTemplate.getHoloBlue());
            set.setHighLightColor(Color.rgb(244,117,177));
            set.setValueTextColor(Color.BLUE);
            set.setValueTextSize(10f);

        return set;
    }

    /**
     * Code that generates the notification on top bar
     *
     */
    protected void makeNotification(){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.mudkip_icon)
            .setContentTitle(getResources().getString(R.string.notification_title))
            .setContentText(getResources().getString(R.string.notification_text));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // mId allows you to update the notification later on.
        mNotificationManager.notify(111, mBuilder.build());
    }

    public void cancelNotification(int notifyId) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notifyId);
    }

    private static float findTotalMemory(){
        RandomAccessFile reader = null;
        String load;
        String[] toks;
        double totalMemory = 0;

        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            load = reader.readLine();

            toks = load.split(" +");
            totalMemory = Double.parseDouble(toks[1]);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return (float)totalMemory;
    }

    /**
    Function to grab CPU usage from /proc/stat

    Output: double freeMemory  | Mem Usage

     */
    private static double readMemUsage() {
        try {
            // vars.
            String load;
            String[] toks;
            double freeMemory;

            // Creates a reader on /proc/meminfo
            RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");

            // Reads first line of /proc/meminfo
            /*
                MemTotal:        8167848 kB
                MemFree:         1409696 kB
                Buffers:          961452 kB
                Cached:          2347236 kB
                SwapCached:            0 kB
             */

            reader.seek(0);
            reader.readLine();
            load = reader.readLine();
            toks = load.split(" +");
            freeMemory = Double.parseDouble(toks[1]);

            return freeMemory;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return 0;
    }


    /**
    Function to grab CPU usage from /proc/stat

    Input: int sampleSize | Amount of samples to take before returning.
           int sampleTime | Time between samples.

    Output: float result  | Calculated time.

     */
    private static float readCPUUsage(int sampleSize, int sampleTime) {
        try {
            // vars.
            long idle;
            long cpu;
            String load;
            String[] toks;
            float total = 0;
            float result = 0 ;
            long[] totalCPU = new long[sampleSize];
            long[] totalIdle = new long[sampleSize];

            // Creates a reader on /proc/stat
            // Need to fix this.. cant constantly create new object.
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");

            // Reads first line of /proc/stat
            /*
                1st column : user = normal processes executing in user mode
                2nd column : nice = niced processes executing in user mode
                3rd column : system = processes executing in kernel mode
                4th column : idle = twiddling thumbs
                5th column : iowait = waiting for I/O to complete
                6th column : irq = servicing interrupts
                7th column : softirq = servicing softirqs
             */

            for(int i=0; i<sampleSize; i++){
                reader.seek(0);
                load = reader.readLine();
                toks = load.split(" +");

                // Parse out idle and cpu values.
                idle = Long.parseLong(toks[4]);
                cpu = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
                // Save values to arrays.
                totalIdle[i] = idle;
                totalCPU[i] = cpu;

                // Short sleep before polling again..
                try {
                    Thread.sleep(sampleTime);
                } catch (Exception e) {}
            }

            for(int j=1; j<=sampleSize-1; j++){
                total = ((float)(totalCPU[j] - totalCPU[j-1])) / ((float)(totalCPU[j] + totalIdle[j]) - (totalCPU[j-1] + totalIdle[j-1]));
                result += total;
            }

            // logic here to calculate result from values stored in both arrays..

            result = result / (sampleSize-1);

            return result;

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

}

