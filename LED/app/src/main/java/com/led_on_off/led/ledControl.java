package com.led_on_off.led;

import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import yuku.ambilwarna.AmbilWarnaDialog;


public class ledControl extends ActionBarActivity {

   // Button btnOn, btnOff, btnDis;
    ImageButton On, Off, Discnt, Abt;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    public BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    SQLiteDatabase db;
    AndroidDatabaseManager dbHandler;
    TimePicker simpleTimePicker;
    Button start;
    Button stop;
    Button picker;
    private InputStream mmInStream = null;
    byte[] buffer = new byte[1024];
    int bytes;
    public int color;
    NumberPicker np;
    int ramptime;
    boolean started = false;

//    private short numSamples = 20;
//    private short samplingDelay = 50;
//    private short pulseDelay = 500;
//    private short rampTime = 5000; // ms
//    private char brightness = 255;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        System.out.println("beginning");
        super.onCreate(savedInstanceState);
        dbHandler = new AndroidDatabaseManager(this);
        db = dbHandler.getReadableDatabase();
        dbHandler.delete(db);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);

        simpleTimePicker = (TimePicker)findViewById(R.id.simpleTimePicker);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        picker = (Button) findViewById(R.id.picker);

        np = (NumberPicker) findViewById(R.id.numberPicker);

        np.setMinValue(1);
        np.setMaxValue(30);


        //call the widgets
//        On = (ImageButton)findViewById(R.id.on);
//        Off = (ImageButton)findViewById(R.id.off);
//        Discnt = (ImageButton)findViewById(R.id.discnt);
//        Abt = (ImageButton)findViewById(R.id.abt);

        new ConnectBT().execute(); //Call the class to connect

        BluetoothReceiver task = new BluetoothReceiver();
        task.execute();

        System.out.println("middle");


        //commands to be sent to bluetooth
//        On.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                turnOnLed();      //method to turn on
//            }
//        });
//
//        Off.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v)
//            {
//                turnOffLed();   //method to turn off
//            }
//        });
//
//        Discnt.setOnClickListener(new View.OnClickListener()
//        {
//            @Override
//            public void onClick(View v)
//            {
//                Disconnect(); //close connection
//            }
//        });

//        simpleTimePicker.setIs24HourView(true);

        Time time = new Time(getApplicationContext());
        String hourmin = time.getTime();
        if (hourmin != "") {
            List<String> timing = Arrays.asList(hourmin.split(","));
            int hour = Integer.parseInt(timing.get(0));
            int minute = Integer.parseInt(timing.get(1));
            simpleTimePicker.setCurrentHour(hour);
            simpleTimePicker.setCurrentMinute(minute);
        }


        simpleTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                Time time = new Time(getApplicationContext());
                time.setTime(hourOfDay, minute);
            }
        });

        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener(){
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                ramptime = numberPicker.getValue();
            }
        });

        start.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int val = 4;
                byte b = (byte) val;
                try {
                    btSocket.getOutputStream().write(b);
                    Log.d("read", "sending " + b);
                } catch (IOException e) {
                    msg("Error");
                }
                started = true;
//                try {
//                    Log.d("read", "hi");
//                    InputStream is = btSocket.getInputStream();
//                    ByteBuffer message = ByteBuffer.allocate(8);
//                    message.put((byte) 4);
//
////                    message.put((byte) 1);
////                    message.putShort((short) 10);
////                    message.putShort((short) 100);
////                    message.putShort((short) 1000);
////                    message.put((byte) 3);
////                    Log.d("read", message.toString());
//                    btSocket.getOutputStream().write(message.array());
//                    btSocket.getOutputStream().flush();
//                    Log.d("read", "done");

//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        });

        stop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int val = 3;
                byte b = (byte) val;
                try {
                    btSocket.getOutputStream().write(b);
                    Log.d("read", "sending " + b);
                } catch (IOException e) {
                    msg("Error");
                }

            }
        });

        picker.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openColorPicker();
            }
        });

//        try {
//            mmInStream = btSocket.getInputStream();
//        } catch (IOException e) {
//        }
//        while (true) {
//            try {
//                int bytes = mmInStream.read(buffer); //read bytes from input buffer
//                String readMessage = new String(buffer, 0, bytes);
//                if (readMessage.length() > 0 && isTime()) {
//                    int val = 2;
//                    byte b = (byte) val;
//                    try {
//                        btSocket.getOutputStream().write(b);
//                    } catch (IOException e) {
//                        msg("Error");
//                    }
//                }
//
//            } catch (IOException e) {
//                break;
//            }
//        }


    }

    public void openColorPicker() {
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, 0xffff8800, new AmbilWarnaDialog.OnAmbilWarnaListener() {

            @Override
            public void onOk(AmbilWarnaDialog dialog, int colors) {
                // color is the color selected by the user.
                color = colors;

//                int colorLong = (int)Long.parseLong(String.valueOf(color), 16);

                int rval = (color >> 16) & 0x00FF;
                int gval = (color >> 8) & 0x00FF;
                int bval = (color >> 0) & 0x00FF;

                System.out.println(String.format("0x%08X", color));

                Log.d("read", "sending " + color);
//                Log.d("read", "sending " + colorLong);
                Log.d("read", "sending " + rval);
                Log.d("read", "sending " + gval);
                Log.d("read", "sending " + bval);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // cancel was selected by the user
            }
        });

        dialog.show();
    }

    public Boolean isTime() {
        if (!started) return false;
        Time time = new Time(getApplicationContext());
        String hourmin = time.getTime();
        Log.d("elapsed", time.toString());
        Log.d("elapsed", hourmin);

        List<String> timing = Arrays.asList(hourmin.split(","));
        int hour = Integer.parseInt(timing.get(0));
        int minute = Integer.parseInt(timing.get(1));
        Log.d("elapsed", Integer.toString(hour));
        Log.d("elapsed", Integer.toString(minute));

        Date currentTime = Calendar.getInstance().getTime();
        Log.d("elapsed", currentTime.toString());


        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss aa");
//        sdf.setNumberFormat();
        String formattedTime = sdf.format(currentTime);
        Log.d("elapsed", sdf.getNumberFormat().toString());

        DateFormat readFormat = new SimpleDateFormat( "hh:mm:ss aa");
        DateFormat writeFormat = new SimpleDateFormat( "HH:mm:ss");
        Date date = null;
        String formattedDate = null;
        try {
            date = readFormat.parse(formattedTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (date != null) {
            formattedDate = writeFormat.format(date);
        }

        Log.d("elapsednew", formattedDate);

        Date date1 = null;
        Date date2 = null;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss");
        try {
            date1 = simpleDateFormat.parse(hour + ":" + minute + ":00");
            date2 = simpleDateFormat.parse(formattedDate);
        } catch (ParseException e) {

        }

//        String formattedDate = null;
//        DateFormat readFormat = new SimpleDateFormat("hh:mm:ss aa");
//        DateFormat writeFormat = new SimpleDateFormat("HH:mm:ss");
//        Date date = null;
//        try {
//            date = readFormat.parse(String.valueOf(date2));
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        if (date != null) {
//            formattedDate = writeFormat.format(date);
//        }
//
        Log.d("elapsed!!", date1.toString());
        Log.d("elapsed!!", date2.toString());



        long different = date1.getTime() - date2.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        Log.d("elapsed hours", Long.toString(elapsedHours));
        Log.d("elapsed hours", Long.toString(elapsedMinutes));
        Log.d("elapsed hours", Long.toString(elapsedSeconds));
        Log.d("elapsed hours", date1.toString());
        Log.d("elapsed hours", date2.toString());



        if (elapsedHours == 0 && elapsedMinutes <= 0 && elapsedSeconds < 30) {
            return true;
        }

        return false;
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                int val = 0;
                byte b = (byte) val;
                btSocket.getOutputStream().write(b);
                Log.d("read", "sending " + b);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void turnOnLed()
    {
        if (btSocket!=null)
        {
            try
            {
                int val = 30;
                byte b = (byte) val;
                btSocket.getOutputStream().write(b);
                Log.d("read", "sending " + b);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // 1 - start
    // 2 - wake
    // 3 - stop


    // gets 1 (not a character) - light sleep
    // light sleep and within frame of time = send 2

//    private void sendStartPacket() {
//        ByteBuffer pump_on_buf =
//        pump_on_buf.putShort(numSamples);
//        pump_on_buf.putShort(samplingDelay);
//        pump_on_buf.putInt(pulseDelay);
//
//        private short numSamples = 20;
//        private short samplingDelay = 50;
//        private short pulseDelay = 500;
//    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

//    public  void about(View v)
//    {
//        if(v.getId() == R.id.abt)
//        {
//            Intent i = new Intent(this, AboutActivity.class);
//            startActivity(i);
//        }
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                 btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    public class BluetoothReceiver extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                InputStream is = btSocket.getInputStream();

                // start packet (1, number of samples, sampling delay, pulse delay, thres)

//                int val = 1;
//                byte b = (byte) val;
//                btSocket.getOutputStream().write(b);
//                btSocket.getOutputStream().flush();
//
//                int parameter1 = 10; // # of samples
//                byte parameter = (byte) parameter1;
//                btSocket.getOutputStream().write(parameter);
//                btSocket.getOutputStream().flush();
//
//                int parameter2 = 100; // # of samples
//                parameter = (byte) parameter2;
//                btSocket.getOutputStream().write(parameter);
//                btSocket.getOutputStream().flush();
//
//                int parameter3 = 1000; // # of samples
//                parameter = (byte) parameter3;
//                btSocket.getOutputStream().write(parameter);
//                btSocket.getOutputStream().flush();
//
//                int parameter4 = 3; // # of samples
//                parameter = (byte) parameter4;
//                btSocket.getOutputStream().write(parameter);
//                btSocket.getOutputStream().flush();

                byte[] buffer = new byte[25];
                while (true) {
                    int read = is.read(buffer);
                    while(read != -1){
                        publishProgress(read);
                        read = is.read(buffer);
                        Log.d("read", Integer.toString(read));
                        if (read == 1) {
                            Log.d("debug", "here in read");
                            if (isTime()) {
                                Log.d("debug", "here in time");
//                                int val = 5;
//                                byte b = (byte) val;
                                try {

                                    // wake up packet (2, rval, gval, bval, ramptime)

//                                    ByteBuffer wakemessage = ByteBuffer.allocate(6);


                                    //int colorLong = (int)Long.parseLong(String.valueOf(color), 16);

                                    System.out.println(String.format("0x%08X", color));
                                    //System.out.println(String.format("0x%08X", colorLong));

                                    int rval = (color >> 16) & 0xFF;
                                    int gval = (color >> 8) & 0xFF;
                                    int bval = (color >> 0) & 0xFF;

//                                    wakemessage.put((byte) 2);
//                                    wakemessage.put((byte) rval); // # of samples
//                                    wakemessage.put((byte) gval); // sampling delay
//                                    wakemessage.put((byte) bval); // pulse delay
//                                    wakemessage.putShort((short) (ramptime*1000)); // threshold
//                                    btSocket.getOutputStream().write(wakemessage.array());
//                                    btSocket.getOutputStream().flush();

                                    Log.d("read", "sending " + color);
//                                    Log.d("read", "sending " + colorLong);
                                    Log.d("read", "sending " + rval);
                                    Log.d("read", "sending " + gval);
                                    Log.d("read", "sending " + bval);

                                    Log.d("read", "sending " + ramptime);

                                    int parameter;

                                    int param1 = 2; // # of samples
                                    parameter = (byte) param1;
                                    btSocket.getOutputStream().write(parameter);
                                    btSocket.getOutputStream().flush();

                                    parameter = (byte) rval;
                                    btSocket.getOutputStream().write(rval);
                                    btSocket.getOutputStream().flush();

                                    parameter = (byte) gval;
                                    btSocket.getOutputStream().write(parameter);
                                    btSocket.getOutputStream().flush();

                                    parameter = (byte) bval;
                                    btSocket.getOutputStream().write(parameter);
                                    btSocket.getOutputStream().flush();

                                    btSocket.getOutputStream().write((byte) (((short) (ramptime*1000)) & 0x0F));
                                    btSocket.getOutputStream().write((byte) (((short) (ramptime*1000))>> 8));
                                    btSocket.getOutputStream().flush();

//                                    btSocket.getOutputStream().write(b);
//                                    Log.d("read", "sending " + b);
                                } catch (IOException e) {
                                }
                                read = 50;
                            }
                        }
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

    }
}