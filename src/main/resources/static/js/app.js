(function () {
  const root = document.documentElement;
  const csrf = () => ({
    token: document.querySelector('meta[name=_csrf]')?.content,
    header: document.querySelector('meta[name=_csrf_header]')?.content
  });
  window.csrf = csrf;

  window.showToast = function (msg, type) {
    const box = document.getElementById('toasts');
    if (!box) return;
    const el = document.createElement('div');
    el.className = 'toast align-items-center text-bg-' + (type === 'error' ? 'danger' : 'success') + ' border-0';
    el.innerHTML = '<div class="d-flex"><div class="toast-body"></div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
    el.querySelector('.toast-body').textContent = msg;
    box.appendChild(el);
    const t = new bootstrap.Toast(el, { delay: 4000 });
    el.addEventListener('hidden.bs.toast', () => el.remove());
    t.show();
  };

  document.addEventListener('DOMContentLoaded', function () {
    const flash = document.getElementById('flash');
    if (flash) {
      if (flash.dataset.success) showToast(flash.dataset.success, 'success');
      if (flash.dataset.error) showToast(flash.dataset.error, 'error');
    }

    const icon = () => {
      const b = document.getElementById('themeToggle');
      if (b) b.innerHTML = root.getAttribute('data-bs-theme') === 'dark' ? '<i class="bi bi-sun"></i>' : '<i class="bi bi-moon-stars"></i>';
    };
    icon();
    document.getElementById('themeToggle')?.addEventListener('click', () => {
      const next = root.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
      root.setAttribute('data-bs-theme', next);
      localStorage.setItem('theme', next);
      icon();
      document.dispatchEvent(new Event('themechange'));
    });
    const sb = document.getElementById('sidebar');
    if (sb) {
      const bd = document.createElement('div');
      bd.className = 'sidebar-backdrop';
      document.body.appendChild(bd);
      const setSb = on => { sb.classList.toggle('show', on); bd.classList.toggle('show', on); };
      document.getElementById('sidebarToggle')?.addEventListener('click', () => setSb(!sb.classList.contains('show')));
      bd.addEventListener('click', () => setSb(false));
    }

    // scan-to-fill: <button data-scan-target="#barcodeInput"> fills the field from the camera
    document.querySelectorAll('[data-scan-target]').forEach(btn => btn.addEventListener('click', () => {
      const t = document.querySelector(btn.dataset.scanTarget);
      Scanner.open({ continuous: false, onScan: code => { t.value = code; t.dispatchEvent(new Event('input', { bubbles: true })); t.focus(); } });
    }));

    // client-side validation
    document.querySelectorAll('form.needs-validation').forEach(f => {
      f.addEventListener('submit', e => {
        if (!f.checkValidity()) { e.preventDefault(); e.stopPropagation(); }
        f.classList.add('was-validated');
      });
    });

    // confirmation dialogs: <form data-confirm="Delete this?">
    const modalEl = document.getElementById('confirmModal');
    if (modalEl) {
      const modal = new bootstrap.Modal(modalEl);
      let pending = null;
      document.querySelectorAll('form[data-confirm]').forEach(f => {
        f.addEventListener('submit', e => {
          if (f.dataset.confirmed) return;
          e.preventDefault();
          pending = f;
          document.getElementById('confirmText').textContent = f.dataset.confirm;
          modal.show();
        });
      });
      document.getElementById('confirmOk').addEventListener('click', () => {
        if (pending) { pending.dataset.confirmed = '1'; modal.hide(); pending.submit(); }
      });
    }
  });
})();
