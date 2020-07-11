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

class SystemInfo{
    //System info
    public static int client_id=2;
    public static int node_id=client_id;//id of client/node
    public static int view = 0;//preset main node id
    public static int node_num=4;
    public static String[] IP_Arr = {"192.168.1.103","192.168.1.102","192.168.1.107","192.168.1.106"};//{"192.168.1.104","192.168.1.104","192.168.1.104","192.168.1.101"};//{"10.0.2.2", "10.0.2.2", "10.0.2.2","192.168.1.101"};//All nodes and clients IP address   {“192.168.1.104”，“192.168.1.104”，“192.168.1.104”，"192.168.1.101"}
    //List ip_list = new ArrayList<>(Arrays.asList(AddessArr));
    public static int[] Port_Arr = {8001, 8003, 8005,8007};// All nodes and clients port
    public static int[] Port_Send_Arr={8001,8003,8005,8007};
    //List port_list = new ArrayList<>(Arrays.asList(PortArr));
    //PC redir port 8001->8000 8003->8002 8005->8004 8007->8006


    public static int request_id=0;
    public static int send_state=0;

    //message count
    public static int request_num=0;
    public static int pre_prepare_num=0;
    public static int prepare_num=0;
    public static int commit_num=0;
    public static int reply_num=0;
    public static int preparing=0;
    public static int commiting=0;

    public static boolean Prepared=false;
    public static boolean Committed=false;

    public static ArrayList<String> logR = new ArrayList<String>();
    public static ArrayList<String> logS = new ArrayList<String>();

}


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static TextView tv;
    private static String[] message_tosend={"","","",""};
    private static String reqinfo="";
    private EditText editText;
    private Lock lock = new ReentrantLock();
    private Condition alreadysent = lock.newCondition();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.text_view3);
        tv.setBackgroundColor(Color.WHITE);
        Button button1 = (Button) findViewById(R.id.button_send);
        //Button button2 = (Button) findViewById(R.id.button_connect);
        editText = (EditText) findViewById(R.id.edit_text);
        button1.setOnClickListener(this);
        //button2.setOnClickListener(this);
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

    class SendThread extends Thread {//create a thread to connect with another machine
        private String message;
        //private String ip;
        //private int port;
        private int device;

        public SendThread(String message,int device) {
            this.message = message;
            //this.ip = ip;
            //this.port = port;
            this.device=device;
        }

        @Override
        public void run() {

            try {
                //create Socket and give server address
                Socket socket = new Socket(SystemInfo.IP_Arr[device],
                        SystemInfo.Port_Send_Arr[device]);
                //Get output stream and send massage to server
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter pw = new PrintWriter(outputStream);
                /*
                pw.write(message);
                pw.flush();*/
                String str = "";
                pw.write("TCP/IP Connected" + "\n");
                pw.flush();

                try {

                    str = message;//message_tosend[device];
                    if ((str != "")&&(str!=null)) {
                        lock.lock();
                        System.out.println("----Sending "+str+"----");
                        pw.write(str+"\n" );
                        pw.flush();
                        //message_tosend[device] = "";
                        //alreadysent.signalAll();
                        lock.unlock();
                    }
                    pw.close();
                    socket.close();
                }catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//------------------RECEIVE----------------------------------------RECEIVE----------------------------------------------------RECEIVE------------------------------------------RECEIVE------------------------------------
    class ReceiveThread extends Thread {
        private int port;
        //private int view;
        //private int id;
        public String message;

        public ReceiveThread(int port) {
            this.port = port;
            //this.view=view;
            //this.id=id;
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
                        System.out.println("info Out");
                        //MainActivity.showReceivedText("Received:" + info);
                        message = info;
                        System.out.println("-----Receive: " + message + "-----");
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
            //Add Check code here
            boolean verify_result;
            //MainActivity.showReceivedText("Resolving");
            String[] message_arr = message.split("<|,|>");
            //MainActivity.showReceivedText(message_arr[2]);
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
                    reqinfo=message;
                    PBFTNode pbft_p = new PBFTNode(message);
                    pbft_p.start();
                }
            }

        }

        public boolean verify(String message) {
            //Add verify code here
            return true;
        }
    }

//--------------------------------CLIENT----------------------------------CLIENT----------------------------------------CLIENT--------------------------------------------CLIENT------------------------------------------
    class Client extends Thread {

        int id = SystemInfo.client_id;//id of client/node
        int node_id = SystemInfo.node_id;
        int view = SystemInfo.view;//preset main node id
        int node_num = SystemInfo.node_num;
        String[] IP_Arr = SystemInfo.IP_Arr;//{"10.0.2.2", "10.0.2.2", "10.0.2.2"};//All nodes and clients IP address
        //List ip_list = new ArrayList<>(Arrays.asList(AddessArr));
        int[] Port_Arr = SystemInfo.Port_Arr;//{8000, 8002, 8004};// All nodes and clients port
        int[] Port_Send_Arr = SystemInfo.Port_Send_Arr;

        //int reply_num=SystemInfo.reply_num;
        String RequestMessage;

        public Client(String message) {
            //when the message button is clicked, the client will start a request.
            //In onClick the message is collected from the textbox
            this.RequestMessage = message;
        }

        public void run() {
            boolean send_result;

            //Generate REQUEST message
            long t = System.currentTimeMillis();
            String signature = "sigC" + id;//Add code of signature here
            RequestMessage = "<REQUEST," + RequestMessage + "," + t + "," + id + ">" + signature;
            //MainActivity.showReceivedText("Client sending:" + RequestMessage);

            try {
                send_result = sendMessage(RequestMessage, view);//IP_Arr[view], Port_Send_Arr[view]);
            } catch (Exception e) {
                System.out.println("---Failed Request to Primary---");
                e.printStackTrace();
            }


            //Wait for reply
            //Add code of Timer here to check primary (receive REPLY or broadcast REQUEST)
            //boolean ReceiveReply=TimeoutTaskUtils.execute(new CheckPrimary(SystemInfo.reply_num,0),10);
            boolean ReceiveReply = true;

            if (!ReceiveReply) {//if the client have not got the first reply in 10s
                //send the request to all nodes
                for (int i = 0; i < node_num; i++) {
                    send_result = sendMessage(RequestMessage,i);//, IP_Arr[i], Port_Send_Arr[i]) shjfkadj;
                }
            } else {//if the client got the first reply
                //wait for the reply from teh rest nodes
                while (true) {
                    if (SystemInfo.reply_num == ((node_num - 1) / 3) +1) {
                        long t_end=System.currentTimeMillis();
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        showReceivedText("--- Operation Completed: Consensus on the Transaction in "+(t_end-t)+"ms");
                        System.out.println("-----Have completed operation in REQUEST!-----");

                        //reset status and message count
                        SystemInfo.request_num=0;
                        SystemInfo.pre_prepare_num=0;
                        SystemInfo.prepare_num=0;
                        SystemInfo.commit_num=0;
                        SystemInfo.reply_num=0;
                        reqinfo = "";
                        SystemInfo.preparing=0;
                        SystemInfo.commiting=0;
                        SystemInfo.Prepared=false;
                        SystemInfo.Committed=false;
                        SystemInfo.logS.clear();
                        SystemInfo.logR.clear();

                        break;
                    }
                }
                //Request have been responded.
                // Add code to Record it to log
            }
        }

    }


    /*class TimeoutTaskUtils {//check if the client get the first REPLY in given seconds

        public static Boolean execute(Callable<Boolean> task, int seconds) {
            Boolean result = FALSE;
            ExecutorService threadPool = Executors.newCachedThreadPool();
            try {////if the REPLY dosen't arrive in given seconds, return true
                Future<Boolean> future = threadPool.submit(task);
                result = future.get(seconds, TimeUnit.SECONDS);
            } catch (Exception e) {//if the REPLY dosen't arrive in given seconds, return false
                result = false;
                System.out.println("First Request Timeout");
            } finally {
                threadPool.shutdownNow();
            }
            return result;
        }
    }


    class CheckPrimary implements Callable<Boolean> {//client wait for the first REPLY
        int message_num;
        int condition;

        public CheckPrimary(int message_num, int condition) {
            this.message_num = message_num;
            this.condition = condition;
        }

        @Override
        public Boolean call() throws Exception {
            while (true) {
                if (SystemInfo.reply_num != condition) {//check if the primary does work
                    return true;//if the primary works, return true
                }
            }
        }
    }

    */

//-------------------------------PBFT-----------------------------------------PBFT----------------------------------------------PBFT------------------------------------------PBFT----------------------------------------
    class PBFTNode extends Thread {

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
            //MainActivity.showReceivedText("PBFT working: " + message);
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

                //Add digest code here to create digest for REQUEST message text
                String d_r = "d(REQUEST)";

                //verify_result=verify(message);//Verify the signature

                if (id == view) {
                    //for primary
                    // check:  if the request hasn't been multicasted -> multicast <pre-prepare>, else -> no action
                    boolean IsRedundant = (SystemInfo.request_num == 1);//.logR.contains(message);
                    //MainActivity.showReceivedText("request count=" + SystemInfo.request_num);
                    if (IsRedundant) {//multicast
                        //MainActivity.showReceivedText("Sending Pre-prepare");
                        primaryPrePrepare(r_o, r_t, r_c, r_sigc, d_r, message);
                        SystemInfo.pre_prepare_num=1;
                    }
                } else {
                    //for backup
                    // check: if the node sent <reply> -> sent again
                    //       else if the node hasn't received <pre-prepare> -> fwd Request and timer: ViewChange
                    //       else no action (wait prepare&commit&operation)
                    String sentr = checkSent("<REPLY," + view + "," + r_t + "," + r_c);
                    if (sentr != null) {
                        /*SendThread resendreply = new SendThread(sentr, IP_Arr[Integer.valueOf(r_c).intValue()], Port_Send_Arr[Integer.valueOf(r_c).intValue()]) fsafsgfsfsdjh;//resend reply from logS
                        resendreply.start();*/
                        sendMessage(sentr,Integer.valueOf(r_c).intValue());

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
                //MainActivity.showReceivedText("pbft got PREPARE");
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
                //MainActivity.showReceivedText("pbft got COMMIT");
                //String c_v = message_arr[2];
                //String c_n = message_arr[3];
                String[] origin_req;

                while (true) {

                    if (reqinfo == "") {
                       // System.out.println("----failed find matched PRE-PREPARE message-----");
                        continue;
                    }
                    if (SystemInfo.Committed) {
                        return;
                    }

                    //String[] resolved_pp = origin_req.split("<|,|>");
                    //System.out.println("---- Pre-prepare = "+origin_req+" ----");
                    //String r_t = resolved_pp[10];
                    //String r_c = resolved_pp[11];
                    //-------- Modify End

                    if (SystemInfo.Prepared && !SystemInfo.Committed) {
                        SystemInfo.Committed = checkCommitted();
                        if (SystemInfo.Committed) {
                            boolean operation_result = true;
                            origin_req = reqinfo.split("<|,|>");
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
                                SystemInfo.request_num=0;
                                SystemInfo.pre_prepare_num=0;
                                SystemInfo.prepare_num=0;
                                SystemInfo.commit_num=0;
                                SystemInfo.reply_num=0;
                                reqinfo = "";
                                SystemInfo.preparing=0;
                                SystemInfo.commiting=0;
                                SystemInfo.Prepared=false;
                                SystemInfo.Committed=false;
                                SystemInfo.logS.clear();
                                SystemInfo.logR.clear();
                            }
                            showReceivedText("Node Committed");
                            showReceivedText("Transaction Recorded");
                            break;
                        }
                    }
                }
                //verify + operation + send <reply>



            } else if (message_arr[2].equals("PRE-PREPARE")) {
                //MainActivity.showReceivedText("pbft got PRE-PREPARE");
                //only the backup will receive pre-prepare from the primary
                //MainActivity.showReceivedText("received pre-p");
                String pp_v = message_arr[3];
                String pp_n = message_arr[4];
                String pp_d = message_arr[5];
                //String pp_sigp = message_arr[6];
                //String pp_m = message_arr[7];

                //Add signature generator here
                String sigi = "sigi" + id;
                //send <prepare>
                //MainActivity.showReceivedText("Sending Prepare");
                prepareSend(pp_v, pp_n, pp_d, sigi);
                SystemInfo.logR.add(message);

            }

            //SystemInfo.pbft_client_state=0;
        }


        public void multicastOther(String message) {
            try {
                for (int i = 0; i < node_num; i++) {
                    if (i != id) {
                        //MainActivity.showReceivedText("Multicasting");
                        //while (SystemInfo.send_state == 1) {

                        //}
                        /*SendThread send = new SendThread(message, IP_Arr[i], Port_Send_Arr[i])         ahfjsdhgjs;
                        send.start();*/
                        sendMessage(message,i);
                        System.out.println("----------Multicast "+message+" "+i+"----------");
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed Multicast");
            }
        }

        public void primaryPrePrepare(String o, String t, String c, String sigc, String d, String m) {

            String sigp = "sigP" + id;//Add signature generator here

            String pre_prepare = "<<PRE-PREPARE," + view + "," + request_id + "," + d + ">"
                    + sigp + "," + m + ">";
            //int i = 1;
            multicastOther(pre_prepare);
            SystemInfo.logS.add(pre_prepare);
            //for(int i=0;i<node_num;i++){
            //MainActivity.showReceivedText("Use Send");
            /*SendThread send_pre_prepare = new SendThread(pre_prepare, IP_Arr[i], Port_Send_Arr[i])       jfkdshfasgfdsfgas;
            send_pre_prepare.start();*/
              //  sendMessage(pre_prepare,i);
                //SystemInfo.logS.add(pre_prepare);
            //}
        }

        public void backupFwdRequest(String REQUESTmessage) {
            /*SendThread fwdRequest = new SendThread(REQUESTmessage, IP_Arr[view], Port_Send_Arr[view])   afhsfkjshflka;
            fwdRequest.start();*/
            sendMessage(REQUESTmessage,view);
            SystemInfo.logS.add(REQUESTmessage);
        }

        public void prepareSend(String v, String n, String d, String sigi) {
            String PrepareMessage = "<PREPARE," + v + "," + n + "," + d + "," + id + ">" + sigi;
            if (SystemInfo.logS.contains(PrepareMessage) == false) {
                multicastOther(PrepareMessage);
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
            multicastOther(CommitMessage);
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
            /*SendThread sendcommit = new SendThread(ReplyMessage, IP_Arr[Integer.valueOf(c).intValue()], Port_Send_Arr[Integer.valueOf(c).intValue()])    fsajdfgsafhj;
            sendcommit.start();*/
            sendMessage(ReplyMessage,Integer.valueOf(c).intValue());
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

    }
//------------------------------------SendMessage----------------------------------------------------SendMessage---------------------------------------------------SendMessage--------------------------------------------
    public boolean sendMessage(String message, int device){
        try {
            lock.lock();
            //alreadysent.await();
            message_tosend[device]=message;
            SendThread send=new SendThread(message_tosend[device], device);
            //showReceivedText("CheckA");
            System.out.println("---- start sending -----"+message);
            send.start();
            lock.unlock();
        } catch (Exception e) {
            System.out.println("----- Error in sending message -----");
            e.printStackTrace();
            return FALSE;
        }
        return TRUE;
    }
}

