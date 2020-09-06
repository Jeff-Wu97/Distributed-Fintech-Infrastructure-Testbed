package com.example.commdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static TextView tv;

    private EditText editText1;
    private EditText editText2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        tv = (TextView) findViewById(R.id.text_view3);
        tv.setBackgroundColor(Color.WHITE);
        Button button1 = (Button) findViewById(R.id.button_send);
        Button button2 = (Button) findViewById(R.id.button_intialize);
        editText1 = (EditText) findViewById(R.id.edit_text1);
        editText2 = (EditText) findViewById(R.id.edit_text2);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        tv.append("Mobile Device ID: " + SystemInfo.client_id+"\n");
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        ReceiveThread receiver = new ReceiveThread(SystemInfo.Port_Arr[SystemInfo.node_id]);
        receiver.start();
        tv.append("IP = "+SystemInfo.IP_Arr[SystemInfo.node_id] +
                "     Listening Port = " + SystemInfo.Port_Arr[SystemInfo.node_id]+"\n"+"System View = Device "+SystemInfo.view+"\n");
        tv.append("Please initialize system");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_intialize:
                showReceivedText("Exchanging public key");
                Initialize.initializeKey();
                showReceivedText("System initialized. Please send transaction");
                break;
            case R.id.button_send:
                String message_size_byte = editText1.getText().toString();//get the text in textline
                String request_interval_ms = editText2.getText().toString();
                //Client client = new Client(inputText);
                //client.start();
                TestScript testScript=new TestScript(SystemInfo.algorithm,10,100);
                testScript.start();
                break;
            default:
                break;
        }
    }

    public void showReceivedText(final String ReceivedText) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                tv.append(ReceivedText + "\n");
                // Stuff that updates the UI
            }
        });
    }

    class TestScript extends Thread{
        int transaction_data_size;
        int transaction_num;
        String algorithm;
        public TestScript(String algorithm, int transaction_data_size,int transaction_num){
            this.transaction_data_size=transaction_data_size;
            this.transaction_num=transaction_num;
            this.algorithm=algorithm;
        }
        public void run(){
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyApp::MyWakelockTag");
            wakeLock.acquire();


            byte[] b = new byte[transaction_data_size];
            Arrays.fill(b, (byte)0x32);
            String s = new String(b);
            long start_time = System.currentTimeMillis();
            int i=0;
            //for (int i=0;i<transaction_num;i++){
                long start_transaction = System.currentTimeMillis();
                Client client=new Client(s);
                client.start();
                /*
                while (client.isAlive()){
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Client client1=new Client(s);
                client1.start();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Client client2=new Client(s);
                client2.start();

                /*
                while (client.isAlive()||client1.isAlive()||client2.isAlive()){

                }

                long end_transaction = System.currentTimeMillis();
                System.out.println("#### "+(i+1)+" transaction completed, time cost = "+(end_transaction-start_transaction)
                        +", "+(transaction_num-i-1)+"left");
            //}
            long end_time = System.currentTimeMillis();
            showReceivedText("##Test completed:"+"\n"+ "Transaction Number = "+transaction_num
                    +"   Message size = "+transaction_data_size+"byte");
            showReceivedText("##Test time cost = "+(end_time-start_time)+"ms");
            System.out.println("##Test completed:"+"\n"+ "Transaction Number = "+transaction_num
                    +"   Message size = "+transaction_data_size+"byte, "+"Test time cost = "+(end_time-start_time)+"ms");*/
            wakeLock.release();
        }
    }
//------------------RECEIVE----------------------------------------RECEIVE----------------------------------------------------RECEIVE------------------------------------------RECEIVE------------------------------------
    class ReceiveThread extends Thread {
        private int port;
        public String message;

        public ReceiveThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {

            try {

                ServerSocket serverSocket = new ServerSocket();//port);
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(port));
                while (true) {
                    Socket socket = serverSocket.accept();

                    InputStream inputStream = socket.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String info = null;
                    while ((info = bufferedReader.readLine()) != null && info != "") {
                        message = info;
                        if (!SystemInfo.test_off){
                            System.out.println("---- Receive: " + message );
                            messageResolve(message);
                        }
                    }
                    socket.close();
                }
                //socket.shutdownInput();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void messageResolve(String message) {
            /*
                Zyzzyva
                <REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                <<ORDER-REQ,u,n,hn,d,ND>sigp,m> => [, , ORDER-REQ, u, n, hn, d, ND, sigp, , REQUEST, o, t, c, sigc]
                <<SPEC-RESPONSE,u,n,hn,H(r),c,t>sigi,i,r,OR> => [, , SPEC-RESPONSE, u, n, hn, H(r), c, t, sigi, i, r, , ORDER-REQ, u, n, hn, d, ND, sigp]
                <FILL-HOLE,u,maxn+1,n,i>sigi => [, FILL-HOLE, u, maxn+1, n, i, sigi]
                <COMMIT,c,CC>sigc => [, COMMIT, c, CC, sigc]
                <LOCAL-COOMIT,u,d,h,i,c>sigi => [, LOCAL-COOMIT, u, d, h, i, c, sigi]
                <CONFIRM-REQ,u,<REQUEST,o,t,c>sigc,i>sigi => [, CONFIRM-REQ, u, , REQUEST, o, t, c, sigc, i, sigi]
                <I-HATE-THE-PRIMARY,v>sigi => [, I-HATE-THE-PRIMARY, v, sigi]
                <VIEW-CHANGE,v+1,CC,O,i>sigi => [, VIEW-CHANGE, v+1, CC, O, i, sigi]
                <NEW-VIEW,v+1,P>signp => [, NEW-VIEW, v+1, P, signp]
                <VIEW-CONFIRM,v+1,n,h,i>sigi => [, VIEW-CONFIRM, v+1, n, h, i, sigi]
             */
            synchronized (SystemInfo.logTransaction){
                String[] seg=message.split("<|,|>");
                boolean verify_result;
                Timer backuptimer= new Timer();
                if (seg.length<=2){ return;}
                SystemInfo.isReceiving=true;
                if (seg[0].equals("KEY")){
                    String otherNodeKeyTxt=seg[1]+","+seg[2];
                    SystemInfo.key_lib.add(otherNodeKeyTxt);
                }else if (seg[1].equals("REQUEST")) {// primary receive request to start a transaction // rep receive a request to send CONFIRM-REQ (<2f+1 case)
                    /*
                    <REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                    seg[1]=REQUEST, seg[2]=operation o, seg[3]=req timeseal, seg[4]= client, seg[5]=sigc
                     */
                    if (SystemInfo.node_id!=SystemInfo.view){// if the this is not the primary,

                        if (Long.valueOf(seg[3])<SystemInfo.timeseal){// if the rep has processed the request,
                            if (SystemInfo.timeSeal_log.contains(seg[3])) { //if the rep responded the the request, resend spec-response
                                ArrayList<String> SRsent = UtilClass.findInLog(SystemInfo.log_timeseal_SR.get(seg[3]), "" + SystemInfo.node_id + ",result_" + SystemInfo.node_id);
                                //String resend_response=SystemInfo.log_timeseal_SR.get(seg[3]);
                                if (SRsent.size() != 0) {
                                    UtilClass.sendMessage(SRsent.get(0), Integer.valueOf(seg[4]));
                                } else {
                                    return;
                                }
                            }// if the rep got the request but hasn't responded the request, let it go.

                        }else{// if the rep has'nt got the request before
                            if (SystemInfo.viewChangeFlag){
                                return;
                            }
                            BackupConfirmRequest backupConfirmRequest=new BackupConfirmRequest(message);
                            backupConfirmRequest.start();
                        }
                        return;
                    }
                    //Primary Case
                    //extract the data
                /*<REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                    seg[1]="REQUEST", seg[2]=operation o , seg[3]=timeseal t, seg[4]=client_id,
                    seg[5]=client_sign for <seg[1],seg[2],seg[3],seg[4]>
                 */
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+">";
                    String sign=seg[5];
                    //check if this is the first request message for the operation
                    //Here we need a method to check the element in message record
                    verify_result = Schnorr.checkSign(data,sign);
                    if (verify_result) {
                        //check if it is a new transaction
                        if(SystemInfo.timeSeal_log.contains(seg[3])) {
                            return;
                        }
                        SystemInfo.timeseal=Long.valueOf(seg[3]);
                        ArrayList<String> mlog=new ArrayList<>();
                        mlog.add(message);
                        SystemInfo.logTransaction.put(""+SystemInfo.request_id,mlog);//hashmap: n0-> list:[request]
                        Zyzzyva zyzzyva=new Zyzzyva(""+SystemInfo.request_id);
                        SystemInfo.request_id++;
                        zyzzyva.start();
                    }
                } else if (seg[2].equals("ORDER-REQ")) {// rep receive OR and start a transaction
                /*<<ORDER-REQ,u,n,hn,d,ND>sigp,m> => [, , ORDER-REQ, u, n, hn, d, ND, sigp, , REQUEST, o, t, c, sigc]
                    seg[2]="ORDER-REQ", seg[3]=view u , seg[4]=request_id n, seg[5]= hash(hash(all req)+hash(this req)),
                    seg[6]=digest of the req hash(req), seg[7]=ND undetermined operation value, seg[8]=primary sign<2,3,4,5,6,7>,
                    seg[10]="REQUEST", seg[11]=operation o , seg[12]=timeseal t, seg[13]=client_id, seg[14]=client sign<10,11,12,13>
                 */
                    String or_data = "<" + seg[2] + "," + seg[3] + "," + seg[4] + "," + seg[5] +","+seg[6]+","+seg[7] +">";
                    String or_sign = seg[8];
                    String req_data = "<" + seg[10] + "," + seg[11] + "," + seg[12] + "," + seg[13] + ">";
                    String req_sign = seg[14];
                    verify_result = (Schnorr.checkSign(or_data, or_sign) && Schnorr.checkSign(req_data, req_sign));
                    if (verify_result) {
                        if (SystemInfo.logTransaction.containsKey(seg[4])) {
                            return;
                        }
                        //check if it is a new transaction
                        boolean check_u=seg[3].equals("" + SystemInfo.view);
                        boolean check_n=Integer.valueOf(seg[4]).equals(SystemInfo.request_id);
                        boolean check_hn=seg[5].equals(UtilClass.digest(SystemInfo.History+UtilClass.digest(req_data+req_sign)));
                        boolean check_u_n_hn = (check_u&&check_n&&check_hn);
                        System.out.println("u,n,hn: "+check_u+check_n+check_hn);

                        if (check_u_n_hn) {
                            backuptimer.cancel();//cancel the backup view change timer
                            ArrayList<String> mlog = new ArrayList<>();//new the message log and add the message into the log
                            mlog.add(message);
                            SystemInfo.logTransaction.put(seg[4], mlog);
                            SystemInfo.log_req_OR.put(seg[4],message);
                            SystemInfo.timeseal=Long.valueOf(seg[12]);
                            Zyzzyva zyzzyva = new Zyzzyva(seg[4]);
                            SystemInfo.request_id++;

                            zyzzyva.start();
                        } else {
                            System.out.println("## Err: Failure in checking Pre-prepare digest, view or history");
                        }
                    }
                }else if (seg[2].equals("SPEC-RESPONSE")) {// client receive SPEC-RESPONSE from reps and record them
                    /*
                    <<SPEC-RESPONSE,u,n,hn,H(r),c,t>sigi,i,r,OR> => [, , SPEC-RESPONSE, u, n, hn, H(r), c, t, sigi, i, r, , ORDER-REQ, u, n, hn, d, ND, sigp]
                    seg[2]="SPEC-RESPONSE", seg[3]=view u, seg[4]=request_id n, seg[5]= history, seg[6]=hash(operation result),
                    seg[7]=client, seg[8]=req timeseal, seg[9]=rep sign<2,3,4,5,6,7,8>, seg[10]=rep i, seg[11]=operation result, seg[13]="ORDER-REQ",
                    seg[14]=view_OR, seg[15]=request_id_OR, seg[16]=hn_OR, seg[17]=req_digest_OR, seg[18]=ND_OR, seg[19]=primary sign<13,14,15,16,17,18>
                     */
                    String sr_data = "<" + seg[2] + "," + seg[3] + "," + seg[4] + "," + seg[5] + "," + seg[6] + "," + seg[7] + "," + seg[8] + ">";
                    String sr_sign = seg[9];
                    verify_result = Schnorr.checkSign(sr_data, sr_sign);
                    if (verify_result) {
                        if (SystemInfo.log_timeseal_SR.containsKey(seg[8])) {
                            if (UtilClass.digest(seg[11]).equals(seg[6])) {

                                ArrayList<String> check_rep=UtilClass.findInLog(SystemInfo.log_timeseal_SR.get(seg[8]),seg[10]+",result_"+seg[10]);
                                if (check_rep.size()==0){// if there is no spec-response from the rep before, this is a effective SR
                                    SystemInfo.log_timeseal_SR.get(seg[8]).add(message);
                                }
                            }
                        }
                    }
                }else if (seg[1].equals("FILL-HOLE")) {//primary receive FILL-HOLE from rep to ask for the OR of lost requests
                    /*<FILL-HOLE,u,maxn+1,n,i>sigi => [, FILL-HOLE, u, maxn+1, n, i, sigi]
                        seg[1]="FILL-HOLE", seg[2]=view u, seg[3]=rep lost request_id (rep max request_id+1), seg[4]=request_id, seg[5]=rep, seg[6]=rep sign<1,2,3,4>
                     */
                    String fh_data = "<" + seg[1] + "," + seg[2] + "," + seg[3] + "," + seg[4] + "," + seg[5] + ">";
                    String fh_sign = seg[6];
                    verify_result = Schnorr.checkSign(fh_data, fh_sign);
                    if (verify_result) {
                        if (seg[2].equals("" + SystemInfo.view)) {
                            HelpFillHole helpFillHole = new HelpFillHole(Integer.valueOf(seg[3]),SystemInfo.request_id-1,Integer.valueOf(seg[5]));
                            helpFillHole.start();
                        }
                    }
                }else if (seg[1].equals("COMMIT")) {//rep receive commit form client in (2f+1~3f case)
                    /* <COMMIT,c,CC>sigc => [, COMMIT, c, CC, sigc]
                        seg[1]="COMMIT", seg[2]=client c, seg[3]=commit certificate CC (spec-response set for the request), seg[4]=client sign<1,2,3>
                     */
                    String co_data = "<" + seg[1] + "," + seg[2] + "," + seg[3] + ">";
                    String co_sign = seg[4];
                    verify_result = Schnorr.checkSign(co_data, co_sign);
                    if (verify_result) {
                        RepLocalCommit repLocalCommit = new RepLocalCommit(seg[3]);
                        repLocalCommit.start();
                        System.out.println("localcommit start");
                    }
                }else if (seg[1].equals("LOCAL-COMMIT")){
                    /* <LOCAL-COMIT,u,d,h,i,c>sigi => [, LOCAL-COMIT, u, d, h, i, c, sigi]
                        seg[1]="LOCAL-COMMIT", seg[2]=view u, seg[3]=hash(request), seg[4]=history, seg[5]=rep i, seg[6]=client, seg[7]=rep sign<1,2,3,4,5,6>
                     */
                    String lc_data= "<" + seg[1] + "," + seg[2] + "," + seg[3] + "," + seg[4]+"," + seg[5]+"," + seg[6]+">";
                    String lc_sign=seg[7];
                    verify_result=Schnorr.checkSign(lc_data,lc_sign);
                    if (verify_result){
                        if (seg[2].equals(""+SystemInfo.view) && SystemInfo.reqhash_localcommit.containsKey(seg[3])){
                            SystemInfo.reqhash_localcommit.get(seg[3]).add(seg[5]);
                        }
                        System.out.println("localcommit num = "+SystemInfo.reqhash_localcommit.get(seg[3]).size());
                    }
                }else if (seg[1].equals("COMFIRM-REQ")) {
                    /*  <CONFIRM-REQ,u,<REQUEST,o,t,c>sigc,i>sigi => [, CONFIRM-REQ, u, , REQUEST, o, t, c, sigc, i, sigi]
                        seg[1]="CONFIRM-REQ", seg[2]=view u, seg[4]="REQUEST", seg[5]=operation o, seg[6]=timeseal t, seg[7]=client, seg[8]=client <4,5,6,7>,
                        seg[8]=rep i, seg[9]=rep sign<1,2,3,4,5,6,7,8>
                     */
                    if (Long.valueOf(seg[6]) <= SystemInfo.timeseal) {
                        UtilClass.sendMessage(SystemInfo.log_req_OR.get("" + (SystemInfo.request_id - 1)), Integer.valueOf(seg[8]));
                    }
                }else if (seg[1].equals("I-HATE-THE-PRIMARY")){
                    /* <I-HATE-THE-PRIMARY,v>sigi => [, I-HATE-THE-PRIMARY, v, sigi]
                        seg[1]="I-HATE-THE-PRIMARY", seg[2]=current view v, seg[3]=rep sign<1,2>
                     */
                    String hate_data="<"+seg[1]+","+seg[2]+">";
                    String hate_sign=seg[3];
                    verify_result=Schnorr.checkSign(hate_data,hate_sign);
                    if (verify_result){
                        SystemInfo.logViewChange.add(message);
                    }

                }else if (seg[1].equals("VIEW-CHANGE")) {
                    /*  <VIEW-CHANGE,v+1,CC,O,i>sigi => [, VIEW-CHANGE, v+1, CC, O, i, sigi]
                        seg[1]="VIEW-CHANGE", seg[2]=new_view view+1, seg[3]=history of request CC, seg[4]=history of OR, seg[5]=rep i, seg[6]=rep sign<1,2,3,4,5>
                     */
                    String vc_data = "<" + seg[1] + "," + seg[2] + "," + seg[3] + "," + seg[4] + "," + seg[5] + ">";
                    String vc_sign = seg[6];
                    verify_result = Schnorr.checkSign(vc_data, vc_sign);
                    if (verify_result) {
                        if (seg[2].equals("" + (SystemInfo.view + 1))) {
                            SystemInfo.logViewChange.add(message);
                        }
                    }
                }else if (seg[1].equals("NEW-VIEW")) {
                    /* <NEW-VIEW,v+1,P>signp => [, NEW-VIEW, v+1, P, signp]
                        seg[1]="NEW-VIEW", seg[2]=new_view v+1, seg[3]=set of 2f+1 view_change message, seg[4]=new primary sign<1,2,3>
                     */
                    String nv_data = "<" + seg[1] + "," + seg[2] + "," + seg[3] + ">";
                    String nv_sign = seg[4];
                    verify_result = Schnorr.checkSign(nv_data, nv_sign);
                    if (verify_result) {
                        if (seg[2].equals("" + (SystemInfo.view + 1))) {
                            SystemInfo.logViewChange.add(message);
                        }
                    }
                }else if (seg[1].equals("VIEW-CONFIRM")){
                    /* <VIEW-CONFIRM,v+1,n,h,i>sigi => [, VIEW-CONFIRM, v+1, n, h, i, sigi]
                        seg[1]="VIEW-CONFIRM", seg[2]=new view v+1, seg[3]=last request_id n, seg[4]=digest(last request), seg[5]=rep i, seg[6]=rep sign<1,2,3,4,5>
                     */
                    String vco_data = "<" + seg[1] + "," + seg[2] + "," + seg[3] + "," + seg[4] + "," + seg[5] + ">";
                    String vco_sign = seg[6];
                    verify_result=Schnorr.checkSign(vco_data,vco_sign);
                    if (verify_result) {
                        if (seg[2].equals("" + (SystemInfo.view + 1))) {
                            SystemInfo.logViewChange.add(message);
                        }
                    }
                }

                SystemInfo.isReceiving=false;
                SystemInfo.logTransaction.notifyAll();
            }
        }

    }

//--------------------------------CLIENT----------------------------------CLIENT----------------------------------------CLIENT--------------------------------------------CLIENT------------------------------------------
    class Client extends Thread {

        int id = SystemInfo.client_id;//id of client/node
        int view = SystemInfo.view;//preset main node id
        int node_num = SystemInfo.node_num;
        String RequestMessage;
        public Client(String message) {
            //when the message button is clicked, the client will start a request.
            //In onClick the message is collected from the textbox
            this.RequestMessage = message;
        }
        public void run() {
            //Generate REQUEST message
            long t = System.currentTimeMillis();
            String tx = ""+t;//timeseal of the transaction
            ArrayList<String> respondedNode=new ArrayList<>();
            ArrayList<String> committedNode=new ArrayList<>();
            RequestMessage = "<REQUEST," + RequestMessage + "," + tx + "," + id + ">";
            RequestMessage=RequestMessage+Schnorr.sign(RequestMessage);

            String req_hash= UtilClass.digest(RequestMessage);
            ArrayList<String> sr_log=new ArrayList<>();
            SystemInfo.log_timeseal_SR.put(tx,sr_log);
            SystemInfo.reqhash_localcommit.put(req_hash,committedNode);

            UtilClass.sendMessage(RequestMessage, view);//IP_Arr[view], Port_Send_Arr[view]);//1. client send the request
            //Wait for reply
            Timer client_commit_timer=new Timer();
            //client_commit_timer.schedule(new ClientCommitTimer(tx),25*1000,50*1000);//start a commit phase timer (receive 2f+1~3f spec-response)

            Timer client_vc_timer=new Timer();
            client_vc_timer.schedule(new ClientTimer(RequestMessage),3*1000,10*1000);// start a view change timer to  if (receive <2f+1 spec-response)
            boolean timer_vc_on=true;

            while (true) {
                //check if receive more than 2f+1 spec-response, cancel commit phase timer
                if (SystemInfo.log_timeseal_SR.get(tx).size() >= ((node_num - 1) / 3 * 2 + 1)) {
                    //System.out.println("receive response num = "+SystemInfo.log_timeseal_SR.get(tx).size());
                    client_vc_timer.cancel();
                    client_commit_timer.schedule(new ClientCommitTimer(tx),5*1000,50*1000);//start a commit phase timer (receive 2f+1~3f spec-response)
                    timer_vc_on=false;
                }

                //check if receive 3f+1 spec-response, cancel commit phase timer, complete the transaction in fast way
                if (SystemInfo.log_timeseal_SR.get(tx).size() >= node_num) {
                    client_vc_timer.cancel();
                    timer_vc_on=false;
                    client_commit_timer.cancel();
                    SystemInfo.log_timeseal_SR.remove(tx);
                    SystemInfo.reqhash_localcommit.remove(req_hash);
                    break;
                }

                //check if receive 2f+1 local-commit, complete the transaction in commit phase case
                if (SystemInfo.reqhash_localcommit.get(req_hash).size() >= ((node_num - 1) / 3 * 2 + 1)) {
                    client_commit_timer.cancel();
                    client_vc_timer.cancel();
                    timer_vc_on=false;
                    SystemInfo.log_timeseal_SR.remove(tx);
                    SystemInfo.reqhash_localcommit.remove(req_hash);
                    break;
                }

            }
            long t_end=System.currentTimeMillis();
            System.out.println("Transaction completed in "+(t_end-t)+"ms");
        }

    }


    class ClientTimer extends TimerTask{
        String RequestMessage;
        public ClientTimer(String RequestMessage){
            this.RequestMessage=RequestMessage;
        }
        public void run(){
            for (int i = 0; i < SystemInfo.node_num; i++) {
                UtilClass.sendMessage(RequestMessage,i);
            }
        }
    }
    class ClientCommitTimer extends TimerTask{
        String timeseal;
        public ClientCommitTimer(String timeseal){
            this.timeseal=timeseal;
        }
        public void run(){
            ArrayList<String> sr_list=SystemInfo.log_timeseal_SR.get(timeseal);
            String CC="";
            for (String str:sr_list){
                String CC_seg=str.replace("<","{");
                CC_seg=CC_seg.replace(">","}");
                CC_seg=CC_seg.replace(",","|");
                CC=CC+CC_seg+"/";
            }
            /* <COMMIT,c,CC>sigc => [, COMMIT, c, CC, sigc]
               seg[1]="COMMIT", seg[2]=client c, seg[3]=commit certificate CC (spec-response set for the request), seg[4]=client sign<1,2,3>
            */
            String commit="<COMMIT,"+SystemInfo.client_id+","+CC+">";
            String commit_message=commit+Schnorr.sign(commit);
            UtilClass.multicastOther(commit_message);
            UtilClass.sendMessage(commit_message,SystemInfo.node_id);
        }
    }


    class BackupConfirmRequest extends Thread{
        int req_id=SystemInfo.request_id;
        String request;
        public BackupConfirmRequest(String request){
            this.request=request;
        }

        @Override
        public void run() {
            if (SystemInfo.viewChangeFlag){
                return;
            }
            //if backup got a request of new transaction from client, it sends a confirm-request

            //<CONFIRM-REQ,u,m,i>sigi => [, CONFIRM-REQ, u, m, i, sigi]
            String confirm_request = "<CONFIRM-REQ," + SystemInfo.view + "," + request + "," + SystemInfo.node_id + ">";
            String comfirm_request_message = confirm_request + Schnorr.sign(confirm_request);
            UtilClass.sendMessage(comfirm_request_message, SystemInfo.view);
            Timer checkOR = new Timer();
            if (SystemInfo.rep_vc_timer_on){
                return;
            }else {
                checkOR.schedule(new ViewChangeTimer(), 4 * 1010);
            }

            for (int i = 0; i <= 79; i++) {
                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (SystemInfo.logTransaction.containsKey(""+req_id) ) {
                    System.out.println("--- Received OR");
                    checkOR.cancel();
                    return;
                }
            }
        }
    }


    class ViewChangeTimer extends TimerTask{
        @Override
        public void run() {
            if (!(SystemInfo.viewChangeFlag&&SystemInfo.rep_vc_timer_on)){
                SystemInfo.viewChangeFlag=true;
                //SystemInfo.logViewChange.add(RequestMessage);
                SystemInfo.rep_vc_timer_on=true;
                ZyzzyvaViewChange viewChange=new ZyzzyvaViewChange();
                viewChange.start();
                //SystemInfo.rep_vc_timer_on=false;

            }
        }
    }


    class HelpFillHole extends Thread{
        int start_id;
        int end_id;
        int device;
        public HelpFillHole(int start_id,int end_id,int device){
            this.start_id=start_id;
            this.end_id=end_id;
            this.device=device;
        }
        @Override
        public void run() {
            for (int i = start_id; i < end_id; i++) {
                String resend_or = SystemInfo.log_req_OR.get("" + i);
                UtilClass.sendMessage(resend_or, device);
            }
        }
    }

    class RepLocalCommit extends Thread{
        String CC;
        public RepLocalCommit(String CC){
            this.CC=CC;
        }
        public void run(){
            /*
            <<SPEC-RESPONSE,u,n,hn,H(r),c,t>sigi,i,r,OR> => [, , SPEC-RESPONSE, u, n, hn, H(r), c, t, sigi, i, r, , ORDER-REQ, u, n, hn, d, ND, sigp]
            seg[2]="SPEC-RESPONSE", seg[3]=view u, seg[4]=request_id n, seg[5]= history, seg[6]=hash(operation result),
            seg[7]=client, seg[8]=req timeseal, seg[9]=rep sign<2,3,4,5,6,7,8>, seg[10]=rep i, seg[11]=operation result, seg[13]="ORDER-REQ",
            seg[14]=view_OR, seg[15]=request_id_OR, seg[16]=hn_OR, seg[17]=req_digest_OR, seg[18]=ND_OR, seg[19]=primary sign<13,14,15,16,17,18>
            */
            String[] sr_arr=CC.split("/");
            String[] sr_seg=sr_arr[0].split("\\{|\\||\\}");
            System.out.println("sr in localcommit = "+sr_arr[0]);
            if (sr_seg[3].equals(""+SystemInfo.view)){
                if (SystemInfo.log_history_SR.containsKey(sr_seg[5])){
                    if(Integer.valueOf(sr_seg[4])>SystemInfo.cc_id){
                    /* <LOCAL-COMIT,u,d,h,i,c>sigi => [, LOCAL-COMIT, u, d, h, i, c, sigi]
                        seg[1]="LOCAL-COMMIT", seg[2]=view u, seg[3]=hash(request), seg[4]=history, seg[5]=rep i, seg[6]=client, seg[7]=rep sign<1,2,3,4,5,6>
                     */
                        String local_commit="<LOCAL-COMMIT,"+SystemInfo.view +"," +sr_seg[17]+ ","+ sr_seg[5] +","+SystemInfo.node_id+","+sr_seg[7]+">";
                        String local_commit_message=local_commit+Schnorr.sign(local_commit);
                        UtilClass.sendMessage(local_commit_message,Integer.valueOf(sr_seg[7]));
                        SystemInfo.cc_id=Integer.valueOf(sr_seg[4]);
                    }
                }else if (Integer.valueOf(sr_seg[4])>SystemInfo.request_id-1){
                    RepFillHole repFillHole = new RepFillHole(sr_seg[4]);
                    repFillHole.start();
                }

            }
        }
    }

    class RepFillHole extends Thread{
        //boolean isSendToPrimary;
        int received_req_id;
        int current_req_max=SystemInfo.request_id-1;
        public RepFillHole(String received_req_id){
            this.received_req_id=Integer.valueOf(received_req_id);
        }

        public  void run(){
            /*<FILL-HOLE,u,maxn+1,n,i>sigi => [, FILL-HOLE, u, maxn+1, n, i, sigi]
              seg[1]="FILL-HOLE", seg[2]=view u, seg[3]=rep lost request_id (rep max request_id+1), seg[4]=request_id, seg[5]=rep, seg[6]=rep sign<1,2,3,4>
            */
            String fill_hole="<FILL-HOLE,"+SystemInfo.view+","+SystemInfo.request_id+","+ received_req_id+"," +SystemInfo.node_id+">";
            String fill_hole_message=fill_hole+Schnorr.sign(fill_hole);

            UtilClass.sendMessage(fill_hole_message,SystemInfo.view);
            try {
                for (int i=0;i<=5;i++){// wait 3s for OR from primary
                    sleep(500);
                    if (SystemInfo.logTransaction.containsKey(current_req_max+1)){
                        return;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            UtilClass.multicastOther(fill_hole_message);
            Timer viewChangetimer=new Timer();

            if (SystemInfo.rep_vc_timer_on){
                return;
            }else {
                viewChangetimer.schedule(new ViewChangeTimer(), 3*1010);
            }

            try {
                for (int i=0;i<=5;i++){// wait 3s for OR from reps
                    sleep(500);
                    if (SystemInfo.logTransaction.containsKey(current_req_max+1)||SystemInfo.viewChangeFlag){
                        viewChangetimer.cancel();
                        return;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class Zyzzyva extends Thread {
        String req_id;
        boolean prepared=false;
        boolean committed=false;
        String t_req;
        String cl_req;
        public Zyzzyva(String req_id){
            this.req_id=req_id;
        }
        @Override
        public void run() {
            if (SystemInfo.viewChangeFlag){
                return;
            }

            ArrayList<String> tx_list = SystemInfo.logTransaction.get(req_id);// logTransaction only record REQUEST and ORDER-REQ
            /*
                <REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                seg[1]=REQUEST, seg[2]=operation o, seg[3]=req timeseal, seg[4]= client, seg[5]=sigc

                <<ORDER-REQ,u,n,hn,d,ND>sigp,m> => [, , ORDER-REQ, u, n, hn, d, ND, sigp, , REQUEST, o, t, c, sigc]
                seg[2]="ORDER-REQ", seg[3]=view u , seg[4]=request_id n, seg[5]= hash(hash(all req)+hash(this req)),
                seg[6]=digest of the req hash(req), seg[7]=ND undetermined operation value, seg[8]=primary sign<2,3,4,5,6,7>,
                seg[10]="REQUEST", seg[11]=operation o , seg[12]=timeseal t, seg[13]=client_id, seg[14]=client sign<10,11,12,13>
            */
            ArrayList<String> OR_list=UtilClass.findInLog(tx_list,"ORDER-REQ");
            if (OR_list.size()==0) {//primary case
                ArrayList<String> req_list = UtilClass.findInLog(tx_list, "REQUEST");
                String req = req_list.get(0);
                String[] req_seg = req.split("<|,|>");

                String d = UtilClass.digest(req);
                SystemInfo.History= UtilClass.digest(SystemInfo.History + d);
                SystemInfo.logHistory.add(SystemInfo.History);
                String ND = "ND_" + req_id;
                String order_request = "<ORDER-REQ," + SystemInfo.view + "," + req_id + "," + SystemInfo.History + "," + d + "," + ND + ">";
                String order_request_sign = Schnorr.sign(order_request);
                String order_request_message = "<" + order_request + order_request_sign + "," + req + ">";
                UtilClass.multicastOther(order_request_message);
                SystemInfo.log_req_OR.put(req_id,order_request_message);

                /*
                <<SPEC-RESPONSE,u,n,hn,H(r),c,t>sigi,i,r,OR> => [, , SPEC-RESPONSE, u, n, hn, H(r), c, t, sigi, i, r, , ORDER-REQ, u, n, hn, d, ND, sigp]
                seg[2]="SPEC-RESPONSE", seg[3]=view u, seg[4]=request_id n, seg[5]= history, seg[6]=hash(operation result),
                seg[7]=client, seg[8]=req timeseal, seg[9]=rep sign<2,3,4,5,6,7,8>, seg[10]=rep i, seg[11]=operation result, seg[13]="ORDER-REQ",
                seg[14]=view_OR, seg[15]=request_id_OR, seg[16]=hn_OR, seg[17]=req_digest_OR, seg[18]=ND_OR, seg[19]=primary sign<13,14,15,16,17,18>
                */
                String operation_result = "result_" + req_id;
                String H_result = UtilClass.digest(operation_result);
                String spec_response = "<SPEC-RESPONSE," + SystemInfo.view + "," + req_id + "," + SystemInfo.History + "," + H_result + "," + req_seg[4] + "," + req_seg[3] + ">";
                String spec_response_sign = Schnorr.sign(spec_response);
                String spec_response_message = "<" + spec_response + spec_response_sign + "," + SystemInfo.node_id + "," + operation_result + "," + order_request+order_request_sign + ">";
                SystemInfo.log_history_SR.put(SystemInfo.History,spec_response_message);
                UtilClass.sendMessage(spec_response_message, Integer.valueOf(req_seg[4]));
                if (!SystemInfo.log_timeseal_SR.containsKey(req_seg[3])){
                    ArrayList<String> sr_log= new ArrayList<>();
                    //sr_log.add(spec_response_message);
                    SystemInfo.log_timeseal_SR.put(req_seg[3],sr_log);
                }//else {
                   // SystemInfo.log_timeseal_SR.get(req_seg[3]).add(spec_response_message);
                //}
                SystemInfo.timeSeal_log.add(req_seg[3]);
            }else{//rep case
                String or=OR_list.get(0);
                String[] or_seg=or.split("<|,|>");
                /*
                    <<ORDER-REQ,u,n,hn,d,ND>sigp,m> => [, , ORDER-REQ, u, n, hn, d, ND, sigp, , REQUEST, o, t, c, sigc]
                    seg[2]="ORDER-REQ", seg[3]=view u , seg[4]=request_id n, seg[5]= hash(hash(all req)+hash(this req)),
                    seg[6]=digest of the req hash(req), seg[7]=ND undetermined operation value, seg[8]=primary sign<2,3,4,5,6,7>,
                    seg[10]="REQUEST", seg[11]=operation o , seg[12]=timeseal t, seg[13]=client_id, seg[14]=client sign<10,11,12,13>
                */
                String req_data = "<" + or_seg[10] + "," + or_seg[11] + "," + or_seg[12] + "," + or_seg[13] + ">";
                String req_sign = or_seg[14];
                String operation_result = "result_" + req_id;
                SystemInfo.History=UtilClass.digest(SystemInfo.History+UtilClass.digest(req_data+req_sign));
                SystemInfo.logHistory.add(SystemInfo.History);

                String H_result = UtilClass.digest(operation_result);
                String spec_response = "<SPEC-RESPONSE," + SystemInfo.view + "," + req_id + "," + SystemInfo.History + "," + H_result + "," + or_seg[13] + "," + or_seg[12] + ">";
                String spec_response_sign = Schnorr.sign(spec_response);
                String or_without_m="<"+or_seg[2]+","+or_seg[3]+","+or_seg[4]+","+or_seg[5]+","+or_seg[6]+","+or_seg[7]+">"+or_seg[8];
                String spec_response_message = "<" + spec_response + spec_response_sign + "," + SystemInfo.node_id + "," + operation_result + "," + or_without_m + ">";
                SystemInfo.log_history_SR.put(SystemInfo.History,spec_response_message);
                UtilClass.sendMessage(spec_response_message, Integer.valueOf(or_seg[13]));
                if (!SystemInfo.log_timeseal_SR.containsKey(or_seg[12])){
                    ArrayList<String> sr_log= new ArrayList<>();
                    //sr_log.add(spec_response_message);
                    SystemInfo.log_timeseal_SR.put(or_seg[12],sr_log);
                }//else {
                   // SystemInfo.log_timeseal_SR.get(or_seg[12]).add(spec_response_message);
                //}
                SystemInfo.timeSeal_log.add(or_seg[12]);
            }



            if (SystemInfo.request_id>=10){
                SystemInfo.logTransaction.remove((""+(SystemInfo.request_id-10)));
                SystemInfo.timeSeal_log.remove(0);
                SystemInfo.log_req_OR.remove(SystemInfo.request_id-10);
                String removedHist=SystemInfo.logHistory.get(0);
                SystemInfo.log_history_SR.remove(removedHist);
                SystemInfo.logHistory.remove(0);
            }
        }
    }


    class ZyzzyvaViewChange extends Thread{
       /* String request_message;
        public ZyzzyvaViewChange(String request_message){
            this.request_message=request_message;
        }*/
        public void run(){
            System.out.println("## Start View Change");

            //broadcast hate message
            String hate="<I-HATE-THE-PRIMARY,"+SystemInfo.view+">";
            String hate_message=hate+Schnorr.sign(hate);
            UtilClass.multicastOther(hate_message);

            /*  <I-HATE-THE-PRIMARY,v>sigi => [, I-HATE-THE-PRIMARY, v, sigi]
                <VIEW-CHANGE,v+1,CC,O,i>sigi => [, VIEW-CHANGE, v+1, CC, O, i, sigi]
                <NEW-VIEW,v+1,P>signp => [, NEW-VIEW, v+1, P, signp]
                <VIEW-CONFIRM,v+1,n,h,i>sigi => [, VIEW-CONFIRM, v+1, n, h, i, sigi]
            */


            //check hate message
            while (true) {
                if (SystemInfo.isReceiving){
                    continue;
                }
                ArrayList<String> hate_list = UtilClass.findInLog(SystemInfo.logViewChange, "<I-HATE-THE-PRIMARY");
                if (hate_list.size() >= (SystemInfo.node_num - 1) / 3 + 1) {
                    /* <VIEW-CHANGE,v+1,CC,O,i>sigi => [, VIEW-CHANGE, v+1, CC, O, i, sigi]

                     */
                    String CC = SystemInfo.log_history_SR.get(SystemInfo.History);
                    SystemInfo.viewChangeFlag = true;
                    if(CC != null) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        CC = SystemInfo.log_history_SR.get(SystemInfo.History);
                        CC = CC.replace("<", "{");
                        CC = CC.replace(",", "|");
                        CC = CC.replace(">", "}");
                    }else {
                        CC="";
                    }

                    String OR = SystemInfo.log_req_OR.get("" + (SystemInfo.request_id - 1));
                    if(OR!=null){
                        OR = OR.replace("<", "{");
                        OR = OR.replace(",", "|");
                        OR = OR.replace(">", "}");
                    }else {
                        OR="";
                    }

                    String view_change = "<VIEW-CHANGE," + (SystemInfo.view + 1) + "," + CC + "," + OR + "," + SystemInfo.node_id + ">";
                    String view_change_message = view_change + Schnorr.sign(view_change);
                    SystemInfo.logViewChange.add(view_change_message);
                    UtilClass.multicastOther(view_change_message);
                    break;
                }
            }
            if (SystemInfo.node_id==SystemInfo.view+1){
                while (true) {
                    if (SystemInfo.isReceiving){
                        continue;
                    }
                    ArrayList<String> vc_list = UtilClass.findInLog(SystemInfo.logViewChange, "<VIEW-CHANGE,");

                    /*  <VIEW-CHANGE,v+1,CC,O,i>sigi => [, VIEW-CHANGE, v+1, CC, O, i, sigi]
                    [, VIEW-CHANGE, v+1, , , SPEC-RESPONSE, u, n, hn, H(r), c, t, sigi, i, r, , ORDER-REQ, u, n, hn, d, ND, sigp, , , , ORDER-REQ, u, n, hn, d, ND, sigp, , REQUEST, o, t, c, sigc, , i, sigi]
                    seg[1]="VIEW-CHANGE", seg[2]=new view view+1, seg[5]="SPEC-RESPONSE",seg[6]=view, seg[7]=history, seg[8]=digest(result), seg[9]=client, seg[10]=request timeseal, seg[11]=rep sign<5,6,7,8,9,10>
                    seg[11]=rep i, seg[12]=operation result, seg[14]="ORDER-REQ", seg[15]=
                     */
                    if (vc_list.size() >= (SystemInfo.node_num - 1) * 2 / 3 + 1) {
                        String P = "";
                        for (String str : vc_list) {
                            String vc = str.replace("<", "{");
                            vc = vc.replace(",", "|");
                            vc = vc.replace(">", "}");
                            P = P + vc + "/";
                        }

                        String new_view = "<NEW-VIEW," + (SystemInfo.view + 1) + "," + P + ">";
                        String new_view_message = new_view + Schnorr.sign(new_view);
                        SystemInfo.logViewChange.add(new_view_message);
                        UtilClass.multicastOther(new_view_message);
                        break;
                    }
                }
            }

            while (true){
                if (SystemInfo.isReceiving){
                    continue;
                }
                ArrayList<String> nv_list = UtilClass.findInLog(SystemInfo.logViewChange, "<NEW-VIEW,");
                if (nv_list.size()!=0){
                    String nv=nv_list.get(0);
                    String[] nv_seg=nv.split("<|,|>");
                    /*  [, NEW-VIEW, v+1, P, signp]
                     */
                    String P=nv_seg[3];
                    String P_seg[]=P.split("/");
                    String sr_or=P_seg[0];
                    String[] sr_or_arr=sr_or.split("\\{\\{|\\}\\}");
                    String hn="";
                    if (sr_or_arr.length==0){
                        hn="";
                    }else {
                        for (String str:sr_or_arr){
                            if (str.contains("ORDER-REQ")){
                                String[] or_seg=str.split("\\|");
                                System.out.println("P contains or = "+str);
                                hn=or_seg[3];
                                break;
                            }
                        }
                    }

                    System.out.println("P_or_seg = "+P_seg[0]);

                    String view_confirm="<VIEW-CONFIRM,"+(SystemInfo.view+1)+","+(SystemInfo.request_id-1)+","+hn+","+SystemInfo.node_id+">";
                    String view_confirm_message=view_confirm+Schnorr.sign(view_confirm);
                    SystemInfo.logViewChange.add(view_confirm_message);
                    UtilClass.multicastOther(view_confirm_message);
                    break;
                }
            }

            while (true) {
                if (SystemInfo.isReceiving){
                    continue;
                }
                ArrayList<String> v_co=UtilClass.findInLog(SystemInfo.logViewChange,"<VIEW-CONFIRM");
                if (v_co.size()>=(SystemInfo.node_num - 1) * 2 / 3 + 1){
                    /* <VIEW-CONFIRM,v+1,n,h,i>sigi => [, VIEW-CONFIRM, v+1, n, h, i, sigi]
                        seg[4]=new_history
                     */
                    String[] v_co_seg=v_co.get(0).split("<|,|>");
                    SystemInfo.History=v_co_seg[4];
                    SystemInfo.view++;
                    SystemInfo.viewChangeFlag=false;
                    SystemInfo.rep_vc_timer_on=false;
                    SystemInfo.logViewChange.clear();
                    System.out.println("## NEW VIEW = "+SystemInfo.view+" history = "+SystemInfo.History);
                    break;
                }
            }
        }
    }


}

