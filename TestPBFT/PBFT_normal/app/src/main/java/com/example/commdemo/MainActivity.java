package com.example.commdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Date;
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

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        tv = (TextView) findViewById(R.id.text_view3);
        tv.setBackgroundColor(Color.WHITE);
        Button button1 = (Button) findViewById(R.id.button_send);
        Button button2 = (Button) findViewById(R.id.button_intialize);
        editText = (EditText) findViewById(R.id.edit_text);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        tv.append("Mobile Device ID: " + SystemInfo.client_id+"\n");
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        ReceiveThread receiver = new ReceiveThread(SystemInfo.Port_Arr[SystemInfo.node_id]);
        receiver.start();
        tv.append("IP = "+SystemInfo.IP_Arr[SystemInfo.node_id] +
                "     Listening Port = " + SystemInfo.Port_Arr[SystemInfo.node_id]+"\n"+"System View = Device "+SystemInfo.view+"\n");
        tv.append("Please initialize system");
        System.out.println("This is Device = "+SystemInfo.node_id);
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
                String inputText = editText.getText().toString();//get the text in textline
                //Client client = new Client(inputText);
                //client.start();
                TestScript testScript=new TestScript(1000,100,1000);
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
        int time_interval;
        public TestScript(int transaction_data_size,int transaction_num,int time_interval){
            this.transaction_data_size=transaction_data_size;
            this.transaction_num=transaction_num;
            this.time_interval=time_interval;
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

            for (int i=0;i<transaction_num;i++){
                Client client=new Client(s,i);
                client.start();
                try {
                    Thread.sleep(time_interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (client.isAlive()){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("#### "+(i+1)+" transaction completed, "+(transaction_num-i-1)+"left");
            }
            long end_time = System.currentTimeMillis();
            System.out.println("##Test completed:"+"\n"+ "Transaction Number = "+transaction_num
                    +"   Message size = "+transaction_data_size+"byte, "+"Test time cost = "+(end_time-start_time)+"ms");
            Log.i("tx","Test End: Data Size = "+transaction_data_size+", tx_num = "+transaction_num+", time cost = "+(end_time-start_time)+"ms");
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
                        //System.out.println("---- Receive: " + message );
                        messageResolve(message);
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
                KEY,publickey,KEYof_id => [KEY, publickey, KEYof_id]
                <REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                <<PRE-PREPARE,v,n,d>sigp,<REQUEST,o,t,c>sigc> => [, , PRE-PREPARE, v, n, d, sigp, , REQUEST, o, t, c, sigc]
                <PREPARE,v,n,d,i>sigi => [, PREPARE, v, n, d, i, sigi]
                <COMMIT,v,n,Dm,i>sigi => [, COMMIT, v, n, Dm, i, sigi]
                <REPLY,v,t,c,i,r>sigi => [, REPLY, v, t, c, i, r, sigi]
                <VIEW-CHANGE,v+1,n,c,p,i>sigi => [, VIEW-CHANGE, v+1, n, c, p, i, sigi]
                <NEW-VIEW,v+1,V,O>signp => [, NEW-VIEW, v+1, V, O, signp]
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
                }else if (seg[1].equals("REQUEST")) {
                    if (SystemInfo.node_id!=SystemInfo.view){
                        //System.out.println("I am not the primary");
                        if (SystemInfo.logCommitted.containsKey(seg[3])){
                            String resend_reply=SystemInfo.logCommitted.get(seg[3]);
                            UtilClass.sendMessage(resend_reply,Integer.valueOf(seg[4]));
                        }else if (SystemInfo.timeSeal_log.contains(seg[3])) {
                            //System.out.println("I got the timeseal "+seg[3]);
                            return;
                        }else{
                            //System.out.println("I don't have the timeseal and I should forward it");
                            backupFwdRequest(message);
                            if (!SystemInfo.vc_timer_on){
                                SystemInfo.vc_timer_on=true;
                                backuptimer.schedule(new BackupTimer(message),4*1000);
                            }
                        }
                        return;
                    }
                    //Primary Case
                    //extract the data
                /*<REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                    seg[1]="REQUEST", seg[2]=operation o , seg[3]=timeseal t, seg[4]=client_id,
                    seg[5]=client_sign for <seg[1],seg[2],seg[3],seg[4]>
                 */
                    //System.out.println("I AM the Primary!");
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+">";
                    String sign=seg[5];
                    //check if this is the first request message for the operation
                    //Here we need a method to check the element in message record
                    verify_result = Schnorr.checkSign(data,sign);
                    if (verify_result) {
                        //check if it is a new transaction
                        if(SystemInfo.timeSeal_log.contains(seg[3])){
                            //System.out.println("I got the timeseal! "+seg[3]);
                            return;
                        }
                        if (UtilClass.isTimeToSleepInViewChangeTest()){
                            System.out.println("I need to sleep for view change check");
                            //SystemInfo.timeSeal_log.add("fill");
                            return;
                        }

                /*
                if (SystemInfo.primaryHistory_reqTimeToN.containsKey(seg[3])){
                    return;
                }else{
                    SystemInfo.primaryHistory_reqTimeToN.put(seg[3],""+SystemInfo.request_id); //hashmap: timeseal->request_id
                    ArrayList<String> mlog=new ArrayList<>();
                    mlog.add(message);
                    SystemInfo.logTransaction.put(""+SystemInfo.request_id,mlog);//hashmap: n0-> list:[request]
                    PBFT pbft=new PBFT(""+SystemInfo.request_id);
                    pbft.start();
                }

                 */
                        //SystemInfo.primaryHistory_reqTimeToN.put(seg[3],""+SystemInfo.request_id); //hashmap: timeseal->request_id
                        ArrayList<String> mlog=new ArrayList<>();
                        mlog.add(message);
                        SystemInfo.logTransaction.put(""+SystemInfo.request_id,mlog);//hashmap: n0-> list:[request]
                        //System.out.println("I give the request id = "+SystemInfo.request_id);
                        PBFT pbft=new PBFT(""+SystemInfo.request_id);
                        pbft.start();
                        SystemInfo.timeSeal_log.add(seg[3]);
                        SystemInfo.request_id++;
                    }
                } else if (seg[2].equals("PRE-PREPARE")) {
                /*<<PRE-PREPARE,v,n,d>sigp,<REQUEST,o,t,c>sigc> => [, , PRE-PREPARE, v, n, d, sigp, , REQUEST, o, t, c, sigc]
                    seg[2]="PRE-PREPARE", seg[3]=view v , seg[4]=request_id n, seg[5]=digest(request_with_sign), seg[6]=primary_sign for <2,3,4,5>,
                    seg[8]="REQUEST", seg[9]=operation o , seg[10]=timeseal t, seg[11]=client_id, seg[12]=sign_client for <8,9,10,11>
                 */

                    String p_p_data="<"+seg[2]+","+seg[3]+","+seg[4]+","+seg[5]+">";
                    String p_p_sign=seg[6];
                    String req_data="<"+seg[8]+","+seg[9]+","+seg[10]+","+seg[11]+">";
                    String req_sign=seg[12];
                    verify_result=(Schnorr.checkSign(p_p_data,p_p_sign)&&Schnorr.checkSign(req_data,req_sign));
                    if (verify_result){
                        //check if it is a new transaction
                        boolean check_d_v=(seg[5].equals(UtilClass.digest(req_data+seg[12]))&&seg[3].equals(""+SystemInfo.view));
                        if (check_d_v){
                            backuptimer.cancel();
                            if(SystemInfo.logTransaction.containsKey(seg[4])){//if the transaction_req n has been created a message log, which also means the PBFT thread for the transaction has been started
                                SystemInfo.logTransaction.get(seg[4]).add(message);//just add the new message into the log
                            }else {//if the transaction_req n hasn't created a message log

                                ArrayList<String> mlog=new ArrayList<>();//new the message log and add the message into the log
                                mlog.add(message);
                                SystemInfo.logTransaction.put(seg[4],mlog);
                                //SystemInfo.timeSeal_reqId.put(seg[10],seg[4]);
                                SystemInfo.timeSeal_log.add(seg[10]);
                                SystemInfo.request_id=Integer.valueOf(seg[4]);
                                //System.out.println("this request id = "+SystemInfo.request_id);
                                PBFT pbft=new PBFT(seg[4]);
                                pbft.start();
                            }
                        }else {
                            System.out.println("## Err: Failure in checking Pre-prepare digest and view");
                        }
                    }
                }else if (seg[1].equals("PREPARE")) {
                /* <PREPARE,v,n,d,i>sigi => [, PREPARE, v, n, d, i, sigi]
                    seg[1]="PREPARE", seg[2]=view v, seg[3]=request_id n, sig[4]=digest(request with sign), seg[5]=prepare_from, seg[6]=rep_sign for <1,2,3,4,5>
                 */
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+","+seg[5]+">";
                    String sign=seg[6];
                    verify_result = Schnorr.checkSign(data,sign);
                    if(verify_result){
                        boolean check_v=seg[2].equals(""+SystemInfo.view);
                        if (check_v){
                            if (SystemInfo.logTransaction.containsKey(seg[3])){
                                SystemInfo.logTransaction.get(seg[3]).add(message);
                            //}else if(Integer.valueOf(seg[3])<SystemInfo.request_id) {
                               // return;
                            }else {
                                ArrayList<String> mlog=new ArrayList<>();//new the message log and add the message into the log
                                mlog.add(message);
                                SystemInfo.logTransaction.put(seg[3],mlog);
                                PBFT pbft= new PBFT(seg[3]);
                                pbft.start();
                            }
                        }else {
                            System.out.println("## Err: Failure in checking Prepare view");
                        }
                    }
                } else if (seg[1].equals("COMMIT")) {
                /*  <COMMIT,v,n,Dm,i>sigi => [, COMMIT, v, n, Dm, i, sigi]
                    seg[1]="COMMIT", seg[2]=view v, seg[3]=request_id, seg[4]=digest(request with sign), seg[5]=commit from, seg[6]=rep_sign for <1,2,3,4,5>
                 */
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+","+seg[5]+">";
                    String sign=seg[6];

                    verify_result = Schnorr.checkSign(data,sign);
                    if (verify_result) {
                        boolean check_v=seg[2].equals(""+SystemInfo.view);
                        if (check_v){
                            if (SystemInfo.logTransaction.containsKey(seg[3])){
                                SystemInfo.logTransaction.get(seg[3]).add(message);
                            //}else if (Integer.valueOf(seg[3])<SystemInfo.request_id) {
                              //  return;
                            }else {
                                ArrayList<String> mlog=new ArrayList<>();//new the message log and add the message into the log
                                mlog.add(message);
                                SystemInfo.logTransaction.put(seg[3],mlog);
                                PBFT pbft=new PBFT(seg[3]);
                                pbft.start();
                            }
                        }else {
                            //System.out.println(message);
                            //System.out.println(seg[2]+ "  "+SystemInfo.view);
                            System.out.println("## Err: Failure in checking Commit view");
                        }
                    }
                } else if (seg[1].equals("REPLY")) {
                /* <REPLY,v,t,c,i,r>sigi => [, REPLY, v, t, c, i, r, sigi]
                    seg[1]="REPLY", seg[2]=view v, seg[3]=req timeseal t, seg[4]=req client_id, seg[5]=reply from, seg[6]=operation result, seg[7]=rep sign for <1,2,3,4,5,6>
                 */
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+","+seg[5]+","+seg[6]+">";
                    String sign=seg[7];
                    verify_result = Schnorr.checkSign(data,sign);
                    if (verify_result) {
                        if(SystemInfo.timeSeal_reply.containsKey(seg[3])){
                            int new_reply_num=Integer.valueOf(SystemInfo.timeSeal_reply.get(seg[3]))+1;
                            SystemInfo.timeSeal_reply.put(seg[3],new_reply_num);//reply_num++;
                            //System.out.println("##reply num = "+SystemInfo.reply_num);
                        }//else {
                            //System.out.println("## reply timeseal = "+ seg[3]+" != "+"system timeseal = "+SystemInfo.timeSeal);
                        //}
                    }
                }else if (seg[1].equals("VIEW-CHANGE")){
                /* <VIEW-CHANGE,v+1,n,c,p,i>sigi => [, VIEW-CHANGE, v+1, n, c, p, i, sigi]
                   seg[1]="VIEW-CHANGE", seg[2]=new_view v+1, seg[3]=last_completed_request_id , seg[4]= req_  , seg[5]= previous_message_set_after_n , seg[6]=view_change from, seg[7]= rep sign <1,2,3,4,5>
                 */
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+","+seg[5]+","+seg[6]+">";
                    String sign=seg[7];
                    verify_result=Schnorr.checkSign(data,sign);
                    if (verify_result){
                        if (UtilClass.isTimeToSleepInViewChangeTest()){
                            if (Integer.valueOf(seg[2])-1==SystemInfo.node_id){
                                return;
                            }
                        }
                        if (seg[2].equals(""+(SystemInfo.view+1)%SystemInfo.node_num)) {
                            SystemInfo.logViewChange.add(message);
                        }
                    }
                }else if (seg[1].equals("NEW-VIEW")) {
                /* <NEW-VIEW,v+1,V,O>signp => [, NEW-VIEW, v+1, V, O, signp]
                    seg[1]="NEW-VIEW", seg[2]=new_view v+1, seg[3]=2f+1_viewchange_message_digest, seg[4]=pre_prepare_message_set, seg[5]=rep_sign for <1,2,3,4>
                 */
                    //[, NEW-VIEW, 1, {V}, , PRE-PREPARE, 2, 1, 37D6D93879A23574395BAE25FACED4B36BE9DBAB1C745EFD9E57501D1E806C76, , 1:ABCE4027F3D9DF5F3F69ED4149390B4EDF639981EE61577DA3706C025B404268F68B54C9BA483A5E6CAC4DE332A18E698B51E3F0E54097AC93AA9789E29F2073]
                    String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+"<"+seg[5]+","+seg[6]+","+seg[7]+","+seg[8]+">>";
                    String sign=seg[10];
                    verify_result=Schnorr.checkSign(data,sign);
                    if (verify_result){
                        if (UtilClass.isTimeToSleepInViewChangeTest()){
                            if (Integer.valueOf(seg[2])==(SystemInfo.node_id+1)%4){
                                SystemInfo.view=(SystemInfo.view+1)%SystemInfo.node_num;
                                System.out.println("I am sleeping but I got new view = "+SystemInfo.view);
                                return;
                            }
                        }
                        if (seg[2].equals(""+(SystemInfo.view+1)%SystemInfo.node_num)) {
                            SystemInfo.logViewChange.add(message);
                        }
                    }
                }
                SystemInfo.isReceiving=false;
                SystemInfo.logTransaction.notifyAll();
            }


        }

        public void backupFwdRequest(String REQUESTmessage) {
            //the fwdRequest has got signature so we don't need  add the signature any more
            UtilClass.sendMessage(REQUESTmessage,SystemInfo.view);
        }
    }

//--------------------------------CLIENT----------------------------------CLIENT----------------------------------------CLIENT--------------------------------------------CLIENT------------------------------------------
    class Client extends Thread {
        int i;
        int id = SystemInfo.client_id;//id of client/node
        int view = SystemInfo.view;//preset main node id
        int node_num = SystemInfo.node_num;
        String RequestMessage;
        public Client(String message, int i) {
            //when the message button is clicked, the client will start a request.
            //In onClick the message is collected from the textbox
            this.RequestMessage = message;
            this.i=i;
        }
        public void run() {
            //Generate REQUEST message
            long t = System.currentTimeMillis();
            SystemInfo.timeSeal_reply.put(""+t,0);
            RequestMessage = "<REQUEST," + RequestMessage + "," + t + "," + id + ">";
            RequestMessage=RequestMessage+Schnorr.sign(RequestMessage);
            //System.out.println("Signed: "+RequestMessage);
            UtilClass.sendMessage(RequestMessage, view);//IP_Arr[view], Port_Send_Arr[view]);
            //System.out.println("## Client sent Request");

            //Wait for reply

            Timer clienttimer=new Timer();
            clienttimer.schedule(new ClientTimer(RequestMessage),3*1000,11*1000);
            //System.out.println("## Client working");
            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if  (SystemInfo.timeSeal_reply.containsKey(""+t) && SystemInfo.timeSeal_reply.get(""+t)>0){
                    clienttimer.purge();
                    clienttimer.cancel();
                }
                //System.out.println("Client checking reply_num : "+ (SystemInfo.reply_num > ((node_num - 1) / 3) +2)
                  //      +" reply_num = "+SystemInfo.reply_num+" (node_num-1)/3+2 = "+(((node_num - 1) / 3) +2));
                if (SystemInfo.timeSeal_reply.containsKey(""+t) && SystemInfo.timeSeal_reply.get(""+t) >= ((node_num - 1) / 3) +2) {
                    //long t_end=System.currentTimeMillis();
                    //System.out.println("## Client receive enough reply");
                    //showReceivedText("## Operation Completed: Consensus on the Transaction in "+(t_end-t)+"ms");
                    //System.out.println("## Operation Completed: Consensus on the Transaction in "+(t_end-t)+"ms");
                    //UtilClass.ClientReset();
                    SystemInfo.timeSeal_reply.remove(""+t);
                    //SystemInfo.client_test_count++;
                    break;
                }

                //System.out.println("finish this check reply_num : "+ (SystemInfo.reply_num > ((node_num - 1) / 3) +2));
            }
             /*   while(SystemInfo.reply_num < ((node_num - 1) / 3) +2){
                    try {
                        Thread.currentThread().wait(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
             long end_transaction=System.currentTimeMillis();
            Log.i("tx","tx = "+i+ ", delay = "+ (end_transaction-t));
            System.out.println("##Clientend");
            //Request have been responded.
            // Add code to Record it to log
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

    class BackupTimer extends TimerTask{
        String RequestMessage;
        public BackupTimer(String RequestMessage){
            this.RequestMessage=RequestMessage;
        }
        public void run(){
            if (!SystemInfo.viewChangeFlag){
                SystemInfo.viewChangeFlag=true;
                SystemInfo.logViewChange.add(RequestMessage);
                ViewChange viewChange=new ViewChange(RequestMessage);
                viewChange.start();
                SystemInfo.vc_timer_on=false;
            }
        }
    }

    class PBFT extends Thread {
        String req_id;
        boolean prepared=false;
        boolean committed=false;
        String t_req;
        String cl_req;
        public PBFT(String req_id){
            this.req_id=req_id;
        }
        @Override
        public void run() {
            SystemInfo.logTransaction.get(req_id);
            ArrayList<String> req_or_pp;
            //System.out.println("## Start PBFT 3-phase-protocol for req "+req_id);
            //synchronized (SystemInfo.logTransaction){
                while (true){
                    if(SystemInfo.viewChangeFlag==true){ System.out.println("PBFT Interrupt");return; }
                    if (SystemInfo.isReceiving){ continue; }
                    if (!SystemInfo.logTransaction.containsKey(req_id)){
                        continue;
                    }else {
                        req_or_pp=UtilClass.findInLog(SystemInfo.logTransaction.get(req_id),"<REQUEST");
                    }

                    if (req_or_pp.size()!=0){
                        if(req_or_pp.get(0).contains("PRE-PREPARE")){//Backup got pre-prepare, send prepare
                            //System.out.println("----- to send Prepare");
                        /*<<PRE-PREPARE,v,n,d>sigp,<REQUEST,o,t,c>sigc> => [, , PRE-PREPARE, v, n, d, sigp, , REQUEST, o, t, c, sigc]
                        seg[2]="PRE-PREPARE", seg[3]=view v , seg[4]=request_id n, seg[5]=digest(request_with_sign), seg[6]=primary_sign for <2,3,4,5>,
                        seg[8]="REQUEST", seg[9]=operation o , seg[10]=timeseal t, seg[11]=client_id, seg[12]=sign_client for <8,9,10,11>
                        */
                            String[] pp = req_or_pp.get(0).split("<|,|>");
                            String v=pp[3];
                            String n=pp[4];
                            String d=pp[5];
                            String o=pp[9];
                            t_req=pp[10];
                            cl_req=pp[11];
                            String prepare="<PREPARE,"+SystemInfo.view+","+n+","+d+","+SystemInfo.node_id+">";//<PREPARE,v,n,d,i>
                            String sign_prepare=Schnorr.sign(prepare);
                            String prepare_message=prepare+sign_prepare;
                            UtilClass.multicastOther(prepare_message);//send prepare
                        }else{//Primary got request, send pre-prepare
                        /*<REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
                        //seg[1]="REQUEST", seg[2]=operation o , seg[3]=timeseal t, seg[4]=client_id,
                        //seg[5]=client_sign for <seg[1],seg[2],seg[3],seg[4]>
                        */
                            //System.out.println("----- to send Pre-prepare");
                            String[] req=req_or_pp.get(0).split("<|,|>");
                            String o=req[2];
                            t_req=req[3];
                            cl_req=req[4];
                            String digest_req=UtilClass.digest(req_or_pp.get(0));
                            //System.out.println("---- digest: "+req_or_pp.get(0));
                            String pre_prepare="<PRE-PREPARE,"+SystemInfo.view+","+req_id+","+digest_req+">";
                            String sign_pre_prepare=Schnorr.sign(pre_prepare);
                            String pre_prepare_message="<"+pre_prepare+sign_pre_prepare+","+req_or_pp.get(0)+">";
                            UtilClass.multicastOther(pre_prepare_message);
                            //send pre-prepare
                        }
                        //System.out.println("## Pre-prepared");
                        break;
                    }
                    try {
                        //SystemInfo.logTransaction.wait();
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }//pre-prepare end

                //Prepare
                ArrayList<String> p_list;
                while (true){
                    if(SystemInfo.viewChangeFlag==true){ System.out.println("## PBFT Interrupt"); return; }
                    if (SystemInfo.isReceiving){ continue; }
                    if (!SystemInfo.logTransaction.containsKey(req_id)){
                        continue;
                    }else {
                        p_list=UtilClass.findInLog(SystemInfo.logTransaction.get(req_id),"<PREPARE,");
                    }

                    if (p_list.size()>=((SystemInfo.node_num - 1) * 2 / 3)){
                        String[] p=p_list.get(0).split("<|,|>");
                    /* <PREPARE,v,n,d,i>sigi => [, PREPARE, v, n, d, i, sigi]
                    seg[1]="PREPARE", seg[2]=view v, seg[3]=request_id n, sig[4]=digest(request with sign), seg[5]=prepare_from, seg[6]=rep_sign for <1,2,3,4,5>
                    */
                        prepared=true;
                        String v=p[2];
                        String n=p[3];
                        String d=p[4];
                        String commit="<COMMIT,"+v+","+n+","+d+","+SystemInfo.node_id+">";
                        String sign_commit=Schnorr.sign(commit);
                        String commit_message=commit+sign_commit;
                        UtilClass.multicastOther(commit_message);
                        //System.out.println("## Prepared");
                        break;
                    }
                    try {
                        //SystemInfo.logTransaction.wait();
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //Commit
                ArrayList<String> co_list;
                while (true) {
                    if (SystemInfo.viewChangeFlag == true) {
                        System.out.println("PBFT Interrupt");
                        return;
                    }
                    if (SystemInfo.isReceiving) {
                        continue;
                    }
                    if (!SystemInfo.logTransaction.containsKey(req_id)){
                        continue;
                    }else {
                        co_list = UtilClass.findInLog(SystemInfo.logTransaction.get(req_id), "<COMMIT,");
                    }

                    if (prepared && co_list.size() >= ((SystemInfo.node_num - 1) * 2 / 3)) {
                        String[] co = co_list.get(0).split("<|,|>");
                    /*  <COMMIT,v,n,Dm,i>sigi => [, COMMIT, v, n, Dm, i, sigi]
                    seg[1]="COMMIT", seg[2]=view v, seg[3]=request_id, seg[4]=digest(request with sign), seg[5]=commit from, seg[6]=rep_sign for <1,2,3,4,5>
                    */
                        committed = true;
                        String v = co[2];
                        String operation_result = "Request" + req_id + " Completed";
                        String reply = "<REPLY," + v + "," + t_req + "," + cl_req + "," + SystemInfo.node_id + "," + operation_result + ">";
                        String sign_reply = Schnorr.sign(reply);
                        String reply_message = reply + sign_reply;
                        UtilClass.sendMessage(reply_message, Integer.valueOf(cl_req));
                        //SystemInfo.logCommitted.put(t_req,reply_message);
                        //SystemInfo.req_History.add(co[3]);
                        //SystemInfo.request_id++;
                        //System.out.println("## Committed");
                        break;
                    }
                    try {
                        //SystemInfo.logTransaction.wait();
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            //}
            /*System.out.println("SystemInfo Info: \n"
                    + " logTransaction size = "+SystemInfo.logTransaction.size()+"\n"
                    + " key_lib size = "+SystemInfo.key_lib.size()+"\n"
                    + " logCommitted size = "+SystemInfo.logCommitted.size()+"\n"
                    + " logViewChange size = "+SystemInfo.logViewChange.size()+"\n"
                    +" privateKey size = "+SystemInfo.privateKey.size()+"\n"
                    +" publicKey size = " +SystemInfo.publicKey.size());
            ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            //最大分配内存
            int memory = activityManager.getMemoryClass();
            System.out.println("memory: "+memory);
            //最大分配内存获取方法2
            float maxMemory = (float) (Runtime.getRuntime().maxMemory() * 1.0/ (1024 * 1024));
            //当前分配的总内存
            float totalMemory = (float) (Runtime.getRuntime().totalMemory() * 1.0/ (1024 * 1024));
            //剩余内存
            float freeMemory = (float) (Runtime.getRuntime().freeMemory() * 1.0/ (1024 * 1024));
            System.out.println(" maxMemory: "+maxMemory);
            System.out.println(" totalMemory: "+totalMemory);
            System.out.println(" freeMemory: "+freeMemory);*/
            if (SystemInfo.request_id>=5){
                SystemInfo.logTransaction.remove((""+(SystemInfo.request_id-5)));
                //SystemInfo.logCommitted.remove(SystemInfo.)
            }
        }
    }

    class ViewChange extends Thread{
        String request_message;
        public ViewChange(String request_message){
            this.request_message=request_message;
        }
        public void run(){
            System.out.println("## Start View Change");
            /*<REQUEST,o,t,c>sigc => [, REQUEST, o, t, c, sigc]
               seg[1]="REQUEST", seg[2]=operation o , seg[3]=timeseal t, seg[4]=client_id,
               seg[5]=client_sign for <seg[1],seg[2],seg[3],seg[4]>
            */

            //rep send view change
            /* <VIEW-CHANGE,v+1,n,c,p,i>sigi => [, VIEW-CHANGE, v+1, n, C, P, i, sigi]
               seg[1]="VIEW-CHANGE", seg[2]=new_view v+1, seg[3]=last_completed_request_id , seg[4]= message set for checkpoint
               seg[5]= previous_message_set_after_n , seg[6]=view_change from, seg[7]= rep sign <1,2,3,4,5>
            */
            String last_n="0";

            /*
            if (!SystemInfo.req_History.isEmpty()) {

                last_n = SystemInfo.req_History.get(SystemInfo.req_History.size()-1);
            }

             */
            last_n=""+(SystemInfo.request_id);
            String C_set=last_n+"_CP";

            String P_set=null;
            if (SystemInfo.logTransaction.containsKey(""+(Integer.valueOf(last_n)+1))){
                ArrayList<String> p_set_list=SystemInfo.logTransaction.get(""+(Integer.valueOf(last_n)+1));
                for(String str:p_set_list){
                    str=str.replace('<','{');
                    str=str.replace('>','}');
                    str=str.replace(',','|');
                    P_set=P_set+str+"/";
                }
            }
            String view_change="<VIEW-CHANGE,"+(SystemInfo.view+1)%SystemInfo.node_num+","+last_n+","+C_set+","+P_set+","+SystemInfo.node_id+">";
            String sign_view_change=Schnorr.sign(view_change);
            String view_change_message=view_change+sign_view_change;
            UtilClass.multicastOther(view_change_message);

            //the new primary check view change
            ArrayList<String> vc_list;
            while (true){
                if (SystemInfo.node_id!=(SystemInfo.view+1)%SystemInfo.node_num){ break; }
                //For the new primary
                if(SystemInfo.logViewChange.size()!=0) {
                    if (SystemInfo.isReceiving){ continue; }
                    vc_list = UtilClass.findInLog(SystemInfo.logViewChange, "VIEW-CHANGE");
                    if (vc_list.size() ==((SystemInfo.node_num - 1) * 2 / 3)){
                        //send new view
                        /* <NEW-VIEW,v+1,V,O>signp => [, NEW-VIEW, v+1, V, O, signp]
                         seg[1]="NEW-VIEW", seg[2]=new_view v+1, seg[3]=2f+1_view_change_message_digest, seg[4]=pre_prepare_message_set, seg[5]=rep_sign for <1,2,3,4>
                        */
                        String new_n= ""+Integer.valueOf(last_n)+1;
                        String V_set="";
                        for(String str:SystemInfo.logViewChange){
                            str=str.replace('<','{');
                            str=str.replace('>','}');
                            str=str.replace(',','|');
                            V_set=V_set+str+"/";
                        }
                        String new_digest=UtilClass.digest(request_message);
                        String new_pre_prepare="<PRE-PREPARE,"+(SystemInfo.node_id+1)+","+(Integer.valueOf(last_n)+1)+","+new_digest+">";
                        String new_view="<NEW-VIEW,"+(SystemInfo.node_id)+","+V_set+","+new_pre_prepare+">";
                        String new_view_message=new_view + Schnorr.sign(new_view);
                        UtilClass.multicastOther(new_view_message);
                        UtilClass.sendMessage(new_view_message,SystemInfo.node_id);
                        break;
                    }
                }
            }

            while (true){
                if (SystemInfo.isReceiving){ continue; }
                ArrayList<String> nv_list= UtilClass.findInLog(SystemInfo.logViewChange,"NEW-VIEW");
                if (nv_list.size()!=0){
                    SystemInfo.view=(SystemInfo.view+1)%SystemInfo.node_num;
                    SystemInfo.logViewChange.clear();
                    String[] nv=nv_list.get(0).split("<|,|>");
                    SystemInfo.request_id=Integer.valueOf(nv[7]);
                    /*[, NEW-VIEW, v+1, V, , PRE-PREPARE, v+1, n, d, , signp]
                    [, NEW-VIEW, 1, {V}, , PRE-PREPARE, 2, 1, 37D6D93879A23574395BAE25FACED4B36BE9DBAB1C745EFD9E57501D1E806C76, ,
                    1:ABCE4027F3D9DF5F3F69ED4149390B4EDF639981EE61577DA3706C025B404268F68B54C9BA483A5E6CAC4DE332A18E698B51E3F0E54097AC93AA9789E29F2073]
                    */
                    String new_p_p="<<"+nv[5]+","+nv[6]+","+nv[7]+","+nv[8]+">"+nv[10]+","+request_message+">";
                    ArrayList<String> mlog=new ArrayList<>();
                    mlog.add(new_p_p);
                    //rintln("## New Pre-prepare : "+new_p_p);
                    SystemInfo.logTransaction.put(nv[7],mlog);
                    String[] req_seg=request_message.split("<|,|>");
 //                   SystemInfo.primaryHistory_reqTimeToN.put(req_seg[3],nv[7]);
                    /*
                    if (SystemInfo.req_History.contains(""+(Integer.valueOf(last_n)+1))){
                        SystemInfo.req_History.remove(""+(Integer.valueOf(last_n)+1));
                    }
                    */
                    //showReceivedText("System View = device "+SystemInfo.view);
                    System.out.println("## View Changed : New View = Device "+SystemInfo.view);
                    SystemInfo.viewChangeFlag=false;
                    //SystemInfo.logTransaction.clear();
                    SystemInfo.timeSeal_log.add(req_seg[3]);
                    //System.out.println("this request recovered from view change = "+SystemInfo.request_id);
                    PBFT pbft=new PBFT(nv[7]);
                    if (SystemInfo.isTestingViewChange && SystemInfo.view==SystemInfo.node_id){
                        if(SystemInfo.node_id==0){
                            UtilClass.sendMessage(new_p_p,SystemInfo.node_id+SystemInfo.node_num-1);
                        }else {
                            UtilClass.sendMessage(new_p_p,SystemInfo.node_id-1);
                        }
                    }
                    pbft.start();
                    SystemInfo.request_id++;
                    break;
                }
            }
        }
    }
}

