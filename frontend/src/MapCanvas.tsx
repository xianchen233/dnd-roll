import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Application, Container, Graphics, ImageSource, Rectangle, Sprite, Text, Texture } from 'pixi.js';
import type { Cell, Scene } from './types';
import { distance, snapCell, zoomAt, type Viewport } from './map-geometry';
interface Props { scene: Scene; selected?: string; disabled: boolean; onSelect(id: string): void; onMove(x: number, y: number): void; mapUrl?: string; distancePerCell?: number; revealed?: Cell[]; path?: Cell[]; fogLocked?: boolean; onRectangle?(a: Cell, b: Cell, reveal: boolean): void }
type Tool = 'move' | 'pan' | 'measure' | 'reveal' | 'cover';
interface Drawing { layer: Container; unitsLayer: Container; draft: Graphics; route: Graphics; units: Map<string, Container>; checkGesture(): void }

export function MapCanvas(props: Props) {
  const host = useRef<HTMLDivElement>(null), current = useRef(props), renderer = useRef<Application | null>(null), viewport = useRef<Viewport>({ x: 0, y: 0, scale: 1 }), reset = useRef<() => void>(() => {}), resetInteraction = useRef<() => void>(() => {}), tool = useRef<Tool>('move'), drawing = useRef<Drawing | undefined>(undefined);
  const [ready, setReady] = useState(0), [error, setError] = useState(''), [mode, setMode] = useState<Tool>('move'), [scale, setScale] = useState(1), [measurement, setMeasurement] = useState<string>(''), [interactionScope, setInteractionScope] = useState(''), [loadedUrl, setLoadedUrl] = useState('');
  const scope = `${props.scene.campaignId}/${props.scene.id}/${props.scene.width}/${props.scene.height}/${props.scene.gridSize}`;
  current.current = props; tool.current = mode;
  // No animations run on this tabletop. Paint only mutations; image invalidation
  // must paint synchronously so an idle canvas cannot retain revoked pixels.
  function paint() { renderer.current?.render(); }
  useEffect(() => {
    const app = new Application(); let disposed = false, initialized = false;
    void app.init({ width: 1152, height: 768, backgroundColor: 0x141e27, antialias: true, resolution: Math.min(window.devicePixelRatio || 1, 2), autoDensity: true, autoStart: false }).then(() => {
      initialized = true;
      if (disposed) { app.destroy(true, { children: true }); return; }
      renderer.current = app; host.current?.appendChild(app.canvas); setReady(n => n + 1);
    }).catch(() => { if (!disposed) setError('地图图形初始化失败；仍可通过坐标表单操作。'); });
    return () => { disposed = true; renderer.current = null; if (initialized) app.destroy(true, { children: true }); };
  }, []);
  // Layout cleanup removes old pixels before the browser paints a new invalidated scene.
  useLayoutEffect(() => {
    const app = renderer.current; if (!app) return;
    const { scene } = props, cell = scene.gridSize, width = scene.width * cell, height = scene.height * cell;
    app.renderer.resize(width, height); app.canvas.style.width = '100%'; app.canvas.style.height = 'auto';
    const layer = new Container(), backdrop = new Graphics().rect(0, 0, width, height).fill(0x18232c), grid = new Graphics(), overlay = new Graphics(), draft = new Graphics(), unitsLayer = new Container();
    layer.addChild(backdrop); app.stage.addChild(layer);
    function applyView() { const v = viewport.current; layer.position.set(v.x, v.y); layer.scale.set(v.scale); setScale(v.scale); paint(); }
    applyView(); reset.current = () => { viewport.current = { x: 0, y: 0, scale: 1 }; applyView(); setMeasurement(''); resetInteraction.current(); };
    for (let x = 0; x <= scene.width; x++) grid.moveTo(x * cell, 0).lineTo(x * cell, height);
    for (let y = 0; y <= scene.height; y++) grid.moveTo(0, y * cell).lineTo(width, y * cell);
    grid.stroke({ color: 0x73828b, alpha: 0.4, width: 1 }); layer.addChild(grid);
    const units = new Map<string, Container>(), route = new Graphics();
    layer.addChild(draft, route, unitsLayer);
    layer.addChild(overlay); app.stage.eventMode = 'static'; app.stage.hitArea = new Rectangle(0, 0, width, height);
    let gesture: { start: Cell; global: Cell; tokenId?: string; selectedId?: string; tokenState?: { id: string; version: number; x: number; y: number }; sceneVersion: number; mode: Tool; pan: boolean; moved: boolean; origin: Viewport } | undefined, measureStart: Cell | undefined;
    function matchesCurrent() {
      if (!gesture) return true;
      const p = current.current, g = gesture;
      if (g.selectedId !== p.selected) return false;
      if ((g.mode === 'reveal' || g.mode === 'cover') && g.sceneVersion !== p.scene.version) return false;
      if (g.tokenState) { const t = p.scene.tokens.find(t => t.id === g.tokenState!.id); if (!t?.canMove || t.version !== g.tokenState.version || t.x !== g.tokenState.x || t.y !== g.tokenState.y) return false; }
      return true;
    }
    const point = (e: { global: { x: number; y: number } }) => layer.toLocal(e.global);
    const down = (event: import('pixi.js').FederatedPointerEvent) => {
      if (event.button !== 0 && event.button !== 1) return;
      const p = point(event), c = snapCell(p, cell, scene.width, scene.height); if (!c) return;
      const token = current.current.scene.tokens.find(t => t.canMove && Math.hypot(p.x - (t.x + 0.5) * cell, p.y - (t.y + 0.5) * cell) <= cell * 0.4);
      const controlled = tool.current === 'move' ? token ?? current.current.scene.tokens.find(t => t.id === current.current.selected) : undefined;
      gesture = { start: c, global: { x: event.global.x, y: event.global.y }, tokenId: tool.current === 'move' ? token?.id : undefined, selectedId: current.current.selected, tokenState: controlled ? { id: controlled.id, version: controlled.version, x: controlled.x, y: controlled.y } : undefined, sceneVersion: current.current.scene.version, mode: tool.current, pan: tool.current === 'pan' || event.button === 1, moved: false, origin: { ...viewport.current } };
      try { app.canvas.setPointerCapture(event.pointerId); } catch { /* Unsupported canvas capture retains Pixi global move/up. */ }
    };
    const move = (event: import('pixi.js').FederatedPointerEvent) => {
      if (!gesture) return; if (!matchesCurrent()) { cancel(); return; } const g = gesture, dx = event.global.x - g.global.x, dy = event.global.y - g.global.y;
      if (Math.hypot(dx, dy) > 4) g.moved = true;
      if (g.pan) { viewport.current = { ...g.origin, x: g.origin.x + dx, y: g.origin.y + dy }; applyView(); return; }
      const p = point(event), c = snapCell(p, cell, scene.width, scene.height); if (!c) return;
      overlay.clear();
      if (tool.current === 'move' && g.tokenId === current.current.selected && !current.current.disabled) units.get(g.tokenId!)?.position.set((c.x + 0.5) * cell, (c.y + 0.5) * cell);
      if ((tool.current === 'reveal' || tool.current === 'cover') && !current.current.fogLocked) overlay.rect(Math.min(c.x, g.start.x) * cell, Math.min(c.y, g.start.y) * cell, (Math.abs(c.x - g.start.x) + 1) * cell, (Math.abs(c.y - g.start.y) + 1) * cell).stroke({ color: tool.current === 'reveal' ? 0x65c997 : 0xf0b09a, width: 3 });
      paint();
    };
    const up = (event: import('pixi.js').FederatedPointerEvent) => {
      if (!matchesCurrent()) { cancel(); return; } const g = gesture; gesture = undefined; if (!g) return;
      try { app.canvas.releasePointerCapture(event.pointerId); } catch { /* Already released. */ }
      const c = snapCell(point(event), cell, scene.width, scene.height);
      for (const t of current.current.scene.tokens) units.get(t.id)?.position.set((t.x + 0.5) * cell, (t.y + 0.5) * cell);
      paint();
      if (!c || g.pan) return;
      if (tool.current === 'measure') {
        if (!measureStart) { measureStart = c; setMeasurement(`起点 (${c.x}, ${c.y})，请选择终点`); }
        else { overlay.clear().moveTo((measureStart.x + 0.5) * cell, (measureStart.y + 0.5) * cell).lineTo((c.x + 0.5) * cell, (c.y + 0.5) * cell).stroke({ color: 0xe0bd83, width: 3 }); setMeasurement(`${distance(measureStart, c, current.current.distancePerCell ?? 1).toFixed(2)} ${current.current.distancePerCell === undefined ? '格' : '距离单位'}`); measureStart = undefined; }
      } else if ((tool.current === 'reveal' || tool.current === 'cover') && !current.current.fogLocked) { current.current.onRectangle?.(g.start, c, tool.current === 'reveal'); overlay.clear(); }
      else if (tool.current === 'move' && !current.current.disabled) {
        if (g.tokenId && (!g.moved || g.tokenId !== current.current.selected)) current.current.onSelect(g.tokenId);
        else current.current.onMove(c.x, c.y);
      }
      paint();
    };
    const cancel = () => { gesture = undefined; measureStart = undefined; overlay.clear(); for (const t of current.current.scene.tokens) units.get(t.id)?.position.set((t.x + 0.5) * cell, (t.y + 0.5) * cell); paint(); };
    resetInteraction.current = cancel;
    drawing.current = { layer, unitsLayer, draft, route, units, checkGesture: () => { if (!matchesCurrent()) cancel(); } };
    const wheel = (event: WheelEvent) => {
      event.preventDefault(); const bounds = app.canvas.getBoundingClientRect(), p = { x: (event.clientX - bounds.left) * width / bounds.width, y: (event.clientY - bounds.top) * height / bounds.height };
      viewport.current = zoomAt(viewport.current, p, event.deltaY < 0 ? 1.2 : 1 / 1.2); applyView();
    };
    app.stage.on('pointerdown', down).on('globalpointermove', move).on('pointerup', up).on('pointerupoutside', up).on('pointercancel', cancel);
    app.canvas.addEventListener('wheel', wheel, { passive: false });
    setInteractionScope(scope);
    return () => {
      drawing.current = undefined; cancel();
      app.canvas.removeEventListener('wheel', wheel); app.stage.off('pointerdown', down).off('globalpointermove', move).off('pointerup', up).off('pointerupoutside', up).off('pointercancel', cancel);
      if (!layer.destroyed) { layer.parent?.removeChild(layer); layer.destroy({ children: true }); }
      paint();
    };
  }, [scope, ready]);
  // Safe image resources have their own lifetime. Refreshing pixels never removes
  // stage handlers or the same-scene pointer gesture, and still clears before paint.
  useLayoutEffect(() => {
    const d = drawing.current; if (!d || !props.mapUrl) return;
    let disposed = false, texture: Texture | undefined, sprite: Sprite | undefined;
    const image = new Image(), url = props.mapUrl;
    image.onload = () => {
      if (disposed) return;
      texture = new Texture({ source: new ImageSource({ resource: image, scaleMode: 'nearest' }) });
      sprite = new Sprite(texture); sprite.width = props.scene.width * props.scene.gridSize; sprite.height = props.scene.height * props.scene.gridSize;
      d.layer.addChildAt(sprite, 1); paint(); setLoadedUrl(url);
    };
    image.onerror = () => { if (!disposed) setError('安全地图图片读取失败，请刷新场景。'); };
    image.src = url;
    return () => { disposed = true; image.onload = null; image.onerror = null; image.src = ''; if (sprite && !sprite.destroyed) { sprite.parent?.removeChild(sprite); sprite.destroy(); } texture?.destroy(true); paint(); };
  }, [props.mapUrl, scope, ready]);
  useLayoutEffect(() => {
    const d = drawing.current; if (!d) return; d.checkGesture();
    for (const child of [...d.unitsLayer.children]) { d.unitsLayer.removeChild(child); child.destroy({ children: true }); } d.units.clear();
    const cell = props.scene.gridSize;
    for (const token of props.scene.tokens) {
      const unit = new Container(); unit.position.set((token.x + 0.5) * cell, (token.y + 0.5) * cell);
      const marker = new Graphics().circle(0, 0, cell * 0.33).fill(token.canMove ? 0xc49d5e : 0x587487).stroke({ color: token.id === props.selected ? 0xffffff : 0x253440, width: token.id === props.selected ? 3 : 2 });
      const initial = new Text({ text: token.name.slice(0, 1), style: { fontFamily: 'sans-serif', fontSize: cell * 0.35, fill: 0x111a21, fontWeight: 'bold' } }); initial.anchor.set(0.5);
      unit.addChild(marker, initial); d.unitsLayer.addChild(unit); d.units.set(token.id, unit);
    }
    paint();
  }, [props.scene, props.selected, scope, ready]);
  useLayoutEffect(() => {
    const d = drawing.current; if (!d) return; d.draft.clear(); const cell = props.scene.gridSize;
    for (const c of props.revealed ?? []) d.draft.rect(c.x * cell, c.y * cell, cell, cell);
    d.draft.fill({ color: 0x65c997, alpha: 0.12 });
    paint();
  }, [props.revealed, scope, ready]);
  useLayoutEffect(() => {
    const d=drawing.current;if(!d)return;d.route.clear();const cell=props.scene.gridSize;
    for(const [i,p] of (props.path??[]).entries()){const x=(p.x+0.5)*cell,y=(p.y+0.5)*cell;if(i===0)d.route.moveTo(x,y);else d.route.lineTo(x,y);}
    d.route.stroke({color:0xe0bd83,width:4});paint();
  },[props.path,scope,ready]);
  useLayoutEffect(() => { viewport.current = { x: 0, y: 0, scale: 1 }; reset.current(); setMeasurement(''); }, [props.scene.id]);
  const tools: [Tool, string][] = [['move', '选择与移动'], ['pan', '平移地图'], ['measure', '两点测距'], ...(props.onRectangle ? [['reveal', '矩形揭露'], ['cover', '矩形遮盖']] as [Tool, string][] : [])];
  return <><div className="map-tools" role="toolbar" aria-label="地图交互">{tools.map(([value, label]) => <button key={value} aria-pressed={mode === value} disabled={(value === 'reveal' || value === 'cover') && props.fogLocked} onClick={() => { resetInteraction.current(); setMode(value); setMeasurement(''); }}>{label}</button>)}<button onClick={() => reset.current()}>重置视图</button><output aria-label="地图缩放">{Math.round(scale * 100)}%</output></div><p className="hint">滚轮以光标为中心缩放；中键或平移工具拖动。选中单位后拖动预览并吸附，松开后等待服务器确认。测距为直线几何辅助。</p><div className="map-surface"><div ref={host} aria-label="场景网格地图" role="img" data-map-image={props.mapUrl && loadedUrl === props.mapUrl ? 'ready' : 'empty'} data-interaction-ready={interactionScope === scope ? 'ready' : 'loading'} />{error && <p role="alert">{error}</p>}</div>{measurement && <output className="map-measurement" aria-label="测距结果">{measurement}</output>}</>;
}
