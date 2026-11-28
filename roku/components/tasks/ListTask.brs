' Baja, lee y ordena una lista (M3U o Xtream). Si no responde, usa la copia guardada.
sub init()
    m.top.functionName = "work"
end sub

function fetchText(url as string) as object
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetUrl(url)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.AddHeader("User-Agent", userAgent())
    xfer.EnableEncodings(true)
    xfer.RetainBodyOnError(true)
    tmp = "tmp:/fitotv_list.m3u"
    fs = CreateObject("roFileSystem")
    fs.Delete(tmp)
    if not xfer.AsyncGetToFile(tmp) then return {ok: false, message: "Esa dirección no parece un enlace válido"}
    msg = wait(60000, port)
    if msg = invalid then
        xfer.AsyncCancel()
        return {ok: false, message: "El servidor de la lista tardó demasiado en responder"}
    end if
    code = msg.GetResponseCode()
    if code = 200 then
        txt = ReadAsciiFile(tmp)
        fs.Delete(tmp)
        return {ok: true, text: txt}
    end if
    fs.Delete(tmp)
    if code = 401 or code = 403 or code = 884 then return {ok: false, message: "El proveedor no dejó entrar. Revisa el usuario y la contraseña, o si la cuenta venció"}
    if code = 404 then return {ok: false, message: "Esa dirección no existe en el servidor"}
    if code < 0 then
        reason = LCase(msg.GetFailureReason())
        if Instr(1, reason, "resolve") > 0 then return {ok: false, message: "No encontramos esa dirección. Revisa que esté bien escrita y que la tele tenga internet"}
        if Instr(1, reason, "timed out") > 0 then return {ok: false, message: "El servidor de la lista tardó demasiado en responder"}
        return {ok: false, message: "No pudimos conectarnos con el servidor de la lista"}
    end if
    return {ok: false, message: "El servidor respondió con un error (" + code.ToStr() + ")"}
end function

sub fail(p as object, msg as string)
    m.top.result = {ok: false, mode: p.mode, list: p.list, message: msg}
end sub

sub work()
    p = m.top.params
    l = p.list
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
    chans = cleanNames(parsed.channels)
    if chans.Count() = 0 then
        fail(p, "La lista llegó sin canales")
        return
    end if
    kids = p.kids = true
    root = CreateObject("roSGNode", "ContentNode")
    secs = {}
    for each k in ["LIVE", "MOVIE", "SERIES"]
        s = root.createChild("ContentNode")
        s.id = "sec_" + LCase(k)
        secs[k] = s
    end for
    groups = {}
    live = 0
    total = 0
    for each ch in chans
        if not kids or isKidsGroup(ch.group) then
            key = ch.kind + "|" + ch.group
            g = groups[key]
            if g = invalid then
                g = secs[ch.kind].createChild("ContentNode")
                g.title = ch.group
                groups[key] = g
            end if
            n = g.createChild("ContentNode")
            n.id = ch.id
            n.title = ch.name
            u = playableUrl(ch.url)
            n.url = u
            n.streamFormat = streamFormatFor(u)
            n.HDPosterUrl = ch.logo
            n.addFields({num: ch.num, group: ch.group, kind: ch.kind, tvgId: ch.tvgId, sid: xtreamStreamId(ch.url), ua: ch.ua, ref: ch.ref})
            if ch.kind = "LIVE" then live = live + 1
            total = total + 1
        end if
    end for
    if total = 0 then
        fail(p, "Esta lista no trae un grupo Infantil. Prueba con otra lista.")
        return
    end if
    m.top.result = {ok: true, mode: p.mode, list: l, content: root, count: total, live: live, epgUrl: parsed.epgUrl, stale: stale}
end sub
