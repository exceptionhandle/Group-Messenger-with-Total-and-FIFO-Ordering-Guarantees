package edu.buffalo.cse.cse486586.groupmessenger2;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.lang.Thread.UncaughtExceptionHandler;
import android.text.format.Time;
/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    ArrayList<msgOrder> mainFIFO = new ArrayList<msgOrder>();
    public double proposal;
    public int diedSoul = -1;
    public int[] replyTimer;
    public int maxReplies = 0;
    public GroupMessengerActivity() {
        processes = new HashMap<Integer, process>();
        replyTimer = new int[5];
        for(int i =0;i<5;i++){
            replyTimer[i]=0;
        }
    }

    public class msgOrder implements Comparable<msgOrder>{
        public int procID;
        public String msg;
        public int sequence;
        public double proposalNum;
        public boolean confirmed;
        public ArrayList<Integer> ProposalsRecpt;
        public int proposalcount = 0;
        public long startTime;
        public ArrayList<Integer> NotReceived;


        msgOrder(double propose, int seq, final String msg, int PORT, int procID) {
            if(diedSoul == PORT){
                diedSoul = -1;
            }
            replyTimer[(PORT - 11108) / 4] ++;
            //if(maxReplies>replyTimer[(PORT - 11108) / 4]){
                maxReplies = replyTimer[(PORT - 11108) / 4];
           // }
            ProposalsRecpt = new ArrayList<Integer>();
            this.procID = procID;
            this.msg = msg;
            this.sequence = seq;
            this.proposalNum = (double)propose;
            if(serverProposal > this.proposalNum){
                this.proposalNum = serverProposal;
            }
            confirmed = false;
            ProposalsRecpt.add(PORT);
            proposalcount++;
            serverProposal++; // increase proposal count with each message added only.
            mainFIFO.add(this);
            proposal = this.proposalNum;
            startTime = System.currentTimeMillis();
            Integer[] REMOTE_PORT = {11108, 11112, 11116, 11120, 11124};
            NotReceived = new ArrayList<Integer>();
            for (int i=0; i<REMOTE_PORT.length; i++) {
                if(REMOTE_PORT[i]==diedSoul||REMOTE_PORT[i]==PORT || REMOTE_PORT[i]==Integer.parseInt(myPort)){
                    continue;
                }
                NotReceived.add(REMOTE_PORT[i]);
            }

        }


        void updateProposal(double proposalNum, int PORT) {
            if(diedSoul == PORT){
                diedSoul = -1;
            }
            replyTimer[(PORT - 11108) / 4]  ++;
            //if(maxReplies>replyTimer[(PORT - 11108) / 4]){
                maxReplies = replyTimer[(PORT - 11108) / 4];
            //}
            if(ProposalsRecpt.contains(PORT)){
                return;
            }
            Log.e("PORT to be checked in notreceived",Integer.toString(PORT));
            if(!NotReceived.isEmpty()&&NotReceived.contains(PORT)){
                Log.e("goint to remove this port",Integer.toString(PORT));
                NotReceived.remove(NotReceived.indexOf(PORT));
            }
            if(diedSoul != -1){
                if(NotReceived.contains(diedSoul)){
                    NotReceived.remove(NotReceived.indexOf(diedSoul));
                }
            }
            ProposalsRecpt.add(PORT);
            if (Double.compare(this.proposalNum,proposalNum)<0) {
                this.proposalNum = proposalNum;
            }


            if (NotReceived.isEmpty()) {
                confirmed = true;
            }
            proposalcount++;
            proposal = this.proposalNum;
            startTime = System.currentTimeMillis();
            Log.e("not revceived",NotReceived.toString());
            Log.e("current port",Integer.toString(PORT));
            Log.e("Size of NotRecieved",Integer.toString(NotReceived.size()));
        }

        @Override
        public int compareTo(@NonNull msgOrder another) {
            return Double.compare(this.proposalNum,another.proposalNum);
        }
    }

    public class process {//implements Comparable<process> {
        public ArrayList<msgOrder> FIFO = new ArrayList<msgOrder>();
        public int procID;
        public int expectedSequence;
        public int lastMsgTotalProposals = 0;

        public int numOfproposals(int sequence) {
            int i = 0;
            for (; i < FIFO.size(); i++) {
                if (FIFO.get(i).sequence == sequence) {
                    return FIFO.get(i).proposalcount;
                }
            }
            return (1<<30);
        }

        process(int processID, double proposalNum, int sequence, String msg, int PORT) {
            procID = processID;
            double newProposal;
            //newProposal = serverProposal>proposalNum ? serverProposal:proposalNum;
            msgOrder msgpriority = new msgOrder(proposalNum, sequence, msg , PORT, procID);
            FIFO.add(msgpriority);
            expectedSequence++;
            lastMsgTotalProposals = msgpriority.proposalcount;
        }

        boolean isConfirmed(int sequence) {
            int i = 0;
            for (; i < FIFO.size(); i++) {
                if (FIFO.get(i).sequence == sequence) {
                    return FIFO.get(i).confirmed;
                }
            }
            return false;
        }

        void updateProposal(double proposalNum, int sequence, String msg, int PORT) {
            int i = 0;
            for (; i < FIFO.size(); i++) {
                if (FIFO.get(i).sequence == sequence) {
                    FIFO.get(i).updateProposal(proposalNum, PORT);
                    break;
                }
            }

            if (i == FIFO.size() && sequence >= expectedSequence) {
                //double newProposal = (serverProposal>proposalNum ? serverProposal:proposalNum);
                msgOrder msgpriority = new msgOrder(proposalNum, sequence, msg, PORT, procID);
                FIFO.add(msgpriority);
                if (expectedSequence == sequence) {
                    expectedSequence++;
                }
            }
            lastMsgTotalProposals = FIFO.get(i).proposalcount;
        }


    }

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    HashMap<Integer, process> processes;
    int clientMsgSequence = 0;
    double serverProposal = 0;
    //static int NumOfProcesses = 0;
    String myPort;
    int onlineProcs = 5;

    public ContentResolver mContentResolver;
    public Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private ContentValues initValues(int serno, String msg) {
        ContentValues cv = new ContentValues();
        cv.put("key", Integer.toString(serno));
        cv.put("value", msg);
        return cv;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        mContentResolver = getContentResolver();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        serverProposal = 0.00001*Double.parseDouble(myPort);
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        try {
            findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    final EditText editText = (EditText) findViewById(R.id.editText1);
                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");
                    TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                    localTextView.append("\t" + msg);
                    TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                    remoteTextView.append("\n");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Buttom Exception");
            e.printStackTrace();
        }
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        private String msgReceived;
        private DataInputStream input;
        private final String TAG = ServerTask.class.getName();
        private int serNo = 0;
        private ContentValues mContentValues;
        private int procID;

        private int sequence;
        private String msg;
        private int lastmsgProposCount = 0;
        String[] msgBox;
        private int sendrPORT;
        private boolean testInsert(ContentValues mContentValues) {
            try {

                mContentResolver.insert(mUri, mContentValues);

            } catch (Exception e) {
                Log.e(TAG, e.toString());
                return false;
            }

            return true;
        }

        void updateProcess(String msgReceived) {
            msgBox = msgReceived.split("#%#");
            procID = Integer.parseInt(msgBox[0]);
            proposal = Double.parseDouble(msgBox[1]);
            sequence = Integer.parseInt(msgBox[2]);
            msg = msgBox[3];
            sendrPORT = Integer.parseInt(msgBox[4]);
            if (processes.containsKey(procID)) {
                if (processes.get(procID).isConfirmed(sequence)) {
                    lastmsgProposCount = processes.get(procID).numOfproposals(sequence);
                    return;
                }
                process procObj = processes.get(procID);
                procObj.updateProposal(proposal, sequence, msg, sendrPORT);
            } else {
                process newProcObj = new process(procID, proposal, sequence, msg, sendrPORT);
                processes.put(procID, newProcObj);
            }
            lastmsgProposCount = processes.get(procID).numOfproposals(sequence);
           // proposal = processes.get(procID).getProposal(sequence);
        }


        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            try {

                do {
                    Socket acceptedSocket = serverSocket.accept();

                    input = new DataInputStream(acceptedSocket.getInputStream());
                    msgReceived = input.readUTF();
                    msgReceived = msgReceived.trim();
                    Log.e("recieved message msgRecieved", msgReceived);
                    if(msgReceived.equals("CONNECTED?")){
                        DataOutputStream output = new DataOutputStream(acceptedSocket.getOutputStream());
                        output.writeUTF("YES");
                        output.flush();
                        break;
                    }
                    Log.e("Updating Message", msgReceived);
                    updateProcess(msgReceived);
                    /// if not first time then don't broadcast the message again as already broadcasted
                    if (lastmsgProposCount < 2){ //&& procID != Integer.parseInt(myPort)) {
                        String newMsg = procID + "#%#" + proposal+ "#%#" + sequence + "#%#" + msg + "#%#" + myPort;
                        BroadcastUpdatedProposals(newMsg);
                    }
                    while(true) {

                        Collections.sort(mainFIFO);

                        int MaxDiff = (1000);
                      //  Log.e("SIZE OF MAIN FIFO",Integer.toString(mainFIFO.size())+" "+mainFIFO.toString());

                        int[] REMOTE_PORT = {11108, 11112, 11116, 11120, 11124};
                        int min = (1 << 30);
                        if(!mainFIFO.isEmpty()&&diedSoul == -1) {
                            for (int i = 0; i < 5; i++) {
                                if (REMOTE_PORT[i] == Integer.parseInt(myPort))
                                    continue;
                                else if(min == (1<<30)){
                                    min = i;
                                }
                                else if (replyTimer[min] > replyTimer[i]) {
                                    min = i;
                                }
                            }

                        }
                        Log.e("replies ==>",maxReplies+" "+min);
                        if(!mainFIFO.isEmpty() && diedSoul == -1&&min<5 && maxReplies - replyTimer[min]>10
                                && mainFIFO.get(0).NotReceived.size()==1){

                                //}
                                //  if(min>5&&maxReplies-replyTimer[min]<6)
                                //     continue;
                            int notrevdPORT = mainFIFO.get(0).NotReceived.get(0);
                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        notrevdPORT);
                                socket.setSoTimeout(1000);
                                Log.e("CHECKING PORT IF ACCEPTING", Integer.toString(notrevdPORT));
                                if (!ifConnected(socket)) {
                                    diedSoul = notrevdPORT;
                                    Log.e("not connected", "this socket" + diedSoul);
                                    socket.close();
                                    for (int x = 0; x < mainFIFO.size(); x++) {
                                        if(mainFIFO.get(x).procID == diedSoul){
                                            mainFIFO.remove(x);
                                        }
                                        else if (mainFIFO.get(x).NotReceived.contains(diedSoul)) {
                                            mainFIFO.get(x).NotReceived.remove(mainFIFO.get(x).NotReceived.indexOf(diedSoul));
                                            mainFIFO.get(x).confirmed = mainFIFO.get(x).NotReceived.isEmpty();
                                        }
                                    }
                                    break;
                                } else {
                                    socket.close();
                                }


                        }
                        else if(diedSoul!=-1){

                            if(!mainFIFO.isEmpty() && !mainFIFO.get(0).confirmed
                                && (mainFIFO.get(0).NotReceived.size()==1 && mainFIFO.get(0).NotReceived.contains(diedSoul))){
                                    mainFIFO.get(0).NotReceived.remove(mainFIFO.get(0).NotReceived.indexOf(diedSoul));
                                    if(mainFIFO.get(0).NotReceived.isEmpty()){
                                        mainFIFO.get(0).confirmed = true;
                                        Log.e("Msg Confirmed",mainFIFO.get(0).msg);
                                    }

                            }
                        }

                        if(!mainFIFO.isEmpty()){
                            Log.e("message in the front of fifo",mainFIFO.get(0).msg);


                        }
                        if (mainFIFO.isEmpty() ||(!mainFIFO.get(0).confirmed)) {
                            Log.e("I am going to break", "mainFIFO empty?"+mainFIFO.isEmpty());
                            break;
                        }
                        //Log.e("front of priority queue", Integer.toString(p.procID));
                        Log.e("Front Confirmed", "publishing the stuff");
                        Log.e("Number of Proposals of Front", Double.toString(mainFIFO.get(0).ProposalsRecpt.size()));
                        Log.e("Proposal Number of Front",Double.toString(mainFIFO.get(0).proposalNum));
                        String seqMsg = mainFIFO.get(0).msg;//p.getFront();
                        mainFIFO.remove(0);
                        publish(seqMsg);
                    }

            } while (true);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }
        public void publish(String seqMsg){
            mContentValues = initValues(serNo, seqMsg);
            testInsert(mContentValues);
            Log.e(TAG, "values inserted : " + serNo + " " + seqMsg);
            serNo++;
            Log.e(TAG, mContentValues.toString());

            publishProgress(seqMsg.trim());
        }

        public boolean ifConnected(Socket socket){
            try {
                Log.e("connected?", "checking");
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                output.writeUTF("CONNECTED?");
                output.flush();
                Log.e("return yes?", "lets see");
                DataInputStream input = new DataInputStream(socket.getInputStream());
                String mReceived = input.readUTF();
                mReceived = mReceived.trim();
                Log.e("recieved ",mReceived);
                if(mReceived.equals("YES")){
                    Log.e("found no exception","wooo");
                    socket.close();
                    return true;
                }

            }catch (SocketTimeoutException te){
                Log.e("socket time out","exception caught");
                return false;
            }catch (StreamCorruptedException sce){
                Log.e("stream corrupt exception","exception caught");
                return false;
            }catch(IOException ioe){
                Log.e("socket IO exception","exception caught");
                return false;
            }
            return false;
        }
        protected Void BroadcastUpdatedProposals(String msg) {
            DataOutputStream output;

            int iter = 0;
            int[] REMOTE_PORT = {11108, 11112, 11116, 11120, 11124};

            while (iter < 5) {
                try {
                    if ((REMOTE_PORT[iter] == Integer.parseInt(myPort))) {
                        iter++;
                        continue;
                    }
                    Log.e("broadcasting", "new");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            REMOTE_PORT[iter]);
                   /* socket.setSoTimeout(500);
                    Log.e("CHECKING PORT IF ACCEPTING", Integer.toString(REMOTE_PORT[iter]));
                    if (!ifConnected(socket)) {
                        diedSoul = REMOTE_PORT[iter];
                        Log.e("not connected", "this socket" + diedSoul);
                        socket.close();
                        continue;

                    }
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            REMOTE_PORT[iter]);

                  */iter++;
                    output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF(msg);
                    output.flush();
                } catch (UnknownHostException e) {
                    Log.e(TAG, "serverBroadcast UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "serverBroadcast socket IOException");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Exception Caught");
                }
                Log.e("Bro! Online Socket", Integer.toString(iter)+" Sending Msg :"+msg);

            }
            Log.e("Total online Sockets", Integer.toString(iter));

            return null;


        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");


            String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";


            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }

        }

    }

    private class ClientTask extends AsyncTask<String, Void, Void> {
        private DataOutputStream output;

        @Override
        protected Void doInBackground(String... msgs) {

                String newmsg = msgs[1] + "#%#" + (serverProposal) + "#%#" + clientMsgSequence + "#%#" + msgs[0]+"#%#"+myPort;
            Log.e("Message from Client Side",newmsg);
            clientMsgSequence++;
                    try {
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[1]));
                        output = new DataOutputStream(socket.getOutputStream());
                        output.writeUTF(newmsg);
                        output.flush();
                        socket.close();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "ClientTask socket IOException");
                    }catch( Exception e){
                        e.printStackTrace();
                        Log.e(TAG,"Exception Caught");
                    }


            return null;
        }

    }


}