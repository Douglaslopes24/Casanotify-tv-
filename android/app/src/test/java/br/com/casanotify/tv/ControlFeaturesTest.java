package br.com.casanotify.tv;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;

public class ControlFeaturesTest {
    @Test public void remoteRequiresControlAndOwnerSession(){
        assertTrue(RemotePolicy.allowed("control",true));
        assertFalse(RemotePolicy.allowed("control",false));
        for(String role:new String[]{"phone","ha","unknown",""})assertFalse(RemotePolicy.allowed(role,true));
    }
    @Test public void remoteRejectsInjectionAndExtraFields()throws Exception{
        for(String command:new String[]{"shell","keycode:26","HOME","up;reboot","","power"})
            assertThrows(IllegalArgumentException.class,()->RemotePolicy.command(new JSONObject().put("command",command)));
        assertThrows(IllegalArgumentException.class,()->RemotePolicy.command(new JSONObject().put("command","up").put("repeat",100000)));
        for(String command:RemotePolicy.COMMANDS)assertEquals(command,RemotePolicy.command(new JSONObject().put("command",command)));
    }
    @Test public void remoteRevocationAndFloodProtection(){
        final int[] executions={0};TvRemote.Executor executor=command->{executions[0]++;return true;};
        try{TvRemote.attach(executor);assertEquals(400,TvRemote.execute("shell"));
            for(int i=0;i<16;i++)assertEquals(200,TvRemote.execute("up"));
            assertEquals(429,TvRemote.execute("up"));assertEquals(16,executions[0]);
            TvRemote.detach(executor);assertFalse(TvRemote.available());assertEquals(409,TvRemote.execute("home"));assertEquals(16,executions[0]);
        }finally{TvRemote.detach(executor);}
    }
    @Test public void cameraRoleBoundaryIncludesAllManualCommands()throws Exception{
        for(String path:new String[]{"/api/cameras","/api/cameras/status","/api/cameras/show","/api/cameras/release","/api/cameras/delete","/api/cameras/save"}){
            assertFalse(CameraAccess.allowed("phone","POST",path,new JSONObject()));
            assertFalse(CameraAccess.allowed("unknown","GET",path,new JSONObject()));
            assertTrue(CameraAccess.allowed("control","POST",path,new JSONObject()));
            assertTrue(CameraAccess.allowed("ha","POST",path,new JSONObject()));
        }
    }
    @Test public void rtspDefaultsAndRoundTrip()throws Exception{
        Notice notice=new Notice(new JSONObject().put("video_url","rtsp://camera.test/live"),new JSONObject());
        assertEquals("tcp",notice.rtsp.transport);assertEquals("auto",notice.rtsp.decoder);assertEquals(800,notice.rtsp.bufferMs);
        Notice restored=new Notice(notice.json,new JSONObject());assertEquals(800,restored.rtsp.bufferMs);
    }
    @Test public void rtspRejectsUnboundedAndMistypedOptions()throws Exception{
        for(Object buffer:new Object[]{249,3001,500.5,"800",true,JSONObject.NULL})
            assertThrows(IllegalArgumentException.class,()->new RtspOptions(new JSONObject().put("video_buffer_ms",buffer)));
        for(String key:new String[]{"video_decoder","video_transport"})assertThrows(IllegalArgumentException.class,()->new RtspOptions(new JSONObject().put(key,"arbitrary")));
        assertEquals(250,new RtspOptions(new JSONObject().put("video_buffer_ms",250)).bufferMs);
        assertEquals(3000,new RtspOptions(new JSONObject().put("video_buffer_ms",3000)).bufferMs);
    }
    @Test public void fallbackIsOnlyOnceAndOnlyVideoDecoderFailures()throws Exception{
        RtspOptions auto=new RtspOptions(new JSONObject());assertTrue(auto.mayRetry(false,true));
        assertFalse(auto.mayRetry(true,true));assertFalse(auto.mayRetry(false,false));
        for(String mode:new String[]{"hardware","software"})assertFalse(new RtspOptions(new JSONObject().put("video_decoder",mode)).mayRetry(false,true));
    }
    @Test public void bridgeAllowsOnlyDefinedRoutes(){
        for(String path:new String[]{"/api/cameras/save","/api/cameras/show","/api/cameras/release","/api/remote/command"})ControlClient.validate(path,"POST","{}");
        for(String path:new String[]{"/api/remote/shell","/api/cameras/../notify","/api/remote/command?repeat=100","https://attacker.test/"})assertThrows(IllegalArgumentException.class,()->ControlClient.validate(path,"POST","{}"));
        assertThrows(IllegalArgumentException.class,()->ControlClient.validate("/api/remote/command","GET",null));
    }
}
