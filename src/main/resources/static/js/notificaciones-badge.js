// Badge de notificaciones - obtiene el conteo del modelo o del API
(function() {
  function actualizarBadge() {
    var count = window.notificacionesNoLeidasCount;

    if (count !== undefined && count !== null) {
      mostrarBadge(count);
      return;
    }

    fetch('/api/notificaciones/no-leidas/count')
      .then(function(resp) {
        if (!resp.ok) return;
        return resp.json();
      })
      .then(function(data) {
        if (data && data.count !== undefined) {
          mostrarBadge(data.count);
        }
      })
      .catch(function() {});
  }

  function mostrarBadge(count) {
    var label = count > 99 ? '99+' : count;
    ['notif-badge', 'sidebar-notif-badge'].forEach(function(id) {
      var badge = document.getElementById(id);
      if (!badge) return;
      if (count > 0) {
        badge.textContent = label;
        badge.style.display = 'flex';
      } else {
        badge.style.display = 'none';
      }
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', actualizarBadge);
  } else {
    actualizarBadge();
  }

  setInterval(actualizarBadge, 60000);
})();