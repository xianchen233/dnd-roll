import { Client } from '@stomp/stompjs';
import type { Api } from './api';
export type Connection = 'connecting' | 'online' | 'reconnecting';
export function connectRealtime(api: Api, campaignId: string, refresh: () => void, onState: (state: Connection) => void) {
  let disposed = false;
  const client = new Client({
    brokerURL: `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}/ws`,
    reconnectDelay: 3000, heartbeatIncoming: 10000, heartbeatOutgoing: 10000,
    beforeConnect: async () => {
      try { const csrf = await api.getCsrf(); client.connectHeaders = { [csrf.headerName]: csrf.token }; }
      catch { client.connectHeaders = {}; if (!disposed) onState('reconnecting'); }
    },
    onConnect: () => {
      if (disposed) return;
      onState('online'); refresh();
      client.subscribe('/user/queue/scene-changes', message => {
        try { const event = JSON.parse(message.body); if (event.type === 'SCENE_CHANGED' && event.campaignId === campaignId) refresh(); } catch { /* An invalid hint cannot alter saved state. */ }
      });
    },
    onWebSocketClose: () => { if (!disposed) onState('reconnecting'); },
    onStompError: () => { if (!disposed) onState('reconnecting'); },
  });
  onState('connecting'); client.activate();
  const interval = window.setInterval(refresh, 15000);
  return () => { disposed = true; window.clearInterval(interval); void client.deactivate(); };
}
