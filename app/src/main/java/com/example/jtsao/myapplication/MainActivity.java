package com.example.jtsao.myapplication;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    static String TAG = "Main Activity";

    private Button cpuButton,memButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "App is Running yay!!");

        // Vars.
        //List<String> resultList = new ArrayList<>();

        // Define UI Elements Here.
        cpuButton = (Button) findViewById(R.id.viewCPU);
        memButton = (Button) findViewById(R.id.viewMem);
        ListView lv = (ListView) findViewById(R.id.listView);


        final List<String> resultList = new ArrayList<>();

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,                 // Context for the activity.
                R.layout.item_layout, // Layout to use ( Create )
                resultList);             // Items to be displayed

        // Thread Handles Here
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String string = bundle.getString("cpu");
                resultList.add(string);
                adapter.notifyDataSetChanged();
                // Update ListView Here.
                Log.i(TAG, "CPU : " + string);
            }
        };

        final Handler memHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();

                String string = bundle.getString("mem");
                resultList.add(string);
                adapter.notifyDataSetChanged();
                // Update ListView Here.
                Log.i(TAG, "Mem : " + string);
            }
        };

        // Set Adapter
        lv.setAdapter(adapter);

        // Set up Button Listeners Here.
        cpuButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "View CPU button clicked.");
                // TODO Auto-generated method stub

                // New Thread Created
                Thread cpuThread = new Thread(){

                    volatile boolean isRunning = true; // Boolean to stop thread.

                    // New Runnable Created
                    public void run() {
                        List<String> movingAvg = new ArrayList<>();

                        // Need to add logic to break out of thread here.
                        while (isRunning) {
                            Log.i(TAG, "CPU Thread Is Running");
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();

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
                                movingAvg.add(Float.toString(cpuUsage));
                            }
                            // Sum all values and get result.
                            for (int i = 0; i < movingAvg.size(); i++) {
                                weight = ((float) (i + 1) / weightedTotal);
                                result += Float.parseFloat(movingAvg.get(i)) * weight;
                            }

                            // Pop first value off.
                            movingAvg.remove(0);

                            String cpu = Float.toString(result * 100);

                            bundle.putString("cpu", cpu);
                            msg.setData(bundle);
                            handler.sendMessage(msg);

                        }
                        Log.i(TAG, "CPU Thread Stopped");
                    }

                    public void setRunning(boolean running){
                        this.isRunning = running;
                    }
                };

                cpuThread.start();
            }
        });

        memButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i(TAG, "View Mem button clicked.");
                // TODO Auto-generated method stub


                // New Thread Created
                // New Runnable Created
                new Thread(new Runnable() {
                    public void run() {

                        // Need to add logic to break out of thread here.
                        while (!Thread.currentThread().isInterrupted()) {
                            Message msg = handler.obtainMessage();
                            Bundle bundle = new Bundle();

                            Double test = readMemUsage();
                            String Memory = Double.toString(test);

                            bundle.putString("mem", Memory);
                            msg.setData(bundle);
                            memHandler.sendMessage(msg);

                            Log.i(TAG, "Free Memory (kB)= " + Memory);
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                Log.i(TAG, "Free Memory (kB)= " + Memory);
                            }
                        }

                    }
                }).start();
            }
        });

    }
    /*
    Function to grab CPU usage from /proc/stat

    Output: double freeMemory  | Mem Usage

     */
    private double readMemUsage() {
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


    /*
    Function to grab CPU usage from /proc/stat

    Input: int sampleSize | Amount of samples to take before returning.
           int sampleTime | Time between samples.

    Output: float result  | Calculated time.

     */
    private float readCPUUsage(int sampleSize, int samepleTime) {
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
                    Thread.sleep(samepleTime);
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

