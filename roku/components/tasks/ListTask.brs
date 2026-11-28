' Baja, lee y ordena una lista. Las de usuario y contraseña (Xtream) van por su API:
' canales completos, y películas y series por categoría cuando las abres (así no se llena la memoria).
' Si la lista no responde, usa la copia guardada.
sub init()
    m.top.functionName = "work"
end sub

sub fail(p as object, msg as string)
    m.top.result = {ok: false, mode: p.mode, list: p.list, message: msg}
end sub

sub work()
    p = m.top.params
    l = p.list
    creds = invalid
    if l.kind = "XTREAM" then creds = {server: normalizeServer(l.server), user: l.user, pass: l.pass}
    if l.kind = "URL" then creds = xtCredsFromUrl(l.url)
    if creds <> invalid then
        if xtLoad(p, l, creds) then return
    end if
    m3uLoad(p, l)
end sub

' ------------------------------------------------------------------ M3U
sub m3uLoad(p as object, l as object)
    cache = "cachefs:/fitotv_" + l.id + ".m3u"
    fs = CreateObject("roFileSystem")
    text = invalid
    stale = false
    if l.kind = "FILE" then
        if p.text <> invalid and p.text <> "" then
            text = p.text
            WriteAsciiFile(cache, text)
        else if fs.Exists(cache) then
            text = ReadAsciiFile(cache)
        else
            fail(p, "No encontramos el archivo de la lista. Vuelve a mandarlo desde el celular")
            return
        end if
    else
        url = l.url
        if l.kind = "XTREAM" then url = xtreamM3uUrl(l.server, l.user, l.pass)
        r = fetchText(url)
        if r.ok then
            text = r.text
            WriteAsciiFile(cache, text)
        else if p.mode = "load" and fs.Exists(cache) then
            text = ReadAsciiFile(cache)
            stale = true
        else
            fail(p, r.message)
            return
        end if
    end if
    if text = invalid or (Instr(1, UCase(Left(text, 4000)), "#EXT") = 0 and Instr(1, LCase(Left(text, 4000)), "http") = 0) then
        fail(p, "Esa dirección no trae una lista de canales")
        return
    end if
    parsed = parseM3U(text)
    text = invalid
    chans = cleanLive(parsed.channels)
    if chans.Count() = 0 then
        fail(p, "La lista llegó sin canales")
        return
    end if
    t = newTree()
    kids = p.kids = true
    total = 0
    live = 0
    for each ch in chans
        if not kids or isKidsGroup(ch.group) then
            if addChannel(t, ch) then
                total = total + 1
                if ch.kind = "LIVE" then live = live + 1
            end if
        end if
    end for
    if total = 0 then
        fail(p, "Esta lista no trae un grupo Infantil. Prueba con otra lista.")
        return
    end if
    sortNewest(t)
    m.top.result = {ok: true, mode: p.mode, list: l, content: t.root, count: total, live: live, epgUrl: parsed.epgUrl, stale: stale}
end sub

' Nombres limpios solo en canales en vivo: en películas y series "E05" o "(2019)" son parte del nombre.
function cleanLive(all as object) as object
    live = []
    idx = []
    for i = 0 to all.Count() - 1
        if all[i].kind = "LIVE" then
            live.Push(all[i])
            idx.Push(i)
        end if
    end for
    if live.Count() = 0 then return all
    cleaned = cleanNames(live)
    for j = 0 to idx.Count() - 1
        all[idx[j]] = cleaned[j]
    end for
    return all
end function

' ------------------------------------------------------------------ árbol de contenido
' root: sec_live, sec_movie, sec_series (grupos con canales, películas o series) y
' sec_episodes (los episodios de cada serie de la lista M3U, fuera de la vista).
function newTree() as object
    root = CreateObject("roSGNode", "ContentNode")
    secs = {}
    for each k in ["LIVE", "MOVIE", "SERIES", "EPISODES"]
        s = root.createChild("ContentNode")
        s.id = "sec_" + LCase(k)
        secs[k] = s
    end for
    return {root: root, secs: secs, groups: {}, shows: {}, ids: {}}
end function

function groupNode(t as object, kind as string, name as string) as object
    key = kind + "|" + name
    g = t.groups[key]
    if g = invalid then
        g = t.secs[kind].createChild("ContentNode")
        g.title = name
        t.groups[key] = g
    end if
    return g
end function

function addChannel(t as object, ch as object) as boolean
    if t.ids.DoesExist(ch.id) then return false
    t.ids[ch.id] = true
    if ch.kind = "SERIES" then
        ep = parseEpisode(ch.name)
        if ep <> invalid then
            key = "m:" + ep.show
            holder = t.shows[key]
            if holder = invalid then
                ' La serie aparece una vez en su categoría; sus episodios van aparte.
                g = groupNode(t, "SERIES", ch.group)
                sh = g.createChild("ContentNode")
                sh.id = "show:" + key
                sh.title = ep.show
                sh.HDPosterUrl = ch.logo
                sh.addFields({num: 0, group: ch.group, kind: "SERIES", tvgId: "", sid: "", ua: "", ref: "", seriesId: key, season: 0, episode: 0, rk: 0})
                holder.addFields({show: sh})
                holder = t.secs.EPISODES.createChild("ContentNode")
                holder.id = "eps:" + key
                t.shows[key] = holder
            end if
            n = holder.createChild("ContentNode")
            fillNode(n, ch)
            n.setFields({seriesId: key, season: ep.season, episode: ep.episode})
            ' La serie es tan nueva como su episodio más nuevo.
            k = numOf(n.sid)
            if k > holder.show.rk then holder.show.rk = k
            n.addFields({epTitle: ep.title})
            return true
        end if
    end if
    g = groupNode(t, ch.kind, ch.group)
    n = g.createChild("ContentNode")
    fillNode(n, ch)
    return true
end function

' Películas y series de lo más nuevo a lo más viejo dentro de cada categoría. Las listas M3U no traen
' fecha; el número del video en el servidor sirve, porque los proveedores los numeran en orden.
sub sortNewest(t as object)
    for each kind in ["MOVIE", "SERIES"]
        sec = t.secs[kind]
        for gi = 0 to sec.getChildCount() - 1
            g = sec.getChild(gi)
            n = g.getChildCount()
            if n > 1 then
                items = []
                kids = g.getChildren(-1, 0)
                for each c in kids
                    k = 0
                    if c.hasField("rk") then k = c.rk else k = numOf(c.sid)
                    items.Push({k: k, n: c})
                end for
                items.SortBy("k", "r")
                g.removeChildrenIndex(n, 0)
                ordered = []
                for each it in items
                    ordered.Push(it.n)
                end for
                g.appendChildren(ordered)
            end if
        end for
    end for
end sub

sub fillNode(n as object, ch as object)
    n.id = ch.id
    n.title = ch.name
    u = playableUrl(ch.url)
    n.url = u
    n.streamFormat = streamFormatFor(u)
    n.HDPosterUrl = ch.logo
    sid = ch.sid
    if sid = invalid then sid = xtreamStreamId(ch.url)
    n.addFields({num: ch.num, group: ch.group, kind: ch.kind, tvgId: ch.tvgId, sid: sid, ua: ch.ua, ref: ch.ref, seriesId: "", season: 0, episode: 0})
end sub

' ------------------------------------------------------------------ Xtream
' Devuelve false si el servidor no tiene la API (entonces se usa el M3U).
function xtLoad(p as object, l as object, c as object) as boolean
    cache = "cachefs:/fitotv_" + l.id + ".xt"
    fs = CreateObject("roFileSystem")
    stale = false
    acc = invalid
    r = fetchText(xtUrl(c, ""), 20000)
    if r.ok then acc = ParseJson(r.text)
    liveText = invalid
    liveCats = invalid
    vodCats = invalid
    serCats = invalid
    if type(acc) = "roAssociativeArray" and type(acc.user_info) = "roAssociativeArray" then
        ui = acc.user_info
        if anyStr(ui.auth) = "0" then
            fail(p, "El proveedor no dejó entrar. Revisa el usuario y la contraseña.")
            return true
        end if
        st = LCase(anyStr(ui.status))
        if st = "expired" then
            fail(p, "La cuenta de tu proveedor venció. Renuévala con él para volver a ver.")
            return true
        end if
        if st = "banned" or st = "disabled" then
            fail(p, "Tu proveedor desactivó la cuenta.")
            return true
        end if
        liveCats = xtCategories(c, "get_live_categories")
        lr = fetchText(xtUrl(c, "get_live_streams"), 90000)
        if not lr.ok then
            if p.mode = "load" and fs.Exists(cache) then
                stale = true
            else
                fail(p, lr.message)
                return true
            end if
        else
            liveText = lr.text
        end if
        vodCats = xtCategories(c, "get_vod_categories")
        serCats = xtCategories(c, "get_series_categories")
    else if r.ok then
        ' Contestó, pero no como Xtream: el M3U de siempre.
        return false
    else if p.mode = "load" and fs.Exists(cache) then
        stale = true
    else
        return false
    end if
    if liveText = invalid then
        ' Sin internet: la copia guardada (canales y categorías).
        saved = ParseJson(ReadAsciiFile(cache))
        if type(saved) <> "roAssociativeArray" then return false
        liveText = saved.live
        liveCats = saved.liveCats
        vodCats = saved.vodCats
        serCats = saved.serCats
    else
        WriteAsciiFile(cache, FormatJson({live: liveText, liveCats: liveCats, vodCats: vodCats, serCats: serCats}))
    end if

    ' Canales en vivo, en el orden de las categorías del proveedor.
    names = {}
    order = []
    for each cat in liveCats
        names["c" + anyStr(cat.category_id)] = anyStr(cat.category_name).Trim()
        order.Push("c" + anyStr(cat.category_id))
    end for
    buckets = {}
    gOrder = []
    for each part in xtParts(liveText)
        o = xtObj(part)
        if o <> invalid then
            id = anyStr(o.stream_id)
            if id <> "" then
                ck = "c" + anyStr(o.category_id)
                g = names[ck]
                if g = invalid or g = "" then g = "Sin grupo"
                if not buckets.DoesExist(g) then
                    buckets[g] = []
                    gOrder.Push(g)
                end if
                buckets[g].Push({id: "xl" + id, num: numOf(o.num), name: anyStr(o.name).Trim(), url: c.server + "/live/" + c.user + "/" + c.pass + "/" + id + ".m3u8", logo: anyStr(o.stream_icon).Trim(), group: g, tvgId: anyStr(o.epg_channel_id).Trim(), kind: "LIVE", ua: "", ref: "", sid: id})
            end if
        end if
    end for
    liveText = invalid
    chans = []
    seenG = {}
    for each ck in order
        g = names[ck]
        if g <> invalid and buckets.DoesExist(g) and not seenG.DoesExist(g) then
            seenG[g] = true
            chans.Append(buckets[g])
        end if
    end for
    for each g in gOrder
        if not seenG.DoesExist(g) then
            seenG[g] = true
            chans.Append(buckets[g])
        end if
    end for
    buckets = invalid
    chans = cleanNames(chans)
    t = newTree()
    kids = p.kids = true
    live = 0
    n = 0
    for each ch in chans
        n = n + 1
        if ch.num <= 0 then ch.num = n
        if ch.name = "" then ch.name = "Canal " + n.ToStr()
        if not kids or isKidsGroup(ch.group) then
            if addChannel(t, ch) then live = live + 1
        end if
    end for
    ' Películas y series: solo las categorías; su contenido se baja al abrirlas.
    lazy = 0
    for each spec in [["MOVIE", vodCats, "get_vod_streams"], ["SERIES", serCats, "get_series"]]
        if type(spec[1]) = "roArray" then
            for each cat in spec[1]
                g = anyStr(cat.category_name).Trim()
                if g <> "" and (not kids or isKidsGroup(g)) then
                    gn = groupNode(t, spec[0], g)
                    if not gn.hasField("xtAction") then
                        gn.addFields({xtAction: spec[2], catId: anyStr(cat.category_id), loaded: false})
                        lazy = lazy + 1
                    end if
                end if
            end for
        end if
    end for
    if live + lazy = 0 then
        if kids then fail(p, "Esta lista no trae un grupo Infantil. Prueba con otra lista.") else fail(p, "La lista llegó sin canales")
        return true
    end if
    t.root.addFields({xt: c})
    m.top.result = {ok: true, mode: p.mode, list: l, content: t.root, count: live, live: live, epgUrl: "", stale: stale, xt: c}
    return true
end function

function xtCategories(c as object, action as string) as object
    r = fetchText(xtUrl(c, action), 30000)
    if not r.ok then return []
    a = ParseJson(r.text)
    if type(a) <> "roArray" then return []
    return a
end function
