package net.networksaremadeofstring.rhybudd;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Created by Gareth on 19/09/13.
 */
public class WriteNFCActivity extends FragmentActivity
{
    public static String PAYLOAD_UID = "uid";
    IntentFilter[] intentFiltersArray = null;
    String[][] techListsArray;
    JSONObject tagMetaData;
    private NfcAdapter mAdapter;
    private PendingIntent pendingIntent;
    String UID = "";
    Handler tagHandler = null;
    NdefRecord aaRecord,idRecord;
    static int READY = 1000;
    static int WRITE_SUCCESS = 1001;
    static int IOEXCEPTION = 1002;
    static int FORMATEXCEPTION = 1003;
    static int TAG_IO_IN_PROGRESS = 1004;
    static int SERVER_IO_IN_PROGRESS = 1005;
    static int SERVER_IO_FAILURE = 1006;
    static int READONLY = 1007;
    static int SIZE_ERROR = 1008;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BugSenseHandler.initAndStartSession(WriteNFCActivity.this, "44a76a8c");


        getActionBar().setSubtitle(getString(R.string.NFCTitle));
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.write_tag_activity);

        try
        {
            UID = getIntent().getExtras().getString(PAYLOAD_UID).replace("/zport/dmd/Devices/","");

            aaRecord = NdefRecord.createApplicationRecord("net.networksaremadeofstring.rhybudd");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            {
                idRecord = NdefRecord.createExternal("rhybudd:tag", "z", UID.getBytes(Charset.forName("US-ASCII")));
            }
            else
            {
                idRecord = NdefRecord.createUri("z://"+UID);
            }

            ((TextView) findViewById(R.id.SizesText)).setText("This payload is " + (aaRecord.toByteArray().length + idRecord.toByteArray().length) + " bytes.\n\nAn ultralight can store up to 46 bytes.\nAn Ultralight C or NTAG203 can store up to 137 bytes.\nDespite the name a 1K can only store up to 716 bytes.");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Sorry there was error parsing the passed UID, we cannot continue.", Toast.LENGTH_SHORT).show();
            finish();
        }

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try
        {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                           You should specify only the ones that you need. */
        }
        catch (IntentFilter.MalformedMimeTypeException e)
        {
            throw new RuntimeException("fail", e);
        }

        IntentFilter td = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        intentFiltersArray = new IntentFilter[] {ndef, td};

        techListsArray = new String[][] { new String[] { NfcF.class.getName(),NfcA.class.getName(),Ndef.class.getName(), NdefFormatable.class.getName() } };

        CreateHandlers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                return true;
            }

            default:
                return false;
        }
    }

    public void onPause()
    {
        super.onPause();

        if(mAdapter != null)
            mAdapter.disableForegroundDispatch(this);
    }

    public void onResume()
    {
        super.onResume();

        if(mAdapter != null)
            mAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray);
    }

    public void onNewIntent(Intent intent)
    {
        Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        WriteTag(tagFromIntent);
    }


    public void CreateHandlers()
    {
        tagHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                if(msg.what == SERVER_IO_FAILURE)
                {
                    Toast.makeText(WriteNFCActivity.this, "There was an error communicating with the Zenoss Server", Toast.LENGTH_LONG).show();
                    Intent in = new Intent();
                    setResult(SERVER_IO_FAILURE,in);
                    finish();
                }
                if(msg.what == SERVER_IO_FAILURE)
                {
                    Toast.makeText(WriteNFCActivity.this, "The Device UID is too large to write to the tag.", Toast.LENGTH_LONG).show();
                    Intent in = new Intent();
                    setResult(SERVER_IO_FAILURE,in);
                    finish();
                }
                else if(msg.what == READY)
                {
                    ((RelativeLayout) findViewById(R.id.WriteTagIndicator)).setBackgroundResource(R.drawable.writetag_ready);
                    ((ProgressBar) findViewById(R.id.IOProgressBar)).setVisibility(View.GONE);
                    ((ImageView) findViewById(R.id.instructionImage)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.IODesc)).setText(R.string.TagReadyFirst);
                }
                else if(msg.what == TAG_IO_IN_PROGRESS)
                {
                    ((RelativeLayout) findViewById(R.id.WriteTagIndicator)).setBackgroundResource(R.drawable.writetag_working);
                    ((ProgressBar) findViewById(R.id.IOProgressBar)).setVisibility(View.VISIBLE);
                    ((ImageView) findViewById(R.id.instructionImage)).setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.IODesc)).setText(R.string.TagIOWait);

                }
                else if(msg.what == WRITE_SUCCESS)
                {
                    ((RelativeLayout) findViewById(R.id.WriteTagIndicator)).setBackgroundResource(R.drawable.writetag_ready);
                    ((ProgressBar) findViewById(R.id.IOProgressBar)).setVisibility(View.GONE);
                    ((ImageView) findViewById(R.id.instructionImage)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.IODesc)).setText(R.string.TagReadyNext);

                    Toast.makeText(WriteNFCActivity.this, "Tag Written successfully!", Toast.LENGTH_LONG).show();
                    Intent in = new Intent();
                    setResult(0,in);
                    finish();
                }
                else if(msg.what == READONLY)
                {
                    Toast.makeText(WriteNFCActivity.this, "That tag was Read Only and couldn't be written too", Toast.LENGTH_LONG).show();
                    ((RelativeLayout) findViewById(R.id.WriteTagIndicator)).setBackgroundResource(R.drawable.writetag_ready);
                    ((ProgressBar) findViewById(R.id.IOProgressBar)).setVisibility(View.GONE);
                    ((ImageView) findViewById(R.id.instructionImage)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.IODesc)).setText(R.string.TagReadyNext);
                }
                else if(msg.what == FORMATEXCEPTION)
                {
                    Toast.makeText(WriteNFCActivity.this, "There was an error trying to format that tag", Toast.LENGTH_LONG).show();
                    ((RelativeLayout) findViewById(R.id.WriteTagIndicator)).setBackgroundResource(R.drawable.writetag_ready);
                    ((ProgressBar) findViewById(R.id.IOProgressBar)).setVisibility(View.GONE);
                    ((ImageView) findViewById(R.id.instructionImage)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.IODesc)).setText(R.string.TagReadyNext);
                }
                else if(msg.what == IOEXCEPTION)
                {
                    Toast.makeText(WriteNFCActivity.this, "AN I/O Error was encountered\n(Did you move the tag away before it was finished writing?)", Toast.LENGTH_LONG).show();
                    ((RelativeLayout) findViewById(R.id.WriteTagIndicator)).setBackgroundResource(R.drawable.writetag_ready);
                    ((ProgressBar) findViewById(R.id.IOProgressBar)).setVisibility(View.GONE);
                    ((ImageView) findViewById(R.id.instructionImage)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.IODesc)).setText(R.string.TagReadyNext);
                }
            }
        };

        tagHandler.sendEmptyMessage(READY);
    }


    public void WriteTag(final Tag receivedTag)
    {
        tagHandler.sendEmptyMessage(TAG_IO_IN_PROGRESS);

        (new Thread(){

            public void run()
            {
                //This could go all kinds of weird
                Ndef thisNdef = null;


                try
                {
                    thisNdef = Ndef.get(receivedTag);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

                if(null == thisNdef)
                {
                    NdefFormatable formatter = NdefFormatable.get(receivedTag);
                    try
                    {
                        formatter.connect();
                        formatter.format( new NdefMessage(new NdefRecord[]{NdefRecord.createApplicationRecord("io.d0")}));
                        formatter.close();
                        thisNdef = Ndef.get(receivedTag);
                    }
                    catch(Exception d)
                    {
                        d.printStackTrace();
                        tagHandler.sendEmptyMessage(FORMATEXCEPTION);
                    }
                }

                try
                {
                    if(null == thisNdef)
                    {
                        throw new FormatException("No NDEF Tag returned from get");
                    }
                    else
                    {
                        thisNdef.connect();
                    }

                    if(thisNdef.isWritable())
                    {
                            //Final Tag Payload;
                            Log.i("WriteTag-Payload", tagMetaData.toString());

                            //Is this a 203 or larger?
                            if(thisNdef.getMaxSize() < aaRecord.toByteArray().length + idRecord.toByteArray().length)
                            {
                                /*Log.i("WriteTag","This tag was too big. tried to write " + (aaRecord.toByteArray().length + idRecord.toByteArray().length) + " to " + thisNdef.getMaxSize());
                                idRecord = NdefRecord.createMime("text/plain", Integer.toString(tagMetaData.getInt("i")).getBytes(Charset.forName("US-ASCII")));
                                Log.i("WriteTag Size Check", "Writing " + (idRecord.toByteArray().length + aaRecord.toByteArray().length) + " to " + thisNdef.getMaxSize());*/
                                tagHandler.sendEmptyMessage(SIZE_ERROR);
                            }
                            else
                            {
                                Log.i("WriteTag Size Check", "Writing " + (aaRecord.toByteArray().length + idRecord.toByteArray().length) + " to " + thisNdef.getMaxSize());

                                NdefMessage tagMsg = new NdefMessage(new NdefRecord[]{idRecord,aaRecord});
                                Log.i("WriteTag Size Check", "Wrote " + tagMsg.getByteArrayLength());
                                thisNdef.writeNdefMessage(tagMsg);
                                thisNdef.makeReadOnly();
                                thisNdef.close();
                                tagHandler.sendEmptyMessage(WRITE_SUCCESS);
                            }
                    }
                    else
                    {
                        tagHandler.sendEmptyMessage(READONLY);
                    }
                }
                catch (IOException e)
                {
                    tagHandler.sendEmptyMessage(IOEXCEPTION);
                }
                catch (FormatException e)
                {
                    e.printStackTrace();
                    tagHandler.sendEmptyMessage(FORMATEXCEPTION);
                }
            }
        }).start();
    }
}
