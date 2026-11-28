' FitoTV para Roku: funciones que no dependen de la pantalla.
' Las usan tanto las pantallas como las tareas en segundo plano.

function fcColors() as object
    return {
        noche: "0x0A1747FF", panel: "0x0D1C55FF", s1: "0x0F1F5CFF", s2: "0x12235FFF", s3: "0x1A2E78FF",
        line: "0x1A2C6EFF", line2: "0x22397FFF", line3: "0x2A3F86FF", text: "0xF5F7FFFF", muted: "0xAEBBEBFF",
        muted2: "0xC8D2F5FF", dim: "0x8E9BD0FF", num: "0x9DB2FFFF", track: "0x1F3378FF", azul: "0x2350E0FF",
        menta: "0x3DE0BEFF", ambar: "0xFFC53DFF", glow: "0x13298AFF", white: "0xFFFFFFFF", black: "0x000000FF",
        clear: "0x00000000", scrim: "0x0A1747E6"
    }
end function

function tileColors() as object
    return ["0x3A55B8FF", "0x1F3A9EFF", "0x6A4FC2FF", "0x1F7A8CFF", "0x2E6A8FFF", "0x4B3FA8FF", "0x1F5F9EFF", "0x3A6FB8FF"]
end function

function tileFor(key as string) as string
    t = tileColors()
    h = 0
    for i = 1 to Len(key)
        h = (h * 31 + Asc(Mid(key, i, 1))) mod 1000003
    end for
    return t[h mod t.Count()]
end function

function userAgent() as string
    return "FitoTV/1.0 (Roku)"
end function

' Número desde texto o número (las API mezclan los dos).
function numOf(v as dynamic) as integer
    t = type(v)
    if t = "String" or t = "roString" then return Int(Val(v))
    if t = "Integer" or t = "roInteger" or t = "LongInteger" or t = "Float" or t = "roFloat" or t = "Double" or t = "roDouble" then return Int(v)
    return 0
end function

' ------------------------------------------------------------------ tiempo
function nowSec() as integer
    return CreateObject("roDateTime").AsSeconds()
end function

function hhmm(sec as integer) as string
    d = CreateObject("roDateTime")
    d.FromSeconds(sec)
    d.ToLocalTime()
    h = d.GetHours()
    mi = d.GetMinutes()
    return pad2(h) + ":" + pad2(mi)
end function

' "5 nov, 14:30"
function dayMonthTime(sec as integer) as string
    d = CreateObject("roDateTime")
    d.FromSeconds(sec)
    d.ToLocalTime()
    months = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]
    return d.GetDayOfMonth().ToStr() + " " + months[d.GetMonth() - 1] + ", " + pad2(d.GetHours()) + ":" + pad2(d.GetMinutes())
end function

function pad2(n as integer) as string
    s = n.ToStr()
    if Len(s) < 2 then s = "0" + s
    return s
end function

function newId() as string
    d = CreateObject("roDateTime")
    r = Rnd(2000000000)
    return md5short(d.AsSeconds().ToStr() + "-" + d.GetMilliseconds().ToStr() + "-" + r.ToStr())
end function

' ------------------------------------------------------------------ texto
function md5short(s as string) as string
    ba = CreateObject("roByteArray")
    ba.FromAsciiString(s)
    dg = CreateObject("roEVPDigest")
    dg.Setup("md5")
    return Left(dg.Process(ba), 12)
end function

function fold(s as string) as string
    t = LCase(s)
    pairs = [["á", "a"], ["à", "a"], ["ä", "a"], ["â", "a"], ["é", "e"], ["è", "e"], ["ë", "e"], ["ê", "e"], ["í", "i"], ["ì", "i"], ["ï", "i"], ["ó", "o"], ["ò", "o"], ["ö", "o"], ["ô", "o"], ["ú", "u"], ["ù", "u"], ["ü", "u"], ["ñ", "n"], ["ç", "c"]]
    for each p in pairs
        t = t.Replace(p[0], p[1])
    end for
    return t
end function

function isKidsGroup(group as string) as boolean
    g = fold(group)
    for each w in ["infantil", "kids", "nino", "ninos", "children", "cartoon", "caricatura", "animad", "familia"]
        if Instr(1, g, w) > 0 then return true
    end for
    return false
end function

function groupIcon(name as string) as string
    g = fold(name)
    if hasAny(g, ["notici", "news", "informa"]) then return "news"
    if hasAny(g, ["deport", "sport", "futbol", "liga", "box", "nfl", "nba"]) then return "ball"
    if isKidsGroup(name) then return "kids"
    if hasAny(g, ["cine", "pelic", "movie", "film"]) then return "film"
    if hasAny(g, ["musica", "music", "radio"]) then return "music"
    if hasAny(g, ["document", "cultura", "educa"]) then return "book"
    if hasAny(g, ["mexico", "local", "nacional", "abierta"]) then return "pin"
    if hasAny(g, ["usa", "latino", "espana", "internacional", "world", "argentina", "colombia"]) then return "globe"
    return "tv"
end function

function hasAny(s as string, words as object) as boolean
    for each w in words
        if Instr(1, s, w) > 0 then return true
    end for
    return false
end function

function initials(name as string) as string
    skip = {de: 1, del: 1, la: 1, el: 1, en: 1, y: 1, los: 1, las: 1, tv: 1, hd: 1, fhd: 1, sd: 1, "4k": 1, canal: 1}
    words = []
    cur = ""
    for i = 1 to Len(name)
        c = Mid(name, i, 1)
        if isWordChar(c) then
            cur = cur + c
        else if cur <> "" then
            words.Push(cur)
            cur = ""
        end if
    end for
    if cur <> "" then words.Push(cur)
    for each w in words
        if Len(w) <= 3 and isDigits(w) then return w
    end for
    out = ""
    for each w in words
        if not skip.DoesExist(LCase(w)) and Len(out) < 2 then out = out + UCase(Left(w, 1))
    end for
    if out = "" and words.Count() > 0 then out = UCase(Left(words[0], 2))
    if out = "" then out = "TV"
    return out
end function

function isDigits(s as string) as boolean
    if Len(s) = 0 then return false
    for i = 1 to Len(s)
        c = Asc(Mid(s, i, 1))
        if c < 48 or c > 57 then return false
    end for
    return true
end function

' Letras (incluidas las acentuadas) y números.
function isWordChar(c as string) as boolean
    if c = "" then return false
    k = Asc(c)
    if k >= 48 and k <= 57 then return true
    if k >= 65 and k <= 90 then return true
    if k >= 97 and k <= 122 then return true
    if k >= 192 and k <= 687 and k <> 215 and k <> 247 then return true
    if k >= 880 and k <= 1279 then return true
    return false
end function

function urlEncode(s as string) as string
    ba = CreateObject("roByteArray")
    ba.FromAsciiString(s)
    out = ""
    hexd = "0123456789ABCDEF"
    for i = 0 to ba.Count() - 1
        b = ba[i]
        if (b >= 48 and b <= 57) or (b >= 65 and b <= 90) or (b >= 97 and b <= 122) or b = 45 or b = 95 or b = 46 or b = 126 then
            out = out + Chr(b)
        else
            out = out + "%" + Mid(hexd, (b \ 16) + 1, 1) + Mid(hexd, (b mod 16) + 1, 1)
        end if
    end for
    return out
end function

function urlDecode(s as string) as string
    ba = CreateObject("roByteArray")
    hexv = "0123456789abcdef"
    i = 1
    n = Len(s)
    while i <= n
        c = Mid(s, i, 1)
        if c = "+" then
            ba.Push(32)
            i = i + 1
        else if c = "%" and i + 2 <= n then
            h1 = Instr(1, hexv, LCase(Mid(s, i + 1, 1))) - 1
            h2 = Instr(1, hexv, LCase(Mid(s, i + 2, 1))) - 1
            if h1 >= 0 and h2 >= 0 then
                ba.Push(h1 * 16 + h2)
                i = i + 3
            else
                ba.Push(37)
                i = i + 1
            end if
        else
            t = CreateObject("roByteArray")
            t.FromAsciiString(c)
            for j = 0 to t.Count() - 1
                ba.Push(t[j])
            end for
            i = i + 1
        end if
    end while
    return ba.ToAsciiString()
end function

function parseForm(body as string) as object
    out = {}
    for each pair in body.Split("&")
        if pair <> "" then
            k = pair
            v = ""
            p = Instr(1, pair, "=")
            if p > 0 then
                k = Left(pair, p - 1)
                v = Mid(pair, p + 1)
            end if
            out[urlDecode(k)] = urlDecode(v)
        end if
    end for
    return out
end function

function escapeHtml(s as string) as string
    return s.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace(Chr(34), "&quot;")
end function

' ------------------------------------------------------------------ guardado
function regRead(key as string) as dynamic
    sec = CreateObject("roRegistrySection", "FitoTV")
    if not sec.Exists(key) then return invalid
    v = ParseJson(sec.Read(key))
    return v
end function

sub regWrite(key as string, value as dynamic)
    sec = CreateObject("roRegistrySection", "FitoTV")
    sec.Write(key, FormatJson(value))
    sec.Flush()
end sub

sub regDelete(key as string)
    sec = CreateObject("roRegistrySection", "FitoTV")
    sec.Delete(key)
    sec.Flush()
end sub

' ------------------------------------------------------------------ Xtream y formatos de video
function normalizeServer(server as string) as string
    s = server.Trim()
    while Right(s, 1) = "/"
        s = Left(s, Len(s) - 1)
    end while
    l = LCase(s)
    if Left(l, 7) <> "http://" and Left(l, 8) <> "https://" then s = "http://" + s
    return s
end function

function xtreamM3uUrl(server as string, user as string, pass as string) as string
    return normalizeServer(server) + "/get.php?username=" + urlEncode(user) + "&password=" + urlEncode(pass) + "&type=m3u_plus&output=m3u8"
end function

function xtreamApi(server as string, user as string, pass as string) as string
    return normalizeServer(server) + "/player_api.php?username=" + urlEncode(user) + "&password=" + urlEncode(pass)
end function

' Roku no reproduce MPEG-TS suelto: los canales de paneles Xtream se piden en HLS.
function playableUrl(url as string) as string
    q = Instr(1, url, "?")
    path = url
    query = ""
    if q > 0 then
        path = Left(url, q - 1)
        query = Mid(url, q)
    end if
    l = LCase(path)
    if Right(l, 3) = ".ts" and Instr(1, l, "/live/") > 0 then return Left(path, Len(path) - 3) + ".m3u8" + query
    return url
end function

function streamFormatFor(url as string) as string
    q = Instr(1, url, "?")
    path = LCase(url)
    if q > 0 then path = LCase(Left(url, q - 1))
    if Right(path, 4) = ".mp4" or Right(path, 4) = ".m4v" or Right(path, 4) = ".mov" then return "mp4"
    if Right(path, 4) = ".mkv" or Right(path, 5) = ".webm" then return "mkv"
    if Right(path, 4) = ".mpd" then return "dash"
    if Right(path, 4) = ".mp3" then return "mp3"
    return "hls"
end function

function xtreamStreamId(url as string) as string
    q = Instr(1, url, "?")
    path = url
    if q > 0 then path = Left(url, q - 1)
    parts = path.Split("/")
    if parts.Count() = 0 then return ""
    last = parts[parts.Count() - 1]
    dot = Instr(1, last, ".")
    if dot > 0 then last = Left(last, dot - 1)
    if isDigits(last) then return last
    return ""
end function

' ------------------------------------------------------------------ M3U
' Qué es cada entrada: en vivo, película o serie.
' Primero lo que diga la lista (tvg-type), luego el enlace y al final el grupo y el nombre.
function classify(url as string, group as string, name = "" as string, typ = "" as string) as string
    q = Instr(1, url, "?")
    path = LCase(url)
    if q > 0 then path = LCase(Left(url, q - 1))
    g = fold(group)
    n = fold(name)
    t = LCase(typ.Trim())
    if t = "movie" or t = "movies" or t = "vod" or t = "film" then return "MOVIE"
    if t = "series" or t = "serie" or t = "tvshow" or t = "show" then return "SERIES"
    if t = "live" or t = "tv" then return "LIVE"
    if Instr(1, path, "/series/") > 0 then return "SERIES"
    if Instr(1, path, "/movie/") > 0 or Instr(1, path, "/movies/") > 0 then return "MOVIE"
    for each ext in [".mp4", ".mkv", ".avi", ".mov", ".m4v", ".wmv", ".webm"]
        if Right(path, Len(ext)) = ext then
            if Instr(1, g, "serie") > 0 or looksLikeEpisode(n) then return "SERIES"
            return "MOVIE"
        end if
    end for
    ' Enlaces típicos de tele en vivo
    if Instr(1, path, "/live/") > 0 or Right(path, 3) = ".ts" then return "LIVE"
    ' Sin pistas en el enlace: grupos tipo "Movies / Hindi / 2026" o "Series 2024".
    ' Un grupo "Películas" a secas puede ser de canales en vivo, por eso se pide además un año
    ' o un capítulo en el nombre.
    vodGroup = hasAny(g, ["movie", "pelicula", "vod"])
    seriesGroup = hasAny(g, ["serie", "tv show", "tvshow", "temporada", "season"])
    ep = looksLikeEpisode(n)
    if ep and (seriesGroup or vodGroup) then return "SERIES"
    if seriesGroup and (hasYear(g) or hasYear(n)) then return "SERIES"
    if vodGroup and (hasYear(g) or hasYear(n)) then return "MOVIE"
    return "LIVE"
end function

' ¿Trae un año suelto entre 1900 y 2099?
function hasYear(s as string) as boolean
    sl = Len(s)
    i = 1
    while i <= sl - 3
        y = Mid(s, i, 4)
        if isDigits(y) and (Left(y, 2) = "19" or Left(y, 2) = "20") then
            okBefore = i = 1
            if not okBefore then okBefore = not isDigits(Mid(s, i - 1, 1))
            okAfter = i + 4 > sl
            if not okAfter then okAfter = not isDigits(Mid(s, i + 4, 1))
            if okBefore and okAfter then return true
        end if
        i = i + 1
    end while
    return false
end function

' S01E02, S1 E2, 1x02, "Temporada", "Capítulo"... (el texto ya viene en minúsculas)
function looksLikeEpisode(n as string) as boolean
    if hasAny(n, ["temporada", "capitulo", "episodio", "episode", "season "]) then return true
    sl = Len(n)
    for i = 1 to sl
        ch = Mid(n, i, 1)
        if ch = "s" or ch = "t" then
            before = i = 1
            if not before then before = not isWordChar(Mid(n, i - 1, 1))
            if before then
                j = i + 1
                while j <= sl and isDigits(Mid(n, j, 1))
                    j = j + 1
                end while
                if j > i + 1 then
                    k = j
                    while k <= sl and (Mid(n, k, 1) = " " or Mid(n, k, 1) = "." or Mid(n, k, 1) = "-" or Mid(n, k, 1) = ":")
                        k = k + 1
                    end while
                    if k <= sl and Mid(n, k, 1) = "e" then
                        k2 = k + 1
                        while k2 <= sl and isDigits(Mid(n, k2, 1))
                            k2 = k2 + 1
                        end while
                        if k2 > k + 1 then return true
                    end if
                end if
            end if
        else if ch = "x" and i > 1 and i < sl then
            if isDigits(Mid(n, i - 1, 1)) and isDigits(Mid(n, i + 1, 1)) then
                a = i - 1
                while a > 1 and isDigits(Mid(n, a - 1, 1))
                    a = a - 1
                end while
                b = i + 1
                while b < sl and isDigits(Mid(n, b + 1, 1))
                    b = b + 1
                end while
                okA = a = 1
                if not okA then okA = not isWordChar(Mid(n, a - 1, 1))
                okB = b = sl
                if not okB then okB = not isWordChar(Mid(n, b + 1, 1))
                if okA and okB and (b - i) = 2 and (i - a) <= 2 then return true
            end if
        end if
    end for
    return false
end function

function m3uAttributes(line as string) as object
    out = {}
    i = 1
    q2 = Chr(34)
    while true
        q = Instr(i, line, "=" + q2)
        if q = 0 then exit while
        k = q - 1
        while k >= 1 and Mid(line, k, 1) = " "
            k = k - 1
        end while
        e = k
        while k >= 1
            c = Mid(line, k, 1)
            if not (isAsciiLetter(c) or isDigits(c) or c = "_" or c = "-") then exit while
            k = k - 1
        end while
        key = Mid(line, k + 1, e - k)
        cl = Instr(q + 2, line, q2)
        if cl = 0 then exit while
        if key <> "" then out[LCase(key)] = Mid(line, q + 2, cl - q - 2)
        i = cl + 1
    end while
    return out
end function

function m3uDisplayName(line as string) as string
    inQ = false
    for i = 1 to Len(line)
        c = Mid(line, i, 1)
        if c = Chr(34) then
            inQ = not inQ
        else if c = "," and not inQ then
            return Mid(line, i + 1).Trim()
        end if
    end for
    return ""
end function

function looksLikeUrl(line as string) as boolean
    l = LCase(Left(line, 8))
    return Left(l, 7) = "http://" or Left(l, 8) = "https://" or Left(l, 7) = "rtmp://" or Left(l, 7) = "rtsp://"
end function

' Devuelve { channels: [ {id, num, name, url, logo, group, tvgId, kind, ua, ref} ], epgUrl }
function parseM3U(text as string, useMd5 = true as boolean) as object
    out = []
    epgUrl = ""
    pendName = invalid
    pendAttrs = {}
    pendGroup = ""
    ua = ""
    ref = ""
    seen = {}
    liveNum = 0
    lines = text.Split(Chr(10))
    for each raw in lines
        line = raw.Trim()
        if Left(line, 1) = Chr(65279) then line = Mid(line, 2)
        if line = "" then
            ' nada
        else if UCase(Left(line, 7)) = "#EXTM3U" then
            a = m3uAttributes(line)
            tvg = ""
            if a.DoesExist("x-tvg-url") then
                tvg = a["x-tvg-url"]
            else if a.DoesExist("url-tvg") then
                tvg = a["url-tvg"]
            else if a.DoesExist("tvg-url") then
                tvg = a["tvg-url"]
            end if
            if tvg <> "" then epgUrl = tvg.Split(",")[0].Trim()
        else if UCase(Left(line, 7)) = "#EXTINF" then
            pendAttrs = m3uAttributes(line)
            pendName = m3uDisplayName(line)
        else if UCase(Left(line, 8)) = "#EXTGRP:" then
            pendGroup = Mid(line, 9).Trim()
        else if LCase(Left(line, 27)) = "#extvlcopt:http-user-agent=" then
            ua = Mid(line, 28).Trim()
        else if LCase(Left(line, 25)) = "#extvlcopt:http-referrer=" then
            ref = Mid(line, 26).Trim()
        else if Left(line, 1) = "#" then
            ' otra etiqueta
        else if looksLikeUrl(line) then
            name = ""
            if pendName <> invalid then name = pendName
            if name = "" and pendAttrs.DoesExist("tvg-name") then name = pendAttrs["tvg-name"]
            if name = "" then
                parts = line.Split("/")
                name = parts[parts.Count() - 1]
            end if
            name = name.Trim()
            if name = "" then name = "Canal"
            group = pendGroup
            if pendAttrs.DoesExist("group-title") then group = pendAttrs["group-title"]
            group = group.Trim()
            if group = "" then group = "Sin grupo"
            typ = ""
            if pendAttrs.DoesExist("tvg-type") then
                typ = pendAttrs["tvg-type"]
            else if pendAttrs.DoesExist("type") then
                typ = pendAttrs["type"]
            end if
            kind = classify(line, group, name, typ)
            num = 0
            if kind = "LIVE" then
                liveNum = liveNum + 1
                num = liveNum
                if pendAttrs.DoesExist("tvg-chno") then
                    n = Val(pendAttrs["tvg-chno"].Trim())
                    if n > 0 then num = Int(n)
                end if
            end if
            if useMd5 then
                base = md5short(line + "|" + name)
            else
                base = "c" + out.Count().ToStr()
            end if
            if seen.DoesExist(base) then
                seen[base] = seen[base] + 1
                id = base + "-" + seen[base].ToStr()
            else
                seen[base] = 0
                id = base
            end if
            logo = ""
            if pendAttrs.DoesExist("tvg-logo") then logo = pendAttrs["tvg-logo"].Trim()
            tvgId = ""
            if pendAttrs.DoesExist("tvg-id") then tvgId = pendAttrs["tvg-id"].Trim()
            if pendAttrs.DoesExist("user-agent") then ua = pendAttrs["user-agent"]
            out.Push({id: id, num: num, name: name, url: line, logo: logo, group: group, tvgId: tvgId, kind: kind, ua: ua, ref: ref})
            pendName = invalid
            pendAttrs = {}
            pendGroup = ""
            ua = ""
            ref = ""
        end if
    end for
    return {channels: out, epgUrl: epgUrl}
end function

' ------------------------------------------------------------------ nombres limpios
' Igual que en la app de Android: quita adornos y etiquetas del proveedor que se repiten
' ("P2 - ", "MX: ", "[LAT] "), la resolución al final "(720p)", las notas repetidas "[Not 24/7]"
' y los códigos del proveedor al final ("Canal 5 P2"). Nunca deja "P2" a la vista.
function cleanNames(channels as object) as object
    if channels.Count() = 0 then return channels
    st = namesState()
    counts = {}
    notes = {}
    tails = {}
    for each ch in channels
        nm = normalizeSeps(ch.name)
        for each t in nameTags(nm, st)
            k = "k" + t
            if counts.DoesExist(k) then counts[k] = counts[k] + 1 else counts[k] = 1
        end for
        nt = trailingNote(nm)
        if nt <> invalid then
            k = "k" + UCase(nt.inner)
            if notes.DoesExist(k) then notes[k] = notes[k] + 1 else notes[k] = 1
        end if
        tc = trailingCode(stripTrailingJunk(nm))
        if tc <> invalid then
            k = "k" + UCase(tc.code)
            if tails.DoesExist(k) then tails[k] = tails[k] + 1 else tails[k] = 1
        end if
    end for
    minRep = 3
    if channels.Count() < 10 then minRep = 2
    remTags = keepAtLeast(counts, minRep)
    remNotes = keepAtLeast(notes, minRep)
    remTails = keepAtLeast(tails, minRep)

    cleaned = []
    byName = {}
    for i = 0 to channels.Count() - 1
        r = stripName(normalizeSeps(channels[i].name), remTags, remNotes, remTails, st)
        cleaned.Push(r)
        k = "k" + LCase(r.name)
        if not byName.DoesExist(k) then byName[k] = []
        byName[k].Push(i)
    end for
    for i = 0 to channels.Count() - 1
        r = cleaned[i]
        idxs = byName["k" + LCase(r.name)]
        finalName = r.name
        if idxs.Count() > 1 then
            differs = false
            for each j in idxs
                if cleaned[j].removedKey <> r.removedKey then differs = true
            end for
            if differs then
                hint = ""
                for each t in r.removed
                    if not codeLike(t) then hint = t
                end for
                if hint <> "" then finalName = r.name + " · " + hint
            end if
        end if
        channels[i].name = finalName
    end for
    return channels
end function

function namesState() as object
    return {
        countries: {MX: 1, MEX: 1, US: 1, USA: 1, ES: 1, ESP: 1, SPA: 1, AR: 1, ARG: 1, CO: 1, COL: 1, PE: 1, PER: 1, CL: 1, CHI: 1, EC: 1, VE: 1, UY: 1, PY: 1, BO: 1, CR: 1, GT: 1, HN: 1, SV: 1, NI: 1, PA: 1, DO: 1, RD: 1, PR: 1, CU: 1, BR: 1, PT: 1, UK: 1, GB: 1, CA: 1, FR: 1, DE: 1, IT: 1, LA: 1, LAT: 1, LATAM: 1, LATINO: 1, LATIN: 1, EN: 1, ENG: 1}
    }
end function

function isAsciiLetter(c as string) as boolean
    if c = "" then return false
    k = Asc(c)
    return (k >= 65 and k <= 90) or (k >= 97 and k <= 122)
end function

' "P2", "S12", "HD1": 1 a 3 letras y luego 1 a 3 números.
function codeLike(t as string) as boolean
    n = Len(t)
    if n < 2 or n > 6 then return false
    i = 1
    while i <= n and isAsciiLetter(Mid(t, i, 1))
        i = i + 1
    end while
    letters = i - 1
    if letters < 1 or letters > 3 then return false
    digits = n - letters
    if digits < 1 or digits > 3 then return false
    return isDigits(Mid(t, i))
end function

function isResolution(t as string) as boolean
    if t = "UHD" or t = "FHD" or t = "HD" or t = "SD" or LCase(t) = "4k" then return true
    n = Len(t)
    if n < 4 or n > 5 then return false
    last = LCase(Right(t, 1))
    return (last = "p" or last = "i") and isDigits(Left(t, n - 1))
end function

' Nota entre corchetes al final: { inner, cut } (cut = cuántos caracteres quitar del final).
function trailingNote(s as string) as dynamic
    t = s.Trim()
    if Right(t, 1) <> "]" then return invalid
    p = 0
    for i = Len(t) - 1 to 1 step -1
        if Mid(t, i, 1) = "[" then
            p = i
            exit for
        end if
    end for
    if p = 0 then return invalid
    inner = Mid(t, p + 1, Len(t) - p - 1)
    if Len(inner) < 1 or Len(inner) > 24 then return invalid
    return {inner: inner.Trim(), keep: RTrimStr(Left(t, p - 1))}
end function

function trailingRes(s as string) as dynamic
    t = s.Trim()
    if Right(t, 1) <> ")" then return invalid
    p = 0
    for i = Len(t) - 1 to 1 step -1
        if Mid(t, i, 1) = "(" then
            p = i
            exit for
        end if
    end for
    if p = 0 then return invalid
    inner = Mid(t, p + 1, Len(t) - p - 1)
    if not isResolution(inner) then return invalid
    return {inner: inner, keep: RTrimStr(Left(t, p - 1))}
end function

' Código del proveedor al final: "Canal 5 P2", "Canal 5 (P2)", "Canal 5 [P2]", "Canal 5 - P2".
function trailingCode(s as string) as dynamic
    t = s.Trim()
    closing = Right(t, 1)
    if closing = ")" or closing = "]" then t = Left(t, Len(t) - 1)
    i = Len(t)
    while i >= 1 and (isAsciiLetter(Mid(t, i, 1)) or isDigits(Mid(t, i, 1)))
        i = i - 1
    end while
    code = Mid(t, i + 1)
    if not codeLike(code) then return invalid
    j = i
    if closing = ")" or closing = "]" then
        opener = "("
        if closing = "]" then opener = "["
        if Mid(t, j, 1) <> opener then return invalid
        j = j - 1
    end if
    sepFound = false
    while j >= 1 and Mid(t, j, 1) = " "
        sepFound = true
        j = j - 1
    end while
    c = Mid(t, j, 1)
    if c = "-" or c = "|" or c = ":" or c = "/" then
        sepFound = true
        j = j - 1
        while j >= 1 and Mid(t, j, 1) = " "
            j = j - 1
        end while
    end if
    if not sepFound or j < 1 then return invalid
    return {code: code, keep: Left(t, j)}
end function

function RTrimStr(s as string) as string
    i = Len(s)
    while i >= 1 and Mid(s, i, 1) = " "
        i = i - 1
    end while
    return Left(s, i)
end function

function collapseSpaces(s as string) as string
    t = s
    while Instr(1, t, "  ") > 0
        t = t.Replace("  ", " ")
    end while
    return t
end function

function keepAtLeast(counts as object, n as integer) as object
    out = {}
    for each k in counts
        if counts[k] >= n then out[k] = true
    end for
    return out
end function

' Guiones y viñetas raros como separador normal; espacios raros como espacio.
function normalizeSeps(s as string) as string
    t = s
    for each c in [Chr(8211), Chr(8212), Chr(8213), Chr(8722), Chr(8226), Chr(183), Chr(10148), Chr(9658), Chr(9656), Chr(187), Chr(8250)]
        t = t.Replace(c, "-")
    end for
    for each c in [Chr(160), Chr(8239), Chr(8201), Chr(8202), Chr(8203), Chr(65279), Chr(12288)]
        t = t.Replace(c, " ")
    end for
    return t
end function

function stripLeadingJunk(s as string) as string
    i = 1
    n = Len(s)
    while i <= n
        c = Mid(s, i, 1)
        if isWordChar(c) or c = "[" or c = "(" or c = "【" then exit while
        i = i + 1
    end while
    return Mid(s, i)
end function

function stripTrailingJunk(s as string) as string
    i = Len(s)
    while i >= 1
        c = Mid(s, i, 1)
        if isWordChar(c) or c = ")" or c = "]" or c = "】" or c = "+" or c = "!" or c = "?" or c = "'" then exit while
        i = i - 1
    end while
    return Left(s, i)
end function

' La etiqueta al inicio si parece código del proveedor: { key, length } o invalid.
function nextNameTag(s as string, st as object) as dynamic
    first = Left(s, 1)
    closers = {"[": "]", "(": ")", "|": "|", "【": "】"}
    if closers.DoesExist(first) then
        cl = closers[first]
        p = Instr(2, s, cl)
        if p > 2 and p - 2 <= 14 then
            inner = Mid(s, 2, p - 2).Trim()
            if inner <> "" then
                j = p + 1
                while Mid(s, j, 1) = " "
                    j = j + 1
                end while
                return {key: UCase(inner), length: j - 1}
            end if
        end if
        return invalid
    end if
    i = 1
    while i <= Len(s) and i <= 7 and isWordChar(Mid(s, i, 1))
        i = i + 1
    end while
    token = Left(s, i - 1)
    if Len(token) = 0 or Len(token) > 6 then return invalid
    j = i
    while Mid(s, j, 1) = " "
        j = j + 1
    end while
    syms = 0
    while syms < 3 and j <= Len(s)
        c = Mid(s, j, 1)
        if c = " " or isWordChar(c) or c = "[" or c = "(" then exit while
        syms = syms + 1
        j = j + 1
    end while
    while Mid(s, j, 1) = " "
        j = j + 1
    end while
    if j > Len(s) or j = i then return invalid
    onlySpace = (syms = 0)
    if codeLike(token) or (not onlySpace and st.countries.DoesExist(UCase(token))) then
        return {key: UCase(token), length: j - 1}
    end if
    return invalid
end function

function nameTags(raw as string, st as object) as object
    out = []
    s = stripLeadingJunk(raw)
    for k = 1 to 3
        t = nextNameTag(s, st)
        if t = invalid then return out
        out.Push(t.key)
        s = stripLeadingJunk(Mid(s, t.length + 1))
    end for
    return out
end function

function stripName(raw as string, remTags as object, remNotes as object, remTails as object, st as object) as object
    removed = []
    s = stripLeadingJunk(raw)
    for k = 1 to 3
        t = nextNameTag(s, st)
        if t = invalid then exit for
        if not remTags.DoesExist("k" + t.key) then exit for
        rest = stripLeadingJunk(Mid(s, t.length + 1))
        if rest.Trim() = "" then exit for
        removed.Push(t.key)
        s = rest
    end for
    for k = 1 to 3
        done = false
        nt = trailingNote(s)
        if nt <> invalid then
            if remNotes.DoesExist("k" + UCase(nt.inner)) and nt.keep <> "" then
                s = nt.keep
                done = true
            end if
        end if
        if not done then
            tr = trailingRes(s)
            if tr <> invalid and tr.keep <> "" then
                removed.Push(tr.inner)
                s = tr.keep
                done = true
            end if
        end if
        if not done then
            tc = trailingCode(stripTrailingJunk(s))
            if tc <> invalid then
                if remTails.DoesExist("k" + UCase(tc.code)) then
                    removed.Push(UCase(tc.code))
                    s = tc.keep
                end if
            end if
        end if
    end for
    s = collapseSpaces(stripTrailingJunk(s)).Trim()
    if s = "" then s = raw.Trim()
    return {name: s, removed: removed, removedKey: removed.Join(",")}
end function

' ------------------------------------------------------------------ series
' "El Chema S01 E05 - El regreso" → {show: "El Chema", season: 1, episode: 5, title: "El regreso"}.
' Reconoce S01E05, S1 E5, T1 E5, 1x05, "Temporada 1 Capítulo 5" y "Season 1 Episode 5".
function parseEpisode(name as string) as dynamic
    n = fold(name)
    sl = Len(n)
    ' S01E05 / T1 E5
    for i = 1 to sl
        ch = Mid(n, i, 1)
        if ch = "s" or ch = "t" then
            r = readDigits(n, i + 1, 2)
            if r <> invalid then
                k = skipSeps(n, r.next)
                if Mid(n, k, 1) = "e" then
                    k2 = k + 1
                    if Mid(n, k2, 1) = "p" then k2 = k2 + 1
                    r2 = readDigits(n, k2, 4)
                    if r2 <> invalid then
                        ep = episodeOf(name, i, r.value, r2.value, r2.next)
                        if ep <> invalid then return ep
                    end if
                end if
            end if
        end if
    end for
    ' 1x05
    for i = 2 to sl - 2
        if Mid(n, i, 1) = "x" and isDigits(Mid(n, i - 1, 1)) then
            a = i - 1
            if a > 1 and isDigits(Mid(n, a - 1, 1)) then a = a - 1
            if a = 1 or not isDigits(Mid(n, a - 1, 1)) then
                r2 = readDigits(n, i + 1, 3)
                if r2 <> invalid and r2.next - (i + 1) >= 2 then
                    ep = episodeOf(name, a, Int(Val(Mid(n, a, i - a))), r2.value, r2.next)
                    if ep <> invalid then return ep
                end if
            end if
        end if
    end for
    ' Temporada 1 Capítulo 5 / Season 1 Episode 5
    for each w in [["temporada", ["capitulo", "episodio", "ep.", "ep", "e"]], ["season", ["episode", "ep.", "ep", "e"]]]
        p = Instr(1, n, w[0])
        if p > 0 then
            k = p + Len(w[0])
            while Mid(n, k, 1) = " "
                k = k + 1
            end while
            r = readDigits(n, k, 2)
            if r <> invalid then
                k = skipSeps(n, r.next)
                for each word in w[1]
                    if Mid(n, k, Len(word)) = word then
                        k2 = k + Len(word)
                        while Mid(n, k2, 1) = " "
                            k2 = k2 + 1
                        end while
                        r2 = readDigits(n, k2, 4)
                        if r2 <> invalid then
                            ep = episodeOf(name, p, r.value, r2.value, r2.next)
                            if ep <> invalid then return ep
                        end if
                    end if
                end for
            end if
        end if
    end for
    return invalid
end function

' Lee de 1 a max dígitos desde i; devuelve {value, next} o invalid.
function readDigits(s as string, i as integer, maxDigits as integer) as dynamic
    j = i
    while j <= Len(s) and isDigits(Mid(s, j, 1)) and j - i < maxDigits
        j = j + 1
    end while
    if j = i then return invalid
    if j <= Len(s) and isDigits(Mid(s, j, 1)) then return invalid
    return {value: Int(Val(Mid(s, i, j - i))), next: j}
end function

function skipSeps(s as string, i as integer) as integer
    k = i
    while k <= Len(s) and Instr(1, " .-_:", Mid(s, k, 1)) > 0
        k = k + 1
    end while
    return k
end function

function isSepChar(c as string) as boolean
    if c = "" then return false
    return Instr(1, " -–—|:._([,", c) > 0
end function

function trimSeps(s as string, extraEnd = "" as string) as string
    a = 1
    b = Len(s)
    while a <= b and isSepChar(Mid(s, a, 1))
        a = a + 1
    end while
    while b >= a and (isSepChar(Mid(s, b, 1)) or (extraEnd <> "" and Instr(1, extraEnd, Mid(s, b, 1)) > 0))
        b = b - 1
    end while
    if b < a then return ""
    return Mid(s, a, b - a + 1)
end function

function episodeOf(name as string, showEnd as integer, season as integer, episode as integer, rest as integer) as dynamic
    show = trimSeps(Left(name, showEnd - 1))
    if show = "" then return invalid
    return {show: show, season: season, episode: episode, title: trimSeps(Mid(name, rest), ")]")}
end function
