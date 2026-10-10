package br.com.casanotify.tv;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

/** Interoperability vectors from the published Polo/Remote v2 protobuf schema. */
public class RemoteTest {
    private static byte[] hex(String value){byte[] bytes=new byte[value.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(value.substring(2*i,2*i+2),16);return bytes;}
    private interface Action{void run()throws Exception;}
    private static void invalid(Action action)throws Exception{try{action.run();fail("Expected rejection");}catch(IOException|IllegalArgumentException expected){}}
    private static RSAPublicKey key(String modulus){return new RSAPublicKey(){public BigInteger getModulus(){return new BigInteger(modulus,16);}public BigInteger getPublicExponent(){return BigInteger.valueOf(65537);}public String getAlgorithm(){return "RSA";}public String getFormat(){return "X.509";}public byte[] getEncoded(){return new byte[0];}};}
    @Test public void pairingFramesMatchIndependentProtobufVectors(){
        assertArrayEquals(hex("080210c80152170a0961747672656d6f7465120a436173614e6f74696679"),RemoteWire.pairRequest());
        assertArrayEquals(hex("080210c801a201080a04080310061801"),RemoteWire.pairOptions());
        assertArrayEquals(hex("080210c801f201080a04080310061001"),RemoteWire.pairConfiguration());
    }
    @Test public void shortKeyVectorsAndUnsupportedKeys(){
        assertArrayEquals(hex("520408131003"),RemoteWire.key(19));
        assertArrayEquals(hex("520508a4011003"),RemoteWire.key(164));
        for(int code:new int[]{0,-1,1,2,82,999})try{RemoteWire.key(code);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void pinProofUsesUnsignedRsaAndRejectsMistypedCode()throws Exception{
        // Tiny PUBLIC numbers are a deterministic hash fixture, never a production key.
        byte[] expected=java.security.MessageDigest.getInstance("SHA-256").digest(hex("80ff010001ff01010001abcd"));
        String pin=String.format(Locale.ROOT,"%02XABCD",expected[0]&255);
        assertArrayEquals(expected,RemoteWire.pairingSecret(key("80ff"),key("ff01"),pin));
        invalid(()->RemoteWire.pairingSecret(key("80ff"),key("ff01"),String.format(Locale.ROOT,"%02XABCD",(expected[0]+1)&255)));
        invalid(()->RemoteWire.pairingSecret(key("80ff"),key("ff01"),"12345G"));
        invalid(()->RemoteWire.pairingSecret(key("80ff"),key("ff01"),null));
    }
    @Test public void pairingRequiresCorrectStatusVersionAndAck()throws Exception{
        RemoteWire.requirePairing(hex("080210c8015a00"),11);
        for(String wrong:new String[]{"080110c8015a00","08021092035a00","080210c801a20100","080210c8015800"})invalid(()->RemoteWire.requirePairing(hex(wrong),11));
    }
    @Test public void framingHandlesSplitReadsAndMultipleFrames()throws Exception{
        byte[] one=RemoteWire.key(19),two=RemoteWire.key(23);ByteArrayOutputStream encoded=new ByteArrayOutputStream();RemoteWire.write(encoded,one);RemoteWire.write(encoded,two);
        InputStream fragmented=new FilterInputStream(new ByteArrayInputStream(encoded.toByteArray())){public int read(byte[] b,int off,int len)throws IOException{return super.read(b,off,Math.min(len,1));}};
        assertArrayEquals(one,RemoteWire.read(fragmented));assertArrayEquals(two,RemoteWire.read(fragmented));assertEquals(-1,fragmented.read());
    }
    @Test public void maliciousLengthsTruncationAndVarintsAreRejected()throws Exception{
        for(byte[] malformed:new byte[][]{hex("00"),hex("818004"),hex("040801"),hex("ffffffffffffffffff02"),hex("80808080808080808080")})invalid(()->RemoteWire.read(new ByteArrayInputStream(malformed)));
        for(byte[] malformed:new byte[][]{hex("00"),hex("08010802"),hex("0a05ffff"),hex("0b00"),hex("0d01"),new byte[65537]})invalid(()->RemoteWire.parse(malformed));
        invalid(()->RemoteWire.write(new ByteArrayOutputStream(),new byte[0]));
    }
    @Test public void handshakeNegotiatesOnlySupportedFeaturesAndEchoesPing()throws Exception{
        RemoteWire.State state=new RemoteWire.State();assertFalse(state.ready());
        byte[] configured=state.accept(hex("0a0308ff07"));Map<Integer,Object> payload=RemoteWire.parse(RemoteWire.bytes(RemoteWire.parse(configured),1));assertEquals(99,RemoteWire.number(payload,1));
        assertArrayEquals(hex("12020863"),state.accept(hex("120308ff07")));
        assertArrayEquals(hex("4a020825"),state.accept(hex("42020825")));
        assertArrayEquals(hex("4a020800"),state.accept(hex("4200"))); // omitted proto3 default
        assertNull(state.accept(hex("c202020801")));assertTrue(state.ready());
    }
    @Test public void optionalActivationAndCapabilityRenegotiationAreSupported()throws Exception{
        RemoteWire.State state=new RemoteWire.State();state.accept(hex("0a020803"));state.accept(hex("c20200"));assertTrue(state.ready());
        assertEquals(3,RemoteWire.number(RemoteWire.parse(RemoteWire.bytes(RemoteWire.parse(state.accept(hex("0a020803"))),1)),1));
        invalid(()->new RemoteWire.State().accept(hex("c20200")));
        invalid(()->new RemoteWire.State().accept(hex("0a020801")));
        invalid(()->state.accept(hex("1a00")));
    }
}
