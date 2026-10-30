import type {StabilizationCard} from './stabilization';
export type StabilizationDamageReview={expectedVersion:number;targetVersion:number;attemptId:string;amount:number;critical:boolean;reviewed:boolean};
export type StabilizationDamageApply=StabilizationDamageReview&{requestId:string;planHash:string;reason:string};
export type StabilizationDamagePlan={id:string;version:number;targetVersion:number;attemptId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;absorbed:number|null;deathSuccesses:number|null;deathFailures:number|null;removed:string[]}};
export type StabilizationDamageEntry={id:string;version:number;attemptId:string;actor:string;at:string;reason:string;review:StabilizationDamageReview;planHash:string;before:StabilizationCard;after:StabilizationCard};
