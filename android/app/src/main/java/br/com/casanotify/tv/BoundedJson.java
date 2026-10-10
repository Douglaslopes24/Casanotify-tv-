package br.com.casanotify.tv;

import java.util.HashSet;
import java.util.Set;
import org.json.*;

/** Strict wire JSON before the platform parser: bounded depth/work and no duplicate keys. */
public final class BoundedJson {
    private final String text;
    private int offset,values;
    private BoundedJson(String text){this.text=text;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("JSON inválido ou excede os limites permitidos.");}
    public static void checkObject(String raw,int limit){
        if(raw==null||raw.length()>limit)throw invalid();BoundedJson reader=new BoundedJson(raw);reader.space();
        if(reader.offset==raw.length()||raw.charAt(reader.offset)!='{')throw invalid();reader.value(0);reader.space();if(reader.offset!=raw.length())throw invalid();
    }
    public static JSONObject object(String raw,int limit)throws JSONException{checkObject(raw,limit);return new JSONObject(raw);}
    public static JSONObject object(byte[] bytes,int limit)throws JSONException{
        if(bytes.length>limit)throw invalid();try{return object(java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString(),limit);}catch(java.nio.charset.CharacterCodingException e){throw invalid();}
    }
    private void space(){while(offset<text.length()&&" \r\n\t".indexOf(text.charAt(offset))>=0)offset++;}
    private char current(){return offset<text.length()?text.charAt(offset):0;}
    private void require(char c){space();if(current()!=c)throw invalid();offset++;}
    private String string(boolean key){
        require('"');StringBuilder decoded=key?new StringBuilder():null;int count=0;
        while(offset<text.length()){
            char c=text.charAt(offset++);if(c=='"')return key?decoded.toString():null;if(c<32)throw invalid();
            if(c=='\\'){
                if(offset==text.length())throw invalid();char escape=text.charAt(offset++);
                if(escape=='u'){if(offset+4>text.length())throw invalid();int code=0;for(int n=0;n<4;n++){char hex=text.charAt(offset++);int digit="0123456789abcdef".indexOf(Character.toLowerCase(hex));if(digit<0)throw invalid();code=code*16+digit;}c=(char)code;}
                else {int index="\"\\/bfnrt".indexOf(escape);if(index<0)throw invalid();c="\"\\/\b\f\n\r\t".charAt(index);}
            }
            if(key){if(++count>128)throw invalid();decoded.append(c);}
        }throw invalid();
    }
    private void literal(String expected){if(!text.startsWith(expected,offset))throw invalid();offset+=expected.length();}
    private static boolean digit(char c){return c>='0'&&c<='9';}
    private void number(){
        int start=offset;if(current()=='-')offset++;
        if(current()=='0')offset++;else{if(current()<'1'||current()>'9')throw invalid();while(digit(current()))offset++;}
        if(current()=='.'){offset++;if(!digit(current()))throw invalid();while(digit(current()))offset++;}
        if(current()=='e'||current()=='E'){offset++;if(current()=='+'||current()=='-')offset++;if(!digit(current()))throw invalid();while(digit(current()))offset++;}
        if(offset-start>64)throw invalid();try{if(!Double.isFinite(Double.parseDouble(text.substring(start,offset))))throw invalid();}catch(NumberFormatException e){throw invalid();}
    }
    private void value(int depth){
        if(depth>16||++values>4096)throw invalid();space();char c=current();
        if(c=='{'){
            offset++;space();Set<String> keys=new HashSet<>();if(current()=='}'){offset++;return;}
            while(true){String key=string(true);if(!keys.add(key))throw invalid();require(':');value(depth+1);space();if(current()=='}'){offset++;return;}require(',');}
        }else if(c=='['){
            offset++;space();if(current()==']'){offset++;return;}
            while(true){value(depth+1);space();if(current()==']'){offset++;return;}require(',');}
        }else if(c=='"')string(false);else if(c=='t')literal("true");else if(c=='f')literal("false");else if(c=='n')literal("null");else number();
    }
}
