export interface Identity { username: string }
export interface Campaign { id: string; name: string; role: 'DM' | 'PLAYER' }
export interface Token { id: string; name: string; x: number; y: number; version: number; canMove: boolean }
export interface Scene { id: string; campaignId: string; name: string; width: number; height: number; gridSize: number; version: number; tokens: Token[] }
export interface Move { requestId: string; expectedVersion: number; x: number; y: number }
export interface Csrf { token: string; headerName: string }
