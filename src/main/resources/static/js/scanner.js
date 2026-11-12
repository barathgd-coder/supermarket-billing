/* Camera barcode / QR scanner - works with the laptop webcam, a phone, or Iriun Webcam.
 *
 * Iriun Webcam turns your phone into a normal webcam on the PC, so the browser sees it as one more camera.
 * This script lists every camera, picks "Iriun Webcam" automatically when it is present, and remembers your choice.
 *
 *   Scanner.open({ continuous, onScan })        pop-up scanner (product forms, search boxes)
 *   Scanner.dock(rootEl, { continuous, onScan }) scanner built into a page (POS counter); returns { start, stop, running }
 *
 * Camera access needs HTTPS or http://localhost. Decoding uses ZXing (bundled in /js/vendor/zxing.min.js). */
(function () {
  'use strict';
  const STORE_KEY = 'scanner.camera';
  const store = {
    get() { try { return localStorage.getItem(STORE_KEY); } catch (e) { return null; } },
    set(v) { try { localStorage.setItem(STORE_KEY, v); } catch (e) { /* optional */ } }
  };
  const isIriun = d => /iriun/i.test(d.label || '');
  let audioCtx = null;

  function beep() {
    try {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      const o = audioCtx.createOscillator(), g = audioCtx.createGain();
      o.type = 'square'; o.frequency.value = 1100; g.gain.value = 0.06;
      o.connect(g); g.connect(audioCtx.destination); o.start(); o.stop(audioCtx.currentTime + 0.09);
    } catch (e) { /* sound is optional */ }
    if (navigator.vibrate) navigator.vibrate(60);
  }

  // ZXing reader is created once and shared by every panel.
  let reader = null;
  function getReader() {
    if (reader) return reader;
    const F = ZXing.BarcodeFormat, hints = new Map();
    hints.set(ZXing.DecodeHintType.POSSIBLE_FORMATS, [F.EAN_13, F.EAN_8, F.UPC_A, F.UPC_E, F.CODE_128, F.CODE_39, F.CODE_93, F.ITF, F.CODABAR, F.QR_CODE, F.DATA_MATRIX]);
    hints.set(ZXing.DecodeHintType.TRY_HARDER, true);
    reader = new ZXing.MultiFormatReader();
    reader.setHints(hints);
    return reader;
  }

  function Engine(root) {
    const q = s => root.querySelector(s);
    const video = q('.sc-video'), msgEl = q('.sc-msg'), select = q('.sc-device'), refreshBtn = q('.sc-refresh'), flashEl = q('.sc-flash');
    const canvas = document.createElement('canvas'), ctx = canvas.getContext('2d', { willReadFrequently: true });
    let opts = { continuous: false, onScan() { } };
    let stream = null, timer = null, running = false, currentId = null, lastCode = '', lastAt = 0, token = 0;

    const say = (t, err) => { msgEl.textContent = t; msgEl.classList.toggle('err', !!err); };

    async function cameras() {
      const all = await navigator.mediaDevices.enumerateDevices();
      return all.filter(d => d.kind === 'videoinput');
    }

    function fillSelect(devs) {
      select.innerHTML = '';
      devs.forEach((d, i) => {
        const o = document.createElement('option');
        o.value = d.deviceId;
        o.textContent = (d.label || 'Camera ' + (i + 1)) + (isIriun(d) ? ' (phone)' : '');
        select.appendChild(o);
      });
      if (currentId) select.value = currentId;
    }

    function choose(devs) {
      const saved = store.get();
      return devs.find(d => d.deviceId === saved) || devs.find(isIriun) || null;
    }

    function stopStream() {
      clearTimeout(timer); timer = null;
      if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null; }
      video.srcObject = null;
    }

    async function begin(deviceId, mine) {
      stopStream();
      const v = { width: { ideal: 1920 }, height: { ideal: 1080 } };
      if (deviceId) v.deviceId = { exact: deviceId }; else v.facingMode = { ideal: 'environment' };
      const s = await navigator.mediaDevices.getUserMedia({ video: v, audio: false });
      if (mine !== token) { s.getTracks().forEach(t => t.stop()); return; }     // closed or switched while starting
      stream = s;
      video.srcObject = s;
      await video.play();
      currentId = s.getVideoTracks()[0].getSettings().deviceId || deviceId || null;
      try { s.getVideoTracks()[0].applyConstraints({ advanced: [{ focusMode: 'continuous' }] }).catch(() => { }); } catch (e) { /* phones only */ }
      running = true;
      loop(mine);
    }

    function loop(mine) {
      if (!running || mine !== token) return;
      const t0 = performance.now();
      if (video.readyState >= 2 && video.videoWidth) {
        const scale = Math.min(1, 1280 / video.videoWidth);
        const w = Math.round(video.videoWidth * scale), h = Math.round(video.videoHeight * scale);
        if (canvas.width !== w) { canvas.width = w; canvas.height = h; }
        ctx.drawImage(video, 0, 0, w, h);
        try {
          const bmp = new ZXing.BinaryBitmap(new ZXing.HybridBinarizer(new ZXing.HTMLCanvasElementLuminanceSource(canvas)));
          hit(getReader().decode(bmp).getText());
        } catch (e) { /* no code in this frame */ }
      }
      timer = setTimeout(() => loop(mine), Math.max(30, 110 - (performance.now() - t0)));
    }

    function hit(raw) {
      const code = (raw || '').trim();
      if (!code) return;
      const now = Date.now();
      if (code === lastCode && now - lastAt < 2000) return;   // same item still in front of the camera
      lastCode = code; lastAt = now;
      beep();
      flashEl.classList.remove('on'); void flashEl.offsetWidth; flashEl.classList.add('on');
      opts.onScan(code);
      if (opts.continuous) say('Scanned ' + code + '. Show the next item.');
      else { say('Scanned ' + code); stop(); if (opts.onDone) opts.onDone(); }
    }

    function explain(err) {
      const n = err && err.name;
      if (n === 'NotAllowedError') return 'Camera permission is blocked. Click the camera icon in the address bar, allow access, then press refresh.';
      if (n === 'NotFoundError' || n === 'OverconstrainedError') return 'No camera found. For Iriun: open the Iriun app on your phone, start Iriun Webcam on the PC, then press refresh.';
      if (n === 'NotReadableError') return 'The camera is busy. Close other apps using it (Zoom, Teams, the Camera app) and press refresh.';
      return (err && err.message) || 'Could not start the camera.';
    }

    async function start(o) {
      if (o) opts = Object.assign({ continuous: false, onScan() { } }, o);
      lastCode = ''; lastAt = 0;
      const mine = ++token;
      if (!window.ZXing) { say('Scanner files did not load. Refresh the page.', true); return; }
      if (!window.isSecureContext || !navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        say('The camera works on https:// or http://localhost only. Use a USB scanner or type the code.', true); return;
      }
      say('Starting camera...');
      try {
        let devs = await cameras();
        if (!devs.length || !devs[0].label) {      // names stay hidden until the browser has camera permission
          const probe = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
          probe.getTracks().forEach(t => t.stop());
          devs = await cameras();
        }
        const pick = choose(devs);
        await begin(pick ? pick.deviceId : null, mine);
        if (mine !== token) return;
        devs = await cameras();
        fillSelect(devs);
        const iriunSeen = devs.some(isIriun);
        say(iriunSeen && currentId && devs.find(d => d.deviceId === currentId && isIriun(d))
          ? 'Iriun Webcam is on. Hold the barcode 15-25 cm from the phone and tap the phone screen to focus.'
          : 'Hold the barcode flat, about half the frame wide, and keep it still.' + (iriunSeen ? '' : ' Using Iriun? Open the app on your phone, then press refresh.'));
      } catch (err) {
        running = false;
        say(explain(err), true);
        try { fillSelect(await cameras()); } catch (e) { /* ignore */ }
      }
    }

    function stop() {
      token++; running = false;
      stopStream();
    }

    select.addEventListener('change', async () => {
      store.set(select.value);
      const mine = ++token;
      say('Switching camera...');
      try { await begin(select.value, mine); say('Camera switched.'); } catch (err) { running = false; say(explain(err), true); }
    });
    refreshBtn.addEventListener('click', () => start());
    if (navigator.mediaDevices && navigator.mediaDevices.addEventListener) {
      navigator.mediaDevices.addEventListener('devicechange', async () => {   // Iriun appears here once the phone connects
        if (!root.offsetParent) return;
        try { fillSelect(await cameras()); } catch (e) { /* ignore */ }
        if (!running && root.offsetParent) start();
      });
    }
    return { start, stop, get running() { return running; } };
  }

  // ---- pop-up scanner
  let modal = null, modalEl = null, popEngine = null, popOpts = null;
  function initModal() {
    if (modal) return true;
    modalEl = document.getElementById('scanModal');
    if (!modalEl) return false;
    popEngine = Engine(modalEl.querySelector('.scan-panel'));
    modal = new bootstrap.Modal(modalEl);
    modalEl.addEventListener('shown.bs.modal', () => popEngine.start(popOpts));
    modalEl.addEventListener('hidden.bs.modal', () => popEngine.stop());
    return true;
  }

  window.Scanner = {
    open(o) {
      if (!initModal()) return;
      popOpts = Object.assign({ continuous: false, onScan() { } }, o, { onDone: () => modal.hide() });
      document.getElementById('scanTitle').textContent = popOpts.continuous ? 'Scan products' : 'Scan a code';
      modal.show();
    },
    dock(rootEl, o) {
      const e = Engine(rootEl);
      const opts = Object.assign({ continuous: true, onScan() { } }, o);
      return { start: () => e.start(opts), stop: e.stop, get running() { return e.running; } };
    }
  };
})();
