package br.com.casanotify.tv;
public final class QuietHours {
 public static int minutes(String s){if(!s.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))throw new IllegalArgumentException("Use HH:MM.");return Integer.parseInt(s.substring(0,2))*60+Integer.parseInt(s.substring(3));}
 public static boolean contains(int now,String start,String end){int a=minutes(start),b=minutes(end);return a==b||(a<b?now>=a&&now<b:now>=a||now<b);}
}
