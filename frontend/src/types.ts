export interface Identity { username: string }
export interface Campaign { id: string; name: string; role: 'DM' | 'PLAYER' }
export interface Token { id: string; name: string; x: number; y: number; version: number; canMove: boolean }
export interface Scene { id: string; campaignId: string; name: string; width: number; height: number; gridSize: number; version: number; tokens: Token[] }
export interface Move { requestId: string; expectedVersion: number; x: number; y: number }
export interface Csrf { token: string; headerName: string }
export interface VersionedCommand { requestId: string; expectedVersion: number }
export interface Member { username: string; role: Campaign['role'] }
export interface Members { campaignVersion: number; members: Member[] }
export interface Invitation { id: string; createdBy: string; expiresAt: string; status: 'ACTIVE' | 'EXPIRED' | 'REVOKED' | 'USED'; usedBy: string | null }
export interface Invitations { campaignVersion: number; invitations: Invitation[] }
export interface CreatedInvitation { id: string; token: string | null; expiresAt: string; campaignVersion: number }
export interface InvitePreview { campaignId: string; campaignName: string; expiresAt: string }
