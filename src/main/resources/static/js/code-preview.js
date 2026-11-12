/* Product form: live barcode + QR preview of whatever is in the Product ID / Barcode box (typed or scanned). */
(function () {
  const input = document.getElementById('barcodeInput');
  const box = document.getElementById('codePreview');
  const bar = document.getElementById('pvBar');
  const qr = document.getElementById('pvQr');
  if (!input || !box || !bar || !qr || !window.ProductCodes) return;
  let last = null;
  function draw() {
    const v = input.value.trim();
    if (v === last) return;
    last = v;
    if (!v) { box.classList.add('d-none'); return; }
    box.classList.remove('d-none');
    bar.style.display = ProductCodes.barcode(bar, v) ? '' : 'none';
    try { qr.innerHTML = ProductCodes.qrSvg(v); } catch (e) { qr.innerHTML = ''; }
  }
  input.addEventListener('input', draw);
  input.addEventListener('change', draw);
  setInterval(draw, 400);   // also catches values filled in by the camera scanner
  draw();
})();
