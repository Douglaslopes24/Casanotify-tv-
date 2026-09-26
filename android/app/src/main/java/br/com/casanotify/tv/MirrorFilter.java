package br.com.casanotify.tv;
import java.util.Locale;

/** Additional privacy filter, independent of Android's own sensitive-notification protections. */
public final class MirrorFilter {
    public static boolean sensitive(String title,String text){String s=(title+" "+text).toLowerCase(Locale.ROOT);return s.matches("(?s).*(\\botp\\b|código|codigo|verification|verifica[cç][aã]o|one.time|senha|password|autentica[cç][aã]o|security code|authentication).*");}
    public static String limit(String s,int n){return s==null?"":s.substring(0,Math.min(n,s.length()));}
}
