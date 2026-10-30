import type {StabilizationCard} from './stabilization';
export type RestabilizationDamageReview={expectedVersion:number;originVersion:number;targetVersion:number;attemptId:string;amount:number;critical:boolean;reviewed:boolean};
export type RestabilizationDamageApply=RestabilizationDamageReview&{requestId:string;planHash:string;reason:string};
export type RestabilizationDamagePlan={id:string;version:number;originVersion:number;targetVersion:number;attemptId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;absorbed:number|null;deathSuccesses:number|null;deathFailures:number|null;removed:string[]}};
export type RestabilizationDamageEntry={id:string;version:number;originVersion:number;attemptId:string;actor:string;at:string;reason:string;review:RestabilizationDamageReview;planHash:string;before:StabilizationCard;after:StabilizationCard};
