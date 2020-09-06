package com.example.commdemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class UtilClass {
    private static String[] message_tosend={"","","",""};
    private static Lock lock = new ReentrantLock();
    private static Condition alreadysent = lock.newCondition();

    static class SendThread extends Thread {//create a thread to connect with another machine
        private String message;
        private int device;
        public SendThread(String message,int device) {
            this.message = message;
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
                String str = "";
                //pw.write("TCP/IP Connected" + "\n");
                //pw.flush();
                try {
                    str = message;//message_tosend[device];
                    if ((str != "")&&(str!=null)) {
                        lock.lock();

                        System.out.println("---- Sending "+str);
                        System.out.println("send len: "+message.length());
                        String[] message_seg=message.split("<|,|>");
                        System.out.println("send seg len: ");
                        for (String seg:message_seg){
                            System.out.println(seg.length());
                        }
                        if (message.length()>=5000){
                            int seg_num=message.length()/5000;
                            boolean remainderFlag;
                            if (message.length()%5000>0){
                                seg_num++;
                                remainderFlag=true;
                            }else {
                                remainderFlag=false;
                            }
                            String[] str_seg=new String[seg_num];
                            for (int i=0;i<=seg_num-1;i++){
                                if (!remainderFlag){
                                    str_seg[i]=message.substring(i*5000,(i+1)*5000);
                                    pw.write(str_seg[i]);
                                }else {
                                    if(i==seg_num-1) {
                                        str_seg[i]=message.substring(i*5000);
                                        pw.write(str_seg[i]+"\n");
                                    }else {
                                        str_seg[i]=message.substring(i*5000,(i+1)*5000);
                                        pw.write(str_seg[i]);
                                    }
                                }
                                pw.flush();
                            }
                        }else {
                            pw.write(str+"\n" );
                            pw.flush();
                        }

                        //pw.write(str+"\n" );
                        //pw.flush();
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

    public static void multicastOther(String message) {
        try {
            for (int i = 0; i < SystemInfo.node_num; i++) {
                if (i != SystemInfo.node_id) {
                    sendMessage(message,i);
                    //System.out.println("----------Multicast "+message+" "+i+"----------");
                    //Thread.sleep(1);
                }
            }
        } catch (Exception e) {
            System.out.println("## Err: Failed Multicast");
        }
    }


    public static boolean sendMessage(String message, int device){
        try {
            lock.lock();
            message_tosend[device]=message;
            UtilClass.SendThread send=new UtilClass.SendThread(message_tosend[device], device);
            //System.out.println("---- start sending -----"+message);
            send.start();
            lock.unlock();
        } catch (Exception e) {
            System.out.println("## Err in sending message");
            e.printStackTrace();
            return FALSE;
        }
        return TRUE;
    }

    public static void writeLineTxt(String doc_name, String txt_line){
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(doc_name,true);//add text line at the end of doc. if the doc is not existed , create the it
            fileWriter.write(txt_line+"\n");
            fileWriter.flush();
            fileWriter.close();
            System.out.println("Written");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.out.println("--- Err: Failed write to txt file: "+doc_name);
            e.printStackTrace();
        }
    }
    public static String findInTxt(String doc_name, String match_txt) {
        String txtline_matched=null;
        try {
            FileReader fr = new FileReader(doc_name);
            BufferedReader bf = new BufferedReader(fr);
            String str;
            while ((str = bf.readLine()) != null) {
                if (str.contains(match_txt)){
                    txtline_matched=str;
                    break;
                }
            }
            bf.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (txtline_matched!=null){
            return txtline_matched;
        }
        else return null;
    }

    //public static void ClientReset(){
        //reset status and message count
        //SystemInfo.timeSeal=null;
        //SystemInfo.reply_num=0;

    //}

    public static String digest(String message) {
        try {
            byte[] msg;
            MessageDigest md5;
            md5 = MessageDigest.getInstance("SHA-256");
            md5.update(message.getBytes());
            msg = md5.digest();
            return Schnorr.bytesToHex(msg);
        }catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static  ArrayList<String> findInLog(ArrayList<String> list,String info){
        //HashMap: [n0]:<list>,[n1]<list>,[n2]<list>...
        //Arraylist:[P-p](Req),[P1,P2,..],[C1,C2,..]
        ArrayList<String> matchedTxt=new ArrayList<>();
        //String[] matchedTxt;
        if (list.size()==0){
            //System.out.println("## Warning: List size = 0");
        }else if (list.size()==1){
            if (list.get(0).contains(info)){
                matchedTxt.add(list.get(0));
            }
        }else{
            for (String str:list){
                if (str.contains(info)){
                    matchedTxt.add(str);
                }
            }
        }
        if (matchedTxt.size()==0){
            //System.out.println("## Warning: Cannot find info in list");
        }
        return matchedTxt;
    }

    public static boolean isTimeToSleepInViewChangeTest(){
        //check if it is a time for the primary to sleep in view change testing
        //check if it is a view change test
        if (!SystemInfo.isTestingViewChange){
            return false;
        }
        //check if this is the primary
        if (SystemInfo.node_id!=SystemInfo.view){
            return false;
        }
        //check if it is the request of view change
        if (SystemInfo.viewChangeTestPeriod==0){
            return false;
        }
        if ((SystemInfo.timeSeal_log.size()-((SystemInfo.viewChangeTestPeriod-1)+SystemInfo.node_id*SystemInfo.viewChangeTestPeriod))%(SystemInfo.node_num*SystemInfo.viewChangeTestPeriod)==0){//+1)%(SystemInfo.viewChangeTestPeriod))==0&&SystemInfo.timeSeal_log.size()!=0){
            System.out.println("sleep for view change: "+"timeSeal_log="+SystemInfo.timeSeal_log.size());
            return true;
        }else {
            return false;
        }
    }
    public static boolean checkStringComplete(String s){
        if (s.charAt(0)=='!'){
            return false;
        }else {
            return true;
        }
    }

    public static void partitionStringSend(String s,int seg_len){
        int seg_num=s.length()/seg_len;
        boolean remainderFlag;
        if (s.length()%seg_len>0){
            seg_num++;
            remainderFlag=true;
        }else {
            remainderFlag=false;
        }
        String[] str_seg=new String[seg_num];
        for (int i=0;i<=seg_num-1;i++){

            if (!remainderFlag){
                str_seg[i]=s.substring(i*seg_len,(i+1)*seg_len);
            }else {
                if(i==seg_num-1) {
                    str_seg[i]=s.substring(i*seg_len);
                }else {
                    str_seg[i]=s.substring(i*seg_len,(i+1)*seg_len);
                }
            }
        }
        for(int i=0;i<=seg_num-1;i++){//String str:str_seg) {
            String str;
            if (i==seg_num-1){
                str="$"+SystemInfo.node_id+"!"+str_seg[i];
            }else {
                str="?"+SystemInfo.node_id+"!"+str_seg[i];
            }

            System.out.println(str);
        }
        System.out.println(Arrays.toString(str_seg));
    }

}