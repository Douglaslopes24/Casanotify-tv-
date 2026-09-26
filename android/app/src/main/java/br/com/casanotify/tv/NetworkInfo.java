package br.com.casanotify.tv;
import java.net.*;import java.util.*;
public final class NetworkInfo {
 public static List<String> addresses(){List<String> out=new ArrayList<>();try{Enumeration<NetworkInterface> all=NetworkInterface.getNetworkInterfaces();while(all.hasMoreElements()){NetworkInterface n=all.nextElement();if(!n.isUp()||n.isLoopback())continue;Enumeration<InetAddress> a=n.getInetAddresses();while(a.hasMoreElements()){InetAddress ip=a.nextElement();if(ip instanceof Inet4Address&&!ip.isLoopbackAddress()&&!ip.isLinkLocalAddress())out.add("https://"+ip.getHostAddress()+":"+Prefs.SECURE_PORT);}}}catch(Exception ignored){}return out;}
 public static String primary(){List<String> a=addresses();return a.isEmpty()?"Conecte a TV à rede Wi-Fi ou cabo":a.get(0);}
}
