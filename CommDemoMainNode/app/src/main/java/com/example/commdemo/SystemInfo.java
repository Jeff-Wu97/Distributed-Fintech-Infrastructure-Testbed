package com.example.commdemo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SystemInfo {

        //System info
        public static int client_id=3;
        public static int node_id=client_id;//id of client/node
        public static int view = 0;//preset main node id
        public static int node_num=4;
        public static String[] IP_Arr = {"10.0.2.2","10.0.2.2","10.0.2.2","10.0.2.2"};//{"192.168.1.104","192.168.1.104","192.168.1.104","192.168.1.101"};//{"10.0.2.2", "10.0.2.2", "10.0.2.2","192.168.1.101"};//All nodes and clients IP address   {“192.168.1.104”，“192.168.1.104”，“192.168.1.104”，"192.168.1.101"}
        //List ip_list = new ArrayList<>(Arrays.asList(AddessArr));
        public static int[] Port_Arr = {8001, 8003, 8005,8007};// All nodes and clients port
        public static int[] Port_Send_Arr={8000,8002,8004,8006};
        //List port_list = new ArrayList<>(Arrays.asList(PortArr));
        //PC redir port 8001->8000 8003->8002 8005->8004 8007->8006

        public static ArrayList<String> key_lib=new ArrayList<>();

        public static int request_id=0;
        //public static int =0;

        //message count
        /*
        public static int request_num=0;
        public static int pre_prepare_num=0;
        public static int prepare_num=0;
        public static int commit_num=0;*/
        public static int reply_num=0;/*
        public static int preparing=0;
        public static int commiting=0;
        public static String reqinfo="";

        public static boolean Prepared=false;
        public static boolean Committed=false;
        */
        public static boolean isReceiving=false;
        public static HashMap<String,String> privateKey=new HashMap<>();
        public static HashMap<String,String> publicKey= new HashMap<>();

        public static String timeSeal=null;
        public static HashMap<String, ArrayList> logTransaction = new HashMap<>();
        public static HashMap<String,String> primaryHistory_reqTimeToN=new HashMap<>();
        public static ArrayList<String> req_History= new ArrayList<>();//record the got transaction id
        public static HashMap<String, String> logCommitted = new HashMap<>();

        public static boolean viewChangeFlag=false;
        public static ArrayList<String> logViewChange= new ArrayList<>();

}
