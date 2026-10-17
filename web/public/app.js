/* Padam TV web app. Plain ES5 so it runs on older Samsung/LG/Sony TV browsers. */
(function () {
  'use strict';

  var IMG = 'https://image.tmdb.org/t/p/';
  var app = document.getElementById('app');
  var layers = document.querySelectorAll('#backdrop .layer');
  var player = document.getElementById('player');
  var playerFrame = document.getElementById('player-frame');
  var current = null;          // focused element
  var lastFocus = {};          // route -> data-key of the element focused when we left it
  var cache = {};
  var moviesById = {};
  var homeRows = null;
  var lastQuery = '';

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
      if (xhr.status !== 200 || !data) return cb(new Error((data && data.error) || 'The server answered with an error (' + xhr.status + ').'));
      cache[url] = data;
      cb(null, data);
    };
    xhr.onerror = function () { cb(new Error('Padam can\'t reach the computer it runs on. Check that it is still switched on.')); };
    xhr.send();
  }

  function initials(name) {
    return esc(String(name).split(/\s+/).slice(0, 2).map(function (w) { return w.charAt(0); }).join('').toUpperCase());
  }

  // A stable, film-specific colour for posters that have no image.
  function hue(id) { return (id * 47) % 360; }

  function metaHtml(m) {
    var parts = [];
    if (m.year) parts.push(esc(m.year));
    if (m.runtime) parts.push(Math.floor(m.runtime / 60) + 'h ' + (m.runtime % 60) + 'm');
    var html = m.rating ? '<span class="rating">★ ' + m.rating.toFixed(1) + '</span>' : '';
    html += parts.join('<span class="dot">·</span>');
    if (m.genres && m.genres.length) {
      html += '<span class="dot"></span>' + m.genres.slice(0, 3).map(function (g) {
        return '<span class="chip">' + esc(g) + '</span>';
      }).join('');
    }
    return html;
  }

  function card(m) {
    moviesById[m.id] = m;
    return '<div class="card focusable" data-key="m' + m.id + '" data-go="#/movie/' + m.id + '" data-movie="' + m.id + '">' +
      '<div class="fallback" style="background:linear-gradient(160deg,hsl(' + hue(m.id) + ',35%,26%),hsl(' + ((hue(m.id) + 40) % 360) + ',30%,12%))">' +
      '<i></i><b>' + esc(m.title) + '</b><span>' + esc(m.year) + '</span></div>' +
      (m.poster ? '<img loading="lazy" src="' + IMG + 'w342' + m.poster + '" alt="" onerror="this.parentNode.removeChild(this)">' : '') +
      '</div>';
  }

  function videoCard(v, i) {
    return '<div class="vcard focusable" data-key="v' + i + '" data-play="' + esc(v.key) + '">' +
      '<div class="thumb"><img loading="lazy" src="https://i.ytimg.com/vi/' + encodeURIComponent(v.key) + '/mqdefault.jpg" alt="" ' +
      'onerror="this.parentNode.removeChild(this)"><span class="play"></span></div>' +
      '<div class="label">' + esc(v.name) + '</div><div class="kind">' + esc(v.type) + '</div></div>';
  }

  // Cross-fades between two backdrop layers so the image never flashes black.
  var activeLayer = 0, backdropPath = null;
  function setBackdrop(path) {
    if (path === backdropPath) return;
    backdropPath = path;
    var next = layers[1 - activeLayer], prev = layers[activeLayer];
    if (!path) { prev.className = 'layer'; next.className = 'layer'; return; }
    var url = IMG + 'w1280' + path;
    var img = new Image();
    img.onload = function () {
      if (backdropPath !== path) return;
      next.style.backgroundImage = 'url(' + url + ')';
      next.className = 'layer on';
      prev.className = 'layer';
      activeLayer = 1 - activeLayer;
    };
    img.src = url;
  }

  function topbar(active) {
    return '<div class="topbar"><div class="brand"><span class="brand-mark"></span><span class="brand-name">Padam</span></div>' +
      '<div class="nav">' +
      '<div class="pill focusable' + (active === 'home' ? ' active' : '') + '" data-key="nav-home" data-go="#/">Home</div>' +
      '<div class="pill focusable' + (active === 'search' ? ' active' : '') + '" data-key="nav-search" data-go="#/search">Search</div>' +
      '</div><div class="clock" id="clock"></div></div>';
  }

  function tickClock() {
    var el = document.getElementById('clock');
    if (!el) return;
    var d = new Date(), h = d.getHours(), m = d.getMinutes();
    el.textContent = ((h % 12) || 12) + ':' + (m < 10 ? '0' : '') + m + (h < 12 ? ' AM' : ' PM');
  }
  setInterval(tickClock, 15000);

  function mount(html) {
    app.innerHTML = '<div class="fade" style="height:100%">' + html + '</div>';
    tickClock();
  }

  function message(title, text, retry) {
    mount('<div class="message"><div class="badge">!</div><h2>' + esc(title) + '</h2><p>' + esc(text) + '</p>' +
      (retry ? '<div class="btn primary focusable" data-action="retry">Try again</div>' : '') + '</div>');
    focusFirst();
  }

  function skeletonRows() {
    var html = '';
    for (var r = 0; r < 2; r++) {
      html += '<div class="row"><div class="skel skel-line" style="width:12vw"></div><div class="strip">';
      for (var c = 0; c < 8; c++) html += '<div class="card skel"></div>';
      html += '</div></div>';
    }
    return html;
  }

  // ---- screens ------------------------------------------------------------

  function renderHome() {
    mount('<div class="home">' + topbar('home') +
      '<div class="hero"><div class="skel skel-line" style="width:8vw"></div><div class="skel" style="width:30vw;height:3.4vw;margin-bottom:1vw"></div>' +
      '<div class="skel skel-line" style="width:20vw"></div><div class="skel skel-line" style="width:40vw"></div></div>' +
      '<div class="rows">' + skeletonRows() + '</div></div>');
    get('/api/home', function (err, data) {
      if (err) return message('Couldn\'t load films', err.message, true);
      homeRows = data.rows;
      var html = '<div class="home">' + topbar('home') + '<div class="hero" id="hero"></div><div class="rows" id="screen">';
      data.rows.forEach(function (row, r) {
        html += '<div class="row" data-row="' + esc(row.title) + '"><h2 class="row-title">' + esc(row.title) +
          '<small>' + row.movies.length + ' films</small></h2><div class="strip">' + row.movies.map(card).join('') + '</div></div>';
      });
      mount(html + '</div></div>');
      restoreFocus() || focus(app.querySelector('.card'));
    });
  }

  function showHero(el, m) {
    var hero = document.getElementById('hero');
    if (!hero || !m) return;
    var row = el.parentNode && el.parentNode.parentNode && el.parentNode.parentNode.getAttribute('data-row');
    setBackdrop(m.backdrop);
    hero.innerHTML = '<div class="fade">' + (row ? '<div class="eyebrow">' + esc(row) + '</div>' : '') +
      '<h1>' + esc(m.title) + '</h1><div class="meta">' + metaHtml(m) + '</div>' +
      '<div class="overview">' + esc(m.overview || 'No synopsis yet.') + '</div></div>';
  }

  function renderSearch() {
    setBackdrop(null);
    mount('<div class="screen" id="screen">' + topbar('search') +
      '<div class="search-field"><span class="glass"></span>' +
      '<input class="searchbox focusable" id="q" data-key="q" type="search" autocomplete="off" spellcheck="false" ' +
      'placeholder="Search Tamil films" value="' + esc(lastQuery) + '"></div>' +
      '<div class="status" id="status"></div><div class="grid" id="results"></div></div>');
    var input = document.getElementById('q');
    var timer = null;
    input.oninput = function () {
      clearTimeout(timer);
      timer = setTimeout(function () { runSearch(input.value); }, 350);
    };
    runSearch(lastQuery);
    restoreFocus() || focus(input);
  }

  function showSuggestions() {
    var status = document.getElementById('status');
    var results = document.getElementById('results');
    var popular = null;
    (homeRows || []).forEach(function (r) { if (!popular && r.title === 'Popular') popular = r; });
    if (!popular && homeRows && homeRows.length) popular = homeRows[0];
    if (!popular) {
      status.textContent = 'Type a film title, for example Vikram, Jailer or Nayakan.';
      results.innerHTML = '';
      return get('/api/home', function (err, data) { if (!err) { homeRows = data.rows; if (!lastQuery.trim()) showSuggestions(); } });
    }
    status.textContent = 'Popular right now';
    results.innerHTML = popular.movies.slice(0, 14).map(card).join('');
  }

  function runSearch(q) {
    lastQuery = q;
    var status = document.getElementById('status');
    var results = document.getElementById('results');
    if (!status) return;
    if (!q.trim()) return showSuggestions();
    status.textContent = 'Searching…';
    get('/api/search?q=' + encodeURIComponent(q.trim()), function (err, data) {
      if (q !== lastQuery || !document.getElementById('results')) return;
      if (err) { status.textContent = err.message; return; }
      var n = data.results.length;
      status.textContent = n ? n + (n === 1 ? ' film' : ' films') + ' for “' + q.trim() + '”'
        : 'No Tamil films match “' + q.trim() + '”. Try a shorter title.';
      results.innerHTML = data.results.map(card).join('');
    });
  }

  function renderMovie(id) {
    var known = moviesById[id];
    setBackdrop(known && known.backdrop);
    mount('<div class="screen">' + topbar('') + '<div class="details"><div class="poster skel"></div><div class="info">' +
      (known ? '<h1>' + esc(known.title) + '</h1>' : '<div class="skel" style="width:30vw;height:3.4vw;margin-bottom:1vw"></div>') +
      '<div class="skel skel-line" style="width:22vw"></div><div class="skel skel-line" style="width:46vw"></div>' +
      '<div class="skel skel-line" style="width:40vw"></div></div></div></div>');
    get('/api/movie/' + id, function (err, m) {
      if (err) return message('Couldn\'t load this film', err.message, true);
      setBackdrop(m.backdrop);
      var html = '<div class="screen" id="screen">' + topbar('') + '<div class="details">' +
        '<div class="poster">' + (m.poster ? '<img src="' + IMG + 'w500' + m.poster + '" alt="">'
          : '<div class="card" style="width:100%;height:100%;margin:0">' + card(m).replace(/^<div[^>]*>/, '')) + '</div>' +
        '<div class="info"><h1>' + esc(m.title) + '</h1>' +
        (m.originalTitle ? '<div class="subtitle">' + esc(m.originalTitle) + '</div>' : '') +
        '<div class="meta">' + metaHtml(m) + '</div>' +
        (m.tagline ? '<div class="tagline">“' + esc(m.tagline) + '”</div>' : '') +
        '<div class="overview">' + esc(m.overview || 'No synopsis yet.') + '</div>' +
        '<div class="credits">' +
        (m.director ? '<div><label>Director</label><span>' + esc(m.director) + '</span></div>' : '') +
        (m.music ? '<div><label>Music</label><span>' + esc(m.music) + '</span></div>' : '') + '</div>' +
        '<div class="actions">' +
        (m.videos.length ? '<div class="btn primary focusable" data-key="play" data-play="' + esc(m.videos[0].key) + '"><span class="icon">▶</span>Play trailer</div>' : '') +
        '</div>' + whereToWatch(m) + '</div></div>';

      if (m.videos.length > 1) {
        html += '<div class="section"><h2 class="row-title">Trailers, songs and clips</h2><div class="strip">' +
          m.videos.slice(1).map(videoCard).join('') + '</div></div>';
      }
      if (m.cast.length) {
        html += '<div class="section"><h2 class="row-title">Cast</h2><div class="cast">' + m.cast.map(function (c) {
          return '<div class="person"><div class="face">' + (c.photo ? '<img loading="lazy" src="' + IMG + 'w185' + c.photo + '" alt="">' : initials(c.name)) +
            '</div><b>' + esc(c.name) + '</b><span>' + esc(c.character) + '</span></div>';
        }).join('') + '</div></div>';
      }
      if (m.more.length) {
        html += '<div class="section"><h2 class="row-title">More like this</h2><div class="strip">' + m.more.map(card).join('') + '</div></div>';
      }
      mount(html + '</div>');
      restoreFocus() || focusFirst();
    });
  }

  function whereToWatch(m) {
    function chips(list) {
      return list.map(function (p) {
        return '<span class="provider">' + (p.logo ? '<img src="' + IMG + 'w92' + p.logo + '" alt="">'
          : '<span class="logo">' + initials(p.name) + '</span>') + esc(p.name) + '</span>';
      }).join('');
    }
    if (m.streaming.length) {
      return '<div class="where"><span class="label">Streaming on</span>' + chips(m.streaming) +
        '<div class="hint">Open that app on your TV to watch.</div></div>';
    }
    if (m.rentBuy.length) return '<div class="where"><span class="label">Rent or buy on</span>' + chips(m.rentBuy) + '</div>';
    return '<div class="where"><span class="label">Not streaming in your region yet.</span></div>';
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
    if (id && moviesById[id] && document.getElementById('hero')) showHero(el, moviesById[id]);
    lastFocus[location.hash || '#/'] = el.getAttribute('data-key');
    return true;
  }

  function focusFirst() {
    var order = ['.details .focusable', '.message .focusable', '.card', '.focusable'];
    for (var i = 0; i < order.length; i++) {
      if (focus(app.querySelector(order[i]))) return true;
    }
    return false;
  }

  function restoreFocus() {
    var key = lastFocus[location.hash || '#/'];
    return key ? focus(app.querySelector('[data-key="' + key + '"]')) : false;
  }

  // Keep the focused element visible inside its horizontal strip and the vertical screen.
  function reveal(el) {
    var strip = el.parentNode;
    var inStrip = strip && strip.className.indexOf('strip') >= 0;
    if (inStrip) {
      var s = strip.getBoundingClientRect(), r = el.getBoundingClientRect();
      if (r.right > s.right) strip.scrollLeft += r.right - s.right + r.width * 0.3;
      else if (r.left < s.left) strip.scrollLeft -= s.left - r.left + r.width * 0.3;
    }
    var screen = document.getElementById('screen');
    if (!screen || !screen.contains(el)) return;
    var box = inStrip ? strip.parentNode : el;
    var sr = screen.getBoundingClientRect(), br = box.getBoundingClientRect();
    var margin = sr.height * 0.06;
    if (br.bottom > sr.bottom - margin) screen.scrollTop += br.bottom - sr.bottom + margin;
    else if (br.top < sr.top + margin) screen.scrollTop = Math.max(0, screen.scrollTop - (sr.top + margin - br.top));
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
      var dx = r.left + r.width / 2 - fx, dy = r.top + r.height / 2 - fy, main, cross;
      // Left/right only moves within the same band, so the top bar never jumps into the rows.
      var overlaps = r.bottom > from.top + from.height * 0.2 && r.top < from.bottom - from.height * 0.2;
      if (dir === 'left') { if (r.right > from.left + 1 || !overlaps) continue; main = -dx; cross = Math.abs(dy); }
      if (dir === 'right') { if (r.left < from.right - 1 || !overlaps) continue; main = dx; cross = Math.abs(dy); }
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
    if (el.getAttribute('data-go')) {
      if (location.hash === el.getAttribute('data-go') || (!location.hash && el.getAttribute('data-go') === '#/')) return;
      location.hash = el.getAttribute('data-go');
      return;
    }
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
  function focusableFrom(target) {
    var el = target;
    while (el && el !== app && !(el.className && el.className.indexOf && el.className.indexOf('focusable') >= 0)) el = el.parentNode;
    return el && el !== app ? el : null;
  }
  app.addEventListener('click', function (e) {
    var el = focusableFrom(e.target);
    if (el) { focus(el); activate(el); }
  });
  app.addEventListener('mouseover', function (e) {
    var el = focusableFrom(e.target);
    if (el && el !== current) focus(el);
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
