import React, { useEffect, useState } from 'react';
const palettes = ['obsidian','forest','moon','ember','parchment'];
function saved() { try { const value=localStorage.getItem('dnd-roll.palette'); return value && palettes.includes(value) ? value : 'obsidian'; } catch { return 'obsidian'; } }
// Visual preferences never hold campaign state or authority.
export function Appearance() {
  const [palette,setPalette]=useState(saved);
  useEffect(()=>{ document.documentElement.dataset.palette=palette; try { localStorage.setItem('dnd-roll.palette',palette); } catch { /* Private browsing may disable storage. */ } },[palette]);
  return <label className="appearance">界面配色<select aria-label="界面配色" value={palette} onChange={event=>setPalette(event.target.value)}><option value="obsidian">黑曜石 · 黄铜</option><option value="forest">深林 · 古金</option><option value="moon">月夜 · 银蓝</option><option value="ember">余烬 · 赤铜</option><option value="parchment">羊皮纸 · 森绿</option></select></label>;
}
