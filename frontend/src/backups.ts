export type BackupJob={id:string;actor:string;expectedVersion:number;kind:'MANUAL'|'AUTOMATIC'|'MIGRATION';state:'RUNNING'|'COMPLETE'|'FAILED'|'INTERRUPTED'|'EXPIRED';at:string;finishedAt:string;bytes:number};
export type BackupStatus={version:number;retention:number;paused:boolean;jobs:BackupJob[];retentionFailed:boolean};
