<?php
// FitoTV, versión sin candado: lee listas M3U y guías XMLTV cuando el proveedor
// no deja leerlas desde páginas web. Solo atiende a la app de este mismo sitio
// y solo texto: los canales (video) nunca pasan por aquí.
declare(strict_types=1);

const MAX_BYTES = 150 * 1024 * 1024; // listas grandes con películas y series
const MAX_REDIRECTS = 5;
const PER_IP_LIMIT = 60;            // pedidos por persona…
const PER_IP_WINDOW = 600;          // …cada 10 minutos

@set_time_limit(150);
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');
header("Content-Security-Policy: default-src 'none'; sandbox");
header('Referrer-Policy: no-referrer');

function fail(int $code, string $why): void
{
    http_response_code($code);
    header('X-FitoTV-Error: ' . $why);
    header('Content-Type: text/plain; charset=utf-8');
    echo $why;
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') fail(405, 'method');

// Solo la propia app: el navegador manda Origin en cada POST.
$origin = (string)($_SERVER['HTTP_ORIGIN'] ?? '');
$self = strtolower(preg_replace('/:\d+$/', '', (string)($_SERVER['HTTP_HOST'] ?? '')));
if ($origin === '' || strtolower((string)parse_url($origin, PHP_URL_HOST)) !== $self) fail(403, 'origin');

// Freno sencillo por persona para que nadie lo use de intermediario.
$ip = (string)($_SERVER['REMOTE_ADDR'] ?? '');
$bucket = sys_get_temp_dir() . '/fitotv-' . md5($self . '|' . $ip);
$now = time();
$hits = array_filter(array_map('intval', explode(',', (string)@file_get_contents($bucket))), fn($t) => $t > $now - PER_IP_WINDOW);
if (count($hits) >= PER_IP_LIMIT) fail(429, 'slow-down');
$hits[] = $now;
@file_put_contents($bucket, implode(',', $hits), LOCK_EX);

$url = trim((string)($_POST['u'] ?? ''));
if ($url === '' || strlen($url) > 4096) fail(400, 'bad-url');

/** Revisa que la dirección sea pública (nada de la red interna del hosting). */
function target(string $url): array
{
    $p = parse_url($url);
    if (!$p || !isset($p['scheme'], $p['host'])) fail(400, 'bad-url');
    $scheme = strtolower($p['scheme']);
    if ($scheme !== 'http' && $scheme !== 'https') fail(400, 'bad-url');
    $host = trim($p['host'], '[]');
    $port = (int)($p['port'] ?? ($scheme === 'https' ? 443 : 80));
    $ips = [];
    if (filter_var($host, FILTER_VALIDATE_IP)) {
        $ips[] = $host;
    } else {
        foreach ((@dns_get_record($host, DNS_A | DNS_AAAA) ?: []) as $r) {
            if (isset($r['ip'])) $ips[] = $r['ip'];
            if (isset($r['ipv6'])) $ips[] = $r['ipv6'];
        }
        if (!$ips) $ips = @gethostbynamel($host) ?: [];
    }
    if (!$ips) fail(502, 'unreachable');
    $flags = FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE;
    if (defined('FILTER_FLAG_GLOBAL_RANGE')) $flags |= FILTER_FLAG_GLOBAL_RANGE;
    foreach ($ips as $a) {
        if (!filter_var($a, FILTER_VALIDATE_IP, $flags)) fail(400, 'private');
    }
    $pin = str_contains($ips[0], ':') ? '[' . $ips[0] . ']' : $ips[0];
    return ['host' => $host, 'port' => $port, 'pin' => $pin, 'scheme' => $scheme];
}

function absolute(string $base, string $loc): string
{
    if (preg_match('#^https?://#i', $loc)) return $loc;
    $b = parse_url($base);
    $root = $b['scheme'] . '://' . (isset($b['user']) ? $b['user'] . (isset($b['pass']) ? ':' . $b['pass'] : '') . '@' : '') . $b['host'] . (isset($b['port']) ? ':' . $b['port'] : '');
    if (str_starts_with($loc, '//')) return $b['scheme'] . ':' . $loc;
    if (str_starts_with($loc, '/')) return $root . $loc;
    $dir = preg_replace('#/[^/]*$#', '/', $b['path'] ?? '/');
    return $root . $dir . $loc;
}

for ($hop = 0; $hop <= MAX_REDIRECTS; $hop++) {
    $t = target($url);
    $status = 0;
    $location = null;
    $length = -1;
    $sent = false;
    $bytes = 0;
    $tooBig = false;

    $ch = curl_init($url);
    curl_setopt_array($ch, [
        CURLOPT_FOLLOWLOCATION => false,
        CURLOPT_RESOLVE => [$t['host'] . ':' . $t['port'] . ':' . $t['pin']],
        CURLOPT_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
        CURLOPT_CONNECTTIMEOUT => 15,
        CURLOPT_TIMEOUT => 140,
        CURLOPT_USERAGENT => 'VLC/3.0.20 LibVLC/3.0.20',
        CURLOPT_ENCODING => '',
        CURLOPT_HEADERFUNCTION => function ($c, string $line) use (&$status, &$location, &$length) {
            if (preg_match('#^HTTP/\S+\s+(\d{3})#', $line, $m)) {
                $status = (int)$m[1];
                $location = null;
                $length = -1;
            } elseif (stripos($line, 'location:') === 0) {
                $location = trim(substr($line, 9));
            } elseif (stripos($line, 'content-length:') === 0) {
                $length = (int)trim(substr($line, 15));
            }
            return strlen($line);
        },
        CURLOPT_WRITEFUNCTION => function ($c, string $data) use (&$status, &$length, &$sent, &$bytes, &$tooBig) {
            if ($status >= 300 && $status < 400) return strlen($data);
            if (!$sent) {
                if ($length > MAX_BYTES) {
                    $tooBig = true;
                    return 0;
                }
                $sent = true;
                http_response_code($status ?: 200);
                header('Content-Type: application/octet-stream');
                header('Content-Disposition: attachment');
            }
            $bytes += strlen($data);
            if ($bytes > MAX_BYTES) return 0;
            echo $data;
            flush();
            return strlen($data);
        },
    ]);
    curl_exec($ch);
    $err = curl_errno($ch);
    curl_close($ch);

    if ($tooBig) fail(413, 'too-big');
    if ($status >= 300 && $status < 400 && $location) {
        $url = absolute($url, $location);
        continue;
    }
    if (!$sent) {
        if ($err || $status === 0) fail(502, 'unreachable');
        http_response_code($status);
    }
    exit;
}
fail(502, 'unreachable');
