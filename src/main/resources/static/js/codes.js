/* Barcode + QR rendering for product labels.
 * Needs vendor/JsBarcode.all.min.js and vendor/qrcode.js loaded first.
 * Every product gets BOTH: a barcode (EAN-13 when the code is a valid 13-digit EAN, otherwise Code 128)
 * and a QR code that contains exactly the same text, so either one scans in the POS. */
(function () {
  function isEan13(c) {
    if (!/^\d{13}$/.test(c)) return false;
    let s = 0;
    for (let i = 0; i < 12; i++) s += (+c[i]) * (i % 2 ? 3 : 1);
    return (10 - (s % 10)) % 10 === +c[12];
  }

  /** Draws the barcode into an <svg> element. Returns the format used, or '' if the text cannot be encoded. */
  function barcode(svg, code, opts) {
    const o = Object.assign({ width: 2, height: 70, margin: 24 }, opts || {});
    const format = isEan13(code) ? 'EAN13' : 'CODE128';
    try {
      JsBarcode(svg, code, { format: format, width: o.width, height: o.height, margin: o.margin,
        displayValue: false, background: '#ffffff', lineColor: '#000000' });
      return format;
    } catch (e) {
      while (svg.firstChild) svg.removeChild(svg.firstChild);
      return '';
    }
  }

  /** Returns an SVG string of a QR code (with the 4-module quiet zone) for the given text. */
  function qrSvg(code) {
    const q = qrcode(0, 'M');
    q.addData(code, /^\d+$/.test(code) ? 'Numeric' : 'Byte');
    q.make();
    const n = q.getModuleCount(), m = 4, size = n + 2 * m;
    let d = '';
    for (let r = 0; r < n; r++) for (let c = 0; c < n; c++) if (q.isDark(r, c)) d += 'M' + (c + m) + ' ' + (r + m) + 'h1v1h-1z';
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + size + ' ' + size + '" shape-rendering="crispEdges" role="img" aria-label="QR code">'
      + '<rect width="' + size + '" height="' + size + '" fill="#fff"/><path d="' + d + '" fill="#000"/></svg>';
  }

  window.ProductCodes = { isEan13: isEan13, barcode: barcode, qrSvg: qrSvg };
})();
