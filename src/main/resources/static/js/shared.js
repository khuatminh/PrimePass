// Brand backgrounds for digital product cards.
window.BRANDS = {
  netflix:   { name: 'StreamFlix',   type: 'Streaming',    bg: 'linear-gradient(135deg, #1a0000 0%, #E50914 100%)', accent: '#E50914', glyph: 'SF' },
  disney:    { name: 'Disney+',      type: 'Streaming',    bg: 'linear-gradient(135deg, #001d3d 0%, #0063e5 100%)', accent: '#0063e5', glyph: 'D+' },
  hbo:       { name: 'Max',          type: 'Streaming',    bg: 'linear-gradient(135deg, #1a1a2e 0%, #8a2be2 100%)', accent: '#8a2be2', glyph: 'MAX' },
  spotify:   { name: 'SoundPass',    type: 'Music',        bg: 'linear-gradient(135deg, #0d3a1c 0%, #1ed760 100%)', accent: '#1ed760', glyph: 'SP' },
  apple:     { name: 'Apple Music',  type: 'Music',        bg: 'linear-gradient(135deg, #2a0030 0%, #FA233B 100%)', accent: '#FA233B', glyph: '♪' },
  youtube:   { name: 'YT Premium',   type: 'Streaming',    bg: 'linear-gradient(135deg, #1a0000 0%, #FF0033 100%)', accent: '#FF0033', glyph: '▶' },
  steam:     { name: 'Steam Key',    type: 'Game Key',     bg: 'linear-gradient(135deg, #0a1628 0%, #1b2838 50%, #66c0f4 100%)', accent: '#66c0f4', glyph: 'STM' },
  epic:      { name: 'Epic Key',     type: 'Game Key',     bg: 'linear-gradient(135deg, #111 0%, #2a2a2a 100%)', accent: '#ffffff', glyph: 'EG' },
  chatgpt:   { name: 'ChatGPT Plus', type: 'AI Tool',      bg: 'linear-gradient(135deg, #0a2e2a 0%, #10a37f 100%)', accent: '#10a37f', glyph: '◎' },
  canva:     { name: 'Canva Pro',    type: 'AI Tool',      bg: 'linear-gradient(135deg, #0a1a3a 0%, #00c4cc 50%, #8b3dff 100%)', accent: '#00c4cc', glyph: 'Cv' },
  adobe:     { name: 'Adobe CC',     type: 'AI Tool',      bg: 'linear-gradient(135deg, #1a0000 0%, #FA0F00 100%)', accent: '#FA0F00', glyph: 'Ae' },
  midjourney:{ name: 'Midjourney',   type: 'AI Tool',      bg: 'linear-gradient(135deg, #000 0%, #3a3a3a 100%)', accent: '#ffffff', glyph: 'MJ' },
  nordvpn:   { name: 'NordVPN',      type: 'VPN',          bg: 'linear-gradient(135deg, #0a1a4a 0%, #4687ff 100%)', accent: '#4687ff', glyph: 'VPN' },
  expressvpn:{ name: 'ExpressVPN',   type: 'VPN',          bg: 'linear-gradient(135deg, #2a0000 0%, #DA3940 100%)', accent: '#DA3940', glyph: 'EX' },
  icloud:    { name: 'iCloud 2TB',   type: 'Cloud',        bg: 'linear-gradient(135deg, #0a2a4a 0%, #62c7ff 100%)', accent: '#62c7ff', glyph: '☁' },
  drive:     { name: 'G-Drive 2TB',  type: 'Cloud',        bg: 'linear-gradient(135deg, #0a2a14 0%, #4285F4 50%, #34A853 100%)', accent: '#4285F4', glyph: 'GD' },
  gplay:     { name: 'Play Gift',    type: 'Gift Card',    bg: 'linear-gradient(135deg, #0a1a2a 0%, #34A853 50%, #FBBC04 100%)', accent: '#34A853', glyph: '▷' },
  appstore:  { name: 'App Store',    type: 'Gift Card',    bg: 'linear-gradient(135deg, #1a1a2a 0%, #007AFF 100%)', accent: '#007AFF', glyph: 'A' },
  xbox:      { name: 'Xbox Pass',    type: 'Game Key',     bg: 'linear-gradient(135deg, #0a2a0a 0%, #107C10 100%)', accent: '#107C10', glyph: 'X' },
  playstation:{name: 'PS Plus',      type: 'Game Key',     bg: 'linear-gradient(135deg, #0a0a2a 0%, #006FCD 100%)', accent: '#006FCD', glyph: 'PS' },
};

window.TWEAK_PRESETS = {
  violet: { accent: '#8B7CF6', accent2: '#4FD1C5' },
  blue:   { accent: '#60A5FA', accent2: '#A4A9B4' },
  amber:  { accent: '#F5B544', accent2: '#F06B7E' },
  rose:   { accent: '#F06B7E', accent2: '#4FD1C5' },
  green:  { accent: '#4ADE80', accent2: '#60A5FA' },
};

window.applyTweakPreset = function(name) {
  const p = window.TWEAK_PRESETS[name];
  if (!p) return;
  const rs = document.documentElement.style;
  rs.setProperty('--accent', p.accent);
  rs.setProperty('--accent-2', p.accent2);
  const hex = p.accent.replace('#','');
  const r = parseInt(hex.slice(0,2),16), g = parseInt(hex.slice(2,4),16), b = parseInt(hex.slice(4,6),16);
  rs.setProperty('--accent-soft', `rgba(${r}, ${g}, ${b}, 0.12)`);
  localStorage.setItem('tweak_accent', name);
  document.querySelectorAll('.tweaks-swatch').forEach(el => {
    el.classList.toggle('active', el.dataset.preset === name);
  });
};

window.initTweaks = function() {
  const saved = localStorage.getItem('tweak_accent') || 'violet';
  window.applyTweakPreset(saved);

  const panel = document.createElement('div');
  panel.className = 'tweaks-panel';
  panel.id = 'tweaksPanel';
  panel.innerHTML = `
    <h6>Tweaks — Theme Accent</h6>
    <div class="tweaks-swatches">
      ${Object.entries(window.TWEAK_PRESETS).map(([k,v]) =>
        `<div class="tweaks-swatch" data-preset="${k}" style="background: linear-gradient(135deg, ${v.accent}, ${v.accent2})" title="${k}"></div>`
      ).join('')}
    </div>
    <h6 style="margin-top:10px">Info</h6>
    <p style="font-size:12px;color:var(--text-muted);line-height:1.5">Swap accent palette across the whole site. Stored locally.</p>
  `;
  document.body.appendChild(panel);

  panel.querySelectorAll('.tweaks-swatch').forEach(el => {
    el.addEventListener('click', () => window.applyTweakPreset(el.dataset.preset));
  });
  const initial = panel.querySelector(`[data-preset="${saved}"]`);
  if (initial) initial.classList.add('active');

  window.addEventListener('message', (e) => {
    if (!e.data) return;
    if (e.data.type === '__activate_edit_mode') panel.classList.add('show');
    if (e.data.type === '__deactivate_edit_mode') panel.classList.remove('show');
  });
  window.parent.postMessage({ type: '__edit_mode_available' }, '*');
};
