// Cloudflare Worker - Telegram Channel Proxy
// این Worker درخواست‌ها رو از t.me می‌گیره و برمی‌گردونه

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
};

export default {
  async fetch(request) {
    // Handle CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: CORS_HEADERS });
    }

    const url = new URL(request.url);
    
    // Route: /api/channel/{username} - fetch channel posts
    const channelMatch = url.pathname.match(/^\/api\/channel\/(.+)$/);
    if (channelMatch) {
      const username = channelMatch[1];
      return fetchChannel(username);
    }

    // Route: /api/fetch - proxy any URL
    if (url.pathname === '/api/fetch') {
      const targetUrl = url.searchParams.get('url');
      if (!targetUrl) {
        return jsonResponse({ error: 'Missing ?url= parameter' }, 400);
      }
      return fetchUrl(targetUrl);
    }

    return jsonResponse({
      name: 'ProxyHub Worker',
      version: '1.0',
      endpoints: [
        'GET /api/channel/{username} - Fetch Telegram channel',
        'GET /api/fetch?url=... - Proxy any URL'
      ]
    });
  }
};

async function fetchChannel(username) {
  try {
    // Try t.me/s/{username} first (public preview)
    let url = `https://t.me/s/${username}`;
    let response = await fetch(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
      },
      redirect: 'follow',
    });

    // If t.me/s fails, try t.me/{username}
    if (!response.ok) {
      url = `https://t.me/${username}`;
      response = await fetch(url, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
          'Accept': 'text/html,application/xhtml+xml',
        },
        redirect: 'follow',
      });
    }

    if (!response.ok) {
      return jsonResponse({
        success: false,
        error: `HTTP ${response.status}`,
        username: username
      });
    }

    const html = await response.text();
    
    // Extract title
    let title = `@${username}`;
    const titleMatch = html.match(/<div class="tgme_channel_info_header_title"><span dir="auto">(.*?)<\/span><\/div>/);
    if (titleMatch) {
      title = cleanHtml(titleMatch[1]);
    } else {
      const ogTitle = html.match(/<meta property="og:title" content="(.*?)">/);
      if (ogTitle) title = cleanHtml(ogTitle[1]);
    }

    // Extract description
    let description = '';
    const descMatch = html.match(/<div class="tgme_channel_info_description">(.*?)<\/div>/s);
    if (descMatch) {
      description = cleanHtml(descMatch[1]);
    }

    return jsonResponse({
      success: true,
      username: username,
      title: title,
      description: description,
      html: html, // Return raw HTML so app can parse configs
    });

  } catch (e) {
    return jsonResponse({
      success: false,
      error: e.message,
      username: username
    });
  }
}

async function fetchUrl(targetUrl) {
  try {
    const response = await fetch(targetUrl, {
      headers: {
        'User-Agent': 'v2rayNG/1.8.19',
        'Accept': '*/*',
      },
      redirect: 'follow',
    });

    const body = await response.text();
    return jsonResponse({
      success: response.ok,
      status: response.status,
      body: body,
    });

  } catch (e) {
    return jsonResponse({
      success: false,
      error: e.message,
    });
  }
}

function cleanHtml(text) {
  return text
    .replace(/<br\/?>/g, '\n')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/<.*?>/g, '')
    .trim();
}

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status: status,
    headers: {
      'Content-Type': 'application/json',
      ...CORS_HEADERS,
    },
  });
}
