import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MapCanvas } from './MapCanvas';
const fake = vi.hoisted(() => ({ apps: [] as any[], textures: [] as any[] }));
vi.mock('pixi.js', () => {
  class Container {
    destroyed = false; parent?: Container; children: any[] = []; handlers = new Map<string, Function>();
    position = { x: 0, y: 0, set: (x: number, y: number) => { this.position.x = x; this.position.y = y; } };
    scale = { x: 1, y: 1, set: (v: number) => { this.scale.x = v; this.scale.y = v; } };
    addChild(...items: any[]) { for (const i of items) { this.children.push(i); i.parent = this; } }
    addChildAt(item: any, index: number) { this.children.splice(index, 0, item); item.parent = this; }
    removeChild(item: any) { this.children = this.children.filter(i => i !== item); }
    destroy() { this.destroyed = true; }
    on(name: string, f: Function) { this.handlers.set(name, f); return this; }
    off(name: string) { this.handlers.delete(name); return this; }
    toLocal(p: { x: number; y: number }) { return { x: (p.x - this.position.x) / this.scale.x, y: (p.y - this.position.y) / this.scale.y }; }
    emit(name: string, p: unknown) { this.handlers.get(name)?.(p); }
  }
  class Graphics extends Container { rect() { return this; } fill() { return this; } stroke() { return this; } moveTo() { return this; } lineTo() { return this; } circle() { return this; } clear() { return this; } }
  class Application {
    stage = new Container(); canvas = document.createElement('canvas'); renderer = { resize: vi.fn() }; init = vi.fn().mockResolvedValue(undefined); destroy = vi.fn();
    constructor() { fake.apps.push(this); }
  }
  class Texture { destroy = vi.fn(); constructor(_p: unknown) { fake.textures.push(this); } }
  return { Application, Container, Graphics, Rectangle: class {}, Text: class extends Container { anchor = { set: vi.fn() }; constructor(_p: unknown) { super(); } }, ImageSource: class { constructor(_p: unknown) {} }, Sprite: class extends Container { constructor(_p: unknown) { super(); } }, Texture };
});
const scene = { id: 's', campaignId: 'c', name: '森林', width: 24, height: 16, gridSize: 48, version: 8, tokens: [{ id: 't', name: '游侠', x: 2, y: 3, version: 0, canMove: true }] };
async function mount(extra: object = {}) { const p = { scene, selected: 't', disabled: false, onSelect: vi.fn(), onMove: vi.fn(), ...extra }; const view = render(<MapCanvas {...p} />); await waitFor(() => expect(fake.apps.at(-1).stage.handlers.has('pointerdown')).toBe(true)); return { p, view, app: fake.apps.at(-1) }; }
function point(app: any, x: number, y: number, event = 'pointerup') { app.stage.emit(event, { button: 0, pointerId: 1, global: { x: (x + 0.5) * 48, y: (y + 0.5) * 48 } }); }
function cell(app: any, x: number, y: number) { act(() => { point(app, x, y, 'pointerdown'); point(app, x, y); }); }
afterEach(() => { fake.apps.length = 0; fake.textures.length = 0; vi.unstubAllGlobals(); });
describe('native Pixi interaction lifecycle', () => {
  it.each(['snapshot', 'image'] as const)('keeps a selected drag across an unchanged same-scene %s refresh while clearing old image pixels', async refresh => {
    const { app, p, view } = await mount({ mapUrl: 'blob:old' });
    act(() => { point(app, 2, 3, 'pointerdown'); point(app, 3, 4, 'globalpointermove'); });
    view.rerender(<MapCanvas {...p} scene={{ ...scene, tokens: scene.tokens.map(t => ({ ...t })) }} mapUrl={refresh === 'image' ? undefined : 'blob:old'} />);
    act(() => { point(app, 4, 5, 'globalpointermove'); point(app, 4, 5); });
    expect(p.onMove).toHaveBeenCalledWith(4, 5);
  });
  it('keeps a DM rectangle gesture across a same-scene image refresh', async () => {
    const onRectangle = vi.fn(), { app, p, view } = await mount({ onRectangle, revealed: [], mapUrl: 'blob:old' });
    fireEvent.click(screen.getByRole('button', { name: '矩形揭露' })); act(() => { point(app, 0, 0, 'pointerdown'); });
    view.rerender(<MapCanvas {...p} mapUrl={undefined} />); act(() => { point(app, 0, 0); });
    expect(onRectangle).toHaveBeenCalledWith({ x: 0, y: 0 }, { x: 0, y: 0 }, true);
  });
  it.each(['scene', 'version', 'control', 'selection'] as const)('cancels an old drag when its %s changes during a refresh', async change => {
    const { app, p, view } = await mount(); act(() => { point(app, 2, 3, 'pointerdown'); point(app, 3, 4, 'globalpointermove'); });
    const next = { ...scene, id: change === 'scene' ? 'other' : 's', tokens: scene.tokens.map(t => ({ ...t, ...(change === 'version' ? { version: 1 } : {}), ...(change === 'control' ? { canMove: false } : {}) })) };
    view.rerender(<MapCanvas {...p} scene={next} selected={change === 'selection' ? undefined : 't'} />);
    act(() => { point(app, 4, 5, 'globalpointermove'); point(app, 4, 5); }); expect(p.onMove).not.toHaveBeenCalled();
  });
  it('previews a selected drag on snapped cells, keeps authoritative coordinates unchanged, and submits only on release', async () => {
    const { app, p } = await mount(); const layer = app.stage.children[0], unit = layer.children.find((c: any) => c.children?.[0]?.children?.length === 2)?.children[0];
    act(() => { point(app, 2, 3, 'pointerdown'); point(app, 4, 5, 'globalpointermove'); });
    expect(unit.position.x).toBe(4.5 * 48); expect(unit.position.y).toBe(5.5 * 48); expect(p.onMove).not.toHaveBeenCalled(); expect(scene.tokens[0]).toMatchObject({ x: 2, y: 3 });
    act(() => { point(app, 4, 5); }); expect(p.onMove).toHaveBeenCalledWith(4, 5); expect(unit.position.x).toBe(2.5 * 48); expect(unit.position.y).toBe(3.5 * 48);
  });
  it('selects controllable units, preserves click movement, and disables all movement while pending', async () => {
    const { app, p, view } = await mount(); cell(app, 2, 3); expect(p.onSelect).toHaveBeenCalledWith('t'); cell(app, 5, 6); expect(p.onMove).toHaveBeenCalledWith(5, 6);
    view.rerender(<MapCanvas {...p} disabled />); cell(app, 6, 7); expect(p.onMove).toHaveBeenCalledOnce();
  });
  it('pans using pointer events and zooms around the cursor, then resets the viewport', async () => {
    const { app } = await mount(); const layer = app.stage.children[0]; fireEvent.click(screen.getByRole('button', { name: '平移地图' }));
    act(() => { point(app, 0, 0, 'pointerdown'); point(app, 1, 2, 'globalpointermove'); point(app, 1, 2); }); expect(layer.position.x).toBe(48); expect(layer.position.y).toBe(96);
    app.canvas.getBoundingClientRect = () => ({ left: 0, top: 0, width: 1152, height: 768 });
    fireEvent.wheel(app.canvas, { clientX: 120, clientY: 240, deltaY: -100 }); expect(screen.getByLabelText('地图缩放')).toHaveTextContent('120%');
    expect((120 - layer.position.x) / layer.scale.x).toBeCloseTo(72); expect((240 - layer.position.y) / layer.scale.y).toBeCloseTo(144);
    fireEvent.click(screen.getByRole('button', { name: '重置视图' })); expect(layer.position.x).toBe(0); expect(layer.scale.x).toBe(1);
  });
  it('the DM rectangle gesture sends an unsaved cell-set edit and remains independent of token visibility', async () => {
    const onRectangle = vi.fn(), { app, p } = await mount({ onRectangle, revealed: [] }); fireEvent.click(screen.getByRole('button', { name: '矩形揭露' }));
    act(() => { point(app, 2, 2, 'pointerdown'); point(app, 0, 0, 'globalpointermove'); point(app, 0, 0); });
    expect(onRectangle).toHaveBeenCalledWith({ x: 2, y: 2 }, { x: 0, y: 0 }, true); expect(p.onMove).not.toHaveBeenCalled(); expect(p.onSelect).not.toHaveBeenCalled();
  });
  it('destroys sources/textures on invalidation and ignores a disposed Image onload', async () => {
    const images: any[] = []; vi.stubGlobal('Image', class { onload?: () => void; onerror?: () => void; src = ''; constructor() { images.push(this); } });
    const { p, view } = await mount({ mapUrl: 'blob:old' }); const late = images[0].onload; act(() => images[0].onload()); expect(fake.textures).toHaveLength(1);
    const first = fake.textures[0]; view.rerender(<MapCanvas {...p} mapUrl={undefined} />); expect(first.destroy).toHaveBeenCalledWith(true); act(() => late()); expect(fake.textures).toHaveLength(1);
    view.rerender(<MapCanvas {...p} mapUrl="blob:new" />); act(() => images[1].onload()); const second = fake.textures[1]; view.unmount(); expect(second.destroy).toHaveBeenCalledWith(true);
  });
  it('reset cancels a half-finished measurement so the next click starts a new pair', async () => {
    const { app } = await mount({ distancePerCell: 5 }); fireEvent.click(screen.getByRole('button', { name: '两点测距' })); cell(app, 1, 1);
    expect(screen.getByLabelText('测距结果')).toHaveTextContent('起点 (1, 1)'); fireEvent.click(screen.getByRole('button', { name: '重置视图' })); cell(app, 4, 5);
    expect(screen.getByLabelText('测距结果')).toHaveTextContent('起点 (4, 5)');
  });
  it('switching tools cancels an unfinished measurement', async () => {
    const { app } = await mount(); fireEvent.click(screen.getByRole('button', { name: '两点测距' })); cell(app, 1, 1);
    fireEvent.click(screen.getByRole('button', { name: '平移地图' })); fireEvent.click(screen.getByRole('button', { name: '两点测距' })); cell(app, 4, 5);
    expect(screen.getByLabelText('测距结果')).toHaveTextContent('起点 (4, 5)');
  });
});
