import type {StabilizationAttempt} from './stabilization';
export type RestabilizationAttempt=Omit<StabilizationAttempt,'status'>&{status:'ROLLED'|'APPLIED'|'HEALED'|'UNSTABLE';originVersion:number};
export type RestabilizationState={id:string;version:number;latest:RestabilizationAttempt|null};
export type RestabilizationReview={expectedVersion:number;originVersion:number;targetVersion:number;helperId:string;helperVersion:number;rollId:string;reviewed:boolean};
export type RestabilizationRollInput=Omit<RestabilizationReview,'rollId'>&{requestId:string;reason:string};
export type RestabilizationApply=RestabilizationReview&{requestId:string;reason:string;planHash:string};
export type RestabilizationPlan=Omit<import('./stabilization').StabilizationPlan,'effect'>&{originVersion:number;effect:import('./stabilization').StabilizationPlan['effect']};
