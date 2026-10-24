import type {Asset,VersionedCommand} from './types';
export type NoteVisibility='TEAM'|'DM';
export type Note={id:string;version:number;visibility:NoteVisibility;title:string;text:string;attachments:Asset[];actor:string;at:string;editedAt:string};
export type NoteRevision={note:Note;reason:string};
export type NoteListing={campaignVersion:number;notes:Note[];next:string};
export type NoteFilters={q:string;visibility:string};
export type NoteCreate=VersionedCommand&{visibility:NoteVisibility;title:string;text:string;attachments:string[]};
export type NoteEdit=VersionedCommand&{title:string;text:string;attachments:string[];reason:string};
