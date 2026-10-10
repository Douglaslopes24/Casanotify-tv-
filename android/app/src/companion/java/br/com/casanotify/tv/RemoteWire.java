package br.com.casanotify.tv;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

/** Android TV Remote v2 wire interoperability. See THIRD_PARTY_NOTICES.md for protocol sources. */
public final class RemoteWire {
    public static final int MAX_FRAME=65536;
    private static final Set<Integer> KEYS=new HashSet<>(Arrays.asList(3,4,19,20,21,22,23,24,25,26,85,164));
    private RemoteWire(){}
    public static byte[] concat(byte[]...parts){ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] part:parts)out.write(part,0,part.length);return out.toByteArray();}
    private static byte[] varint(long value){ByteArrayOutputStream out=new ByteArrayOutputStream();while((value&~127L)!=0){out.write((int)(value&127)|128);value>>>=7;}out.write((int)value);return out.toByteArray();}
    private static long varint(InputStream in)throws IOException{long result=0;for(int i=0;i<10;i++){int b=in.read();if(b<0)throw new EOFException("Quadro incompleto.");if(i==9&&(b&254)!=0)throw new IOException("Inteiro inválido.");result|=(long)(b&127)<<(7*i);if((b&128)==0)return result;}throw new IOException("Inteiro muito longo.");}
    public static byte[] number(int field,long value){return concat(varint((long)field<<3),varint(value));}
    public static byte[] bytes(int field,byte[] value){return concat(varint(((long)field<<3)|2),varint(value.length),value);}
    public static byte[] text(int field,String value){return bytes(field,value.getBytes(StandardCharsets.UTF_8));}
    public static Map<Integer,Object> parse(byte[] raw)throws IOException{
        if(raw.length>MAX_FRAME)throw new IOException("Quadro muito grande.");Map<Integer,Object> result=new HashMap<>();ByteArrayInputStream in=new ByteArrayInputStream(raw);int fields=0;
        while(in.available()>0){if(++fields>256)throw new IOException("Muitos campos.");long tag=varint(in);if(tag<=0||tag>0xffffffffL)throw new IOException("Campo inválido.");int field=(int)(tag>>>3),type=(int)(tag&7);if(field==0)throw new IOException("Campo inválido.");Object value;
            if(type==0)value=varint(in);
            else if(type==2){long length=varint(in);if(length<0||length>MAX_FRAME||length>in.available())throw new IOException("Comprimento inválido.");byte[] data=new byte[(int)length];if(in.read(data)!=data.length&&data.length>0)throw new EOFException();value=data;}
            else if(type==1||type==5){int length=type==1?8:4;byte[] data=new byte[length];if(in.read(data)!=length)throw new EOFException();value=data;}
            else throw new IOException("Formato de campo não aceito.");
            if(result.put(field,value)!=null)throw new IOException("Campo duplicado.");
        }return result;
    }
    public static long number(Map<Integer,Object> message,int field)throws IOException{Object value=message.get(field);if(!(value instanceof Long))throw new IOException("Número ausente.");return(Long)value;}
    public static byte[] bytes(Map<Integer,Object> message,int field)throws IOException{Object value=message.get(field);if(!(value instanceof byte[]))throw new IOException("Mensagem ausente.");return(byte[])value;}
    public static void write(OutputStream out,byte[] payload)throws IOException{if(payload.length==0||payload.length>MAX_FRAME)throw new IOException("Quadro inválido.");out.write(varint(payload.length));out.write(payload);out.flush();}
    public static byte[] read(InputStream in)throws IOException{long length=varint(in);if(length<=0||length>MAX_FRAME)throw new IOException("Quadro inválido.");byte[] data=new byte[(int)length];int offset=0;while(offset<data.length){int n=in.read(data,offset,data.length-offset);if(n<0)throw new EOFException();if(n==0)continue;offset+=n;}return data;}
    public static byte[] pairing(int field,byte[] payload){return concat(number(1,2),number(2,200),bytes(field,payload));}
    public static void requirePairing(byte[] frame,int expected)throws IOException{Map<Integer,Object> outer=parse(frame);if(number(outer,1)!=2||number(outer,2)!=200||!outer.containsKey(expected))throw new IOException("A Android TV recusou o pareamento. Gere um novo PIN.");bytes(outer,expected);}
    public static byte[] pairRequest(){return pairing(10,concat(text(1,"atvremote"),text(2,"CasaNotify")));}
    private static byte[] encoding(){return concat(number(1,3),number(2,6));}
    public static byte[] pairOptions(){return pairing(20,concat(bytes(1,encoding()),number(3,1)));}
    public static byte[] pairConfiguration(){return pairing(30,concat(bytes(1,encoding()),number(2,1)));}
    private static byte[] unsigned(BigInteger value){byte[] bytes=value.toByteArray();return bytes.length>1&&bytes[0]==0?Arrays.copyOfRange(bytes,1,bytes.length):bytes;}
    public static byte[] pairingSecret(PublicKey client,PublicKey server,String code)throws Exception{
        if(!(client instanceof RSAPublicKey)||!(server instanceof RSAPublicKey))throw new IOException("Certificado de controle incompatível.");
        if(code==null||!code.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Digite os seis caracteres mostrados na TV.");
        RSAPublicKey c=(RSAPublicKey)client,s=(RSAPublicKey)server;MessageDigest sha=MessageDigest.getInstance("SHA-256");sha.update(unsigned(c.getModulus()));sha.update(unsigned(c.getPublicExponent()));sha.update(unsigned(s.getModulus()));sha.update(unsigned(s.getPublicExponent()));sha.update((byte)Integer.parseInt(code.substring(2,4),16));sha.update((byte)Integer.parseInt(code.substring(4,6),16));byte[] secret=sha.digest();
        if((secret[0]&255)!=Integer.parseInt(code.substring(0,2),16))throw new IllegalArgumentException("O PIN não corresponde a esta TV. Confira a tela e tente novamente.");return secret;
    }
    public static byte[] key(int code){if(!KEYS.contains(code))throw new IllegalArgumentException("Botão não permitido.");return bytes(10,concat(number(1,code),number(2,3)));}
    public static final class State {
        private int features=99;private boolean configured,ready;
        public boolean ready(){return ready;}
        public byte[] accept(byte[] frame)throws IOException{
            Map<Integer,Object> message=parse(frame);
            if(message.containsKey(3))throw new IOException("O serviço de controle recusou o comando.");
            if(message.containsKey(1)){
                features=99&(int)number(parse(bytes(message,1)),1);if((features&2)==0)throw new IOException("Esta TV não aceita comandos de navegação.");configured=true;
                byte[] info=concat(text(1,"CasaNotify"),text(2,"CasaNotify"),number(3,1),text(4,"1"),text(5,"atvremote"),text(6,"2.4.0"));return bytes(1,concat(number(1,features),bytes(2,info)));
            }
            if(message.containsKey(2)){if(!configured)throw new IOException("Controle fora de sequência.");return bytes(2,number(1,features));}
            if(message.containsKey(8)){Map<Integer,Object> ping=parse(bytes(message,8));return bytes(9,number(1,ping.containsKey(1)?number(ping,1):0));}
            if(message.containsKey(40)){if(!configured)throw new IOException("Controle não configurado.");bytes(message,40);ready=true;}
            return null;
        }
    }
}
