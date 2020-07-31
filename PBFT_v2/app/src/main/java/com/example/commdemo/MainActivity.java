package com.example.commdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
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
                "     Listening Port = " + SystemInfo.Port_Arr[SystemInfo.node_id]+"\n");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_intialize:
                Initialize.initializeKey();
                System.out.println("Got all keys");
                break;
            case R.id.button_send:
                String inputText = editText.getText().toString();//get the text in textline
                //this.showReceivedText(inputText);
                //SendThread sender=new SendThread(inputText);
                //sender.start();
                Client client = new Client(inputText);
                client.start();
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
//--------------------------SEND----------------------------SEND--------------------------------------SEND----------------------------------------SEND--------------------------------------SEND--------------------------



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
                        //System.out.println("info Out");
                        //MainActivity.showReceivedText("Received:" + info);
                        message = info;
                        System.out.println("-----Receive: " + message + "-----");
                        //UtilClass.writeLineTxt("messagelog.txt",message);
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
            String[] seg=message.split("<|,|>");
            boolean verify_result;
            Timer backuptimer= new Timer();
            if (seg.length<=2){ return;}
            SystemInfo.isReceiving=true;
            if (seg[0].equals("KEY")){
                String otherNodeKeyTxt=seg[1]+","+seg[2];
                SystemInfo.key_lib.add(otherNodeKeyTxt);
                //UtilClass.writeLineTxt("key.txt",otherNodeKeyTxt);

            }else if (seg[1].equals("REQUEST")) {
                if (SystemInfo.node_id!=SystemInfo.view){
                    if (SystemInfo.logCommitted.containsKey(seg[3])){
                        String resend_reply=SystemInfo.logCommitted.get(seg[3]);
                        UtilClass.sendMessage(resend_reply,Integer.valueOf(seg[4]));
                    }else {
                        backupFwdRequest(message);
                        backuptimer.schedule(new BackupTimer(message),10*1000);
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
                            PBFT pbft=new PBFT(seg[4]);
                            pbft.start();
                        }
                    }else {
                        System.out.println("Failure in checking Pre-prepare digest and view");
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
                        }else{
                            ArrayList<String> mlog=new ArrayList<>();//new the message log and add the message into the log
                            mlog.add(message);
                            SystemInfo.logTransaction.put(seg[3],mlog);
                            PBFT pbft= new PBFT(seg[3]);
                            pbft.start();
                        }
                    }else {
                        System.out.println("Failure in checking Prepare view");
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
                        }else {
                            ArrayList<String> mlog=new ArrayList<>();//new the message log and add the message into the log
                            mlog.add(message);
                            SystemInfo.logTransaction.put(seg[3],mlog);
                            PBFT pbft=new PBFT(seg[3]);
                            pbft.start();
                        }
                    }else {
                        System.out.println("Failure in checking Commit view");
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
                    if(seg[3].equals(SystemInfo.timeSeal)){
                        SystemInfo.reply_num++;
                    }
                }
            }else if (seg[1].equals("VIEW-CHANGE")){
                    /* <VIEW-CHANGE,v+1,n,c,p,i>sigi => [, VIEW-CHANGE, v+1, n, c, p, i, sigi]
                       seg[1]="VIEW-CHANGE", seg[2]=new_view v+1, seg[3]=last_completed_request_id , seg[4]= req_  , seg[5]= previous_message_set_after_n , seg[6]=view_change from, seg[7]= rep sign <1,2,3,4,5>
                     */
                String data="<"+seg[1]+","+seg[2]+","+seg[3]+","+seg[4]+","+seg[5]+","+seg[6]+">";
                String sign=seg[7];
                verify_result=Schnorr.checkSign(data,sign);
                if (verify_result){
                    if (seg[2].equals(""+(SystemInfo.view+1))) {
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
                    if (seg[2].equals(""+(SystemInfo.view+1))) {
                        SystemInfo.logViewChange.add(message);
                    }
                }
            }
            SystemInfo.isReceiving=false;
        }

        public void backupFwdRequest(String REQUESTmessage) {
            //the fwdRequest has got signature so we don't need  add the signature any more
            UtilClass.sendMessage(REQUESTmessage,SystemInfo.view);
        }
        /*
        public void messageResolve(String message) {
            //Add Check code here
            boolean verify_result;
            String[] message_arr = message.split("<|,|>");
            //switch (message_arr[1]) {//timeout to wait pre-prepare to trigger view change
            if (message_arr.length<=3){
                return;
            }
            System.out.println("--------"+message_arr[1]+"--------");

            if (message_arr[1].equals("REPLY")) {
                verify_result = verify(message);
                if (verify_result) {
                    showReceivedText("Client received: "+message);
                    SystemInfo.reply_num++;
                    //client action
                }
            } else if (message_arr[1].equals("REQUEST")) {
                verify_result = verify(message);
                if (verify_result) {
                    showReceivedText("Node received: "+message);
                    SystemInfo.request_num++;
                    PBFTNode pbft_pp = new PBFTNode(message);
                    pbft_pp.start();
                }
            } else if (message_arr[1].equals("PREPARE")) {
                verify_result = verify(message);
                if (verify_result) {
                    showReceivedText("Node received: "+message);
                    SystemInfo.prepare_num++;
                    System.out.println("---prepare num = "+SystemInfo.prepare_num);
                    SystemInfo.logR.add(message);
                    if (SystemInfo.prepare_num >= 1 && SystemInfo.preparing!=1) {
                        PBFTNode pbft_c = new PBFTNode(message);
                        pbft_c.start();
                        SystemInfo.preparing=1;
                    }
                }
            } else if (message_arr[1].equals("COMMIT")) {
                verify_result = verify(message);
                if (verify_result) {
                    showReceivedText("Node received: "+message);
                    SystemInfo.commit_num++;
                    System.out.println("---commit num = "+SystemInfo.commit_num);
                    SystemInfo.logR.add(message);
                    if (SystemInfo.commit_num == 1 && SystemInfo.commiting!=1) {
                        PBFTNode pbft_r = new PBFTNode(message);
                        pbft_r.start();
                        SystemInfo.commiting=1;
                    }
                }
            } else if (message_arr[2].equals("PRE-PREPARE")) {
                //MainActivity.showReceivedText("Checked R P-P");
                verify_result = verify(message);
                if (verify_result) {
                    showReceivedText("Node received: "+message);
                    //showReceivedText("Node received: "+message);
                    SystemInfo.pre_prepare_num++;
                    SystemInfo.logR.add(message);
                    //MainActivity.showReceivedText("start pbft");
                    SystemInfo.reqinfo=message;
                    PBFTNode pbft_p = new PBFTNode(message);
                    pbft_p.start();
                }
            }else if (message_arr[0].equals("KEY")){
                String otherNodeKeyTxt=message_arr[1]+","+"KEYof"+message_arr[2];
                UtilClass.writeLineTxt("key.txt",otherNodeKeyTxt);
            }

        }

        public boolean verify(String data, String sig) {
            boolean result=Schnorr.checkSign(data,sig);
            //Add verify code here
            return true;
        }*/
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
            SystemInfo.timeSeal=""+t;
            RequestMessage = "<REQUEST," + RequestMessage + "," + SystemInfo.timeSeal + "," + id + ">";
            RequestMessage=RequestMessage+Schnorr.sign(RequestMessage);
            System.out.println("Signed: "+RequestMessage);
            UtilClass.sendMessage(RequestMessage, view);//IP_Arr[view], Port_Send_Arr[view]);
            System.out.println("---- client sent request");

            //Wait for reply
            //boolean ReceiveReply=TimeoutTaskUtils.execute(new CheckPrimary(SystemInfo.reply_num,0),10);
            Timer clienttimer=new Timer();
            clienttimer.schedule(new ClientTimer(RequestMessage),10*1000);

            while (true) {
                if  (SystemInfo.reply_num>0){
                    clienttimer.cancel();
                }
                if (SystemInfo.reply_num == ((node_num - 1) / 3) +1) {
                    long t_end=System.currentTimeMillis();
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    showReceivedText("--- Operation Completed: Consensus on the Transaction in "+(t_end-t)+"ms");
                    System.out.println("-----Have completed operation in REQUEST!-----");
                    UtilClass.ClientReset();
                    break;
                }
            }
            //Request have been responded.
            // Add code to Record it to log
        }

    }


//-------------------------------PBFT-----------------------------------------PBFT----------------------------------------------PBFT------------------------------------------PBFT----------------------------------------
    /*class PBFTNode extends Thread {

        int id = SystemInfo.node_id;//0;//id of client/node
        //int client_id=SystemInfo.client_id;
        int view = SystemInfo.view;//0;//preset main node id
        int node_num = SystemInfo.node_num;//3;
        String[] IP_Arr = SystemInfo.IP_Arr;//{"10.0.2.2", "10.0.2.2", "10.0.2.2"};//All nodes and clients IP address
        //List ip_list = new ArrayList<>(Arrays.asList(AddessArr));
        int[] Port_Arr = SystemInfo.Port_Arr;//{8000, 8002, 8004};// All nodes and clients port
        int[] Port_Send_Arr = SystemInfo.Port_Send_Arr;
        int request_id = SystemInfo.request_id;



        public String message;//received node messsage
        public String[] message_arr;


        public PBFTNode(String message) {
            this.message = message;
        }

        public void run() {
            //Record received message, delete the redundant message

            //resolve the message further and process
            //SystemInfo.pbft_client_state=1;
            //MainActivity.showReceivedText("PBFT working: " + message)

            System.out.println("-------PBFT Node starts working-------");
            message_arr = message.split("<|,|>");
            System.out.println("-------------"+message_arr[1]+"--------------------");

            if (message_arr[1].equals("REQUEST")) {
                //MainActivity.showReceivedText("pbft got REQUEST");
                //check
                String r_o = message_arr[2];
                String r_t = message_arr[3];
                String r_c = message_arr[4];
                String r_sigc = message_arr[5];
                //verify_result=verify(message);//Verify the signature

                if (id == view) {
                    //for primary
                    // check:  if the request hasn't been multicasted -> multicast <pre-prepare>, else -> no action
                    if (SystemInfo.request_num == 1) {//multicast
                        primaryPrePrepare(message);
                        SystemInfo.pre_prepare_num=1;
                    }
                } else {
                    //for backup
                    // check: if the node sent <reply> -> sent again
                    //       else if the node hasn't received <pre-prepare> -> fwd Request and timer: ViewChange
                    //       else no action (wait prepare&commit&operation)
                    String sentr = checkSent("<REPLY," + view + "," + r_t + "," + r_c);
                    if (sentr != null) {
                        sentr=sentr+Schnorr.sign(sentr);
                        UtilClass.sendMessage(sentr,Integer.valueOf(r_c).intValue());

                    } else {
                        //Add code of Timer to check primary and prepare for ViewChange
                        //boolean waitPre=TimeoutTaskUtils.execute(new CheckPrimary(SystemInfo.pre_prepare_num,0),5);
                        boolean GotPrePrepare = true;

                        if (!GotPrePrepare) {//if the client have no reply and the backup has no pre-prepare for a long time, send ViewChange
                            backupFwdRequest(message);
                            System.out.println("The system will start ViewChange");
                            //Add code to ViewChange
                        }
                    }
                }

            } else if (message_arr[1].equals("PREPARE")) {
                String p_v = message_arr[2];
                String p_n = message_arr[3];
                String p_d = message_arr[4];
                String p_i = message_arr[5];

                String d_m = "d(Message)";
                //send <commit>
                while (true) {
                    if (SystemInfo.pre_prepare_num>=1&&!SystemInfo.Prepared) {
                        SystemInfo.Prepared = checkPrepared();
                        if (SystemInfo.Prepared) {
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            showReceivedText("Prepared");
                            commitSend(p_v, p_n, "REQUEST");
                            break;
                        }
                    }//check until get enough PREPARE
                }


            } else if (message_arr[1].equals("COMMIT")) {
                String[] origin_req;
                while (true) {
                    if (SystemInfo.reqinfo == "") {
                       // System.out.println("----failed find matched PRE-PREPARE message-----");
                        continue;
                    }
                    if (SystemInfo.Committed) {
                        return;
                    }
                    if (SystemInfo.Prepared && !SystemInfo.Committed) {
                        SystemInfo.Committed = checkCommitted();
                        if (SystemInfo.Committed) {
                            boolean operation_result = true;
                            origin_req = SystemInfo.reqinfo.split("<|,|>");
                            String r_t;
                            String r_c;
                            if (id == view) {
                                r_t = origin_req[3];
                                r_c = origin_req[4];
                                replySend(r_t, r_c, operation_result);
                            } else {
                                r_t = origin_req[10];
                                r_c = origin_req[11];
                                replySend(r_t, r_c, operation_result);

                                //reset status and message count
                                UtilClass.requestReset();
                            }
                            showReceivedText("Node Committed");
                            showReceivedText("Transaction Recorded");
                            break;
                        }
                    }
                }
                //verify + operation + send <reply>
            } else if (message_arr[2].equals("PRE-PREPARE")) {
                //only the backup will receive pre-prepare from the primary
                String pp_v = message_arr[3];
                String pp_n = message_arr[4];
                String pp_d = message_arr[5];

                //Add signature generator here
                String sigi = "sigi" + id;
                //send <prepare>
                //MainActivity.showReceivedText("Sending Prepare");
                prepareSend(pp_v, pp_n, pp_d, sigi);
                SystemInfo.logR.add(message);
            }
            //SystemInfo.pbft_client_state=0;
        }




        public void primaryPrePrepare(String m) {
            //String Pre_prepareMessage = "<<PRE-PREPARE," + view + "," + request_id + "," + d + ">"+ sigp + "," + m + ">";
            String d=UtilClass.digest(message);
            String Pre_prepareMessage = "<PRE-PREPARE," + view + "," + request_id + "," + d + ">";
            Pre_prepareMessage=Pre_prepareMessage+Schnorr.sign(Pre_prepareMessage);
            Pre_prepareMessage="<"+Pre_prepareMessage+","+m+">";

            UtilClass.multicastOther(Pre_prepareMessage);
            SystemInfo.logS.add(Pre_prepareMessage);
        }



        public void prepareSend(String v, String n, String d, String sigi) {
            String PrepareMessage = "<PREPARE," + v + "," + n + "," + d + "," + id + ">" + sigi;
            PrepareMessage=PrepareMessage+Schnorr.sign(PrepareMessage);
            if (SystemInfo.logS.contains(PrepareMessage) == false) {
                UtilClass.multicastOther(PrepareMessage);
                SystemInfo.logS.add(PrepareMessage);
            }else {
                showReceivedText("have sent prepare");
            }
        }

        public boolean checkPrepared() {
            //System.out.println("------------Prepare Num = "+SystemInfo.prepare_num+"------------");
            if (!SystemInfo.Prepared && SystemInfo.prepare_num >= ((node_num - 1) * 2 / 3)) {//Add code to check and verify PREPARE message
                System.out.println("--------------- Prepared!!--------");
                return true;
            } else {
                return false;
            }
        }

        public void commitSend(String v, String n, String m) {
            String sigi = "sig" + id;//Add signature code
            String d_m = "d(RequestMsg)";//Add digest of REQUEST message here
            String CommitMessage = "<COMMIT," + v + "," + n + "," + id + ">" + sigi;
            UtilClass.multicastOther(CommitMessage);
        }

        public boolean checkCommitted() {
            if (!SystemInfo.Committed && SystemInfo.commit_num >= ((node_num - 1) * 2 / 3)) {//Add code to check and verify PREPARE message
                System.out.println("----------Commited!!--------");
                return true;
            } else {

                return false;
            }
        }

        public void replySend(String t, String c, Boolean operation_result) {
            String sigi = "sigi" + id;//Add signature code
            String ReplyMessage = "<REPLY," + view + "," + t + "," + c + "," + id + ","
                    + operation_result + ">" + sigi;
            //SendThread sendcommit = new SendThread(ReplyMessage, IP_Arr[Integer.valueOf(c).intValue()], Port_Send_Arr[Integer.valueOf(c).intValue()])    fsajdfgsafhj;
            //sendcommit.start();
            UtilClass.sendMessage(ReplyMessage,Integer.valueOf(c).intValue());
            SystemInfo.logS.add(ReplyMessage);
        }

        public String checkSent(String text) {
            for (String log : SystemInfo.logS) {
                Pattern pstr = Pattern.compile(text);//Add code to match REQUEST message exactly
                Matcher chstr = pstr.matcher(log);
                if (chstr.lookingAt()) {
                    return log;
                }
            }
            return null;
        }

    }*/
//------------------------------------SendMessage----------------------------------------------------SendMessage---------------------------------------------------SendMessage--------------------------------------------
    /*class Resolver extends Thread {
        String message;
        public Resolver(String message){this.message=message;}

    }*/

    class ClientTimer extends TimerTask{
        String RequestMessage;
        public ClientTimer(String RequestMessage){
            this.RequestMessage=RequestMessage;
        }
        public void run(){
            for (int i = 0; i < SystemInfo.node_num; i++) {
                //RequestMessage=RequestMessage+Schnorr.sign(RequestMessage);
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
            System.out.println("---- Start PBFT for req "+req_id);
            while (true){
                if(SystemInfo.viewChangeFlag==true){ System.out.println("PBFT Interrupt");return; }
                if (SystemInfo.isReceiving){ continue; }
                req_or_pp=UtilClass.findInLog(SystemInfo.logTransaction.get(req_id),"<REQUEST");
                if (req_or_pp.size()!=0){
                    if(req_or_pp.get(0).contains("PRE-PREPARE")){//Backup got pre-prepare, send prepare
                        System.out.println("----- to send Prepare");
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
                        System.out.println("----- to send Pre-prepare");
                        String[] req=req_or_pp.get(0).split("<|,|>");
                        String o=req[2];
                        t_req=req[3];
                        cl_req=req[4];
                        String digest_req=UtilClass.digest(req_or_pp.get(0));
                        System.out.println("---- digest: "+req_or_pp.get(0));
                        String pre_prepare="<PRE-PREPARE,"+SystemInfo.view+","+req_id+","+digest_req+">";
                        String sign_pre_prepare=Schnorr.sign(pre_prepare);
                        String pre_prepare_message="<"+pre_prepare+sign_pre_prepare+","+req_or_pp.get(0)+">";
                        UtilClass.multicastOther(pre_prepare_message);
                        //send pre-prepare
                    }
                    System.out.println("------ pre-prepared");
                    break;
                }
            }//pre-prepare end

            //Prepare
            ArrayList<String> p_list;
            while (true){
                if(SystemInfo.viewChangeFlag==true){ System.out.println("PBFT Interrupt"); return; }
                if (SystemInfo.isReceiving){ continue; }
                p_list=UtilClass.findInLog(SystemInfo.logTransaction.get(req_id),"<PREPARE,");
                if (p_list.size()==((SystemInfo.node_num - 1) * 2 / 3)){
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
                    System.out.println("------ prepared");
                    break;
                }
            }

            //Commit
            ArrayList<String> co_list;
            while (true){
                if(SystemInfo.viewChangeFlag==true){ System.out.println("PBFT Interrupt"); return; }
                if (SystemInfo.isReceiving){ continue; }
                co_list=UtilClass.findInLog(SystemInfo.logTransaction.get(req_id),"<COMMIT,");
                if (prepared && co_list.size()==((SystemInfo.node_num - 1) * 2 / 3)){
                    String[] co=co_list.get(0).split("<|,|>");
                    /*  <COMMIT,v,n,Dm,i>sigi => [, COMMIT, v, n, Dm, i, sigi]
                    seg[1]="COMMIT", seg[2]=view v, seg[3]=request_id, seg[4]=digest(request with sign), seg[5]=commit from, seg[6]=rep_sign for <1,2,3,4,5>
                    */
                    committed=true;
                    String v=co[2];
                    String operation_result="Request"+req_id+" Completed";
                    String reply="<REPLY,"+ v+","+ t_req+","+ cl_req+","+ SystemInfo.node_id+","+ operation_result+">";
                    String sign_reply=Schnorr.sign(reply);
                    String reply_message=reply+sign_reply;
                    UtilClass.sendMessage(reply_message,Integer.valueOf(cl_req));
                    SystemInfo.logCommitted.put(t_req,reply_message);
                    SystemInfo.req_History.add(co[3]);
                    SystemInfo.request_id++;
                    System.out.println("------ committed");
                    break;
                }
            }
        }
    }

    class ViewChange extends Thread{
        String request_message;
        public ViewChange(String request_message){
            this.request_message=request_message;
        }
        public void run(){
            System.out.println("Start View Change");
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
            if (!SystemInfo.req_History.isEmpty()) {
                last_n = SystemInfo.req_History.get(SystemInfo.req_History.size()-1);
            }

            String C_set=last_n+"_CP";

            String P_set=null;
            if (SystemInfo.logTransaction.containsKey(""+(Integer.valueOf(last_n)+1))){
                ArrayList<String> p_set_list=SystemInfo.logTransaction.get(""+(Integer.valueOf(last_n)+1));
                for(String str:p_set_list){
                    P_set=P_set+str+"|";
                }
            }
            String view_change="<VIEW-CHANGE,"+(SystemInfo.view+1)+","+last_n+","+C_set+","+P_set+","+SystemInfo.node_id+">";
            String sign_view_change=Schnorr.sign(view_change);
            String view_change_message=view_change+sign_view_change;
            UtilClass.multicastOther(view_change_message);

            //the new primary check view change
            ArrayList<String> vc_list;
            while (true){
                if (SystemInfo.node_id!=SystemInfo.view+1){ break; }
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
                    //
                    SystemInfo.view++;
                    SystemInfo.logViewChange.clear();

                    String[] nv=nv_list.get(0).split("<|,|>");

                    SystemInfo.request_id=Integer.valueOf(nv[6]);
                    //[, NEW-VIEW, v+1, V, , PRE-PREPARE, v+1, n, d, , signp]
                    //[, NEW-VIEW, 1, {V}, , PRE-PREPARE, 2, 1, 37D6D93879A23574395BAE25FACED4B36BE9DBAB1C745EFD9E57501D1E806C76, , 1:ABCE4027F3D9DF5F3F69ED4149390B4EDF639981EE61577DA3706C025B404268F68B54C9BA483A5E6CAC4DE332A18E698B51E3F0E54097AC93AA9789E29F2073]
                    String new_p_p="<<"+nv[5]+","+nv[6]+","+nv[7]+","+nv[8]+">"+nv[10]+","+request_message+">";
                    ArrayList<String> mlog=new ArrayList<>();
                    mlog.add(new_p_p);
                    System.out.println("----- New Pre-prepare : "+new_p_p);
                    SystemInfo.logTransaction.put(nv[7],mlog);
                    String[] req_seg=request_message.split("<|,|>");
                    SystemInfo.primaryHistory_reqTimeToN.put(req_seg[3],nv[7]);
                    if (SystemInfo.req_History.contains(""+(Integer.valueOf(last_n)+1))){
                        SystemInfo.req_History.remove(""+(Integer.valueOf(last_n)+1));
                    }
                    System.out.println("--- View Changed : New View = device "+SystemInfo.view);
                    SystemInfo.viewChangeFlag=false;
                    PBFT pbft=new PBFT(nv[7]);
                    pbft.start();
                    break;
                }
            }
        }
    }




}

