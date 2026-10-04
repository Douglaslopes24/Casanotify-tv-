'use strict';
// The panel is bundled with the APK. This transport exists only at the app's local origin.
if(location.origin==='https://app.casanotify.local'){
(()=>{
let port=null,seq=0;const waiting=new Map();let readyResolve;
const ready=new Promise(resolve=>{readyResolve=resolve;});
window.addEventListener('message',event=>{
 if(event.data!=='casanotify-native'||event.ports.length!==1||port)return;
 port=event.ports[0];port.onmessage=e=>{let reply;try{reply=JSON.parse(e.data);}catch(_){return;}const item=waiting.get(reply.id);if(!item)return;waiting.delete(reply.id);clearTimeout(item.timer);if(reply.error)item.reject(new Error(reply.error));else item.resolve(reply.result);};readyResolve();
});
async function request(command,data){await Promise.race([ready,new Promise((_,reject)=>setTimeout(()=>reject(new Error('Feche e abra o aplicativo para reconectar.')),10000))]);return new Promise((resolve,reject)=>{const id=++seq;const timer=setTimeout(()=>{waiting.delete(id);reject(new Error('Sem resposta. Confira a TV ou tente novamente.'));},command==='pickImage'?300000:data.path?.startsWith('/auth/')?90000:30000);waiting.set(id,{resolve,reject,timer});port.postMessage(JSON.stringify({id,command,...data}));});}
window.CasaControl={request,fetch:async(path,options={})=>{const r=await request('api',{path,method:options.method||'GET',body:options.body||null,csrf:options.headers?.['X-CasaNotify-CSRF']||''});return {status:r.status,ok:r.status>=200&&r.status<300,json:async()=>r.data,blob:async()=>{const bytes=Uint8Array.from(atob(r.image||''),c=>c.charCodeAt(0));return new Blob([bytes],{type:'image/png'});}};}};
})();
}
