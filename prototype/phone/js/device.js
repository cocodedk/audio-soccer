// The phone's extras, each optional: fullscreen, a portrait lock, a screen wake lock and vibration.
// Every call reports 'ok', 'refused' or 'unsupported' and never throws, so the test carries on without it.
const outcome = (e) => (e && e.name === 'NotSupportedError' ? 'unsupported' : 'refused');

// Call straight from the Start click, before any await: fullscreen needs the user's gesture.
export function enterFullscreen() {
  const el = document.documentElement;
  if (!el.requestFullscreen) return Promise.resolve('unsupported');
  try {
    return el.requestFullscreen({ navigationUI: 'hide' }).then(() => 'ok', outcome);
  } catch (e) { return Promise.resolve(outcome(e)); }
}

// Android allows the lock only in fullscreen.
export async function lockPortrait() {
  const o = typeof screen !== 'undefined' ? screen.orientation : null;
  if (!o || !o.lock) return 'unsupported';
  try { await o.lock('portrait'); return 'ok'; } catch (e) { return outcome(e); }
}

let wake = null;
export async function keepAwake() {
  if (!navigator.wakeLock) return 'unsupported';
  try { wake = await navigator.wakeLock.request('screen'); return 'ok'; } catch (e) { return outcome(e); }
}

export function leaveFullscreen() {
  try { screen.orientation.unlock(); } catch (e) { /* not locked */ }
  if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
  try { wake?.release(); } catch (e) { /* already released */ }
  wake = null;
}

export const canVibrate = () => typeof navigator.vibrate === 'function';

export function vibrate(pattern) {
  try { navigator.vibrate(pattern); } catch (e) { /* no vibration */ }
}
