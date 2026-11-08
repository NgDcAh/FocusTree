'use strict';
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const ROOT = path.resolve(__dirname, '..', '..');
const ARTWORK = path.join(ROOT, 'artwork');
const PORT = Number(process.env.PIXEL_GALLERY_PORT || 4177);
const NO_CACHE = { 'Cache-Control': 'no-store' };
const MIME = { '.html':'text/html; charset=utf-8', '.css':'text/css; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.json':'application/json; charset=utf-8', '.png':'image/png', '.gif':'image/gif', '.webp':'image/webp', '.jpg':'image/jpeg', '.jpeg':'image/jpeg', '.svg':'image/svg+xml' };
let cachedInventory = { folders: [], summary: {}, warnings: [], refreshedAt: null };
let refreshInFlight = null;
function findPxl() {
  const candidates = [process.env.PXL_CLI, path.join(process.env.USERPROFILE || '', '.agents', 'skills', 'pxl-pixel-art', 'bin', 'pxl.js'), path.join(process.env.USERPROFILE || '', '.codex', 'skills', 'pxl-pixel-art', 'bin', 'pxl.js')].filter(Boolean);
  return candidates.find((candidate) => fs.existsSync(candidate));
}
function isWithin(parent, target) { const rel = path.relative(parent, target); return rel === '' || (!rel.startsWith('..' + path.sep) && rel !== '..' && !path.isAbsolute(rel)); }
function walk(dir, predicate, skip = new Set(['out', 'node_modules', '.git'])) {
  if (!fs.existsSync(dir)) return [];
  const found = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) { if (!skip.has(entry.name)) found.push(...walk(full, predicate, skip)); }
    else if (entry.isFile() && predicate(full)) found.push(full);
  }
  return found;
}
function pxlList(source, cli) {
  const stdout = execFileSync(process.execPath, [cli, 'list', source], { cwd: path.dirname(source), encoding: 'utf8', windowsHide: true, timeout: 30000 });
  return stdout.split(/\r?\n/).map((line) => { const m = line.match(/^(scene|sprite|anim|sheet)\s+(\S+)/); return m ? { kind:m[1], name:m[2] } : null; }).filter(Boolean);
}
function sourceInputs(source) {
  const inputs = [source], body = fs.readFileSync(source, 'utf8');
  const refs = /\b(?:image|include)\s*\(\s*(['"])(.*?)\1/g;
  for (const match of body.matchAll(refs)) { const ref = path.resolve(path.dirname(source), match[2]); if (isWithin(ARTWORK, ref)) inputs.push(ref); }
  return [...new Set(inputs)];
}
function expectedOutputs(out, a) {
  if (a.kind === 'scene') return [path.join(out, 'scenes', a.name + '.png')];
  if (a.kind === 'sprite') return [path.join(out, 'sprites', a.name + '.png')];
  if (a.kind === 'anim') return [path.join(out, 'anims', a.name + '.png'), path.join(out, 'anims', a.name + '.json'), path.join(out, 'anims', a.name + '.gif')];
  return [path.join(out, a.name + '.png'), path.join(out, a.name + '.json')];
}
function buildStatus(source, assets, out) {
  const inputs=sourceInputs(source), available=inputs.filter(fs.existsSync), missingInputs=inputs.filter((f)=>!fs.existsSync(f));
  const newestInput=Math.max(...available.map((f)=>fs.statSync(f).mtimeMs)), expected=assets.flatMap((a)=>expectedOutputs(out,a));
  const missingOutputs=expected.filter((f)=>!fs.existsSync(f)), staleOutputs=expected.filter((f)=>fs.existsSync(f)&&fs.statSync(f).mtimeMs<newestInput);
  return {needed:missingOutputs.length>0||staleOutputs.length>0,missingInputs,missingOutputs,staleOutputs};
}
function relArtwork(file) { return path.relative(ARTWORK,file).split(path.sep).join('/'); }
function imageUrl(file) { return '/artwork/'+relArtwork(file).split('/').map(encodeURIComponent).join('/'); }
function safeStat(file) { try{return fs.statSync(file);}catch{return null;} }
function readAtlas(file) {
  try { const data=JSON.parse(fs.readFileSync(file,'utf8')),raw=data.frames,entries=Array.isArray(raw)?raw.map((f,i)=>[String(i),f]):Object.entries(raw||{});
    return entries.map(([key,item],i)=>{const r=item.frame||item.rect||item;if(![r.x,r.y,r.w,r.h].every(Number.isFinite))return null;return{label:item.filename||item.name||key,crop:{x:r.x,y:r.y,w:r.w,h:r.h},duration:item.duration||item.ms||item.msPerFrame||null,index:i};}).filter(Boolean);
  }catch{return[];}
}
function makeAsset(base) { const stat=base.file&&safeStat(base.file);return{...base,url:stat?imageUrl(base.file):null,previewUrl:base.previewFile&&safeStat(base.previewFile)?imageUrl(base.previewFile):null,bytes:stat?.size||0}; }
function sourceInventory(source,folder) {
  const out=path.join(path.dirname(source.file),'out'),assets=[],claimed=new Set();
  for(const def of source.defs){
    if(def.kind==='sheet'){
      const file=path.join(out,def.name+'.png'),json=path.join(out,def.name+'.json');if(!safeStat(file))continue;claimed.add(path.resolve(file));const frames=readAtlas(json);
      assets.push(makeAsset({id:`${source.rel}:${def.name}:sheet`,name:def.name,kind:'sheet',file,jsonFile:safeStat(json)?json:null,frames,folder,source:source.rel,previewFile:path.join(out,'_preview',def.name+'.png')}));
      frames.forEach((f)=>assets.push(makeAsset({id:`${source.rel}:${def.name}:${f.index}`,name:f.label,kind:'sheet-frame',file,crop:f.crop,duration:f.duration,folder,source:source.rel,parentName:def.name,previewFile:null})));continue;
    }
    const dir=def.kind==='scene'?'scenes':def.kind==='anim'?'anims':'sprites',file=path.join(out,dir,def.name+'.png');if(!safeStat(file))continue;claimed.add(path.resolve(file));const preview=path.join(out,'_preview',def.name+'.png');
    if(def.kind==='anim'){
      const json=path.join(out,dir,def.name+'.json'),gif=path.join(out,dir,def.name+'.gif'),frames=readAtlas(json);
      assets.push(makeAsset({id:`${source.rel}:${def.name}:animation`,name:def.name,kind:'animation',file:safeStat(gif)?gif:file,stripFile:file,jsonFile:safeStat(json)?json:null,frames,folder,source:source.rel,previewFile:path.join(out,'_preview',def.name+'.gif')}));
      frames.forEach((f)=>assets.push(makeAsset({id:`${source.rel}:${def.name}:frame:${f.index}`,name:`${def.name} · frame ${f.index+1}`,kind:'animation-frame',file,crop:f.crop,duration:f.duration,folder,source:source.rel,parentName:def.name,previewFile:preview})));
    }else assets.push(makeAsset({id:`${source.rel}:${def.name}`,name:def.name,kind:def.kind,file,folder,source:source.rel,previewFile:preview}));
  }
  return{file:source.rel,status:source.status,message:source.message,assets};
}
async function rescanAndBuild(){
  if(refreshInFlight)return refreshInFlight;
  refreshInFlight=Promise.resolve().then(()=>{
    const cli=findPxl(),files=walk(ARTWORK,(f)=>f.toLowerCase().endsWith('.px.js')),warnings=[],sources=[];let built=0;
    for(const file of files){
      const rel=relArtwork(file);let defs=[],status='current',message='';
      if(cli){try{defs=pxlList(file,cli);}catch(e){warnings.push(`${rel}: PXL list failed: ${e.stderr?.toString().trim()||e.message}`);}}
      const out=path.join(path.dirname(file),'out');
      if(!cli){status='PXL missing';message='Set PXL_CLI or install pxl-pixel-art.';}
      else if(defs.length){
        const fresh=buildStatus(file,defs,out);if(fresh.missingInputs.length)warnings.push(`${rel}: missing input: ${fresh.missingInputs.map((f)=>path.relative(path.dirname(file),f)).join(', ')}`);
        if(fresh.needed){try{const log=execFileSync(process.execPath,[cli,'build',file],{cwd:path.dirname(file),encoding:'utf8',windowsHide:true,timeout:120000});built++;status='built';message=log.trim().split(/\r?\n/).slice(-2).join(' · ');}catch(e){status='build failed';message=(e.stderr?.toString()||e.stdout?.toString()||e.message).trim().slice(-1200);warnings.push(`${rel}: build failed: ${message}`);}}
      }
      sources.push({file,rel,defs,status,message});
    }
    const map=new Map();for(const s of sources){const folder=path.relative(ARTWORK,path.dirname(s.file)).split(path.sep).join('/')||'.';if(!map.has(folder))map.set(folder,[]);map.get(folder).push(s);}
    const folders=[...map].sort(([a],[b])=>a.localeCompare(b)).map(([folder,list])=>({path:folder,sources:list.map((s)=>sourceInventory(s,folder))}));
    const all=folders.flatMap((f)=>f.sources.flatMap((s)=>s.assets));
    cachedInventory={folders,summary:{folders:folders.length,sources:sources.length,assets:all.length,built,animations:all.filter((a)=>a.kind==='animation').length,frames:all.filter((a)=>a.kind==='animation-frame'||a.kind==='sheet-frame').length},warnings,refreshedAt:new Date().toISOString()};return cachedInventory;
  }).finally(()=>{refreshInFlight=null;});return refreshInFlight;
}
function send(res,status,body,type='application/json; charset=utf-8'){res.writeHead(status,{...NO_CACHE,'Content-Type':type});res.end(body);}
function sendFile(res,file){const st=safeStat(file);if(!st||!st.isFile())return send(res,404,'Not found','text/plain; charset=utf-8');res.writeHead(200,{...NO_CACHE,'Content-Type':MIME[path.extname(file).toLowerCase()]||'application/octet-stream','Content-Length':st.size});fs.createReadStream(file).pipe(res);}
const ui=new Map([['/','index.html'],['/app.css','app.css'],['/app.js','app.js']]);
const server=http.createServer(async(req,res)=>{const url=new URL(req.url,`http://127.0.0.1:${PORT}`);
  if(req.method==='GET'&&url.pathname==='/api/gallery'){if(!cachedInventory.refreshedAt)await rescanAndBuild();return send(res,200,JSON.stringify(cachedInventory));}
  if(req.method==='POST'&&url.pathname==='/api/refresh'){try{return send(res,200,JSON.stringify(await rescanAndBuild()));}catch(e){return send(res,500,JSON.stringify({error:e.message,...cachedInventory}));}}
  if(req.method==='GET'&&url.pathname.startsWith('/artwork/')){let rel;try{rel=decodeURIComponent(url.pathname.slice(9));}catch{return send(res,400,'Bad path','text/plain; charset=utf-8');}const file=path.resolve(ARTWORK,rel);if(!isWithin(ARTWORK,file))return send(res,403,'Forbidden','text/plain; charset=utf-8');return sendFile(res,file);}
  if(req.method==='GET'&&ui.has(url.pathname))return sendFile(res,path.join(__dirname,ui.get(url.pathname)));return send(res,404,'Not found','text/plain; charset=utf-8');
});
server.listen(PORT,'127.0.0.1',()=>{console.log(`Pixel Art Gallery listening at http://127.0.0.1:${PORT}`);rescanAndBuild().then((x)=>console.log(`Scanned ${x.summary.sources} sources, ${x.summary.assets} images/frames; built ${x.summary.built}.`)).catch(console.error);});
