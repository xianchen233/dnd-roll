import React, { useEffect, useRef, useState } from 'react';
import { Application, Container, Graphics, Rectangle, Text } from 'pixi.js';
import type { Scene } from './types';
interface Props { scene: Scene; selected?: string; disabled: boolean; onSelect(id: string): void; onMove(x: number, y: number): void }
export function MapCanvas(props: Props) {
  const host = useRef<HTMLDivElement>(null), current = useRef(props), renderer = useRef<Application | null>(null);
  const [ready, setReady] = useState(0), [error, setError] = useState('');
  current.current = props;
  useEffect(() => {
    const app = new Application(); let disposed = false, initialized = false;
    void app.init({ width: 1152, height: 768, backgroundColor: 0x141e27, antialias: true, resolution: Math.min(window.devicePixelRatio || 1, 2), autoDensity: true }).then(() => {
      initialized = true;
      if (disposed) { app.destroy(true, { children: true }); return; }
      renderer.current = app; host.current?.appendChild(app.canvas); setReady(n => n + 1);
    }).catch(() => { if (!disposed) setError('地图图形初始化失败；仍可通过右侧坐标表单操作。'); });
    return () => { disposed = true; renderer.current = null; if (initialized) app.destroy(true, { children: true }); };
  }, []);
  useEffect(() => {
    const app = renderer.current; if (!app) return;
    const { scene } = props, cell = scene.gridSize;
    app.renderer.resize(scene.width * cell, scene.height * cell);
    // autoDensity writes fixed inline dimensions on resize; scale the complete
    // logical grid into its panel instead of clipping cells off the right edge.
    app.canvas.style.width = '100%';
    app.canvas.style.height = 'auto';
    const layer = new Container(), grid = new Graphics();
    grid.rect(0, 0, scene.width * cell, scene.height * cell).fill(0x18232c);
    for (let x = 0; x <= scene.width; x++) grid.moveTo(x * cell, 0).lineTo(x * cell, scene.height * cell);
    for (let y = 0; y <= scene.height; y++) grid.moveTo(0, y * cell).lineTo(scene.width * cell, y * cell);
    grid.stroke({ color: 0x30414b, width: 1 });
    grid.eventMode = 'static'; grid.hitArea = new Rectangle(0, 0, scene.width * cell, scene.height * cell);
    grid.on('pointertap', event => { const point = event.getLocalPosition(grid); if (!current.current.disabled) current.current.onMove(Math.floor(point.x / cell), Math.floor(point.y / cell)); });
    layer.addChild(grid);
    scene.tokens.forEach(token => {
      const unit = new Container(); unit.position.set((token.x + 0.5) * cell, (token.y + 0.5) * cell);
      const marker = new Graphics().circle(0, 0, cell * 0.33).fill(token.canMove ? 0xc49d5e : 0x587487).stroke({ color: token.id === props.selected ? 0xffffff : 0x253440, width: token.id === props.selected ? 3 : 2 });
      const initial = new Text({ text: token.name.slice(0, 1), style: { fontFamily: 'sans-serif', fontSize: cell * 0.35, fill: 0x111a21, fontWeight: 'bold' } }); initial.anchor.set(0.5);
      unit.addChild(marker, initial); unit.eventMode = token.canMove ? 'static' : 'none'; unit.cursor = token.canMove ? 'pointer' : 'default';
      unit.on('pointertap', event => { event.stopPropagation(); if (!current.current.disabled) current.current.onSelect(token.id); }); layer.addChild(unit);
    });
    app.stage.addChild(layer);
    return () => { if (!layer.destroyed) { layer.parent?.removeChild(layer); layer.destroy({ children: true }); } };
  }, [props.scene, props.selected, ready]);
  return <div className="map-surface"><div ref={host} aria-label="场景网格地图" role="img" />{error && <p role="alert">{error}</p>}</div>;
}
