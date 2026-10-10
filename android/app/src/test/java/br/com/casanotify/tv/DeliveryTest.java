package br.com.casanotify.tv;

import org.junit.*;
import static org.junit.Assert.*;
import org.json.*;

public class DeliveryTest {
    private static class Memory implements AccountManager.Store{String raw="";public String read(){return raw;}public void write(String value){raw=value;}public void delete(){raw="";}}
    private static class Clock implements AccountManager.Clock{long at=1800000000000L;public long now(){return at;}}
    @Test public void receiptsSurviveProcessRestartAndAreBoundToDestination()throws Exception{
        Memory memory=new Memory();Clock clock=new Clock();MirrorReceipts receipts=new MirrorReceipts(memory,clock);receipts.delivered("tv-and-key-A","notice1");
        receipts=new MirrorReceipts(memory,clock);assertTrue(receipts.contains("tv-and-key-A","notice1"));assertFalse(receipts.contains("tv-and-key-B","notice1"));
        receipts.delivered("tv-and-key-B","notice2");assertFalse(receipts.contains("tv-and-key-A","notice1"));assertTrue(receipts.contains("tv-and-key-B","notice2"));
        JSONObject item=new JSONArray(memory.raw).getJSONObject(0);assertEquals(3,item.length());assertFalse(item.has("message"));
    }
    @Test public void receiptsExpireAndStayBounded()throws Exception{
        Memory memory=new Memory();Clock clock=new Clock();MirrorReceipts receipts=new MirrorReceipts(memory,clock);for(int i=0;i<520;i++)receipts.delivered("tv","notice"+i);
        assertEquals(512,new JSONArray(memory.raw).length());assertFalse(receipts.contains("tv","notice0"));assertTrue(receipts.contains("tv","notice519"));
        receipts.delivered("tv","notice519");assertEquals(512,new JSONArray(memory.raw).length());clock.at+=MirrorOutbox.MAX_AGE;assertFalse(receipts.contains("tv","notice519"));assertEquals("",memory.raw);
        receipts.delivered("tv","new");clock.at--;assertFalse(receipts.contains("tv","new"));
    }
    @Test public void recoveryNeverIncludesOldDisabledOrFutureNotices(){
        long now=1800000000000L;assertTrue(MirrorReceipts.recover(now-10000,now-20000,now));
        assertFalse(MirrorReceipts.recover(now-30000,now-20000,now));assertFalse(MirrorReceipts.recover(now-MirrorOutbox.MAX_AGE,0,now));assertFalse(MirrorReceipts.recover(now+1,0,now));assertFalse(MirrorReceipts.recover(0,0,now));
    }
    @Test public void cameraOperationsRequireHomeAssistantRole()throws Exception{
        for(String role:new String[]{"control","phone","unknown"}){
            assertFalse(CameraAccess.allowed(role,"POST","/api/notify",new JSONObject().put("video_url","rtsp://camera.test/live")));
            assertFalse(CameraAccess.allowed(role,"POST","/api/notify",new JSONObject().put("camera_id","legacy")));
            assertFalse(CameraAccess.allowed(role,"GET","/api/cameras",new JSONObject()));
            assertFalse(CameraAccess.allowed(role,"POST","/api/cameras/save",new JSONObject()));
        }
        assertTrue(CameraAccess.allowed("ha","POST","/api/notify",new JSONObject().put("video_url","rtsp://camera.test/live")));
        assertTrue(CameraAccess.allowed("ha","POST","/api/cameras/delete",new JSONObject()));
        assertTrue(CameraAccess.allowed("control","POST","/api/notify",new JSONObject().put("image_url","http://camera.test/still.jpg")));
    }
}
