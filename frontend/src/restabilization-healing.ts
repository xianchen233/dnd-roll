import type {StabilizationCard} from './stabilization';
export type RestabilizationHealingReview={expectedVersion:number;originVersion:number;targetVersion:number;attemptId:string;amount:number;reviewed:boolean};
export type RestabilizationHealingApply=RestabilizationHealingReview&{requestId:string;planHash:string;reason:string};
export type RestabilizationHealingPlan={id:string;version:number;originVersion:number;targetVersion:number;attemptId:string;hash:string;effect:{profile:string;status:'CANDIDATE'|'MANUAL_REQUIRED';reasons:string[];hp:number|null;tempHp:number|null;healed:number|null;deathSuccesses:number|null;deathFailures:number|null;removed:string[]}};
export type RestabilizationHealingEntry={id:string;version:number;originVersion:number;attemptId:string;actor:string;at:string;reason:string;review:RestabilizationHealingReview;planHash:string;before:StabilizationCard;after:StabilizationCard};
