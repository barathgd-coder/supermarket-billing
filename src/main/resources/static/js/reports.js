(function () {
  const $ = id => document.getElementById(id);
  let chart = null;
  const palette = ['#1f6feb', '#16a34a', '#f59e0b', '#8b5cf6', '#ef4444', '#14b8a6', '#ec4899', '#64748b', '#84cc16', '#0ea5e9'];
  const params = () => new URLSearchParams({ type: $('type').value, from: $('from').value, to: $('to').value }).toString();

  async function load() {
    $('csvBtn').href = '/reports/export?' + params();
    const r = await fetch('/reports/data?' + params());
    if (!r.ok) { showToast('Could not load the report', 'error'); return; }
    const d = await r.json();
    $('repTitle').textContent = d.title;
    $('repHead').innerHTML = '<tr>' + d.headers.map((h, i) => `<th class="${i ? 'text-end' : ''}">${h}</th>`).join('') + '</tr>';
    $('repBody').innerHTML = d.rows.length
      ? d.rows.map(row => '<tr>' + row.map((c, i) => `<td class="${i ? 'text-end' : ''}"></td>`).join('') + '</tr>').join('')
      : `<tr><td colspan="${d.headers.length}" class="text-center text-muted py-4">No data for this period</td></tr>`;
    // fill cells via textContent (no HTML injection from product names)
    $('repBody').querySelectorAll('tr').forEach((tr, ri) => { if (d.rows[ri]) tr.querySelectorAll('td').forEach((td, ci) => td.textContent = d.rows[ri][ci]); });

    const labels = d.rows.map(r => r[d.labelColumn]).slice(0, 15);
    const values = d.rows.map(r => parseFloat(r[d.valueColumn])).slice(0, 15);
    if (chart) chart.destroy();
    const pie = d.chartType === 'pie';
    chart = new Chart($('repChart'), {
      type: d.chartType,
      data: { labels, datasets: [{ label: d.headers[d.valueColumn], data: values, borderColor: '#1f6feb', tension: .3,
        backgroundColor: pie ? palette : (d.chartType === 'line' ? 'rgba(31,111,235,.15)' : '#1f6feb'), fill: d.chartType === 'line' }] },
      options: { maintainAspectRatio: false, plugins: { legend: { display: pie } }, scales: pie ? {} : { y: { beginAtZero: true } } }
    });
  }
  $('repForm').addEventListener('submit', e => { e.preventDefault(); load(); });
  $('type').addEventListener('change', load);
  load();
})();
