import { describe, expect, it } from 'vitest';
import { distance, rectangle, snapCell, zoomAt } from './map-geometry';
describe('map geometry', () => {
  it('keeps the logical point beneath the cursor fixed while clamping zoom', () => {
    const before = { x: 20, y: -30, scale: 1 }, p = { x: 120, y: 170 }, after = zoomAt(before, p, 2);
    expect((p.x - after.x) / after.scale).toBe(100); expect((p.y - after.y) / after.scale).toBe(200); expect(zoomAt(after, p, 100).scale).toBe(4);
  });
  it('snaps to integer cells and rejects points outside rather than moving into another edge cell', () => {
    expect(snapCell({ x: 143.9, y: 48 }, 48, 4, 3)).toEqual({ x: 2, y: 1 }); expect(snapCell({ x: -1, y: 0 }, 48, 4, 3)).toBeUndefined(); expect(snapCell({ x: 192, y: 0 }, 48, 4, 3)).toBeUndefined();
  });
  it('measures geometric Euclidean distance multiplied by the map calibration', () => { expect(distance({ x: 1, y: 1 }, { x: 4, y: 5 }, 5)).toBe(25); });
  it('reveals and re-covers inclusive reverse-drag rectangles in canonical order without duplicate cells', () => {
    const cells = rectangle([{ x: 1, y: 1 }], { x: 2, y: 2 }, { x: 1, y: 1 }, true); expect(cells).toEqual([{ x: 1, y: 1 }, { x: 2, y: 1 }, { x: 1, y: 2 }, { x: 2, y: 2 }]);
    expect(rectangle(cells, { x: 1, y: 1 }, { x: 1, y: 2 }, false)).toEqual([{ x: 2, y: 1 }, { x: 2, y: 2 }]);
  });
});
