/* Fills every .label-card on the Print labels page with its barcode and QR code. */
(function () {
  document.querySelectorAll('.label-card').forEach(function (card) {
    const code = card.dataset.code || '';
    if (!code) return;
    const svg = card.querySelector('svg.lc-bar');
    if (svg && !ProductCodes.barcode(svg, code)) svg.remove();
    const qr = card.querySelector('.lc-qr');
    try { if (qr) qr.innerHTML = ProductCodes.qrSvg(code); } catch (e) { if (qr) qr.remove(); }
  });
  const btn = document.getElementById('printLabels');
  if (btn) btn.addEventListener('click', function () { window.print(); });
})();
