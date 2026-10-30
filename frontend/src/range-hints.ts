export interface RangeHintRequest {
  sceneId:string; sceneVersion:number; sourceId:string; sourceVersion:number;
  targetId:string; targetVersion:number; profile:'GRID_5R_5FT_RANGE_V1'; reviewed:boolean;
  mode:'MELEE'|'RANGED'; normalFeet:number; longFeet:number;
}
export interface RangeHint {
  status:'CANDIDATE'|'MANUAL_REQUIRED'; distanceFeet:number|null;
  band:'WITHIN_REACH'|'NORMAL_RANGE'|'LONG_RANGE'|'OUT_OF_RANGE'|null;
  reason:''|'REVIEW_REQUIRED'|'GRID_REQUIRES_MANUAL';
}
