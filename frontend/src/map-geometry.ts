import type { Cell } from './types';
export interface Viewport { x: number; y: number; scale: number }
export function zoomAt(view: Viewport, point: Cell, factor: number): Viewport {
  const scale = Math.max(0.25, Math.min(4, view.scale * factor)), ratio = scale / view.scale;
  return { x: point.x - (point.x - view.x) * ratio, y: point.y - (point.y - view.y) * ratio, scale };
}
export function snapCell(point: Cell, gridSize: number, width: number, height: number): Cell | undefined {
  const x = Math.floor(point.x / gridSize), y = Math.floor(point.y / gridSize);
  return x >= 0 && y >= 0 && x < width && y < height ? { x, y } : undefined;
}
export function distance(a: Cell, b: Cell, perCell: number) { return Math.hypot(b.x - a.x, b.y - a.y) * perCell; }
export function rectangle(revealed: Cell[], a: Cell, b: Cell, reveal: boolean): Cell[] {
  const cells = new Map(revealed.map(c => [`${c.x},${c.y}`, c]));
  for (let y = Math.min(a.y, b.y); y <= Math.max(a.y, b.y); y++) for (let x = Math.min(a.x, b.x); x <= Math.max(a.x, b.x); x++) {
    if (reveal) cells.set(`${x},${y}`, { x, y }); else cells.delete(`${x},${y}`);
  }
  return [...cells.values()].sort((c, d) => c.y - d.y || c.x - d.x);
}
