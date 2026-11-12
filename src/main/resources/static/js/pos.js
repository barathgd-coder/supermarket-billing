(function () {
  const $ = id => document.getElementById(id);
  const cur = window.POS_CURRENCY || '';
  const round2 = x => Math.round((x + Number.EPSILON) * 100) / 100;
  const fmt = x => round2(x).toFixed(2);

  let cart = [];          // {id, barcode, name, price, gst, stock, qty}
  let results = [], active = -1, timer = null, busy = false, justAdded = null;

  const search = $('search'), box = $('results');

  function esc(s) { const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }

  // ---------------------------------------------------------------- search
  async function lookup(q) {
    const r = await fetch('/billing/api/products?q=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' } });
    if (!r.ok) return [];
    return r.json();
  }

  function renderResults() {
    if (!results.length) { box.classList.add('d-none'); box.innerHTML = ''; return; }
    box.innerHTML = results.map((p, i) =>
      `<button type="button" class="list-group-item list-group-item-action d-flex justify-content-between ${i === active ? 'active-row' : ''}" data-i="${i}">
         <span><strong>${esc(p.name)}</strong><br><small class="opacity-75">${esc(p.barcode)} - ${esc(p.category)}</small></span>
         <span class="text-end">${cur}${fmt(p.price)}<br><small class="${p.stock <= 0 ? 'text-danger' : 'opacity-75'}">Stock: ${p.stock}</small></span>
       </button>`).join('');
    box.classList.remove('d-none');
  }

  search.addEventListener('input', () => {
    clearTimeout(timer);
    const q = search.value.trim();
    if (!q) { results = []; active = -1; renderResults(); return; }
    timer = setTimeout(async () => { results = await lookup(q); active = results.length ? 0 : -1; renderResults(); }, 120);
  });

  search.addEventListener('keydown', async e => {
    if (e.key === 'ArrowDown' && results.length) { e.preventDefault(); active = (active + 1) % results.length; renderResults(); }
    else if (e.key === 'ArrowUp' && results.length) { e.preventDefault(); active = (active - 1 + results.length) % results.length; renderResults(); }
    else if (e.key === 'Enter') {
      e.preventDefault();
      const q = search.value.trim();
      if (!q) return;
      clearTimeout(timer);
      // barcode scanners type quickly and press Enter: look up fresh and prefer an exact barcode match
      const fresh = await lookup(q);
      const exact = fresh.find(p => p.barcode.toLowerCase() === q.toLowerCase());
      const pick = exact || (results.length && active >= 0 && search.value.trim() === q && results[active] && fresh.some(f => f.id === results[active].id)
        ? results[active] : fresh[0]);
      if (pick) addToCart(pick); else showToast('No product found for "' + q + '"', 'error');
      search.value = ''; results = []; active = -1; renderResults();
    }
  });

  box.addEventListener('mousedown', e => {
    const btn = e.target.closest('button[data-i]');
    if (!btn) return;
    e.preventDefault();
    addToCart(results[+btn.dataset.i]);
    search.value = ''; results = []; active = -1; renderResults(); search.focus();
  });

  // ---------------------------------------------------------------- scan feedback + camera
  const lastScan = $('lastScan');
  function flash(text, err) {
    lastScan.innerHTML = '<i class="bi ' + (err ? 'bi-exclamation-circle' : 'bi-check-circle-fill') + '"></i> ' + esc(text);
    lastScan.classList.toggle('err', !!err);
    lastScan.classList.add('show');
    clearTimeout(flash.t); flash.t = setTimeout(() => lastScan.classList.remove('show'), 4000);
  }

  // Look a scanned code up and add it. Exact barcode / QR text match only, so a camera misread never adds a wrong item.
  async function addScanned(code) {
    const found = await lookup(code);
    const exact = found.find(p => p.barcode.toLowerCase() === code.toLowerCase());
    if (exact) addToCart(exact);
    else flash('No product with code ' + code, true);
  }

  // The camera lives in a panel on the page, so the bill stays visible while scanning (works with Iriun Webcam).
  const dockEl = $('scanDock'), camBtn = $('camBtn');
  let dock = null;
  function setDock(on) {
    dockEl.classList.toggle('open', on);
    camBtn.setAttribute('aria-pressed', on ? 'true' : 'false');
    if (!dock) dock = Scanner.dock(dockEl.querySelector('.scan-panel'), { continuous: true, onScan: addScanned });
    if (on) dock.start(); else { dock.stop(); search.focus(); }
  }
  function openCamera() { setDock(!dockEl.classList.contains('open')); }
  camBtn.addEventListener('click', openCamera);
  $('dockClose').addEventListener('click', () => setDock(false));

  // ---------------------------------------------------------------- cart
  function addToCart(p) {
    if (p.stock <= 0) { showToast(p.name + ' is out of stock', 'error'); flash(p.name + ' is out of stock', true); return; }
    const line = cart.find(l => l.id === p.id);
    if (line) {
      if (line.qty + 1 > p.stock) { showToast('Only ' + p.stock + ' of ' + p.name + ' in stock', 'error'); flash('Only ' + p.stock + ' of ' + p.name + ' in stock', true); return; }
      line.qty++;
    } else {
      cart.push({ id: p.id, barcode: p.barcode, name: p.name, price: +p.price, gst: +p.gstPercent, stock: p.stock, qty: 1 });
    }
    flash('Added ' + p.name + ' (' + (cart.find(l => l.id === p.id).qty) + ')');
    justAdded = p.id;
    render();
    justAdded = null;
  }

  function setQty(i, q) {
    const l = cart[i];
    q = Math.floor(+q) || 1;
    if (q > l.stock) { showToast('Only ' + l.stock + ' of ' + l.name + ' in stock', 'error'); q = l.stock; }
    if (q < 1) q = 1;
    l.qty = q;
    render();
  }

  $('cartBody').addEventListener('click', e => {
    const b = e.target.closest('button[data-act]');
    if (!b) return;
    const i = +b.dataset.i;
    if (b.dataset.act === 'inc') setQty(i, cart[i].qty + 1);
    if (b.dataset.act === 'dec') setQty(i, cart[i].qty - 1);
    if (b.dataset.act === 'del') { cart.splice(i, 1); render(); }
  });
  $('cartBody').addEventListener('change', e => {
    if (e.target.matches('input[data-qty]')) setQty(+e.target.dataset.qty, e.target.value);
  });

  // totals: same algorithm as BillingCalculator on the server (the server result is authoritative)
  function compute() {
    const lines = cart.map(l => ({ gross: round2(l.price * l.qty), gst: l.gst }));
    const sub = lines.reduce((a, l) => a + l.gross, 0);
    const type = $('discType').value;
    const val = Math.max(+$('discValue').value || 0, 0);
    let disc = type === 'PERCENT' ? round2(sub * Math.min(val, 100) / 100) : type === 'AMOUNT' ? round2(val) : 0;
    disc = Math.min(disc, sub);
    let allocated = 0, tax = 0, total = 0, discSum = 0;
    lines.forEach((l, i) => {
      let share = (sub === 0 || disc === 0) ? 0 : (i === lines.length - 1 ? round2(disc - allocated) : round2(disc * l.gross / sub));
      share = Math.min(Math.max(share, 0), l.gross);
      allocated += share; discSum += share;
      l.taxable = round2(l.gross - share);
      l.tax = round2(l.taxable * l.gst / 100);
      l.total = round2(l.taxable + l.tax);
      tax += l.tax; total += l.total;
    });
    return { lines, sub: round2(sub), disc: round2(discSum), tax: round2(tax), total: round2(total) };
  }

  function render() {
    const c = compute();
    $('cartBody').innerHTML = cart.map((l, i) => `
      <tr class="cart-row ${l.id === justAdded ? 'just-added' : ''}">
        <td><div class="nm">${esc(l.name)}</div><div class="bc">${esc(l.barcode)}</div></td>
        <td class="text-end tnum">${fmt(l.price)}</td>
        <td class="text-center text-nowrap">
          <div class="stepper">
            <button type="button" data-act="dec" data-i="${i}" tabindex="-1" aria-label="Decrease quantity">&minus;</button>
            <input type="number" class="qty-input" min="1" max="${l.stock}" value="${l.qty}" data-qty="${i}" aria-label="Quantity">
            <button type="button" data-act="inc" data-i="${i}" tabindex="-1" aria-label="Increase quantity">+</button>
          </div>
        </td>
        <td class="text-end text-muted">${l.gst}%</td>
        <td class="text-end tnum fw-semibold">${fmt(c.lines[i].gross)}</td>
        <td class="text-end"><button class="btn btn-sm btn-light" data-act="del" data-i="${i}" tabindex="-1" aria-label="Remove ${esc(l.name)}"><i class="bi bi-x-lg"></i></button></td>
      </tr>`).join('');
    const units = cart.reduce((a, l) => a + l.qty, 0);
    $('itemCount').textContent = units + (units === 1 ? ' item' : ' items');
    $('emptyCart').classList.toggle('d-none', cart.length > 0);
    $('tSub').textContent = fmt(c.sub);
    $('tDisc').textContent = '-' + fmt(c.disc);
    $('tTax').textContent = fmt(c.tax);
    $('tTotal').textContent = cur + fmt(c.total);
    $('payBtn').disabled = cart.length === 0 || busy;
    lastTotal = c.total;
    updateChange();
  }

  let lastTotal = 0;
  function updateChange() {
    const box = $('changeBox'), got = parseFloat($('cashIn').value);
    if (selectedMethod() !== 'CASH' || !(got >= 0) || $('cashIn').value === '' || !cart.length) { box.classList.remove('show', 'short'); return; }
    const diff = round2(got - lastTotal);
    box.classList.add('show');
    box.classList.toggle('short', diff < 0);
    $('changeLbl').textContent = diff < 0 ? 'Still to collect' : 'Change to return';
    $('changeAmt').textContent = cur + fmt(Math.abs(diff));
  }
  $('cashIn').addEventListener('input', updateChange);

  $('discType').addEventListener('change', () => {
    $('discValue').disabled = $('discType').value === 'NONE';
    if ($('discType').value === 'NONE') $('discValue').value = 0;
    render();
  });
  $('discValue').addEventListener('input', render);
  document.querySelectorAll('input[name=pay]').forEach(r => r.addEventListener('change', () => {
    $('payRef').classList.toggle('d-none', selectedMethod() === 'CASH');
    $('cashBox').classList.toggle('d-none', selectedMethod() !== 'CASH');
    updateChange();
  }));
  const selectedMethod = () => document.querySelector('input[name=pay]:checked').value;

  $('custPhone').addEventListener('blur', async () => {
    const ph = $('custPhone').value.trim();
    if (ph.length < 7) return;
    const r = await fetch('/billing/api/customers/lookup?phone=' + encodeURIComponent(ph));
    if (r.ok) { const j = await r.json(); if (j.name && !$('custName').value) $('custName').value = j.name; }
  });

  // ---------------------------------------------------------------- checkout
  function showPayError(msg) {
    const el = $('payError');
    el.textContent = msg; el.classList.remove('d-none');
    showToast(msg, 'error');
  }

  async function checkout() {
    if (!cart.length || busy) return;
    $('payError').classList.add('d-none');
    const phone = $('custPhone').value.trim();
    if (phone && !/^[0-9+\- ]{7,15}$/.test(phone)) { showPayError('Enter a valid phone number'); $('custPhone').focus(); return; }
    if ($('discType').value !== 'NONE' && (+$('discValue').value || 0) < 0) { showPayError('Discount cannot be negative'); return; }

    busy = true; render();
    const csrf = window.csrf();
    const body = {
      customerName: $('custName').value.trim(), customerPhone: phone,
      paymentMethod: selectedMethod(), paymentReference: $('payRef').value.trim(),
      discountType: $('discType').value, discountValue: +$('discValue').value || 0,
      items: cart.map(l => ({ productId: l.id, quantity: l.qty }))
    };
    try {
      const r = await fetch('/billing/api/checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', [csrf.header]: csrf.token },
        body: JSON.stringify(body)
      });
      const j = await r.json().catch(() => ({}));
      if (!r.ok) { showPayError(j.message || 'Payment failed'); busy = false; render(); return; }
      cart = [];            // clear the cart after a successful payment...
      render();
      window.location.href = j.invoiceUrl;   // ...then open the printable invoice
    } catch (err) {
      showPayError('Network error - the sale was not completed.');
      busy = false; render();
    }
  }

  $('payBtn').addEventListener('click', checkout);
  function clearCart() { if (cart.length) { cart = []; $('cashIn').value = ''; render(); } search.focus(); }
  $('clearBtn').addEventListener('click', clearCart);

  document.addEventListener('keydown', e => {
    if (e.key === 'F2') { e.preventDefault(); search.focus(); search.select(); }
    else if (e.key === 'F9') { e.preventDefault(); checkout(); }
    else if (e.key === 'F8') { e.preventDefault(); clearCart(); }
    else if (e.key === 'F4') { e.preventDefault(); openCamera(); }
    else if (e.key === 'Escape') { search.value = ''; results = []; renderResults(); search.focus(); }
  });
  search.addEventListener('blur', () => setTimeout(() => { box.classList.add('d-none'); }, 150));
  search.addEventListener('focus', () => { if (results.length) renderResults(); });

  render();
})();
