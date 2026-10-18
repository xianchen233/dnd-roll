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
export interface SceneSummary { id: string; name: string }
export interface Scenes { campaignVersion: number; currentSceneId: string; scenes: SceneSummary[] }
export interface SceneReceipt extends SceneSummary { campaignVersion: number }
export interface Asset { id: string; mediaType: string; byteSize: number; width: number; height: number }
export interface Assets { campaignVersion: number; assets: Asset[] }
export interface MapConfiguration { assetId: string; gridSize: number; offsetX: number; offsetY: number; distancePerCell: number }
export interface SceneMap { sceneId: string; sceneVersion: number; map: MapConfiguration | null }
export interface AuthoredToken { id: string; name: string; x: number; y: number; version: number; hidden: boolean; controllers: string[] }
export interface AuthoredTokens { sceneId: string; sceneVersion: number; tokens: AuthoredToken[] }
export interface TokenCommand extends VersionedCommand { name: string; x: number; y: number; hidden: boolean; controllers: string[] }
export interface TokenReceipt { sceneId: string; sceneVersion: number; token: AuthoredToken }
