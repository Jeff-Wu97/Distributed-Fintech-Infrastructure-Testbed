package com.example.commdemo;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

public class SystemInfo {
        //System info
        public static int client_id=0;
        public static String algorithm="Zyzzyyva";
        public static int node_id=client_id;//id of client/node
        public static int view = 0;//preset main node id
        public static int node_num=4;
        public static boolean test_off=false;
        public static String[] IP_Arr = {"10.0.2.2","10.0.2.2","10.0.2.2","10.0.2.2"};//{"192.168.1.104","192.168.1.104","192.168.1.104","192.168.1.101"};//{"10.0.2.2", "10.0.2.2", "10.0.2.2","192.168.1.101"};//All nodes and clients IP address   {“192.168.1.104”，“192.168.1.104”，“192.168.1.104”，"192.168.1.101"}

        public static int[] Port_Arr = {8001, 8003, 8005,8007};// All nodes and clients port
        public static int[] Port_Send_Arr={8000,8002,8004,8006};
        //PC redir port 8001->8000 8003->8002 8005->8004 8007->8006

        public static ArrayList<String> key_lib=new ArrayList<>();

        //public static int client_test_count=0;
        public static int request_id=0;
        //message count (client use)
        //public static ArrayList<String> respondedNode=new ArrayList<>();
//        public static HashMap<String, ArrayList> timeSeal_response= new HashMap<>();//hashmap:timeseal - spec_response_node // {timeseal:reply_num}
        //public static ArrayList<String> committedNode= new ArrayList<>();
        public static HashMap<String, ArrayList>reqhash_localcommit= new HashMap<>();//hashmap:timeseal - local_commit_node // {timeseal:local_commit_num}
//        public static int reply_num=0;
        //(device use)
        public static boolean isReceiving=false;//check if receiving message now

        public static HashMap<String,String> privateKey=new HashMap<>();
        public static HashMap<String,String> publicKey= new HashMap<>();


        //(node use)
        public static Long timeseal=(long)0;//record the newest coming request timeseal
        public static ArrayList<String> timeSeal_log=new ArrayList<>();// list(timeseal) record all the committed/responded timeseal

        public static String History=""; //history for request
        public static ArrayList<String> logHistory=new ArrayList<>();
        public static HashMap<String, ArrayList> logTransaction = new HashMap<>();// hashmap:request_id - related messages // {n:list(messages)} record the received messages
        public static int cc_id=-1;

        public static HashMap<String, String> log_req_OR = new HashMap<>();// hashmap:request_id - order-req message   for all rep

        public static HashMap<String, ArrayList> log_timeseal_SR =new HashMap<>();// hashmap:timeseal - spec_response {timeseal:SpecResp message} for all rep
        public static HashMap<String, String> log_history_SR=new HashMap<>();

        public static boolean viewChangeFlag=false;  //check if there is a view change now
        public static ArrayList<String> logViewChange= new ArrayList<>(); // record the message about the current view change
        public static boolean rep_vc_timer_on=false; // keep there is a only one view change clock on

        public static boolean startedvctimer=false;
}
