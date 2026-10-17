/* Padam TV web app. Plain ES5 so it runs on older Samsung/LG/Sony TV browsers. */
(function () {
  'use strict';

  var IMG = 'https://image.tmdb.org/t/p/';
  var app = document.getElementById('app');
  var backdrop = document.getElementById('backdrop');
  var player = document.getElementById('player');
  var playerFrame = document.getElementById('player-frame');
  var current = null;          // focused element
  var lastFocus = {};          // route -> data-key of the element focused when we left it
  var cache = {};

  // ---- helpers ------------------------------------------------------------

  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function get(url, cb) {
    if (cache[url]) return cb(null, cache[url]);
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url);
    xhr.onload = function () {
      var data = null;
      try { data = JSON.parse(xhr.responseText); } catch (e) { /* handled below */ }
      if (xhr.status !== 200 || !data) return cb(new Error((data && data.error) || 'Request failed (' + xhr.status + ')'));
      cache[url] = data;
      cb(null, data);
    };
    xhr.onerror = function () { cb(new Error('Can\'t reach the Padam server.')); };
    xhr.send();
  }

  function metaLine(m) {
    var parts = [];
    if (m.year) parts.push(m.year);
    if (m.runtime) parts.push(Math.floor(m.runtime / 60) + 'h ' + (m.runtime % 60) + 'm');
    if (m.rating) parts.push('★ ' + m.rating.toFixed(1));
    if (m.genres && m.genres.length) parts.push(m.genres.slice(0, 3).join(', '));
    return parts.join('  ·  ');
  }

  function card(m) {
    return '<div class="card focusable" tabindex="-1" data-key="m' + m.id + '" data-go="#/movie/' + m.id + '"' +
      ' data-movie="' + m.id + '"><span>' + esc(m.title) + '</span>' +
      (m.poster ? '<img loading="lazy" src="' + IMG + 'w342' + m.poster + '" alt="" onerror="this.remove()">' : '') +
      '</div>';
  }

  function setBackdrop(path) {
    backdrop.style.backgroundImage = path ? 'url(' + IMG + 'w1280' + path + ')' : 'none';
  }

  function message(text, retry) {
    app.innerHTML = '<div class="message"><p>' + esc(text) + '</p>' +
      (retry ? '<div class="btn primary focusable" tabindex="-1" data-action="retry">Retry</div>' : '') + '</div>';
    focusFirst();
  }

  // ---- screens ------------------------------------------------------------

  var moviesById = {};

  function renderHome() {
    message('Loading…');
    get('/api/home', function (err, data) {
      if (err) return message(err.message, true);
      var html = '<div class="home"><div class="topbar"><div class="logo">Padam</div>' +
        '<div class="btn focusable" tabindex="-1" data-key="search" data-go="#/search">⌕ Search</div></div>' +
        '<div class="hero" id="hero"></div><div class="rows" id="screen">';
      data.rows.forEach(function (row, r) {
        html += '<div class="row"><h2>' + esc(row.title) + '</h2><div class="strip">';
        row.movies.forEach(function (m) { moviesById[m.id] = m; html += card(m); });
        html += '</div></div>';
      });
      app.innerHTML = html + '</div></div>';
      restoreFocus() || focus(app.querySelector('.card'));
    });
  }

  function showHero(m) {
    var hero = document.getElementById('hero');
    if (!hero || !m) return;
    setBackdrop(m.backdrop);
    hero.innerHTML = '<h1>' + esc(m.title) + '</h1><div class="meta">' + esc(metaLine(m)) + '</div>' +
      '<div class="overview">' + esc(m.overview) + '</div>';
  }

  function renderSearch() {
    setBackdrop(null);
    app.innerHTML = '<div class="screen" id="screen"><div class="logo">Search Tamil films</div>' +
      '<input class="searchbox focusable" id="q" data-key="q" type="search" autocomplete="off" ' +
      'placeholder="Title, e.g. Vikram, Jailer, Nayakan" value="' + esc(lastQuery) + '">' +
      '<div class="note" id="status"></div><div class="grid" id="results"></div></div>';
    var input = document.getElementById('q');
    var timer = null;
    input.oninput = function () {
      clearTimeout(timer);
      timer = setTimeout(function () { runSearch(input.value); }, 350);
    };
    if (lastQuery) runSearch(lastQuery);
    restoreFocus() || focus(input);
  }

  var lastQuery = '';
  function runSearch(q) {
    lastQuery = q;
    var status = document.getElementById('status');
    var results = document.getElementById('results');
    if (!status) return;
    if (!q.trim()) { status.textContent = ''; results.innerHTML = ''; return; }
    status.textContent = 'Searching…';
    get('/api/search?q=' + encodeURIComponent(q.trim()), function (err, data) {
      if (q !== lastQuery || !document.getElementById('results')) return;
      if (err) { status.textContent = err.message; return; }
      status.textContent = data.results.length ? '' : 'No Tamil films match "' + q + '".';
      results.innerHTML = data.results.map(card).join('');
      if (current && current.getAttribute('data-movie')) restoreFocus();
    });
  }

  function renderMovie(id) {
    message('Loading…');
    get('/api/movie/' + id, function (err, m) {
      if (err) return message(err.message, true);
      setBackdrop(m.backdrop);
      var html = '<div class="screen" id="screen"><div class="details">' +
        (m.poster ? '<img class="poster" src="' + IMG + 'w342' + m.poster + '" alt="">' : '<div class="poster"></div>') +
        '<div class="info"><h1>' + esc(m.title) + '</h1>' +
        (m.originalTitle ? '<div class="subtitle">' + esc(m.originalTitle) + '</div>' : '') +
        '<div class="meta">' + esc(metaLine(m)) + '</div>' +
        (m.tagline ? '<div class="tagline">' + esc(m.tagline) + '</div>' : '') +
        '<div class="overview">' + esc(m.overview) + '</div>' +
        (m.director || m.music ? '<div class="people">' + esc([m.director && 'Director: ' + m.director,
          m.music && 'Music: ' + m.music].filter(Boolean).join('     ')) + '</div>' : '') +
        (m.cast.length ? '<div class="people">Cast: ' + esc(m.cast.join(', ')) + '</div>' : '') +
        '<div class="actions">';
      if (m.videos.length) {
        html += '<div class="btn primary focusable" tabindex="-1" data-key="play" data-play="' + esc(m.videos[0].key) + '">▶ Play trailer</div>';
      }
      html += '</div>';
      if (m.streaming.length) {
        html += '<div class="note">Streaming on ' + esc(m.streaming.map(function (p) { return p.name; }).join(', ')) +
          '. Open that app on your TV to watch.</div>';
      } else if (m.rentBuy.length) {
        html += '<div class="note">Rent or buy on ' + esc(m.rentBuy.map(function (p) { return p.name; }).join(', ')) + '.</div>';
      } else {
        html += '<div class="note">Not streaming in your region yet.</div>';
      }
      html += '</div></div>';
      if (m.videos.length > 1) {
        html += '<div class="row" style="margin-top:2vw"><h2>Trailers, songs and clips</h2><div class="strip">';
        m.videos.slice(1).forEach(function (v, i) {
          html += '<div class="btn focusable" tabindex="-1" data-key="v' + i + '" data-play="' + esc(v.key) + '">▶ ' + esc(v.name) + '</div>';
        });
        html += '</div></div>';
      }
      if (m.more.length) {
        html += '<div class="row" style="margin-top:2vw"><h2>More like this</h2><div class="strip">' +
          m.more.map(card).join('') + '</div></div>';
      }
      app.innerHTML = html + '</div>';
      restoreFocus() || focusFirst();
    });
  }

  // ---- player -------------------------------------------------------------

  function play(key) {
    playerFrame.src = 'https://www.youtube-nocookie.com/embed/' + encodeURIComponent(key) + '?autoplay=1&rel=0&modestbranding=1';
    player.hidden = false;
    // A history entry means the TV browser's own Back button closes the player.
    history.pushState({ player: true }, '', location.href);
    playerFrame.focus();
  }

  function closePlayer() {
    playerFrame.src = 'about:blank';
    player.hidden = true;
    if (current) current.focus();
  }

  // ---- focus & spatial navigation ------------------------------------------

  function focus(el) {
    if (!el) return false;
    if (current) current.className = current.className.replace(/\s*focused/g, '');
    current = el;
    el.className += ' focused';
    el.focus();
    reveal(el);
    var id = el.getAttribute('data-movie');
    if (id && moviesById[id] && document.getElementById('hero')) showHero(moviesById[id]);
    lastFocus[location.hash || '#/'] = el.getAttribute('data-key');
    return true;
  }

  function focusFirst() { return focus(app.querySelector('.focusable')); }

  function restoreFocus() {
    var key = lastFocus[location.hash || '#/'];
    return key ? focus(app.querySelector('[data-key="' + key + '"]')) : false;
  }

  // Keep the focused element visible inside its horizontal strip and the vertical screen.
  function reveal(el) {
    var strip = el.parentNode;
    if (strip && strip.className.indexOf('strip') >= 0) {
      var s = strip.getBoundingClientRect(), r = el.getBoundingClientRect();
      if (r.right > s.right) strip.scrollLeft += r.right - s.right + r.width * 0.2;
      else if (r.left < s.left) strip.scrollLeft -= s.left - r.left + r.width * 0.2;
    }
    var screen = document.getElementById('screen');
    if (screen) {
      var box = el.parentNode.className.indexOf('strip') >= 0 ? el.parentNode.parentNode : el;
      var sr = screen.getBoundingClientRect(), br = box.getBoundingClientRect();
      var margin = sr.height * 0.08;
      if (br.bottom > sr.bottom) screen.scrollTop += br.bottom - sr.bottom + margin;
      else if (br.top < sr.top + margin) screen.scrollTop -= sr.top + margin - br.top;
    }
  }

  function move(dir) {
    if (!current || !document.body.contains(current)) return focusFirst();
    var from = current.getBoundingClientRect();
    var fx = from.left + from.width / 2, fy = from.top + from.height / 2;
    var best = null, bestScore = Infinity;
    var all = app.querySelectorAll('.focusable');
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (el === current) continue;
      var r = el.getBoundingClientRect();
      if (!r.width) continue;
      var x = r.left + r.width / 2, y = r.top + r.height / 2;
      var dx = x - fx, dy = y - fy, main, cross;
      if (dir === 'left') { if (r.right > from.left + 1) continue; main = -dx; cross = Math.abs(dy); }
      if (dir === 'right') { if (r.left < from.right - 1) continue; main = dx; cross = Math.abs(dy); }
      if (dir === 'up') { if (r.bottom > from.top + 1) continue; main = from.top - r.bottom; cross = Math.abs(dx); }
      if (dir === 'down') { if (r.top < from.bottom - 1) continue; main = r.top - from.bottom; cross = Math.abs(dx); }
      // Left/right: stay on the same row. Up/down: go to the nearest row first, then the closest item in it.
      var score = dir === 'left' || dir === 'right' ? main + cross * 10 : main * 5 + cross;
      if (score < bestScore) { bestScore = score; best = el; }
    }
    if (best) focus(best);
  }

  function activate(el) {
    if (!el) return;
    if (el.getAttribute('data-play')) return play(el.getAttribute('data-play'));
    if (el.getAttribute('data-go')) { location.hash = el.getAttribute('data-go'); return; }
    if (el.getAttribute('data-action') === 'retry') { cache = {}; route(); }
  }

  function back() {
    if (!player.hidden) return history.back();
    if (location.hash && location.hash !== '#/') history.back();
  }

  var KEYS = { 37: 'left', 38: 'up', 39: 'right', 40: 'down' };

  document.addEventListener('keydown', function (e) {
    var k = e.keyCode;
    var typing = document.activeElement && document.activeElement.tagName === 'INPUT';
    if (k === 27 || k === 461 || k === 10009 || (k === 8 && !typing)) { e.preventDefault(); return back(); }
    if (!player.hidden) return;
    if (KEYS[k]) {
      if (typing && (k === 37 || k === 39)) return; // move the text cursor
      e.preventDefault();
      if (typing) document.activeElement.blur();
      return move(KEYS[k]);
    }
    if (k === 13 && !typing) { e.preventDefault(); activate(current); }
  });

  // Pointer/magic-remote support.
  app.addEventListener('click', function (e) {
    var el = e.target;
    while (el && el !== app && !(el.className && el.className.indexOf && el.className.indexOf('focusable') >= 0)) el = el.parentNode;
    if (el && el !== app) { focus(el); activate(el); }
  });
  app.addEventListener('mouseover', function (e) {
    var el = e.target;
    while (el && el !== app && !(el.className && el.className.indexOf && el.className.indexOf('focusable') >= 0)) el = el.parentNode;
    if (el && el !== app && el !== current) focus(el);
  });

  // ---- routing ------------------------------------------------------------

  function route() {
    current = null;
    var h = location.hash || '#/';
    var m = h.match(/^#\/movie\/(\d+)/);
    if (m) return renderMovie(m[1]);
    if (h === '#/search') return renderSearch();
    renderHome();
  }

  window.addEventListener('popstate', function () { if (!player.hidden) closePlayer(); });
  window.addEventListener('hashchange', function () { if (!player.hidden) closePlayer(); route(); });
  route();
})();
