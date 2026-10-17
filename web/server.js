// Padam home server: serves the TV web app and proxies TMDB so the API key stays on this computer.
// Zero dependencies. Node 18+.  Run:  TMDB_API_KEY=xxxx node server.js
'use strict';
const http = require('http');
const fs = require('fs');
const path = require('path');
const mdns = require('./mdns');

loadDotEnv(path.join(__dirname, '.env'));
const API_KEY = (process.env.TMDB_API_KEY || '').trim();
const REGION = process.env.TMDB_REGION || 'IN';
const LANGUAGE = process.env.TMDB_LANGUAGE || 'en-IN';
// Port 80 is the browser default, so the TV address needs no ":port". Falls back to 8080 if 80 is unavailable.
const PORT = Number(process.env.PORT || 80);
const FALLBACK_PORT = 8080;
const NAME = (process.env.PADAM_NAME || 'padam').toLowerCase().replace(/\.local$/, '') + '.local';
const MOCK = process.env.PADAM_MOCK === '1';
const PUBLIC = path.join(__dirname, 'public');
const CACHE_MS = 6 * 60 * 60 * 1000;

function loadDotEnv(file) {
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z_]+)\s*=\s*(.*)\s*$/);
    if (m && !process.env[m[1]]) process.env[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
}

// ---- TMDB ----------------------------------------------------------------

const cache = new Map();

async function tmdb(endpoint, params = {}) {
  const url = new URL('https://api.themoviedb.org/3/' + endpoint);
  url.searchParams.set('language', LANGUAGE);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  const headers = { accept: 'application/json' };
  // v4 read tokens are long JWTs; v3 keys are 32 hex chars.
  if (API_KEY.length > 40) headers.authorization = 'Bearer ' + API_KEY;
  else url.searchParams.set('api_key', API_KEY);

  const key = url.toString();
  const hit = cache.get(key);
  if (hit && Date.now() - hit.at < CACHE_MS) return hit.data;
  const res = await fetch(url, { headers });
  if (!res.ok) throw new Error('TMDB ' + res.status + ' for ' + endpoint);
  const data = await res.json();
  cache.set(key, { at: Date.now(), data });
  return data;
}

const day = (offset = 0) => new Date(Date.now() + offset * 864e5).toISOString().slice(0, 10);
const tamil = { with_original_language: 'ta', include_adult: 'false' };
const genre = (id) => ({ ...tamil, with_genres: String(id), sort_by: 'popularity.desc', 'vote_count.gte': '10' });

function shelves() {
  return [
    ['In theatres', { ...tamil, sort_by: 'popularity.desc', 'primary_release_date.gte': day(-45), 'primary_release_date.lte': day() }],
    ['Streaming on your apps', { ...tamil, sort_by: 'primary_release_date.desc', watch_region: REGION,
      with_watch_monetization_types: 'flatrate|free', 'primary_release_date.lte': day() }],
    ['Popular', { ...tamil, sort_by: 'popularity.desc' }],
    ['Top rated', { ...tamil, sort_by: 'vote_average.desc', 'vote_count.gte': '150' }],
    ['Coming soon', { ...tamil, sort_by: 'popularity.desc', 'primary_release_date.gte': day(1) }],
    ['Action', genre(28)], ['Comedy', genre(35)], ['Thriller', genre(53)],
    ['Romance', genre(10749)], ['Family', genre(10751)],
    ['Classics', { ...tamil, sort_by: 'vote_average.desc', 'vote_count.gte': '50', 'primary_release_date.lte': '1999-12-31' }],
  ];
}

// TMDB's fixed movie genre ids, so list items can show genre names without an extra request.
const GENRES = { 28: 'Action', 12: 'Adventure', 16: 'Animation', 35: 'Comedy', 80: 'Crime', 99: 'Documentary',
  18: 'Drama', 10751: 'Family', 14: 'Fantasy', 36: 'History', 27: 'Horror', 10402: 'Music', 9648: 'Mystery',
  10749: 'Romance', 878: 'Sci-Fi', 10770: 'TV Movie', 53: 'Thriller', 10752: 'War', 37: 'Western' };

const slim = (m) => ({
  id: m.id, title: m.title, year: (m.release_date || '').slice(0, 4), rating: m.vote_average || 0,
  overview: m.overview || '', poster: m.poster_path, backdrop: m.backdrop_path,
  genres: (m.genre_ids || []).map((g) => GENRES[g]).filter(Boolean).slice(0, 3),
});

async function home() {
  const rows = await Promise.all(shelves().map(async ([title, params]) => {
    try {
      const page = await tmdb('discover/movie', params);
      return { title, movies: page.results.filter((m) => m.poster_path).map(slim) };
    } catch (e) {
      console.warn(e.message);
      return null;
    }
  }));
  return { rows: rows.filter((r) => r && r.movies.length) };
}

async function search(q) {
  const page = await tmdb('search/movie', { query: q, include_adult: 'false' });
  return { results: page.results.filter((m) => m.original_language === 'ta').map(slim) };
}

async function movie(id) {
  const m = await tmdb('movie/' + id, { append_to_response: 'credits,videos,watch/providers,recommendations' });
  const crew = (m.credits && m.credits.crew) || [];
  const who = (...jobs) => (crew.find((c) => jobs.includes(c.job)) || {}).name || null;
  const videos = ((m.videos && m.videos.results) || [])
    .filter((v) => v.site === 'YouTube')
    .sort((a, b) => (b.type === 'Trailer') - (a.type === 'Trailer') || b.official - a.official)
    .map((v) => ({ key: v.key, name: v.name, type: v.type }));
  const wp = (m['watch/providers'] && m['watch/providers'].results && m['watch/providers'].results[REGION]) || {};
  const names = (list) => {
    const seen = new Set();
    return (list || []).filter((p) => !seen.has(p.provider_id) && seen.add(p.provider_id))
      .map((p) => ({ name: p.provider_name, logo: p.logo_path }));
  };
  return {
    ...slim(m),
    originalTitle: m.original_title !== m.title ? m.original_title : null,
    tagline: m.tagline || null,
    runtime: m.runtime || null,
    genres: (m.genres || []).map((g) => g.name),
    director: who('Director'),
    music: who('Original Music Composer', 'Music'),
    cast: ((m.credits && m.credits.cast) || []).slice(0, 12)
      .map((c) => ({ name: c.name, character: c.character || '', photo: c.profile_path })),
    videos,
    streaming: names([...(wp.flatrate || []), ...(wp.free || []), ...(wp.ads || [])]),
    rentBuy: names([...(wp.rent || []), ...(wp.buy || [])]),
    more: ((m.recommendations && m.recommendations.results) || [])
      .filter((r) => r.original_language === 'ta' && r.poster_path).map(slim),
  };
}

// Offline fixture data so the UI can be tried without a key (PADAM_MOCK=1).
const mock = (() => {
  const titles = ['Vikram', 'Jailer', 'Nayakan', 'Anbe Sivam', 'Super Deluxe', 'Asuran', 'Kaithi', 'Vada Chennai',
    'Jigarthanda', 'Soorarai Pottru', 'Visaranai', '96', 'Pariyerum Perumal', 'Karnan', 'Mahanadhi'];
  const movies = titles.map((t, i) => ({ id: 1000 + i, title: t, year: String(1987 + i * 2), rating: 7 + (i % 3) / 2,
    overview: t + ' is a Tamil film. This is sample text shown in mock mode so the layout can be checked without a TMDB key. It runs long enough to wrap onto a few lines.',
    poster: null, backdrop: null, genres: [['Action', 'Thriller'], ['Drama'], ['Comedy', 'Family'], ['Crime', 'Drama']][i % 4] }));
  const rows = shelves().map(([title], r) => ({ title, movies: movies.slice(r % 5).concat(movies.slice(0, r % 5)) }));
  return {
    home: () => ({ rows }),
    search: (q) => ({ results: movies.filter((m) => m.title.toLowerCase().includes(q.toLowerCase())) }),
    movie: (id) => {
      const m = movies.find((x) => x.id === Number(id)) || movies[0];
      return { ...m, originalTitle: null, tagline: 'Sample tagline', runtime: 154, genres: ['Action', 'Drama'],
        director: 'Sample Director', music: 'Sample Composer',
        cast: ['Kamal Haasan', 'Vijay Sethupathi', 'Fahadh Faasil', 'Gayathrie', 'Kalidas Jayaram', 'Narain']
          .map((n, i) => ({ name: n, character: 'Role ' + (i + 1), photo: null })),
        videos: [{ key: 'dQw4w9WgXcQ', name: 'Official Trailer', type: 'Trailer' }, { key: 'dQw4w9WgXcQ', name: 'Pathala Pathala (Lyric Video)', type: 'Clip' },
          { key: 'dQw4w9WgXcQ', name: 'Teaser', type: 'Teaser' }, { key: 'dQw4w9WgXcQ', name: 'Making of', type: 'Behind the Scenes' }],
        streaming: [{ name: 'Netflix', logo: null }, { name: 'Amazon Prime Video', logo: null }],
        rentBuy: [{ name: 'Apple TV', logo: null }], more: movies.slice(0, 6) };
    },
  };
})();

// ---- HTTP ----------------------------------------------------------------

const TYPES = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon' };

function send(res, status, body, type) {
  res.writeHead(status, { 'content-type': type || 'application/json; charset=utf-8', 'cache-control': 'no-store' });
  res.end(typeof body === 'string' || Buffer.isBuffer(body) ? body : JSON.stringify(body));
}

async function api(pathname, query) {
  const src = MOCK ? mock : { home, search, movie };
  if (pathname === '/api/config') return { region: REGION, ready: MOCK || !!API_KEY, mock: MOCK };
  if (!MOCK && !API_KEY) throw Object.assign(new Error('TMDB_API_KEY is not set on the server.'), { status: 503 });
  if (pathname === '/api/home') return src.home();
  if (pathname === '/api/search') return src.search((query.get('q') || '').trim());
  const m = pathname.match(/^\/api\/movie\/(\d+)$/);
  if (m) return src.movie(m[1]);
  throw Object.assign(new Error('Not found'), { status: 404 });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://local');
  try {
    if (url.pathname.startsWith('/api/')) return send(res, 200, await api(url.pathname, url.searchParams));
    const file = path.normalize(path.join(PUBLIC, url.pathname === '/' ? 'index.html' : url.pathname));
    if (!file.startsWith(PUBLIC) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
      return send(res, 404, 'Not found', 'text/plain');
    }
    send(res, 200, fs.readFileSync(file), TYPES[path.extname(file)] || 'application/octet-stream');
  } catch (e) {
    send(res, e.status || 502, { error: e.message });
  }
});

function announce(port) {
  const suffix = port === 80 ? '' : ':' + port;
  const ips = mdns.lanAddresses().map((i) => i.address);
  console.log('Padam is running' + (MOCK ? ' (mock data)' : '') + '. On your TV browser, open:');
  console.log('');
  console.log('    http://' + NAME + suffix);
  console.log('');
  console.log('If the TV says it can\'t find that, use: ' +
    (ips.length ? ips.map((ip) => 'http://' + ip + suffix).join('  or  ') : 'http://localhost' + suffix));
  if (port !== 80) {
    console.log('(Port 80 was unavailable, so the address needs ":' + port + '". See README, "No port number".)');
  }
  if (!MOCK && !API_KEY) console.log('Warning: TMDB_API_KEY is not set. Put it in .env next to server.js.');
}

server.once('error', (e) => {
  if (process.env.PORT || (e.code !== 'EACCES' && e.code !== 'EADDRINUSE')) throw e;
  server.listen(FALLBACK_PORT, '0.0.0.0');
});
server.on('listening', () => announce(server.address().port));
server.listen(PORT, '0.0.0.0');
mdns.start(NAME);
