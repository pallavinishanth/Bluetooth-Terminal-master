package me.aflak.bluetoothterminal;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import me.aflak.bluetooth.Bluetooth;

public class Chat extends AppCompatActivity implements Bluetooth.CommunicationCallback {
    private String name;
    private Bluetooth b;
    private EditText message;
    private Button send;
    private TextView text;
    private ScrollView scrollView;
    private boolean registered=false;

    private static final int READ_REQUEST_CODE = 42;

    private final String TAG = "Chat";

    List<String> finalHex = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView)findViewById(R.id.text);
        message = (EditText)findViewById(R.id.message);
        send = (Button)findViewById(R.id.send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        text.setMovementMethod(new ScrollingMovementMethod());
        send.setEnabled(false);

        b = new Bluetooth(this);
        b.enableBluetooth();

        b.setCommunicationCallback(this);

        int pos = getIntent().getExtras().getInt("pos");
        name = b.getPairedDevices().get(pos).getName();

        Display("Connecting...");
        b.connectToDevice(b.getPairedDevices().get(pos));

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = message.getText().toString();
                message.setText("");
                b.send(msg);
                Display("You: "+msg);
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        registered=true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(registered) {
            unregisterReceiver(mReceiver);
            registered=false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.close:
                b.removeCommunicationCallback();
                b.disconnect();
                Intent intent = new Intent(this, Select.class);
                startActivity(intent);
                finish();
                return true;

            case R.id.findBMP:
                Toast.makeText(getApplicationContext(), "Find BMP clicked",
                        Toast.LENGTH_SHORT).show();
                performFileSearch();
                return true;

            case R.id.rate:
                Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void Display(final String s){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.append(s + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Display("Connected to "+device.getName()+" - "+device.getAddress());
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                send.setEnabled(true);
            }
        });
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        Display("Disconnected!");
        Display("Connecting again...");
        b.connectToDevice(device);
    }

    @Override
    public void onMessage(String message) {
        Display(name+": "+message);
    }

    @Override
    public void onError(String message) {
        Display("Error: "+message);
    }

    @Override
    public void onConnectError(final BluetoothDevice device, String message) {
        Display("Error: "+message);
        Display("Trying again in 3 sec.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        b.connectToDevice(device);
                    }
                }, 2000);
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Intent intent1 = new Intent(Chat.this, Select.class);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        if(registered) {
                            unregisterReceiver(mReceiver);
                            registered=false;
                        }
                        startActivity(intent1);
                        finish();
                        break;
                }
            }
        }
    };

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                try {
                    readTextFromUri(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);

        byte[] bytes = readBytes(inputStream);
        ArrayList<String> rawData = new ArrayList<>();
        ArrayList<String> pixelArray =  new ArrayList<>();
        String newS= "";

        System.out.println(bytes);

        for(byte b:bytes){

            String s = byteToHex(b);
            rawData.add(s);
            //System.out.println(s);
        }

        System.out.println("......Print Raw Data......");
        for(int i=0; i<rawData.size();i++){

            if(i>=62){
                pixelArray.add(rawData.get(i));
            }
            //System.out.println(rawData.get(i));
        }

        System.out.println("......Print Pixel Data......");
        for(int i=0;i<pixelArray.size();i++){
            System.out.println(pixelArray.get(i));
        }

        //byte[] testbytes = new byte[] { (byte) 0x02, (byte) 0x0F, (byte) 0xF0 };

        for(int i=0; i<pixelArray.size();i++){

            // Create a BigInteger using the byte array
            BigInteger bi = new BigInteger(pixelArray.get(i), 16);

            String s = bi.toString(2);
            newS += String.format("%8s", s).replace(' ', '0');
        }

        System.out.println(newS);

        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        databaseAccess.open();
        List<Integer> table_data = databaseAccess.read_632_Horizontal("632_Horizontal");
        databaseAccess.close();

        for(int i=0;i<table_data.size();i++){
            System.out.println(table_data.get(i));
        }

        //Preparing data
        prepareData(newS, table_data);
    }

    public void prepareData(String bitString, List<Integer> dbData){

        int bitPos = 0;

        for(int i=0; i<dbData.size();i++){

            if(dbData.get(i) ==2){
                bitPos = i-1;
                break;
            }
        }

        System.out.println("Bit String Length " + bitString.length());
        System.out.println("Database Size " + dbData.size());
        System.out.println("Bit data start position in db " + bitPos);

        //Placing first bit
        dbData.set(bitPos, Integer.parseInt(String.valueOf(bitString.charAt(0))));

        int count=1;

        //Place the bits in dbData List at respective positions
        for(int i=bitPos+1;i<dbData.size();i++){

            if(dbData.get(i)!= 1 && dbData.get(i)!= 0){

                if(count<632){
                    dbData.set(i, Integer.parseInt(String.valueOf(bitString.charAt(count))));
                    count++;
                }else{
                    break;
                }
            }
        }

        System.out.println("count value " + count);

        System.out.println("Updated Database Size" + dbData.size());
        for(int i=0;i<dbData.size();i++){
            System.out.println(dbData.get(i));
        }

        //Appending zeros after 13 bits of each row in db data
        int k=0;
        while(k< dbData.size()){

            List<Integer> dbData_append = new ArrayList<>();
            for(int j=0; j<13;j++){
                dbData_append.add(dbData.get(k));
                k++;
            }
            dbData_append.add(0);
            dbData_append.add(0);
            dbData_append.add(0);

            //System.out.println("16 bits..." + dbData_append);
            bitListtoHex(dbData_append);

        }

        System.out.println("FinalHex data... ");
        for(int i=0;i<finalHex.size();i++){
            System.out.println(finalHex.get(i));
        }

    }

    public static byte[] readBytes( InputStream stream ) throws IOException {
        if (stream == null) return new byte[] {};
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean error = false;
        try {
            int numRead = 0;
            while ((numRead = stream.read(buffer)) > -1) {
                output.write(buffer, 0, numRead);
            }
        } catch (IOException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } catch (RuntimeException e) {
            error = true; // this error should be thrown, even if there is an error closing stream
            throw e;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                if (!error) throw e;
            }
        }
        output.flush();
        return output.toByteArray();
    }

    /**
     * method to convert a byte to a hex string.
     *
     * @param data the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte data) {

        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));

        return buf.toString();
    }

    /**
     * Convenience method to convert an int to a hex char.
     *
     * @param i the int to convert
     * @return char the converted char
     */
    public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + (i - 10));
        }
    }

    public void bitListtoHex(List<Integer> binary){

        StringBuilder hexString = new StringBuilder();
        StringBuilder eightBits = new StringBuilder();
        for(int i = 0; i < binary.size(); i += 4) {
            for(int j = i; j < (i + 4) && j < binary.size(); j++) { // read next 8 bits or whatever bits are remaining
                eightBits.append(binary.get(j)); // build 8 bit value
            }
            int decimal = Integer.parseInt(eightBits.toString(), 2); // get decimal value
            hexString.append(Integer.toHexString(decimal)); // add hex value to the hex string and a space for readability
            eightBits.setLength(0); // reset so we can append the new 8 bits
        }

        finalHex.add(hexString.toString().substring(0,2));
        finalHex.add(hexString.toString().substring(2));
        //System.out.println(hexString.toString());
    }
}

