'use strict';
// Test-only receiver: no real credentials or camera addresses.
module.exports=function receiver(){
const state={registered:false,session:false,config:{device_name:'TV de teste',defaults:{},auto_start:false,paused:false,quiet_enabled:false,quiet_start:'22:00',quiet_end:'07:00',ui_theme:'system',startup_animation:true},profile:{username:'douglas',display_name:'Douglas',two_factor:false},events:[],calls:[],cameras:[],phones:[{id:'phone-test',name:'Meu celular'}],notice:null};
const csrf='fixture-csrf',result=(status,data)=>({ok:status<300,status,json:async()=>data,blob:async()=>new Blob([]),data});
async function request(url,opts={}){const pathname=new URL(url,'https://192.168.1.50:8766').pathname,b=opts.body?JSON.parse(opts.body):{},headers=Object.fromEntries(Object.entries(opts.headers||{}).map(([k,v])=>[k.toLowerCase(),v]));state.calls.push({path:pathname,opts});
if(pathname==='/auth/state')return result(200,{registered:state.registered,terms_version:'2026-09-28'});
if(pathname==='/auth/register'){if(b.code!=='123456'||!b.accepted_terms||b.terms_version!=='2026-09-28')return result(400,{error:'Código ou termos inválidos'});state.profile={...state.profile,username:b.username,display_name:b.display_name};state.registered=true;state.session=true;return result(200,{profile:state.profile,csrf});}
if(pathname==='/auth/login'){if(b.username!=='douglas'||b.password!=='long test password')return result(401,{error:'Login inválido'});state.session=true;return result(200,{profile:state.profile,csrf});}
if(!state.session)return result(401,{error:'Entre novamente'});
if(opts.method==='POST'&&headers['x-casanotify-csrf']!==csrf)return result(403,{error:'CSRF ausente'});
if(pathname==='/auth/session')return result(200,{profile:state.profile,csrf});
if(pathname==='/auth/logout'||pathname==='/auth/password'){state.session=false;return result(200,{ok:true});}
if(pathname==='/auth/profile'){state.profile.display_name=b.display_name;return result(200,state.profile);}
if(pathname==='/api/config'){if(opts.method==='POST')state.config={...state.config,...b};return result(200,JSON.parse(JSON.stringify(state.config)));}
if(pathname==='/api/status')return result(200,{device_name:state.config.device_name,paused:state.config.paused,overlay_permission:true,vpn_active:false,tls_fingerprint:'ab'.repeat(32)});
if(pathname==='/api/notify'){state.notice=b;state.events.push({title:b.title,time:Date.now(),result:'exibido'});return result(200,{status:state.config.paused?'suppressed':'displayed'});}
if(pathname==='/api/history')return result(200,{items:state.events});
if(pathname==='/api/clear')return result(200,{status:'cleared'});
if(pathname==='/api/cameras/save'){state.cameras.push({id:'camera-test',name:b.name});return result(200,{items:state.cameras});}
if(pathname==='/api/cameras/delete')state.cameras=state.cameras.filter(c=>c.id!==b.id);
if(pathname==='/api/cameras/test')return result(200,{status:'displayed'});
if(pathname.startsWith('/api/cameras'))return result(200,{items:state.cameras});
if(pathname==='/api/phones/revoke')state.phones=state.phones.filter(p=>p.id!==b.id);
if(pathname.startsWith('/api/phones'))return result(200,{items:state.phones});
if(pathname.startsWith('/api/media'))return result(200,{ok:true});
return result(404,{error:'Sem imagem'});
}
return {state,request};};
