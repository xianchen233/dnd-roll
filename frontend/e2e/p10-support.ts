import { randomUUID } from 'node:crypto';
import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { emptySheet } from '../src/characters';

export function required(name: string) { const value = process.env[name]; if (!value) throw Error(`Missing ${name}`); return value; }
export async function read(request: APIRequestContext, path: string) { const response = await request.get(path); expect(response.status(), path).toBe(200); return response.json(); }
export async function post(request: APIRequestContext, path: string, data: unknown, status = 200) {
  const csrf = await read(request, '/api/auth/csrf');
  const response = await request.post(path, { headers: { [csrf.headerName]: csrf.token }, data });
  expect(response.status(), `${path}: ${await response.text()}`).toBe(status);
  return status === 204 ? undefined : response.json();
}
export async function login(request: APIRequestContext, role: 'DM' | 'PLAYER') { await post(request, '/api/auth/login', { username: required(`E2E_${role}_USERNAME`), password: required(`E2E_${role}_PASSWORD`) }); }
export async function join(dm: APIRequestContext, player: APIRequestContext, root: string) {
  const members = await read(dm, `${root}/members`);
  const invite = await post(dm, `${root}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion }, 201);
  await post(player, '/api/invitations/accept', { token: invite.token });
}
export async function createCharacter(request: APIRequestContext, root: string, name: string) {
  const sheet = emptySheet(name); sheet.combat.hp = 12; sheet.combat.maxHp = 12;
  sheet.resources = [{ id: 'focus', name: '专注点', kind: 'CUSTOM', current: 2, max: 2, recovery: '手动恢复', source: { book: '测试团自制', revision: '1', rules: 'CUSTOM', detail: '仅验证存储' } }];
  sheet.attacks = [{ id: 'sword', name: '练习剑', ability: 'STR', proficient: false, hitBonus: 0, damageBonus: 0, dice: '1d6', damageType: '手动', mastery: '', source: { book: '测试团自制', revision: '1', rules: 'CUSTOM', detail: '不推断效果' } }];
  const list = await read(request, `${root}/characters`);
  return (await post(request, `${root}/characters`, { requestId: randomUUID(), expectedVersion: list.campaignVersion, sheet })).id as string;
}
export async function enter(page: Page, campaignId: string) {
  await page.goto('/'); await expect(page.getByRole('combobox', { name: '选择战役' })).not.toHaveValue('');
  await page.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId);
  await expect(page.locator('.connection')).toHaveText('● 实时连接');
}
export async function click(page: Page, name: string) { const button = page.getByRole('button', { name, exact: true }); await expect(button).toBeEnabled(); await button.click(); }
export async function noOverflow(page: Page) { expect(await page.evaluate(() => ({ width: innerWidth, document: document.documentElement.scrollWidth }))).toEqual({ width: page.viewportSize()!.width, document: page.viewportSize()!.width }); }
