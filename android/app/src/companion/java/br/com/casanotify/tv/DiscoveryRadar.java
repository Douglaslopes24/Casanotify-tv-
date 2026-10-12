package br.com.casanotify.tv;
import android.content.Context;import android.graphics.*;import android.view.View;

/** Illustration of local discovery; found receivers always come from authenticated NSD flow. */
public final class DiscoveryRadar extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public DiscoveryRadar(Context context){super(context);setContentDescription("Busca de receptores CasaNotify na rede local");}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(getWidth(),getHeight())*.42f;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),1));
        for(int i=1;i<=3;i++){paint.setColor(Ui.dark(getContext())?0xff294e33:0xffa5c39b);canvas.drawCircle(x,y,r*i/3,paint);}
        paint.setColor(0xffa3e635);paint.setStrokeWidth(Ui.dp(getContext(),3));RectF tv=new RectF(x-r*.32f,y-r*.22f,x+r*.32f,y+r*.2f);canvas.drawRoundRect(tv,Ui.dp(getContext(),5),Ui.dp(getContext(),5),paint);canvas.drawLine(x,y+r*.2f,x,y+r*.32f,paint);canvas.drawLine(x-r*.13f,y+r*.32f,x+r*.13f,y+r*.32f,paint);
        paint.setStyle(Paint.Style.FILL);canvas.drawCircle(x+r*.72f,y-r*.34f,Ui.dp(getContext(),5),paint);canvas.drawCircle(x-r*.63f,y+r*.57f,Ui.dp(getContext(),4),paint);
    }
}
