package br.com.casanotify.tv;
import android.content.Context;import android.graphics.*;import android.util.AtomicFile;import java.io.*;import java.util.Base64;

/** Local images are decoded, resized and re-encoded, removing EXIF and original file metadata. */
public final class MediaAssets {
    private final File root;
    public MediaAssets(Context c){root=new File(c.getFilesDir(),"images");root.mkdirs();}
    private File path(String kind){if(!kind.equals("logo")&&!kind.equals("background"))throw new IllegalArgumentException("Tipo de imagem inválido.");return new File(root,kind+".png");}
    public synchronized void save(String kind,String data)throws Exception{
        if(!data.startsWith("data:image/")||data.length()>1500000)throw new IllegalArgumentException("A imagem editada deve ter até 1 MiB.");int comma=data.indexOf(',');if(comma<0)throw new IllegalArgumentException("Imagem inválida.");byte[] bytes=Base64.getDecoder().decode(data.substring(comma+1));if(bytes.length>1048576)throw new IllegalArgumentException("A imagem editada deve ter até 1 MiB.");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<1||options.outHeight<1||options.outWidth>8192||options.outHeight>8192)throw new IllegalArgumentException("Dimensões de imagem inválidas.");int max=kind.equals("logo")?512:1280;options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>max*2)options.inSampleSize*=2;options.inJustDecodeBounds=false;
        Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(image==null)throw new IllegalArgumentException("Não foi possível ler a imagem.");Bitmap out=image;float scale=Math.min(1,max/(float)Math.max(image.getWidth(),image.getHeight()));if(scale<1)out=Bitmap.createScaledBitmap(image,Math.max(1,Math.round(image.getWidth()*scale)),Math.max(1,Math.round(image.getHeight()*scale)),true);
        AtomicFile file=new AtomicFile(path(kind));FileOutputStream stream=null;try{stream=file.startWrite();if(!out.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IOException();file.finishWrite(stream);}catch(Exception e){if(stream!=null)file.failWrite(stream);throw e;}finally{if(out!=image)out.recycle();image.recycle();}
    }
    public synchronized byte[] bytes(String kind)throws Exception{File f=path(kind);if(!f.isFile())return null;try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>6*1024*1024)throw new IOException();out.write(b,0,n);}return out.toByteArray();}}
    public synchronized Bitmap bitmap(String kind){return BitmapFactory.decodeFile(path(kind).getAbsolutePath());}
    public synchronized void remove(String kind){new AtomicFile(path(kind)).delete();}
}
